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

import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.WayHelper;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * Inner class to fill the List of results with.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class WayPlace extends Place implements IWayPlace {

    /**
     * The way to return.
     */
    private Way myWay;

    /**
     * my map.
     */
    private IDataSet myMap;

    /**
     * ${@inheritDoc}.
     */
    public Node getResult() {
        List<Node> iter = myMap.getWayHelper().getNodes(myWay);

        if (iter.size() == 0)
            throw new IllegalStateException("the way has no nodes!");

        return iter.get(0);
    };

    /**
     * @param aWay The way to return. At least one of it's nodes must be in aMap.
     * @param aMap my map
     */
    public WayPlace(final Way aWay, final IDataSet aMap) {
        super(WayHelper.getTag(aWay, Tags.TAG_NAME),
                getRepresentingNode(aWay, aMap).getLatitude(),
                getRepresentingNode(aWay, aMap).getLongitude());
//will fail anyway        if (aWay == null)
      //will fail anyway            throw new IllegalStateException("null way given!");
        this.myWay = aWay;
        this.myMap = aMap;
    }

    /**
     * Find the first of the Nodes identified by the
     * waynodes of the way that is in the given map.
     * @param aWay the way to check
     * @param aMap the map to search
     * @return a node
     */
    private static Node getRepresentingNode(final Way aWay, final IDataSet aMap) {
        List<WayNode> wayNodes = aWay.getWayNodes();
        for (WayNode wayNode : wayNodes) {
            Node node = aMap.getNodeByID(wayNode.getNodeId());
            if (node != null) {
                return node;
            }
        }
        throw new IllegalArgumentException("NONE of the nodes of this way are contained in the given map");
    }

    /**
     * ${@inheritDoc}.
     */
    public Way getWay() {
        return myWay;
    }

}
