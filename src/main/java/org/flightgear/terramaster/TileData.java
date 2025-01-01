package org.flightgear.terramaster;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The data associated with a tile. Supports multiple @link TerraSy
 * 
 * @author keith.paterson
 * @author Simon
 */


public class TileData {

  /**
   * Compound key
   */
  private class TileDataKey {
    TerraSyncRootDirectoryType rootType;
    TerraSyncDirectoryType dirType;

    public TileDataKey(TerraSyncRootDirectoryType rootType, TerraSyncDirectoryType dirType) {
      this.rootType = rootType;
      this.dirType = dirType;
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 13 * hash + Objects.hashCode(this.rootType);
      hash = 13 * hash + Objects.hashCode(this.dirType);
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
      final TileDataKey other = (TileDataKey) obj;
      if (this.rootType != other.rootType) {
        return false;
      }
      return this.dirType == other.dirType;
    }

  }
  
  private static final Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);
  /** Flags indicating what the tiles contain. Used for the mouse over. */
  private final HashSet<TileDataKey> directoryTypes;

  /** Map with the File objects for the directory types. */
  private final HashMap<TileDataKey, File> dirs;

  public TileData() {
    directoryTypes = new HashSet<>();
    dirs = new HashMap<>();
  }

  /**
   * Delete a complete tile.
   */
  public void delete() {
    for (TerraSyncRootDirectoryType rootType : TerraSyncRootDirectoryType.values()) {
      for (TerraSyncDirectoryType type : TerraSyncDirectoryType.values()) {
        if (hasDirectory(rootType, type)) {
          deltree(getDir(rootType, type));
        }
      }
    }
  }

  private void deltree(File dir) {
    if (!dir.exists())
      return;
    if (dir.isDirectory()) {
      for (File f : dir.listFiles()) {
        if (f.isDirectory())
          deltree(f);
        try {        
          Files.delete(f.toPath());
        } catch (SecurityException | IOException x) {
          log.log(Level.WARNING, "Deltree", x);
        }
      }
    }
    try {
      Files.delete(dir.toPath());
    } catch (SecurityException | IOException x) {
      log.log(Level.WARNING, "Deltree", x);
    }

    File parent = dir.getParentFile();
    for (File packedFile : parent.listFiles((pathname) -> (pathname.getName().startsWith(dir.getName())||dir.isFile()))) {
      if (packedFile.exists()) {
        try {
          Files.delete(packedFile.toPath());
        } catch (SecurityException | IOException x) {
          log.log(Level.WARNING, "Deltree", x);
        }
      }      
    }
  }

  public synchronized boolean hasDirectory(final TerraSyncRootDirectoryType rootType, TerraSyncDirectoryType type) {
    return directoryTypes.contains(new TileDataKey(rootType, type));
  }

  public synchronized boolean hasDirectory(final TerraSyncDirectoryType type) {
    boolean ret = false;
    for (TerraSyncRootDirectoryType rootType : type.getRootTypes()) {
      ret |= directoryTypes.contains(new TileDataKey(rootType, type));
    }
    return ret;
  }

  public synchronized boolean hasAllDirTypes(final TerraSyncRootDirectoryType rootType, final TerraSyncDirectoryType[] dirTypes) {
    boolean hasAll = true;
    for (TerraSyncDirectoryType type : dirTypes) {
        if (type.isTile() && !hasDirectory(rootType, type)) {
          hasAll = false;
        }
    }
    return hasAll;
  }

  public void setDirTypePath(final TerraSyncRootDirectoryType rootType, TerraSyncDirectoryType type, File file) {
    if (file != null && file.exists()) {
      directoryTypes.add(new TileDataKey( rootType, type));
      dirs.put(new TileDataKey(rootType, type), file);
    }
  }

  public File getDir(final TerraSyncRootDirectoryType rootType, final TerraSyncDirectoryType type) {
    return dirs.get(new TileDataKey(rootType, type));
  }
}
