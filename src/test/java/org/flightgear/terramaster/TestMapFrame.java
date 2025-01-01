package org.flightgear.terramaster;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JProgressBar;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestMapFrame {

  @Mock
  TerraMaster tm;
  private JProgressBar mockProgress;
  private FGMap fgmap = null;
  private Properties props = new Properties();
  private Map<TileName, TileData> mapScenery = new HashMap<>();
  private Robot r;
  private File scnPath = new File("C:\\Users\\keith.paterson\\Documents\\FlightGear\\TerraSync");

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
    tm.frame = mock(MapFrame.class);
    mockProgress = mock(JProgressBar.class);
    tm.frame.progressBar = mockProgress;
    tm.frame.butStop = mock(JButton.class);
    HTTPTerraSync sync = new HTTPTerraSync(tm);
    mapScenery = sync.newScnMap();
    sync.start();
    doReturn(sync).when(tm).getTileService();
    doReturn(props).when(tm).getProps();
    tm.getProps().setProperty(TerraMasterProperties.DNS_GOOGLE, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.DNS_GCA, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.LOG_LEVEL, Level.ALL.toString());
    tm.getProps().setProperty(TerraMasterProperties.SCENERY_PATH, scnPath.getAbsolutePath());
   
    fgmap = new FGMap(tm);
    
    fgmap.addAirport(new Airport("BLA", "BLABLA Airport"));
    doReturn(fgmap).when(tm).getFgmap();
    doReturn(mapScenery).when(tm).getMapScenery();
    tm.log = Logger.getAnonymousLogger();
  }

  @Test
  public void testSettings() {
    MapFrame mf = new MapFrame(tm, "");
    mf.setSize(100, 100);
    mf.storeSettings();
    mf.setSize(110, 110);
    mf.restoreSettings();
    assertEquals(100, mf.getSize().getWidth(), 0);
  }

  @Test
  public void testShowing() throws AWTException {
    MapFrame mf = new MapFrame(tm, "");
    try {
      mf.setSize(1000, 500);
      mf.setBounds(0, 0, mf.getWidth(), mf.getHeight());
      mf.setVisible(true);
      mf.map.setProjection(false);
      r = new Robot();
      r.setAutoDelay(300);
      mouseMove(400, 305);
      r.mousePress(InputEvent.BUTTON3_DOWN_MASK);
      r.delay(1000);
      mouseMove(315, 330);
      mouseMove(300, 300);
      mouseMove(350, 330);
      r.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
      r.mouseWheel(-14);
      mouseMove(490, 320);
      r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      mouseMove(520, 320);
      r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      Collection<TileName> selection = mf.map.getSelection();
      List<Syncable> synch = new ArrayList<>();
      synch.addAll(selection);
      assertEquals(2, selection.size());
      mouseMove(490, 40);
      r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      r.delay(2000);
      tm.getTileService().sync(synch, true);
      for (int i = 500; i < 540; i += 6) {
        mouseMove(i, 320);
      }
      for (int i = 540; i > 500; i -= 6) {
        mouseMove(i, 320);
      }
      mouseMove(520, 320);
      keyType(KeyEvent.VK_ADD, false);
      keyType(KeyEvent.VK_PLUS, false);
      keyType(KeyEvent.VK_MINUS, false);
      keyType(KeyEvent.VK_SUBTRACT, false);
      keyType(KeyEvent.VK_LEFT, false);
      keyType(KeyEvent.VK_RIGHT, false);
      keyType(KeyEvent.VK_UP, false);
      keyType(KeyEvent.VK_DOWN, false);
      keyType(KeyEvent.VK_RIGHT, true);
      assertEquals(2, mf.map.getSelection().size());
      r.delay(3000);
      mouseMove(100, 40);
      r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      r.delay(3000);
      mouseMove(180, 40);
      r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      r.delay(3000);
      mouseMove(430, 40);
      r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      r.delay(1000);
      r.mouseWheel(14);
      mouseMove(590, 40);
      r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      r.mousePress(InputEvent.BUTTON1_DOWN_MASK);
      r.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
      assertEquals(0, mf.map.getSelection().size());
      r.delay(2000);
    } finally {
      mf.setVisible(false);
    }
  }

  /**
   * @deprecated Use {@link #keyType(int,boolean)} instead
   */
  public void keyType(int keyCode) {
    keyType(keyCode, false);
  }

  public void keyType(int keyCode, boolean shifted) {
    if (shifted)
      r.keyPress(KeyEvent.VK_SHIFT);
    r.keyPress(keyCode);
    r.keyRelease(keyCode);
    if (shifted)
      r.keyRelease(KeyEvent.VK_SHIFT);
  }

  private void mouseMove(int x, int y) {
    for (int count = 0; (MouseInfo.getPointerInfo().getLocation().getX() != x
        || MouseInfo.getPointerInfo().getLocation().getY() != y) && count < 100; count++) {
      r.mouseMove(x, y);
    }
  }
}
