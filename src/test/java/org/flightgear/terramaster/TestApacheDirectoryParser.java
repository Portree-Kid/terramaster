package org.flightgear.terramaster;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests the online parsing of the http://terramaster.flightgear.org server
 */

public class TestApacheDirectoryParser {

    private URL rootURL;

    @Before
    public void before() throws MalformedURLException {
        rootURL = new URL("http://terramaster.flightgear.org/terrasync/");
    }

    @Test
        public void testApacheDirectoryParser() throws MalformedURLException {
            String[] result = ApacheDirectoryParser.listDirectories(rootURL);
        assertNotEquals(0, result.length);
    }

    @Test
    public void testApacheDirectoryParserRoot() throws MalformedURLException {
        String[] result = ApacheDirectoryParser.listDirectories(rootURL);
        assertNotEquals(0, result.length);
        for (String string : result) {
          TerraSyncRootDirectory type = ApacheDirectoryParser.getType(rootURL, string);    
          assertNotNull(type);
          assertNotEquals(0, type.getTypes().length);
        }
    }
}
