package org.flightgear.terramaster;

import java.io.File;

public class AirportsSync extends Syncable {

  private final String icaoPart;

  public AirportsSync(String path, String icaoPart) {
    super(path);
    this.icaoPart = icaoPart;
  }

  public AirportsSync(String path) {
    super(path);
    icaoPart = "";
  }

  @Override
  public String buildPath() {
    if (!icaoPart.isEmpty()) {
      return icaoPart.charAt(0) + File.separator + icaoPart.charAt(1);
    } else {
      return "";
    }
  }

  @Override
  public String getName() {
    return "Airports";
  }

  @Override
  public TerraSyncDirectoryType[] getTypes(final TerraSyncRootDirectoryType rootType) {
    return new TerraSyncDirectoryType[]{TerraSyncDirectoryType.AIRPORTS};
  }

}
