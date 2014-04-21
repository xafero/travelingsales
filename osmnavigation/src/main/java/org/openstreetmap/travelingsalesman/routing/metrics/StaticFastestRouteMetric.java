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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.travelingsalesman.navigation.traffic.TrafficRuleManager;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.trafficblocks.TrafficMessage;
import org.openstreetmap.travelingsalesman.trafficblocks.TrafficMessageStore;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * This metric simply calculated the travel-time of a way-
 * segment using known averages.<br/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class StaticFastestRouteMetric implements IRoutingMetric, ITimeRoutingMetric {

    /**
     * The map we operate on.
     */
    private IDataSet myMap;

    /**
     * Used to evaluate traffic congestions.
     */
    private TrafficMessageStore myTrafficMessages = TrafficMessageStore.getInstance();

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
        Collection<TrafficMessage> allMessages = myTrafficMessages.getAllMessages(getMap());
        Set<TrafficMessage> evaluatedMessages = null;

        double estimatedSpeed = getEstimatedSpeed(aSegment);
        double cost = 0;
        List<WayNode> nodes = aSegment.getNodes();
        Node lastNode = null;
        double distance = 0;
        for (WayNode node : nodes) {
            Node thisNode = myMap.getNodeByID(node.getNodeId());
            if (lastNode != null) {
                distance += getDistance(lastNode, thisNode);
            }
            for (TrafficMessage trafficMessage : allMessages) {
                if (evaluatedMessages != null
                    && evaluatedMessages.contains(trafficMessage)) {
                    continue;
                }
                if (trafficMessage.getEntity() != null
                    && thisNode.equals(trafficMessage.getEntity())) {
                    // get the cost of a traffic jam
                    double speed = getReducedSpeed(trafficMessage, aSegment.getWay());
                    if (speed < estimatedSpeed) {
                        double length = trafficMessage.getLengthInMeters();
                        distance -= length;
                        cost += (length / (speed/120));
                    }
                    if (evaluatedMessages == null) {
                        evaluatedMessages = new HashSet<TrafficMessage>();
                    }
                    evaluatedMessages.add(trafficMessage);
                }
            }

            lastNode = thisNode;
        }

        for (TrafficMessage trafficMessage : allMessages) {
            if (evaluatedMessages != null
                && evaluatedMessages.contains(trafficMessage)) {
                continue;
            }
            if (trafficMessage.getEntity() != null
                && aSegment.getWay().equals(trafficMessage.getEntity())) {
                // get the cost of a traffic jam
                double speed = getReducedSpeed(trafficMessage, aSegment.getWay());
                if (speed < estimatedSpeed) {
                    double length = trafficMessage.getLengthInMeters();
                    distance -= length;
                    cost += (length / (speed/120));
                }
                if (evaluatedMessages == null) {
                    evaluatedMessages = new HashSet<TrafficMessage>();
                }
                evaluatedMessages.add(trafficMessage);
            }
        }

        return cost + (distance / (estimatedSpeed/120)); //getWayClass(aSegment.getWay());
    }

    /**
     * Calculate the speed we move with during the given
     * traffic obstruction.
     * @param aTrafficMessage the traffic obstruction
     * @param aWay the road we are on.
     * @return the speed in km/h
     */
    private double getReducedSpeed(final TrafficMessage aTrafficMessage, final Way aWay) {
        // TODO these numbers are just guessed
        TrafficMessage.TYPES type = aTrafficMessage.getType();
        switch (type) {
        case ROADBLOCK: return 1;
        case TRAFFICJAM: return 30;
        case SLOWTRAFFIC: return 60;
        case IGNORED :
        default: return Double.MAX_VALUE;
        }
    }

    /**
     * Calculate the distance between 2 adjectant nodes.<br/>
     * We return Double.MAX_VALUE if one of the nodes is null.
     * @param aStart the first node
     * @param aEnd the second node
     * @return the distance between them.
     */
    private double getDistance(final Node aStart, final Node aEnd) {
        if (aStart == null) {
            return Double.MAX_VALUE;
        }
        if (aEnd == null) {
            return Double.MAX_VALUE;
        }
        double dist =  LatLon.distanceInMeters(aStart, aEnd);
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
        return 0;
    }

    /**
     * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
     * Project: osmnavigation<br/>
     * StaticFastestRouteMetric.java<br/>
     * created: 18.11.2007 13:58:54 <br/>
     * The road-types we know of.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private enum WayClasses {
        /**
         * A motorway is the fastest known road.
         */
        motorway,
        /**
         * Link to enter/exit a {@link #motorway}.
         */
        motorway_link,
        /**
         * A road that connects {@link #primary}-roads to
         * {@link #motorway}s.
         */
        trunk,
        /**
         * Link to enter/exit a {@link #trunk}.
         */
        trunk_link,
        /**
         * A very fast road. Next class below {@link #motorway}.
         */
        primary,
        /**
         * Link to enter/exit a {@link #primary}.
         */
        primary_link,
        /**
         * A road of the usual secondary road-network
         * connecting all cities.
         */
        secondary,
        /**
         * Link to enter/exit a {@link #secondary}.
         */
        secondary_link,
        /**
         * A road of the usual tertiary road-network
         * connecting all villages.
         */
        tertiary,
        /**
         * A road inside a city/village where people
         * live. Usually heavily speed-limited.
         */
        residential,
        /**
         * The lowest kind of road in the conventional
         * road-network. This does not denote roads that
         * have not yet been classified.
         */
        unclassified;

        /**
         * The value to return from {@link #getFactor(String)}
         * for an unknown type of road.
         */
        public static final int UNKNOWNROADFACTOR = 100;

        /**
         * Get the factor to multiple
         * with the length of the way to get
         * the cost of traveling it.
         * @param highway the value of the highway-attribute
         * @return a value between 0 and 15 or 100 if unknown class
         */
        public static int getFactor(final String highway) {
            for (WayClasses wayClass : WayClasses.values()) {

                if (wayClass.name().equalsIgnoreCase(highway))
                    return wayClass.ordinal();
            }
            return UNKNOWNROADFACTOR;
        }

        /**
         * @param highway the value of the highway-attribute
         * @return the wayclass for it or null
         */
        public static WayClasses getWayClass(final String highway) {
            for (WayClasses wayClass : WayClasses.values()) {

                if (wayClass.name().equalsIgnoreCase(highway))
                    return wayClass;
            }
            return null;
        };
    }

