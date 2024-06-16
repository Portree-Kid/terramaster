package org.flightgear.terramaster;

public class ModelsSync implements Syncable {

  @Override
  public String buildPath() {
    return "";
  }

  @Override
  public String getName() {
    return "Models";
  }

  @Override
  public TerraSyncDirectoryType[] getTypes() {
    return new TerraSyncDirectoryType[]{TerraSyncDirectoryType.MODELS};
  }

}
