/**
 * H2DataSetTest.java
 * created: 12.11.2009
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

//other imports
import static org.junit.Assert.*;


//automatically created logger for debug and error -output
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

//automatically created propertyChangeListener-Support
import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.h2.H2DataSet;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * H2DataSetTest.java<br/>
 * created: 12.11.2009 <br/>
 *<br/><br/>
 * <b>Write some testdata into an H2DataSet and retrieve it.</b>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class H2DataSetTest {

    /**
     * A small location offset.
     */
    private static final double OFFSET = 0.001d;

    /**
     * coordinates are allowed to change this much
     * during saving.
     */
    private static final double ALLOWEDCOORDINATEDELTA = 0.0001;

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(H2DataSetTest.class
            .getName());

    /**
     * The class we are testing.
     */
    private H2DataSet mySubject;

    /**
     * A temporary directory for our subject to use.
     */
    private File myTempDirectory;

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "H2DataSetTest@" + hashCode();
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        this.myTempDirectory = File.createTempFile("H2DataSetTest", null);
        if (this.myTempDirectory.exists()) {
            this.myTempDirectory.delete();
            this.myTempDirectory.mkdirs();
        }
        this.mySubject = new H2DataSet(this.myTempDirectory);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() {
        if (mySubject != null) {
            this.mySubject.shutdown();
            this.mySubject = null;
        }
        if (this.myTempDirectory.exists()) {
            File[] list = this.myTempDirectory.listFiles();
            for (File file : list) {
                file.delete();
            }
            this.myTempDirectory.delete();
        }
    }

    /**
     * Clean up all temporary dirs.
     * Restore changed values.
     * @throws InterruptedException 
     */
    @AfterClass
    public static void tearDownSharedFixture() throws InterruptedException {
        try {
            File tmpFile = File.createTempFile("H2DataSetTest", null);
            File tmpFolder = tmpFile.getParentFile();
            File[] list = tmpFolder.listFiles();
            for (File folder : list) {
                String filename = folder.getName();
                if (filename.matches("H2DataSetTest\\d*\\.tmp")) {
                    if (folder.isDirectory()) {
                        File[] files = folder.listFiles();
                            for (File file : files) {
                                file.delete();
                            }
                    }
                    folder.delete();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test method for {@link org.openstreetmap.osm.data.OsmBinDataSet#addNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)}.
     */
    @Test
    public void testSingleNode() {
        final double lat = 0.1d;
        final double lon = 0.1d;
        Node testNode = new Node((long) 1, 0, new Date(), null, 0, lat, lon);
        testNode.getTags().add(new Tag("highway", "primary"));
        
        assertFalse(mySubject.containsNode(testNode));
        this.mySubject.addNode(testNode);
        assertTrue(mySubject.containsNode(testNode));
        assertNull("getNodeByID returned a node with a wrong id", this.mySubject.getNodeByID((long) 2));

        Node rereat = mySubject.getNodeByID(1l);
        assertEquals("Id changed after reading and writing", testNode.getId(), rereat.getId());
        assertEquals("Latitude changed after reading and writing", testNode.getLatitude(), rereat.getLatitude(), ALLOWEDCOORDINATEDELTA);
        assertEquals("Longitude changed after reading and writing", testNode.getLongitude(), rereat.getLongitude(), ALLOWEDCOORDINATEDELTA);
        assertEquals("attribute-count changed after reading and writing", testNode.getTags().size(), rereat.getTags().size());
        assertEquals("attribute-name changed after reading and writing", testNode.getTags().iterator().next().getKey(), rereat.getTags().iterator().next().getKey());
        assertEquals("attribute-value changed after reading and writing", testNode.getTags().iterator().next().getValue(), rereat.getTags().iterator().next().getValue());

        Node nearestNode = mySubject.getNearestNode(new LatLon(testNode.getLatitude(), testNode.getLongitude()), null);
        assertNotNull(nearestNode);

        nearestNode = mySubject.getNearestNode(new LatLon(testNode.getLatitude() + OFFSET, testNode.getLongitude() - OFFSET), null);
        assertNotNull(nearestNode);

        mySubject.removeNode(testNode);
        assertFalse(mySubject.containsNode(testNode));
        assertNull("getNodeByID returned a deleted node", this.mySubject.getNodeByID((long) 1));

        // add again. This time use free space instead of file-growing
        this.mySubject.addNode(testNode);
        assertTrue(mySubject.containsNode(testNode));
        rereat = mySubject.getNodeByID(1l);
        assertEquals("Id changed after reading and writing a second time", testNode.getId(), rereat.getId());
        assertEquals("Latitude changed after reading and writing a second time", testNode.getLatitude(), rereat.getLatitude(), ALLOWEDCOORDINATEDELTA);
        assertEquals("Longitude changed after reading and writing a second time", testNode.getLongitude(), rereat.getLongitude(), ALLOWEDCOORDINATEDELTA);
        assertEquals("attribute-count changed after reading and writing a second time", testNode.getTags().size(), rereat.getTags().size());
        assertEquals("attribute-name changed after reading and writing a second time",
                testNode.getTags().iterator().next().getKey(), rereat.getTags().iterator().next().getKey());
        assertEquals("attribute-value changed after reading and writing a second time",
                testNode.getTags().iterator().next().getValue(), rereat.getTags().iterator().next().getValue());

        Iterator<Node> nodes2D = this.mySubject.getNodes(new Bounds(0.01d, 0.01d, 0.2, 0.2d));
        assertNotNull(nodes2D);
        assertTrue(nodes2D.hasNext());
        Node node2D = nodes2D.next();
        assertNotNull(node2D);
        assertFalse(nodes2D.hasNext());
        assertEquals(testNode.getId(), node2D.getId());
    }

    /**
     * Test method for {@link org.openstreetmap.osm.data.OsmBinDataSet#addNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)}.
     */
        @Test
        public void testManyAttrsWay() {
            final long testnodeID = 10;
            Node testNode = new Node(testnodeID, 0, new Date(), null, 0, 0.1d, 0.2d);
            testNode.getTags().add(new Tag("AAA", "aaa"));
            testNode.getTags().add(new Tag("BBB", "bbb"));
            testNode.getTags().add(new Tag("CCC", "ccc"));
            
            Node testNode2 = new Node(testnodeID + 1, 0, new Date(), null, 0, 0.3d, 0.4d);
            testNode2.getTags().add(new Tag("A2AA", "a2aa"));
            testNode2.getTags().add(new Tag("B2BB", "b2bb"));
            testNode2.getTags().add(new Tag("C2CC", "c2cc"));
            testNode2.getTags().add(new Tag("D2DD", "d2ddd"));
            testNode2.getTags().add(new Tag("E2E", "e2ee"));

            Way testWay = new Way(testnodeID + 3, 0, new Date(), null, 0);
            testWay.getWayNodes().add(new WayNode(testnodeID));
            testWay.getWayNodes().add(new WayNode(testnodeID + 1));

            Way testWay2 = new Way(testnodeID + 4, 0, new Date(), null, 0);
            testWay2.getWayNodes().add(new WayNode(testnodeID));
            testWay2.getWayNodes().add(new WayNode(testnodeID + 1));

            Way testWay3 = new Way(testnodeID + 5, 0, new Date(), null, 0);
            testWay3.getWayNodes().add(new WayNode(testnodeID));
            testWay3.getWayNodes().add(new WayNode(testnodeID + 1));

            Way testWay4 = new Way(testnodeID + 6, 0, new Date(), null, 0);
            testWay4.getWayNodes().add(new WayNode(testnodeID));
            testWay4.getWayNodes().add(new WayNode(testnodeID + 1));

            Way testWay5 = new Way(testnodeID + 7, 0, new Date(), null, 0);
            testWay5.getWayNodes().add(new WayNode(testnodeID));
            testWay5.getWayNodes().add(new WayNode(testnodeID + 1));
            assertFalse(mySubject.containsNode(testNode));
            assertFalse(mySubject.containsNode(testNode2));

            this.mySubject.addNode(testNode);
            this.mySubject.addNode(testNode2);
            this.mySubject.addWay(testWay);
            this.mySubject.addWay(testWay2);
            this.mySubject.addWay(testWay3);
            this.mySubject.addWay(testWay4);
            this.mySubject.addWay(testWay5);

            assertTrue(mySubject.containsNode(testNode));
            assertTrue(mySubject.containsNode(testNode2));

            Node rereat = mySubject.getNodeByID(testnodeID);
            Node rereat2 = mySubject.getNodeByID(testnodeID + 1);
            Iterator<Way> waysForNode1 = mySubject.getWaysForNode(testnodeID);
            assertTrue(waysForNode1.hasNext()); assertNotNull(waysForNode1.next());
            assertTrue(waysForNode1.hasNext()); assertNotNull(waysForNode1.next());
            assertTrue(waysForNode1.hasNext()); assertNotNull(waysForNode1.next());
            assertTrue(waysForNode1.hasNext()); assertNotNull(waysForNode1.next());
            assertTrue(waysForNode1.hasNext()); assertNotNull(waysForNode1.next());
            assertFalse(waysForNode1.hasNext());
            Iterator<Way> waysForNode2 = mySubject.getWaysForNode(testnodeID);
            assertTrue(waysForNode2.hasNext());
            Way rereatWay1 = waysForNode2.next();
            assertNotNull(rereatWay1);
            assertTrue(waysForNode2.hasNext());
            Way rereatWay2 = waysForNode2.next();
            assertNotNull(rereatWay2);
            assertTrue(waysForNode2.hasNext());
            Way rereatWay3 = waysForNode2.next();
            assertNotNull(rereatWay3);
            assertTrue(waysForNode2.hasNext());
            Way rereatWay4 = waysForNode2.next();
            assertNotNull(rereatWay4);
            assertTrue(waysForNode2.hasNext());
            Way rereatWay5 = waysForNode2.next(); assertNotNull(rereatWay5);
            assertFalse("node now has more ways then it should", waysForNode2.hasNext());
            Map<Long, Way> rereatWays = new HashMap<Long, Way>();
            rereatWays.put(rereatWay1.getId(), rereatWay1);
            rereatWays.put(rereatWay2.getId(), rereatWay2);
            rereatWays.put(rereatWay3.getId(), rereatWay3);
            rereatWays.put(rereatWay4.getId(), rereatWay4);
            rereatWays.put(rereatWay5.getId(), rereatWay5);
            for (Way way : rereatWays.values()) {
                assertEquals(2, way.getWayNodes().size());
                assertEquals(0, way.getTags().size());
            }
            assertTrue(rereatWays.containsKey(testWay.getId()));
            assertTrue(rereatWays.containsKey(testWay2.getId()));
            assertTrue(rereatWays.containsKey(testWay3.getId()));
            assertTrue(rereatWays.containsKey(testWay4.getId()));
            assertTrue(rereatWays.containsKey(testWay5.getId()));
            assertEquals("Id changed after reading and writing", testNode.getId(), rereat.getId());
            assertEquals("Latitude changed after reading and writing", testNode.getLatitude(), rereat.getLatitude(), ALLOWEDCOORDINATEDELTA);
            assertEquals("Longitude changed after reading and writing", testNode.getLongitude(), rereat.getLongitude(), ALLOWEDCOORDINATEDELTA);
            assertEquals("attribute-count changed after reading and writing", testNode.getTags().size(), rereat.getTags().size());
            assertEquals("Id changed 2 after reading and writing", testNode2.getId(), rereat2.getId());
            assertEquals("Latitude 2 changed after reading and writing", testNode2.getLatitude(), rereat2.getLatitude(), ALLOWEDCOORDINATEDELTA);
            assertEquals("Longitude 2 changed after reading and writing", testNode2.getLongitude(), rereat2.getLongitude(), ALLOWEDCOORDINATEDELTA);
            assertEquals("attribute-count 2 changed after reading and writing", testNode2.getTags().size(), rereat2.getTags().size());

//            for (int i = 0; i < testNode.getTagList().size(); i++) {
//                assertEquals("attribute-name " + i + " changed after reading and writing", testNode.getTagList().get(0).getKey(), rereat.getTagList().get(0).getKey());
//                assertEquals("attribute-value " + i + " changed after reading and writing", testNode.getTagList().get(0).getValue(), rereat.getTagList().get(0).getValue());
//            }
//            for (int i = 0; i < testNode2.getTags().size(); i++) {
//                assertEquals("attribute-name 2-" + i + " changed after reading and writing", testNode2.getTagList().get(0).getKey(), rereat2.getTagList().get(0).getKey());
//                assertEquals("attribute-value 2-" + i + " changed after reading and writing", testNode2.getTagList().get(0).getValue(), rereat2.getTagList().get(0).getValue());
//            }
        }

    /**
     * Test method for {@link org.openstreetmap.osm.data.OsmBinDataSet#addNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)}.
     */
    @Test
    public void testManyAttrsNode() {
        final long testnodeID = 10;
        Node testNode = new Node(testnodeID, 0, new Date(), null, 0, 0.1d, 0.2d);
        testNode.getTags().add(new Tag("AAA", "aaa"));
        testNode.getTags().add(new Tag("BBB", "bbb"));
        testNode.getTags().add(new Tag("CCC", "ccc"));
        testNode.getTags().add(new Tag("DDD", "dddd"));
        testNode.getTags().add(new Tag("EE", "eee"));

        Node testNode2 = new Node(testnodeID + 1, 0, new Date(), null, 0, 0.3d, 0.4d);
        testNode2.getTags().add(new Tag("A2AA", "a2aa"));
        testNode2.getTags().add(new Tag("B2BB", "b2bb"));
        testNode2.getTags().add(new Tag("C2CC", "c2cc"));
        testNode2.getTags().add(new Tag("D2DD", "d2ddd"));
        testNode2.getTags().add(new Tag("E2E", "e2ee"));
        assertFalse(mySubject.containsNode(testNode));
        assertFalse(mySubject.containsNode(testNode2));

        this.mySubject.addNode(testNode);
        this.mySubject.addNode(testNode2);

        assertTrue(mySubject.containsNode(testNode));
        assertTrue(mySubject.containsNode(testNode2));

        Node rereat = mySubject.getNodeByID(testnodeID);
        Node rereat2 = mySubject.getNodeByID(testnodeID + 1);
        assertEquals("Id changed after reading and writing", testNode.getId(), rereat.getId());
        assertEquals("Latitude changed after reading and writing", testNode.getLatitude(), rereat.getLatitude(), ALLOWEDCOORDINATEDELTA);
        assertEquals("Longitude changed after reading and writing", testNode.getLongitude(), rereat.getLongitude(), ALLOWEDCOORDINATEDELTA);
        assertEquals("attribute-count changed after reading and writing", testNode.getTags().size(), rereat.getTags().size());
        assertEquals("Id changed 2 after reading and writing", testNode2.getId(), rereat2.getId());
        assertEquals("Latitude 2 changed after reading and writing", testNode2.getLatitude(), rereat2.getLatitude(), ALLOWEDCOORDINATEDELTA);
        assertEquals("Longitude 2 changed after reading and writing", testNode2.getLongitude(), rereat2.getLongitude(), ALLOWEDCOORDINATEDELTA);
        assertEquals("attribute-count 2 changed after reading and writing", testNode2.getTags().size(), rereat2.getTags().size());

//        for (int i = 0; i < testNode.getTagList().size(); i++) {
//            assertEquals("attribute-name " + i + " changed after reading and writing", testNode.getTagList().get(0).getKey(), rereat.getTagList().get(0).getKey());
//            assertEquals("attribute-value " + i + " changed after reading and writing", testNode.getTagList().get(0).getValue(), rereat.getTagList().get(0).getValue());
//        }
//        for (int i = 0; i < testNode2.getTagList().size(); i++) {
//            assertEquals("attribute-name 2-" + i + " changed after reading and writing", testNode2.getTagList().get(0).getKey(), rereat2.getTagList().get(0).getKey());
//            assertEquals("attribute-value 2-" + i + " changed after reading and writing", testNode2.getTagList().get(0).getValue(), rereat2.getTagList().get(0).getValue());
//        }
    }

    /**
     * Test method for long key/value pairs.
     */
    @Test
    public void testLongAttrs() {
        final long testnodeID = 20;
        Node longValuesNode = new Node(testnodeID, 0, new Date(), null, 0, 0.5d, 0.6d);
        longValuesNode.getTags().add(new Tag("name", "It's longest tag value for cities and towns on the OpenStreetMap."));

        Node longKeyNode = new Node(testnodeID + 1, 0, new Date(), null, 0, 0.7d, 0.8d);
        longKeyNode.getTags().add(new Tag("It's longest tag key name for all around the World founded on the OpenStreetMap", "and short value for it."));

        assertFalse(mySubject.containsNode(longValuesNode));
        assertFalse(mySubject.containsNode(longKeyNode));
        this.mySubject.addNode(longValuesNode);
        this.mySubject.addNode(longKeyNode);
        Node readedLongNode = mySubject.getNodeByID(testnodeID);
        Node readedLongKeyNode = mySubject.getNodeByID(testnodeID + 1);
        assertTrue(mySubject.containsNode(longValuesNode));
        assertTrue(mySubject.containsNode(longKeyNode));
        assertEquals("long tag value node test", "It's longest tag value for cities and towns on the OpenStreetMap.", readedLongNode.getTags().iterator().next().getValue());
        assertEquals("long tag key node test", "It's longest tag key name for all around the World founded on the OpenStreetMap", readedLongKeyNode.getTags().iterator().next().getKey());
    }

    /**
     * Test for method getNodes(Bounds).
     * Tracker bug issue : ID: 2471163
     * http://sourceforge.net/tracker2/?func=detail&aid=2471163&group_id=203597&atid=986231
     */
    @Test
    public void testGetNodesInBounds() {
        final long testnodeID = 30;
        Node testNode = new Node(testnodeID, 0, new Date(), null, 0, 0.1d, 0.1d);
        testNode.getTags().add(new Tag("tagkey1", "tagvalue1"));
        testNode.getTags().add(new Tag("tagkey2", "tagvalue2"));

        this.mySubject.addNode(testNode);
        Node readedNode = mySubject.getNodeByID(testnodeID);
        Iterator<Node> readedNodesInBound = mySubject.getNodes(new Bounds(new LatLon(0,0), 0.2d));
        Node readedNodeFromBounds = readedNodesInBound.next();
        assertEquals(readedNode.getTags().size(), readedNodeFromBounds.getTags().size());
    }
}


