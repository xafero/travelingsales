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
package org.openstreetmap.osm.data;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

/**
 * This class stores all the functionality that we want for nodes
 * but that is not included in the osmosis-way-class.
 * The classes org.openstreetmap.osm.data.Node/Segment/Way will
 * disapear with the upcomming OSM-0.5 -API. (That will also no
 * longer have segments at all.)
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class NodeHelper {

    /**
     * my logger for debug and error-output.
     */
    static final Logger LOG = Logger.getLogger(NodeHelper.class.getName());

    /**
     * The map we are working with to
     * resolve ids, ....
     */
    private IDataSet myMap;

    /**
     * @param aMap The map we are working with to resolve ids
     */
    public NodeHelper(final IDataSet aMap) {
        super();
        myMap = aMap;
    }

    /**
     * @return the map
     */
    public IDataSet getMap() {
        return myMap;
    }


//    /**
//     * Return all ways in this dataset that contain
//     * this node.
//     * @param aNode the node to look for
//     * @return an iterator over the list
//     */
//    public Iterator<Way> getWays(final Node aNode) {
//        return getMap().getWaysForNode(aNode.getId());
//    }

    //-------------------------------

    /**
     * @return the tag-value for the given key
     * @param node the way to get the tag from
     * @param key the key of the tag
     */
    public static String getTag(final Entity node, final String key) {
    	// LOG.info("getTag: "+node+", "+key);
    	
        for (Tag tag : node.getTags()) {
            try {
                if (tag.getKey().equals(key))
                    return tag.getValue();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Could not read tag. Skipping it.", e);
            }
        }
        return null;
    }

//    /**
//     * Find the node that is nearest to the given coordinates.
//     * @param dataSet where to look in
//     * @param coord the coordinates
//     * @return the node found or null if there are no nodes.
//     */
//    public static Node findNearestNode(final DataSet dataSet, final Coordinate coord) {
//        if (dataSet == null) {
//            throw new IllegalArgumentException("null dataSet given");
//        }
//
//        double minDist = Double.MAX_VALUE;
//        Node minDistNode = null;
//
//        //TO DO: getNodesByBounds
//        for (Iterator<Node> iter = dataSet.getNodes(); iter.hasNext();) {
//            Node node = iter.next();
//            double dist = Coordinate.distance(node, coord);
//            if (dist < minDist) {
//                minDist = dist;
//                minDistNode = node;
//            }
//        }
//
//        return minDistNode;
//    }
}
