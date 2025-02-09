package org.flightgear.terramaster;
// WinkelTriple, Azimuthal Orthographic (globe)

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.*;
import static org.flightgear.terramaster.MapFrame.LOG;

import org.flightgear.terramaster.dns.FlightgearNAPTRQuery;
import org.flightgear.terramaster.dns.FlightgearNAPTRQuery.HealthStats;
import org.flightgear.terramaster.dns.WeightedUrl;

/**
 * Main class
 *
 * @author keith.paterson
 * @author Simon
 */

public class TerraMaster {
  public static final String LOGGER_CATEGORY = "org.flightgear";
  Logger log = Logger.getLogger(LOGGER_CATEGORY);

  MapFrame frame;

  private Map<TileName, TileData> mapScenery;

  /** The service getting the tiles */
  private TileService tileService;

  private FGMap fgmap;
  private Properties props = new Properties();
  private final Logger staticLogger = Logger.getLogger(TerraMaster.class.getCanonicalName());

  public TerraMaster() {
    setFgmap(new FGMap(this)); // handles webqueries
  }

  void createAndShowGUI() {
    // find our jar
    java.net.URL url = getClass().getClassLoader().getResource("maps/gshhs_i.b");
    log.log(Level.FINE, "getResource: {0}", url);
    if (url == null) {
      JOptionPane.showMessageDialog(null, "Couldn't load resources", "ERROR", JOptionPane.ERROR_MESSAGE);
      return;
    }
    (new Thread() {
      @Override
      public void run() {
        setMapScenery(tileService.newScnMap());
        SwingUtilities.invokeLater(() -> {
          if (frame != null) {
            frame.repaint();
          }
          if (frame.isVisible() && getMapScenery() == null) {
            JOptionPane.showMessageDialog(frame,
                    "Scenery folder not found. Click the gear icon and select the folder containing your scenery files.",
                    "Warning", JOptionPane.WARNING_MESSAGE);
          } else if (frame.isVisible() && getMapScenery().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Scenery folder is empty.", "Warning", JOptionPane.WARNING_MESSAGE);
            LOG.warning("Scenery folder empty.");
          }
        });
      }      
    }).start();
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      log.log(Level.INFO, "Failed to load system look and feel: ", e);
    }  
    
