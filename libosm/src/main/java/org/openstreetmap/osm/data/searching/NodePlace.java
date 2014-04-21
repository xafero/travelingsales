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

import java.util.Iterator;

import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.WayHelper;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * This is a place that consists of only a single {@link Node}.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class NodePlace extends Place {

    /**
     * The node to return.
     */
    private Node myNode;

    /**
     * my map.
     */
    private IDataSet myMap;

    /**
     * @return Returns the node.
     * @see #myNode
     */
    public Node getNode() {
        return myNode;
    }

    /**
     * @return Returns the DataSet.
     */
    public IDataSet getMap() {
        return myMap;
    }
    
    /**
     * @return The node to return.
     */
    public Node getResult() {
        return myNode;
    };

    /**
     * Find a name to display.
     * @param aNode the node
     * @param aMap the map
     * @return the name, never null or empty
     */
    private static String getName(final Node aNode, final IDataSet aMap) {
        String name = WayHelper.getTag(aNode, Tags.TAG_NAME);
        if (name == null || name.trim().length() == 0) {
            final Iterator<Way> ways = aMap.getWaysForNode(aNode.getId());
            while (ways.hasNext()) {
                String wayName = WayHelper.getTag(ways.next(), Tags.TAG_NAME);
                if (wayName != null) {
                    if (name == null || name.trim().length() == 0) {
                        name = wayName;
                    } else {
                        name += " - " + wayName;
                    }
                }
            }
            // non-named node and ways
            if (name == null || name.trim().length() == 0) {
                name = "(" + aNode.getLatitude() + "," + aNode.getLongitude() + ")";
            }
        }
        return name;
    }
    /**
     * @param aNode The node to return.
     * @param aMap my map
     */
    public NodePlace(final Node aNode, final IDataSet aMap) {
        super(NodePlace.getName(aNode, aMap),
                aNode.getLatitude(),
                aNode.getLongitude());
//will fail anyway        if (aNode == null)
//will fail anyway            throw new IllegalStateException("null node given!");
        this.myNode = aNode;
        this.myMap = aMap;
    }

}
