package org.openstreetmap.osm.data;


import java.util.Date;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

public class LODDataSetTest {

    private LODDataSet mySubject = null;
    private MemoryDataSet myTestBaseDataSet;
    private MemoryDataSet myTestLOD1DataSet;
    private MemoryDataSet myTestLOD2DataSet;
    private MemoryDataSet myTestLOD3DataSet;
    @Before
    public void setUp() throws Exception {
        this.mySubject = new LODDataSet();
        this.myTestBaseDataSet = new MemoryDataSet() {
            /**
             * These help in single-stepping.
             */
            public boolean BASEDATASET = true;
        };
        this.mySubject.setBaseDataSet(this.myTestBaseDataSet);

        this.myTestLOD1DataSet = new MemoryDataSet() {
            /**
             * These help in single-stepping.
             */
            public boolean LOD1DATASET = true;
        };
        this.mySubject.setLOD1DataSet(this.myTestLOD1DataSet);

        this.myTestLOD2DataSet = new MemoryDataSet() {
            /**
             * These help in single-stepping.
             */
            public boolean LOD2DATASET = true;
        };
        this.mySubject.setLOD2DataSet(this.myTestLOD2DataSet);

        this.myTestLOD3DataSet = new MemoryDataSet() {
            /**
             * These help in single-stepping.
             */
            public boolean LOD3DATASET = true;
        };
        this.mySubject.setLOD3DataSet(this.myTestLOD3DataSet);
    }

    @After
    public void tearDown() throws Exception {
        this.mySubject = null;
        this.myTestBaseDataSet = null;
        this.myTestLOD1DataSet = null;
        this.myTestLOD2DataSet = null;
        this.myTestLOD3DataSet = null;
    }

    @Test
    public void testAddRelation() throws Exception {
        Relation testRelation = new Relation(1, 0, new Date(), null, 0);

        Assert.assertEquals(0, this.myTestBaseDataSet.getRelationsCount());
        Assert.assertEquals(0, this.myTestLOD1DataSet.getRelationsCount());
        Assert.assertEquals(0, this.myTestLOD2DataSet.getRelationsCount());
        Assert.assertEquals(0, this.myTestLOD3DataSet.getRelationsCount());

        // all relations should be in the BaseDataSet
        mySubject.addRelation(testRelation);

        Assert.assertEquals(1, this.myTestBaseDataSet.getRelationsCount());
        Assert.assertEquals(0, this.myTestLOD1DataSet.getRelationsCount());
        Assert.assertEquals(0, this.myTestLOD2DataSet.getRelationsCount());
        Assert.assertEquals(0, this.myTestLOD3DataSet.getRelationsCount());

        // type=street relations should be in the other dataSets too
        testRelation = new Relation(1, 0, new Date(), null, 0);
        testRelation.getTags().add(new Tag("type", "street"));
        mySubject.addRelation(testRelation);

        Assert.assertEquals(1, this.myTestBaseDataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD1DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD2DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD3DataSet.getRelationsCount());

        // can we remove relations too?
        mySubject.removeRelation(testRelation);

        Assert.assertEquals(0, this.myTestBaseDataSet.getRelationsCount());
        Assert.assertEquals(0, this.myTestLOD1DataSet.getRelationsCount());
        Assert.assertEquals(0, this.myTestLOD2DataSet.getRelationsCount());
        Assert.assertEquals(0, this.myTestLOD3DataSet.getRelationsCount());

        // members of type SIMPLIFIEDWAYRELATIONROLE should be removed automatically
        testRelation = new Relation(2, 0, new Date(), OsmUser.NONE, 0);
        testRelation.getTags().add(new Tag("type", "street"));
        testRelation.getMembers().add(new RelationMember(1L, EntityType.Way, LODDataSet.SIMPLIFIEDWAYRELATIONROLE));
        mySubject.addRelation(testRelation);

        Assert.assertEquals(1, this.myTestBaseDataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD1DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD2DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD3DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD1DataSet.getRelationByID(2).getMembers().size());
        //this is no longer true Assert.assertTrue(1 != this.myTestLOD1DataSet.getRelationByID(2).getMembers().iterator().next().getMemberId());
    }


    @Test
    public void testAddWay() throws Exception {
        Way testWay = new Way(1, 0, new Date(), null, 0);
        testWay.getTags().add(new Tag("highway", "motorway"));
        testWay.getWayNodes().add(new WayNode(1));
        testWay.getWayNodes().add(new WayNode(2));
        testWay.getWayNodes().add(new WayNode(3));
        Node testNode0 = new Node(1, 0, new Date(), null, 0, 1, 1);
        Node testNode1 = new Node(2, 0, new Date(), null, 0, 1.00001, 1.00001);
        Node testNode2 = new Node(3, 0, new Date(), null, 0, 3, 3);

        mySubject.addNode(testNode0);
        mySubject.addNode(testNode1);
        mySubject.addNode(testNode2);
        mySubject.addWay(testWay);

        Assert.assertEquals(1, this.myTestBaseDataSet.getWaysCount());
        Assert.assertEquals(3, this.myTestBaseDataSet.getNodesCount());

        Assert.assertEquals(1, this.myTestLOD1DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD1DataSet.getWaysCount());
        Assert.assertEquals(2, this.myTestLOD1DataSet.getNodesCount());

        Assert.assertEquals(1, this.myTestLOD2DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD2DataSet.getWaysCount());
        Assert.assertEquals(2, this.myTestLOD2DataSet.getNodesCount());

        Assert.assertEquals(1, this.myTestLOD3DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD3DataSet.getWaysCount());
        Assert.assertEquals(2, this.myTestLOD3DataSet.getNodesCount());
    }

