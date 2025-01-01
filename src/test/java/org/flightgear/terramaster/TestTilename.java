package org.flightgear.terramaster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * FIXME The coordinates are the switched
 * @author keith.paterson
 *
 */

public class TestTilename {

  @Test
  public void test1() {
    java.awt.geom.Point2D.Double d = new java.awt.geom.Point2D.Double(7.5, -50);
    int result = TileName.getTileIndex(d);
    assertEquals(3072770, result);
  }

  @Test
  public void test2() {
    java.awt.geom.Point2D.Double d = new java.awt.geom.Point2D.Double(-7.5, 50);
    int result = TileName.getTileIndex(d);
    assertEquals(2820610, result);
  }

  @Test
  public void test3() {
    java.awt.geom.Point2D.Double d = new java.awt.geom.Point2D.Double(-5.74110808, -57.2786546);
    int result = TileName.getTileIndex(d);
    assertEquals(2860241, result);
  }
  
  @Test
  public void test32() {
    java.awt.geom.Point2D.Double d = new java.awt.geom.Point2D.Double(17.402344, -86.846419);
    int result = TileName.getTileIndex(d);
    assertEquals(3238960, result);
  }

  @Test
  public void test4() {
    java.awt.geom.Point2D.Double d = new java.awt.geom.Point2D.Double(-7.5, 10);
    int result = TileName.getTileIndex(d);
    assertEquals(2823172, result);
  }
  
  @Test
  public void test5() {
    TileName tileName = new TileName("", 10, 3);
    assertEquals("e003n10", tileName.getName());
  }

  @Test
  public void test6() {
    TileName tileName = new TileName("", 10, 3).getNeighbour(1, 1);
    assertEquals("e004n11", tileName.getName());
  }

  @Test
  public void test7() {
    TileName tileName = new TileName("", 89, 3).getNeighbour(1, 1);
    assertEquals("e004n90", tileName.getName());
  }
  @Test
  public void test8() {
    TileName tileName = new TileName("", 89, 3).getNeighbour(1, 1).getNeighbour(-1, -1);
    assertEquals("e003n89", tileName.getName());
  }

  @Test
  public void testEquals() {
    TileName tileName1 = new TileName("", 89, 3).getNeighbour(1, -1);
    TileName tileName2 = new TileName("", 87, 5).getNeighbour(-1, 1);
    assertTrue(tileName1.equals(tileName2));
  }
}
