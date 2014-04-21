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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedWay;
import org.openstreetmap.travelingsalesman.routing.IProgressListener;
import org.openstreetmap.travelingsalesman.routing.IRouter;
import org.openstreetmap.travelingsalesman.routing.Route;
import org.openstreetmap.travelingsalesman.routing.IVehicle;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.routing.metrics.IRoutingMetric;
import org.openstreetmap.travelingsalesman.routing.metrics.ShortestRouteMetric;

import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;


/**
 * Adaption of the {@link MultiTargetDijkstraRouter} to operate
 * on RoutingSteps instead of nodes.<br/>
 * The distance of a routing-step to a node is that of its source-node.<br/>
 * The next reachable nodes of a routing-step are they ones of its source-node
 * when considering turn-restrictions between the 3 nodes.<br/>
 * With the {@link AStarComparator} this is actually A* and no longer
 * Dijkstra.<br/>
 *
 * We assume:
 * <ul>
 *  <li>there are no direct 2 ways between the same 2 nodes.</li>
 *  <li>The metric is never negative</li>
 * </ul>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class TurnRestrictedAStar implements IRouter {

    /**
     * Compare 2 routing-steps for sorting.
     * Compare by:
     * estimated cost from the step
     * + actual cost of the step
     * + actual cost to the step
     */
    private final class AStarComparator implements Comparator<RoutingStep> {
        /**
         * The target-nodes where our routing starts.
         */
        private final Collection<Node>    myTargetNodes;
        /**
         * The start-node where our routing ends.
         */
        private final Node                myStartingPoint;
        /**
         * getStepKey(RoutingStep) -> summed cost to get to its endNode.
         */
        private final Map<String, Double> myBestDistancesFrom;
        /**
         * myBestStepsFrom
         */
        private final Map<String, RoutingStep> myBestStepsFrom;
        /**
         * @param aTargetNodes The target-nodes where our routing starts.
         * @param aStartingPoint The start-node where our routing ends.
         * @param aBestDistancesFrom getStepKey(RoutingStep) -> summed cost to get to its endNode
         */
        private AStarComparator(final Collection<Node> aTargetNodes,
                final Node aStartingPoint,
                final Map<String, Double> aBestDistancesFrom,
                final Map<String, RoutingStep> aBestStepsFrom) {
            myTargetNodes = aTargetNodes;
            myStartingPoint = aStartingPoint;
            myBestDistancesFrom = aBestDistancesFrom;
            myBestStepsFrom = aBestStepsFrom;
        }

        /**
         * Estimate the cost before taking this step
         * for comparison with the actual cost.
         * @param aStep the step to calculate for
         * @return the estimated cost to reach the nearest target
         */
        public double getSummedHeuristic(final RoutingStep aStep) {
            double minDist = Double.MAX_VALUE;
            if (aStep == null) {
                return minDist;
            }
            for (Node targetNode : myTargetNodes) {
                double dist =  LatLon.distanceInMeters(aStep.getEndNode(), targetNode);
                minDist = Math.min(minDist, dist);
            }
            return minDist;
        }

        /**
         * Estimate the cost after taking this step.
         * @param aStep the step to calculate for
         * @return the estimated cost to the start-node.
         */
        public double getHeuristic(final RoutingStep aStep) {
            double minDist = Double.MAX_VALUE;
            if (aStep == null) {
                return minDist;
            }
            double dist =  LatLon.distanceInMeters(aStep.getStartNode(), myStartingPoint);
            return dist;
        }

        /**
         * Get the actual cost before taking this step.
         * @param aStep the step to calculate for
         * @return the actual cost to reach the nearest target
         */
        public double getSummedMetricTo(final RoutingStep aStep) {
            if (aStep == null) {
                return 0;
            }
            Double double1 = myBestDistancesFrom.get(getStepKey(aStep));
            if (double1 == null) {
                return 0;
            }
            double bestDistanceFromCurrentNode = double1;
            return bestDistanceFromCurrentNode;
        }

        /**
         * @param aStep the sum of cost, summed cost to get to aStep and estimated cost to get to the start-point
         * @return a value used to compare 2 steps
         */
        public double getComparedValue(final RoutingStep aStep) {
            if (aStep == null) {
                return Double.MAX_VALUE;
            }
            if (myBestStepsFrom.get(aStep) != null) LOG.log(Level.INFO,myBestStepsFrom.get(aStep).toString());
            double before = getSummedMetricTo(aStep);
//            double before = 0;
            double beforeEstimated = getSummedHeuristic(aStep);
            double cost = getMetric().getCost(aStep);
            double remainingEstimated = getHeuristic(aStep);
            // heuristic and metric are not comparable,
            // thus assume a linear relationship and find
            // the right conversion-factor
            //if (before > 0 && beforeEstimated > 0) {
            //    remainingEstimated = remainingEstimated * before / beforeEstimated;
            //}
            return before + cost + remainingEstimated;
        }

        /**
         * @param stepA first node to compare
         * @param stepB second node to compare
         * @return -1 0 or 1 depending on the metrics of both
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        public int compare(final RoutingStep stepA, final RoutingStep stepB) {

            double a = getComparedValue(stepA);
            double b = getComparedValue(stepB);

            if (a < b) {
                return -1;
            }
            if (b < a) {
                return +1;
            }
            return stepA.hashCode() - stepB.hashCode(); // never return 0 for different steps
        }
    }

    /**
     * Dummy-Node to build a {@link RoutingStep} for the targetNodes.
     */
    private static final Node DUMMYTARGETNODE = new Node((long) Integer.MIN_VALUE + 1, 0, new Date(), OsmUser.NONE, 0, 0, 0);

    /**
     * Dummy-way used to build a {@link RoutingStep} for the targetNodes.
     */
    private static final Way DUMMYWAY = new Way((long) Integer.MIN_VALUE + 1,
            0,
            new Date(),
            new OsmUser(1, "dummy"), 0);

    /**
     * The minimal progress that has to have been made
     * in order to inform our listeners.
     * Value is between 1.0 (inform at 0% and 100%) and 0 (inform all the time).
     */
    private static final double MINPROGRESSMADE = 0.01;

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(TurnRestrictedAStar.class.getName());

    /**
     * used by {@link #progressMade(Node, Node, Node)} to
     * see if an update for our listeners is waranted.
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
     * Get the shortest of the distances from any of the target-nodes to the start.
     * @param aTargetNodes the targets (any)
     * @param start the stat to reach
     * @return the distance
     * @see LatLon#distance(Node, org.openstreetmap.osm.data.coordinates.Coordinate)
     */
    private double getShortestDistance(final Collection<Node> aTargetNodes, final Node start) {
        double retval = Double.MAX_VALUE;
        for (Node target : aTargetNodes) {
            retval = Math.min(retval, LatLon.distance(start, target));
        }
        return retval;
    }
    /**
     * Inform our {@link IProgressListener}s.
     * @param here where we are
     * @param aTargetNodes where we are going (any of them)
     * @param start where we are started
     */
    protected void progressMade(final Node here, final Collection<Node> aTargetNodes, final Node start) {

        double distTotal = getShortestDistance(aTargetNodes, start);
        double distRemaining = LatLon.distance(here, start);

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

        List<Node> nodes =  aMap.getWayHelper().getNodes(targetWay);
        List<Node> targets = new LinkedList<Node>();
        for (Node targetNode : nodes) {

            if (selector != null && !selector.isAllowed(aMap, targetNode)) {
                continue;
            }
            targets.add(targetNode);
        }
        return route(aMap, targets, startNode, selector);
    }

    /**
     * ${@inheritDoc}.
     */
    public Route route(final IDataSet aMap, final Node aTargetNode, final Node aStartNode, final IVehicle aSelector) {
        List<Node> targets = new LinkedList<Node>();
        targets.add(aTargetNode);
        return route(aMap, targets, aStartNode, aSelector);
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
    private double calculateDistance(final IDataSet aMap, final RoutingStep aLastStep, final RoutingStep step) {

        myMetric.setMap(aMap);
        double cost = myMetric.getCost(step);
        if (aLastStep != null)
            cost += myMetric.getCost(aLastStep.getStartNode(), step, aLastStep);
        return cost;
    }

    /**
     * @param aTargetNodes the {@link Node}s, any one of wich we want to reach
     * @param aStartingPoint the {@link Node} we are now at
     * @param aSelector (may be null) optional selector to determine unallowed roads.
     * @param aMap the map to route on
     * @return null or a list of Segments to use in order to reach the destination
     * @see IRouter#route(org.openstreetmap.osm.data.Node, org.openstreetmap.osm.data.Node, org.openstreetmap.travelingsalesman.routing.IVehicle)
     */
    public Route route(final IDataSet aMap, final Collection<Node> aTargetNodes, final Node aStartingPoint, final IVehicle aSelector) {

        LOG.log(Level.INFO,  "===================================================");
        LOG.log(Level.INFO,  "===================================================");
        LOG.log(Level.INFO,  "TurnRestrictedMultiTargetDijkstraRouter starting...");

        final Map<String, Double> bestDistancesFrom = new HashMap<String, Double>();       // Step-Key->best Metric of a route to the targetNodes from this node so far
        Map<String, RoutingStep> bestStepsFrom = new HashMap<String, RoutingStep>(); // last routing-step taken to reach what is in bestDistancesFrom
        Set<String> stepKeysVisited = new HashSet<String>();

        AStarComparator comparator = new AStarComparator(aTargetNodes, aStartingPoint, bestDistancesFrom, bestStepsFrom);
        NavigableMap<Double, RoutingStep> stepsToVisit = new TreeMap<Double, RoutingStep>();
//        ArrayList<RoutingStep> stepsToVisit = new ArrayList<RoutingStep>();

        //------------------------------------------------------------------------
        // add all target-nodes reachable from the single dummy target-node (multi-target -routing)
        // to bestDistances using dummy RoutingSteps
        for (Node target : aTargetNodes) {
            Way dummyway = DUMMYWAY;
            Collection<RoutingStep> nextSteps = getNextNodes(aMap, aStartingPoint, new RoutingStep(aMap, target, DUMMYTARGETNODE, dummyway), stepKeysVisited, aSelector);
            for (RoutingStep nextStep  : nextSteps) {
                String stepKey = getStepKey(nextStep);
                if (stepKeysVisited.contains(stepKey)) {
                    throw new IllegalStateException("getNextNodes returned a step we already evaluated");
                }
                bestStepsFrom.put(stepKey, nextStep);
                bestDistancesFrom.put(stepKey, (double) (nextStep.distanceInMeters()));
//                bestDistancesFrom.put(stepKey, calculateDistance(aMap, null, nextStep));
                stepsToVisit.put(comparator.getComparedValue(nextStep), nextStep);
            }
        }

        //------------------------------------------------------------------------
        // try to find better ways to each RoutingStep in bestDistances from the target
        while (!stepsToVisit.isEmpty()) {
            final Entry<Double, RoutingStep> currentEntry = stepsToVisit.pollFirstEntry();
            RoutingStep currentStep = currentEntry.getValue();
            // see if the SortedSet did indeed return the lowest-cost element
            currentStep = checkSorting(comparator, stepsToVisit, currentStep);

            Node currentStepNode = currentStep.getStartNode();
            assert !stepsToVisit.containsValue(currentStep);
            if (stepKeysVisited.contains(getStepKey(currentStep))) {
                LOG.warning("Implementation error. stepsToVisit contains an already visited step");
                continue;
            }
            stepKeysVisited.add(getStepKey(currentStep));
            LOG.log(Level.INFO,  "=================================================== We are at: "
                    + getStepKey(currentStep)
                    + " (best distance of this step to the start-nodes was "
                    + currentEntry.getKey() + " )");
            LOG.info("Forbidding further evaluation of " + getStepKey(currentStep) + " #stepsToVisit=" + stepsToVisit.size() + " =" + stepKeysVisited.hashCode());
            if (currentStepNode.getId() == aStartingPoint.getId()) {
                // return with shortest path
                LOG.log(Level.INFO,  "found a shortest path, reconstructing path...");
                return reconstructShortestPath(aMap, aTargetNodes, currentStep, bestStepsFrom, bestDistancesFrom);
            }
            Collection<RoutingStep> nextSteps = getNextNodes(aMap, aStartingPoint, currentStep, stepKeysVisited, aSelector);
            progressMade(currentStepNode, aTargetNodes, aStartingPoint);

            double bestDistanceFromCurrentNode = bestDistancesFrom.get(getStepKey(currentStep));

            for (RoutingStep nextStep  : nextSteps) {
                Node nextNode = nextStep.getStartNode();
                if (stepKeysVisited.contains(getStepKey(nextStep))) {
                    assert false;
                    continue;
                }

                LOG.info("Evaluating step from node " + nextNode.getId() + " via way " + nextStep.getWay().getId() + " to node " + nextStep.getEndNode().getId());

                assert (nextStep.getEndNode().getId() == currentStepNode.getId())
                    :  "Dijkstra: getNextNodes returned a step that does not end where it should!!!\n"
                            + "should end at: " + currentStepNode.getId() + "\n"
                            + "does   end at: " + nextStep.getStartNode().getId() + "\n"
                            + "does start at: " + nextStep.getEndNode().getId() + "\n";
                assert (nextNode.getId() != currentStepNode.getId())
                    : "Dijkstra: getNextNodes returned a step that does START where it should END!!!\n"
                            + "should end at: " + currentStepNode.getId() + "\n"
                            + "does   end at: " + nextStep.getEndNode().getId() + "\n"
                            + "does start at: " + nextStep.getStartNode().getId() + "\n";

                double calculatedDistance = calculateDistance(aMap, currentStep, nextStep);
                boolean alreadyVisited = bestDistancesFrom.containsKey(getStepKey(nextStep));
                if (!alreadyVisited
                    || bestDistancesFrom.get(getStepKey(nextStep)) > bestDistanceFromCurrentNode + calculatedDistance) {

                    LOG.info("Best distance from node " + nextNode.getId()
                            + " is via way " + nextStep.getWay().getId()
                            + " to node " + currentStepNode.getId());
                    bestDistancesFrom.put(getStepKey(nextStep), bestDistanceFromCurrentNode + calculatedDistance);
                    bestStepsFrom.put(getStepKey(nextStep), currentStep);

                    if (alreadyVisited) {
                        // this happens very seldomly andd taked 8% of the time in a city
                        LOG.log(Level.FINE, "resorting stepsToVisit, length=" + stepsToVisit.size());
                        NavigableMap<Double, RoutingStep> temp = new TreeMap<Double, RoutingStep>();
                        for (RoutingStep next : stepsToVisit.values()) {
                            temp.put(comparator.getComparedValue(next), next);
                        }
                        stepsToVisit = temp;
                    }
                }

                // this must happen after bestDistancesFrom is filled
                stepsToVisit.put(comparator.getComparedValue(nextStep), nextStep);
            }

        }


        LOG.log(Level.INFO,  "TurnRestrictedMultiTargetDijkstraRouter found nothing");

        // no path found
        return null;
    }

    /**
     * Check that currentStep is indeed the lowest cost element of stepsToVisit.
     * Unless assertions are enabled or debuging this method does nothing
     * @param comparator used to evaluate the sorting
     * @param stepsToVisit all elements
     * @param currentStep the presumed lowest cost element
     * @return the lowest cost element
     */
    private RoutingStep checkSorting(final AStarComparator comparator,
                                     final NavigableMap<Double, RoutingStep> stepsToVisit,
                                     final RoutingStep currentStep) {
//          check if first is indeed the optimal choice
//            RoutingStep best = null;
//            Double      bestKey = null;
//            double bestMetric = Double.MAX_VALUE;
//            Iterator<Entry<Double, RoutingStep>> iterator = stepsToVisit.entrySet().iterator();
//            while (iterator.hasNext()) {
//                Entry<Double, RoutingStep> entry = iterator.next();
//                RoutingStep next = entry.getValue();
//                double metric = comparator.getComparedValue(next);
//                if (metric != entry.getKey()) {
//                    LOG.warning("Metric has changed from " + entry.getKey() + " to " + metric);
//                }
//                if (metric < bestMetric) {
//                    best = next;
//                    bestKey = entry.getKey();
//                    bestMetric = metric;
//                }
//            }
////                currentStep = best;
//            double currentMetric = comparator.getComparedValue(currentStep);
//            if (currentStep != best && currentMetric > bestMetric) {
//                LOG.warning("Implementation error. next step is not optimal (" + currentMetric + " vs " + bestMetric + ")");
//                stepsToVisit.put(currentMetric, currentStep);
//                currentStep = best;
//                if (stepsToVisit.remove(bestKey) == null) {
//                    LOG.severe("Implementation error. stepsToVisit.remove failed");
//                    //our unstable comparator may break remove()
////                    iterator = stepsToVisit.iterator();
////                    while (iterator.hasNext()) {
////                        RoutingStep next = iterator.next();
////                        if (getStepKey(next).equals(getStepKey(currentStep))) {
////                            iterator.remove();
////                        }
////                    }
//                }
//            }
            return currentStep;
    }

    /**
     * @param currentStep the step to get the key for
     * @return a unique key for this step.
     */
    private String getStepKey(final RoutingStep currentStep) {
        return currentStep.getStartNode().getId()
             + "-" + currentStep.getWay().getId()
             + "-" + currentStep.getEndNode().getId();
    }

    /**
     * Construct the route with the best metric given the steps.
     * @param aMap the map (needed for the {@link Route}-class)
     * @param aTargetNodes where we are going (any of these points is fine)
     * @param aStartingStep where we started
     * @param bestStepsFrom the best last-step indexed by the stepKey of the step we come from
     * @param aBestDistances - for debugging only
     * @return the route
     */
    private Route reconstructShortestPath(final IDataSet aMap, final Collection<Node> aTargetNodes, final RoutingStep aStartingStep,
                                          final Map<String, RoutingStep> bestStepsFrom, final Map<String, Double> aBestDistances) {
        List<RoutingStep> steps = new LinkedList<RoutingStep>();
        RoutingStep currentStep = aStartingStep;
        RoutingStep lastStep = currentStep;
        steps.add(currentStep);
        Set<Long> targetNodeIDs = new HashSet<Long>();
        for (Node target : aTargetNodes) {
            targetNodeIDs.add(target.getId());
        }

        while (!targetNodeIDs.contains(currentStep.getEndNode().getId())) {
            RoutingStep bestStep = bestStepsFrom.get(getStepKey(currentStep));

            if (bestStep.getStartNode().getId() != currentStep.getEndNode().getId()) {
                throw new  IllegalStateException("I TurnRestrictedMultiPathDijkstra's bestStep we have a step that does not start where it should!!\n"
                        + "should start at: " + currentStep.getEndNode().getId() + "\n"
                        + "does   start at: " + bestStep.getStartNode().getId() + "\n"
                        + "does     end at: " + bestStep.getEndNode().getId() + "\n");
            }

            currentStep = bestStep;

            // join steps that simply follow the same road
            // unless we are making a U-turn
            if (lastStep != null
                    && lastStep.getWay().getId() == bestStep.getWay().getId()
                    && lastStep.getStartNode().getId() != currentStep.getEndNode().getId()) {

                // no loops
                assert !(lastStep.getStartNode().getId() == bestStep.getEndNode().getId()
                         && lastStep.getEndNode().getId() == bestStep.getStartNode().getId())
                         :   "Found a loop in MultiPathDijkstra!!\n"
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
                lastStep.setEndNode(currentStep.getEndNode());
            } else {
                steps.add(bestStep);
                lastStep = bestStep;
            }

        }
        Route r = new Route(aMap, steps, aStartingStep.getStartNode());
        return r;
    }


    //---------------------------------------------------------------------- helper-functions

    /**
     * @param aStartingPointToReach the {@link Node} we want to reach
     * @param aCurrentStep where we are (startNode) and have gone to (toNode)
     * @param forbiddenStepKeys the {@link RoutingStep}s we already stepped on
     * @param aMap the map to route in
     * @param aSelector the selector giving us the way that are allowed
     * @return an iterator giving all segments of a node (maybe ordered)
     */
    protected Collection<RoutingStep> getNextNodes(final IDataSet aMap, final Node aStartingPointToReach, final RoutingStep aCurrentStep,
                                                   final Set<String> forbiddenStepKeys, final IVehicle aSelector) {
        // we use a map StepKey->Step to be sure we have no step in here 2 times
        Set<RoutingStep> retval = new HashSet<RoutingStep>();

        try {
            Iterator<Way> waysForNode = aMap.getWaysForNode(aCurrentStep.getStartNode().getId());
            while (waysForNode != null && waysForNode.hasNext()) {
                Way way = waysForNode.next();
                Collection<Relation> turnRestrictionsFrom = getTurnRestrictionsFrom(aMap, way, aCurrentStep.getStartNode());
                if (!isAllowedTurn(turnRestrictionsFrom, aCurrentStep, way)) {
                    LOG.log(Level.FINE, "turn-restriction forbids to go "
                            + "from way " + way.getId()
                            + " via node " + aCurrentStep.getStartNode().getId()
                            + " to way " + aCurrentStep.getWay().getId()
                            + " to node " + aCurrentStep.getEndNode().getId());
                    continue;
                }

                try {
                    if (!aSelector.isAllowed(aMap, way)) {
                        continue;
                    }

                    List<WayNode> wayNodeList = way.getWayNodes();
                    int index = getNodeIndex(aCurrentStep.getStartNode(), wayNodeList);

                    // one step ahead in the list of wayNodes
                    if (wayNodeList.size() > index + 1 && !aSelector.isOneway(aMap, way)) {
                        long nodeID = wayNodeList.get(index + 1).getNodeId();
                        Node node = aMap.getNodeByID(nodeID);
                        if (node == null) {
                            LOG.log(Level.SEVERE, "could not load node with ID=" + nodeID);
                            continue;
                        }
                        if (node.getId() == aCurrentStep.getStartNode().getId()) {
                            continue;
                        }
                        RoutingStep step = new RoutingStep(aMap, node, aCurrentStep.getStartNode(), way);
                        if ((!forbiddenStepKeys.contains(getStepKey(step))) && aSelector.isAllowed(aMap, node)) {
                            LOG.info("Scheduling step ahead from node " + node.getId() + " via way " + way.getId() + " to " + aCurrentStep.getStartNode().getId());
                            retval.add(step); // we are going FROM node TO aCurrentNode!
                        }
                    } else // turn around a roundabout
                        if (wayNodeList.size() == index + 1
                                && wayNodeList.size() > 2
                                && wayNodeList.get(0).getNodeId() == wayNodeList.get(wayNodeList.size() - 1).getNodeId()
                                && !aSelector.isOneway(aMap, way)) {
                        long nodeID = wayNodeList.get(1).getNodeId();
                        Node node = aMap.getNodeByID(nodeID);
                        if (node == null) {
                            LOG.log(Level.SEVERE, "could not load node with ID=" + nodeID);
                            continue;
                        }
                        RoutingStep step = new RoutingStep(aMap, node, aCurrentStep.getStartNode(), way);
                        if ((!forbiddenStepKeys.contains(getStepKey(step))) && aSelector.isAllowed(aMap, node))
                            LOG.info("Scheduling step around from node " + node.getId() + " via way " + way.getId() + " to " + aCurrentStep.getStartNode().getId());
                            retval.add(step); // we are going FROM node TO aCurrentNode!
                    }
                    // one step back in the list of wayNodes
                    index = getLastNodeIndex(aCurrentStep.getStartNode(), wayNodeList);
                    if (index > 0 && !aSelector.isReverseOneway(aMap, way)) {
                        long nodeId = wayNodeList.get(index - 1).getNodeId();
                        Node node = aMap.getNodeByID(nodeId);
                        if (node == null) {
                            LOG.log(Level.SEVERE, "Could not load node with ID=" + nodeId
                                    + " ignoring this nextNode in Dijkstra.");
                            continue;
                        }
                        if (node.getId() == aCurrentStep.getStartNode().getId()) {
                            continue;
                        }
                        RoutingStep step = new RoutingStep(aMap, node, aCurrentStep.getStartNode(), way);
                        if ((!forbiddenStepKeys.contains(getStepKey(step))) && aSelector.isAllowed(aMap, node)) {
                            LOG.info("Scheduling step back from node " + node.getId() + " via way " + way.getId() + " to " + aCurrentStep.getStartNode().getId()
                                    + " (stepkey=" + getStepKey(step) + ") forbiddenStepKeys=" + forbiddenStepKeys.hashCode());
                            retval.add(step); // we are going FROM node TO aCurrentNode!
                        }
                    }
                } catch (Exception e) {
                     LOG.log(Level.SEVERE, "Exception while doing way #" +  way
                                + " way in getNextNodes(NodeID=" + aCurrentStep.getStartNode().getId() + ") in MultiTargetDijkstra! "
                                + "Ignoring this way.", e);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while doing getNextNodes(NodeID="
                    + aCurrentStep.getStartNode().getId() + ") in TurnRestrictedMultiTargetDijkstra! Considering this node a dead end.", e);
        }

        return retval;
    }


    /**
     * Check if a given turn is allowed to be made.
     * @param aTurnRestrictionsFrom all known turn-restrictions involving aFromWay
     * @param aToStep where we go to
     * @param aFromWay where we come from
     * @return true if allowed
     */
    private boolean isAllowedTurn(
            final Collection<Relation> aTurnRestrictionsFrom,
            final RoutingStep aToStep,
            final Way aFromWay) {
        try {
            if (aToStep.getWay() == null || aToStep.getWay() == DUMMYWAY) {
                return true;
            }
            // check aTurnRestrictionsFrom
            for (Relation relation : aTurnRestrictionsFrom) {
                String type = WayHelper.getTag(relation.getTags(), "restriction");
                if (type == null) {
                    LOG.info("incomplete turn-restriction " + relation.getId() + " as no restriction-attribute");
                    continue;
                }
                type = type.toLowerCase();
                boolean only = type.startsWith("only");
                if (!only && !type.startsWith("no")) {
                    LOG.info("illegal turn-restriction " + relation.getId() + " has type `"
                            + type + "` that does not start with only or no");
                    continue;
                }
                List<RelationMember> members = relation.getMembers();
                if (only) {
                    for (RelationMember member : members) {
                        if (member.getMemberRole() != null
                                && member.getMemberRole().equalsIgnoreCase("to")
                                && member.getMemberType().equals(EntityType.Way)) {
                            if (aToStep.getWay().getId() == member.getMemberId()) {
                                return true; // explicitely allowed
                            }
                        }
                    }
                    return false; // not allowed
                } else {
                    for (RelationMember member : members) {
                        if (member.getMemberRole().equalsIgnoreCase("to")
                                && member.getMemberType().equals(EntityType.Way)) {
                            if (aToStep.getWay().getId() == member.getMemberId()) {
                                return false; // explicitely denied
                            }
                        }
                    }
                    return true; // allowed
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while checking turn-restrictions", e);
        }
        return true; // no restrictions apply to us
    }

    /**
     * Get all turn-restrictions applying to this FROM-direction.
     * @param aMap the map we operate on
     * @param aFromWay the way we are comming from.
     * @param aViaNode the node of the intersection
     * @return the turn-restrictions
     */
    private Collection<Relation> getTurnRestrictionsFrom(final IDataSet aMap, final Way aFromWay, final Node aViaNode) {
        Collection<Relation> retval = new LinkedList<Relation>();
        if (aFromWay == null || aFromWay == DUMMYWAY) {
            return retval;
        }
        if (!(aFromWay instanceof ExtendedWay)) {
            return retval;
        }
        ExtendedWay way = (ExtendedWay) aFromWay;
        Set<Long> referencedRelationIDs = way.getReferencedRelationIDs();
        for (Long relID : referencedRelationIDs) {
            Relation relation = aMap.getRelationByID(relID);
            if (relation == null) {
                continue;
            }
            // is it a turn-restriction
            String type = WayHelper.getTag(relation.getTags(), "type");
            if (type == null || !type.equalsIgnoreCase("restriction")) {
                continue;
            }
            //are we in the "from"-role?
            List<RelationMember> members = relation.getMembers();

            // check the via-nodes (if present) if this applies to
            // the same intersection
            boolean hasViaNodes = false;
            boolean hasMatchingViaNode = false;
            for (RelationMember relationMember : members) {
                if (relationMember.getMemberRole() != null
                        && relationMember.getMemberRole().equalsIgnoreCase("via")
                        && relationMember.getMemberType().equals(EntityType.Node)) {
                    hasViaNodes = true;
                    if (relationMember.getMemberId() == aViaNode.getId()) {
                        hasMatchingViaNode = true;
                        break;
                    }
                }
            }
            if (hasViaNodes && !hasMatchingViaNode) {
                LOG.fine("Ignoring turn-restriction because of mismatching via-node");
                continue;
            } else if (hasViaNodes) {
                LOG.fine("NOT ignoring turn-restriction because of via-node " + aViaNode.getId() + " is matching");
            }

            // check if we are in the from -role
            for (RelationMember relationMember : members) {
                if (relationMember.getMemberRole() != null
                        && relationMember.getMemberRole().equalsIgnoreCase("from")
                        && relationMember.getMemberType().equals(EntityType.Way)
                        && relationMember.getMemberId() == aFromWay.getId()) {
                    retval.add(relation);
                    break;
                }
            }
        }
        return retval;
    }

    /**
     * @param aStartNode the node to look for
     * @param wayNodeList the list to look in
     * @return the first index of this node in the list.
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
     * @param aStartNode the node to look for
     * @param wayNodeList the list to look in
     * @return the last index of this node in the list.
     */
    private int getLastNodeIndex(final Node aStartNode, final List<WayNode> wayNodeList) {
        int index = wayNodeList.size() - 1;
        int lastIndex = -1;
        while (lastIndex == -1 && index >= 0) {
            if (wayNodeList.get(index).getNodeId() == aStartNode.getId()) {
                lastIndex = index;
            }
            index--;
        }
        if (lastIndex == -1
                && wayNodeList.size() > 2
                && wayNodeList.get(0).getNodeId() == wayNodeList.get(wayNodeList.size() - 1).getNodeId()
                && wayNodeList.get(0).getNodeId() == aStartNode.getId()) {
            lastIndex = wayNodeList.size() - 2;
        }
        return lastIndex;
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
