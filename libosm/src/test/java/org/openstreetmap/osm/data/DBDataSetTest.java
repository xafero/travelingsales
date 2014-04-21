/**
 *
 */
package org.openstreetmap.osm.data;

//import java.io.File;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.logging.ConsoleHandler;
//import java.util.logging.Handler;
//import java.util.logging.Level;
//import java.util.logging.Logger;

import junit.framework.TestCase;

//import org.openstreetmap.osm.data.coordinates.Bounds;
//import org.openstreetmap.osm.io.FileLoader;
//
//import org.openstreetmap.osmosis.core.database.DatabaseLoginCredentials;
//import org.openstreetmap.osmosis.core.domain.v0_5.Node;
//import org.openstreetmap.osmosis.core.domain.v0_5.Way;

/**
 * Test the {@link DBDataSet} by comparing it to the {@link MemoryDataSet}.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class DBDataSetTest extends TestCase {
//
//    /**
//     * An instance that is known to work.
//     */
//    private MemoryDataSet compareTo;
//
//    /**
//     * The instance we are testing.
//     */
//    private DBDataSet subject;
//
//    /**
//     * ${@inheritDoc}.
//     * @see junit.framework.TestCase#setUp()
//     */
//    protected void setUp() throws Exception {
//        super.setUp();
//        FileLoader loader = new FileLoader(new File("../osmnavigation/testdata/FreiburgA5Karlsruhe-2007-09-29_OSMv0.5.osm"));
//        compareTo =  loader.parseOsm();
//        subject = new DBDataSet(new DatabaseLoginCredentials("localhost", "osm", "osm", ""));
//
//        //--------- logging
//        Logger rootLogger = Logger.getLogger("");
//
//        // Remove any existing handlers.
//        for (Handler handler : rootLogger.getHandlers()) {
//            rootLogger.removeHandler(handler);
//        }
//
//        // Add a new console handler.
//        Handler consoleHandler = new ConsoleHandler();
//        consoleHandler.setLevel(Level.ALL);
//        rootLogger.addHandler(consoleHandler);
//        Logger.getLogger("").setLevel(Level.ALL);
//        Logger.getLogger("sun").setLevel(Level.WARNING);
//        Logger.getLogger("com.sun").setLevel(Level.WARNING);
//        Logger.getLogger("java").setLevel(Level.WARNING);
//        Logger.getLogger("javax").setLevel(Level.WARNING);
//    }
//
//    /**
//     * ${@inheritDoc}.
//     */
//    protected void tearDown() throws Exception {
//        super.tearDown();
//    }
//
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#addNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)}.
////     */
////    public void testAddNode() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#addRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)}.
////     */
////    public void testAddRelation() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#addWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)}.
////     */
////    public void testAddWay() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#containsNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)}.
////     */
////    public void testContainsNode() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#containsRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)}.
////     */
////    public void testContainsRelation() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#containsWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)}.
////     */
////    public void testContainsWay() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#getNearestNode(org.openstreetmap.osm.data.coordinates.LatLon, org.openstreetmap.osm.data.Selector)}.
////     */
////    public void testGetNearestNode() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#getNodeByID(long)}.
////     */
////    public void testGetNodeByID() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#getNodesByName(java.lang.String)}.
////     */
////    public void testGetNodesByName() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#getRelationByID(long)}.
////     */
////    public void testGetRelationByID() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#getWayHelper()}.
////     */
////    public void testGetWayHelper() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#getWaysByID(long)}.
////     */
////    public void testGetWaysByID() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#getWaysByName(java.lang.String)}.
////     */
////    public void testGetWaysByName() {
////        fail("Not yet implemented");
////    }
//
//    /**
//     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#getWaysForNode(long)}.
//     */
//    public void testGetWaysForNode() {
//
//        // compare the result of both implementations for all nodes.
//        Iterator<Node> nodes = this.compareTo.getNodes(Bounds.WORLD);
//        long nodesCount = 0;
//        long brokenCount = 0;
//
//        //CachingDataSet cache = new CachingDataSet(this.subject);
//
//        while (nodes.hasNext()) {
//            Node next = nodes.next();
//            nodesCount++;
//
//            Iterator<Way> other = this.compareTo.getWaysForNode(next.getId());
//            Iterator<Way> mine  = subject.getWaysForNode(next.getId());
//            int count = 0;
//            String names = "";
//            Map<Long, Way> otherID2Way = new HashMap<Long, Way>();
//            Map<Long, Way> myID2Way = new HashMap<Long, Way>();
//
//            //////////////////
//            // read all ways for the nodes
//            while (other.hasNext()) {
//                Way otherWay = other.next();
//                otherID2Way.put(otherWay.getId(), otherWay);
//                names += "/" + WayHelper.getTag(otherWay, "name") + "(" + WayHelper.getTag(otherWay, "ref") + ")";
//            }
//            while (mine.hasNext()) {
//                Way myWay = mine.next();
//                myID2Way.put(myWay.getId(), myWay);
//            }
//
//            //////////////////
//            // compare if the sets of way-ids are equal
//            Long[] otherArray = otherID2Way.keySet().toArray(new Long[count]);
//            Arrays.sort(otherArray);
//            Long[] myArray = myID2Way.keySet().toArray(new Long[count]);
//            Arrays.sort(myArray);
//
//            assertTrue("getWaysForNode gives different results "
//                    + "for nodeID=" + next.getId()
//                    + " then the reference-implementation\n"
//                    + "gave: " + Arrays.toString(myArray) + "\n"
//                    + "expected: " + Arrays.toString(otherArray) + "\n"
//                    + " (so far " + brokenCount + " out of " + nodesCount + " nodes had a different result!)\n"
//                    + "ways so far: " + names,
//                    Arrays.equals(otherArray, myArray));
//
//        }
//        System.err.println(brokenCount + " out of " + nodesCount + " nodes had a different result!");
//        assertEquals(brokenCount, 0);
//    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#removeNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)}.
////     */
////    public void testRemoveNode() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#removeRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)}.
////     */
////    public void testRemoveRelation() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#removeWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)}.
////     */
////    public void testRemoveWay() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#visitAll(org.openstreetmap.osm.data.visitors.Visitor, org.openstreetmap.osm.data.coordinates.Bounds)}.
////     */
////    public void testVisitAll() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#getNodes(org.openstreetmap.osm.data.coordinates.Bounds)}.
////     */
////    public void testGetNodes() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#getRelations(org.openstreetmap.osm.data.coordinates.Bounds)}.
////     */
////    public void testGetRelations() {
////        fail("Not yet implemented");
////    }
////
////    /**
////     * Test method for {@link org.openstreetmap.osm.data.DBDataSet#getWays(org.openstreetmap.osm.data.coordinates.Bounds)}.
////     */
////    public void testGetWays() {
////        fail("Not yet implemented");
////    }
//
    /**
     * Test nothing.
     */
    public void testDummy() {
    }
}
