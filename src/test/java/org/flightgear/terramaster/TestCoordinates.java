package org.flightgear.terramaster;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class TestCoordinates {

	@Test
	public void test() {
		double bearing = CoordinateCalculation.greatCircleBearing(10, 1, 10, 2);
		assertEquals(90, bearing, 1);
	}

	@Test
	public void test1() {
		double bearing = CoordinateCalculation.greatCircleBearing(10, 1, 10, -2);
		assertEquals(270, bearing, 1);
	}
	@Test
	public void test2() {
		double bearing = CoordinateCalculation.greatCircleBearing(10, 1, 11, 1);
		assertEquals(0, bearing, 1);
	}
	@Test
	public void test3() {
		double bearing = CoordinateCalculation.greatCircleBearing(10, 1, 9, 1);
		assertEquals(180, bearing, 1);
	}

	@Test
	public void test4() {
		double bearing = CoordinateCalculation.greatCircleBearing(40.777244, -73.872608, 51.423889, 12.236389);
		assertEquals(47.79, bearing, 1);
	}
	@Test
	public void test5() {
		double dist = CoordinateCalculation.greatCircleDistance(40.777244, -73.872608, 51.423889, 12.236389);
		assertEquals(6353.295, dist, 2);
	}
	
	@Test
	public void testFlightplan() {
	  List<TileName> allTiles = CoordinateCalculation.findAllTiles(55, 10.5, 55, 11.5);
	  assertEquals(2, allTiles.size());
	}

	@Test
  public void testFlightplan2() {
    List<TileName> allTiles = CoordinateCalculation.findAllTiles(-0.5, 10.5, +1.5, 10.5);
    assertEquals(2, allTiles.size());
  }

	@Test
  public void testFlightplan3() {
    List<TileName> allTiles = CoordinateCalculation.findAllTiles(-0.5, -179.5, -0.5, +179.5);
    assertEquals(2, allTiles.size());
  }
}
