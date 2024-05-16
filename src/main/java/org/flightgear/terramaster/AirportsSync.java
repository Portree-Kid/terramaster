package org.flightgear.terramaster;

import java.io.File;

public class AirportsSync implements Syncable {

  private String icaoPart;

  public AirportsSync(String icaoPart) {
    this.icaoPart = icaoPart;
  }

  @Override
  public String buildPath() {
    return icaoPart.charAt(0) + File.separator + icaoPart.charAt(1);
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
