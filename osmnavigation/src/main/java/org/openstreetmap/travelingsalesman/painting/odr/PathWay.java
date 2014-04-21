/**
 * PathWay.java
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
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.util.Iterator;


import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.travelingsalesman.painting.ODRPaintVisitor;



/**
 * This is a way implementation which is able to draw non-closed ways.
 * it is used for highways, cycleways, railsways and stuff by the
 * {@link ODRPaintVisitor}.
 * @author <a href="mailto:tdad@users.sourceforge.net">Sven Koehler</a>
 */
public class PathWay extends ODRWay {

    /**
     * @param way the way we are painting.
     * @param wayType the type of the way
     */
    public PathWay(final Way way, final ODR_WAY_TYPE wayType) {
        super(way, wayType);
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.travelingsalesman.painting.odr.ODRWay#getWayShape()
     */
    @Override
    public Shape getWayShape() {
        return getPath();
    }

    /**
     * path getter.
     *
     * @return {@link GeneralPath}
     */
    public GeneralPath getPath() {
        if (path == null) {
            path = buildPath();
        }
        return path;
    }

    /**
     * builds the path for this way.
     *
     * @return {@link GeneralPath} representation of the way points.
     */
    private GeneralPath buildPath() {
        Iterator<Point> wayPointsIterator = getWayPoints().iterator();

        GeneralPath aPath = new GeneralPath();
        if (!wayPointsIterator.hasNext()) {
            return null;
        }
        Point start = wayPointsIterator.next();
        aPath.moveTo(start.x, start.y);
        while (wayPointsIterator.hasNext()) {
            Point next = wayPointsIterator.next();
            aPath.lineTo(next.x, next.y);
        }

        return aPath;
    }


    /** my path (maybe introduce some kind of caching for built paths). */
    private GeneralPath path = null;
}
