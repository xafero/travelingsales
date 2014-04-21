/**
 * StaticFastestRouteMetricTest.java
 * created: 18.11.2007 14:04:19
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
package org.openstreetmap.travelingsalesman.routing.metrics;


import java.util.Date;

import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.travelingsalesman.routing.Route;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import junit.framework.TestCase;


/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: osmnavigation<br/>
 * StaticFastestRouteMetricTest.java<br/>
 * created: 18.11.2007 14:04:19 <br/>
 * Test the  StaticFastestRouteMetric.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class StaticFastestRouteMetricTest extends TestCase {



    /**
     * Test method for {@link StaticFastestRouteMetric#getCost(RoutingStep)}.
     */
    public void testGetCostRoutingStep() {
        StaticFastestRouteMetric  subject = new StaticFastestRouteMetric();

        IDataSet map = new MemoryDataSet();
        subject.setMap(map);
        Node startNode = new Node(1, 0, new Date(), null, 0,
                       Math.random(), Math.random());
        Node endNode = new Node(2, 0, new Date(), null, 0,
                       startNode.getLatitude(), 1 + startNode.getLongitude());
        map.addNode(startNode);
        map.addNode(endNode);

     // calculate the cost of using a road of unknown quality
        Way wayGarbage = new Way(1, 0, new Date(), null, 0);
        wayGarbage.getTags().add(new Tag("highway", "GARBAGE"));
        wayGarbage.getWayNodes().add(new WayNode(startNode.getId()));
        wayGarbage.getWayNodes().add(new WayNode(endNode.getId()));

        Route.RoutingStep step = new Route.RoutingStep(map, startNode, endNode, wayGarbage);
        double costGarbage = subject.getCost(step);
        assertTrue("negative cost returned", costGarbage >= 0);

        // calculate the cost of using a motorway
        Way wayMotorway = new Way(1, 0, new Date(), null, 0);
        wayMotorway.getTags().add(new Tag("highway", "motorway"));
        wayMotorway.getWayNodes().add(new WayNode(startNode.getId()));
        wayMotorway.getWayNodes().add(new WayNode(endNode.getId()));
        step = new Route.RoutingStep(map, startNode, endNode, wayMotorway);
        double costMotorway = subject.getCost(step);
        assertTrue("negative cost returned", costMotorway >= 0);
        assertTrue("The cost of using a motorway is not less then the cost of an unknown road-type!", costMotorway < costGarbage);

     // calculate the cost of using a secondary road
        Way waySecondary = new Way(1, 0, new Date(), null, 0);
        waySecondary.getTags().add(new Tag("highway", "secondary"));
        waySecondary.getWayNodes().add(new WayNode(startNode.getId()));
        waySecondary.getWayNodes().add(new WayNode(endNode.getId()));
        step = new Route.RoutingStep(map, startNode, endNode, waySecondary);
        double costSecondary = subject.getCost(step);
        assertTrue("negative cost returned", costSecondary >= 0);
        //assertTrue("The cost of using a secondary road is not less then the cost of an unknown road-type!", costSecondary < costGarbage);
        assertTrue("The cost of using a motorway is not less then the cost of a secodary road-type!", costMotorway < costSecondary);
    }

    /**
     * Test method for
     * {@link StaticFastestRouteMetric#getCost(Node, RoutingStep, Route.RoutingStep)}.
     */
    public void testGetCostNodeRoutingStepRoutingStep() {
        StaticFastestRouteMetric  subject = new StaticFastestRouteMetric();
        IDataSet map = new MemoryDataSet();
        subject.setMap(map);
        Node startNode = new Node(1, 0, new Date(), null, 0,
                       Math.random(), Math.random());
        Node endNode = new Node(2, 0, new Date(), null, 0,
                       startNode.getLatitude(), 2 + startNode.getLongitude());
        Node crossingNode = new Node(2 + 1, 0, new Date(), null, 0,
                startNode.getLatitude(), 1 + startNode.getLongitude());
        map.addNode(startNode);
        map.addNode(endNode);
        map.addNode(crossingNode);

        // calculate the cost of using a road of unknown quality
        Way wayToCrossing = new Way(1, 0, new Date(), null, 0);
        wayToCrossing.getTags().add(new Tag("highway", "secondary"));
        wayToCrossing.getWayNodes().add(new WayNode(startNode.getId()));
        wayToCrossing.getWayNodes().add(new WayNode(endNode.getId()));

        Route.RoutingStep stepToCrossing = new Route.RoutingStep(map, startNode, endNode, wayToCrossing);

        // calculate the cost of using a road of unknown quality
        Way wayFromCrossing = new Way(1, 0, new Date(), null, 0);
        wayFromCrossing.getTags().add(new Tag("highway", "secondary"));
        wayFromCrossing.getWayNodes().add(new WayNode(startNode.getId()));
        wayFromCrossing.getWayNodes().add(new WayNode(endNode.getId()));

        Route.RoutingStep stepFromCrossing = new Route.RoutingStep(map, startNode, endNode, wayFromCrossing);

        // just assert that we do not throw exceptions and return a non-negative cost
        double cost = subject.getCost(crossingNode, stepFromCrossing, stepToCrossing);
        assertTrue(cost >= 0);
    }

    /**
     * Test method for {@link StaticFastestRouteMetric#getMap()}.
     */
    public void testGetMap() {
        StaticFastestRouteMetric  subject = new StaticFastestRouteMetric();
        IDataSet map = new MemoryDataSet();
        // trivial test to detect programming-errors introduced.
        subject.setMap(map);
        assertSame(map, subject.getMap());
    }
}
