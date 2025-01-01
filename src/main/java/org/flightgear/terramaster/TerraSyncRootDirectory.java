
package org.flightgear.terramaster;

/**
 *
 * @author keith
 */
public class TerraSyncRootDirectory {

  /**
   * @return the terraSyncRootDirectoryType
   */
  public TerraSyncRootDirectoryType getTerraSyncRootDirectoryType() {
    return terraSyncRootDirectoryType;
  }

  /**
   * @return the types
   */
  public TerraSyncDirectoryType[] getTypes() {
    return types;
  }

  private final TerraSyncRootDirectoryType terraSyncRootDirectoryType;
  private final TerraSyncDirectoryType[] types;

  TerraSyncRootDirectory(TerraSyncRootDirectoryType terraSyncRootDirectoryType, TerraSyncDirectoryType[] types) {
    this.terraSyncRootDirectoryType = terraSyncRootDirectoryType;
    this.types = types;
  }
}
