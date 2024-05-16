package org.flightgear.terramaster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.logging.Logger;

/**
 * this class handles webqueries and returns results it keeps a per-session
 * HashMap of known airports It queries the multiplayer map.
 * {@link <a href="https://mpmap02.flightgear.org/fg_nav_xml_proxy.cgi?sstr=wbks&apt_code">mpmap02</a>}
 */

public class FGMap extends Observable implements AirportResult {
  Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);
  private HashMap<String, Airport> map;
  private List<Airport> searchResult = new ArrayList<>();
  private TerraMaster terraMaster;

  public FGMap(TerraMaster terraMaster) {
    this.terraMaster = terraMaster;
    map = new HashMap<>();
  }

  public void addAirport(Airport apt) {
    if (apt.code != null) {
      // first add the current airport to the HashMap
      map.put(apt.code, apt);

      // and to the current query's result
      searchResult.add(apt);
    }
  }

  public Map<String, Airport> getAirportMap() {
    return (HashMap<String, Airport>) map.clone();
  }

  public synchronized List<Airport> getSearchResult() {
    return searchResult;
  }

  public void clearAirports() {
    map.clear();
  }

  @Override
  public void done() {
    setChanged();
    notifyObservers();
    terraMaster.frame.repaint();
  }

  @Override
  public void clearLastResult() {
    searchResult.clear();
  }

  @Override
  public MapFrame getMapFrame() {
    return terraMaster.frame;
  }
}
