package org.flightgear.terramaster;

import java.util.Arrays;

/**
 * The subdirectories that TerraMaster supports in the directory
 *
 * @author keith.paterson
 * @author Simon
 */
public enum TerraSyncDirectoryType {

  TERRAIN("Terrain", true, new TerraSyncRootDirectoryType[]{TerraSyncRootDirectoryType.WS20, TerraSyncRootDirectoryType.WS30}, "Terr"),
  VPB("vpb", true, new TerraSyncRootDirectoryType[]{TerraSyncRootDirectoryType.WS30}, "VPB"),
  OBJECTS("Objects", true, new TerraSyncRootDirectoryType[]{TerraSyncRootDirectoryType.WS20, TerraSyncRootDirectoryType.WS30}, "Obj"),
  MODELS("Models", false, new TerraSyncRootDirectoryType[0], ""),
  NAVDATA("Navdata", false, new TerraSyncRootDirectoryType[]{TerraSyncRootDirectoryType.WS20, TerraSyncRootDirectoryType.WS30}, ""),
  AIRPORTS("Airports", false, new TerraSyncRootDirectoryType[]{TerraSyncRootDirectoryType.WS20, TerraSyncRootDirectoryType.WS30}, ""),
  BUILDINGS("Buildings", true, new TerraSyncRootDirectoryType[]{TerraSyncRootDirectoryType.OSM}, "Bui"),
  PYLONS("Pylons", true, new TerraSyncRootDirectoryType[]{TerraSyncRootDirectoryType.OSM}, "Py"),
  ROADS("Roads", true, new TerraSyncRootDirectoryType[]{TerraSyncRootDirectoryType.OSM}, "Rd"),
  DETAILS("Details", true, new TerraSyncRootDirectoryType[]{TerraSyncRootDirectoryType.OSM}, "Det"),
  TREES("Trees", true, new TerraSyncRootDirectoryType[]{TerraSyncRootDirectoryType.OSM}, "Trs");

  private final String name;
  private final boolean tile;
  private final String abbreviation;
  private TerraSyncRootDirectoryType[] rootTypes;

  /**
   * 
   * @param name
   * @param tile true if it is a tiled type
   * @param rootTypes ref
   * @param abbreviation shown in the tool tip 
   */
  TerraSyncDirectoryType(String name, boolean tile, TerraSyncRootDirectoryType[] rootTypes, String abbreviation) {
    this.name = name;
    this.tile = tile;
    this.rootTypes = rootTypes;
    this.abbreviation = abbreviation;
  }

  public String getDirname() {
    return name;
  }

  public String getAbbreviation() {
    return abbreviation;
  }

  boolean isInRoot(TerraSyncRootDirectoryType rootType) {
    return Arrays.stream(rootTypes).filter(t -> t.equals(rootType)).count()==1;
  }
  
  public TerraSyncRootDirectoryType[] getRootTypes() {
    return rootTypes;
  }

  public synchronized boolean isTile() {
    return tile;
  }

  
}
