/**
 * MysqlDataSet2.java
 * created: 21.07.2009
 * (c) 2007 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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
package org.openstreetmap.osm.data.mysql2;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.h2.H2DataSet;
import org.openstreetmap.osm.data.h2.IConnection;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedNode;
import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import com.mysql.jdbc.Connection;

/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * MysqlDataSet2.java<br/>
 * created: 21.07.2009 <br/>
 *<br/><br/>
 * This is an experimental new way to store your map in a Mysql database.<br/>
 * May eventually replace the outdated {@link org.openstreetmap.osm.data.DBDataSet}.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 * @author <a href="mailto:andre.lison@general-bytes.com">Andr&acute; Lison</a>
 */
public class MysqlDataSet2 extends H2DataSet {


    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(H2DataSet.class.getName());

    /**
     * did we load the driver already?
     * @see #loadDatabaseDriver()
     */
    private static boolean driverLoaded;

    /**
     * Create the DataSet and load the connection-settings from {@link Settings}.
     */
    public MysqlDataSet2() {
        super(Settings.getInstance().get("mysql.dburl",      "jdbc:mysql://localhost/travelingsalesman"),
              Settings.getInstance().get("mysql.dbuser",     "root"),
              Settings.getInstance().get("mysql.dbpassword", ""));
        // TODO Auto-generated constructor stub
    }


    /* (non-Javadoc)
     * @see org.openstreetmap.osm.data.h2.H2DataSet#loadDatabaseDriver()
     */
    @Override
    protected void loadDatabaseDriver() {
        if (!driverLoaded) {
            // Lock to ensure two threads don't try to load the driver at the same time.
            synchronized (com.mysql.jdbc.Driver.class) {
                // Check again to ensure another thread hasn't loaded the driver
                // while we waited for the lock.
                try {
                    Class.forName("com.mysql.jdbc.Driver").newInstance();

                } catch (Exception e) {
                    throw new OsmosisRuntimeException(
                            "Unable to find database driver.", e);
                }

                driverLoaded = true;
            }
        }
    }


    /**
     * Do not use this queue directly but use
     * #getConnection() and #returnConnection() instead.
     */
    private Queue<IConnection> myConnection = new LinkedBlockingQueue<IConnection>();

    private boolean myIsInitialized = false;

    /**
     * This class is not thread-safe.
     * It should not be used by more then 2 threads at a time.
     *
     */
    public class MyMysqlConnection implements IConnection {

        /**
         * The actual database-connection.
         */
        private java.sql.Connection myConnection;

        /**
         * @return the connection
         */
        public java.sql.Connection getConnection() {
            return myConnection;
        }

        /**
         * This constructor is only to be used by {@link H2DataSet#getConnection()}.
         * @param aConnection the underlying database-connection.
         */
        public MyMysqlConnection(final java.sql.Connection aConnection) {
            super();
            myConnection = aConnection;
        }

