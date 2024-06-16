package org.flightgear.terramaster;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Async Webservice caller to query the mpmap03 airport search.
 *
 */

public class WebWorker extends SwingWorker<List<Airport>, Void> {
  Logger log = Logger.getLogger(TerraMaster.LOGGER_CATEGORY);
  Collection<TileName> selection;
  private List<Airport> result;

  JOBTYPES jobType;
  AirportResult callback;
  private String searchString;

  private enum JOBTYPES {
    SEARCH, BROWSE
  }

  /**
   * Search for Airport by String.
   * 
   * @param str
   */
  public WebWorker(String str, AirportResult callback) {
    jobType = JOBTYPES.SEARCH;
    searchString = str;
    this.callback = callback;
  }

  /**
   * Search for all airports in tiles.
   * 
   * @param list
   */
  public WebWorker(Collection<TileName> list, AirportResult callback) {
    jobType = JOBTYPES.BROWSE;
    selection = list;
    this.callback = callback;
  }

  @Override
  public List<Airport> doInBackground() {
    switch (jobType) {
    case SEARCH:
      result = search(searchString);
      break;
    case BROWSE:
      result = browse(selection);
      break;
    }
    return result;
  }

  @Override
  public void done() {
    callback.done();
  }

  public synchronized List<Airport> getResult() {
    return result;
  }

  /**
   * Perform the webquery. The result is parsed via XPath
   * 
   * @param url
   * @return
   */
  private List<Airport> webquery(URL url) {
    List<Airport> currentResult = new LinkedList<>();
    try {
      try (Scanner scanner = new Scanner(url.openStream(), StandardCharsets.UTF_8.toString())) {
        scanner.useDelimiter("\\A");
        String content = scanner.hasNext() ? scanner.next() : "";
        callback.clearLastResult();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;
        Document doc = null;
        builder = factory.newDocumentBuilder();
        doc = builder.parse(new InputSource(new StringReader(content)));

        // Create XPathFactory object
        XPathFactory xpathFactory = XPathFactory.newInstance();

        // Create XPath object
        XPath xpath = xpathFactory.newXPath();
        NodeList airportNodes = (NodeList) xpath.evaluate("/navaids/airport", doc, XPathConstants.NODESET);
        for (int i = 0; i < airportNodes.getLength(); i++) {
          Element n = (Element) airportNodes.item(i);

          Airport a = new Airport(n.getAttribute("code"), n.getAttribute("name"));
          // Find the center of all the runways
          NodeList runwayNodes = (NodeList) xpath.evaluate("runway", n, XPathConstants.NODESET);
          for (int j = 0; j < runwayNodes.getLength(); j++) {
            Element runwayNode = (Element) runwayNodes.item(j);
            a.updatePosition(runwayNode.getAttribute("lat"), runwayNode.getAttribute("lng"));
          }

          callback.addAirport(a);
        }
      }

    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      JOptionPane.showMessageDialog(callback.getMapFrame(), "Can't query Airports " + e.toString(), "Error",
          JOptionPane.ERROR_MESSAGE);
    }

    return currentResult;
  }

  /**
   * Search
   * 
   * @param str
   * @return
   */
  private List<Airport> search(String str) {
    try {
      str = URLEncoder.encode(str.trim(), "UTF-8");
      String url = String.format("https://mpmap02.flightgear.org/fg_nav_xml.cgi?sstr=%s&apt_code&apt_name", str);
      return webquery(new URL(url));
    } catch (MalformedURLException e) {
      log.log(Level.WARNING, e.toString(), e);
    } catch (UnsupportedEncodingException e) {
      log.log(Level.SEVERE, e.toString(), e);
    }
    return new ArrayList<>();
  }

  /**
   * Browse all airports in the tiles.
   * 
   * @param list
   * @return
   */

  private List<Airport> browse(Collection<TileName> list) {
    List<Airport> currentResult = new LinkedList<>();

    for (TileName t : list) {
      int lat = t.getLat();
      int lon = t.getLon();
      String sw = String.format("%d,%d", lat, lon);
      String ne = String.format("%d,%d", lat + 1, lon + 1);
      String url = String.format("https://mpmap02.flightgear.org/fg_nav_xml.cgi?ne=%s&sw=%s&apt_code", ne, sw);
      try {
        currentResult.addAll(webquery(new URL(url)));
        SwingUtilities.invokeLater(() -> callback.getMapFrame().repaint());
      } catch (MalformedURLException e) {
        log.severe(String.format("Error: Malformed URL: %s%n", url));
      }
    }
    return currentResult;
  }
}
