package org.flightgear.terramaster;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JProgressBar;

import org.flightgear.terramaster.dns.FlightgearNAPTRQuery;
import org.flightgear.terramaster.dns.WeightedUrl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TestHTTPTerraSync2 {

  @Mock
  TerraMaster tm;
  private JProgressBar mockProgress;
  private HTTPTerraSync ts;
  File scnDir = null;
  private Map<TileName, TileData> mapScenery = new HashMap<>();
  private Properties props = new Properties();
  

  @Before
  public void initMocks() throws IOException {
    MockitoAnnotations.initMocks(this);
    tm.frame = mock(MapFrame.class);
    mockProgress = mock(JProgressBar.class);
    tm.frame.progressBar = mockProgress;
    tm.frame.butStop = mock(JButton.class);
    doReturn(props ).when(tm).getProps();
    tm.getProps().setProperty(TerraMasterProperties.DNS_GOOGLE, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.DNS_GCA, Boolean.TRUE.toString());
    tm.getProps().setProperty(TerraMasterProperties.LOG_LEVEL, Level.ALL.toString());
    doReturn(mapScenery).when(tm).getMapScenery();
    tm.log =   Logger.getAnonymousLogger();
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
  public void testTar() throws InterruptedException, MalformedURLException {
    /*
    Collection<Syncable> m = new ArrayList<>();
    Collection<TileName> tl = new ArrayList<>();
    TileName t = new TileName("w009n70");
    t.setTypes(new TerraSyncDirectoryType[] { TerraSyncDirectoryType.OBJECTS });
    tl.add(t);
    ts.delete(tl);
    m.add(t);
    ts.flightgearNAPTRQuery = mock(FlightgearNAPTRQuery.class);
    WeightedUrl w = new WeightedUrl();
    w.setUrl(new URL("http://localhost:1181/"));
    ArrayList<WeightedUrl> urls = new ArrayList<>();
    urls.add(w);
    doReturn(urls).when(ts.flightgearNAPTRQuery).queryDNSServer("ws20");
    // when(ts.flightgearNAPTRQuery.queryDNSServer("ws20")).thenReturn(urls);
    ts.sync(m, true);
    // there must be exactly 1 call
    verify(tm.frame.progressBar, timeout(100000).times(1)).setVisible(true);
    verify(tm.frame.progressBar, timeout(100000).times(1)).setVisible(false);
    assertEquals(0, ts.getSyncList().size());
    ts.quit();
    await().atMost(10, TimeUnit.SECONDS).until(() -> ts.isAlive() == false);
    assertEquals(false, ts.isAlive());

    File f = new File(scnDir, "Objects/w010n70/w009n70/2811960.stg");
    assertEquals(true, f.exists());
    */
  }

  @Test
  public void testDirectory() throws InterruptedException, MalformedURLException {
    /* 
    Collection<Syncable> m = new ArrayList<>();
    Collection<TileName> tl = new ArrayList<>();
    TileName t = new TileName("w001n53");
    t.setTypes(new TerraSyncDirectoryType[] { TerraSyncDirectoryType.OBJECTS });
    tl.add(t);
    ts.delete(tl);
    m.add(t);
    ts.flightgearNAPTRQuery = mock(FlightgearNAPTRQuery.class);
    WeightedUrl w = new WeightedUrl();
    w.setUrl(new URL("http://localhost:1181/"));
    ArrayList<WeightedUrl> urls = new ArrayList<>();
    urls.add(w);
    // Disable DNS lookup
    doReturn(urls).when(ts.flightgearNAPTRQuery).queryDNSServer("ws20");
    ts.sync(m, true);
    // there must be exactly 1 call
    verify(tm.frame.progressBar, timeout(100000).times(1)).setVisible(true);
    verify(tm.frame.progressBar, timeout(100000).times(1)).setVisible(false);
    assertEquals(0, ts.getSyncList().size());
    File f = new File(scnDir, "Objects/w010n50/w001n53/2941888.stg");
    assertEquals(true, f.exists());
    ts.delete(tl);
    assertEquals(false, f.exists());
    
    ts.quit();
    await().atMost(10, TimeUnit.SECONDS).until(() -> ts.isAlive() == false);
    assertEquals(false, ts.isAlive());
    */
  }

  @Test
  public void testTarModels() throws InterruptedException, MalformedURLException {
    /* 
    Collection<Syncable> m = new ArrayList<>();
    m.add(new ModelsSync());
    ts.flightgearNAPTRQuery = mock(FlightgearNAPTRQuery.class);
    WeightedUrl w = new WeightedUrl();
    w.setUrl(new URL("http://localhost:1181/"));
    ArrayList<WeightedUrl> urls = new ArrayList<>();
    urls.add(w);
    doReturn(urls).when(ts.flightgearNAPTRQuery).queryDNSServer("ws20");
    ts.sync(m, true);
    // there must be exactly 1 call
    verify(tm.frame.progressBar, timeout(100000).times(1)).setVisible(true);
    verify(tm.frame.progressBar, timeout(100000).times(1)).setVisible(false);
    assertEquals(0, ts.getSyncList().size());
    ts.quit();
    await().atMost(10, TimeUnit.SECONDS).until(() -> ts.isAlive() == false);
    assertEquals(false, ts.isAlive());

    File f = new File(scnDir, "Models/Effects/taxilampblue.png");
    assertEquals(true, f.exists());
    */
  }

}
