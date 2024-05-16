package org.flightgear.terramaster;

import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The data associated with a tile
 * 
 * @author keith.paterson
 * @author Simon
 */

public class TileData {
  private final Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);
  /** The square drawn on the map. */
  public Polygon poly;
  /** Flags indicating what the tiles contain. Used for the mouse over. */
  private final HashSet<TerraSyncDirectoryTypes> directoryTypes;

  /** Map with the File objects for the directory types. */
  private final HashMap<TerraSyncDirectoryTypes, File> dirs;

  public TileData() {
    directoryTypes = new HashSet<>();
    dirs = new HashMap<>();
  }

  public void delete() {
    for (TerraSyncDirectoryTypes type : TerraSyncDirectoryTypes.values()) {
      if (hasDirectory(type)) {
        deltree(getDir(type));
      }
    }
  }

  private void deltree(File dir) {
    if (!dir.exists())
      return;
    for (File f : dir.listFiles()) {
      if (f.isDirectory())
        deltree(f);
      try {        
        Files.delete(f.toPath());
      } catch (SecurityException | IOException x) {
        log.log(Level.WARNING, "Deltree", x);
      }
    }
    try {
      Files.delete(dir.toPath());
    } catch (SecurityException | IOException x) {
      log.log(Level.WARNING, "Deltree", x);
    }

    // Delete compressed tar file
    File tarFile = new File(dir.getParent() + File.separator + dir.getName() + ".txz");
    if (tarFile.exists()) {
      try {
        Files.delete(tarFile.toPath());
      } catch (SecurityException | IOException x) {
        log.log(Level.WARNING, "Deltree", x);
      }
    }
  }

  public synchronized boolean hasDirectory(TerraSyncDirectoryTypes type) {
    return directoryTypes.contains(type);
  }

  public synchronized boolean hasAllDirs() {
    boolean hasAll = true;
    for (TerraSyncDirectoryTypes type : TerraSyncDirectoryTypes.values()) {
      if (type.isTile() && !hasDirectory(type)) {
        hasAll = false;
      }
    }
    return hasAll;
  }

  public void setDirTypePath(TerraSyncDirectoryTypes type, File file) {
    if (file != null && file.exists()) {
      directoryTypes.add(type);
      dirs.put(type, file);
    }
  }

  public File getDir(TerraSyncDirectoryTypes type) {
    return dirs.get(type);
  }
}
