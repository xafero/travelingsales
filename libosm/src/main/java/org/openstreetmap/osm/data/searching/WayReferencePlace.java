/**
 * This file is part of LibOSM by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  LibOSM is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LibOSM is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LibOSM.  If not, see <http://www.gnu.org/licenses/>.
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

import java.util.List;

import org.openstreetmap.osm.data.IDataSet;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * Inner class to fill the List of results with.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class WayReferencePlace extends Place {

    /**
     * The id of the way found.
     */
    private long wayID;

    /**
     * The map we are searching on.
     */
    private IDataSet myMap;

    /**.
     * This method will throw an IllegalStateException if the map does not cover this node.
     * @return The first node of the way.
     */
    public Node getResult() {
        IDataSet map = myMap;
        if (map == null)
            throw new IllegalStateException("my PlaceFinderPanel has a null map!");

        Way way = map.getWaysByID(this.wayID);
        if (way == null)
            throw new IllegalStateException("no way found for id " + this.wayID + "!");

        List<Node> iter = myMap.getWayHelper().getNodes(way);
        if (iter.size() == 0)
            throw new IllegalStateException("the way with id " + this.wayID + " has no nodes!");

        return iter.get(0);
    };

    /**
     * @param aDisplayName The name that shall be displayed.
     * @param aWayID the id of the way to return.
     * @param aMap the map to search for the id in.
     * @param lon the lonitude
     * @param lat the latitude
     */
    public WayReferencePlace(final IDataSet aMap, final String aDisplayName, final long aWayID, final double lat, final double lon) {
        super(aDisplayName, lat, lon);
        this.myMap = aMap;
        this.wayID = aWayID;
    }

}
