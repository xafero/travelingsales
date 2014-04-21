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
package org.openstreetmap.travelingsalesman.routing;

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.LatLon;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;


/**
 * A route is a sorted list of segments to follow.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class Route {

    /**
     * A Routing-Step is a part of a route that is between
     * 2 nodes on the same way.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    public static class RoutingStep {
        /**
         * Our dataset.
         */
        private IDataSet myMap;

        /**
         * Where we start.
         */
        private Node myStartNode;

        /**
         * Where we arrive.
         */
        private Node myEndNode;

        /**
         * What way {@link #myStartNode}
         * and {@link #myEndNode} are.
         */
        private Way myWay;

        /**
         * @return the endNode
         */
        public Node getEndNode() {
            return myEndNode;
        }

        /**
         * @return the startNode
         */
        public Node getStartNode() {
            return myStartNode;
        }

        /**
         * @return the way
         */
        public Way getWay() {
            return myWay;
        }

        /**
         * @return the our dataset.
         */
        public IDataSet getMap() {
            return myMap;
        }

        /**
         * @return distance of this routing step in meters.
         */
        public double distanceInMeters() {
            double distance = 0;
            Node from = this.getStartNode();
            Node to = null;
            List<WayNode> nodes = this.getNodes();
            if (!nodes.isEmpty()) {
                Iterator<WayNode> itWayNodes = nodes.iterator();
                itWayNodes.next();  // skip the startNode
                while (itWayNodes.hasNext()) {
                   to = getMap().getNodeByID(itWayNodes.next().getNodeId());
                   distance += LatLon.distanceInMeters(from, to);
                   from = to;
                }
                return distance;
            } else {
                return Double.MAX_VALUE; // no start-node
            }
        }

        /**
         * @param aMap map where our route
         * @param aStartNode where we start
         * @param aEndNode where we arrive
         * @param aWay what way both nodes are on
         */
        public RoutingStep(final IDataSet aMap, final Node aStartNode, final Node aEndNode, final Way aWay) {
            if (aMap == null) {
                throw new IllegalArgumentException("Null Map given");
            }
            if (aStartNode == null) {
                throw new IllegalArgumentException("Null StartNode given");
            }
            if (aEndNode == null) {
                throw new IllegalArgumentException("Null EndNode given");
            }
            if (aWay == null) {
                throw new IllegalArgumentException("Null Way given");
            }
            myMap = aMap;
            myStartNode = aStartNode;
            myEndNode = aEndNode;
            myWay = aWay;

            if (aStartNode.getId() == aEndNode.getId())
                throw new IllegalArgumentException("start and end -nodes must be different!");
        }

        /**
         * @param aEndNode the endNode to set
         */
        public void setEndNode(final Node aEndNode) {
            if (myStartNode.getId() == aEndNode.getId())
                throw new IllegalArgumentException("end-node must not be the start-node!");
            myEndNode = aEndNode;
            myNodesOnWay = null;
        }

        /**
         * @param aStartNode the startNode to set
         */
        public void setStartNode(final Node aStartNode) {
            if (myEndNode.getId() == aStartNode.getId())
                throw new IllegalArgumentException("start-node must not be the end-node!");
            myStartNode = aStartNode;
            myNodesOnWay = null;
        }

        /**
         * @param aWay What way {@link #myStartNode}
         * and {@link #myEndNode} are.
         */
        public void setWay(final Way aWay) {
            myWay = aWay;
            myNodesOnWay = null;
        }

        /**
         * Cache for {@link #getNodes()}}.
         */
        private volatile SoftReference<List<WayNode>> myNodesOnWay = null;

        /**
         * Get all nodes on the way in the order
         * they are passed.
         * @return the ordered list of nodes.
         */
        public List<WayNode> getNodes() {

            // use cache
            if (myNodesOnWay != null) {
                List<WayNode> retval = myNodesOnWay.get();
                if (retval != null)
                    return retval;
            }

            List<WayNode> retval = new LinkedList<WayNode>();
            List<WayNode> wayNodes = getWay().getWayNodes();

            // determine the location of start and end-index
            int endIndex = getEndIndex();
            int startIndex = getStartIndex(endIndex);

            // TODO switch WayHelper to IVehicle
            // iterate the way in the proper direction
            if (wayNodes.get(0).getNodeId() != wayNodes.get(wayNodes.size() - 1).getNodeId()
                    && !WayHelper.isOneway(getWay())
                    && !WayHelper.isReverseOneway(getWay())) {
                // is not cycled (not closed) way - usual way
                if (endIndex > startIndex) {
                    for (int i = startIndex; i <= endIndex; i++) {
                        retval.add(wayNodes.get(i));
                    }
                } else {
                    for (int i = startIndex; i >= endIndex; i--) {
                        retval.add(wayNodes.get(i));
                    }
                }
            } else {
                // is cycled (closed) way
                // cases:
                // 1. cycled oneway
                // 2. cycled reversed oneway
                // TODO 3. closed twoway road, rare but possible

                // 1. cycled oneway - usual roundabout
                if (WayHelper.isOneway(getWay())) {
                    if (endIndex > startIndex) {
                        for (int i = startIndex; i <= endIndex; i++) {
                            retval.add(wayNodes.get(i));
                        }
                    } else {
                        for (int i = startIndex; i < wayNodes.size(); i++) {
                            retval.add(wayNodes.get(i));
                        }
                        for (int i = 1; i <= endIndex; i++) {
                            retval.add(wayNodes.get(i));
                        }
                    }
                }

                // 2. cycled reversed oneway - reversed roundabout
                if (WayHelper.isReverseOneway(getWay())) {
                    if (endIndex < startIndex) {
                        for (int i = startIndex; i >= endIndex; i--) {
                            retval.add(wayNodes.get(i));
                        }
                    } else {
                        for (int i = startIndex; i > 0; i--) {
                            retval.add(wayNodes.get(i));
                        }
                        for (int i = wayNodes.size() - 1; i >= endIndex; i--) {
                            retval.add(wayNodes.get(i));
                        }
                    }
                }

                // 3. closed two-way road, rare but possible
                // There are we have an ambiguity,
                // because we don't have criteria
                // for directional choice. Suppose we are select
                // direction with minimal amount of nodes lying on the way.
                if (!WayHelper.isReverseOneway(getWay()) && !WayHelper.isOneway(getWay())) {
                    // counter clockwise direction
                    List<WayNode> ccwval = new LinkedList<WayNode>();
                    if (endIndex > startIndex) {
                        for (int i = startIndex; i <= endIndex; i++) {
                            ccwval.add(wayNodes.get(i));
                        }
                    } else {
                        for (int i = startIndex; i < wayNodes.size(); i++) {
                            ccwval.add(wayNodes.get(i));
                        }
                        for (int i = 1; i <= endIndex; i++) {
                            ccwval.add(wayNodes.get(i));
                        }
                    }
                    // clockwise direction
                    List<WayNode> cwval = new LinkedList<WayNode>();
                    if (endIndex < startIndex) {
                        for (int i = startIndex; i >= endIndex; i--) {
                            cwval.add(wayNodes.get(i));
                        }
                    } else {
                        for (int i = startIndex; i > 0; i--) {
                            cwval.add(wayNodes.get(i));
                        }
                        for (int i = wayNodes.size() - 1; i >= endIndex; i--) {
                            cwval.add(wayNodes.get(i));
                        }
                    }
                    // select minimum or conterclockwise direction if equal
                    if (cwval.size() < ccwval.size()) {
                        retval = cwval;
                    } else {
                        retval = ccwval;
                    }
                }
            }

//            boolean started = false;
//            WayNode last = null;
//            for (WayNode node : wayNodes) {
//
//                if (started && last != null)
//                    retval.add(node);
//
//                if (started && (node.getNodeId() == getEndNode().getId()
//                    || node.getNodeId() == getStartNode().getId())) {
//                    retval.add(node);
//                    break;
//                }
//
//                if ((!started) && (node.getNodeId() == getEndNode().getId()
//                     || node.getNodeId() == getStartNode().getId())) {
//                    started = true;
//                    retval.add(node);
//                }
//
//                last = node;
//            }
            myNodesOnWay = new SoftReference<List<WayNode>>(retval);
            return retval;
        }

        /**
         * @param anEndIndex {@link #getEndIndex()}.
         * @return the index into the nodes of the way where we start.
         */
        private int getStartIndex(final int anEndIndex) {
            int startIndex = -1;
            int index = 0;
            List<WayNode> wayNodes = getWay().getWayNodes();
            for (WayNode node : wayNodes) {
                if (node.getNodeId() == getStartNode().getId()) {
                    startIndex = index;
                    if (anEndIndex >= 0) {
                        break;
                    }
                }
                index++;
            }
            if (startIndex < 0) {
                throw new IllegalStateException("Could not find startNode "
                        + getStartNode().getId() + " in given way "
                        + getWay().getId());
            }
            return startIndex;
        }

        /**
         * @return the index into the nodes of the way where we stop.
         */
        private int getEndIndex() {
            int endIndex = -1;
            List<WayNode> wayNodes = getWay().getWayNodes();
            int indexEnd = wayNodes.size() - 1;
            while (endIndex == -1 && indexEnd >= 0) {
                if (wayNodes.get(indexEnd).getNodeId() == getEndNode().getId()) {
                    endIndex = indexEnd;
                }
                indexEnd--;
            }
            if (endIndex < 0) {
                throw new IllegalStateException("Could not find endNode "
                        + getEndNode().getId() + " in given way "
                        + getWay().getId());
            }
            return endIndex;
        }

    }

    /**
     * The ordered list of segments from the start to our destination.
     */
    private List<RoutingStep> myRoutingStepList;

    /**
     * The node where this route starts.
     */
    private Node myStartNode;

    /**
     * The map that is routed on.
     */
    private IDataSet myMap;

    /**
     * @param aMap The map that is routed on.
     * @param aRoutingStepList The ordered list of {@link RoutingStep}s from the start to our destination.
     * @param aStartNode The node where this route starts.
     */
    public Route(final IDataSet aMap, final List<RoutingStep> aRoutingStepList, final Node aStartNode) {
        super();
        if (aMap == null)
            throw new IllegalArgumentException("null map given");
        if (aRoutingStepList == null)
            throw new IllegalArgumentException("null segmentList given");
        if (aStartNode == null)
            throw new IllegalArgumentException("null startNode given");

        this.myMap = aMap;
        this.myRoutingStepList = aRoutingStepList;
        this.myStartNode = aStartNode;
    }

    /**
     * @return The ordered list of segments from the start to our destination.
     */
    public List<RoutingStep> getRoutingSteps() {
        return this.myRoutingStepList;
    }

    /**
     * @return the node where this route starts.
     */
    public Node getStartNode() {
        return myStartNode;
    }

    /**
     * @return the map that was routed on.
     */
    public IDataSet getMap() {
        return myMap;
    }

    /**
     * Combine multiple routes into a single one.
     * @param aRoutes the routes to combine.
     * The start of one route needs to be the target
     * of the previous route.
     * @return the route visiting all these routes in order.
     */
    public static Route combine(final List<Route> aRoutes) {
        if (aRoutes == null)
            throw new IllegalArgumentException("null routes given!");
        if (aRoutes.size() < 1)
            throw new IllegalArgumentException("no routes given!");

        Route firstRoute = aRoutes.get(0);
        List<RoutingStep> steps = new LinkedList<RoutingStep>();
        for (Route route : aRoutes) {
            if (route == null)
                throw new IllegalArgumentException("one of the given routes is null!");
            steps.addAll(route.getRoutingSteps());
        }

        return new Route(firstRoute.getMap(), steps, firstRoute.getStartNode());
    }

    /**
     * @return distance of this route in meters.
     */
    public double distanceInMeters() {
        double distance = 0;
        for (RoutingStep step : this.getRoutingSteps()) {
            distance += step.distanceInMeters();
        }
        return distance;
    }

}
