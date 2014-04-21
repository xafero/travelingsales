/**
 * This file is part of OSMNavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  OSMNavigation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  OSMNavigation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OSMNavigation.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.routing.routers;

import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.travelingsalesman.routing.IVehicle;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;

/**
 * This is a special DepthFirstRouter that tries the segment
 * that leads near the target first and goes on in that order.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class DirectedDepthFirstRouter extends DepthFirstRouter {


    /**
     * @param aTargetNode the {@link Node} we want to reach
     * @param aCurrentNode the {@link Node} we are now in our recursion
     * @param forbiddenNodes the {@link Node}s we already stepped on
     * @param aMap the map to route in
     * @param aSelector the selector giving us the way that are allowed
     * @return an iterator giving all segments of a node (maybe ordered)
     */
    @Override
    protected Iterator<RoutingStep> getNextStepIterator(final IDataSet aMap,
                    final Node aTargetNode,
                    final Node aCurrentNode,
                    final Set<Node> forbiddenNodes,
                    final IVehicle aSelector) {

       SortedSet<RoutingStep> orderedSegments = new TreeSet<RoutingStep>(new DistanceComparator(aMap, aTargetNode, getMetric()));

       for (Iterator<RoutingStep> iter = super.getNextStepIterator(aMap, aTargetNode, aCurrentNode, forbiddenNodes, aSelector); iter.hasNext();) {
           RoutingStep segment = iter.next();

           orderedSegments.add(segment);

       }
       return orderedSegments.iterator();
   }

}
