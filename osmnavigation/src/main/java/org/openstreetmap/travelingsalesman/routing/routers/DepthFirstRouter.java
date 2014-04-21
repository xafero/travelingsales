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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.coordinates.Coordinate;
import org.openstreetmap.travelingsalesman.routing.IProgressListener;
import org.openstreetmap.travelingsalesman.routing.IRouter;
import org.openstreetmap.travelingsalesman.routing.NameHelper;
import org.openstreetmap.travelingsalesman.routing.Route;
import org.openstreetmap.travelingsalesman.routing.IVehicle;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.routing.metrics.IRoutingMetric;
import org.openstreetmap.travelingsalesman.routing.metrics.ShortestRouteMetric;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * This is a simple recursive demo-router that does a depth-first-search in
 * the graph of Segments and aborts at a given maximum depth or
 * the first route found.<br/>
 * This router IGNORED any metric!
 *
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class DepthFirstRouter implements IRouter {

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(DepthFirstRouter.class.getName());

    /**
     * The metric we are to optimize for.
     */
    private IRoutingMetric myMetric = new ShortestRouteMetric();

    /**
     * my IProgressListenerts.
     * @see #addProgressListener(IProgressListener)
     */
    private Set<IProgressListener> myProgressListeners = new HashSet<IProgressListener>();

    /**
     * This plugin has no  settings, thus this method returns null
     * as described in {@link IPlugin#getSettings()}.
     * @return null
     */
    public ConfigurationSection getSettings() {
        return null;
    }

    /**
     * @param targetWay the {@link Way} we want to reach
     * @param startNode the {@link Node} we are now
     * @param selector (may be null) optional selector to determine unallowed roads.
     * @param aMap the map to route on
     * @return null or a list of Segments to use in order to reach the destination
     * @see IRouter#route(IDataSet, Way, Node, IVehicle)
     */
    public Route route(final IDataSet aMap, final Way targetWay, final Node startNode, final IVehicle selector) {

        if (targetWay == null) {
            throw new IllegalArgumentException("null targetWay given!");
        }
        if (aMap == null) {
            throw new IllegalArgumentException("no DataSet given!");
        }
        if (startNode == null) {
            throw new IllegalArgumentException("null startNode given!");
        }

        // try to find a route until we have reached a recursion-depth
        // that is larger then the number of nodes in the map.
        // Doubling the depth each time.

        int maxDepth = 2;
        Route route = null;
        while (route == null && maxDepth < Integer.MAX_VALUE) {
            maxDepth *= 2;
            route = findRoute(aMap, targetWay, startNode, maxDepth, selector);
        }
        return route;
    };

    /**
     * @param targetWay the {@link Way} we want to reach
     * @param startNode the {@link Node} we are now
     * @param selector (may be null) optional selector to determine unallowed roads.
     * @param aMap the map to route on
     * @return null or a list of Segments to use in order to reach the destination
     * @see IRouter#route(DataSet, Node, Node, IVehicle)
     */
    public Route route(final IDataSet aMap, final Node targetWay, final Node startNode, final IVehicle selector) {

        // try to find a route until we have reached a recursion-depth
        // that is larger then the number of nodes in the map.
        // Doubling the depth each time.

        int maxDepth = 2;
        while (maxDepth < (Integer.MAX_VALUE / 2)) {
            maxDepth *= 2;
            List<Route.RoutingStep> routeSegments = findRoute(aMap, targetWay, startNode, new HashSet<Node>(), maxDepth, selector);
            if (routeSegments != null) {
                return new Route(aMap, routeSegments, startNode);
            }
        }
        return null;
    };


    //=============================================== internal algorithm ================

    /**
     * @param targetWay the {@link Way} we want to reach
     * @param startNode the {@link Node} we are now
     * @param maxDepth how deep we may recurse into the graph of streets
     * @param aMap the map to route on
     * @param selector (may be null) optional selector to determine unallowed roads.
     * @return null or a list of Segments to use in order to reach the destination
     */
    protected Route findRoute(final IDataSet aMap, final Way targetWay, final Node startNode, final int maxDepth, final IVehicle selector) {

        HashSet<Node> failedTargetNodes = new HashSet<Node>();
        List<Node> nodes =  aMap.getWayHelper().getNodes(targetWay);
        for (Node targetNode : nodes) {

            if (selector != null && !selector.isAllowed(aMap, targetNode)) {
                continue;
            }

            if (!failedTargetNodes.contains(targetNode)) {
                this.startDistance = Double.MAX_VALUE; //initialize for IProgressListeners
                List<Route.RoutingStep> retval = findRoute(aMap, targetNode, startNode, new HashSet<Node>(), maxDepth, selector);
                if (retval != null) {
                    return new Route(aMap, retval, startNode);
                }
                failedTargetNodes.add(targetNode);
            }
        }
        return null;
    }

    /**
     *
     * @param targetNode the {@link Node} we want to reach
     * @param startNode the {@link Node} we are now in our recursion
     * @param lastNodes the {@link Node}s we came from (may be null)
     * @param maxDepth how deep we may recurse into the graph of streets
     * @param selector (may be null) optional selector to determine unallowed roads.
     * @param aMap the map to route on
     * @return null or a list of Segments to use in order to reach the destination
     */
    protected List<Route.RoutingStep> findRoute(final IDataSet aMap,
                                                final Node targetNode, final Node startNode,
                                                final Set<Node> lastNodes, final int maxDepth, final IVehicle selector) {

        lastNodes.add(startNode);

        progressMade(startNode, targetNode);

        LOG.log(Level.FINE, "[DepthFirstRouter] starting in \"" + startNode.getId()
                + "\" = \"" + NameHelper.getNameForNode(aMap, startNode) + "\" maxDepth=" + maxDepth);

        if (startNode == targetNode) {

            LOG.log(Level.INFO, "[DepthFirstRouter] we found a route! ending in Node \"" + NameHelper.getNameForNode(aMap, startNode) + "\"");
            LOG.log(Level.FINE, "[DepthFirstRouter] statistics: lastNodes contains " + lastNodes.size() + " nodes");

            LinkedList<Route.RoutingStep> retval = new LinkedList<Route.RoutingStep>();
            return retval;
        }

        if (maxDepth < 1) {
            LOG.log(Level.FINE, "[DepthFirstRouter] maximum depth reached - backtracking");
            return null;
        }

        // get all Segments at this node
        Iterator<Route.RoutingStep> segments = getNextStepIterator(aMap, targetNode, startNode, lastNodes, selector);
        while (segments.hasNext()) {
            Route.RoutingStep w = segments.next();

            Node otherNode = w.getEndNode();

            // dont go back where we came from
            if (lastNodes.contains(otherNode)) {
                LOG.log(Level.FINER, "[DepthFirstRouter] ignoring this segment - it leads back to where we came from (to \""
                        + otherNode.getId() + "\" = \"" + NameHelper.getNameForNode(aMap, otherNode) + "\")");
                continue;
            }

            if (LOG.isLoggable(Level.FINEST)) {
                String name = NameHelper.getNameForNode(aMap, otherNode);
                String msg  = "[DepthFirstRouter] trying Segment to: " + otherNode.getId();
                if (name != null) {
                    msg += " named \"" + name + "\"";
                }
                LOG.log(Level.FINEST, msg);
            }

            List<RoutingStep> retval = findRoute(aMap, targetNode, otherNode, lastNodes, maxDepth - 1, selector);
            if (retval == null) {
                LOG.log(Level.FINEST, "[DepthFirstRouter] no route behind otherNode= \"" + otherNode.getId()
                        + "\" from \"" + startNode.getId() + "\" maxDepth=" + maxDepth);
                continue;
            }

            if (LOG.isLoggable(Level.FINE)) {
                String name = NameHelper.getNameForNode(aMap, otherNode);
                String msg  = "[DepthFirstRouter] found a route by going: " + NameHelper.getNameForNode(aMap, otherNode);
                if (name != null) {
                    msg += " named \"" + name + "\"";
                }
                LOG.log(Level.FINE, msg);
            }

            // shorten the path by combining parts that are on the same way
            if (retval.size() > 0) {
                RoutingStep lastStep = retval.get(retval.size() - 1);
                if (lastStep.getWay().getId() == w.getWay().getId()) {
                    LOG.log(Level.FINEST, "DepthFirstRouter - combining 2 steps that share a way");
                    lastStep.setStartNode(w.getStartNode());
                } else {
                    retval.add(0, w);
                }
            } else {
                retval.add(0, w);
            }

            return retval;

        }

        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "[DepthFirstRouter] no more segments to follow in \"" + startNode.getId()
                    + "\" = \"" + NameHelper.getNameForNode(aMap, startNode) + "\" maxDepth=" + maxDepth);
        }

        //lastNodes.remove(startNode);
        return null;

    }

    /**
     * @param aTargetNode the {@link Node} we want to reach
     * @param forbiddenNodes the {@link Node}s we already stepped on
     * @param aMap the map to route in
     * @param aSelector the selector giving us the way that are allowed
     * @param aCurrentNode the node we are at
     * @return an iterator giving all segments of a node (maybe ordered)
     */
    protected Iterator<RoutingStep> getNextStepIterator(final IDataSet aMap, final Node aTargetNode, final Node aCurrentNode,
                                                        final Set<Node> forbiddenNodes, final IVehicle aSelector) {

        Iterator<Way> waysForNode = aMap.getWaysForNode(aCurrentNode.getId());
        LinkedList<RoutingStep> retval = new LinkedList<RoutingStep>();
        while (waysForNode.hasNext()) {
            Way way = waysForNode.next();

            if (!aSelector.isAllowed(aMap, way)) {
                continue;
            }

            List<WayNode> wayNodeList = way.getWayNodes();
            int index = getNodeIndex(aCurrentNode, wayNodeList);

            if (wayNodeList.size() > index + 1) {
                Node node = aMap.getNodeByID(wayNodeList.get(index + 1).getNodeId());
                if (!forbiddenNodes.contains(node) && aSelector.isAllowed(aMap, node))
                    retval.add(new RoutingStep(aMap, aCurrentNode, node, way));
            }

            if (index > 0 && !aSelector.isOneway(aMap, way)) {
                Node node = aMap.getNodeByID(wayNodeList.get(index - 1).getNodeId());
                if (!forbiddenNodes.contains(node) && aSelector.isAllowed(aMap, node))
                    retval.add(new RoutingStep(aMap, aCurrentNode, node, way));
            }
        }

        return retval.iterator();
    }

    /**
     * @param aStartNode the node to look for
     * @param wayNodeList the list to look in
     * @return the index of the node in the list.
     */
    private int getNodeIndex(final Node aStartNode, final List<WayNode> wayNodeList) {
        int index = 0;
        for (WayNode node : wayNodeList) {
            if (node.getNodeId() == aStartNode.getId()) {
                break;
            }
            index++;
        }
        return index;
    }


    /**
     * The total distance between the start- and end-point
     * of the route.
     */
    private double startDistance = Double.MAX_VALUE;

    /**
     * Inform our {@link IProgressListener}s.
     * @param here where does our partitial route end
     * @param target where are we going
     */
    protected void progressMade(final Node here, final Node target) {

        double dist =  Coordinate.distance(here.getLatitude(), here.getLongitude(),
                target.getLatitude(), target.getLongitude());
        if (startDistance == Double.MAX_VALUE)
            startDistance = dist;

        for (IProgressListener listener : this.myProgressListeners) {
            listener.progressMade(startDistance - dist, startDistance, here);
        }
    }

    /**
     * Add a listener to be informed about the progress we make.
     * @param aListener the listener
     */
    public void addProgressListener(final IProgressListener aListener) {
        this.myProgressListeners.add(aListener);
    }

    /**
     * @return the metric we are to optimize for
     */
    public IRoutingMetric getMetric() {
        return myMetric;
    }

    /**
     * @param aMetric the metric we are to optimize for
     */
    public void setMetric(final IRoutingMetric aMetric) {
        myMetric = aMetric;
    }

}
