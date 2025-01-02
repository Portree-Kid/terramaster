package org.flightgear.terramaster;

public class NavDataSync extends Syncable {

  public NavDataSync(String path) {
    super(path);
  }

  @Override
  public String buildPath() {
    return "";
  }

  @Override
  public String getName() {
    return "Navdata";
  }

  @Override
  public TerraSyncDirectoryType[] getTypes(final TerraSyncRootDirectoryType rootType) {
    return new TerraSyncDirectoryType[]{TerraSyncDirectoryType.NAVDATA};
  }

}
