/**
 * This file is part of osmnavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
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
 *
 *  Created: 08.11.2007
 */
package org.openstreetmap.travelingsalesman.routing.metrics;

import java.util.List;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.coordinates.Coordinate;
import org.openstreetmap.travelingsalesman.navigation.traffic.TrafficRuleManager;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * This metric uses a simple physics-model to calculate the
 * fastest route.<br/>
 * TODO: FastestCarPhysicsMetric is not complete yet
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class FastestCarPhysicsMetric implements IRoutingMetric, ITimeRoutingMetric {

    /**
     * The map we operate on.
     */
    private IDataSet myMap;

    /**
     * This plugin has no  settings, thus this method returns null
     * as described in {@link IPlugin#getSettings()}.
     * @return null
     */
    public ConfigurationSection getSettings() {
        return null;
    }

    /**
     * ${@inheritDoc}.
     */
    public double getCost(final RoutingStep aSegment) {

        List<WayNode> nodes = aSegment.getNodes();
        Node lastNode = null;
        WayNode lastWayNode = null;
        Node lastNode2 = null;
        double cost = 0;
        for (WayNode wayNode : nodes) {
            if (lastWayNode == null) {
                lastWayNode = wayNode;
            } else {
                if (lastNode == null) {
                    lastNode = myMap.getNodeByID(lastWayNode.getNodeId());
                }
            Node node = myMap.getNodeByID(wayNode.getNodeId());

            cost += getCost(lastNode2, lastNode, node);

            lastNode2 = lastNode;
            lastNode = node;
            lastWayNode = wayNode;
            }
        }

        return cost;
    }

    /**
     * Calculate the distance between 2 adjectant nodes.<br/>
     * We return Double.MAX_VALUE if one of the nodes is null.
     * @param aPreStart the node before first node (to calculate angle)
     * @param aStart the first node
     * @param aEnd the second node
     * @return the distance between them.
     */
    private double getCost(final Node aPreStart, final Node aStart, final Node aEnd) {
        if (aStart == null) {
            return Double.MAX_VALUE;
        }
        if (aEnd == null) {
            return Double.MAX_VALUE;
        }
        double dist =  Coordinate.distance(aStart.getLatitude(), aStart.getLongitude(),
                                           aEnd.getLatitude(), aEnd.getLongitude());

//TODO: implement physics

        //return Math.sqrt(dist);
        return dist;
    }

    /**
     * This metric allways returns 0 for crossings.
     * @param crossing the crossing we tage
     * @param from the way+node we come from
     * @param to the way+node we go to
     * @return a cost. Guaranteed to be >=0.
     */
    public double getCost(final Node crossing, final RoutingStep from, final RoutingStep to) {
    //TODO: implement physics
        return 0;
    }

    /**
     * @return the map we operate on
     */
    public IDataSet getMap() {
        return myMap;
    }

    /**
     * @param aMap the map we operate on
     */
    public void setMap(final IDataSet aMap) {
        myMap = aMap;
    }

    /**
     * {@inheritDoc}
     */
    public double getEstimatedSpeed(final RoutingStep aRoutingStep) {
        if (aRoutingStep.getWay() == null) {
            return 0;
        }
        final int averageSpeed = 100; //TODO: average speed in FastestCarPhysicsMetric
        int maxspeed = TrafficRuleManager.getMaxspeed(aRoutingStep.getWay(), this.myMap);
        if (maxspeed > averageSpeed) {
            return maxspeed;
        }
        return averageSpeed;
    }

}
