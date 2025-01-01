package org.flightgear.terramaster;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApacheDirectoryParser {
  private final static Logger log = Logger
      .getLogger(TerraMaster.LOGGER_CATEGORY + ApacheDirectoryParser.class.getName());

  private final static String HREF_REGEX = "href=\"([a-zA-Z0-9]*)\\/\"";

  public static String[] listDirectories(URL url) {
    try {
      ArrayList<String> ret = new ArrayList<>();
      String s = getFile(url);

      int tableStart = s.indexOf("<table>");
      int tableEnd = s.indexOf("</table>");
      if (tableStart > 0 && tableEnd > 0) {
        String[] tableString = s.substring(tableStart + 7, tableEnd).split("<tr>");
        Pattern p = Pattern.compile(HREF_REGEX);
        for (String line : tableString) {
          if (line.indexOf("[DIR]") > 0) {
            Matcher m = p.matcher(line);
            m.find();
            String group = m.group(1);
            ret.add(group);
          }
        }

        return ret.toArray(new String[] {});
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.getMessage(), e);
    }
    return new String[] {};
  }

  public static TerraSyncRootDirectory getType(URL url, String dirName) {
    try {
      URL url2 = new URL(url, dirName + "/.dirindex");
      String dirindex = getFile(url2);      
      if (dirindex.contains(TerraSyncDirectoryType.VPB.getDirname())) {
        return new TerraSyncRootDirectory(TerraSyncRootDirectoryType.WS30, getTypes(dirindex));
      } else if (dirindex.contains(TerraSyncDirectoryType.PYLONS.getDirname())) {
        return new TerraSyncRootDirectory(TerraSyncRootDirectoryType.OSM, getTypes(dirindex));
      } else if (dirindex.contains(TerraSyncDirectoryType.TERRAIN.getDirname())) {
        return new TerraSyncRootDirectory(TerraSyncRootDirectoryType.WS20, getTypes(dirindex));
      }
      return new TerraSyncRootDirectory(TerraSyncRootDirectoryType.OTHER, getTypes(dirindex));
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, e.getMessage(), e);
    }
    return null;
  }
  
  private static TerraSyncDirectoryType[] getTypes(String dirindex) {
    String[] lines = dirindex.split("\r?\n");
    ArrayList<TerraSyncDirectoryType> types = new ArrayList<>();
    for (String line : lines) {
      if(line.startsWith("d:")) {
        TerraSyncDirectoryType type = TerraSyncDirectoryType.valueOf(line.split(":")[1].toUpperCase());
        types.add(type);
      }
    }
    return types.toArray(new TerraSyncDirectoryType[0]);
  }

  private static String getFile(URL url) {
    try {
      log.finest(() -> "Downloading : " + url.toExternalForm());
      HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
      httpConn.setConnectTimeout(10000);
      httpConn.setReadTimeout(20000);
      int responseCode = (httpConn).getResponseCode();

      if (responseCode == HttpURLConnection.HTTP_OK) {
        String contentType = httpConn.getContentType();
        int contentLength = httpConn.getContentLength();

        log.finest(() -> "Content-Type = " + contentType);
        log.finest(() -> "Content-Length = " + contentLength);

        // opens input stream from the HTTP connection
        InputStream inputStream = httpConn.getInputStream();

        // opens an output stream to save into file
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        int bytesRead;
        byte[] buffer = new byte[1024];
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }

        String s = new String(outputStream.toByteArray());
        outputStream.close();
        inputStream.close();
        return s;
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }
}
