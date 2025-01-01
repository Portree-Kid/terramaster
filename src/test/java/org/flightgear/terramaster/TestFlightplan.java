package org.flightgear.terramaster;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestFlightplan {
  @Mock
  TerraMaster tm;
  private JProgressBar mockProgress;
  private HTTPTerraSync ts;
  File scnDir = null;
  private Map<TileName, TileData> mapScenery = new HashMap<>();
  private Properties props = new Properties();
  private Robot r = null;;

  @Before
  public void initMocks() throws IOException {
    MockitoAnnotations.initMocks(this);
    tm.frame = mock(MapFrame.class);
    mockProgress = mock(JProgressBar.class);
    tm.frame.progressBar = mockProgress;
    tm.frame.butStop = mock(JButton.class);
    doReturn(props).when(tm).getProps();
    tm.getProps().setProperty(TerraMasterProperties.DNS_GOOGLE, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.DNS_GCA, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.LOG_LEVEL, Level.ALL.toString());
    tm.frame.map = new MapPanel(tm);
    doReturn(mapScenery).when(tm).getMapScenery();
    System.out.println(tm.getMapScenery().getClass().getName());
    tm.log = Logger.getAnonymousLogger();
    scnDir = Files.createTempDirectory("").toFile();

    ts = new HTTPTerraSync(tm);
    ts.start();

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        scnDir.delete();
      }
    });
  }

  @Test
  public void test() throws AWTException {
    FlightPlan f = new FlightPlan(tm);
    f.setBounds(0, 0, f.getWidth(), f.getHeight());
    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        f.setVisible(true);
      }
    });

    r = new Robot();
    r.setAutoDelay(100);
    mouseMove(210, 54);
    r.mousePress(InputEvent.BUTTON1_MASK);
    r.mouseRelease(InputEvent.BUTTON1_MASK);
    enterText("Leipzig");
    mouseMove(410, 54);
    r.mousePress(InputEvent.BUTTON1_MASK);
    r.mouseRelease(InputEvent.BUTTON1_MASK);
    mouseMove(210, 90);
    r.mousePress(InputEvent.BUTTON1_MASK);
    r.mouseRelease(InputEvent.BUTTON1_MASK);
    enterText("EDDT");
    mouseMove(410, 90);
    r.mousePress(InputEvent.BUTTON1_MASK);
    r.mouseRelease(InputEvent.BUTTON1_MASK);
    mouseMove(310, 144);
    r.delay(2000);
    r.mousePress(InputEvent.BUTTON1_MASK);
    r.mouseRelease(InputEvent.BUTTON1_MASK);
    await().atMost(20, TimeUnit.SECONDS).until(() -> tm.frame.map.getSelection().size() == 3);
    for (TileName iterable_element : tm.frame.map.getSelection()) {
      System.out.println(iterable_element);
    }
  }

  private void enterText(String t) {
    for (int i = 0; i < t.length(); i++) {
      char c = t.charAt(i);
      if(Character.isUpperCase(c))
        r.keyPress(KeyEvent.VK_SHIFT);
      r.keyPress(Character.toUpperCase(c));
      r.keyRelease(Character.toUpperCase(c));
      if(Character.isUpperCase(c))
        r.keyRelease(KeyEvent.VK_SHIFT);
    }
  }

  private void mouseMove(int x, int y) {
    for (int count = 0; (MouseInfo.getPointerInfo().getLocation().getX() != x
        || MouseInfo.getPointerInfo().getLocation().getY() != y) && count < 100; count++) {
      r.mouseMove(x, y);
    }
  }

}