//    /**
//     * Calculates an integer that represents the "class"
//     * of a way.
//     * motorway is the minimum (0), trunk (2),...
//     * @param way the way to evaluate
//     * @return a value between 0 and 15 or 100 if unknown class
//     */
//    private int getWayClass(final Way way) {
//        String highway = WayHelper.getTag(way, Tags.TAG_HIGHWAY);
//        if (highway == null)
//            return WayClasses.UNKNOWNROADFACTOR;
//        return WayClasses.getFactor(highway.toLowerCase());
//    }

    /**
     * @param way the way
     * @return the average speed in kilometers per hour
     */
    private int getAverageSpeed(final Way way) {
        String highway = WayHelper.getTag(way, Tags.TAG_HIGHWAY);
        final int defaultSpeed = 100;
        if (highway == null)
            return defaultSpeed;
        WayClasses wayClass = WayClasses.getWayClass(highway.toLowerCase());
        if (wayClass == null) {
            return defaultSpeed;
        }
        //TODO: these depend on the current country and vehicle
        switch (wayClass) {
        case motorway: return 120;
        case motorway_link: return 80;
        case trunk: return 90;
        case trunk_link: return 60;
        case primary: return 85;
        case primary_link: return 60;
        case secondary: return 75;
        case secondary_link: return 50;
        case tertiary: return 65;
        case residential: return 40;
        case unclassified: return 40;
        default: return defaultSpeed;
        }
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
     * @param aRoutingStep the step to calculate the speed for
     * @return the estimated speed in kilometers per hour
     */
    @Override
    public double getEstimatedSpeed(final RoutingStep aRoutingStep) {
        if (aRoutingStep.getWay() == null) {
            return 0;
        }
        int averageSpeed = getAverageSpeed(aRoutingStep.getWay());
        int maxspeed = TrafficRuleManager.getMaxspeed(aRoutingStep.getWay(), this.myMap);
        if (averageSpeed > maxspeed) {
            return maxspeed;
        }
        return averageSpeed;
    }
}
