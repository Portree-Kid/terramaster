package org.flightgear.terramaster;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.awt.AWTException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

public class TestHTTPTerraSync {

  @Mock
  TerraMaster tm;
  private JProgressBar mockProgress;
  private HTTPTerraSync ts;
  private Map<TileName, TileData> mapScenery = new HashMap<>();
  private Properties props = new Properties();
  private File scnPath = new File("C:\\Users\\keith.paterson\\Documents\\FlightGear\\TerraSync");

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
    doReturn(mapScenery).when(tm).getMapScenery();
    tm.log = Logger.getAnonymousLogger();

    ts = new HTTPTerraSync(tm);
    ts.start();
    ts.setScnPath(scnPath);
  }

  @Test
  public void testModels() throws InterruptedException {
    File f = new File(scnPath, "Models/Aircraft/a310-tnt.xml");
    
    Collection<Syncable> m = new ArrayList<>();
    m.add(new ModelsSync());
    ts.sync(m, true);
    verify(tm.frame.progressBar, timeout(400000).times(1)).setVisible(true);
    verify(tm.frame.progressBar, timeout(400000).times(1)).setVisible(false);
    assertEquals(0, ts.getSyncList().size());
    ts.quit();
    Thread.sleep(1000);
    assertEquals(false, ts.isAlive());
    assertEquals(true, f.exists());
  }

  @Test
  public void testModelsCancel() throws InterruptedException, AWTException {
    Collection<Syncable> m = new ArrayList<>();
    m.add(new ModelsSync());
    ts.sync(m, true);
    verify(tm.frame.progressBar, timeout(400000).times(1)).setVisible(true);
    ts.cancel();
    verify(tm.frame.progressBar, timeout(400000).times(1)).setVisible(false);
    assertEquals(0, ts.getSyncList().size());
    ts.quit();
    Thread.sleep(1000);
    assertEquals(false, ts.isAlive());
  }

  @Test
  public void testTile() throws InterruptedException {
    Collection<Syncable> m = new ArrayList<>();
    Collection<TileName> tl = new ArrayList<>();
    TileName t = new TileName("w006n56");
    t.setTypes(new TerraSyncDirectoryType[] { TerraSyncDirectoryType.BUILDINGS, TerraSyncDirectoryType.OBJECTS,
        TerraSyncDirectoryType.ROADS, TerraSyncDirectoryType.TERRAIN, TerraSyncDirectoryType.PYLONS });
    tl.add(t);
    ts.delete(tl);
    m.add(t);
    ts.sync(m, true);
    // there must be exactly 1 call
    verify(tm.frame.progressBar, timeout(400000).times(1)).setVisible(true);
    verify(tm.frame.progressBar, timeout(300000).times(1)).setVisible(false);
    assertEquals(0, ts.getSyncList().size());
    ts.quit();
    Thread.sleep(1000);
    assertEquals(false, ts.isAlive());

    File f = new File(scnPath, "Terrain/w010n50/w006n56/2860184.stg");
    assertEquals("2860 must exist", true, f.exists());
  }
}
