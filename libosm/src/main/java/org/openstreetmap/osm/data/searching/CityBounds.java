/**
 * CityBounds.java
 * created: 18.11.2007 21:42:30
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
package org.openstreetmap.osm.data.searching;

import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.coordinates.PolygonBounds;

/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * CityBounds.java<br/>
 * created: 18.11.2007 21:42:30 <br/>
 *<br/><br/>
 * The bounds of a city. This adds a name-property to PolygonBounds
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class CityBounds extends PolygonBounds {
    /**
     * The name of the city.
     */
    private String name;

    /**
     * @param pName The name of the city.
     */
    public CityBounds(final String pName) {
        super();
        this.name = pName;
    }

    /**
     * @return The name of the city.
     * @see #name
     */
    public String getName() {
        return this.name;
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public String toString() {
        LatLon min = getMin();
        LatLon max = getMax();
        return ("CityBounds('" + getName() + "' "
        + "size=" + getSize() + " center=" + getCenter()
        + "'[" + min.lat() + "x" + min.lon() + "]"
        + "-[" + max.lat() + "x" + max.lon() + "])");
    }
}
