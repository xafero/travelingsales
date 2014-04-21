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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.travelingsalesman.routing.IProgressListener;
import org.openstreetmap.travelingsalesman.routing.IRouter;
import org.openstreetmap.travelingsalesman.routing.Route;
import org.openstreetmap.travelingsalesman.routing.IVehicle;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.routing.metrics.IRoutingMetric;
import org.openstreetmap.travelingsalesman.routing.metrics.ShortestRouteMetric;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * A simple Dijkstra.
 * <b>Warning: This implementation has some known bugs!</b>
 *
 * We assume:
 * <ul>
 *  <li>there are no 2 ways between the same 2 nodes.</li>
 *  <li>The metric is never negative</li>
 *  <li>the metric does only depend on the RoutingStep,
 *       not on the step we took before that (e.g.speed-decreased in
 *       sharp corners)</li>
 * </ul>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class DijkstraRouter implements IRouter {

    /**
     * The minimal progress that has to have been made
     * in order to inform our listeners.
     * Value is between 1.0 (inform at 0% and 100%) and 0 (inform all the time).
     */
    private static final double MINPROGRESSMADE = 0.01;

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(DijkstraRouter.class.getName());

    /**
     * used by {@link #progressMade(Node, Node, Node)} to
     * see if an update for our listeners is warranted.
     * (we cannot inform them on every loop-step without
     *  hurting performance dramatically.)
     */
    private double myLastDistRemaining = Double.MAX_VALUE;

    /**
     * This plugin has no  settings, thus this method returns null
     * as described in {@link IPlugin#getSettings()}.
     * @return null
     */
    public ConfigurationSection getSettings() {
        return null;
    }

    /**
     * Inform our {@link IProgressListener}s.
     * @param here where we are
     * @param target where we are going
     * @param start where we are started
     */
    protected void progressMade(final Node here, final Node target, final Node start) {

        double distTotal = LatLon.distance(start, target);
        double distRemaining = LatLon.distance(here, target);

        if (Math.abs(myLastDistRemaining - distRemaining) / distTotal > MINPROGRESSMADE) {
            // at least 1% progress was made
            for (IProgressListener listener : this.myProgressListeners) {
                listener.progressMade(distTotal - distRemaining, distTotal, here);
            }
            myLastDistRemaining = distRemaining;
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
     * my IProgressListenerts.
     * @see #addProgressListener(IProgressListener)
     */
    private Set<IProgressListener> myProgressListeners = new HashSet<IProgressListener>();


    /**
     * @param targetWay the {@link Way} we want to reach
     * @param startNode the {@link Node} we are now
     * @param aMap the map to route on
     * @param selector (may be null) optional selector to determine unallowed roads.
     * @return null or a list of Segments to use in order to reach the destination
     */
    public Route route(final IDataSet aMap, final Way targetWay, final Node startNode, final IVehicle selector) {

        HashSet<Node> failedTargetNodes = new HashSet<Node>();
        List<Node> nodes =  aMap.getWayHelper().getNodes(targetWay);
        for (Node targetNode : nodes) {

            if (selector != null && !selector.isAllowed(aMap, targetNode)) {
                continue;
            }

            if (!failedTargetNodes.contains(targetNode)) {
                Route retval = route(aMap, targetNode, startNode, selector);
                if (retval != null) {
                    return retval;
                }
                failedTargetNodes.add(targetNode);
            }
        }
        return null;
    }


    //--------------------------------------------------------------- core-algorithm

    /**
     * This is the metric we are optimizing for.<br/>
     * We do not guarantee that all crossings will be
     * called with the correct {@link RoutingStep} we
     * took to reach that crossing.
     */
    private IRoutingMetric myMetric = new ShortestRouteMetric();

    /**
     * This is the metric we use for Dijkstra.
     * @param step the way-segment to calculate the metric for
     * @param aMap the map to route on
     * @param aLastStep the step we most likely took to the current place
     * @return the metric
     */
    protected double calculateDistance(final IDataSet aMap, final RoutingStep aLastStep, final RoutingStep step) {

        myMetric.setMap(aMap);
        double cost = myMetric.getCost(step);
        if (aLastStep != null)
            cost += myMetric.getCost(aLastStep.getEndNode(), aLastStep, step);
        return cost;
    }

    /**
     * @param aTargetNode the {@link Way} we want to reach
     * @param aStartNode the {@link Node} we are now
     * @param aSelector (may be null) optional selector to determine unallowed roads.
     * @param aMap the map to route on
     * @return null or a list of Segments to use in order to reach the destination
     * @see IRouter#route(org.openstreetmap.osm.data.Node, org.openstreetmap.osm.data.Node, org.openstreetmap.travelingsalesman.routing.IVehicle)
     */
    public Route route(final IDataSet aMap, final Node aTargetNode, final Node aStartNode, final IVehicle aSelector) {

        LOG.log(Level.INFO,  "DijkstraRouter starting...");


        SortedSet<Node> nodesToVisit = new TreeSet<Node>(new NodeDistanceComparator(aMap, aTargetNode));
        Map<Long, Double> bestDistances = new HashMap<Long, Double>(); // nodeID->best Metric of a route from the startNode to this node so far
        Map<Long, RoutingStep> bestStepsTo = new HashMap<Long, RoutingStep>(); // routing-steps taken to reach whas is in bestDistances
        Set<Long> nodeIDsVisited = new HashSet<Long>();
        nodeIDsVisited.add(aStartNode.getId());

        //------------------------------------------------------------------------
        // add all nodes reachable from the Start to nodesToVisit and bestDistances
        Collection<RoutingStep> nextSteps = getNextNodes(aMap, aTargetNode, aStartNode, nodeIDsVisited, aSelector);
        for (RoutingStep nextStep  : nextSteps) {
            bestStepsTo.put(nextStep.getEndNode().getId(), nextStep);
            bestDistances.put(nextStep.getEndNode().getId(), calculateDistance(aMap, null, nextStep));
            nodesToVisit.add(nextStep.getEndNode());
        }

        //------------------------------------------------------------------------
        // try to find better ways to each node in bestDistances
        while (!nodesToVisit.isEmpty()) {
            Node currentNode = nodesToVisit.first();
            nodesToVisit.remove(currentNode);
            nodeIDsVisited.add(currentNode.getId());

            if (currentNode.getId() == aTargetNode.getId()) {
                // return with shortest path
                LOG.log(Level.INFO,  "DijkstraRouter found a shortest path, reconstructing path...");
                return reconstructShortestPath(aMap, aTargetNode, aStartNode, bestStepsTo, bestDistances);
            }
            nextSteps = getNextNodes(aMap, aTargetNode, currentNode, nodeIDsVisited, aSelector);
            progressMade(currentNode, aTargetNode, aStartNode);

            double bestDistanceToCurrentNode = bestDistances.get(currentNode.getId());

            for (RoutingStep nextStep  : nextSteps) {
                Node nextNode = nextStep.getEndNode();
                nodesToVisit.add(nextNode);

//                assert (nextStep.getStartNode().getId() == currentNode.getId())
//                    :  "Dijkstra: getNextNodes returned a stap that does not start where it should!!!\n"
//                            + "should start at: " + currentNode.getId() + "\n"
//                            + "does   start at: " + nextStep.getEndNode().getId() + "\n"
//                            + "does     end at: " + nextStep.getStartNode().getId() + "\n";
//                assert (nextNode.getId() == currentNode.getId())
//                    : "Dijkstra: getNextNodes returned a stap that does END where it should START!!!\n"
//                            + "should start at: " + currentNode.getId() + "\n"
//                            + "does   start at: " + nextStep.getEndNode().getId() + "\n"
//                            + "does     end at: " + nextStep.getStartNode().getId() + "\n";

                double calculatedDistance = calculateDistance(aMap, bestStepsTo.get(currentNode.getId()), nextStep);

                if (!bestDistances.containsKey(nextNode.getId())
                    || bestDistances.get(nextNode.getId()) > bestDistanceToCurrentNode + calculatedDistance) {

                    bestDistances.put(nextNode.getId(), bestDistanceToCurrentNode + calculatedDistance);
                    bestStepsTo.put(nextNode.getId(), nextStep);
                }

            }

        }


        LOG.log(Level.INFO,  "DijkstraRouter found nothing");

        // no path found
        return null;
    }

    /**
     * Construct the route with the best metric given the steps.
     * @param aMap the map (needed for the {@link Route}-class)
     * @param aTargetNode where we are going
     * @param aStartNode where we started
     * @param bestStepsTo the best last-step indexed by the node it takes us to
     * @param aBestDistances - for debugging only
     * @return the route
     */
    protected Route reconstructShortestPath(final IDataSet aMap, final Node aTargetNode, final Node aStartNode,
                                          final Map<Long, RoutingStep> bestStepsTo, final Map<Long, Double> aBestDistances) {
        List<RoutingStep> steps = new LinkedList<RoutingStep>();
        Node currentNode = aTargetNode;
        RoutingStep lastStep = null;
        while (currentNode.getId() != aStartNode.getId()) {
            RoutingStep bestStep = bestStepsTo.get(currentNode.getId());

            if (bestStep.getEndNode().getId() != currentNode.getId()) {
                throw new  IllegalStateException("I Dijkstra's bestStep we have a step that does not end where it should!!\n"
                        + "should end at: " + currentNode.getId() + "\n"
                        + "does   end at: " + bestStep.getEndNode().getId() + "\n"
                        + "does start at: " + bestStep.getStartNode().getId() + "\n");
            }

            currentNode = bestStep.getStartNode();

            // join steps that simply follow the same road
            if (lastStep != null && lastStep.getWay().getId() == bestStep.getWay().getId()) {

                // no loops
                assert !(lastStep.getStartNode().getId() == bestStep.getEndNode().getId()
                         && lastStep.getEndNode().getId() == bestStep.getStartNode().getId())
                         :   "Found a loop in Dijkstra!!\n"
                           + "Node: " + lastStep.getStartNode().getId() + " road-dist-to-target=" + aBestDistances.get(lastStep.getStartNode().getId()) + "\n"
                           + "Node: " + bestStep.getStartNode().getId() + " road-dist-to-target=" + aBestDistances.get(bestStep.getStartNode().getId()) + "\n"
                           + "current Step: " + bestStep.getStartNode().getId() + " -> " + bestStep.getEndNode().getId() + " (walking best-steps from target to start)\n"
                           + "last    Step: " + lastStep.getStartNode().getId() + " -> " + lastStep.getEndNode().getId() + " (walking best-steps from target to start)\n";
//                //DEBUG---------------
//                if (lastStep.getStartNode().getId() == bestStep.getEndNode().getId()
//                   && lastStep.getEndNode().getId() == bestStep.getStartNode().getId()) {
//                    double distA = Coordinate.distance(lastStep.getStartNode().getLatitude(), lastStep.getStartNode().getLongitude(),
//                                                       aTargetNode.getLatitude(), aTargetNode.getLongitude());
//                    double distB = Coordinate.distance(bestStep.getStartNode().getLatitude(), bestStep.getStartNode().getLongitude(),
//                            aTargetNode.getLatitude(), aTargetNode.getLongitude());
//                    throw new  IllegalStateException("Found a loop in Dijkstra!!\n"
//                            + "Node: " + lastStep.getStartNode().getId() + " dist-to-target=" + distA + " road-dist-to-target="
//                                              + aBestDistances.get(lastStep.getStartNode().getId()) + "\n"
//                            + "Node: " + bestStep.getStartNode().getId() + " dist-to-target=" + distB + " road-dist-to-target="
//                                              + aBestDistances.get(bestStep.getStartNode().getId()) + "\n"
//                            + "current Step: " + bestStep.getStartNode().getId() + " -> " + bestStep.getEndNode().getId() + " (walking best-steps from target to start)\n"
//                            + "last    Step: " + lastStep.getStartNode().getId() + " -> " + lastStep.getEndNode().getId() + " (walking best-steps from target to start)\n"
//                            + "length of bestStep: " + calculateDistance(bestStep) + "\n"
//                            + "length of lastStep: " + calculateDistance(lastStep) + "\n");
//                }
//                //--------------------
                lastStep.setStartNode(currentNode);
            } else {
                steps.add(bestStep);
                lastStep = bestStep;
            }

        }
        Route r = new Route(aMap, steps, aStartNode);
        return r;
    }


    //---------------------------------------------------------------------- helper-functions

    /**
     * @param aTargetNode the {@link Node} we want to reach
     * @param aCurrentNode where we are
     * @param forbiddenNodes the {@link Node}s we already stepped on
     * @param aMap the map to route in
     * @param aSelector the selector giving us the way that are allowed
     * @return an iterator giving all segments of a node (maybe ordered)
     */
    protected Collection<RoutingStep> getNextNodes(final IDataSet aMap, final Node aTargetNode, final Node aCurrentNode, final Set<Long> forbiddenNodeIDs, final IVehicle aSelector) {
        LinkedList<RoutingStep> retval = new LinkedList<RoutingStep>();

        try {
            Iterator<Way> waysForNode = aMap.getWaysForNode(aCurrentNode.getId());
            while (waysForNode.hasNext()) {
                Way way = waysForNode.next();

                if (!aSelector.isAllowed(aMap, way)) {
                    continue;
                }

                List<WayNode> wayNodeList = way.getWayNodes();
                int index = getNodeIndex(aCurrentNode, wayNodeList);

                if (wayNodeList.size() > index + 1) {
                    Node node = aMap.getNodeByID(wayNodeList.get(index + 1).getNodeId());
                    if (!forbiddenNodeIDs.contains(node.getId()) && aSelector.isAllowed(aMap, node))
                        retval.add(new RoutingStep(aMap, aCurrentNode, node, way));
                }

                if (index > 0 && !aSelector.isOneway(aMap, way)) {
                    Node node = aMap.getNodeByID(wayNodeList.get(index - 1).getNodeId());
                    if (!forbiddenNodeIDs.contains(node.getId()) && aSelector.isAllowed(aMap, node))
                        retval.add(new RoutingStep(aMap, aCurrentNode, node, way));
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while doing getNextNodes(NodeID=" + aCurrentNode.getId() + ") in Dijkstra! Considering this node a dead end.", e);
        }

        return retval;
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
