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

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedWay;
import org.openstreetmap.travelingsalesman.routing.IRouter;
import org.openstreetmap.travelingsalesman.routing.Route;
import org.openstreetmap.travelingsalesman.routing.IVehicle;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.routing.selectors.Motorcar;
import org.openstreetmap.travelingsalesman.routing.metrics.ShortestRouteMetric;
import org.openstreetmap.travelingsalesman.routing.metrics.StaticFastestRouteMetric;

import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * Unit-Test for routers. Downloads
 * some streets and tests some routed through them.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class GeneralRouterTest extends TestCase {

    /**
     * our testdata.
     */
    private IDataSet osmData;

    /**
     * Set default preferences and testdata.
     * @throws Exception may throw anything and fail.
     */
    public void setUp() throws Exception {
        super.setUp();
        MemoryDataSet map = new MemoryDataSet();
        int i = 2;
        List<WayNode> wayNodesShort = new LinkedList<WayNode>();
        List<WayNode> wayNodesLong = new LinkedList<WayNode>();

        myStartNode = new Node(i++, 0, new Date(), null, 0, TESTSTARTLAT, TESTSTARTLON);
        myStartNode.getTags().add(new Tag("name", "myStartNode"));
        map.addNode(myStartNode);
        wayNodesShort.add(new WayNode(i - 1));
        wayNodesLong.add(new WayNode(i - 1));

        Node middleNode0 = new Node(i++, 0, new Date(), null, 0, TESTSTARTLAT + 1, TESTSTARTLON);
        middleNode0.getTags().add(new Tag("name", "middleNode0(rechable from start on allways)"));
        map.addNode(middleNode0);
        wayNodesShort.add(new WayNode(i - 1));
        wayNodesLong.add(new WayNode(i - 1));

        Node middleNode1 = new Node(i++, 0, new Date(), null, 0, TESTSTARTLAT + 1, TESTSTARTLON + 1);
        map.addNode(middleNode1);
        wayNodesLong.add(new WayNode(i - 1));

        Node middleNode2 = new Node(i++, 0, new Date(), null, 0, TESTSTARTLAT + 1, TESTSTARTLON + 2);
        middleNode2.getTags().add(new Tag("name", "middleNode2(reachable from middleNode0 on long way)"));
        map.addNode(middleNode2);
        wayNodesLong.add(new WayNode(i - 1));

        myEndNode = new Node(i++, 0, new Date(), null, 0, TESTENDLAT, TESTENDLON);
        myEndNode.getTags().add(new Tag("name", "myEndNode"));
        map.addNode(myEndNode);
        wayNodesShort.add(new WayNode(i - 1));
        wayNodesLong.add(new WayNode(i - 1));

        Way shortestWay = new Way(0, 0, new Date(), null, 0);
        shortestWay.getWayNodes().addAll(wayNodesShort);
        shortestWay.getTags().add(new Tag("highway", "residential"));

        Way longestWay = new Way(1, 0, new Date(), null, 0);
        longestWay.getWayNodes().addAll(wayNodesLong);
        longestWay.getTags().add(new Tag("highway", "motorway"));

        map.addWay(shortestWay);
        map.addWay(longestWay);

        this.osmData = map;

        //--------- logging
        Logger rootLogger = Logger.getLogger("");

        // Remove any existing handlers.
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // Add a new console handler.
        Handler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(Level.FINEST);
        rootLogger.addHandler(consoleHandler);
        Logger.getLogger("").setLevel(Level.FINEST);
        Logger.getLogger("sun").setLevel(Level.WARNING);
        Logger.getLogger("com.sun").setLevel(Level.WARNING);
        Logger.getLogger("java").setLevel(Level.WARNING);
        Logger.getLogger("javax").setLevel(Level.WARNING);
    }

    /**
     * A selector that forbids footways and cycleways.
     */
    private IVehicle mySelector = new IVehicle() {

        @Override
        public boolean isAllowed(final IDataSet aMap, final Relation aRelation) {
            return true;
        }

        public boolean isAllowed(final IDataSet aMap, final Node aNode) {
            for (Iterator<Way> iter = osmData.getWaysForNode(aNode.getId()); iter.hasNext();) {
                if (!isAllowed(aMap, iter.next())) {
                    return false;
                }
            }
            return true;
        }


        public boolean isAllowed(final IDataSet aMap, final Way aNode) {

            String highway = WayHelper.getTag(aNode, Tags.TAG_HIGHWAY);
            if (highway == null) {
                return true;
            }
            if (highway.equalsIgnoreCase("footway")) {
                return false;
            }
            if (highway.equalsIgnoreCase("cycleway")) {
                return false;
            }
            return true;
        }


        public boolean isOneway(final IDataSet aMap, final Way aWay) {
            return false;
        }
        public boolean isReverseOneway(final IDataSet aMap, final Way aWay) {
            return false;
        }

        @Override
        public ConfigurationSection getSettings() {
            // TODO Auto-generated method stub
            return null;
        }
    };

    /**
     * Start of the test-routes.
     */
    private static final double TESTSTARTLAT = 48.00569707630937d;

    /**
     * Start of the test-routes.
     */
    private static final double TESTSTARTLON = 7.813869145927337d;

    /**
     * Test method for {@link DepthFirstRouter#route(org.openstreetmap.osm.data.Way, org.openstreetmap.osm.data.Node, org.openstreetmap.travelingsalesman.routing.IVehicle)}.
     */
    public void testRouteWayNodeSelector() {
//
//        long timingStart = System.currentTimeMillis();
//
//        LatLon startCoord = new LatLon(TESTSTARTLAT, TESTSTARTLON);
//        Node startNode = NodeHelper.findNearestNode(osmData, startCoord);
//        assertNotNull(startNode);
//
//        long timingFindNearestNode = System.currentTimeMillis() - timingStart;
//
//        Iterator<Way> targetWays = osmData.getWaysByName("Ku�maulstra�e");
//        Way    targetWay  = targetWays.next();
//        while (targetWay.getSegmentReferenceList().size() == 0 && targetWays.hasNext()) {
//            targetWay  = targetWays.next();
//        }
//        if (targetWay.getSegmentReferenceList().size() == 0) {
//            targetWays = osmData.getWaysByName("Kussmaulstrasse");
//            while (targetWay.getSegmentReferenceList().size() == 0 && targetWays.hasNext()) {
//                targetWay  = targetWays.next();
//            }
//        }
//        assertNotNull(targetWay);
//        assertTrue("taget-way contains no segments", targetWay.getSegmentReferenceList().size() > 0);
//
//        long timingGetWaysByName = System.currentTimeMillis() - timingStart;
//
//        IRouter router = new DirectedDepthFirstRouter();
//        Route theRoute = router.route(osmData, targetWay, startNode, mySelector);
//
//        long timingRoute = System.currentTimeMillis() - timingStart;
//
//        assertNotNull("could not calculare route from startNode " + startNode
//                    + " to Ku�maulstra�e", theRoute);
//        assertEquals(4, theRoute.getRoutingSteps().size());
//
//        System.out.println("Timing: \n"
//                + "\t findNearestNode took " + timingFindNearestNode + "ms\n"
//                + "\t getWaysByName took " + timingGetWaysByName + "ms\n"
//                + "\t route took " + timingRoute + "ms\n");
//
////      read the steps to the user
//
//        IRouteDescriber describer = new SimpleRouteDescriber(theRoute);
//        assertTrue(describer.hasNextStep());
//
//        while (describer.hasNextStep()) {
//            String nextStep = describer.getNextStep();
//            assertNotNull(nextStep);
//            assertNotNull(describer.getCurrentStepLocation());
//        }
    }

    /**
     * End of the test-routes.
     */
    private static final double TESTENDLAT = 48.0073681853736d;

    /**
     * End of the test-routes.
     */
    private static final double TESTENDLON = 7.81417606363d;

    /**
     * The node where we start.
     */
    private Node myStartNode;

    /**
     * The node where we want the route to end.
     */
    private Node myEndNode;

    /**
     * Test method for {@link IRouter#route(IDataSet, Node, Node, IVehicle)}.
     */
    public void testRouteToNode() {

        long timingStart = System.currentTimeMillis();

        LatLon startCoord = new LatLon(TESTSTARTLAT, TESTSTARTLON);
        Node startNode = osmData.getNearestNode(startCoord, null);
        assertNotNull(startNode);
        assertEquals(myStartNode, startNode);

        long timingFindNearestNode1 = System.currentTimeMillis() - timingStart;

        LatLon targetCoord = new LatLon(TESTENDLAT, TESTENDLON);
        Node targetNode = osmData.getNearestNode(targetCoord, null);
        assertNotNull(targetNode);
        assertEquals(myEndNode, targetNode);

        long timingFindNearestNode2 = System.currentTimeMillis() - timingStart;

        System.out.println("Timing: \n"
                + "\t findNearestNode(1) took " + timingFindNearestNode1 + "ms\n"
                + "\t findNearestNode(2) took " + timingFindNearestNode2 + "ms\n");
        IRouter router = new DepthFirstRouter();
        routeToNode(startNode, targetNode, router);
        router = new DirectedDepthFirstRouter();
        routeToNode(startNode, targetNode, router);
        router = new DijkstraRouter();
        routeToNode(startNode, targetNode, router);
        router = new MultiTargetDijkstraRouter();
        routeToNode(startNode, targetNode, router);
        router = new TurnRestrictedAStar();
        routeToNode(startNode, targetNode, router);
    }

//  test for a possible bug. Test showed the result to be correct
//    public void testRoundabout() {
//        MultiTargetDijkstraRouter router = new MultiTargetDijkstraRouter();
//        LODDataSet data = new LODDataSet();
//        Node start = data.getNodeByID(30003829);
//        Node target = data.getNodeByID(32462703);
//        Route route = router.route(data, target, start, new SimpleCarSelector());
//        List<RoutingStep> routingSteps = route.getRoutingSteps();
//        for (RoutingStep routingStep : routingSteps) {
//            System.out.println("From: " + routingStep.getStartNode().getId());
//            System.out.println("Via way: " + routingStep.getWay().getId()
//                    + " [highway=" + WayHelper.getTag(routingStep.getWay(), "highway") + "]"
//                    + "[name=" + WayHelper.getTag(routingStep.getWay(), "name") + "]"
//                    + "[junction=" + WayHelper.getTag(routingStep.getWay(), "junction") + "]");
//            System.out.println("To: " + routingStep.getEndNode().getId());
//        }
//    }

    /**
     * @param startNode  where we are starting from
     * @param targetNode where we want to go
     * @param router the router to examine
     */
    private void routeToNode(final Node startNode, final Node targetNode, final IRouter router) {
        try {
            long timingStart = System.currentTimeMillis();
            Route theRoute = router.route(osmData, targetNode, startNode, mySelector);

            long timingRoute = System.currentTimeMillis() - timingStart;

            System.out.println(
                    "\t route took " + timingRoute + "ms using " + router.getClass().getSimpleName() + "\n");

            assertNotNull(theRoute);
            assertTrue(theRoute.getRoutingSteps().size() > 0);
        } catch (RuntimeException e) {
            e.printStackTrace();
            fail("router " + router.getClass().getSimpleName() + " failed the RouteToNode-test. with an exception");
        }
    }

    /**
     * Test routing with turn-restrictions.
     */
    public void testTurnRestriction() {
        final int startNodeID = 1;
        final int viaNodeID   = 2;
        final int toNodeID    = 3;
        final int otherNodeID = 4;
        Node start = new Node(startNodeID , 1, new Date(), null, 0, 0, 0);
        Node via   = new Node(viaNodeID   , 1, new Date(), null, 0, 1, 1);
        Node to    = new Node(toNodeID    , 1, new Date(), null, 0, 1, 0);
        Node other = new Node(otherNodeID , 1, new Date(), null, 0, 0, 1);

        List<Tag> highway = new LinkedList<Tag>();
        highway.add(new Tag(Tags.TAG_HIGHWAY, "primary"));

        List<WayNode> start2viaNodes = new LinkedList<WayNode>();
        start2viaNodes.add(new WayNode(start.getId()));
        start2viaNodes.add(new WayNode(via.getId()));
        ExtendedWay start2via = new ExtendedWay(1, 1, highway, start2viaNodes);

        List<WayNode> via2otherNodes = new LinkedList<WayNode>();
        via2otherNodes.add(new WayNode(via.getId()));
        via2otherNodes.add(new WayNode(other.getId()));
        ExtendedWay via2other = new ExtendedWay(toNodeID, 1, highway, via2otherNodes);

        List<WayNode> via2toNodes = new LinkedList<WayNode>();
        via2toNodes.add(new WayNode(via.getId()));
        via2toNodes.add(new WayNode(to.getId()));
        ExtendedWay via2to = new ExtendedWay(2, 1, highway, via2toNodes);

        IDataSet map = new MemoryDataSet();
        map.addNode(start);
        map.addNode(via);
        map.addNode(to);
        map.addNode(other);
        map.addWay(start2via);
        map.addWay(via2to);

        // route without turn-restriction must work
        IRouter router = new TurnRestrictedAStar();
        Route route = router.route(map, to, start, new Motorcar());
        Assert.assertNotNull(route);

        // route with turn-restriction must NOT work
        List<RelationMember> members = new LinkedList<RelationMember>();
        LinkedList<Tag> relationTags = new LinkedList<Tag>();
        relationTags.add(new Tag("type", "restriction"));
        relationTags.add(new Tag("restriction", "no_right_turn"));
        members.add(new RelationMember(start2via.getId(), EntityType.Way, "from"));
        start2via.addReferencedRelation(1);
        members.add(new RelationMember(via2to.getId(), EntityType.Way, "to"));
        via2to.addReferencedRelation(1);
        Relation turnRestriction = new Relation(1, 1, new Date(), null, 0);
        turnRestriction.getTags().addAll(relationTags);
        turnRestriction.getMembers().addAll(members);
        map.addRelation(turnRestriction);

        route = router.route(map, to, start, new Motorcar());
        Assert.assertNull(route);

        // add another way, so we can reach the start via a U-turn on Node "other"
        map.addWay(via2other);
        route = router.route(map, to, start, new Motorcar());
        Assert.assertNotNull(route);
        //result is (start-way-end):
        // 1(start)-1-2(via)
        // 2(via)  -3-4(other)
        // 4(other)-3-2(via)
        // 2(via)  -2-3(to)
        List<RoutingStep> routingSteps = route.getRoutingSteps();
        final int expectedSteps = 4;
        assertEquals(expectedSteps, routingSteps.size());
        assertEquals(start.getId()    , routingSteps.get(0).getStartNode().getId());
        assertEquals(start2via.getId(), routingSteps.get(0).getWay().getId());
        assertEquals(via.getId()      , routingSteps.get(0).getEndNode().getId());

        assertEquals(via.getId()      , routingSteps.get(1).getStartNode().getId());
        assertEquals(via2other.getId(), routingSteps.get(1).getWay().getId());
        assertEquals(other.getId()    , routingSteps.get(1).getEndNode().getId());

        assertEquals(other.getId()     , routingSteps.get(2).getStartNode().getId());
        assertEquals(via2other.getId() , routingSteps.get(2).getWay().getId());
        assertEquals(via.getId()       , routingSteps.get(2).getEndNode().getId());

        final int i = 3;
        assertEquals(via.getId()       , routingSteps.get(i).getStartNode().getId());
        assertEquals(via2to.getId()    , routingSteps.get(i).getWay().getId());
        assertEquals(to.getId()        , routingSteps.get(i).getEndNode().getId());

        // route with noUturn turn-restriction must NOT work
        List<RelationMember> members2 = new LinkedList<RelationMember>();
        LinkedList<Tag> relationTags2 = new LinkedList<Tag>();
        relationTags2.add(new Tag("type", "restriction"));
        relationTags2.add(new Tag("restriction", "no_u_turn"));
        members2.add(new RelationMember(via2other.getId(), EntityType.Way, "from"));
        members2.add(new RelationMember(via2other.getId(), EntityType.Way, "to"));
        via2other.addReferencedRelation(2);
        Relation turnRestriction2 = new Relation(2, 1, new Date(), null, 0);
        turnRestriction2.getTags().addAll(relationTags);
        turnRestriction2.getMembers().addAll(members2);

        map.addRelation(turnRestriction2);

        route = router.route(map, to, start, new Motorcar());
        Assert.assertNull(route);

        // it must however work if the via-node of that relation is another one
        members2.add(new RelationMember(Integer.MAX_VALUE, EntityType.Node, "via"));
        turnRestriction2 = new Relation(2, 2, new Date(), null, 0);
        turnRestriction2.getTags().addAll(relationTags);
        turnRestriction2.getMembers().addAll(members2);
        map.addRelation(turnRestriction2);
        route = router.route(map, to, start, new Motorcar());
        Assert.assertNotNull(route);
        routingSteps = route.getRoutingSteps();
        assertEquals(expectedSteps, routingSteps.size());
        assertEquals(start.getId()    , routingSteps.get(0).getStartNode().getId());
        assertEquals(start2via.getId(), routingSteps.get(0).getWay().getId());
        assertEquals(via.getId()      , routingSteps.get(0).getEndNode().getId());

        assertEquals(via.getId()      , routingSteps.get(1).getStartNode().getId());
        assertEquals(via2other.getId(), routingSteps.get(1).getWay().getId());
        assertEquals(other.getId()    , routingSteps.get(1).getEndNode().getId());

        assertEquals(other.getId()     , routingSteps.get(2).getStartNode().getId());
        assertEquals(via2other.getId() , routingSteps.get(2).getWay().getId());
        assertEquals(via.getId()       , routingSteps.get(2).getEndNode().getId());

        assertEquals(via.getId()       , routingSteps.get(i).getStartNode().getId());
        assertEquals(via2to.getId()    , routingSteps.get(i).getWay().getId());
        assertEquals(to.getId()        , routingSteps.get(i).getEndNode().getId());

    }

    /**
     * A test for turn-restrictions.
     */
    public void testTurnRestriction2() {
        /* Route from 1 to 3 using the following map
         *   .007 |                  3
         *        |                / |
         *   .0054| betaway  ___5    | alphaway
         *   .0019|     ____/    ____6
         *        | ___/   _____/
         *   .0011|2    __4
         * deltaway|___/ gammaway
         *        |1
         *        |_________________________________
         *       0 .007 .0011 .007 .012
         *
         * 1,4,3 is the shortest path, but TurnRestrictedAStar +
         * ShortestRouteMetric calculates 1,2,3 as the shortest route
         */

        final Node one   = new Node(1 , 1, new Date(), null, 0, 0, 0);
        final Node two   = new Node(2 , 1, new Date(), null, 0, 0.007, 0);
        final Node three = new Node(3 , 1, new Date(), null, 0, 0.012, 0.007);
        final Node four  = new Node(4 , 1, new Date(), null, 0, 0.0011, 0.0011);
        final Node five  = new Node(5 , 1, new Date(), null, 0, 0.012, 0.0019);
        final Node six   = new Node(6 , 1, new Date(), null, 0, 0.007, 0.0054);

        LinkedList<Tag> highwaytags = new LinkedList<Tag>();
        highwaytags.add(new Tag("highway", "unclassified"));

        List<WayNode> alphawaynodes = new LinkedList<WayNode>();
        alphawaynodes.add(new WayNode(four.getId()));
        alphawaynodes.add(new WayNode(six.getId()));
        alphawaynodes.add(new WayNode(three.getId()));
        final Way alphaway = new Way(8, 0, new Date(), null, 0);
        alphaway.getTags().addAll(highwaytags);
        alphaway.getWayNodes().addAll(alphawaynodes);

        List<WayNode> betawaynodes = new LinkedList<WayNode>();
        betawaynodes.add(new WayNode(2));
        betawaynodes.add(new WayNode(five.getId()));
        betawaynodes.add(new WayNode(three.getId()));
        final Way betaway = new Way(9, 0, new Date(), null, 0);
        betaway.getTags().addAll(highwaytags);
        betaway.getWayNodes().addAll(betawaynodes);

        List<WayNode> gammawaynodes = new LinkedList<WayNode>();
        gammawaynodes.add(new WayNode(four.getId()));
        gammawaynodes.add(new WayNode(one.getId()));
        final Way gammaway = new Way(10, 0, new Date(), null, 0);
        gammaway.getTags().addAll(highwaytags);
        gammaway.getWayNodes().addAll(gammawaynodes);

        List<WayNode> deltawaynodes = new LinkedList<WayNode>();
        deltawaynodes.add(new WayNode(1));
        deltawaynodes.add(new WayNode(2));
        final Way deltaway = new Way(7, 0, new Date(), null, 0);
        deltaway.getTags().addAll(highwaytags);
        deltaway.getWayNodes().addAll(deltawaynodes);

        IDataSet map = new MemoryDataSet();
        map.addNode(one);
        map.addNode(two);
        map.addNode(three);
        map.addNode(four);
        map.addNode(five);
        map.addNode(six);

        map.addWay(alphaway);
        map.addWay(betaway);
        map.addWay(gammaway);
        map.addWay(deltaway);

        IRouter router = new TurnRestrictedAStar();
        ShortestRouteMetric metric = new ShortestRouteMetric();
        router.setMetric(metric);
        Route theRoute = router.route(map, map.getNodeByID(1), map.getNodeByID(three.getId()), new Motorcar());
        double routedDistance = theRoute.distanceInMeters();
        double worstDistance = 0;
        worstDistance += metric.getCost(new RoutingStep(map, three, two, betaway));
        worstDistance += metric.getCost(new RoutingStep(map, two, one, deltaway));
        assertFalse("worst_distance (" + worstDistance + ") == routed_distance (" + routedDistance + ")", worstDistance == routedDistance);

        double bestDistance = 0;
        bestDistance += metric.getCost(new RoutingStep(map, three, four, alphaway));
        bestDistance += metric.getCost(new RoutingStep(map, four, one, gammaway));

//        System.out.println("routed distance : " + routedDistance);
//        System.out.println("best distance : " + bestDistance);
        assertTrue("best_distance (" + bestDistance + ") == routed_distance (" + routedDistance + ")", bestDistance == routedDistance);
    }


    /**
     * Test routing on a roundabout road junction, contre clockwise.
     *        *2
     *      *3  *1
     *        *4
     *
     * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
     */
    public void testRoundAbout() {
        final double offset = 0.01;
        int i = 1;
        Node one   = new Node(i++ , 1, new Date(), null, 0, 0,  offset);
        Node two   = new Node(i++ , 1, new Date(), null, 0, offset,  0);
        Node three = new Node(i++ , 1, new Date(), null, 0, 0, -1 * offset);
        Node four  = new Node(i++ , 1, new Date(), null, 0, -1 * offset, 0);

        LinkedList<Tag> roundaboutTags = new LinkedList<Tag>();
        roundaboutTags.add(new Tag("highway", "primary"));
        roundaboutTags.add(new Tag("oneway", "yes"));
        roundaboutTags.add(new Tag("junction", "roundabout"));

        List<WayNode> roundaboutWayNodes = new LinkedList<WayNode>();
        i = 1;
        roundaboutWayNodes.add(new WayNode(i++));
        roundaboutWayNodes.add(new WayNode(i++));
        roundaboutWayNodes.add(new WayNode(i++));
        roundaboutWayNodes.add(new WayNode(i++));
        roundaboutWayNodes.add(new WayNode(1));
        Way roundaboutWay = new Way(i++, 0, new Date(), null, 0);
        roundaboutWay.getTags().addAll(roundaboutTags);
        roundaboutWay.getWayNodes().addAll(roundaboutWayNodes);

        IDataSet map = new MemoryDataSet();
        map.addNode(one);
        map.addNode(two);
        map.addNode(three);
        map.addNode(four);

        map.addWay(roundaboutWay);

        IRouter router = new TurnRestrictedAStar();
//        IRouter router = new MultiTargetDijkstraRouter();
        ShortestRouteMetric metric = new ShortestRouteMetric();
        router.setMetric(metric);

        // Route from 1 to 4 throught nodes 1-2-3-4
        final int lastNodeID = 4;
        Route route1234 = router.route(map, map.getNodeByID(lastNodeID), map.getNodeByID(1), new Motorcar());
        assertNotNull(route1234);
        List<WayNode> routeNodes1234 = route1234.getRoutingSteps().get(0).getNodes();
        i = 1;
        assertEquals(i++, routeNodes1234.get(i - 2).getNodeId());
        assertEquals(i++, routeNodes1234.get(i - 2).getNodeId());
        assertEquals(i++, routeNodes1234.get(i - 2).getNodeId());
        assertEquals(i++, routeNodes1234.get(i - 2).getNodeId());

        // Route from 4 to 1 throught nodes 4-1
        Route route41 = router.route(map, map.getNodeByID(1), map.getNodeByID(lastNodeID), new Motorcar());
        assertNotNull(route41);
        List<WayNode> routeNodes41 = route41.getRoutingSteps().get(0).getNodes();
        assertEquals(lastNodeID, routeNodes41.get(0).getNodeId());
        assertEquals(1, routeNodes41.get(1).getNodeId());

        // Route from 3 to 1 throught nodes 3-4-1
        Route route341 = router.route(map, map.getNodeByID(1), map.getNodeByID(lastNodeID - 1), new Motorcar());
        assertNotNull(route341);
        List<WayNode> routeNodes341 = route341.getRoutingSteps().get(0).getNodes();
        assertEquals(lastNodeID - 1, routeNodes341.get(0).getNodeId());
        assertEquals(lastNodeID, routeNodes341.get(1).getNodeId());
        assertEquals(1, routeNodes341.get(2).getNodeId());

        // Route from 3 to 2 throught nodes 3-4-1-2
        // check if we routing in proper order
        Route route3412 = router.route(map, map.getNodeByID(2), map.getNodeByID(lastNodeID - 1), new Motorcar());
        assertNotNull(route3412);
        List<WayNode> routeNodes3412 = route3412.getRoutingSteps().get(0).getNodes();
        assertEquals(lastNodeID - 1, routeNodes3412.get(0).getNodeId());
        assertEquals(lastNodeID, routeNodes3412.get(1).getNodeId());
        assertEquals(1, routeNodes3412.get(2).getNodeId());
        assertEquals(2, routeNodes3412.get(lastNodeID - 1).getNodeId());
    }

    /**
     * Test for shortest routing on parallel roads.
     * Map figure:
     *           *5    *6
     *          / \    |
     *     *1--*2--*3--*4
     *
     * Shortest routing  between 1-4 is 1-2-3-4, nor 1-2-5-3-4
     * 1-2-3-4 - is primary highway
     * 2-5-3   - is service way
     * 6-4 - is additional highway
     * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
     */
    public void testAlternativeWayDecision() {
        int i = 1;
        final Node one   = new Node(i++ , 1, new Date(), null, 0, 56.7684, 53.7126);
        final Node two   = new Node(i++ , 1, new Date(), null, 0, 56.7754, 53.7286);
        final Node three = new Node(i++ , 1, new Date(), null, 0, 56.7762, 53.7302);
        final Node four  = new Node(i++ , 1, new Date(), null, 0, 56.7765, 53.7310);
        final Node five  = new Node(i++ , 1, new Date(), null, 0, 56.7761, 53.7290);
        final Node six   = new Node(i++ , 1, new Date(), null, 0, 56.7793, 53.7253);

        LinkedList<Tag> highwayTags = new LinkedList<Tag>();
        highwayTags.add(new Tag("highway", "primary"));

        LinkedList<Tag> serviceTags = new LinkedList<Tag>();
        serviceTags.add(new Tag("highway", "tertiary"));

        List<WayNode> highwayNodes = new LinkedList<WayNode>();
        highwayNodes.add(new WayNode(one.getId()));
        highwayNodes.add(new WayNode(two.getId()));
        highwayNodes.add(new WayNode(three.getId()));
        highwayNodes.add(new WayNode(four.getId()));
        Way highwayWay = new Way(i++, 0, new Date(), null, 0);
        highwayWay.getTags().addAll(highwayTags);
        highwayWay.getWayNodes().addAll(highwayNodes);

        List<WayNode> serviceNodes = new LinkedList<WayNode>();
        serviceNodes.add(new WayNode(two.getId()));
        serviceNodes.add(new WayNode(five.getId()));
        serviceNodes.add(new WayNode(three.getId()));
        Way serviceWay = new Way(i++, 0, new Date(), null, 0);
        serviceWay.getTags().addAll(serviceTags);
        serviceWay.getWayNodes().addAll(serviceNodes);


        List<WayNode> additionalWayNodes = new LinkedList<WayNode>();
        additionalWayNodes.add(new WayNode(six.getId()));
        additionalWayNodes.add(new WayNode(four.getId()));
        Way additionalWay = new Way(i++, 0, new Date(), null, 0);
        additionalWay.getTags().addAll(highwayTags);
        additionalWay.getWayNodes().addAll(additionalWayNodes);

        IDataSet map = new MemoryDataSet();
        map.addNode(one);
        map.addNode(two);
        map.addNode(three);
        map.addNode(four);
        map.addNode(five);
        map.addNode(six);

        map.addWay(highwayWay);
        map.addWay(serviceWay);
        map.addWay(additionalWay);

        IRouter router = new TurnRestrictedAStar();
//        IRouter router = new MultiTargetDijkstraRouter();
        StaticFastestRouteMetric metric = new StaticFastestRouteMetric();
        metric.setMap(map);
        router.setMetric(metric);

        // Route from 2 to 3 throught nodes 2-3
        // not throught  2-5-3
        Route route23 = router.route(map, map.getNodeByID(three.getId()), map.getNodeByID(two.getId()), new Motorcar());
        assertNotNull(route23);

        double routedDistance1 = route23.distanceInMeters();
        double bestDistance1 = 0d;
        bestDistance1 += metric.getCost(new RoutingStep(map, two, three, highwayWay));
        assertFalse("best_distance (" + bestDistance1 + ") <= routed_distance (" + routedDistance1 + ")", bestDistance1 < routedDistance1);

        // Route from 1 to 4 throught nodes 1-2-3-4
        // Actually it is route throught  1-2-5-3-4
        Route route1234 = router.route(map, map.getNodeByID(four.getId()), map.getNodeByID(one.getId()), new Motorcar());
        assertNotNull(route1234);
        double routedDistance = route1234.distanceInMeters();
        double bestDistance = 0;
        bestDistance += metric.getCost(new RoutingStep(map, one, two, highwayWay));
        bestDistance += metric.getCost(new RoutingStep(map, two, three, highwayWay));
        bestDistance += metric.getCost(new RoutingStep(map, three, four, highwayWay));
        assertTrue(bestDistance >= routedDistance);

        List<WayNode> routeNodes1234 = route1234.getRoutingSteps().get(0).getNodes();
        i = 0;
        assertEquals(one.getId(),   routeNodes1234.get(i++).getNodeId());
        assertEquals(two.getId(),   routeNodes1234.get(i++).getNodeId());
        assertEquals(three.getId(), routeNodes1234.get(i++).getNodeId());
        assertEquals(four.getId(),  routeNodes1234.get(i++).getNodeId());


        // test case for "before" calculation
        // should be (6-(4)-3-2-1), not (6-(4)-(3)-5-(2)-1)
        Route route64321 = router.route(map, map.getNodeByID(1), map.getNodeByID(six.getId()), new Motorcar());
        assertNotNull(route64321);
        routedDistance = route64321.distanceInMeters();

        bestDistance = 0;
        bestDistance += metric.getCost(new RoutingStep(map, six, four, additionalWay));
        bestDistance += metric.getCost(new RoutingStep(map, four, three, highwayWay));
        bestDistance += metric.getCost(new RoutingStep(map, three, two, highwayWay));
        bestDistance += metric.getCost(new RoutingStep(map, two, one, highwayWay));

//        System.out.println("routed distance : " + routedDistance);
//        System.out.println("best distance : " + bestDistance);

        assertTrue("routed distance ("+routedDistance+") larger than best distance ("+bestDistance+")", routedDistance <= bestDistance);
//        List<WayNode> routeNodes64321 =
        route64321.getRoutingSteps().get(0).getNodes();
//        assertEquals(1, routeNodes1234.get(0).getNodeId());
//        assertEquals(2, routeNodes1234.get(1).getNodeId());
//        assertEquals(3, routeNodes1234.get(2).getNodeId());
//        assertEquals(4, routeNodes1234.get(3).getNodeId());

    }

    /**
     * Reproduce mantis issue #152.
     * http://travelingsales.sourceforge.net/bugs/view.php?id=152
     * Test case for java.util.NoSuchElementException at
     * org.openstreetmap.travelingsalesman.routing.Route$RoutingStep.distanceInMeters
     * and two-way closed (cycled) way.
     */
    public void testRouteRoutingStepDistanceInMeters() {
        int i = 1;
        final Node one = new Node(i++ , 1, new Date(), null, 0, 52.4183022, 4.8881568);
        final Node two = new Node(i++ , 1, new Date(), null, 0, 52.4182659, 4.8880457);
        final Node three = new Node(i++ , 1, new Date(), null, 0, 52.4181933, 4.8879822);
        final Node four = new Node(i++ , 1, new Date(), null, 0, 52.4181619, 4.8880537);
        final Node five = new Node(i++ , 1, new Date(), null, 0, 52.4181716, 4.888141);
        final Node six = new Node(i++ , 1, new Date(), null, 0, 52.4182151, 4.8882044);

        List<WayNode> alphawaynodes = new LinkedList<WayNode>();
        alphawaynodes.add(new WayNode(one.getId()));
        alphawaynodes.add(new WayNode(two.getId()));
        alphawaynodes.add(new WayNode(three.getId()));
        alphawaynodes.add(new WayNode(four.getId()));
        alphawaynodes.add(new WayNode(five.getId()));
        alphawaynodes.add(new WayNode(six.getId()));
        alphawaynodes.add(new WayNode(one.getId()));

        Way alphaway = new Way(1, 0, new Date(), null, 0);
        alphaway.getWayNodes().addAll(alphawaynodes);

        IDataSet map = new MemoryDataSet();
        map.addNode(one);
        map.addNode(two);
        map.addNode(three);
        map.addNode(four);
        map.addNode(five);
        map.addNode(six);

        RoutingStep step = new RoutingStep(map, two, one, alphaway);
        double distance = step.distanceInMeters();

        // 8.54 meters from JOSM measurement plugin, way 30651468
        // http://www.openstreetmap.org/?lat=52.418229&lon=4.88809&zoom=19
        final double minExpectedDist = 8.53;
        final double maxExpectedDist = 8.55;
        assertTrue(distance + "> " + minExpectedDist
                + " && " + distance + "< "
                + maxExpectedDist, distance > minExpectedDist && distance < maxExpectedDist);
    }
}
