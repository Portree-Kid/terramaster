package org.flightgear.terramaster;

import java.io.File;

public class AirportsSync implements Syncable {

  private final String icaoPart;

  public AirportsSync(String icaoPart) {
    this.icaoPart = icaoPart;
  }

  public AirportsSync() {
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
  public TerraSyncDirectoryType[] getTypes() {
    return new TerraSyncDirectoryType[]{TerraSyncDirectoryType.AIRPORTS};
  }

}
