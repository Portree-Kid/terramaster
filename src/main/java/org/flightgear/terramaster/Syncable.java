package org.flightgear.terramaster;

public abstract class Syncable {

  private String basePath;

  abstract String buildPath();

  abstract String getName();

  abstract TerraSyncDirectoryType[] getTypes(TerraSyncRootDirectoryType rootType); 

  public Syncable(String basePath) {
    this.basePath = basePath;
  }
  
  /**The path to be synced with.*/
  public String basePath() {
    return basePath;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

}
