package org.flightgear.terramaster;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JProgressBar;

import org.flightgear.terramaster.dns.WeightedUrl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import net.sf.ivmaidns.dns.DNSName;

public class TestDownloadStatsDialog {
  @Mock
  TerraMaster tm;
  private JProgressBar mockProgress;
  private Properties props = new Properties();
  private Robot r = null;
  File scnDir = null;

  @Before
  public void initMocks() throws AWTException, IOException {
    MockitoAnnotations.initMocks(this);
    tm.frame = mock(MapFrame.class);
    mockProgress = mock(JProgressBar.class);
    tm.frame.progressBar = mockProgress;
    tm.frame.butStop = mock(JButton.class);
    tm.frame.map = mock(MapPanel.class);
    doReturn(props).when(tm).getProps();
    tm.getProps().setProperty(TerraMasterProperties.DNS_GOOGLE, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.DNS_GCA, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.LOG_LEVEL, Level.ALL.toString());
    scnDir = Files.createTempDirectory("").toFile();

    tm.getProps().setProperty(TerraMasterProperties.SCENERY_PATH, scnDir.getAbsolutePath());
    tm.log = Logger.getAnonymousLogger();
    r = new Robot();

    HTTPTerraSync ts = new HTTPTerraSync(tm);
    ts.start();
    
    doReturn(ts).when(tm).getTileService();
    
    
    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        scnDir.delete();
      }
    });

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        scnDir.delete();
      }
    });
  }

  @Test
  public void test() throws InterruptedException {
    HashMap<WeightedUrl, TileResult> stats = new HashMap<>();
    WeightedUrl url = new WeightedUrl("12", "!^.*$!https://ukmirror.flightgear.org/fgscenery!", new DNSName("\tterrasync\nflightgear\u0003org\u0000".getBytes(), 10));
    stats.put(url, new TileResult(url));
    DownloadResultDialog sd = new DownloadResultDialog(stats);
    sd.setBounds(0, 0, sd.getWidth(), sd.getHeight());
    (new Thread() {
      @Override
      public void run() {
        sd.setVisible(true);
      }
    }).start();
    r.setAutoDelay(200);
    mouseMove(130, 174);
    r.mousePress(InputEvent.BUTTON1_MASK);
    r.mouseRelease(InputEvent.BUTTON1_MASK);
    mouseMove(320, 348);
    r.mousePress(InputEvent.BUTTON1_MASK);
    r.mouseRelease(InputEvent.BUTTON1_MASK);
  }

  private void mouseMove(int x, int y) {
    for (int count = 0; (MouseInfo.getPointerInfo().getLocation().getX() != x
        || MouseInfo.getPointerInfo().getLocation().getY() != y) && count < 100; count++) {
      r.mouseMove(x, y);
    }
  }

}
