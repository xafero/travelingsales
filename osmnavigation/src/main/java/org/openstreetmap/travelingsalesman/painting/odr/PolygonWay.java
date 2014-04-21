/**
 * PolygonWay.java
 * created: 09.12.2007 07:23:00
 * This file is part of osmnavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 *
 *  osmnavigation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  osmnavigation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with osmnavigation.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.painting.odr;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;


import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.travelingsalesman.painting.ODRPaintVisitor;


/**
 * This is a Way that can paint closed polygons as areas by the
 * {@link ODRPaintVisitor}.
 * @author <a href="mailto:tdad@users.sourceforge.net">Sven Koehler</a>
 */
public class PolygonWay extends ODRWay {

    /**
     * @param way the way we are painting.
     * @param aWayType the type of my way.
     */
    public PolygonWay(final Way way, final ODR_WAY_TYPE aWayType) {
        super(way, aWayType);
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.travelingsalesman.painting.odr.ODRWay#getWayShape()
     */
    @Override
    public Shape getWayShape() {
        return getPolygon();
    }

    /**
     *  @return my {@link Polygon}
     */
    public Polygon getPolygon() {
        if (p == null) {
            p = buildPolygon();
        }

        return p;
    }

    /**
     *  @return the stuffed {@link Polygon}
     */
    private Polygon buildPolygon() {
        p = new Polygon();
        for (Point point : getWayPoints()) {
            p.addPoint(point.x, point.y);
        }

        return p;
    }

    /** the polygon itself. */
    private Polygon p = null;

}
