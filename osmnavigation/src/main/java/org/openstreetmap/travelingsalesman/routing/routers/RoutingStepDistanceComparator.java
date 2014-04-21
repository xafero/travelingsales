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

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.routing.metrics.IRoutingMetric;

/**
 * This comparator compares based on the distance
 * of the source-nodes of {@link RoutingStep}s to a target-node.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class RoutingStepDistanceComparator implements Comparator<RoutingStep> {

    /**
     * The node to compare the distance to.
     */
    private NodeDistanceComparator myComparator;
    /**
     * Metric to compare with.
     * We use {@link #myComparator} if the metric is null
     * or gives equal cost.
     */
    private IRoutingMetric myMetric;

    /**
     * The map we operate on.
     */
    //private IDataSet myMap;

    /**
     * @param aTargetNode The node to compare the distance to.
     * @param aMap The map we operate on.
     * one for comparison.
     */
    public RoutingStepDistanceComparator(final IDataSet aMap, final Node aTargetNode) {
        super();
        this.myComparator = new NodeDistanceComparator(aMap, aTargetNode);
    }

    /**
     * Override this to add a metric from the start
     * to a step to the step's metric.
     * @param aStep the step to evaluate
     * @return value >=0
     */
    public double getSummedMetricTo(final RoutingStep aStep) {
        return 0;
    }
    /**
     * @param aTargetNode The node to compare the distance to.
     * @param aMap The map we operate on.
     * one for comparison.
     * @param aMetric metric to compare with (null to use distance)
     */
    public RoutingStepDistanceComparator(final IDataSet aMap, final IRoutingMetric aMetric, final Node aTargetNode) {
        super();
        this.myComparator = new NodeDistanceComparator(aMap, aTargetNode);
        this.myMetric = aMetric;
    }



    /**
     * @param stepA first node to compare
     * @param stepB second node to compare
     * @return -1 0 or 1 depending on the metrics of both
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(final RoutingStep stepA, final RoutingStep stepB) {

        IRoutingMetric metric = myMetric;
        if (metric != null) {
            double a = metric.getCost(stepA) + getSummedMetricTo(stepA);
            double b = metric.getCost(stepB) + getSummedMetricTo(stepB);
            if (a < b) {
                return -1;
            }
            if (b < a) {
                return +1;
            }
        }
        // no metric or the mettic is indetrminate,
        // compare by distance to target
        int compareNode = this.myComparator.compare(stepA.getStartNode(), stepB.getStartNode());
        if (compareNode == 0) {
            // impose a secondary ordering on the wayID we are traveling
            long a = stepA.getWay().getId();
            long b = stepB.getWay().getId();

            if (a < b) {
                return -1;
            }
            if (b < a) {
                return +1;
            }
            // impose a tertiary ordering on the end-node
            // (we can go from the same start-node via the same way into 2 directions
            //   without being equal steps)
            return this.myComparator.compare(stepA.getEndNode(), stepB.getEndNode());
        }
        return compareNode;
    }
}