    frame = new MapFrame(this, "TerraMaster " + getProps().getProperty("version"));
    frame.restoreSettings();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
    frame.toFront();
    new VersionChecker(this).run();
  }

  public static void main(String[] args) {

    SwingUtilities.invokeLater(() -> {
      try {
        InputStream resourceAsStream = TerraMaster.class.getClassLoader()
            .getResourceAsStream("terramaster.logging.properties");
        if (resourceAsStream != null) {
          LogManager.getLogManager().readConfiguration(resourceAsStream);
          Logger.getGlobal().info("Successfully configured logging");
        }
      } catch (SecurityException | IOException e1) {
        Logger.getGlobal().log(Level.WARNING, "Error loading Logger settings: ", e1);
      }
      TerraMaster tm = new TerraMaster();
      tm.loadVersion();
      tm.readMetaINF();
      tm.startUp();
      tm.setTileService();
      tm.initLoggers();

      tm.createAndShowGUI();
    });

  }

    /**
   * Migrate an old version of the settings
   */
  private void migrateSettings() {
    ArrayList<TerraSyncDirectoryType> enabledTypes = new ArrayList<>();
    
    for (TerraSyncDirectoryType value : TerraSyncDirectoryType.values()) {
      boolean enabled = Boolean.parseBoolean(getProps().getProperty(value.name(),"false"));
      if (enabled) {
        enabledTypes.add(value);
      }
      getProps().remove(value.name());
    }
    getProps().remove("SceneryVersion");
    
    String path = getProps().getProperty(TerraMasterProperties.SCENERY_PATH);
    for (String version : getProps().getProperty(TerraMasterProperties.SCENERY_VERSION,
            TerraMasterProperties.DEFAULT_SCENERY_VERSION).split(",")) {
      switch (version) {
        case "":
          {
            getProps().setProperty(TerraSyncRootDirectoryType.OSM + "." + TerraMasterProperties.SCENERY_PATH , path);      
            String enabledTypesString = String.join(",", enabledTypes.stream().filter((t) -> t.isInRoot(TerraSyncRootDirectoryType.OSM)).map(t -> t.name()).collect(Collectors.toList()));
            getProps().setProperty(TerraSyncRootDirectoryType.OSM + "." + TerraMasterProperties.ENABLED_DIRECTORIES , enabledTypesString);      
            getProps().setProperty(TerraSyncRootDirectoryType.OSM + "." + TerraMasterProperties.ENABLED , "true");                  
            
            getProps().setProperty(TerraSyncRootDirectoryType.WS20 + "." + TerraMasterProperties.SCENERY_PATH , path);      
            enabledTypesString = String.join(",", enabledTypes.stream().filter((t) -> t.isInRoot(TerraSyncRootDirectoryType.WS20)).map(t -> t.name()).collect(Collectors.toList()));
            getProps().setProperty(TerraSyncRootDirectoryType.WS20 + "." + TerraMasterProperties.ENABLED_DIRECTORIES , enabledTypesString);      
            getProps().setProperty(TerraSyncRootDirectoryType.WS20 + "." + TerraMasterProperties.ENABLED , "true");                  
          }
          break;
        case "ws2":
        case "ws20":
          {
            getProps().setProperty(TerraSyncRootDirectoryType.WS20 + "." + TerraMasterProperties.SCENERY_PATH , path);      
            String enabledTypesString = String.join(",", enabledTypes.stream().map(t -> t.name()).collect(Collectors.toList()));
            getProps().setProperty(TerraSyncRootDirectoryType.WS20 + "." + TerraMasterProperties.ENABLED_DIRECTORIES , enabledTypesString);      
            getProps().setProperty(TerraSyncRootDirectoryType.WS20 + "." + TerraMasterProperties.ENABLED , "true");                  
          }
          break;
        case "ws3":
        case "ws30":
          {
            getProps().setProperty(TerraSyncRootDirectoryType.WS30 + "." + TerraMasterProperties.SCENERY_PATH , path);      
            String enabledTypesString = String.join(",", enabledTypes.stream().map(t -> t.name()).collect(Collectors.toList()));
            getProps().setProperty(TerraSyncRootDirectoryType.WS30 + "." + TerraMasterProperties.ENABLED_DIRECTORIES , enabledTypesString);      
            getProps().setProperty(TerraSyncRootDirectoryType.WS30 + "." + TerraMasterProperties.ENABLED , "true");      
          }
          break;
        case "o2c":
          {
            getProps().setProperty(TerraSyncRootDirectoryType.OSM + "." + TerraMasterProperties.SCENERY_PATH , path);      
            String enabledTypesString = String.join(",", enabledTypes.stream().map(t -> t.name()).collect(Collectors.toList()));
            getProps().setProperty(TerraSyncRootDirectoryType.OSM + "." + TerraMasterProperties.ENABLED_DIRECTORIES , enabledTypesString);      
            getProps().setProperty(TerraSyncRootDirectoryType.OSM + "." + TerraMasterProperties.ENABLED , "true");      
          }
          break;
        default:
          log.log(Level.WARNING, "Wrong Version " + version);
      }
    }
    getProps().remove(TerraMasterProperties.SERVER_TYPE);
    getProps().remove(TerraMasterProperties.SCENERY_PATH);
  }

  protected void initLoggers() {
    if(!getProps().getProperty(TerraMasterProperties.LOG_LEVEL, "").isEmpty()) {      
      try {
        Level newLevel = Level.parse(getProps().getProperty(TerraMasterProperties.LOG_LEVEL));
        staticLogger.getParent().setLevel(newLevel);
      } catch (IllegalArgumentException | SecurityException e) {
        staticLogger.log(Level.WARNING, "Couldn't load properties : " + e, e);
      }
    }
  }

  protected void startUp() {
    try {
      getProps().load(new FileReader("terramaster.properties"));
      if (getProps().containsKey(TerraMasterProperties.SCENERY_PATH)) {
        migrateSettings();
      }
      
    } catch (IOException e) {
      staticLogger.log(Level.WARNING, "Couldn't load properties : " + e, e);
    }
    try {
      if (getProps().getProperty(TerraMasterProperties.LOG_LEVEL) != null) {
        Logger.getGlobal().getParent().setLevel(Level.INFO);
        Logger.getLogger(TerraMaster.LOGGER_CATEGORY)
            .setLevel(Level.parse(getProps().getProperty(TerraMasterProperties.LOG_LEVEL)));
        Logger.getGlobal().getParent().setLevel(Level.INFO);
      } else {
        Logger.getGlobal().getParent().setLevel(Level.INFO);
        Logger.getLogger(TerraMaster.LOGGER_CATEGORY).setLevel(Level.INFO);
        Logger.getGlobal().getParent().setLevel(Level.INFO);
      }
    } catch (Exception e) {
      staticLogger.log(Level.WARNING, "Couldn't set LOGGER : " + e, e);
    }
    staticLogger.info("Starting TerraMaster " + getProps().getProperty("version"));
  }

  private void readMetaINF() {
    try {
      Enumeration<URL> resources = TerraMaster.class.getClassLoader().getResources("META-INF/MANIFEST.MF");
      while (resources.hasMoreElements()) {
        readManifest(resources.nextElement());
      }
    } catch (IOException e) {
      staticLogger.log(Level.SEVERE, e.toString(), e);
    }
  }

  public void readManifest(URL resource) {
    try {
      Manifest manifest = new Manifest(resource.openStream());
      // check that this is your manifest and do what you need or
      // get the next one
      if ("TerraMasterLauncher".equals(manifest.getMainAttributes().getValue("Main-Class"))) {
        for (Entry<Object, Object> entry : manifest.getMainAttributes().entrySet()) {
          staticLogger.finest(entry.getKey() + "\t:\t" + entry.getValue());
        }
      }
    } catch (IOException e) {
      staticLogger.log(Level.WARNING, e.toString(), e);
    }
  }

  public void setTileService() {
    if (tileService == null) {
      tileService = new HTTPTerraSync(this);
      tileService.start();
    }
    tileService.restoreSettings();
  }

  public synchronized TileService getTileService() {
    return tileService;
  }

  public FGMap getFgmap() {
    return fgmap;
  }

  public void setFgmap(FGMap fgmap) {
    this.fgmap = fgmap;
  }

  public Properties getProps() {
    return props;
  }

  public void setProps(Properties props) {
    this.props = props;
  }

  private void loadVersion() {
    
    try (InputStream is = TerraMaster.class
        .getResourceAsStream("/META-INF/maven/org.flightgear/terramaster/pom.properties")) {
      getProps().remove("version");
      getProps().load(is);
    } catch (IOException e) {
      staticLogger.log(Level.WARNING, "Couldn't load properties : " + e, e);
    } catch (Exception e) {
      staticLogger.log(Level.WARNING, e.toString(), e);
    }
  }

  public Map<TileName, TileData> getMapScenery() {
    return mapScenery;
  }

  public void setMapScenery(Map<TileName, TileData> mapScenery) {
    this.mapScenery = mapScenery;
  }

  void showDnsStats(FlightgearNAPTRQuery flightgearNAPTRQuery) {
    int errors = 0;
    for (Entry<String, HealthStats> entry : flightgearNAPTRQuery.getStats().entrySet()) {
      HealthStats stats = entry.getValue();
      log.fine(stats::toString);
      errors += stats.errors;
    }
    if (errors > 0) {
      JOptionPane.showMessageDialog(null,
          "There where errors in DNS queries. Consider enabling 8.8.8.8 or 9.9.9.9 in settings", "DNS Error",
          JOptionPane.WARNING_MESSAGE);
    }
  }

  public void showStats(Map<WeightedUrl, TileResult> completeStats) {
    try {

      new DownloadResultDialog(completeStats).setVisible(true);
    } catch (Exception e) {
      log.log(Level.SEVERE, "Error showing stats ", e);
    }
  }

  public TerraSyncDirectoryType[] getSyncTypes(final TerraSyncRootDirectoryType rootType) {
    ArrayList<TerraSyncDirectoryType> types = new ArrayList<>();
    
    String[] enabledTypesString = getProps().getProperty(rootType + "." + TerraMasterProperties.ENABLED_DIRECTORIES, "").split(",");      
    if (enabledTypesString.length==0||enabledTypesString.length==1||enabledTypesString[0].isEmpty()) {
      return new TerraSyncDirectoryType[0];
    }
    TerraSyncDirectoryType[] enumConstants = Arrays.stream(enabledTypesString).map((t) -> TerraSyncDirectoryType.valueOf(t)).toArray(TerraSyncDirectoryType[]::new);
    return enumConstants;
  }
}
