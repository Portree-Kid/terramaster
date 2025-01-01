package org.flightgear.terramaster;

public class ModelsSync extends Syncable {

  public ModelsSync(String basePath) {
    super(basePath);
  }

  @Override
  public String buildPath() {
    return "";
  }

  @Override
  public String getName() {
    return "Models";
  }

  @Override
  public TerraSyncDirectoryType[] getTypes(final TerraSyncRootDirectoryType rootType) {
    return new TerraSyncDirectoryType[]{TerraSyncDirectoryType.MODELS};
  }

}
