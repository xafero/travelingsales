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

import java.util.Comparator;

import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.coordinates.Coordinate;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.routing.metrics.IRoutingMetric;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;

/**
 * This comparator compares based on the distance
 * to a target-node.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class DistanceComparator implements Comparator<RoutingStep> {

    /**
     * The node to compare the distance to.
     */
    private Node targetNode;

    /**
     * The metric we are optimizing for.
     */
    private IRoutingMetric myMetric;

    /**
     * @param aTargetNode The node to compare the distance to.
     * @param aMap The map we operate on.
     * one for comparison.
     * @param aMetric the metric we are to optimize for
     */
    public DistanceComparator(final IDataSet aMap, final Node aTargetNode, final IRoutingMetric aMetric) {
        super();
        this.targetNode = aTargetNode;
        this.myMetric = aMetric;
        this.myMetric.setMap(aMap);
    }



    /**
     * @param stepA first node to compare
     * @param stepB second node to compare
     * @return -1 0 or 1 depending on the metrics of both
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(final RoutingStep stepA, final RoutingStep stepB) {

        double a = getMetric(stepA.getEndNode());
        a = a + myMetric.getCost(stepA);
        double b = getMetric(stepB.getEndNode());
        b = b + myMetric.getCost(stepB);

        if (a < b) {
            return -1;
        }
        if (b < a) {
            return +1;
        }
        return 0;
    }

    /**
     * @param nodeA the node to compure the distance for
     * @return the distance
     */
    private double getMetric(final Node nodeA) {
        double dist =  Coordinate.distance(nodeA.getLatitude(), nodeA.getLongitude(),
                                           this.targetNode.getLatitude(), this.targetNode.getLongitude());
        return dist;
    }
}
