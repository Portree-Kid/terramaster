package org.flightgear.terramaster;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;

import org.flightgear.terramaster.gshhs.MapPoly;

import com.jhlabs.map.proj.OrthographicAzimuthalProjection;
import com.jhlabs.map.proj.Projection;
import com.jhlabs.map.proj.WinkelTripelProjection;

/**
 * Panel painting the map
 *
 * @author keith.paterson
 * @author Simon
 */

public class MapPanel extends JPanel {

  private final class MapKeyAdapter extends KeyAdapter {
    private TileName selstart;

    @Override
    public void keyReleased(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
        selstart = null;
      }

    }

    @Override
    public void keyPressed(KeyEvent e) {
      if (e.getKeyCode() == KeyEvent.VK_SHIFT) {
        selstart = cursorTilename;
        selectionSet.clear();
      } else if (e.isShiftDown()) {
        keyEvent(e);
        boxSelection(selstart, cursorTilename);
        repaint();
      } else {
        keyEvent(e);
      }
    }

  }
  
  /**
   * Class holding a point and a distance to make it sortable.
   * @author keith.paterson
   */

  class SortablePoint implements Comparable<SortablePoint> {

    Point p;
    long d;

    SortablePoint(Point pt, long l) {
      p = pt;
      d = l;
    }

    public int compareTo(SortablePoint l) {
      return (int) (d - l.d);
    }
    
    public String toString() {
      return d + " " + p;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + Long.hashCode(d);
      result = prime * result + ((p == null) ? 0 : p.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof SortablePoint))
        return false;
      SortablePoint other = (SortablePoint) obj;
      if (d != other.d)
        return false;
      if (p == null) {
        return other.p == null;
      } else return p.equals(other.p);
    }

  }

  class SimpleMouseHandler extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
      TileName t = TileName.getTile(screen2geo(e.getPoint()));
      projectionLatitude = Math.toRadians(-t.getLat());
      projectionLongitude = Math.toRadians(t.getLon());
      setOrtho();
      mapFrame.repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
      int n = e.getWheelRotation();
      fromMetres -= n;
      pj.setFromMetres(Math.pow(2, fromMetres / 4));
      pj.initialize();
      mapFrame.repaint();
    }
  }

  class MouseHandler extends MouseAdapter {
    Point press, last;
    int mode = 0;

    @Override
    public void mousePressed(MouseEvent e) {
      press = e.getPoint();
      mode = e.getButton();
      requestFocus();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      switch (e.getButton()) {
      case MouseEvent.BUTTON3:
        mouseReleasedPanning(e);
        break;
      case MouseEvent.BUTTON1:
        mouseReleasedSelection(e);
        break;
      }
      enableButtons();
    }

    public void mouseReleasedPanning(MouseEvent e) {
      if (!e.getPoint().equals(press)) {
        mapFrame.repaint();
      }
      press = null;
    }

    public void mouseReleasedSelection(MouseEvent e) {
      last = null;
      dragbox = null;
      Point2D.Double d1 = screen2geo(press);
      Double d2 = screen2geo(e.getPoint());
      if (d1 == null || d2 == null)
        return;

      if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0)
        selectionSet.clear();

      int x1 = (int) Math.floor(d1.x);
      int y1 = (int) -Math.ceil(d1.y);
      int x2 = (int) Math.floor(d2.x);
      int y2 = (int) -Math.ceil(d2.y);
      int inc_i = (x2 > x1 ? 1 : -1);
      int inc_j = (y2 > y1 ? 1 : -1);

      // build a list of Points
      ArrayList<SortablePoint> l = new ArrayList<>();
      x2 += inc_i;
      y2 += inc_j;
      for (int i = x1; i != x2; i += inc_i) {
        for (int j = y1; j != y2; j += inc_j) {
          l.add(new SortablePoint(new Point(i, j), (long) (i - x1) * (i - x1) + (long) (j - y1) * (j - y1)));
        }
      }
      // sort by distance from x1,y1
      Object[] arr = l.toArray();
      Arrays.sort(arr);

      // finally, add the sorted list to selectionSet
      for (Object t : arr) {
        SortablePoint p = (SortablePoint) t;
        TileName n = TileName.getTile(p.p.x, p.p.y);
        if (!selectionSet.add(n))
          selectionSet.remove(n); // remove on reselect
      }

      mapFrame.repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
      switch (mode) {
      case MouseEvent.BUTTON3:
        mouseDraggedPanning(e);
        break;
      case MouseEvent.BUTTON1:
        mouseDraggedSelection(e);
        break;
      }
    }

    public void mouseDraggedPanning(MouseEvent e) {
      Point2D.Double d1 = screen2geo(press);
      Double d2 = screen2geo(e.getPoint());
      if (d1 == null || d2 == null)
        return;

      d2.x -= d1.x;
      d2.y -= d1.y;
      projectionLatitude -= Math.toRadians(d2.y);
      projectionLongitude -= Math.toRadians(d2.x);
      press = e.getPoint();
      pj.setProjectionLatitude(projectionLatitude);
      pj.setProjectionLongitude(projectionLongitude);
      pj.initialize();
      mapFrame.repaint();
    }

    public void mouseDraggedSelection(MouseEvent e) {
      last = e.getPoint();
      if ((e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) == 0)
        selectionSet.clear();
      boxSelection(screen2geo(press), screen2geo(last));
      mapFrame.repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      Point2D.Double p2 = screen2geo(e.getPoint());
      if (p2 == null)
        return;

      TileName tile = TileName.getTile(p2);
      cursorTilename = tile;
      String txt = tile.getName();
      mapFrame.tileName.setText(txt);

    }

    /**
     * Zoom
     */

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
      int n = e.getWheelRotation();
      fromMetres -= n;
      setFromMetres();
    }
  }

  class MPAdapter extends ComponentAdapter {
    @Override
    public void componentResized(ComponentEvent e) {
      double w = getWidth();
      double h = getHeight();
      double r = pj.getEquatorRadius();
      double i = Math.min(w, h); // the lesser dimension

      scale = i / r / 2;
      affine = new AffineTransform();
      affine.translate(w / 2, h / 2);
      affine.scale(scale, scale);
      mapFrame.repaint();
    }
  }

  private List<MapPoly> continents;  
  private List<MapPoly> borders;
  transient MouseAdapter mousehandler;

  double scale;
  MapFrame mapFrame;
  AffineTransform affine;
  boolean isWinkel = true;

  private transient Projection pj;
  private double projectionLatitude = -Math.toRadians(-30);
  private double projectionLongitude = Math.toRadians(145);
  private double mapRadius = HALFPI;
  private double fromMetres = 1;
  static final double HALFPI = Math.PI / 2;
  static final double TWOPI = Math.PI * 2.0;

  private final Collection<TileName> selectionSet = new LinkedHashSet<>();
  private int[] dragbox;
  private transient BufferedImage offScreen;
  private TileName cursorTilename;
  private final TerraMaster terraMaster;

  public MapPanel(TerraMaster terraMaster) {
    this.terraMaster = terraMaster;
    MPAdapter ad = new MPAdapter();
    addComponentListener(ad);

    continents = new ArrayList<>();
    setWinkel();

    setToolTipText("Hover for tile info");
    ToolTipManager tm = ToolTipManager.sharedInstance();
    tm.setDismissDelay(999999);
    tm.setInitialDelay(0);
    tm.setReshowDelay(0);
    addKeyListener(new MapKeyAdapter());
    setFocusable(true);
  }

  /**
   * Converts screen coordinates to lat/lon point
   */
  public Point2D.Double screen2geo(Point n) {
    if (n == null)
      return null;
    Point s = new Point(n);
    // s.y += getY();
    Point p = new Point();

    try {
      affine.createInverse().transform(s, p);
      Point2D.Double dp = new Point2D.Double(p.x, p.y), dd = new Point2D.Double();
      pj.inverseTransform(dp, dd);
      return dd;
    } catch (Exception x) {
      return null;
    }
  }

  private void setOrtho() {
    pj = new OrthographicAzimuthalProjection();
    mapRadius = HALFPI - 0.1;
    isWinkel = false;

    /*
     * depends on click projectionLatitude = -Math.toRadians(0); projectionLongitude
     * = Math.toRadians(0);
     */
    pj.setProjectionLatitude(projectionLatitude);
    pj.setProjectionLongitude(projectionLongitude);
    fromMetres = 1;
    pj.setFromMetres(Math.pow(2, fromMetres / 4));
    pj.initialize();

    double r = pj.getEquatorRadius();
    double w = getWidth();
    double h = getHeight();
    double i = (Math.min(h, w)); // the lesser dimension
    scale = i / r / 2;
    affine = new AffineTransform();
    affine.translate(w / 2, h / 2);
    affine.scale(scale, scale);

    removeMouseListener(mousehandler);
    mousehandler = new MouseHandler();
    addMouseWheelListener(mousehandler);
    addMouseListener(mousehandler);
    addMouseMotionListener(mousehandler);
  }

  private void setWinkel() {
    pj = new WinkelTripelProjection();
    // System.out.println(pj.getPROJ4Description());
    mapRadius = TWOPI;
    isWinkel = true;

    projectionLatitude = -Math.toRadians(0);
    projectionLongitude = Math.toRadians(0);
    pj.setProjectionLatitude(projectionLatitude);
    pj.setProjectionLongitude(projectionLongitude);
    fromMetres = -5;
    pj.setFromMetres(Math.pow(2, fromMetres / 4));
    pj.initialize();

    double r = pj.getEquatorRadius();
    double w = getWidth();
    double h = getHeight();
    double i = (Math.min(h, w)); // the lesser dimension
    scale = i / r / 2;
    affine = new AffineTransform();
    affine.translate(w / 2, h / 2);
    affine.scale(scale, scale);

    removeMouseWheelListener(mousehandler);
    removeMouseListener(mousehandler);
    removeMouseMotionListener(mousehandler);
    mousehandler = new SimpleMouseHandler();
    addMouseListener(mousehandler);

    clearSelection();
  }

  public void toggleProj() {
    if (isWinkel)
      setOrtho();
    else
      setWinkel();
  }

  public void setProjection(boolean winkel) {
    isWinkel = winkel;
    if (!isWinkel)
      setOrtho();
    else
      setWinkel();
  }

  void reset() {
    projectionLatitude = -Math.toRadians(-30);
    projectionLongitude = Math.toRadians(145);
    fromMetres = 1;
    pj.setProjectionLatitude(projectionLatitude);
    pj.setProjectionLongitude(projectionLongitude);
    pj.setFromMetres(Math.pow(2, fromMetres / 4));
    pj.initialize();
  }

  /**
   * Returns the tooltip for the tile.
   */

  @Override
  public String getToolTipText(MouseEvent e) {
    Point s = e.getPoint();
    StringBuilder txt = new StringBuilder();
    StringBuilder str = new StringBuilder();

    Double p2 = screen2geo(s);
    TileName t = TileName.getTile(p2);
    if (t != null)
      txt = new StringBuilder(t.getName());

    // Is it downloaded?
    if (terraMaster.getMapScenery().containsKey(t)) {
      // list Terr, Obj, airports

      TileData d = terraMaster.getMapScenery().get(t);
      txt.insert(0, "<html>");

      for (TerraSyncDirectoryTypes type : TerraSyncDirectoryTypes.values()) {
        if (d.hasDirectory(type)) {
          txt.append(" +").append(type.getAbbreviation());

          if (type == TerraSyncDirectoryTypes.TERRAIN) {
            File f = d.getDir(TerraSyncDirectoryTypes.TERRAIN);
            if (f != null && f.exists()) {
              int count = 0;
              for (String i : f.list()) {
                if (i.endsWith(".btg.gz")) {
                  int n = i.indexOf('.');
                  if (n > 4)
                    n = 4;
                  i = i.substring(0, n);
                  try {
                    Short.parseShort(i);
                  } catch (Exception x) {
                    str.append(i).append(" ");
                    if ((++count % 4) == 0)
                      str.append("<br>");
                  }
                }
              }
            }
          }
        }
      }

      if (str.length() > 0)
        txt.append("<br>").append(str);

      txt.append("</html>");
      mapFrame.tileindex.setText(t.getName() + " (" + TileName.getTileIndex(p2) + ")");
    }

    return txt.toString();
  }

  public int polyCount() {
    return continents.size();
  }

  /**
   * selection stuff
   */

  private void boxSelection(TileName selstart2, TileName cursor) {
    Point2D.Double p1 = new Double(selstart2.getLon(), -selstart2.getLat());
    Double p2 = new Double(cursor.getLon(), -cursor.getLat());
    boxSelection(p1, p2);
  }

  /**
   * capture all 1x1 boxes between press and last (to be drawn by paint() later)
   */
  private void boxSelection(Point2D.Double p1, Point2D.Double p2) {
    if (p1 == null || p2 == null) {
      dragbox = null;
      return;
    }

    dragbox = new int[4];
    dragbox[0] = (int) Math.floor(p1.x);
    dragbox[1] = (int) -Math.ceil(p1.y);
    dragbox[2] = (int) Math.floor(p2.x);
    dragbox[3] = (int) -Math.ceil(p2.y);
  }

  public void setSelection(Collection<TileName> selectionSet) {
    this.selectionSet.clear();
    this.selectionSet.addAll(selectionSet);
  }

  /**
   * returns union of selectionSet + dragbox
   */

  Collection<TileName> getSelection() {
    Collection<TileName> selSet = new LinkedHashSet<>(selectionSet);

    if (dragbox != null) {
      int l = dragbox[0];
      int b = dragbox[1];
      int r = dragbox[2];
      int t = dragbox[3];
      int inc_i = (r > l ? 1 : -1), inc_j = (t > b ? 1 : -1);
      r += inc_i;
      t += inc_j;
      for (int i = l; i != r; i += inc_i) {
        for (int j = b; j != t; j += inc_j) {
          TileName n = TileName.getTile(i, j);
          if (!selSet.add(n))
            selSet.remove(n); // remove on reselect
        }
      }
    }
    return selSet;
  }

  void clearSelection() {
    selectionSet.clear();
    if (mapFrame != null) {
      mapFrame.butSync.setEnabled(false);
      mapFrame.butDelete.setEnabled(false);
      mapFrame.butSearch.setEnabled(false);
    }
  }

  /**
   * Paints the current selection
   */

  void showSelection(Graphics g) {
    Collection<TileName> a = getSelection();
    if (a == null)
      return;

    g.setColor(Color.red);
    for (TileName t : a) {
      Polygon p = getBox(t.getLon(), t.getLat());
      if (p != null)
        g.drawPolygon(p);
    }
  }

  void showSyncList(Graphics g) {
    if (terraMaster.getTileService() == null)
      return;
    Collection<Syncable> a = new ArrayList<>(terraMaster.getTileService().getSyncList());
    g.setColor(Color.cyan);
    for (Syncable s : a) {
      if (s instanceof TileName) {
        TileName t = (TileName) s;
        Polygon p = getBox(t.getLon(), t.getLat());
        if (p != null)
          g.drawPolygon(p);        
      }
    }
  }

  private void enableButtons() {
    boolean b = !selectionSet.isEmpty();
    mapFrame.butSync.setEnabled(b);
    mapFrame.butDelete.setEnabled(b);
    mapFrame.butSearch.setEnabled(b);
  }

  void drawGraticule(Graphics g, int sp) {
    int x, y;
    Point2D.Double p = new Point2D.Double();
    double l;
    double r;
    double t;
    double b;
    int[] x4 = new int[4];
    int[] y4 = new int[4];

    x = -180;
    while (x < 180) {
      y = -70;
      while (y < 90) {
        l = Math.toRadians(x);
        b = Math.toRadians(y);
        r = Math.toRadians((double) x + sp);
        t = Math.toRadians((double) y - sp);

        if (inside(l, b)) {
          project(l, b, p);
          x4[0] = (int) p.x;
          y4[0] = (int) p.y;
          project(r, b, p);
          x4[1] = (int) p.x;
          y4[1] = (int) p.y;
          project(r, t, p);
          x4[2] = (int) p.x;
          y4[2] = (int) p.y;
          project(l, t, p);
          x4[3] = (int) p.x;
          y4[3] = (int) p.y;
          g.drawPolygon(x4, y4, 4);
        }

        y += sp;
      }
      x += sp;
    }
  }

  /**
   * Returns a box that is paintable w and s are negative
   */
  Polygon getBox(int x, int y) {
    double l;
    double r;
    double t;
    double b;
    int[] x4 = new int[4];
    int[] y4 = new int[4];
    Point2D.Double p = new Point2D.Double();

    
    double inc = 1;
    l = Math.toRadians(x);
    b = Math.toRadians(-y);
    r = Math.toRadians((double) x + inc);
    t = Math.toRadians((double) -y - inc);

    if (!inside(l, b)) {
      return null;
    }

    project(l, b, p);
    x4[0] = (int) p.x;
    y4[0] = (int) p.y;
    project(r, b, p);
    x4[1] = (int) p.x;
    y4[1] = (int) p.y;
    project(r, t, p);
    x4[2] = (int) p.x;
    y4[2] = (int) p.y;
    project(l, t, p);
    x4[3] = (int) p.x;
    y4[3] = (int) p.y;
    return new Polygon(x4, y4, 4);
  }

  /**
   * Shows the downloaded tiles
   */

  void showTiles(Graphics g0) {
    Graphics2D g = (Graphics2D) g0;

    g.setBackground(Color.black);
    g.setColor(Color.gray);
    drawGraticule(g, 10);

    if (terraMaster.getMapScenery() == null)
      return;
    Set<TileName> keys = terraMaster.getMapScenery().keySet();
    Pattern p = Pattern.compile("([ew])(\\d{3})([ns])(\\d{2})");

    for (TileName n : keys) {
      if (n == null)
        continue;
      Matcher m = p.matcher(n.getName());
      if (m.matches()) {
        int lon = Integer.parseInt(m.group(2));
        int lat = Integer.parseInt(m.group(4));
        lon = m.group(1).equals("w") ? -lon : lon;
        lat = m.group(3).equals("s") ? -lat : lat;

        Polygon poly = getBox(lon, lat);
        TileData t = terraMaster.getMapScenery().get(n);
        t.poly = poly;
        if (poly != null) {
          if (t.hasAllDirs())
            g.setColor(Color.green);
          else
            g.setColor(Color.yellow);
          g.drawPolygon(poly);
        }
      }
    }
  }

  /**
   * Shows the airports on the map
   */
  void showAirports(Graphics g0) {
    if (terraMaster.getFgmap() == null)
      return;
    Graphics2D g = (Graphics2D) g0.create();
    Map<String, Airport> apts = terraMaster.getFgmap().getAirportMap();
    Point2D.Double p = new Point2D.Double();
    Point p2 = new Point();

    g.setColor(Color.white);
    g.setBackground(Color.white);
    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
    g.setTransform(new AffineTransform()); // restore identity

    // we perform manual transform for shape drawing
    // (because text transformation is screwed up)

    for (Airport a : apts.values()) {
      double x = Math.toRadians(a.lon);
      double y = Math.toRadians(-a.lat);
      int n = (int) fromMetres * 2 - 16; // the circle size changes with
      // zoom
      if (n < 2)
        n = 2;
      if (inside(x, y)) {
        project(x, y, p);
        affine.transform(p, p2);
        g.drawOval(p2.x - n / 2, p2.y - n / 2, n, n);
        g.drawString(a.code, p2.x - 12, p2.y + n);
      }
    }
  }

  /**
   * draws the landmass. filter by polygon size and zoom
   */
  void showLandmass(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    Color sea = new Color(0, 0, 64);
    Color land = new Color(64, 128, 0);
    Color border = new Color(128, 192, 128);
    Rectangle r = g2.getClipBounds();

    g2.setColor(land);
    g2.setBackground(sea);
    g2.clearRect(r.x, r.y, r.width, r.height);
    g2.setTransform(affine);
    for (MapPoly s : continents) {
      if (s.getNumPoints() > 20 / Math.pow(2, fromMetres / 4)) {
        MapPoly d = convertPoly(s);
        g2.setColor(s.level % 2 == 1 ? land : sea);
        if (d.npoints != 0)
          g2.fillPolygon(d);
      }
    }
    // borders
    g2.setColor(border);
    if (borders != null) {
      for (MapPoly s : borders) {
        int[] xp = new int[s.npoints], yp = new int[s.npoints];
        int n = convertPolyline(s, xp, yp);
        if (n != 0)
          g2.drawPolyline(xp, yp, n);
      }
    }
  }

  // in: MapPoly
  // out: transformed new MapPoly
  MapPoly convertPoly(MapPoly s) {
    int i;
    Point2D.Double p = new Point2D.Double();
    MapPoly d = new MapPoly();

    for (i = 0; i < s.npoints; ++i) {
      double x = s.xpoints[i], y = s.ypoints[i];
      x = Math.toRadians(x / 100.0);
      y = Math.toRadians(y / 100.0);
      if (inside(x, y)) {
        project(x, y, p);
        d.addPoint((int) p.x, (int) p.y);
      }
    }
    return d;
  }

  // in: MapPoly
  // out: npoints
  int convertPolyline(MapPoly s, int[] xpoints, int[] ypoints) {
    Point2D.Double p = new Point2D.Double();

    int i, j = 0;
    for (i = 0; i < s.npoints; ++i) {
      double x = s.xpoints[i], y = s.ypoints[i];
      x = Math.toRadians(x / 100.0);
      y = Math.toRadians(y / 100.0);
      if (inside(x, y)) {
        project(x, y, p);
        xpoints[j] = (int) p.x;
        ypoints[j] = (int) p.y;
        ++j;
      }
    }
    return j;
  }

  /**
   * Projects the given point on the globe.
   */

  void project(double lam, double phi, Point2D.Double d) {
    Point2D.Double s = new Point2D.Double(lam, phi);
    pj.transformRadians(s, d);
  }

  boolean inside(double lon, double lat) {
    return CoordinateCalculation.oldHaversine(lat, lon, projectionLatitude, projectionLongitude) < mapRadius;
  }

  void passFrame(MapFrame f) {
    mapFrame = f;
  }

  void passPolys(List<MapPoly> p) {
    continents = p;
  }

  void passBorders(List<MapPoly> p) {
    borders = p;
  }

  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (offScreen == null) {
      return;
    }
    Graphics graphics = offScreen.getGraphics();
    graphics.setClip(0, 0, getWidth(), getHeight());
    showLandmass(graphics);
    showTiles(graphics);
    showSelection(graphics);
    showSyncList(graphics);
    showAirports(graphics);

    // crosshair
    {
      Graphics2D g2 = (Graphics2D) graphics;
      g2.setTransform(new AffineTransform());
      g2.setColor(Color.white);
      g2.drawLine(getWidth() / 2 - 50, getHeight() / 2, getWidth() / 2 + 50, getHeight() / 2);
      g2.drawLine(getWidth() / 2, getHeight() / 2 - 50, getWidth() / 2, getHeight() / 2 + 50);
    }
    // Draw double buffered Image
    g.drawImage(offScreen, 0, 0, this);
  }

  @Override
  public void setSize(int width, int height) {
    super.setSize(width, height);
    offScreen = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    offScreen.getGraphics().setClip(0, 0, width, height);
  }

  public void setFromMetres() {
    pj.setFromMetres(Math.pow(2, fromMetres / 4));
    pj.initialize();
    mapFrame.repaint();
  }

  public void keyEvent(KeyEvent e) {
    if (cursorTilename == null)
      return;
    TileName newSelection = null;
    switch (e.getKeyCode()) {
    case KeyEvent.VK_LEFT:
      newSelection = cursorTilename.getNeighbour(-1, 0);
      break;
    case KeyEvent.VK_RIGHT:
      newSelection = cursorTilename.getNeighbour(1, 0);
      break;
    case KeyEvent.VK_UP:
      newSelection = cursorTilename.getNeighbour(0, 1);
      break;
    case KeyEvent.VK_DOWN:
      newSelection = cursorTilename.getNeighbour(0, -1);
      break;
    case KeyEvent.VK_ADD:
    case KeyEvent.VK_PLUS:
      fromMetres += 1;
      setFromMetres();
      return;
    case KeyEvent.VK_SUBTRACT:
    case KeyEvent.VK_MINUS:
      fromMetres -= 1;
      setFromMetres();
      return;
    default:
      break;
    }
    if (newSelection != null) {
      if (!e.isShiftDown()) {
        dragbox = null;
        selectionSet.clear();
        selectionSet.add(newSelection);
      }
      cursorTilename = newSelection;
    }
    repaint();

  }

  public void restoreSettings() {
    projectionLatitude = java.lang.Double
        .parseDouble(terraMaster.getProps().getProperty(TerraMasterProperties.PROJECTION_LAT, "0"));
    projectionLongitude = java.lang.Double
        .parseDouble(terraMaster.getProps().getProperty(TerraMasterProperties.PROJECTION_LON, "0"));
    setProjection(Boolean.parseBoolean(terraMaster.getProps().getProperty(TerraMasterProperties.PROJECTION, "false")));
    fromMetres = java.lang.Double.parseDouble(terraMaster.getProps().getProperty(TerraMasterProperties.FROM_METRES, "1"));
    setFromMetres();
  }

  public void setProjection(double lat, double lon) {
    projectionLatitude = -Math.toRadians(lat);

    projectionLongitude = Math.toRadians(lon);
    pj.setProjectionLatitude(projectionLatitude);
    pj.setProjectionLongitude(projectionLongitude);
    pj.initialize();
  }

  public void storeSettings() {
    terraMaster.getProps().setProperty(TerraMasterProperties.PROJECTION_LAT, java.lang.Double.toString(projectionLatitude));
    terraMaster.getProps().setProperty(TerraMasterProperties.PROJECTION_LON, java.lang.Double.toString(projectionLongitude));
    terraMaster.getProps().setProperty(TerraMasterProperties.FROM_METRES, java.lang.Double.toString(fromMetres));
  }
}
