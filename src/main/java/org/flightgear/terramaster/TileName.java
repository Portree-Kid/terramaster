package org.flightgear.terramaster;

import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.Serializable;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing a tile of scenery in the "e000s00" format and convenience
 * methods for converting between lat,lon and "e000s00" formats.
 */

public class TileName implements Comparable<TileName>, Serializable, Syncable {
  private int lat;
  private int lon;
  /** String representing this tile. */
  private String name;
  
  /**The types being synced. Not part of HashCode/Equals!*/
  private TerraSyncDirectoryType[] types;

  private static HashMap<String, TileName> tilenameMap;

  // creates a hashtable of all possible 1x1 tiles in the world
  static {
    tilenameMap = new HashMap<>();

    for (int x = -180; x < 180; ++x) {
      for (int y = -90; y < 90; ++y) {
        TileName t = new TileName(y, x);
        tilenameMap.put(t.getName(), t);
      }
    }
  }

  public TileName(int lat, int lon) {
    this.lat = lat;
    this.lon = lon;
    name = computeTileName(lat, lon);
    types = new TerraSyncDirectoryType[] {TerraSyncDirectoryType.TERRAIN};
  }

  public TileName(String name) {
    this.name = name;
    types = new TerraSyncDirectoryType[] {TerraSyncDirectoryType.TERRAIN};
    Pattern p = Pattern.compile("([ew])(\\p{Digit}{3})([ns])(\\p{Digit}{2})");
    Matcher m = p.matcher(name);
    if (m.matches()) {
      int lon = Integer.parseInt(m.group(2));
      int lat = Integer.parseInt(m.group(4));
      this.lon = m.group(1).equals("w") ? -lon : lon;
      this.lat = m.group(3).equals("s") ? -lat : lat;
    } else
      lat = lon = 0;
  }

  public int compareTo(TileName l) {
    return name.compareTo(l.getName());
  }

  public String getName() {
    return name;
  }

  public int getLat() {
    return lat;
  }

  public int getLon() {
    return lon;
  }

  // W and S are negative
  public static String computeTileName(Point2D.Double p) {
    if (p == null)
      return "";
    return computeTileName((int) -Math.ceil(p.y), (int) Math.floor(p.x));
  }

  /** W and S are negative */
  public static String computeTileName(int lat, int lon) {
    char ew = 'e';
    char ns = 'n';

    if (lon < 0) {
      lon = -lon;
      ew = 'w';
    }
    if (lat < 0) {
      lat = -lat;
      ns = 's';
    }
    // XXX check sanity
    return String.format("%c%03d%c%02d", ew, lon, ns, lat);
  }

  /**
   * Returns ICAO code from "ICAO.btg.gz", or null
   */
  
  public static String getAirportCode(String n) {
    Pattern p = Pattern.compile("([A-Z0-9]{1,4}).btg.gz");
    Matcher m = p.matcher(n);
    if (m.matches()) {
      return m.group(1);
    }
    return null;
  }

  public static TileName getTile(String n) {
    return tilenameMap.get(n);
  }

  public static TileName getTile(int x, int y) {
    if( y == 90)
      return new TileName(y, x);
    return tilenameMap.get(computeTileName(y, x));
  }

  public static TileName getTile(Point2D.Double p) {
    return tilenameMap.get(computeTileName(p));
  }

  /**
   * given a 1x1 tile, figure out the parent 10x10 container return the 10/1 path
   */
  public String buildPath() {
    if (name.length() < 7)
      return null;

    // XXX throw an exception
    int lon = Integer.parseInt(name.substring(1, 4));
    int lat = Integer.parseInt(name.substring(5));
    char ew = name.charAt(0);
    char ns = name.charAt(4);

    int modlon = lon % 10;
    lon -= ew == 'w' && modlon != 0 ? modlon - 10 : modlon;

    int modlat = lat % 10;
    lat -= ns == 's' && modlat != 0 ? modlat - 10 : modlat;

    return String.format("%s%03d%s%02d/%s", ew, lon, ns, lat, name);
  }

  public TileName getNeighbour(int i, int j) {
    TileName tile = TileName.getTile(lon + i, lat + j);
    return tile;
  }
  
  /**
   * Returns a TileIndex for the given lat/lon
   * @see <a href="http://wiki.flightgear.org/Tile_Index_Scheme">http://wiki.flightgear.org/Tile_Index_Scheme</a>
   * @param p (x lon, y lat)
   */

  public static int getTileIndex(Point2D.Double p) {
    double lon = p.x;
    //FIXME Lats are always with the wrong sign. Negative is towards North
    double lat = -p.y; 
    double baseY = Math.floor(lat);
    int y = (int)((lat - baseY) * 8);
    double tileWidth = getTileWidth(p);
    double base_x = Math.floor(Math.floor(lon / tileWidth) * tileWidth);
    if (base_x < -180)
      base_x = -180;
    double x = Math.floor((lon - base_x) / tileWidth);
    int index =  ((int)Math.floor(lon)+180)<<14;
    index +=    ((int)Math.floor(lat) + 90) << 6 ; 
    index += (y << 3); 
    index += x;
    return index;
  }
  
  /**
   * Get the amount of a tile is covered by one tileindex.
   */

  public static double getTileWidth(Double p) {
    if (Math.abs(p.y) < 22)
      return 0.125;
    if (Math.abs(p.y) < 62)
      return 0.25;
    if (Math.abs(p.y) < 76)
      return 0.5;
    if (Math.abs(p.y) < 83)
      return 1;
    if (Math.abs(p.y) < 86)
      return 2;
    if (Math.abs(p.y) < 88)
      return 4;
    if (Math.abs(p.y) < 89)
      return 8;
    if (Math.abs(p.y) <= 90)
      return 360;
    return 0.125;
  }

  @Override
  public String toString() {
    return getName();
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + lat;
    result = prime * result + lon;
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof TileName))
      return false;
    TileName other = (TileName) obj;
    if (lat != other.lat)
      return false;
    if (lon != other.lon)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  public void setTypes(TerraSyncDirectoryType[] t) {
    this.types = t;
  }

  @Override
  public TerraSyncDirectoryType[] getTypes() {
    return types;
  }
  
  
}
