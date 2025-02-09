package org.flightgear.terramaster;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import org.flightgear.terramaster.VersionChecker.Version;

import com.jayway.jsonpath.JsonPath;
import java.util.Objects;

public class VersionChecker extends SwingWorker<Version, Object>{
  Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);
  private final TerraMaster terramaster;
  
  public VersionChecker(TerraMaster tm) {
    terramaster = tm;
  }

  public List<Version> getVersionList() {
    InputStream is;
    try {
      is = new URL("https://api.github.com/repos/Portree-Kid/terramaster/releases").openStream();
      try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.toString())) {
        scanner.useDelimiter("\\A");
        String jsonText = scanner.hasNext() ? scanner.next() : "";
        List<String> names = JsonPath.read(jsonText, "$..tag_name");
        List<Version> ret = names.stream().map(x -> new Version(x.replaceAll("terramaster-", ""))).collect(Collectors.toList());
        Collections.sort(ret);
        return ret;
      } catch (Exception e) {
        log.log(Level.SEVERE, "Couldn't get Version", e);
      }
    } catch (IOException e1) {
        log.log(Level.SEVERE, "Couldn't get Version", e1);
    }
    return null;
  }
  
  public Version getMaxVersion() {
    List<Version> l = getVersionList();
    Collections.reverse(l);
    return l.get(0);
  }

  public class Version implements Comparable<Version> {

    @Override
    public String toString() {
      return "Version [version=" + version + "]";
    }

    private String version;

    public final String get() {
      return this.version;
    }

    public Version(String version) {
      if (version == null)
        throw new IllegalArgumentException("Version can not be null");
      if (!version.matches("[0-9]+(\\.[0-9]+)*"))
        throw new IllegalArgumentException("Invalid version format");
      this.version = version;
    }

    @Override
    public int compareTo(Version that) {
      if (that == null)
        return 1;
      String[] thisParts = this.get().split("\\.");
      String[] thatParts = that.get().split("\\.");
      int length = Math.max(thisParts.length, thatParts.length);
      for (int i = 0; i < length; i++) {
        int thisPart = i < thisParts.length ? Integer.parseInt(thisParts[i]) : 0;
        int thatPart = i < thatParts.length ? Integer.parseInt(thatParts[i]) : 0;
        if (thisPart < thatPart) {
          return -1;
        }
        if (thisPart > thatPart) {
          return 1;
        }
      }
      return 0;
    }

    @Override
    public boolean equals(Object that) {
      if (this == that)
        return true;
      if (that == null)
        return false;
      if (this.getClass() != that.getClass())
        return false;
      return this.compareTo((Version) that) == 0;
    }

    @Override
    public int hashCode() {
      int hash = 5;
      hash = 97 * hash + Objects.hashCode(this.version);
      return hash;
    }

  }

  @Override
  protected Version doInBackground() throws Exception {

    Version v = getMaxVersion();
    Version localVersion = loadVersion();
    boolean update = v.compareTo(localVersion)>0;
    log.log(Level.INFO, "Remote {0} Local {1}", new Object[]{v, localVersion});
    if(update) {
      JOptionPane.showMessageDialog(null, "Version " + v.version + " available", "Version", JOptionPane.INFORMATION_MESSAGE, null);
    }
    return null;
  }
  
  private Version loadVersion() {
    try {

      Properties props = new Properties();
      props.put("version","-.-.-");
      try (InputStream is = getClass()
          .getResourceAsStream("/META-INF/maven/org.flightgear/terramaster/pom.properties")) {
        props.load(is);
      }
      String ret = props.getProperty("version").replaceAll("-SNAPSHOT", "");
      log.log(Level.INFO, "Current local version " + ret);
      // build.minor.number=10
      // build.major.number=1
      terramaster.getProps().put("version", ret);
      return new Version(ret);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    return null;
  }

}
