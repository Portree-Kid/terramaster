package org.flightgear.terramaster;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.flightgear.terramaster.dns.WeightedUrl;

import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;
import org.tukaani.xz.XZInputStream;

/**
 * Implementation of the new TerraSync version. Does the most of the sync logic and provides 
 * the information of locally known data.
 *
 * @author keith.paterson
 * @author Simon
 */
public class HTTPTerraSync extends Thread implements TileService {

    private static final String DIRINDEX_FILENAME = ".dirindex";

    private static final int DIR_SIZE = 2000;

    private static final int AIRPORT_MAX = 30000;

    private static final Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);

  private String[] getEnabledVersions() {
    Properties props = terraMaster.getProps();
    return Arrays.stream(TerraSyncRootDirectoryType.values()).filter((t) -> props.containsKey(t.name() + "." + TerraMasterProperties.ENABLED)).map((t) -> t.name()).toArray(String[]::new);
  }

    enum UPDATETYPE {
        RESET, UPDATE, EXTEND, START
    }

    private static final int MAXRETRY = 10;
    private final CopyOnWriteArrayList<Syncable> syncList = new CopyOnWriteArrayList<>();
    private boolean cancelFlag = false;

    private List<WeightedUrl> urls = new ArrayList<>();
    SecureRandom rand = new SecureRandom();

    private HttpURLConnection httpConn;

    private boolean ageCheck;

    private long maxAge;

    private final Object mutex = new Object();

    private final HashMap<WeightedUrl, TileResult> downloadStats = new HashMap<>();
    private final HashMap<WeightedUrl, TileResult> badUrls = new HashMap<>();
    
    private class DirIndexCacheKey {
      private WeightedUrl url;
      private String dir;

      public DirIndexCacheKey(WeightedUrl url, String dir) {
        this.url = url;
        this.dir = dir;
      }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 37 * hash + Objects.hashCode(this.url);
      hash = 37 * hash + Objects.hashCode(this.dir);
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      final DirIndexCacheKey other = (DirIndexCacheKey) obj;
      if (!Objects.equals(this.dir, other.dir)) {
        return false;
      }
      return Objects.equals(this.url, other.url);
    }
      

            
    }
    
    private final HashMap<DirIndexCacheKey, String[]> dirIndexCache = new HashMap<>();

    private final TerraMaster terraMaster;

    private boolean quitFlag;

    private int retryCount;

    public HTTPTerraSync(TerraMaster terraMaster) {
        super("HTTPTerraSync");
        this.terraMaster = terraMaster;
    }

    @Override
    public void sync(Collection<Syncable> set, boolean ageCheck) {

        this.ageCheck = ageCheck;
        for (Syncable syncable : set) {
            if (syncable == null) {
                continue;
            }
            synchronized (syncList) {
                syncList.add(syncable);
                cancelFlag = false;
                syncList.sort(Comparator.comparing(Syncable::getName));
            }
            log.log(Level.FINEST, "Added {0} to queue", syncable.getName());
        }
        wakeUp();
    }

    @Override
    public void wakeUp() {
        synchronized (mutex) {
            mutex.notifyAll();
        }
    }

    @Override
    public Collection<Syncable> getSyncList() {
        return syncList;
    }

    @Override
    public void quit() {
        quitFlag = true;
        synchronized (syncList) {
            syncList.clear();
        }
        synchronized (mutex) {
            mutex.notifyAll();
        }
        (new Thread(() -> {
            try {
                if (httpConn != null && httpConn.getInputStream() != null) {
                    httpConn.getInputStream().close();
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "Error while shutting down HTTPTerraSync: ", e);
            }
        })).start();
    }

    @Override
    public void cancel() {
        cancelFlag = true;
        synchronized (syncList) {
            syncList.clear();
        }
        (new Thread("Http Cancel Thread") {
            @Override
            public void run() {
                try {
                    if (httpConn != null && httpConn.getInputStream() != null) {
                        httpConn.getInputStream().close();
                    }
                } catch (IOException e) {
                    // Expecting to throw error
                }
            }
        }).start();
    }

    @Override
    public void delete(Collection<TileName> selection) {
        for (TileName n : selection) {
            TileData d = terraMaster.getMapScenery().remove(n);
            if (d == null) {
                continue;
            }
            d.delete();

            synchronized (syncList) {
                syncList.remove(n);
            }
        }
    }

    @Override
    public void run() {
        try {
            while (!quitFlag) {
                synchronized (mutex) {
                    if (syncList.isEmpty()) {
                        mutex.wait(2000);
                    }
                    if (syncList.isEmpty()) {
                        continue;
                    }
                }
                // Woke up
                sync();
            }
            log.fine("HTTP TerraSync ended gracefully");
        } catch (Exception e) {
            log.log(Level.SEVERE, "HTTP Crashed ", e);
            e.printStackTrace();
        }
    }

    private void sync() {
        int tilesize = 10000;
        // update progressbar
        invokeLater(UPDATETYPE.START, 0); // update
        invokeLater(UPDATETYPE.EXTEND, syncList.size() * tilesize + AIRPORT_MAX); // update
        downloadStats.clear();
        badUrls.clear();
        while (!syncList.isEmpty()) {
            String[] versions = getEnabledVersions();
            final Syncable syncable;
            synchronized (syncList) {
                if (syncList.isEmpty()) {
                    continue;
                }
                syncable = syncList.get(0);
            }
            for (String version : versions) {
                TerraSyncRootDirectoryType rootType = TerraSyncRootDirectoryType.valueOf(version);
                final List<WeightedUrl> newUrls = getUrl(rootType);
                if (newUrls != urls) {
                    urls = newUrls;
                    urls.forEach(element -> downloadStats.put(element, new TileResult(element)));
                }                
                
                TerraSyncDirectoryType[] types = syncable.getTypes(rootType);
                for (TerraSyncDirectoryType terraSyncDirectoryType : types) {
                    int updates = 0;
                    if (terraSyncDirectoryType.isInRoot(rootType)) {
                      final String basePath;
                      if (syncable instanceof TileName) {
                        basePath = terraMaster.getProps().getProperty(rootType + "." + TerraMasterProperties.SCENERY_PATH);
                        if (basePath==null||basePath.isBlank()) {
                          continue;
                        }
                      } else {
                        basePath = syncable.basePath();
                      }
                      updates = syncDirectory(terraSyncDirectoryType.getDirname() + "/" + syncable.buildPath(), basePath, false, terraSyncDirectoryType, rootType);
                    }
                    invokeLater(UPDATETYPE.UPDATE, DIR_SIZE - updates); // update progressBar
                }
            }

            synchronized (syncList) {
                syncList.remove(syncable);
            }
        }
        HashMap<WeightedUrl, TileResult> completeStats = new HashMap<>();
        completeStats.putAll(downloadStats);
        completeStats.putAll(badUrls);

        terraMaster.showStats(completeStats);
        // syncList is now empty
        invokeLater(UPDATETYPE.RESET, 0); // reset progressBar
    }

  private List<WeightedUrl> getUrl(TerraSyncRootDirectoryType dirType) {
    WeightedUrl url = new WeightedUrl("100", terraMaster.getProps().getProperty(dirType + "." + TerraMasterProperties.URL));
    return Arrays.asList(url);
  }

    /**
     * Get a weighted random URL
     */
    private WeightedUrl getBaseUrl() {
        if (urls.isEmpty()) {
            log.warning("No URLs to sync with, retrying.");
            if (retryCount++ < MAXRETRY) {
                resetUrls();
            }
        }

        // Compute the total weight of all items together
        double totalWeight = 0.0d;
        for (WeightedUrl i : urls) {
            totalWeight += i.getWeight();
        }
        // Now choose a random item
        int randomIndex = -1;

        double random = rand.nextDouble() * totalWeight;
        for (int i = 0; i < urls.size(); ++i) {
            random -= urls.get(i).getWeight();
            if (random <= 0.0d) {
                randomIndex = i;
                break;
            }
        }
        return urls.get(randomIndex);
    }

    private void resetUrls() {
        urls.addAll(badUrls.keySet());
    }

    /**
     * Downloads a File into a byte[]
     *
     * @return byte[] of the file or empty byte[]
     */
    private byte[] downloadFile(WeightedUrl baseUrl, String file) throws IOException {
        long start = System.currentTimeMillis();
        URL url = new URL(baseUrl.getUrl().toExternalForm() + (file.startsWith("/")?file:("/"+file)));

        log.finest(() -> "Downloading : " + url.toExternalForm());
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setConnectTimeout(10000);
        httpConn.setReadTimeout(20000);
        int responseCode = (httpConn).getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            final String fileName;
            String disposition = httpConn.getHeaderField("Content-Disposition");
            String contentType = httpConn.getContentType();
            int contentLength = httpConn.getContentLength();

            if (disposition != null) {
                // extracts file name from header field
                int index = disposition.indexOf("filename=");
                if (index > 0) {
                    fileName = disposition.substring(index + 10, disposition.length() - 1);
                } else {
                    fileName = "";
                }
            } else {
                fileName = url.getFile();
            }

            log.finest(() -> "Content-Type = " + contentType);
            log.finest(() -> "Content-Disposition = " + disposition);
            log.finest(() -> "Content-Length = " + contentLength);
            log.finest(() -> "fileName = " + fileName);

            // opens input stream from the HTTP connection
            InputStream inputStream = httpConn.getInputStream();

            // opens an output stream to save into file
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            int bytesRead;
            byte[] buffer = new byte[1024];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            log.fine("File downloaded");
            downloadStats.get(baseUrl).actualDownloads += 1;
            downloadStats.get(baseUrl).numberBytes += outputStream.size();
            downloadStats.get(baseUrl).time += System.currentTimeMillis() - start;
            return outputStream.toByteArray();
        } else {
            downloadStats.get(baseUrl).errors += 1;
            log.warning(
                    () -> "No file to download. Server replied HTTP code: " + responseCode + " for " + url.toExternalForm());
        }
        httpConn.disconnect();
        return "".getBytes();
    }

    /**
     * Syncs the given directory.
     */
    private int syncDirectory(String path, String localBaseDir, boolean force, TerraSyncDirectoryType type, TerraSyncRootDirectoryType rootType) {
        while (!urls.isEmpty()) {

            WeightedUrl baseUrl = getBaseUrl();
            try {
                int updates = 0;
                if (cancelFlag) {
                    return updates;
                }
              final String[] parentRemoteDirIndex = getRemoteDirIndex(baseUrl, localBaseDir, getParent(path));
              
                HashMap<String, String> parentTypeLookup = buildTypeLookup(parentRemoteDirIndex);
                HashMap<String, String> parentFileLookup = buildFileLookup(parentRemoteDirIndex);
                HashMap<String, String> parentHashLookup = buildHashLookup(parentRemoteDirIndex);
                String[] parts = path.replace("\\", "/").replace("//", "/").split("/");
                String tileName = parts[parts.length - 1];
                String pathType = parentTypeLookup.get(tileName);
                if (pathType==null||pathType.isBlank()) {
                  log.log(Level.WARNING, () -> "Couldn't process " + path + " type empty");
                  return updates;
                }
                

              switch (pathType) {
                case "t":
                  updates += processTar(path + ".txz", localBaseDir, force, type);
                  break;
                case "d":
                  updates += processDir(path, localBaseDir, force, type, rootType);
                  break;
                case "f":                  
                  updates+= processFile(localBaseDir, getParent(path), parentFileLookup.get(tileName), parentHashLookup.get(tileName));
                  break;
                default:
                  log.log(Level.WARNING, () -> "Couldn't process " + path + " with type " + pathType);
                  break;
              }

                if (type.isTile()) {
                    addScnMapTile(terraMaster.getMapScenery(), new File(localBaseDir, path), rootType, type);
                }

                return updates;
            } catch (javax.net.ssl.SSLHandshakeException e) {
                log.log(Level.WARNING, "Handshake Error " + e + " syncing " + path, e);
                JOptionPane.showMessageDialog(terraMaster.frame,
                        "Sync can fail if Java older than 8u101 and 7u111 with https hosts.\r\n"
                        + baseUrl.getUrl().toExternalForm(),
                        "SSL Error", JOptionPane.ERROR_MESSAGE);
                markBad(baseUrl, e);
            } catch (SocketException e) {
                log.log(Level.WARNING, "Connect Error " + e + " syncing with " + baseUrl.getUrl().toExternalForm()
                        + path.replace("\\", "/") + " removing URL", e);
                markBad(baseUrl, e);
                return 0;
            } catch (UnknownHostException e) {
                log.log(Level.WARNING, "Unknown Host Error " + e + " syncing with "
                        + baseUrl.getUrl().toExternalForm() + path.replace("\\", "/") + " removing URL. Connected?", e);
                markBad(baseUrl, e);
                return 0;
            } catch (Exception e) {
                log.log(Level.WARNING, "General Error " + e + " syncing with " + baseUrl.getUrl().toExternalForm()
                        + path.replace("\\", "/"), e);
                e.printStackTrace();
                return 0;
            }
        }
        return 0;
    }

    private int processTar(String pathString, String localBaseDir, boolean force, TerraSyncDirectoryType type) throws IOException {
        Path path = Paths.get(pathString);
        String fileName = path.getFileName().toString();
        String[] remoteDirIndex = getRemoteDirIndex(getBaseUrl(), localBaseDir, path.getParent().toString());
        String remoteHash = "";
        for (String line : remoteDirIndex) {
            String[] splitLine = line.split(":");
            if (splitLine[1].equals(fileName)) {
                remoteHash = splitLine[2];
            }
        }

        boolean load = true;
        File localFile = new File(localBaseDir + File.separator + pathString);
        if (localFile.exists()) {
            log.log(Level.FINEST, "Localfile : {0}", localFile.getAbsolutePath());
            byte[] localHashBytes;
            try {
                localHashBytes = calcSHA1(localFile);
            } catch (NoSuchAlgorithmException e) {
                log.log(Level.WARNING, "Error while checking local txz file hash:", e);
                localHashBytes = new byte[0];
            }
            String localHash = bytesToHex(localHashBytes);
            load = !remoteHash.equals(localHash);
        }

        if (load || force) {
            byte[] bs = downloadFile(getBaseUrl(), pathString);
            try {
                if (!Files.exists(localFile.getParentFile().toPath())) {
                  Files.createDirectories(localFile.getParentFile().toPath());
                }
                Files.copy(new ByteArrayInputStream(bs), localFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.log(Level.WARNING, "Error while copying txz:", e);
            }
        } else {
            log.log(Level.INFO, "Not downloading {0} because file hashes match", pathString);
            return 0;
        }

        int updates = 0;
        // Extract txz file
        try (TarInputStream tar = new TarInputStream(new XZInputStream(Files.newInputStream(localFile.toPath())))) {
            TarEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                Path extractTo = localFile.toPath().getParent().resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(extractTo);
                } else {
                    Files.copy(tar, extractTo, StandardCopyOption.REPLACE_EXISTING);
                    updates++;
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Error while untarring " + pathString + ":", e);
            e.printStackTrace();
        }
        return updates;
    }

    private int processDir(String path, String localBaseDir, boolean force, TerraSyncDirectoryType type, TerraSyncRootDirectoryType rootType)
            throws IOException, NoSuchAlgorithmException {
        int updates = 0;
        String localDirIndex = readLocalDirIndex(path, localBaseDir);
        String[] localLines = localDirIndex.split("\r?\n");
        if (!force && ageCheck && getDirIndexAge(path, localBaseDir) < maxAge) {
            return localLines.length;
        }
        String[] lines = getRemoteDirIndex(getBaseUrl(), localBaseDir, path);
        HashMap<String, String> lookup = buildLookup(localLines);
        for (String line : lines) {
            if (cancelFlag) {
                return updates;
            }
            String[] splitLine = line.split(":");
            if (line.startsWith("d:")) {
                // We've got a directory if force ignore what we know
                // otherwise check the SHA against
                // the one from the server
                String dirname = path + "/" + splitLine[1];
                dirname = dirname.replace("\\", "/").replace("//", "/");
                if (force || !(new File(dirname).exists()) || !splitLine[2].equals(lookup.get(splitLine[1]))) {
                    updates += syncDirectory(dirname, localBaseDir, force, type, rootType);
                }
            } else if (line.startsWith("f:")) {
                updates+= processFile(localBaseDir, path, splitLine[1], splitLine[2]);
            } else if (line.startsWith("t:")) {
                updates += processTar(path + splitLine[1], localBaseDir, force, type);
            }
            log.finest(line);
        }
        return updates;
    }

  public int processFile(String localBaseDir, String path, String fileName, String localSHA1) throws IOException, NoSuchAlgorithmException {
    // We've got a file
    File localFile = new File(localBaseDir, path + File.separator + fileName);
    log.finest(localFile.getAbsolutePath());
    boolean load = true;
    if (localFile.exists()) {
      log.log(Level.FINEST, "Localfile : {0}", localFile.getAbsolutePath());
      byte[] b = calcSHA1(localFile);
      String bytesToHex = bytesToHex(b);
      // Changed
      load = !localSHA1.equals(bytesToHex);
    } else {
      // New
      if (!localFile.getParentFile().exists()) {
        localFile.getParentFile().mkdirs();
      }
    }
    WeightedUrl filebaseUrl = getBaseUrl();
    if (load) {
      downloadFile(path, getBaseUrl(), fileName, localFile, filebaseUrl);
    } else {
      downloadStats.get(filebaseUrl).equal += 0;
    }
    invokeLater(UPDATETYPE.UPDATE, 1);
    return 1;
  }

    private String getParent(String path) {
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.contains("/")) {
            return path.substring(0, path.lastIndexOf('/'));
        }
        return "";
    }

    private HashMap<String, String> buildLookup(String[] localLines) {
        HashMap<String, String> lookup = new HashMap<>();
        for (String line : localLines) {
            String[] splitLine = line.split(":");
            if (splitLine.length > 2) {
                lookup.put(splitLine[1], splitLine[2]);
            }
        }
        return lookup;
    }

    private HashMap<String, String> buildTypeLookup(String[] localLines) {
        HashMap<String, String> lookup = new HashMap<>();
        for (String line : localLines) {
            String[] splitLine = line.split(":");
            if (splitLine.length > 2) {
                lookup.put(splitLine[1].split("\\.")[0], splitLine[0]);
            }
        }
        return lookup;
    }

    private HashMap<String, String> buildFileLookup(String[] localLines) {
        HashMap<String, String> lookup = new HashMap<>();
        for (String line : localLines) {
            String[] splitLine = line.split(":");
            if (splitLine.length > 2) {
                lookup.put(splitLine[1].split("\\.")[0], splitLine[1]);
            }
        }
        return lookup;
    }

    private HashMap<String, String> buildHashLookup(String[] localLines) {
        HashMap<String, String> lookup = new HashMap<>();
        for (String line : localLines) {
            String[] splitLine = line.split(":");
            if (splitLine.length > 2) {
                lookup.put(splitLine[1].split("\\.")[0], splitLine[2]);
            }
        }
        return lookup;
    }

    private String[] getRemoteDirIndex(WeightedUrl baseUrl, String localBaseDir, String path) throws IOException {
        DirIndexCacheKey key = new DirIndexCacheKey(baseUrl, path);
        if (dirIndexCache.containsKey(key)) {
            return dirIndexCache.get(key);
        }
        String remoteDirIndex = new String(downloadFile(baseUrl, path.replace("\\", "/") + "/.dirindex"));
        if (!remoteDirIndex.isEmpty()) {
            storeDirIndex(path, localBaseDir, remoteDirIndex);
            dirIndexCache.put(key, remoteDirIndex.split("\r?\n"));
        }
        return remoteDirIndex.split("\r?\n");
    }

    private void downloadFile(String path, WeightedUrl baseUrl, String fileName, File localFile,
            WeightedUrl filebaseUrl) throws IOException {
        try {
            downloadFile(localFile, filebaseUrl, path.replace("\\", "/") + "/" + fileName);
        } catch (javax.net.ssl.SSLHandshakeException e) {
            log.log(Level.WARNING, "Handshake Error " + e + " syncing " + path + " removing Base-URL", e);
            JOptionPane.showMessageDialog(terraMaster.frame,
                    "Sync can fail if Java older than 8u101 and 7u111 with https hosts.\r\n"
                    + filebaseUrl.getUrl().toExternalForm(),
                    "SSL Error", JOptionPane.ERROR_MESSAGE);
            markBad(filebaseUrl, e);
        } catch (SocketException e) {
            log.log(Level.WARNING, "Connect Error " + e + " syncing with " + baseUrl.getUrl().toExternalForm()
                    + path.replace("\\", "/") + " removing Base-URL", e);
            markBad(filebaseUrl, e);
        }
    }

    private boolean markBad(WeightedUrl filebaseUrl, Exception e) {
        TileResult tileResult = downloadStats.get(filebaseUrl);
        tileResult.setException(e);
        badUrls.put(filebaseUrl, tileResult);
        return urls.remove(filebaseUrl);
    }

    /**
     * Downloads a file and stores it in the given local file
     */
    private int downloadFile(File localFile, WeightedUrl filebaseUrl, String url) throws IOException {
        byte[] fileContent = downloadFile(filebaseUrl, url);
        if (fileContent.length == 0)
          return 0;
        try (FileOutputStream fos = new FileOutputStream(localFile.getAbsolutePath())) {
            fos.write(fileContent);
        }
        return fileContent.length;
    }

    private String readLocalDirIndex(String path, String localBaseDir) throws IOException {
        File file = new File(new File(localBaseDir, path), DIRINDEX_FILENAME);
        return file.exists() ? new String(readFile(file)) : "";
    }

    private long getDirIndexAge(String path, String localBaseDir) {
        File file = new File(new File(localBaseDir, path), DIRINDEX_FILENAME);
        return file.exists() ? (System.currentTimeMillis() - file.lastModified()) : (Long.MAX_VALUE);
    }

    private void storeDirIndex(String path, String localBaseDir, String remoteDirIndex) throws IOException {
        File file = new File(new File(localBaseDir, path), DIRINDEX_FILENAME);
        writeFile(file, remoteDirIndex);
    }

    private static final char[] HEXARRAY = "0123456789abcdef".toCharArray();

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEXARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEXARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Calculates the SHA1 Hash for the given File
     */
    private byte[] calcSHA1(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (InputStream fis = Files.newInputStream(file.toPath())) {
            int n = 0;
            byte[] buffer = new byte[8192];
            while (n != -1) {
                n = fis.read(buffer);
                if (n > 0) {
                    digest.update(buffer, 0, n);
                }
            }
        }
        return digest.digest();
    }

    /**
     * Reads the given File.
     */
    private byte[] readFile(File file) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (InputStream fis = Files.newInputStream(file.toPath())) {
            int n = 0;
            byte[] buffer = new byte[8192];
            while (n != -1) {
                n = fis.read(buffer);
                if (n > 0) {
                    bos.write(buffer, 0, n);
                }
            }
        }
        return bos.toByteArray();
    }

    private void writeFile(File file, String remoteDirIndex) throws IOException {
        file.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(remoteDirIndex.getBytes());
        }
        log.finest(() -> "Written " + file.getAbsolutePath());
    }

    /**
     * Does the Async notification of the GUI
     */
    private void invokeLater(UPDATETYPE action, final int num) {
        if (num < 0) {
            log.warning(() -> "Update < 0 (" + action.name() + ")");
        }
        // invoke this on the Event Disp Thread
        SwingUtilities.invokeLater(() -> {
            switch (action) {

                case RESET: // reset progressBar
                    terraMaster.frame.butStop.setEnabled(false);
                    try {
                        Thread.sleep(1200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    terraMaster.frame.progressBar.setMaximum(0);
                    terraMaster.frame.progressBar.setVisible(false);
                    break;
                case UPDATE: // update progressBar
                    terraMaster.frame.progressUpdate(num);
                    break;
                case EXTEND: // progressBar maximum++
                    terraMaster.frame.progressBar.setMaximum(terraMaster.frame.progressBar.getMaximum() + num);
                    break;
                case START:
                    terraMaster.frame.progressBar.setMaximum(terraMaster.frame.progressBar.getMaximum() + syncList.size() * 2);
                    terraMaster.frame.progressBar.setVisible(true);
                    terraMaster.frame.butStop.setEnabled(true);
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public void restoreSettings() {
        maxAge = Long.parseLong(terraMaster.getProps().getProperty(TerraMasterProperties.MAX_TILE_AGE, "0"));

    }

    /**
     * 
     * @param map
     * @param i
     * @param rootType
     * @param type 
     */
    public void addScnMapTile(Map<TileName, TileData> map, File i, TerraSyncRootDirectoryType rootType, TerraSyncDirectoryType type) {
      
        TileName n = TileName.getTile(i.getName().replace(".zip", ""));
        TileData t = map.get(n);
        if (t == null) {
            // make a new TileData
            t = new TileData();
        }
        if (type.isTile()) {
            t.setDirTypePath(rootType, type, i);
        } else {
            throw new IllegalArgumentException("Models and Airports not supported");
        }
        map.put(n, t);
    }

    /** given a 10x10 dir, add the 1x1 tiles within to the HashMap*/
    void buildScnMap(File dir, Map<TileName, TileData> map, TerraSyncRootDirectoryType rootType, TerraSyncDirectoryType type) {
        File[] tiles = dir.listFiles();
        Pattern p = Pattern.compile(TileName.TILENAME_PATTERN + "(.zip)?");

        for (File f : tiles) {
            Matcher m = p.matcher(f.getName());
            if (m.matches()) {
                addScnMapTile(map, f, rootType, type);
            }
        }
    }

    /**
     * builds a HashMap of @link TerraSyncRootDirectoryType
     * @return a map of tiles to their state.
     */
    public Map<TileName, TileData> newScnMap() {
        List<TerraSyncDirectoryType> types = new ArrayList<>();
        for (TerraSyncDirectoryType type : TerraSyncDirectoryType.values()) {
            if (type.isTile()) {
                types.add(type);
            }
        }
        Pattern patt = Pattern.compile(TileName.TILENAME_PATTERN);
        Map<TileName, TileData> map = new HashMap<>(180 * 90);
        
        for (TerraSyncRootDirectoryType rootType : TerraSyncRootDirectoryType.values()) {
          String path = terraMaster.getProps().getProperty(rootType + "." + TerraMasterProperties.SCENERY_PATH);
          if (path == null) {
            continue;
          }
          for (TerraSyncDirectoryType terraSyncDirectoryType : types) {
              File d = new File(path + File.separator + terraSyncDirectoryType.getDirname());
              File[] list = d.listFiles();
              if (list != null) {
                  // list of 10x10 dirs
                  for (File f : list) {
                      Matcher m = patt.matcher(f.getName());
                      if (m.matches()) {
                          // now look inside this dir
                          buildScnMap(f, map, rootType, terraSyncDirectoryType);
                      }
                  }
              }
          }        
        }

        return map;
    }
}
