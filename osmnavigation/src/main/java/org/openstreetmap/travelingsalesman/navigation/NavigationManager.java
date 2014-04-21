/**
 * NavigationManager.java
 * (c) 2007 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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
 */
package org.openstreetmap.travelingsalesman.navigation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.searching.IWayPlace;
import org.openstreetmap.osm.data.searching.NodePlace;
import org.openstreetmap.osm.data.searching.Place;
import org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener;
import org.openstreetmap.travelingsalesman.routing.IProgressListener;
import org.openstreetmap.travelingsalesman.routing.IRouteChangedListener;
import org.openstreetmap.travelingsalesman.routing.IRouter;
import org.openstreetmap.travelingsalesman.routing.Route;
import org.openstreetmap.travelingsalesman.routing.IVehicle;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.routing.metrics.IRoutingMetric;
import org.openstreetmap.travelingsalesman.routing.metrics.StaticFastestRouteMetric;
import org.openstreetmap.travelingsalesman.routing.routers.MultiTargetDijkstraRouter;
import org.openstreetmap.travelingsalesman.routing.routers.TurnRestrictedAStar;
import org.openstreetmap.travelingsalesman.trafficblocks.tmc.RDSTMCParser;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * The NavigationManager is a helper-class to simplify the
 * core of a navigation-system. (As opposed to a simple
 * route-planing.)
 * It accepts a list of targets and will take care or updating
 * the route as the driver moves along.
 * Thus a simple navigation-program need not care about
 * implementing all the house-keeping like snapping the gps-
 * location to the route and re-calculating the route if
 * the driver did a "wrong" turn.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class NavigationManager implements  IGPSListener {

    /**
     * If we are this far from the nearest point on the route...
     * recalculate.
     */
    private static final double DEFAULTMAXDISTFROMROUTE = 0.15;

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(NavigationManager.class.getName());

    /**
     * The listeners to inform about changes to the route.
     */
    private Collection<IRouteChangedListener> myRouteChangedListeners = new HashSet<IRouteChangedListener>();

    /**
     * The progress-bars/... to update.
     */
    private Collection<IProgressListener> myProgressListeners = new LinkedList<IProgressListener>();

    /**
     * a listener to be informed about changes
     * to the current routing-step in navigation.
     */
    private Collection<IRoutingStepListener> myRoutingStepListeners = new LinkedList<IRoutingStepListener>();

    /**
     * Helper used for informing our {@link IProgressListener}s.
     * Contains the sum of the max-progress for all our {@link RouteCalculator}s.
     */
    private double maxProgress = 0;

    /**
     * Helper used for informing our {@link IProgressListener}s.
     * Contains the sum of the progress made for all our {@link RouteCalculator}s.
     */
    private double currentProgress = 0;

    /**
     * Our IProgressListener to inform our own IProgressListeners.
     * Gets called by our {@link RouteCalculator}s.
     */
    private IProgressListener myProgressListener = new IProgressListener() {

        public void progressMade(final double aDone, final double aTotal, final Node here) {
            for (IProgressListener listener : myProgressListeners) {
                listener.progressMade(aDone, aTotal, here);
            }
        }
    };

    /**
     * May be null.
     * This current route we are following.
     */
    private Route myRoute = null;

    /**
     * Optional selector to determine what
     * ways we are allowed to travel on.
     * (The map may also contain power-lines,
     *  train-lanes, political boundaries, ...
     *  that we are not to route on.)
     *  May be null. Defaults to null.
     */
    private IVehicle mySelector = Settings.getInstance().getPluginProxy(IVehicle.class, null);

    /**
     * The map we operate on.
     * Never null.
     * Defaults to an empty map.
     */
    private IDataSet myMap = new MemoryDataSet();

    /**
     * The list of destinations that are not yet reached.
     */
    private List<Place> myDestinations = new LinkedList<Place>();

    /**
     * Used to display the time we needed to calculate
     * the routes.
     */
    private long timingStart;

    /**
     * Add a listener to inform about changes to the route..
     * @param listener the new listener
     */
    public void addRouteChangedListener(final IRouteChangedListener listener) {
        this.myRouteChangedListeners.add(listener);
    }

    /**
     * Remove a listener to inform about changes to the route..
     * @param listener the listener
     */
    public void removeRouteChangedListener(final IRouteChangedListener listener) {
        this.myRouteChangedListeners.remove(listener);
    }

    /**
     * Set {@link #myRoute}} and inform all {@link IRouteChangedListener}}s.
     * @param newRoute the new route to follow.
     */
    protected void setRoute(final Route newRoute) {
        this.myRoute = newRoute;
        for (IRouteChangedListener istener : myRouteChangedListeners) {
            istener.routeChanged(newRoute);
        }
    }
    /**
     * Inform our listeners that we found no route.
     */
    protected void noRouteFound() {
        for (IRouteChangedListener istener : myRouteChangedListeners) {
            istener.noRouteFound();
        }
    }

    /**
     * Result is never null.
     * @return the map we operate on
     */
    public IDataSet getMap() {
        return myMap;
    }

    /**
     * Result is never null.
     * @return the map we operate on
     */
    public IDataSet getMapForRouters() {
        if (myMap == null)
            throw new IllegalStateException("null map cannot be returned");
        IDataSet routingMap = myMap;
/*        //TO DO: take all destinations and the current
        // location and load a strip of width N between
        // these points into memory
        if (!(myMap instanceof MemoryDataSet)) {
            MysqlDatabaseLoader databaseLoader = new MysqlDatabaseLoader();
            routingMap = databaseLoader.parseOsm();
        }*/
        return routingMap;
    }

    /**
     * Null is not allowed!
     * @param aMap the map we operate on
     */
    public void setMap(final IDataSet aMap) {
        if (aMap == null)
            throw new IllegalArgumentException("null map given");
        myMap = aMap;
        // we are ready now.
        // start collecting Traffic-messages for this map.
        RDSTMCParser.initialize(this);
    };


    /**
     * Optional selector to determine what
     * ways we are allowed to travel on.
     * (The map may also contain power-lines,
     *  train-lanes, political boundaries, ...
     *  that we are not to route on.)
     *  May be null. Defaults to null.
     * @return the selector
     */
    public IVehicle getSelector() {
        return mySelector;
    }

    /**
     * Set the optional selector to determine what
     * ways we are allowed to travel on.
     * (The map may also contain power-lines,
     *  train-lanes, political boundaries, ...
     *  that we are not to route on.)
     *  May be null. Defaults to null.
     * @param aSelector the selector to set
     */
    public void setSelector(final IVehicle aSelector) {
        mySelector = aSelector;
    }

    /**
     * Adds a progress-bar/... to inform.
     * @param listener the new listener
     */
    public void addProgressListener(final IProgressListener listener) {
        this.myProgressListeners.add(listener);
    }

    /**
     * Removes a progress-bar/... to inform.
     * @param listener the listener
     */
    public void removeProgressListener(final IProgressListener listener) {
        this.myProgressListeners.remove(listener);
    }

    /**
     * Adds a listener to be informed about changes
     * to the current routing-step in navigation.
     * @param listener the new listener
     */
    public void addRoutingStepListener(final IRoutingStepListener listener) {
        this.myRoutingStepListeners.add(listener);
    }

    /**
     * Remove a listener to be informed about changes
     * to the current routing-step in navigation.
     * @param listener the listener
     */
    public void removeRoutingStepListener(final IRoutingStepListener listener) {
        this.myRoutingStepListeners.remove(listener);
    }

    /**
     * Never null. Read only!
     * Contains only the unreached destinations!
     * @return the destinations
     */
    public List<Place> getDestinations() {
        return Collections.unmodifiableList(myDestinations);
    }

    /**
     * Set the destinations (not null but empty is allowed).
     * This starts the asyncronous route-calculation resulting
     * in our {@link IRouteChangedListener}}s to be informed
     * when done.
     * @param aDestinations the destinations to set
     * @param startAtGPS start at the gps-position (navigation) or at the first destination (route-planing)
     */
    public void setDestinations(final List<Place> aDestinations, final boolean startAtGPS) {
        if (aDestinations == null)
            throw new IllegalArgumentException("null destinations given!");

        // cancel all currently running calculations.
        if (myCurrentCalculations != null) {
            for (Future<Route> future : myCurrentCalculations.keySet()) {
                // the thread executing this task should be interrupted
                future.cancel(true);
            }
            myCurrentCalculations.clear();
        }

        if (aDestinations != null && aDestinations.size() > 1 && !startAtGPS) {
            // remove the first "destination" if we are routing from
            // A to B instead of Gsp->here to B. Else automatic-rerouting
            // would navigate us to the start-point.
            ArrayList<Place> temp = new ArrayList<Place>(aDestinations);
            temp.remove(0);
            myDestinations = temp;
        } else {
            myDestinations = aDestinations;
        }


        if (aDestinations.size() == 0) {
            // no destinations!
            setRoute(null);
            return;
        }


        maxProgress = 0.0;
        currentProgress = 0.0;

        List<RouteCalculator> routeCalculators = new LinkedList<RouteCalculator>();
        Place lastPlace = aDestinations.get(0);
        if (startAtGPS) {
            lastPlace = getPlaceAtGPS();
        }

        List<Place> destinations = getDestinations();
        for (Iterator<Place> iter = destinations.iterator(); iter.hasNext();) {
            Place nextPlace = iter.next();
            if (lastPlace != null) {
                routeCalculators.add(new RouteCalculator(lastPlace, nextPlace, !iter.hasNext()));
            } else {
                throw new IllegalArgumentException("one of the places give is null!");
            }
            lastPlace = nextPlace;
        }
        this.timingStart = System.currentTimeMillis();

        // start the routing
        ExecutorService executor = getExecutorService();
        Map<Future<Route>, RouteCalculator> calculations = new HashMap<Future<Route>, RouteCalculator>(routeCalculators.size());
        for (RouteCalculator calculator : routeCalculators) {
            calculations.put(executor.submit(calculator), calculator);
        }
        myCurrentCalculations = calculations;

        //TODO: do not start a new thread for this.
        Thread t = new Thread() {
            public void run() {
                List<Route> routes = new LinkedList<Route>();
                boolean cancelRemainingRoutes = false;
                for (Future<Route> future : myCurrentCalculations.keySet()) {

                    if (cancelRemainingRoutes) {
                        future.cancel(true);
                        continue;
                    }

                    try {
                        Route route = future.get();
                        if (route == null) {
                            LOG.log(Level.INFO, "no route found!!");
                            cancelRemainingRoutes = true;
                        }
                        routes.add(route);
                    } catch (InterruptedException e) {
                        LOG.log(Level.INFO, "InterruptedException while calculating a route! Aborting!", e);
                        return;
                    } catch (ExecutionException e) {
                        LOG.log(Level.SEVERE, "ExecutionException while calculating a route! Aborting!", e);
                        return;
                    } catch (CancellationException e) {
                        LOG.log(Level.INFO, "CancellationException while calculating a route! Aborting!", e);
                        return;
                    }
                }
                if (!cancelRemainingRoutes) {
                    // we found a route!
                    Route combinedRoute = Route.combine(routes);
                    LOG.log(Level.INFO, "route with " + combinedRoute.getRoutingSteps().size() + " steps found!!");
                    long timingRoute = System.currentTimeMillis() - timingStart;
                    LOG.log(Level.FINE, "Timing: routing took " + timingRoute + "ms\n");
                    setRoute(combinedRoute);
                } else {
                    noRouteFound();
                }
            }
        };
        t.setName("NavigationManager - finish");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Throws an IllegalStateException if no such place can be found.
     * (no gps-fix, empty map, ...)
     * @return a place that will return the current GPS-position.
     */
    private Place getPlaceAtGPS() {
        if (getLastGPSPos() == null)
            throw new IllegalStateException("no current GPS-location to start routing at!");
        Node nearestNode = getMap().getNearestNode(getLastGPSPos(), getSelector());
        if (nearestNode == null)
            throw new IllegalStateException("no allowed nodes in the map!");
        return new NodePlace(nearestNode, getMap());
    }

    /**
     * The ExecutorService to use.
     */
    private static java.util.concurrent.ExecutorService myExecutorService = null;

    /**
     * The currently running calculations.
     */
    private Map<Future<Route>, RouteCalculator> myCurrentCalculations;

    /**
     * The last GPS-fix we had.
     * Used by {@link #getPlaceAtGPS()}}.
     */
    private LatLon myLastGPSPos = null;


    /**
     * A RouteCalculator is capable of calculating the route between
     * 2 nodes. For visiting multiple places, multiple RouteCalculators
     * are used.
     */
    private class RouteCalculator implements Callable<Route>, IProgressListener {

        /**
         * where to route from.
         */
        private Place myStartPlace;

        /**
         * where to route from.
         */
        private Node myStartNode;

        /**
         * where to route to.
         */
        private Place myTargetPlace;

        /**
         * where to route to.
         */
        private Node myTargetNode;

        /**
         * if the place is a way, route to any point on it.
         */
        private boolean myIsRoutingToWay;

        /**
         * The total progress made the last time
         * {@link #progressMade(double, double, Node)} was called.
         */
        private double myProgressDone = 0.0;

        /**
         * The total progress to make the last time
         * {@link #progressMade(double, double, Node)} was called.
         */
        private double myTotal = 0.0;

        /**
         * @param aStartPlace where to start
         * @param aTargetPlace where to route to
         * @param routeToWay if the place is a way, route to any point on it
         */
        public RouteCalculator(final Place aStartPlace, final Place aTargetPlace, final boolean routeToWay) {
            if (aStartPlace == null)
                throw new IllegalArgumentException("null startPlace give");
            if (aTargetPlace == null)
                throw new IllegalArgumentException("null targetPlace give");
            
            myIsRoutingToWay = routeToWay;
            myStartPlace = aStartPlace;
            myTargetPlace = aTargetPlace;
            
            myStartNode = myStartPlace.getResult();
            if (myStartNode == null)
                throw new IllegalArgumentException("startPlace cannot be resolved");
            
            myTargetNode = myTargetPlace.getResult();
            if (myTargetNode == null)
                throw new IllegalArgumentException("targetPlace cannot be resolved");

            if (myStartNode.getId() == myTargetNode.getId())
                LOG.log(Level.WARNING, "startNode = targetNode!!");

            // replace start+end -node with the nearest node on a way the selector accepts
            if (mySelector != null && !mySelector.isAllowed(getMap(), myStartNode)) {
                myStartNode = getMap().getNearestNode(new LatLon(myStartNode.getLatitude(), myStartNode.getLongitude()), mySelector);
            }
            if (mySelector != null && !mySelector.isAllowed(getMap(), myTargetNode)) {
                myTargetNode = getMap().getNearestNode(new LatLon(myTargetNode.getLatitude(), myTargetNode.getLongitude()), mySelector);
            }
        }

        /**
         * ${@inheritDoc}.
         */
        public void progressMade(final double aDone, final double aTotal, final Node here) {

            synchronized (myProgressListener) {
                NavigationManager.this.maxProgress += aTotal - myTotal;
                NavigationManager.this.currentProgress += aDone - myProgressDone;
                myProgressListener.progressMade(NavigationManager.this.currentProgress,
                                                NavigationManager.this.maxProgress,
                                                here);
            }

            myProgressDone = aDone;
            myTotal = aTotal;
        }


        /**
         * @return the startNode
         */
        public Node getStartNode() {
            return myStartNode;
        }

        /**
         * @return the targetNode
         */
        public Node getTargetNode() {
            return myTargetNode;
        }

        /**
         * @return the startPlace
         */
        public Place getStartPlace() {
            return myStartPlace;
        }

        /**
         * @return the targetPlace
         */
        public Place getTargetPlace() {
            return myTargetPlace;
        }

        /**
         * Do the actual calculation.
         * Called by the {@link ExecutorService}.
         * @throws Exception may throw anything
         * @return the route calculated or null.
         */
        public Route call() throws Exception {
            Thread thr = Thread.currentThread();
        	String oldThreadName = thr.getName();
            thr.setName("Routing");
            try {
                IRouter router = Settings.getInstance().getPlugin(IRouter.class, MultiTargetDijkstraRouter/*TurnRestrictedAStar*/.class.getName());
                router.addProgressListener(this);
                IRoutingMetric metric = Settings.getInstance().getPlugin(IRoutingMetric.class, StaticFastestRouteMetric.class.getName());
                metric.setMap(getMapForRouters());
                router.setMetric(metric);

                Route theRoute = null;
                if (myIsRoutingToWay && getTargetPlace() instanceof IWayPlace) {
                    IWayPlace way = (IWayPlace) getTargetPlace();
                    theRoute = router.route(getMapForRouters(), way.getWay(), getStartNode(), getSelector());
                } else {
                    theRoute = router.route(getMapForRouters(), getTargetNode(), getStartNode(), getSelector());
                }
                return theRoute;
            } finally {
                thr.setName(oldThreadName);
            }
        }
    }


    /**
     * @return the executorService to use for asynchronous calculations.
     */
    public static java.util.concurrent.ExecutorService getExecutorService() {
        if (myExecutorService == null)
            myExecutorService = new java.util.concurrent.ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2);
        return myExecutorService;
    }

    /**
     * @param anExecutorService the executorService to use for asynchronous calculations.
     */
    public void setExecutorService(final ExecutorService anExecutorService) {
        //TODO: provide a central ExecutorService
        if (anExecutorService == null)
            throw new IllegalArgumentException("null ExecutorService given!");
        myExecutorService = anExecutorService;
    }

    /**
     * The current GPS-location has changed.
     * @param aLat the latitude
     * @param aLon the longitude
     */
    public void gpsLocationChanged(final double aLat, final double aLon) {
        setLastGPSPos(new LatLon(aLat, aLon));
    }

    /**
     * @return the lastGPSPos
     */
    public LatLon getLastGPSPos() {
        return myLastGPSPos;
    }

    /**
     * @param aLastGPSPos the lastGPSPos to set
     */
    public void setLastGPSPos(final LatLon aLastGPSPos) {
        myLastGPSPos = aLastGPSPos;

        //TODO: update currently running route-calculations.

        if (this.myRoute == null)
            return;

        // find the nearest node on the current route.
        Node nearestNodeOnRoute = null;
        double minDist = Double.MAX_VALUE;
        RoutingStep minDistStep = null;

        for (RoutingStep step : this.myRoute.getRoutingSteps()) {
            List<WayNode> nodes = step.getNodes();
            for (WayNode node : nodes) {
                Node n = getMap().getNodeByID(node.getNodeId());
                if (n == null) {
                    LOG.log(Level.WARNING, "Node " + node.getNodeId() + " of our route cannot be found in he map!");
                } else {
                    LatLon pos = new LatLon(n.getLatitude(), n.getLongitude());
                    double dist = pos.distance(myLastGPSPos);
                    if (dist <= minDist) { // must be <= instead of <
                        minDist = dist;
                        nearestNodeOnRoute = n;
                        minDistStep = step;
                    }
                }
            }
        }
        // check destinations in the calculation of
        // mindist too, so we  dont restart route-calculation
        // while we are already recalculating
        if (myCurrentCalculations != null) {
            for (RouteCalculator calculator : myCurrentCalculations.values()) {
                Place place = calculator.getStartPlace();
                LatLon pos = new LatLon(place.getLatitude(), place.getLongitude());
                double dist = pos.distance(myLastGPSPos);
                if (dist <= minDist) { // must be <= instead of <
                    minDist = dist;
                }
            }
        }

        // check if we need to recalculate
        //TODO: different distance if we are on a motorway, ...
        double minDistInKm = LatLon.distanceToKilomters(minDist);
        double thresh = Settings.getInstance().getDouble("routing.reroute.treshold.distFromRouteInKm", DEFAULTMAXDISTFROMROUTE);
        if (minDistInKm > thresh) {
            LOG.log(Level.INFO, "We are " + minDistInKm + "Km away from our route and will now recalculate.");
            setDestinations(getDestinations(), true);
        } else {
            LOG.log(Level.FINEST, "We are only " + minDistInKm + " < "
                    + thresh + "Km away from our route and will NOT recalculate.");
        }

        if (nearestNodeOnRoute == null) {
            LOG.log(Level.WARNING, "Could not find ANY node on our route!");
            return;
        }
        //TODO: remove destinations already reached

        // select the current routing-step informRoutingStepListener(minDistStep)
        if (minDistStep != null)
        for (IRoutingStepListener listener : myRoutingStepListeners) {
            //the "nearest Node" is in the the "current step"
            listener.nearestRoutingStepChanged(minDistStep);
        }
    }

    /**
     * We have lost the gps-signal.
     */
    public void gpsLocationLost() {
        this.myLastGPSPos = null;
    }

    /**
     * ignored.
     */
    public void gpsLocationObtained() {
        // do nothing.
    }

    /**
     * ignored.
     * @param course ignored
     */
    public void gpsCourseChanged(final double course) {
        // do nothing.
    }
}