        /**
         * Close the underlying connection.
         * This method is ONLY to by used by {@link H2DataSet#returnConnection(IConnection)}.
         */
        public void close() {
            try {
                if (myAddNodeStmt != null) {
                    myAddNodeStmt.close();
                    myAddNodeStmt = null;
                }
                if (myGetNodeByIDStmt != null) {
                    myGetNodeByIDStmt.close();
                    myGetNodeByIDStmt = null;
                }
                if (myGetRelationByID != null) {
                    myGetRelationByID.close();
                    myGetRelationByID = null;
                }
                if (myGetNodeByAreaStmt != null) {
                    myGetNodeByAreaStmt.close();
                    myGetNodeByAreaStmt = null;
                }
                if (myGetWayByIDStmt != null) {
                    myGetWayByIDStmt.close();
                    myGetWayByIDStmt = null;
                }
                if (myGetWaysForNodeStmt != null) {
                    myGetWaysForNodeStmt.close();
                    myGetWaysForNodeStmt = null;
                }
                if (myGetRelationsForNodeStmt != null) {
                    myGetRelationsForNodeStmt.close();
                    myGetRelationsForNodeStmt = null;
                }
                if (myGetRelationsForWayStmt != null) {
                    myGetRelationsForWayStmt.close();
                    myGetRelationsForWayStmt = null;
                }
                if (myGetNodesForWayStmt != null) {
                    myGetNodesForWayStmt.close();
                    myGetNodesForWayStmt = null;
                }
                if (myGetMembersForRelation != null) {
                    myGetMembersForRelation.close();
                    myGetMembersForRelation = null;
                }
                if (myAddRelationStmt != null) {
                    myAddRelationStmt.close();
                    myAddRelationStmt = null;
                }
                if (myAddRelationMemberStmt != null) {
                    myAddRelationMemberStmt.close();
                    myAddRelationMemberStmt = null;
                }
                if (myAddWayStmt != null) {
                    myAddWayStmt.close();
                    myAddWayStmt = null;
                }
                if (myAddWayNodeStmt != null) {
                    myAddWayNodeStmt.close();
                    myAddWayNodeStmt = null;
                }
                if (myDeleteWaysNodesStmt != null) {
                    myDeleteWaysNodesStmt.close();
                    myDeleteWaysNodesStmt = null;
                }
                if (myDeleteRelationMemberStmt != null) {
                    myDeleteRelationMemberStmt.close();
                    myDeleteRelationMemberStmt = null;
                }
                if (myDeleteNodeStmt != null) {
                    myDeleteNodeStmt.close();
                    myDeleteNodeStmt = null;
                }
                if (myDeleteWayStmt != null) {
                    myDeleteWayStmt.close();
                    myDeleteWayStmt = null;
                }
                if (myDeleteRelationStmt != null) {
                    myDeleteRelationStmt.close();
                    myDeleteRelationStmt = null;
                }
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Exception while closing prepared statements", e);
            }
            try {
                myConnection.close();
            } catch (SQLException e) {
                LOG.log(Level.WARNING, "Exception while closing database-connection", e);
            }
        }

        /**
         * @see Connection#isClosed()
         * @return true if the underlying connection is closed.
         * @throws SQLException if something bad happens
         */
        public boolean isClosed() throws SQLException {
            return myConnection.isClosed();
        }

        /**
         * the prepared statement used by {@link H2DataSet#addNode(Node)}.
         */
        private PreparedStatement myAddNodeStmt;

        /**
         * The prepared statement used by {@link H2DataSet#getNodeByID(long)}.
         */
        private PreparedStatement myGetNodeByIDStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getRelationByID(long)}.
         */
        private PreparedStatement myGetRelationByID;

