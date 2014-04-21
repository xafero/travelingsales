///**
// * This file is part of LibOSM by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
// * You can purchase support for a sensible hourly rate or
// * a commercial license of this file (unless modified by others) by contacting him directly.
// *
// *  LibOSM is free software: you can redistribute it and/or modify
// *  it under the terms of the GNU General Public License as published by
// *  the Free Software Foundation, either version 3 of the License, or
// *  (at your option) any later version.
// *
// *  LibOSM is distributed in the hope that it will be useful,
// *  but WITHOUT ANY WARRANTY; without even the implied warranty of
// *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// *  GNU General Public License for more details.
// *
// *  You should have received a copy of the GNU General Public License
// *  along with LibOSM.  If not, see <http://www.gnu.org/licenses/>.
// *
// ***********************************
// * Editing this file:
// *  -For consistent code-quality this file should be checked with the
// *   checkstyle-ruleset enclosed in this project.
// *  -After the design of this file has settled it should get it's own
// *   JUnit-Test that shall be executed regularly. It is best to write
// *   the test-case BEFORE writing this class and to run it on every build
// *   as a regression-test.
// */
//package org.openstreetmap.osm.io;
//
//import java.util.Collection;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Map;
//
//import org.openstreetmap.osm.Tags;
//import org.openstreetmap.osm.data.MemoryDataSet;
//import org.openstreetmap.osm.data.TileCalculator;
//import org.openstreetmap.osm.data.mysql.ConstrainedCurrentNodeReader;
//import org.openstreetmap.osm.data.mysql.ConstrainedCurrentRelationReader;
//import org.openstreetmap.osm.data.mysql.ConstrainedCurrentWayReader;
//import org.openstreetmap.osm.data.mysql.ConstrainedCurrentNodeReader.COMPARESTYLE;
//import org.openstreetmap.osm.data.mysql.ConstrainedCurrentNodeReader.CONDITION;
//
//import org.openstreetmap.osmosis.core.database.DatabaseLoginCredentials;
//import org.openstreetmap.osmosis.core.database.DatabasePreferences;
//import org.openstreetmap.osmosis.core.domain.v0_6.Node;
//import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
//import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
//import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
//import org.openstreetmap.osmosis.core.domain.v0_6.Way;
//import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
//import org.openstreetmap.osmosis.core.mysql.common.DatabaseContext;
//import org.openstreetmap.osmosis.core.util.FixedPrecisionCoordinateConvertor;
//import org.openstreetmap.osmosis.core.mysql.v0_6.MysqlReader;
//import org.openstreetmap.osmosis.core.mysql.v0_6.impl.CurrentNodeReader;
//import org.openstreetmap.osmosis.core.mysql.v0_6.impl.CurrentWayReader;
//import org.openstreetmap.osmosis.core.mysql.v0_5.impl.EmbeddedTagProcessor;
//import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
//
///**
// * Helper-class to load <a hef="http://wiki.openstreetmap.org/index.php/Develop">OpenStreetMap</a>-data from a database.
// * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
// */
//public class MysqlDatabaseLoader implements IDatabaseLoader {
//
//    /**
//     * The database-login we are using.
//     */
//    private DatabaseLoginCredentials myCredentials;
//
//    /**
//     * A database-connection used to write to the database.
//     */
//    private DatabaseContext mydatabaseContext;
//
//    /**
//     * Do perform validation.
//     */
//    private DatabasePreferences myDBPreferences;
//
//    /**
//     * Since we can reinitialize it, we cache our
//     * WayReader.
//     */
//    private ConstrainedCurrentWayReader myWayLoader = null;
//
//    /**
//     * Since we can reinitialize it, we cache our
//     * NodeReader.
//     */
//    private ConstrainedCurrentNodeReader myNodeLoader = null;
//
//    /**
//     * Since we can reinitialize it, we cache our
//     * RelationReader.
//     */
//    private ConstrainedCurrentRelationReader myRelationLoader = null;
//
//    /**
//     * @param aCredentials The database-login we are using.
//     * @param aPreferences the preferences to use
//     */
//    public MysqlDatabaseLoader(final DatabaseLoginCredentials aCredentials, final DatabasePreferences aPreferences) {
//        myCredentials = aCredentials;
//        myDBPreferences = aPreferences;
//    }
//
//    /**
//     * Use the local database "osm" with the user "osm" and no password.
//     */
//    public MysqlDatabaseLoader() {
//        myCredentials = new DatabaseLoginCredentials("localhost", "osm", "osm", "", false, false);
//        myDBPreferences = new DatabasePreferences(true, false);
//    }
//
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadBoundingBox(double, double, double, double)
//     */
//    public MemoryDataSet loadBoundingBox(final double minLat, final double minLon, final double maxLat, final double maxLon) {
//
//        MysqlReader reader;
//        if (minLat != Double.MIN_VALUE || maxLat != Double.MAX_VALUE
//                || minLon != Double.MIN_VALUE || maxLon != Double.MAX_VALUE) {
//                   // if a bounding-box is given
//            /*reader = new MysqlReader(myCredentials,
//                    minLat, minLon, maxLat, maxLon);*/
//            reader = new MysqlReader(myCredentials, new DatabasePreferences(true, false), new Date(), true);
//        } else {
//            reader = new MysqlReader(myCredentials, myDBPreferences, new Date(), false);
//        }
//        DataSetSink sink = new DataSetSink();
//        reader.setSink(sink);
//        reader.run();
//
//        return (MemoryDataSet) sink.getDataSet();
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadPlanet()
//     */
//    public MemoryDataSet loadPlanet() {
//
//        MysqlReader reader = new MysqlReader(myCredentials, myDBPreferences, new Date(), false);
//        DataSetSink sink = new DataSetSink();
//        reader.setSink(sink);
//        reader.run();
//
//        return (MemoryDataSet) sink.getDataSet();
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadAllNodes()
//     */
//    public ReleasableIterator<Node> loadAllNodes() {
//        CurrentNodeReader reader = new CurrentNodeReader(myCredentials, false);
//        return reader;
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadAllNodesInBoundingBox(double, double, double, double)
//     */
//    public ReleasableIterator<Node> loadAllNodesInBoundingBox(final double minLat, final double minLon, final double maxLat, final double maxLon) {
//        if (myNodeLoader != null && myNodeLoader.isReleased()) {
//            myNodeLoader.reInitialize(minLat, minLon, maxLat, maxLon);
//        } else {
//            myNodeLoader = new ConstrainedCurrentNodeReader(myCredentials, minLat, minLon, maxLat, maxLon);
//        }
//
//        return myNodeLoader;
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadNode(long)
//     */
//    public Node loadNode(final long nodeID) {
//        if (myNodeLoader != null && myNodeLoader.isReleased()) {
//            myNodeLoader.reInitialize(nodeID);
//        } else {
//            myNodeLoader = new ConstrainedCurrentNodeReader(myCredentials, nodeID);
//        }
//
//        if (myNodeLoader.hasNext()) {
//            Node node = myNodeLoader.next();
//            myNodeLoader.release();
//            return node;
//        }
//        myNodeLoader.release();
//        return null;
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadRelation(long)
//     */
//    public Relation loadRelation(final long relationID) {
//        if (myRelationLoader != null && myRelationLoader.isReleased()) {
//            myRelationLoader.reInitialize(relationID);
//        } else {
//            myRelationLoader = new ConstrainedCurrentRelationReader(myCredentials, relationID);
//        }
//
//        if (myRelationLoader.hasNext()) {
//            Relation rel = myRelationLoader.next();
//            myRelationLoader.release();
//            return rel;
//        }
//        myRelationLoader.release();
//        return null;
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadAllWays()
//     */
//    public ReleasableIterator<Way> loadAllWays() {
//        ReleasableIterator<Way> reader = new CurrentWayReader(myCredentials, false);
//        return reader;
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadAllWaysInBoundingBox(double, double, double, double)
//     */
//    public ReleasableIterator<Way> loadAllWaysInBoundingBox(final double minLat, final double minLon, final double maxLat, final double maxLon) {
//        ReleasableIterator<Way> reader;
//
//        if (minLat != Double.MIN_VALUE || maxLat != Double.MAX_VALUE
//                || minLon != Double.MIN_VALUE || maxLon != Double.MAX_VALUE) {
//                   // if a bounding-box is given
//            /*reader = new CurrentWayReader(myCredentials, false,
//                    minLat, minLon, maxLat, maxLon);*/
//              reader = new ConstrainedCurrentWayReader(myCredentials,
//                    minLat, minLon, maxLat, maxLon);
//        } else {
//            reader = new CurrentWayReader(myCredentials, false);
//        }
//        return reader;
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadWay(long)
//     */
//    public Way loadWay(final long aWayID) {
//        if (myWayLoader != null && myWayLoader.isReleased()) {
//            myWayLoader.reInitialize(aWayID);
//        } else {
//            myWayLoader = new ConstrainedCurrentWayReader(myCredentials, aWayID);
//        }
//        if (myWayLoader.hasNext()) {
//            Way way = myWayLoader.next();
//            myWayLoader.release();
//            return way;
//        }
//        myWayLoader.release();
//        return null;
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadWaysForNode(long)
//     */
//    public ReleasableIterator<Way> loadWaysForNode(final long aNodeID) {
//        if (myWayLoader != null && myWayLoader.isReleased()) {
//            myWayLoader.reInitializeForNode(aNodeID);
//        } else {
//            myWayLoader = new ConstrainedCurrentWayReader(myCredentials);
//            myWayLoader.reInitializeForNode(aNodeID);
//        }
//
//        return myWayLoader;
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadNodesByName(java.lang.String)
//     */
//    public ReleasableIterator<Node> loadNodesByName(final String aLookFor) {
//        Map<String, String> tags = new HashMap<String, String>();
//        tags.put(Tags.TAG_NAME, aLookFor);
//        tags.put(Tags.TAG_NAME + ":%", aLookFor);
//        tags.put(Tags.TAG_REF, aLookFor);
//        tags.put(Tags.TAG_REF + ":%", aLookFor);
//
//        if (myNodeLoader != null && myNodeLoader.isReleased()) {
//            myNodeLoader.reInitialize(tags, ConstrainedCurrentNodeReader.CONDITION.OR, ConstrainedCurrentNodeReader.COMPARESTYLE.LIKE);
//        } else {
//            myNodeLoader = new ConstrainedCurrentNodeReader(myCredentials, tags, ConstrainedCurrentNodeReader.CONDITION.OR, ConstrainedCurrentNodeReader.COMPARESTYLE.LIKE);
//        }
//
//        return  myNodeLoader;
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadNodesByName(java.lang.String)
//     */
//    public ReleasableIterator<Node> loadNodesByName(final String aLookFor,
//            final double minLat, final double minLon, final double maxLat, final double maxLon) {
//        Map<String, String> tags = new HashMap<String, String>();
//        tags.put(Tags.TAG_NAME, aLookFor);
//        tags.put(Tags.TAG_NAME + ":%", aLookFor);
//        tags.put(Tags.TAG_REF, aLookFor);
//        tags.put(Tags.TAG_REF + ":%", aLookFor);
//
//        if (myNodeLoader != null && myNodeLoader.isReleased()) {
//            myNodeLoader.reInitialize(tags, ConstrainedCurrentNodeReader.CONDITION.OR, ConstrainedCurrentNodeReader.COMPARESTYLE.LIKE,
//                    minLat, minLon, maxLat, maxLon);
//        } else {
//            myNodeLoader = new ConstrainedCurrentNodeReader(myCredentials, tags, ConstrainedCurrentNodeReader.CONDITION.OR, ConstrainedCurrentNodeReader.COMPARESTYLE.LIKE,
//                    minLat, minLon, maxLat, maxLon);
//        }
//
//        return  myNodeLoader;
//    }
//
//    /**
//     * ${@inheritDoc}.
//     */
//    public ReleasableIterator<Node> loadNodesByTag(final String aKey, final String aValue) {
//        Map<String, String> tags = new HashMap<String, String>();
//        tags.put(aKey, aValue);
//
//        if (myNodeLoader != null && myNodeLoader.isReleased()) {
//            myNodeLoader.reInitialize(tags, ConstrainedCurrentNodeReader.CONDITION.OR, ConstrainedCurrentNodeReader.COMPARESTYLE.LIKE);
//        } else {
//            myNodeLoader = new ConstrainedCurrentNodeReader(myCredentials, tags, ConstrainedCurrentNodeReader.CONDITION.OR, ConstrainedCurrentNodeReader.COMPARESTYLE.LIKE);
//        }
//
//        return  myNodeLoader;
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadWaysByName(java.lang.String)
//     */
//    public ReleasableIterator<Way> loadWaysByName(final String aLookFor,
//            final double minLat, final double minLon, final double maxLat, final double maxLon) {
//        Map<String, String> tags = new HashMap<String, String>();
//        tags.put(Tags.TAG_NAME, aLookFor);
//        tags.put(Tags.TAG_NAME + ":%", aLookFor);
//        tags.put(Tags.TAG_REF, aLookFor);
//        tags.put(Tags.TAG_REF + ":%", aLookFor);
//        return  new ConstrainedCurrentWayReader(myCredentials, tags, ConstrainedCurrentNodeReader.CONDITION.OR, ConstrainedCurrentNodeReader.COMPARESTYLE.LIKE,
//                minLat, minLon, maxLat, maxLon);
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadWaysByName(java.lang.String)
//     */
//    public ReleasableIterator<Way> loadWaysByName(final String aLookFor) {
//        Map<String, String> tags = new HashMap<String, String>();
//        tags.put(Tags.TAG_NAME, aLookFor);
//        tags.put(Tags.TAG_NAME + ":%", aLookFor);
//        tags.put(Tags.TAG_REF, aLookFor);
//        tags.put(Tags.TAG_REF + ":%", aLookFor);
//        return  new ConstrainedCurrentWayReader(myCredentials, tags, ConstrainedCurrentNodeReader.CONDITION.OR, ConstrainedCurrentNodeReader.COMPARESTYLE.LIKE);
//    }
//
//    /**
//     * @param aKey a key to look for
//     * @param aValue (may be null) a value for the give key to look for
//     * @return all ways containing the given key (with the given value is specified)
//     */
//    public ReleasableIterator<Way> loadWaysByTag(final String aKey, final String aValue) {
//        Map<String, String> tags = new HashMap<String, String>();
//        tags.put(aKey, aValue);
//        return  new ConstrainedCurrentWayReader(myCredentials, tags, CONDITION.OR, COMPARESTYLE.LIKE);
//    }
//
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#createNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
//     */
//    public void createNode(final Node aW) {
//        EmbeddedTagProcessor tagger = new EmbeddedTagProcessor();
//
//        List<org.openstreetmap.osmosis.core.domain.v0_5.Tag> oldTags = new LinkedList<org.openstreetmap.osmosis.core.domain.v0_5.Tag>();
//        Collection<Tag> newTags = aW.getTags();
//        for (Tag tag : newTags) {
//            oldTags.add(new org.openstreetmap.osmosis.core.domain.v0_5.Tag(tag.getKey(), tag.getValue()));
//        }
//        
//        DatabaseContext databaseContext = getMydatabaseContext();
//        databaseContext.executeStatement("INSERT INTO current_nodes (id, latitude, longitude, user_id, visible, tags, timestamp, tile) "
//                + "VALUES (" + aW.getId() + ", "
//                + FixedPrecisionCoordinateConvertor.convertToFixed(aW.getLatitude()) + ", "
//                + FixedPrecisionCoordinateConvertor.convertToFixed(aW.getLongitude()) + ", "
//                + "1, true, " //TODO: care about the user
//                + "'" + tagger.format(oldTags).replace("'", "\\'") + "', now(), "
//                + (new TileCalculator()).calculateTile(aW.getLatitude(), aW.getLongitude()) + ")");
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#createWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
//     */
//    public void createWay(final Way aNode) {
//        DatabaseContext databaseContext = getMydatabaseContext();
//        databaseContext.executeStatement("INSERT INTO current_ways (id, user_id, visible, timestamp) "
//                + "VALUES (" + aNode.getId() + ", "
//                + "1, " //TODO: care about the user
//                + "now())");
//        int seq = 1;
//        for (WayNode node : aNode.getWayNodes()) {
//            databaseContext.executeStatement("INSERT INTO current_way_nodes (id, node_id, sequence_id) "
//                    + "VALUES (" + aNode.getId() + ", " + node.getNodeId() + ", " + seq + ")");
//            seq++;
//        }
//        for (Tag tag : aNode.getTags()) {
//            databaseContext.executeStatement("INSERT INTO current_way_tags (id, k, v) "
//                    + "VALUES (" + aNode.getId() + ", '" + tag.getKey().replace("'", "\\'") + "', '" + tag.getValue().replace("'", "\\'") + "')");
//        }
//    }
//
//    /**
//     * ${@inheritDoc}.
//     * @see org.openstreetmap.osm.io.IDatabaseLoader#createRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
//     */
//    public void createRelation(final Relation aRelation) {
//        DatabaseContext databaseContext = getMydatabaseContext();
//        databaseContext.executeStatement("INSERT INTO current_relations (id, user_id, visible, timestamp) "
//                + "VALUES (" + aRelation.getId() + ", "
//                + "1, " //TODO: care about the user
//                + "now())");
//        int seq = 1;
//        for (RelationMember member : aRelation.getMembers()) {
//            databaseContext.executeStatement("INSERT INTO current_relation_members (id, member_type, member_id, member_role) "
//                    + "VALUES (" + aRelation.getId() + ", '" + member.getMemberType().toString() + "', "
//                    + member.getMemberId() + ", '" + member.getMemberRole().replace("'", "\\'") + "')");
//            seq++;
//        }
//        for (Tag tag : aRelation.getTags()) {
//            databaseContext.executeStatement("INSERT INTO current_relation_tags (id, k, v) "
//                    + "VALUES (" + aRelation.getId() + ", '" + tag.getKey().replace("'", "\\'") + "', '" + tag.getValue().replace("'", "\\'") + "')");
//            seq++;
//        }
//    }
//
//    /**
//     * @return A database-connection used to write to the database.
//     */
//    protected DatabaseContext getMydatabaseContext() {
//        if (mydatabaseContext == null)
//            mydatabaseContext = new DatabaseContext(myCredentials);
//        return mydatabaseContext;
//    }
//
//    /**
//     * @param aMydatabaseContext A database-connection used to write to the database.
//     */
//    protected void setMydatabaseContext(final DatabaseContext aMydatabaseContext) {
//        mydatabaseContext = aMydatabaseContext;
//    }
//
//    /**
//     * do nothing
//     */
//	@Override
//	public void commit() {
//		// do nothing
//	}
//
//	@Override
//	public void shutdown() {
//		 getMydatabaseContext().commit();
//		 getMydatabaseContext().release();
//		 this.mydatabaseContext = null;
//	}
//}
