/**
 * WayHelperTest.java
 * created: 02.03.2008 10:40:08
 * (c) 2008 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of libosm by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  libosm is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  libosm is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with libosm.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.osm.data;



//automatically created logger for debug and error -output
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
//import java.util.logging.Logger;

//automatically created propertyChangeListener-Support
//import java.beans.PropertyChangeListener;
//import java.beans.PropertyChangeSupport;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * WayHelperTest.java<br/>
 * created: 02.03.2008 10:40:08 <br/>
 *<br/><br/>
 * Test-Cases for the WayHelper.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class WayHelperTest extends TestCase  {

    /**
     * Automatically created logger for debug and error-output.
     */
//    private static final Logger  LOG = Logger.getLogger(
//                                             WayHelperTest.class.getName());

    //-------------------------------------------------------

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "WayHelperTest@" + hashCode();
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
    }

    /**
     * Test method for {@link org.openstreetmap.osm.data.WayHelper#getMap()}.
     */
    @Test
    public void testGetMap() {
        MemoryDataSet map = new MemoryDataSet();
        WayHelper subject = new WayHelper(map);
        assertSame(map, subject.getMap());
    }

    /**
     * Test method for {@link org.openstreetmap.osm.data.WayHelper#getTag(org.openstreetmap.osmosis.core.domain.v0_5.Entity, java.lang.String)}.
     */
    @Test
    public void testGetTag() {
        Way way0 = new Way(1L, 0, new Date(), null, 0);
        way0.getTags().add(new Tag("name0", "value0"));
        assertEquals(WayHelper.getTag(way0, "name0"), "value0");
    }

    /**
     * Test method for {@link org.openstreetmap.osm.data.WayHelper#getNodes(org.openstreetmap.osmosis.core.domain.v0_5.Way)}.
     */
    @Test
    public void testGetNodes() {
        MemoryDataSet map = new MemoryDataSet();
        WayHelper subject = new WayHelper(map);

        Way way0 = new Way(1L, 0, new Date(), null, 0);
        way0.getWayNodes().add(new WayNode(1L));
        way0.getWayNodes().add(new WayNode(2L));
        Node node0 = new Node(1L, 0, new Date(), null, 0, 1.0, 1.0);
        Node node1 = new Node(2L, 0, new Date(), null, 0, 2.0, 2.0);
        map.addNode(node0);
        map.addNode(node1);
        map.addWay(way0);
        List<Node> nodes = subject.getNodes(way0);
        assertNotNull(nodes);
        assertEquals(nodes.size(), 2);
        Node n0 = nodes.get(0);
        Node n1 = nodes.get(1);
        assertNotNull(n0);
        assertNotNull(n0);
        if (n0 == node0) {
            assertSame(node1, n1);
        } else {
            assertSame(node1, n0);
        }
    }

    /**
     * Test method for {@link org.openstreetmap.osm.data.WayHelper#simplifyWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)}.
     */
    @Test
    public void testSimplifyWayWay() {
        MemoryDataSet map = new MemoryDataSet();
        WayHelper subject = new WayHelper(map);

        // create a straight way with too many nodes

        long id = 1L;
        final int nodeCount = 10;
        Way way0 = new Way(id++, 0, new Date(), null, 0);
        for (int i = 0; i<nodeCount; i++) {
            Node node0 = new Node(id, 0, new Date(), null,
                            0,
                            1.0, 1.0 + nodeCount);
            way0.getWayNodes().add(new WayNode(node0.getId()));
            map.addNode(node0);
            id++;
        }
        map.addWay(way0);

        assertEquals("created way has wrong number of waynodes",
                      nodeCount, way0.getWayNodes().size());
        assertEquals("created way has wrong number of nodes",
                nodeCount, subject.getNodes(way0).size());

        List<WayNode> simplifiedWay = subject.simplifyWay(way0);

        assertEquals("simplified way has wrong number of nodes",
                     2, simplifiedWay.size());
        assertEquals("simplified way has wrong start-node",
                2L, simplifiedWay.get(0).getNodeId());
        assertEquals("simplified way has wrong end-node",
                nodeCount + 1, simplifiedWay.get(1).getNodeId());
    }

    /**
     * Test method for {@link org.openstreetmap.osm.data.WayHelper#simplifyWay(org.openstreetmap.osmosis.core.domain.v0_5.Way, double, boolean)}.
     */
    @Test
    public void testSimplifyWayWayDoubleBoolean() {

    }

    /**
     * Test method for {@link org.openstreetmap.osm.data.WayHelper#simplifyWayRange(java.util.List, int, int, double)}.
     */
    @Test
    public void testSimplifyWayRange() {

    }

    /**
     * Test method for getDistance(node, start,end).
     */
    @Test
    public void testgetDistanceNodeLine() {
        Node start = new Node(1, 1, new Date(), null, 0, 1.0, 1.0);
        Node test = new Node(2, 1, new Date(), null, 0, 2.0, 5.0);
        Node testOnLine = new Node(5, 1, new Date(), null, 0, 1.0, 5.0);
        Node end = new Node(3, 1, new Date(), null, 0, 1.0, 10.0);
        final double expected = LatLon.distanceInMeters(test, testOnLine);
        double actual = WayHelper.getDistance(test, start, end);
        assertEquals(expected, actual, 0.0001d);
    }

    /**
     * Test method for getDistance(node, start,end).
     */
    @Test
    public void testgetDistanceNodeWay() {
        Node start = new Node(1, 1, new Date(), null, 0, 1.0, 1.0);
        Node test = new Node(2, 1, new Date(), null, 0, 2.0, 5.0);
        Node testOnLine = new Node(5, 1, new Date(), null, 0, 1.0, 5.0);
        Node end = new Node(3, 1, new Date(), null, 0, 1.0, 10.0);
        Node afterEnd = new Node(4, 1, new Date(), null, 0, 1.0, 20.0);
        final double expected = LatLon.distance(test, testOnLine);
        List<Node> way = new ArrayList<Node>();
        way.add(start);
        way.add(end);
        way.add(afterEnd);
//        double actual = WayHelper.getDistance(test, way);
//        assertEquals(expected, actual, 0.0001d);
//        assertEquals(4, way.size());
//        assertEquals(start.getId(), way.get(0).getId());
//        assertEquals(test.getId(), way.get(1).getId());
//        assertEquals(end.getId(), way.get(2).getId());
//        assertEquals(afterEnd.getId(), way.get(3).getId());   
    }

    /**
     * Test method for getDistance(node, start,end).
     */
    @Test
    public void testgetDistanceNodeAfterWay() {
        Node start = new Node(1, 1, new Date(), null, 0, 1.0, 1.0);
        Node test = new Node(2, 1, new Date(), null, 0, 2.0, 10.0);
        Node end = new Node(3, 1, new Date(), null, 0, 1.0, 5.0);
        final double expected = LatLon.distance(test, end);
        List<Node> way = new ArrayList<Node>();
        way.add(start);
        way.add(end);
//        double actual = WayHelper.getDistance(test, way);
//        assertEquals(expected, actual, 0.0001d);
//        assertEquals(3, way.size());
//        assertEquals(start.getId(), way.get(0).getId());
//        assertEquals(end.getId(), way.get(1).getId());
//        assertEquals(test.getId(), way.get(2).getId());
    }

    /**
     * Test method for getDistance(node, start,end).
     */
    @Test
    public void testIsCompletelyParallel() {
        Node start     = new Node(1, 1, new Date(), null, 0, 1.0, 1.0);
        Node testStart = new Node(2, 1, new Date(), null, 0, 1.00001, 5.0);
        Node testEnd   = new Node(3, 1, new Date(), null, 0, 1.00001, 6.0);
        Node end       = new Node(4, 1, new Date(), null, 0, 1.0, 10.0);
        Node afterEnd  = new Node(5, 1, new Date(), null, 0, 1.0, 20.0);

        List<Node> existingWay = new ArrayList<Node>();
        existingWay.add(start);
        existingWay.add(end);
        existingWay.add(afterEnd);
        List<Node> addmeWay = new ArrayList<Node>();
        addmeWay.add(testStart);
        addmeWay.add(testEnd);
        List<Node> actual = WayHelper.isParallel(existingWay, addmeWay);
        assertNotNull(actual);
        assertEquals(5, actual.size());
        assertEquals(start.getId(), actual.get(0).getId());
        assertEquals(testStart.getId(), actual.get(1).getId());
        assertEquals(testEnd.getId(), actual.get(2).getId());
        assertEquals(end.getId(), actual.get(3).getId());
        assertEquals(afterEnd.getId(), actual.get(4).getId());

        // now the same with the direction reversed
        existingWay = new ArrayList<Node>();
        existingWay.add(start);
        existingWay.add(end);
        existingWay.add(afterEnd);
        List<Node> addmeWayReversed = new ArrayList<Node>();
        addmeWayReversed.add(testEnd);
        addmeWayReversed.add(testStart);
        actual = WayHelper.isParallel(existingWay, addmeWay);
        assertNotNull(actual);
        assertEquals(5, actual.size());//ids = 1,2,2,3,3,4,5
        assertEquals(start.getId(), actual.get(0).getId());
        assertEquals(testStart.getId(), actual.get(1).getId());
        assertEquals(testEnd.getId(), actual.get(2).getId());
        assertEquals(end.getId(), actual.get(3).getId());
        assertEquals(afterEnd.getId(), actual.get(4).getId());

    }
    /**
     * Test method for getDistance(node, start,end).
     */
    @Test
    public void testIsOverlappingParallelLeft() {
        Node testStart = new Node(1, 1, new Date(), null, 0, 1.00001, 1.00001);
        Node start     = new Node(2, 1, new Date(), null, 0, 1.0, 1.0);
        Node testEnd   = new Node(3, 1, new Date(), null, 0, 1.00001, 6.0);
        Node end       = new Node(4, 1, new Date(), null, 0, 1.0, 10.0);
        Node afterEnd  = new Node(5, 1, new Date(), null, 0, 1.0, 20.0);

        List<Node> existingWay = new ArrayList<Node>();
        existingWay.add(start);
        existingWay.add(end);
        existingWay.add(afterEnd);
        List<Node> addmeWay = new ArrayList<Node>();
        addmeWay.add(testStart);
        addmeWay.add(testEnd);
        List<Node> actual = WayHelper.isParallel(existingWay, addmeWay);
        assertNotNull(actual);
        assertEquals(5, actual.size());
        assertEquals(start.getId(),     actual.get(0).getId());
        assertEquals(testStart.getId(), actual.get(1).getId());
        assertEquals(testEnd.getId(),   actual.get(2).getId());
        assertEquals(end.getId(),       actual.get(3).getId());
        assertEquals(afterEnd.getId(),  actual.get(4).getId());
    }

    /**
     * Test method for getDistance(node, start,end).
     */
    @Test
    public void testIsOverlappingParallelLeftReversed() {
        Node testStart = new Node(1, 1, new Date(), null, 0, 1.00001, 1.00001);
        Node start     = new Node(2, 1, new Date(), null, 0, 1.0, 1.0);
        Node testEnd   = start;//new Node(3, 1, new Date(), null, 0, 1.00001, 6.0);
        Node end       = new Node(4, 1, new Date(), null, 0, 1.0, 10.0);
        Node afterEnd  = new Node(5, 1, new Date(), null, 0, 1.0, 20.0);

        List<Node> existingWay = new ArrayList<Node>();
        existingWay.add(start);
        existingWay.add(end);
        existingWay.add(afterEnd);
        List<Node> addmeWay = new ArrayList<Node>();
        addmeWay.add(testEnd);
        addmeWay.add(testStart);
        List<Node> actual = WayHelper.isParallel(existingWay, addmeWay);
        assertNotNull(actual);
        assertEquals(4, actual.size());
        assertEquals(start.getId(),     actual.get(0).getId());
        assertEquals(testStart.getId(), actual.get(1).getId());
        assertEquals(end.getId(),   actual.get(2).getId());
        //assertEquals(testEnd.getId(),       actual.get(2).getId());
        assertEquals(afterEnd.getId(),  actual.get(3).getId());
    }

    /**
     * Test method for getDistance(node, start,end).
     */
    @Test
    public void testIsNotParallelLeft() {
        Node testStart = new Node(1, 1, new Date(), null, 0, 1.00001, -5.0);
        Node testEnd   = new Node(3, 1, new Date(), null, 0, 1.00001, 0.0);
        Node start     = new Node(2, 1, new Date(), null, 0, 1.0, 1.0);
        Node end       = new Node(4, 1, new Date(), null, 0, 1.0, 10.0);
        Node afterEnd  = new Node(5, 1, new Date(), null, 0, 1.0, 20.0);

        List<Node> existingWay = new ArrayList<Node>();
        existingWay.add(start);
        existingWay.add(end);
        existingWay.add(afterEnd);
        List<Node> addmeWay = new ArrayList<Node>();
        addmeWay.add(testStart);
        addmeWay.add(testEnd);
        List<Node> actual = WayHelper.isParallel(existingWay, addmeWay);
        assertNull(actual);
    }

    /**
     * Test method for getDistance(node, start,end).
     */
    @Test
    public void testIsOverlappingParallelRight() {
        Node start     = new Node(1, 1, new Date(), null, 0, 1.0, 5.0);
        Node testStart = new Node(2, 1, new Date(), null, 0, 1.00001, 5.0);
        Node end       = new Node(3, 1, new Date(), null, 0, 1.0, 10.0);
        Node afterEnd  = new Node(4, 1, new Date(), null, 0, 1.0, 20.0);
        Node testEnd   = new Node(5, 1, new Date(), null, 0, 1.00001, 30.0);
        Node testEnd2  = new Node(6, 1, new Date(), null, 0, 1.00001, 35.0);
        
        List<Node> existingWay = new ArrayList<Node>();
        existingWay.add(start);
        existingWay.add(end);
        existingWay.add(afterEnd);
        List<Node> addmeWay = new ArrayList<Node>();
        addmeWay.add(testStart);
        addmeWay.add(testEnd);
        addmeWay.add(testEnd2);
        List<Node> actual = WayHelper.isParallel(existingWay, addmeWay);
        assertNotNull(actual);
        assertEquals(6, actual.size());
        assertEquals(start.getId(), actual.get(5).getId());
        assertEquals(testStart.getId(), actual.get(4).getId());
        assertEquals(end.getId(), actual.get(3).getId());
        assertEquals(afterEnd.getId(), actual.get(2).getId());
        assertEquals(testEnd.getId(), actual.get(1).getId());
        assertEquals(testEnd2.getId(), actual.get(0).getId());
    }
    /**
     * Test method for getDistance(node, start,end).
     */
    @Test
    public void testIsNotParallelRight() {
        Node start     = new Node(1, 1, new Date(), null, 0, 1.0, 1.0);
        Node end       = new Node(3, 1, new Date(), null, 0, 1.0, 10.0);
        Node afterEnd  = new Node(4, 1, new Date(), null, 0, 1.0, 20.0);
        Node testStart = new Node(2, 1, new Date(), null, 0, 1.00001, 21.0);
        Node testEnd   = new Node(5, 1, new Date(), null, 0, 1.00001, 30.0);
        Node testEnd2  = new Node(6, 1, new Date(), null, 0, 1.00001, 35.0);
        
        List<Node> existingWay = new ArrayList<Node>();
        existingWay.add(start);
        existingWay.add(end);
        existingWay.add(afterEnd);
        List<Node> addmeWay = new ArrayList<Node>();
        addmeWay.add(testStart);
        addmeWay.add(testEnd);
        addmeWay.add(testEnd2);
        List<Node> actual = WayHelper.isParallel(existingWay, addmeWay);
        assertNull(actual);
    }

}