        /**
         * the prepared statement used by {@link H2DataSet#getNodes(Bounds)}.
         */
        private PreparedStatement myGetNodeByAreaStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getWaysForNode(long)}.
         */
        private PreparedStatement myGetWaysForNodeStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getNodeByID(long).
         */
        private PreparedStatement myGetRelationsForNodeStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getWaysByID(long).
         */
        private PreparedStatement myGetRelationsForWayStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getWaysByTag(String, String).
         */
        private PreparedStatement mygetGetWayByTagStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getWaysByTag(String, String).
         */
        private PreparedStatement mygetGetNodesByTagStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getWaysByTag(String, String).
         */
        private PreparedStatement mygetGetRelByTagStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getWaysByID(long)}.
         */
        private PreparedStatement myGetWayByIDStmt;

        /**
         * the prepared statement used by {@link H2DataSet#addWay(Way)}.
         */
        private PreparedStatement myAddWayStmt;

        /**
         * the prepared statement used by {@link H2DataSet#addWay(Way)}.
         */
        private PreparedStatement myAddWayNodeStmt;

        /**
         * the prepared statement used by {@link H2DataSet#addRelation(Relation)}.
         */
        private PreparedStatement myAddRelationStmt;

        /**
         * the prepared statement used by {@link H2DataSet#addRelation(Relation)}.
         */
        private PreparedStatement myAddRelationMemberStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getWaysByID(long)}.
         */
        private PreparedStatement myGetNodesForWayStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getRelationByID(long)}.
         */
        private PreparedStatement myGetMembersForRelation;

        /**
         * the prepared statement used by {@link H2DataSet#addWay(Way)} and {@link H2DataSet#removeWay(Way)}.
         */
        private PreparedStatement myDeleteWaysNodesStmt;

        /**
         * the prepared statement used by {@link H2DataSet#addRelation(Relation)} and {@link H2DataSet#removeRelation(Relation)}.
         */
        private PreparedStatement myDeleteRelationMemberStmt;

        /**
         * the prepared statement used by {@link H2DataSet#removeNode(Node)}.
         */
        private PreparedStatement myDeleteNodeStmt;

        /**
         * the prepared statement used by {@link H2DataSet#removeWay(Way)}.
         */
        private PreparedStatement myDeleteWayStmt;

        /**
         * the prepared statement used by {@link H2DataSet#removeRelation(Relation)}.
         */
        private PreparedStatement myDeleteRelationStmt;

        /**
         * @return the addWayNodeStmt the prepared statement used by {@link H2DataSet#addNode(Node)}
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getAddNodeStmt() throws SQLException {
            if (myAddNodeStmt == null) {
                myAddNodeStmt = myConnection.prepareStatement(
                        "REPLACE INTO nodes (nodeid, version, tags, pt) " +
                                "VALUES (?,?,?, GeomFromText(Concat('POINT(',?,' ',?,')')))");
            }
            return myAddNodeStmt;
        }

        /**
         * @return the addWayNodeStmt the prepared statement used by {@link H2DataSet#addWay(Way)}
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getAddWayNodeStmt() throws SQLException {
            if (myAddWayNodeStmt == null) {
                myAddWayNodeStmt = myConnection.prepareStatement("INSERT INTO waynodes  (wayid, nodeid, memberindex) VALUES (?,?,?)");
            }
            return myAddWayNodeStmt;
        }

        /**
         * @return the addWayStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getAddWayStmt() throws SQLException {
            if (myAddWayStmt == null) {
                myAddWayStmt = myConnection.prepareStatement("REPLACE INTO ways  (wayid, version, tags) VALUES (?,?,?)");
            }
            return myAddWayStmt;
        }

        /**
         * @return the deleteWaysNodesStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getDeleteWaysNodesStmt() throws SQLException {
            if (myDeleteWaysNodesStmt == null) {
                myDeleteWaysNodesStmt = myConnection.prepareStatement("DELETE FROM waynodes  WHERE wayid = ?");
            }
            return myDeleteWaysNodesStmt;
        }

        /**
         * @return The prepared statement used by {@link H2DataSet#getNodeByID(long)}.
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getGetNodeByIDStmt() throws SQLException {
            if (myGetNodeByIDStmt == null) {
                myGetNodeByIDStmt = myConnection.prepareStatement(
                        "select nodeid, version, Y(pt) AS lat, X(pt) AS lon, tags from nodes where nodeid = ?");
            }
            return myGetNodeByIDStmt;
        }

        /**
         * @return the getWaysForNodeStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getGetWaysForNodeStmt() throws SQLException {
            if (myGetWaysForNodeStmt == null) {
                myGetWaysForNodeStmt = myConnection.prepareStatement("select wayid from waynodes where nodeid = ?");
            }
            return myGetWaysForNodeStmt;
        }

        /**
         * @return the getWayByIDStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getGetWayByIDStmt() throws SQLException {
            if (myGetWayByIDStmt == null) {
                myGetWayByIDStmt = myConnection.prepareStatement("select * from ways where wayid = ?");
            }
            return myGetWayByIDStmt;
        }

        /**
         * @return the getNodesForWayStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getGetNodesForWayStmt() throws SQLException {
            if (myGetNodesForWayStmt == null) {
                myGetNodesForWayStmt = myConnection.prepareStatement("SELECT * FROM waynodes WHERE wayid = ? order by memberindex");
            }
            myGetNodesForWayStmt.clearWarnings();
            return myGetNodesForWayStmt;
        }

        /**
         * @return the getNodeByAreaStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getGetNodeByAreaStmt() throws SQLException {
            if (myGetNodeByAreaStmt == null) {
                String sql = "select nodeid, version, Y(pt) AS lat, X(pt) AS lon, tags from nodes " +
                        "where MBRContains(GeomFromText(?), pt)";
                myGetNodeByAreaStmt = myConnection.prepareStatement(sql);
            }
            myGetNodeByAreaStmt.clearWarnings();
            return myGetNodeByAreaStmt;
        }

        /**
         * @return the deleteNodeStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getDeleteNodeStmt() throws SQLException {
            if (myDeleteNodeStmt == null) {
                myDeleteNodeStmt = myConnection.prepareStatement("DELETE FROM nodes WHERE nodeid = ?");
            }
            return myDeleteNodeStmt;
        }

        /**
         * @return the deleteWayStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getDeleteWayStmt() throws SQLException {
            if (myDeleteWayStmt == null) {
                myDeleteWayStmt = myConnection.prepareStatement("DELETE FROM ways WHERE wayid = ?");
            }
            return myDeleteWayStmt;
        }

        /**
         * @return the deleteRelationStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getDeleteRelationStmt() throws SQLException {
            if (myDeleteRelationStmt == null) {
                myDeleteRelationStmt = myConnection.prepareStatement("DELETE FROM relations WHERE relid = ?");
            }
            return myDeleteRelationStmt;
        }

        /**
         * @return the addRelationStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getAddRelationStmt() throws SQLException {
            if (myAddRelationStmt == null) {
                myAddRelationStmt = myConnection.prepareStatement(
                        "REPLACE INTO relations (relid, version, tags) VALUES (?,?,?)");
            }
            return myAddRelationStmt;
        }

        /**
         * @return the addRelationMemberStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getAddRelationMemberStmt() throws SQLException {
            if (myAddRelationMemberStmt == null) {
                myAddRelationMemberStmt = myConnection.prepareStatement(
                        "INSERT INTO relmembers  (relid, entityid, entitytype, memberindex, role) VALUES (?,?,?,?,?)");
            }
            return myAddRelationMemberStmt;
        }

        /**
         * @return the deleteRelationMemberStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getDeleteRelationMemberStmt() throws SQLException {
            if (myDeleteRelationMemberStmt == null) {
                myDeleteRelationMemberStmt = myConnection.prepareStatement("DELETE FROM relmembers WHERE relid = ?");
            }
            return myDeleteRelationMemberStmt;
        }

        /**
         * @return the getMembersForRelation
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getGetRelationByID() throws SQLException {
            if (myGetRelationByID == null) {
                myGetRelationByID = myConnection.prepareStatement("SELECT * FROM relations WHERE relid = ?");
            }
            return myGetRelationByID;
        }

        /**
         * @return the getMembersForRelation
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getGetMembersForRelation() throws SQLException {
            if (myGetMembersForRelation == null) {
                myGetMembersForRelation = myConnection.prepareStatement("SELECT * FROM relmembers WHERE relid = ? order by memberindex");
            }
            return myGetMembersForRelation;
        }

        /**
         * @return the getRelationsForNodeStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getGetRelationsForNodeStmt() throws SQLException {
            if (myGetRelationsForNodeStmt == null) {
                myGetRelationsForNodeStmt = myConnection.prepareStatement("SELECT relid FROM relmembers WHERE entityid = ? AND entitytype = " + EntityType.Node.ordinal());
            }
            return myGetRelationsForNodeStmt;
        }

        /**
         * @return the getRelationsForWayStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getGetRelationsForWayStmt() throws SQLException {
            if (myGetRelationsForWayStmt == null) {
                myGetRelationsForWayStmt = myConnection.prepareStatement("SELECT relid FROM relmembers WHERE entityid = ? AND entitytype = " + EntityType.Way.ordinal());
            }
            return myGetRelationsForWayStmt;
        }

        /**
         * @return the getGetWayByTagStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getGetWayByTagStmt() throws SQLException {
            if (mygetGetWayByTagStmt == null) {
                mygetGetWayByTagStmt = myConnection.prepareStatement("SELECT wayid FROM ways WHERE tags like ?");
            }
            return mygetGetWayByTagStmt;
        }

        /**
         * @return the getGetWayByTagStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getGetNodesByTagStmt() throws SQLException {
            if (mygetGetNodesByTagStmt == null) {
                mygetGetNodesByTagStmt = myConnection.prepareStatement("SELECT nodeid FROM nodes WHERE tags like ?");
            }
            return mygetGetNodesByTagStmt;
        }

        /**
         * @return the getGetWayByTagStmt
         * @throws SQLException if the prepared statement cannot be created.
         */
        public PreparedStatement getGetRelByTagStmt() throws SQLException {
            if (mygetGetRelByTagStmt == null) {
                mygetGetRelByTagStmt = myConnection.prepareStatement("SELECT relid FROM relations WHERE tags like ?");
            }
            return mygetGetRelByTagStmt;
        }
    }


    /**
     * If no database connection is open, a new connection is opened. The
     * database connection is then returned.<br/>
     * <b>When you are done with it, use {@link #returnConnection(IConnection)}.</b>
     * @return The database connection.
     * @throws SQLException if we cannot provide a connection
     * @see #returnConnection(IConnection)
     */
    protected IConnection getConnection() throws SQLException {
        IConnection connection = myConnection.poll();
        if (connection == null || connection.isClosed()) {
            try {
                if (!myIsInitialized) {
                    myIsInitialized = true;
                    loadDatabaseDriver();

                    try {
                        java.sql.Connection con = DriverManager.getConnection(getDatabaseURL(), getDatabaseUser(), getDatabasePassword());
                        con.setAutoCommit(false);
                        checkSchema(con);

                    } catch (SQLException e) {
                        throw new OsmosisRuntimeException(
                                "Unable to establish a database connection to '" + getDatabaseURL() + "'.", e);
                    }
                }

                LOG.log(Level.INFO, "Opening new connection.  DB=" + getDatabaseURL(), new Exception("DEBUG"));
                java.sql.Connection con = DriverManager.getConnection(getDatabaseURL(), getDatabaseUser(), getDatabasePassword());
                con.setAutoCommit(false);
                connection = new MyMysqlConnection(con);
            } catch (NullPointerException e) {
                //connection = new MyConnection(myConnectionPool.getConnection());
                return getConnection();
            }
        }
        return connection;
    }

    /**
     * Return a used connection to the pool.
     * @param aConnection the connection to return
     * @see #getConnection()
     */
    protected void returnConnection(final IConnection aConnection) {
        if (!myConnection.offer(aConnection)) {
            LOG.info("Closing connection. as the pool is full");
            aConnection.close();
        }
    }


    /**
     * Make sure all tables we need exist.
     * Convert old versions of the database-schema if needed.
     * @param aConnection our connections. Not yet used.
     * @throws SQLException if we cannot create/alter the tables or the current schma is incompatible.
     */
    @Override
    protected void checkSchema(final java.sql.Connection aConnection) throws SQLException {
        try {
            Statement stmt = aConnection.createStatement();
            try {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS nodes ("
                        + "nodeid BIGINT PRIMARY KEY,"
                        + "version INT,"
                        + "pt POINT NOT NULL,"
                        + "tags MEDIUMTEXT, "
                        + "SPATIAL KEY pt_idx (pt) "
                        + ") ENGINE=MYISAM;");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS waynodes ("
                        + "wayid BIGINT NOT NULL,"
                        + "nodeid BIGINT NOT NULL,"
                        + "memberindex INT NOT NULL,"
                        + "PRIMARY KEY (wayid, memberindex)"
                        + ") ENGINE=MYISAM;");
                try {
                    stmt.executeUpdate("CREATE INDEX waynodes ON waynodes (wayid);");
                } catch (Exception e) {
                    if (!e.getMessage().contains("Duplicate key name")) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                }
                try {
                    stmt.executeUpdate("CREATE INDEX nodeways ON waynodes (nodeid);");
                } catch (Exception e) {
                    if (!e.getMessage().contains("Duplicate key name")) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                }
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ways ("
                        + "wayid BIGINT PRIMARY KEY,"
                        + "version INT,"
                        + "tags MEDIUMTEXT"
                        + ") ENGINE=MYISAM;");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS relations ("
                        + "relid BIGINT PRIMARY KEY,"
                        + "version INT,"
                        + "tags MEDIUMTEXT"
                        + ") ENGINE=MYISAM;");
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS relmembers ("
                        + "relid BIGINT NOT NULL, "
                        + "entityid BIGINT NOT NULL, "
                        + "entitytype SMALLINT NOT NULL, "
                        + "memberindex INT NOT NULL, "
                        + "role VARCHAR(64), "
                        + "PRIMARY KEY (relid, memberindex) "
                        + ") ENGINE=MYISAM;");
                try {
                    stmt.executeUpdate("CREATE INDEX relmembers ON relmembers (relid);");
                } catch (Exception e) {
                    if (!e.getMessage().contains("Duplicate key name")) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                }

                try {
                    stmt.executeUpdate("CREATE INDEX memberrels ON relmembers (entityid, entitytype);");
                } catch (Exception e) {
                    if (!e.getMessage().contains("Duplicate key name")) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }
                }

            } finally {
                stmt.close();
            }
        } finally {
            aConnection.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNode(final Node aN) {
        try {
            myNodeCache.put(aN.getId(), aN);
            int i = 1;
            IConnection con = getConnection();
            try {
                PreparedStatement addNodeStmt = con.getAddNodeStmt();
                addNodeStmt.setLong(i++, aN.getId());
                addNodeStmt.setInt(i++, aN.getVersion());
//                final int lat = FixedPrecisionCoordinateConvertor.convertToFixed(aN.getLatitude());
//                addNodeStmt.setInt(i++, lat);
//                final int lon = FixedPrecisionCoordinateConvertor.convertToFixed(aN.getLongitude());
//                addNodeStmt.setInt(i++, lon);

                addNodeStmt.setString(i++, serializeTags(aN.getTags()));
                addNodeStmt.setDouble(i++, aN.getLongitude());
                addNodeStmt.setDouble(i++, aN.getLatitude());
                addNodeStmt.execute();
                con.getConnection().commit();
            } finally {
                returnConnection(con);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot add node #" + aN.getId()
                    + " at " + aN.getLatitude() + "/" + aN.getLongitude(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Node> getNodes(final Bounds aBoundingBox) {
        HashMap<Long, ExtendedNode> nodes = new HashMap<Long, ExtendedNode>();
        try {
            IConnection connection = getConnection();
            final LatLon minLoc = aBoundingBox.getMin();
            final LatLon maxLoc = aBoundingBox.getMax();

            PreparedStatement localPStmt = null;
            ResultSet rs = null;

            try {
                String lineString = new StringBuilder("LINESTRING(")
                        .append(minLoc.lon()).append(' ').append(minLoc.lat()).append(',')
                        .append(maxLoc.lon()).append(' ').append(maxLoc.lat())
                        .append(")").toString();

                PreparedStatement pstmt = connection.getGetNodeByAreaStmt();
                pstmt.setString(1, lineString);
                rs = pstmt.executeQuery();

                while (rs.next()) {
                    ExtendedNode node = new ExtendedNode(rs.getLong(1),
                            rs.getInt(2),
                            0,
                            rs.getDouble(3),
                            rs.getDouble(4));
                    String attrs = rs.getString(5);
                    node.getTags().addAll(deserializeTags(attrs));
                    nodes.put(node.getId(), node);
                }
                rs.close();
                rs = null;

                if (nodes.size() > 0) {
                    String sql = "select wayid, n.nodeid from nodes n inner join waynodes w ON w.nodeid=n.nodeid " +
                            "where MBRContains(GeomFromText(?), n.pt)";
                    localPStmt = connection.getConnection().prepareStatement(sql);
                    localPStmt.setString(1, lineString);
                    rs = localPStmt.executeQuery();
                    while (rs.next()) {
                        ExtendedNode node = nodes.get(rs.getLong(2));
                        node.addReferencedWay(rs.getLong(1));
                    }
                    rs.close();
                    localPStmt.close();

                    sql = "select r.relid, n.nodeid from nodes n inner join relmembers r ON r.entityid=n.nodeid " +
                            "WHERE r.entitytype=" + EntityType.Node.ordinal()
                            + " AND  MBRContains(GeomFromText(?), n.pt)";
                    localPStmt = connection.getConnection().prepareStatement(sql);
                    localPStmt.setString(1, lineString);
                    rs = localPStmt.executeQuery();
                    while (rs.next()) {
                        ExtendedNode node = nodes.get(rs.getLong(2));
                        node.addReferencedRelation(rs.getLong(1));
                    }
                    localPStmt.close();
                }

            } finally {
                if (rs != null && !rs.isClosed()) {
                    rs.close();
                }
                if (localPStmt != null && !localPStmt.isClosed()) {
                    localPStmt.close();
                }
                returnConnection(connection);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot get nodes by area", e);
        }
        return new ArrayList<Node>(nodes.values()).iterator();
    }

    /**
     * This method only returns an extendedNode if one is in the cache.
     * @param connection the connection to use ({@link #returnConnection(IConnection)} is called automatically)
     * @param aStatement a prepared-statement to execute
     * @return the ExtendedNode of the first way returned or null
     * @throws SQLException may happen
     */
    private Node getSimpleNodeFromQuery(final IConnection connection,
            final PreparedStatement aStatement) throws SQLException {
        ResultSet rs = null;
        try {
            rs = aStatement.executeQuery();
            if (rs.next()) {
                long nodeID = rs.getLong(1);
                Node cached = myNodeCache.get(nodeID);
                if (cached != null) {
                    return cached;
                }
                Node node = new Node(nodeID,
                        rs.getInt(2),
                        new Date(),
                        OsmUser.NONE,
                        0,
                        rs.getDouble(3),
                        rs.getDouble(4));
                String attrs = rs.getString(5);
                node.getTags().addAll(deserializeTags(attrs));

                myNodeCache.put(node.getId(), node);
                return node;
            }
        } finally {
            if (rs != null && !rs.isClosed()) {
                rs.close();
            }
            returnConnection(connection);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Node getNodeByID(final long aNodeID) {

        Node n = myNodeCache.get(aNodeID);
        if (n != null) {
            return n;
        }
        try {
            IConnection connection = getConnection();
            PreparedStatement getNodeByIDStmt = connection.getGetNodeByIDStmt();
            getNodeByIDStmt.setLong(1, aNodeID);
            // During imports we return Nodes instead
            // of ExtendedNodes in {@link #getNodeByID(long)}.

            if (isImporting()) {
                return getSimpleNodeFromQuery(connection, getNodeByIDStmt);
            } else {
                return getNodeFromQuery(connection, getNodeByIDStmt);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot get node by ID", e);
        }
        return null;
    }

    /**
     * @param connection the connection to use ({@link #returnConnection(IConnection)} is called automaticaly)
     * @param aStatement a prepared-statement to execute
     * @return the ExtendedNode of the first way returned or null
     * @throws SQLException may happen
     */
    private Node getNodeFromQuery(final IConnection connection,
            final PreparedStatement aStatement) throws SQLException {
        try {
            ResultSet rs = aStatement.executeQuery();
            try {
                while (rs.next()) {
                    long nodeID = rs.getLong(1);
                    Node cached = myNodeCache.get(nodeID);
                    if (cached != null) {
                        return cached;
                    }
                    ExtendedNode node = new ExtendedNode(nodeID,
                            rs.getInt(2),
                            0,
                            rs.getDouble(3),
                            rs.getDouble(4));
                    String attrs = rs.getString(5);
                    node.getTags().addAll(deserializeTags(attrs));
                    rs.close();

                    PreparedStatement getWaysForNodeStmt = connection.getGetWaysForNodeStmt();
                    getWaysForNodeStmt.clearParameters();
                    getWaysForNodeStmt.setLong(1, nodeID);
                    ResultSet rs2 = getWaysForNodeStmt.executeQuery();
                    try {
                        while (rs2.next()) {
                            //                            node.addReferencedWay(rs2.getLong("wayid"));
                            node.addReferencedWay(rs2.getLong(1));
                        }
                    } finally {
                        rs2.close();
                    }

                    PreparedStatement getRelationsForNodeStmt = connection.getGetRelationsForNodeStmt();
                    getRelationsForNodeStmt.clearParameters();
                    getRelationsForNodeStmt.setLong(1, nodeID);
                    rs2 = getRelationsForNodeStmt.executeQuery();
                    try {
                        while (rs2.next()) {
                            //node.addReferencedRelation(rs2.getLong("relid"));
                            node.addReferencedRelation(rs2.getLong(1));
                        }
                    } finally {
                        rs2.close();
                    }

                    myNodeCache.put(node.getId(), node);
                    return node;
                }
            } finally {
                rs.close();
            }
        } finally {
            returnConnection(connection);
        }
        return null;
    }


}
