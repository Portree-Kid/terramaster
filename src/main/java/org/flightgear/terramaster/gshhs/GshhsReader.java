package org.flightgear.terramaster.gshhs;

import java.io.DataInput;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GshhsReader {
  public static final String LOGGER_CATEGORY = "org.flightgear";
  Logger log = Logger.getLogger(LOGGER_CATEGORY);

  
  private int readGshhsHeader(DataInput s, GshhsHeader h) {
    int fl;
    try {
      h.id = s.readInt();
      h.setNumPoints(s.readInt()); // npoints
      fl = s.readInt();
      h.setGreenwich((fl & 1 << 16) > 0 ? true : false);
      h.setLevel((byte) (fl & 0xff));
      h.setWest(s.readInt());
      h.setEast(s.readInt());
      h.setSouth(s.readInt());
      h.setNorth(s.readInt());
      h.area = s.readInt();
      h.areaFull = s.readInt();
      h.container = s.readInt();
      h.ancestor = s.readInt();
      return h.getNumPoints();
    } catch (Exception e) {
      return -1;
    }
  }

  /**
   * reads in GSHHS and builds ArrayList of polys
   */

  public ArrayList<MapPoly> newPolyList(String filename) {

    ArrayList<MapPoly> poly = new ArrayList<MapPoly>();

    try {
      DataInput s = new DataInputStream(
          // new FileInputStream(filename));
          getClass().getClassLoader().getResourceAsStream(filename));
      int n = 0;
      do {
        GshhsHeader h = new GshhsHeader();
        n = readGshhsHeader(s, h);
        if (n > 0)
          poly.add(new MapPoly(s, h));
      } while (n > 0);
    } catch (Exception e) {
      log.log(Level.SEVERE, filename, e);
    }

    // System.out.format("%s: %d polys\n", filename, poly.size());
    return poly;
  }

}
