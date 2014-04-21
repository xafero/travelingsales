/**
 * TileCalculatorTest.java
 * created: 18.11.2007 13:09:45
 * (c) 2007 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of libosm by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  libosm is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  libosm is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with libosm.  If not, see <http://www.gnu.org/licenses/>.
 *
 ***********************************
 * Editing this file:
 *  -For consistent code-quality this file should be checked with the
 *   checkstyle-ruleset enclosed in this project.
 *  -After the design of this file has settled it should get it's own
 *   JUnit-Test that shall be executed regularly. It is best to write
 *   the test-case BEFORE writing this class and to run it on every build
 *   as a regression-test.
 */
package org.openstreetmap.osm.data;


import org.openstreetmap.osm.data.TileCalculator.XyPair;
import org.openstreetmap.osm.data.coordinates.LatLon;

import junit.framework.TestCase;


/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * TileCalculatorTest.java<br/>
 * created: 18.11.2007 13:09:45 <br/>
 *<br/><br/>
 * Do some simple sanity-tests to the {@link TileCalculator}.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class TileCalculatorTest extends TestCase {


    /**
     * The tile-number for lat=lon=0 .
     */
    private static final long NULLTILE = 3221225472L;

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "TileCalculatorTest@" + hashCode();
    }

    /**
     * Test method for {@link TileCalculator#calculateTile(double, double)}.
     */
    public void testCalculateTile() {
        TileCalculator calc = new TileCalculator();
        double lat = 0d;
        double lon = 0d;

        long tile = calc.calculateTile(lat, lon);
        assertEquals(NULLTILE, tile);
        long tile2 = calc.calculateTile(new LatLon(lat, lon));
        assertEquals(tile, tile2);
    }

    /**
     * Test method for {@link TileCalculator#calculateXyPairFromTileNumber(long)}.
     */
    public void testCalculateXyPairFromTileNumber() {
        TileCalculator calc = new TileCalculator();
        double lat = 0d;
        double lon = 0d;
        long tile = calc.calculateTile(lat, lon);

        // does xy.getTile() give the same tile?
        XyPair xy = calc.calculateXyPairFromTileNumber(tile);
        assertEquals(tile, xy.getTile());

        // see if the tile changes if we change x
        xy.setXNumber(xy.getXNumber() + 1);
        assertTrue(tile != xy.getTile());
        xy.setXNumber(xy.getXNumber() - 1);
        assertEquals(tile, xy.getTile());

        // see if the tile changes if we change y
        xy.setYNumber(xy.getYNumber() + 1);
        assertTrue(tile != xy.getTile());
        xy.setYNumber(xy.getYNumber() - 1);
        assertEquals(tile, xy.getTile());

        // switch x and y
        int x = xy.getXNumber();
        int y = xy.getYNumber();
        xy.setYNumber(x);
        xy.setXNumber(y);
        assertTrue(tile == xy.getTile());
    }
}
