package org.flightgear.terramaster;

/**
 * The subdirectories that TerraMaster supports in the directory
 *
 * @author keith.paterson
 * @author Simon
 */

public enum TerraSyncDirectoryType {

  TERRAIN("Terrain", true, false, "Terr"),
  OBJECTS("Objects", true, false, "Obj"),
  MODELS("Models", false, false, ""),
  AIRPORTS("Airports", false, false, ""),
  BUILDINGS("Buildings", true, true, "Bui"),
  PYLONS("Pylons", true, true, "Py"),
  ROADS("Roads", true, true, "Rd"),
  DETAILS("Details", true, true, "Det"),
  TREES("Trees", true, true, "Trs");

  private final String name;
  private final boolean tile;
  private final boolean osm;
  private final String abbreviation;
  
  public synchronized boolean isTile() {
    return tile;
  }

  public synchronized boolean isOsm() {
    return osm;
  }

  TerraSyncDirectoryType(String name, boolean tile, boolean osm, String abbreviation) {
    this.name = name;
    this.tile = tile;
    this.osm = osm;
    this.abbreviation = abbreviation;
  }

  public String getDirname() {
    return name + "/";
  }

  public String getAbbreviation() {
    return abbreviation;
  }
}
