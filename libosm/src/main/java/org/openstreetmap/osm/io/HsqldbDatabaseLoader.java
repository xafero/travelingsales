/**
 * This file is part of LibOSM by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  LibOSM is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LibOSM is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LibOSM.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.osm.io;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.TileCalculator;
import org.openstreetmap.osm.data.hsqldb.DatabaseContext;
import org.openstreetmap.osm.data.hsqldb.HsqldbCurrentNodeReader;
import org.openstreetmap.osm.data.hsqldb.HsqldbCurrentRelationReader;
import org.openstreetmap.osm.data.hsqldb.HsqldbCurrentWayReader;
import org.openstreetmap.osm.data.mysql.ConstrainedCurrentNodeReader.COMPARESTYLE;
import org.openstreetmap.osm.data.mysql.ConstrainedCurrentNodeReader.CONDITION;

import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.util.FixedPrecisionCoordinateConvertor;
/*import org.openstreetmap.osmosis.core.mysql.v0_5.impl.EmbeddedTagProcessor;*/ // TODO: Check tag processing!
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;

/**
 * Helper-class to load <a hef="http://wiki.openstreetmap.org/index.php/Develop">OpenStreetMap</a>-data from a database.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class HsqldbDatabaseLoader implements IDatabaseLoader {

    /**
     * Milliseconds for the thread started by {@link #doCommitAsync()}
     * before actually commiting ({@value #PRECOMMITSLEEPMILLIS} ms).
     */
    private static final int PRECOMMITSLEEPMILLIS = 60 * 1000;

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger
            .getLogger(HsqldbDatabaseLoader.class.getName());

    /**
     * value for the typeID-field in the database for a node.
     */
    public static final int NODETYPEID = 0;

    /**
     * value for the typeID-field in the database for a way.
     */
    public static final int WAYTYPEID = 1;

    /**
     * value for the typeID-field in the database for a relation.
     */
    public static final int RELATIONTYPEID = 3;

    /**
     * A database-connection used to write to the database.
     */
    private DatabaseContext mydatabaseContext;

    /**
     * Since we can reinitialize it, we cache our
     * WayReader.
     */
    private HsqldbCurrentWayReader myWayLoader = null;

    /**
     * Since we can reinitialize it, we cache our
     * NodeReader.
     */
    private HsqldbCurrentNodeReader myNodeLoader = null;

    /**
     * Since we can reinitialize it, we cache our
     * RelationReader.
     */
    private HsqldbCurrentRelationReader myRelationLoader = null;

    /**
     * PreparedStatement used by {@link #createNode(Node)}.
     */
    private PreparedStatement myCreateNodePreparedStatement;

    /**
     * PreparedStatement used by {@link #createWay(Way)..
     */
    private PreparedStatement myCreateWayPreparedStatement;

    /**
     * PreparedStatement used by {@link #createWay(Way)..
     */
    private PreparedStatement myCreateWayNodesPreparedStatement;

    /**
     * PreparedStatement used by {@link #createWay(Way).
     */
    private PreparedStatement myCreateWayTagsPreparedStatement;

    /**
     * PreparedStatement used by {@link #createRelation(Relation).
     */
    private PreparedStatement myCreateRelationPreparedStatement;

    /**
     * PreparedStatement used by {@link #createRelation(Relation).
     */
    private PreparedStatement myCreateRelationMembersPreparedStatement;

    /**
     * PreparedStatement used by {@link #createRelation(Relation).
     */
    private PreparedStatement myCreateRelationTagsPreparedStatement;

    /**
     */
    public HsqldbDatabaseLoader() {
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadBoundingBox(double, double, double, double)
     */
    public MemoryDataSet loadBoundingBox(final double minLat, final double minLon, final double maxLat, final double maxLon) {

        /*MysqlReader reader;
        if (minLat != Double.MIN_VALUE || maxLat != Double.MAX_VALUE
                || minLon != Double.MIN_VALUE || maxLon != Double.MAX_VALUE) {
                   // if a bounding-box is given
            reader = new MysqlReader(myCredentials,
                    minLat, minLon, maxLat, maxLon);
        } else {
            reader = new MysqlReader(myCredentials, myDBPreferences, new Date(), false);
        }
        DataSetSink sink = new DataSetSink();
        reader.setSink(sink);
        reader.run();

        return (MemoryDataSet) sink.getDataSet();*/
        throw new IllegalStateException("loadBoundingBox() not implemented for HSqlDB yet");
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadPlanet()
     */
    public MemoryDataSet loadPlanet() {

/*        MysqlReader reader = new MysqlReader(myCredentials, myDBPreferences, new Date(), false);
        DataSetSink sink = new DataSetSink();
        reader.setSink(sink);
        reader.run();

        return (MemoryDataSet) sink.getDataSet();*/
        throw new IllegalStateException("loadPlanet() not implemented for HSqlDB yet");
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadAllNodes()
     */
    public ReleasableIterator<Node> loadAllNodes() {
        myNodeLoader = new HsqldbCurrentNodeReader();
        return myNodeLoader;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadAllNodesInBoundingBox(double, double, double, double)
     */
    public ReleasableIterator<Node> loadAllNodesInBoundingBox(final double minLat, final double minLon, final double maxLat, final double maxLon) {
        if (myNodeLoader != null && myNodeLoader.isReleased()) {
            myNodeLoader.reInitialize(minLat, minLon, maxLat, maxLon);
        } else {
            myNodeLoader = new HsqldbCurrentNodeReader(minLat, minLon, maxLat, maxLon);
        }

        return myNodeLoader;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadNode(long)
     */
    public Node loadNode(final long nodeID) {
        if (myNodeLoader != null && myNodeLoader.isReleased()) {
            myNodeLoader.reInitialize(nodeID);
        } else {
            myNodeLoader = new HsqldbCurrentNodeReader(nodeID);
        }

        if (myNodeLoader.hasNext()) {
            Node node = myNodeLoader.next();
            myNodeLoader.release();
            return node;
        }
        myNodeLoader.release();
        return null;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadRelation(long)
     */
    public Relation loadRelation(final long relationID) {
        if (myRelationLoader != null && myRelationLoader.isReleased()) {
            myRelationLoader.reInitialize(relationID);
        } else {
            myRelationLoader = new HsqldbCurrentRelationReader();
        }

        if (myRelationLoader.hasNext()) {
            Relation rel = myRelationLoader.next();
            myRelationLoader.release();
            return rel;
        }
        myRelationLoader.release();
        return null;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadAllWays()
     */
    public ReleasableIterator<Way> loadAllWays() {
        ReleasableIterator<Way> reader = new HsqldbCurrentWayReader();
        return reader;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadAllWaysInBoundingBox(double, double, double, double)
     */
    public ReleasableIterator<Way> loadAllWaysInBoundingBox(final double minLat, final double minLon, final double maxLat, final double maxLon) {
        ReleasableIterator<Way> reader;

        if (minLat != Double.MIN_VALUE || maxLat != Double.MAX_VALUE
                || minLon != Double.MIN_VALUE || maxLon != Double.MAX_VALUE) {
                   // if a bounding-box is given
            /*reader = new CurrentWayReader(myCredentials, false,
                    minLat, minLon, maxLat, maxLon);*/
              reader = new HsqldbCurrentWayReader(
                    minLat, minLon, maxLat, maxLon);
        } else {
            reader = new HsqldbCurrentWayReader();
        }
        return reader;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadWay(long)
     */
    public Way loadWay(final long aWayID) {
        if (myWayLoader != null && myWayLoader.isReleased()) {
            myWayLoader.reInitialize(aWayID);
        } else {
            myWayLoader = new HsqldbCurrentWayReader(aWayID);
        }
        if (myWayLoader.hasNext()) {
            Way way = myWayLoader.next();
            myWayLoader.release();
            return way;
        }
        myWayLoader.release();
        return null;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadWaysForNode(long)
     */
    public ReleasableIterator<Way> loadWaysForNode(final long aNodeID) {
        if (myWayLoader != null && myWayLoader.isReleased()) {
            myWayLoader.reInitializeForNode(aNodeID);
        } else {
            myWayLoader = new HsqldbCurrentWayReader();
            myWayLoader.reInitializeForNode(aNodeID);
        }

        return myWayLoader;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadNodesByName(java.lang.String)
     */
    public ReleasableIterator<Node> loadNodesByName(final String aLookFor) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(Tags.TAG_NAME, aLookFor);
        tags.put(Tags.TAG_NAME + ":%", aLookFor);
        tags.put(Tags.TAG_REF, aLookFor);
        tags.put(Tags.TAG_REF + ":%", aLookFor);

        if (myNodeLoader != null && myNodeLoader.isReleased()) {
            myNodeLoader.reInitialize(tags, CONDITION.OR, COMPARESTYLE.LIKE);
        } else {
            myNodeLoader = new HsqldbCurrentNodeReader(tags, CONDITION.OR, COMPARESTYLE.LIKE);
        }

        return  myNodeLoader;
    }

    /**
     * ${@inheritDoc}.
     */
    public ReleasableIterator<Node> loadNodesByTag(final String aKey, final String aValue) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(aKey, aValue);

        if (myNodeLoader != null && myNodeLoader.isReleased()) {
            myNodeLoader.reInitialize(tags, CONDITION.OR, COMPARESTYLE.LIKE);
        } else {
            myNodeLoader = new HsqldbCurrentNodeReader(tags, CONDITION.OR, COMPARESTYLE.LIKE);
        }

        return  myNodeLoader;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadWaysByName(java.lang.String)
     */
    public ReleasableIterator<Way> loadWaysByName(final String aLookFor) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(Tags.TAG_NAME, aLookFor);
        tags.put(Tags.TAG_NAME + ":%", aLookFor);
        tags.put(Tags.TAG_REF, aLookFor);
        tags.put(Tags.TAG_REF + ":%", aLookFor);
        return  new HsqldbCurrentWayReader(tags, CONDITION.OR, COMPARESTYLE.LIKE);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#loadWaysByName(java.lang.String)
     */
    public ReleasableIterator<Way> loadWaysByName(final String aLookFor, final double minLat, final double minLon, final double maxLat, final double maxLon) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(Tags.TAG_NAME, aLookFor);
        tags.put(Tags.TAG_NAME + ":%", aLookFor);
        tags.put(Tags.TAG_REF, aLookFor);
        tags.put(Tags.TAG_REF + ":%", aLookFor);
        return  new HsqldbCurrentWayReader(tags, CONDITION.OR, COMPARESTYLE.LIKE,
                minLat, minLon, maxLat, maxLon);
    }

    /**
     * @param aKey a key to look for
     * @param aValue (may be null) a value for the give key to look for
     * @return all ways containing the given key (with the given value is specified)
     */
    public ReleasableIterator<Way> loadWaysByTag(final String aKey, final String aValue) {
        Map<String, String> tags = new HashMap<String, String>();
        tags.put(aKey, aValue);
        return  new HsqldbCurrentWayReader(tags, CONDITION.OR, COMPARESTYLE.LIKE);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#createNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public void createNode(final Node aW) {
        //EmbeddedTagProcessor tagger = new EmbeddedTagProcessor();

        DatabaseContext databaseContext = getMydatabaseContext();
        if (myCreateNodePreparedStatement == null)
             myCreateNodePreparedStatement = databaseContext.prepareStatement("INSERT INTO current_nodes (id, latitude, longitude, tags, timestamp, tile) "
                        + "VALUES (?, ?, ?, ?, now(), ?)");
        try {
            int i = 1;
            List<org.openstreetmap.osmosis.core.domain.v0_6.Tag> oldTags = new LinkedList<org.openstreetmap.osmosis.core.domain.v0_6.Tag>();
            Collection<Tag> newTags = aW.getTags();
            for (Tag tag : newTags) {
                oldTags.add(new org.openstreetmap.osmosis.core.domain.v0_6.Tag(tag.getKey(), tag.getValue()));
            }
            myCreateNodePreparedStatement.setLong(i++, aW.getId());
            myCreateNodePreparedStatement.setInt(i++, FixedPrecisionCoordinateConvertor.convertToFixed(aW.getLatitude()));
            myCreateNodePreparedStatement.setInt(i++, FixedPrecisionCoordinateConvertor.convertToFixed(aW.getLongitude()));
            // myCreateNodePreparedStatement.setString(i++, tagger.format(oldTags)); // TODO: Check tag processing!
            myCreateNodePreparedStatement.setLong(i++, (new TileCalculator()).calculateTile(aW.getLatitude(), aW.getLongitude()));
            myCreateNodePreparedStatement.execute();
            doCommitAsync();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot create node " + aW, e);
        }
/*        databaseContext.executeStatement("INSERT INTO current_nodes (id, latitude, longitude, tags, timestamp, tile) "
                + "VALUES (" + aW.getId() + ", "
                + convertor.convertToFixed(aW.getLatitude()) + ", "
                + convertor.convertToFixed(aW.getLongitude()) + ", "
                + "'" + .replace("'", "\\'") + "', now(), "
                +  + ")");*/
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#createWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public void createWay(final Way aNode) {
        DatabaseContext databaseContext = getMydatabaseContext();
        if (myCreateWayPreparedStatement == null)
            myCreateWayPreparedStatement = databaseContext.prepareStatement(
                    "INSERT INTO current_ways (id, timestamp) VALUES (?,now())");
        if (myCreateWayNodesPreparedStatement == null)
            myCreateWayNodesPreparedStatement = databaseContext.prepareStatement(
                    "INSERT INTO current_way_nodes (id, node_id, sequence_id) "
                    + "VALUES (?, ?, ?)");
        if (myCreateWayTagsPreparedStatement == null)
            myCreateWayTagsPreparedStatement = databaseContext.prepareStatement(
                    "INSERT INTO current_way_tags (id, k, v) "
                    + "VALUES (?, ?, ?)");

        try {
            myCreateWayPreparedStatement.setLong(1, aNode.getId());
            myCreateWayPreparedStatement.execute();

            int seq = 1;
            for (WayNode node : aNode.getWayNodes()) {
                int i = 1;
                myCreateWayNodesPreparedStatement.setLong(i++, aNode.getId());
                myCreateWayNodesPreparedStatement.setLong(i++, node.getNodeId());
                myCreateWayNodesPreparedStatement.setInt(i++, seq);
                myCreateWayNodesPreparedStatement.execute();
                seq++;
            }

            for (Tag tag : aNode.getTags()) {
                int i = 1;
                myCreateWayTagsPreparedStatement.setLong(i++, aNode.getId());
                myCreateWayTagsPreparedStatement.setString(i++, tag.getKey());
                myCreateWayTagsPreparedStatement.setString(i++, tag.getValue());
                myCreateWayTagsPreparedStatement.execute();
            }
            doCommitAsync();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot create way " + aNode, e);
        }
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.io.IDatabaseLoader#createRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public void createRelation(final Relation aRelation) {
        DatabaseContext databaseContext = getMydatabaseContext();
        if (myCreateRelationPreparedStatement == null)
            myCreateRelationPreparedStatement = databaseContext.prepareStatement(
                    "INSERT INTO current_relations (id, timestamp) VALUES (?, now())");
        if (myCreateRelationMembersPreparedStatement == null)
            myCreateRelationMembersPreparedStatement = databaseContext.prepareStatement(
                    "INSERT INTO current_relation_members (id, member_type, member_id, member_role) "
                        + "VALUES (?, ?, ?, ?)");
        if (myCreateRelationTagsPreparedStatement == null)
            myCreateRelationTagsPreparedStatement = databaseContext.prepareStatement(
                    "INSERT INTO current_relation_tags (id, k, v) "
                    + "VALUES (?, ?, ?)");

        try {
            myCreateRelationPreparedStatement.setLong(1, aRelation.getId());

            for (RelationMember member : aRelation.getMembers()) {

                int memberType = NODETYPEID;
                if (member.getMemberType() == EntityType.Node) {
                    memberType = NODETYPEID;
                } else if (member.getMemberType() == EntityType.Way) {
                    memberType = WAYTYPEID;
                } else if (member.getMemberType() == EntityType.Relation) {
                    memberType = RELATIONTYPEID;
                } else {
                    memberType = RELATIONTYPEID + 1; //unknown
                }
                int i = 1;
                myCreateRelationMembersPreparedStatement.setLong(i++, aRelation.getId());
                myCreateRelationMembersPreparedStatement.setInt(i++, memberType);
                myCreateRelationMembersPreparedStatement.setLong(i++, member.getMemberId());
                myCreateRelationMembersPreparedStatement.setString(i++, member.getMemberRole());
                myCreateRelationMembersPreparedStatement.execute();

            }
            for (Tag tag : aRelation.getTags()) {
                int i = 1;
                myCreateRelationTagsPreparedStatement.setLong(i++, aRelation.getId());
                myCreateRelationTagsPreparedStatement.setString(i++, tag.getKey());
                myCreateRelationTagsPreparedStatement.setString(i++, tag.getValue());
                myCreateRelationTagsPreparedStatement.execute();
            }

            doCommitAsync();
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot create relation " + aRelation, e);
        }
    }

    /**
     * @return A database-connection used to write to the database.
     */
    protected DatabaseContext getMydatabaseContext() {
        if (mydatabaseContext == null)
            mydatabaseContext = new DatabaseContext();
        return mydatabaseContext;
    }

    /**
     * @param aMydatabaseContext A database-connection used to write to the database.
     */
    protected void setMydatabaseContext(final DatabaseContext aMydatabaseContext) {
        mydatabaseContext = aMydatabaseContext;
    }

    /**
     * Commit our database asynchronously.<br/>
     * Do nothing if we are already commiting.
     */
    private void doCommitAsync() {
        if (commitThread == null || !commitThread.isAlive()) {
            commitThread = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(PRECOMMITSLEEPMILLIS);
                    } catch (InterruptedException e1) {
                        LOG.log(Level.FINEST, "commit-thread has been interrupted");
                    }
                    try {
                        getMydatabaseContext().commit();
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Cannot commit AddressDBPlaceFinder-Database.");
                    }
                }
            };
            commitThread.start();
        }
    }

    /**
     *  Used by {@link #doCommitAsync()}.
     */
    private Thread commitThread = null;

    /**
     * Checkpoint the database.
     */
    @Override
    public void commit() {
        getMydatabaseContext().executeStatement("CHECKPOINT DEFRAG");
    }
    /**
     * Shutdown the database.
     */
    @Override
    public void shutdown() {
        getMydatabaseContext().commit();
        getMydatabaseContext().executeStatement("SHUTDOWN");
        getMydatabaseContext().release();
        mydatabaseContext = null;
    }
}