    /**
     * Add 2 connected ways belonging to the same motorway
     * and check that a type=street -relation containing them
     * is automatically generated.
     * @throws Exception
     */
    @Test
    public void testAddCombinedWay() throws Exception {
        Way testWay0 = new Way(1, 0, new Date(), null, 0);
        testWay0.getWayNodes().add(new WayNode(1));
        testWay0.getWayNodes().add(new WayNode(2));
        testWay0.getWayNodes().add(new WayNode(3));
        testWay0.getTags().add(new Tag("highway", "motorway"));
        testWay0.getTags().add(new Tag("ref", "test-motorway"));

        Way testWay1 = new Way(2, 0, new Date(), null, 0);
        testWay1.getWayNodes().add(new WayNode(3));
        testWay1.getWayNodes().add(new WayNode(4));
        testWay1.getTags().add(new Tag("highway", "motorway"));
        testWay1.getTags().add(new Tag("ref", "test-motorway"));
        Node testNode0 = new Node(1, 0, new Date(), null, 0, 1, 1);
        Node testNode1 = new Node(2, 0, new Date(), null, 0, 1.00001, 1.00001);
        Node testNode2 = new Node(3, 0, new Date(), null, 0, 3, 3);
        Node testNode3 = new Node(4, 0, new Date(), null, 0, 4, 4);

        mySubject.addNode(testNode0);
        mySubject.addNode(testNode1);
        mySubject.addNode(testNode2);
        mySubject.addNode(testNode3);
        mySubject.addWay(testWay0);
        mySubject.addWay(testWay1);

        Assert.assertEquals(2, this.myTestBaseDataSet.getWaysCount());
        Assert.assertEquals(4, this.myTestBaseDataSet.getNodesCount());

        Assert.assertEquals(1, this.myTestLOD1DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD1DataSet.getWaysCount()); // contains only the one, simplified way now
        Assert.assertEquals(3, this.myTestLOD1DataSet.getNodesCount());
        
        Assert.assertEquals(1, this.myTestLOD2DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD2DataSet.getWaysCount()); // contains only the one, simplified way now
        Assert.assertEquals(3, this.myTestLOD2DataSet.getNodesCount());

        Assert.assertEquals(1, this.myTestLOD3DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD3DataSet.getWaysCount()); // contains only the one, simplified way now
        Assert.assertEquals(3, this.myTestLOD3DataSet.getNodesCount());
    }
    /**
     * Add 2 connected ways facing in opposite directions belonging to the same motorway
     * and check that a type=street -relation containing them
     * is automatically generated.
     * @throws Exception
     */
    @Test
    public void testAddCombinedWayReverse() throws Exception {
        Way testWay0 = new Way(1, 0, new Date(), null, 0);
        testWay0.getWayNodes().add(new WayNode(1));
        testWay0.getWayNodes().add(new WayNode(2));
        testWay0.getWayNodes().add(new WayNode(3));
        testWay0.getTags().add(new Tag("highway", "motorway"));
        testWay0.getTags().add(new Tag("ref", "test-motorway"));

        Way testWay1 = new Way(2, 0, new Date(), null, 0);
        testWay1.getWayNodes().add(new WayNode(4));
        testWay1.getWayNodes().add(new WayNode(3));
        testWay1.getTags().add(new Tag("highway", "motorway"));
        testWay1.getTags().add(new Tag("ref", "test-motorway"));
        Node testNode0 = new Node(1, 0, new Date(), null, 0, 1, 1);
        Node testNode1 = new Node(2, 0, new Date(), null, 0, 1.00001, 1.00001);
        Node testNode2 = new Node(3, 0, new Date(), null, 0, 3, 3);
        Node testNode3 = new Node(4, 0, new Date(), null, 0, 4, 4);

        mySubject.addNode(testNode0);
        mySubject.addNode(testNode1);
        mySubject.addNode(testNode2);
        mySubject.addNode(testNode3);
        mySubject.addWay(testWay0);
        mySubject.addWay(testWay1);

        Assert.assertEquals(2, this.myTestBaseDataSet.getWaysCount());
        Assert.assertEquals(4, this.myTestBaseDataSet.getNodesCount());

        Assert.assertEquals(1, this.myTestLOD1DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD1DataSet.getWaysCount()); // contains only the one, simplified way now
        Assert.assertEquals(3, this.myTestLOD1DataSet.getNodesCount());
        
        Assert.assertEquals(1, this.myTestLOD2DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD2DataSet.getWaysCount()); // contains only the one, simplified way now
        Assert.assertEquals(3, this.myTestLOD2DataSet.getNodesCount());

        Assert.assertEquals(1, this.myTestLOD3DataSet.getRelationsCount());
        Assert.assertEquals(1, this.myTestLOD3DataSet.getWaysCount()); // contains only the one, simplified way now
        Assert.assertEquals(3, this.myTestLOD3DataSet.getNodesCount());
    }
}
