/**
 * H2DataSet.java
 * created: 17.04.2009
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
package org.openstreetmap.osm.data.h2;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.h2.jdbc.JdbcConnection;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.MultiDimension;
import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.IHintableDataSet;
import org.openstreetmap.osm.data.Selector;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedNode;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedWay;
import org.openstreetmap.osm.data.projection.Projection;
import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.util.FixedPrecisionCoordinateConvertor;

/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * H2DataSet.java<br/>
 * created: 17.04.2009 <br/>
 *<br/><br/>
 * This is an experimental way to store your map in a H2 database.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class H2DataSet implements IDataSet, IHintableDataSet {


    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(H2DataSet.class.getName());

    /**
     * In {@link #getNearestNode(LatLon, Selector)} search
     * in increasing circles starting with this radius.
     */
    private static final double SEARCHSTARTRADIUS = 0.0001d;

    /**
     * The default initial capacity - MUST be a power of two.
     * @see #myNodeCache
     */
    static final int DEFAULT_CACHE_INITIAL_CAPACITY = 16;

    /**
     * The load factor used when none specified in constructor.
     * @see #myNodeCache
     */
    static final float DEFAULT_CACHE_LOAD_FACTOR = 0.75f;


    /**
     * Cache for nodes to avoid loading them repeatedly.
     */
    protected Map<Long, Node> myNodeCache = Collections.synchronizedMap(
            new LinkedHashMap<Long, Node>(DEFAULT_CACHE_INITIAL_CAPACITY, DEFAULT_CACHE_LOAD_FACTOR, true) {

                /**
                 * generated.
                 */
                private static final long serialVersionUID = -2064475227892362436L;
                /**
                 * Maximum size of this cache
                 */
                static final int MAXCACHESIZE = 64;
                /**
                 * {@inheritDoc}
                 */
                @Override
                protected boolean removeEldestEntry(final Map.Entry<Long, Node> anEldestEntry) {
                    return size() > MAXCACHESIZE;
                }
            });

    /**
     * did we load the driver already?
     * @see #loadDatabaseDriver()
     */
    private static boolean driverLoaded;

    /**
     * Our database-connection.
     */
    private JdbcConnectionPool myConnectionPool;

    /**
     * The URL of the database we are using.
     */
    private String myDatabaseURL;

    /**
     * True during imports.<br/>
     * During imports we return Nodes instead
     * of ExtendedNodes in {@link #getNodeByID(long)}.
     */
    private boolean myHintImporting = false;

    /**
     * @return the databaseURL
     */
    public String getDatabaseURL() {
        return myDatabaseURL;
    }

    /**
     * @return the databaseUser
     */
    public String getDatabaseUser() {
        return myDatabaseUser;
    }

    /**
     * @return the databasePassword
     */
    public String getDatabasePassword() {
        return myDatabasePassword;
    }

    /**
     * The user of the database we are using.
     */
    private String myDatabaseUser;

    /**
     * The password of the database we are using.
     */
    private String myDatabasePassword;

    /**
     * This class is not thread-safe.
     * It should not be used by more then 2 threads at a time.
     *
     */
    private class MyConnection implements IConnection {

        /**
         * The actual database-connection.
         */
        private Connection myConnection;

        /**
         * @return the connection
         */
        public Connection getConnection() {
            return myConnection;
        }

        /**
         * This constructor is only to be used by {@link H2DataSet#getConnection()}.
         * @param aConnection the underlying database-connection.
         */
        public MyConnection(final Connection aConnection) {
            super();
            myConnection = aConnection;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#close()
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
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#isClosed()
         */
        public boolean isClosed() throws SQLException {
            if (myConnection.isClosed()) {
                return true;
            }
            if (myConnection instanceof JdbcConnection) {
                return ((JdbcConnection) myConnection).getSession().isClosed();
            }
            return false;
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
         * the prepared statement used by {@link H2DataSet#getNodeByID(long)}.
         */
        private PreparedStatement myGetRelationsForNodeStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getWaysByID(long)}.
         */
        private PreparedStatement myGetRelationsForWayStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getWaysByTag(String, String)}.
         */
        private PreparedStatement mygetGetWayByTagStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getWaysByTag(String, String)}.
         */
        private PreparedStatement mygetGetNodesByTagStmt;

        /**
         * the prepared statement used by {@link H2DataSet#getWaysByTag(String, String)}.
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
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getAddNodeStmt()
         */
        public PreparedStatement getAddNodeStmt() throws SQLException {
            if (myAddNodeStmt == null) {
                myAddNodeStmt = myConnection.prepareStatement("MERGE INTO nodes (nodeid, version, lat, lon, location, tags) VALUES (?,?,?,?,?,?)");
            }
            return myAddNodeStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getAddWayNodeStmt()
         */
        public PreparedStatement getAddWayNodeStmt() throws SQLException {
            if (myAddWayNodeStmt == null) {
                myAddWayNodeStmt = myConnection.prepareStatement("MERGE INTO waynodes  (wayid, nodeid, index) VALUES (?,?,?)");
            }
            return myAddWayNodeStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getAddWayStmt()
         */
        public PreparedStatement getAddWayStmt() throws SQLException {
            if (myAddWayStmt == null) {
                myAddWayStmt = myConnection.prepareStatement("MERGE INTO ways  (wayid, version, tags) VALUES (?,?,?)");
            }
            return myAddWayStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getDeleteWaysNodesStmt()
         */
        public PreparedStatement getDeleteWaysNodesStmt() throws SQLException {
            if (myDeleteWaysNodesStmt == null) {
                myDeleteWaysNodesStmt = myConnection.prepareStatement("DELETE FROM waynodes  WHERE wayid = ?");
            }
            return myDeleteWaysNodesStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getGetNodeByIDStmt()
         */
        public PreparedStatement getGetNodeByIDStmt() throws SQLException {
            if (myGetNodeByIDStmt == null) {
                myGetNodeByIDStmt = myConnection.prepareStatement("select * from nodes where nodeid = ?");
            }
            return myGetNodeByIDStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getGetWaysForNodeStmt()
         */
        public PreparedStatement getGetWaysForNodeStmt() throws SQLException {
            if (myGetWaysForNodeStmt == null) {
                myGetWaysForNodeStmt = myConnection.prepareStatement("select wayid from waynodes where nodeid = ?");
            }
            return myGetWaysForNodeStmt;
        }

        /**
         * {@inheritDoc}
         */
        public PreparedStatement getGetWayByIDStmt() throws SQLException {
            if (myGetWayByIDStmt == null) {
                myGetWayByIDStmt = myConnection.prepareStatement("select * from ways where wayid = ?");
            }
            return myGetWayByIDStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getGetNodesForWayStmt()
         */
        public PreparedStatement getGetNodesForWayStmt() throws SQLException {
            if (myGetNodesForWayStmt == null) {
                myGetNodesForWayStmt = myConnection.prepareStatement("SELECT * FROM waynodes WHERE wayid = ? order by index");
            }
            myGetNodesForWayStmt.clearWarnings();
            return myGetNodesForWayStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getGetNodeByAreaStmt()
         */
        public PreparedStatement getGetNodeByAreaStmt() throws SQLException {
            if (myGetNodeByAreaStmt == null) {
                String sql = MultiDimension.getInstance().generatePreparedQuery("nodes", "location", new String[0]) + " TRUE";
                //                String sql = "select * FROM nodes WHERE lat < ? AND lat > ? AND lon < ? AND lon > ? ";
                myGetNodeByAreaStmt = myConnection.prepareStatement(sql);
            }
            myGetNodeByAreaStmt.clearWarnings();
            return myGetNodeByAreaStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getDeleteNodeStmt()
         */
        public PreparedStatement getDeleteNodeStmt() throws SQLException {
            if (myDeleteNodeStmt == null) {
                myDeleteNodeStmt = myConnection.prepareStatement("DELETE FROM nodes  WHERE nodeid = ?");
            }
            return myDeleteNodeStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getDeleteWayStmt()
         */
        public PreparedStatement getDeleteWayStmt() throws SQLException {
            if (myDeleteWayStmt == null) {
                myDeleteWayStmt = myConnection.prepareStatement("DELETE FROM ways  WHERE wayid = ?");
            }
            return myDeleteWayStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getDeleteRelationStmt()
         */
        public PreparedStatement getDeleteRelationStmt() throws SQLException {
            if (myDeleteRelationStmt == null) {
                myDeleteRelationStmt = myConnection.prepareStatement("DELETE FROM relations  WHERE relid = ?");
            }
            return myDeleteRelationStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getAddRelationStmt()
         */
        public PreparedStatement getAddRelationStmt() throws SQLException {
            if (myAddRelationStmt == null) {
                myAddRelationStmt = myConnection.prepareStatement("MERGE INTO relations  (relid, version, tags) VALUES (?,?,?)");
            }
            return myAddRelationStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getAddRelationMemberStmt()
         */
        public PreparedStatement getAddRelationMemberStmt() throws SQLException {
            if (myAddRelationMemberStmt == null) {
                myAddRelationMemberStmt = myConnection.prepareStatement("MERGE INTO relmembers  (relid, entityid, entitytype, index, role) VALUES (?,?,?,?,?)");
            }
            return myAddRelationMemberStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getDeleteRelationMemberStmt()
         */
        public PreparedStatement getDeleteRelationMemberStmt() throws SQLException {
            if (myDeleteRelationMemberStmt == null) {
                myDeleteRelationMemberStmt = myConnection.prepareStatement("DELETE FROM relmembers WHERE relid = ?");
            }
            return myDeleteRelationMemberStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getGetRelationByID()
         */
        public PreparedStatement getGetRelationByID() throws SQLException {
            if (myGetRelationByID == null) {
                myGetRelationByID = myConnection.prepareStatement("SELECT * FROM relations WHERE relid = ?");
            }
            return myGetRelationByID;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getGetMembersForRelation()
         */
        public PreparedStatement getGetMembersForRelation() throws SQLException {
            if (myGetMembersForRelation == null) {
                myGetMembersForRelation = myConnection.prepareStatement("SELECT * FROM relmembers WHERE relid = ? order by index");
            }
            return myGetMembersForRelation;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getGetRelationsForNodeStmt()
         */
        public PreparedStatement getGetRelationsForNodeStmt() throws SQLException {
            if (myGetRelationsForNodeStmt == null) {
                myGetRelationsForNodeStmt = myConnection.prepareStatement("SELECT relid FROM relmembers WHERE entityid = ? AND entitytype = " + EntityType.Node.ordinal());
            }
            return myGetRelationsForNodeStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getGetRelationsForWayStmt()
         */
        public PreparedStatement getGetRelationsForWayStmt() throws SQLException {
            if (myGetRelationsForWayStmt == null) {
                myGetRelationsForWayStmt = myConnection.prepareStatement("SELECT relid FROM relmembers WHERE entityid = ? AND entitytype = " + EntityType.Way.ordinal());
            }
            return myGetRelationsForWayStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getGetWayByTagStmt()
         */
        public PreparedStatement getGetWayByTagStmt() throws SQLException {
            if (mygetGetWayByTagStmt == null) {
                mygetGetWayByTagStmt = myConnection.prepareStatement("SELECT wayid FROM ways WHERE tags like ?");
            }
            return mygetGetWayByTagStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getGetNodesByTagStmt()
         */
        public PreparedStatement getGetNodesByTagStmt() throws SQLException {
            if (mygetGetNodesByTagStmt == null) {
                mygetGetNodesByTagStmt = myConnection.prepareStatement("SELECT nodeid FROM nodes WHERE tags like ?");
            }
            return mygetGetNodesByTagStmt;
        }

        /**
         * {@inheritDoc}
         * @see org.openstreetmap.osm.data.h2.IConnection#getGetRelByTagStmt()
         */
        public PreparedStatement getGetRelByTagStmt() throws SQLException {
            if (mygetGetRelByTagStmt == null) {
                mygetGetRelByTagStmt = myConnection.prepareStatement("SELECT relid FROM relations WHERE tags like ?");
            }
            return mygetGetRelByTagStmt;
        }
    }

    /**
     * Do not use this queue directly but use
     * #getConnection() and #returnConnection() instead.
     */
    private Queue<IConnection> myConnection = new LinkedBlockingQueue<IConnection>();

    /**
     * counter used for debugging.
     */
    private int myTotalConnectionsCount;

    /**
     * counter used for debugging.
     */
    private int totalClosedConnections;

    /**
     * Store the map in the directory denoted by the config-property "map.dir".
     */
    public H2DataSet() {
        this(new File(Settings.getInstance().get("map.dir",
                Settings.getInstance().get("tiledMapCache.dir",
                        System.getProperty("user.home")
                        + File.separator + ".openstreetmap" + File.separator + "map" + File.separator))));
    }

    /**
     * @param aDatabaseURL the database to connect to.
     */
    protected H2DataSet(final String aDatabaseURL) {
        super();
        this.myDatabaseURL = aDatabaseURL;
        this.myDatabaseUser = "";
        this.myDatabasePassword = "";
    }

    /**
     * @param aDatabaseURL the database to connect to.
     * @param aDatabaseUser the username to use
     * @param aDatabasePassword the password to use
     */
    protected H2DataSet(final String aDatabaseURL, final String aDatabaseUser, final String aDatabasePassword) {
        super();
        this.myDatabaseURL = aDatabaseURL;
        this.myDatabaseUser = aDatabaseUser;
        this.myDatabasePassword = aDatabasePassword;
    }

    /**
     * @param aDatabaseFile the base-filename of the database-files to use.
     */
    public H2DataSet(final File aDatabaseFile) {
        super();
        this.myDatabaseURL = "jdbc:h2:" + aDatabaseFile.getAbsolutePath() + ";AUTO_SERVER=TRUE";      // Curt
        this.myDatabaseUser = "";
        this.myDatabasePassword = "";
    }

    /**
     * Give a hint that we are to start
     * inserting/updating lots of data
     * without giving hints about the
     * aproximate number of imported items.
     */
    public void hintImportStarting() {
        try {
            IConnection connection = getConnection();
            Statement stmt = connection.getConnection().createStatement();
            try {
                this.myHintImporting = true;
                //stmt.execute("SET CACHE_SIZE 65536");
                stmt.execute("SET LOCK_MODE 0");
                stmt.execute("SET LOG 0");
                stmt.execute("SET UNDO_LOG 0");
                stmt.execute("SET CACHE_SIZE 393216"); //384MB cache instead of 16MB
            } finally {
                stmt.close();
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot disable transaction log for import", e);
        }
    }

    /**
     * @se {@link #hintImportStarting()}
     */
    public void hintImportEnded() {
        try {
            IConnection connection = getConnection();
            Statement stmt = connection.getConnection().createStatement();
            try {
                this.myHintImporting = true;
                stmt.execute("SET CACHE_SIZE 16384");
                stmt.execute("SET LOCK_MODE 3");
                stmt.execute("SET LOG 0");
                stmt.execute("SET UNDO_LOG 0");
            } finally {
                stmt.close();
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot re-enable transaction log for import", e);
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
        if (myConnectionPool == null) {

            loadDatabaseDriver();

            try {
                myConnectionPool = JdbcConnectionPool.create(getDatabaseURL(), getDatabaseUser(), getDatabasePassword());
                Connection connection = myConnectionPool.getConnection();
                if (connection == null) {
                    throw new SQLException("could not get database-connection from pool!");
                }
                checkSchema(connection);
                myConnectionPool.setMaxConnections(Short.MAX_VALUE);

            } catch (SQLException e) {
                throw new OsmosisRuntimeException(
                        "Unable to establish a database connection to '" + myDatabaseURL + "'.", e);
            }
        }
        IConnection connection = myConnection.poll();
        if (connection == null || connection.isClosed()) {
            try {
                LOG.log(Level.INFO, "Opening new connection. " + (connection == null?"because pool is empty" : "because pooled-connection is closed")
                        + " (#connections so far: " + myTotalConnectionsCount + ", #closed=" + totalClosedConnections + ") DB=" + myDatabaseURL/*, new Exception("DEBUG")*/);
                myTotalConnectionsCount++;
                connection = new MyConnection(myConnectionPool.getConnection());
            } catch (NullPointerException e) {
                //connection = new MyConnection(myConnectionPool.getConnection());
                myConnectionPool = null;
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
            totalClosedConnections++;
        }
    }

    /**
     * Make sure all tables we need exist.
     * Convert old versions of the database-schema if needed.
     * @param aConnection our connections. Not yet used.
     * @throws SQLException if we cannot create/alter the tables or the current schma is incompatible.
     */
    protected void checkSchema(final Connection aConnection) throws SQLException {
        try {
            Statement stmt = aConnection.createStatement();
            try {
                stmt.executeUpdate("CREATE CACHED TABLE  IF NOT EXISTS nodes ("
                        + "nodeid BIGINT PRIMARY KEY,"
                        + "version INT,"
                        + "lat INT ,"
                        + "lon INT ,"
                        + "location BIGINT ,"
                        + "tags LONGVARCHAR(32767)"
                        + ");");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS nodesLocation ON nodes (location);");
                stmt.executeUpdate("CREATE CACHED TABLE  IF NOT EXISTS waynodes ("
                        + "wayid BIGINT NOT NULL,"
                        + "nodeid BIGINT NOT NULL,"
                        + "index INT NOT NULL,"
                        + "PRIMARY KEY (wayid, index)"
                        + ");");
                //                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS waynodes ON waynodes (wayid);"); //TODO: maybe remove this index
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS nodeways ON waynodes (nodeid);");
                stmt.executeUpdate("CREATE CACHED TABLE  IF NOT EXISTS ways ("
                        + "wayid BIGINT PRIMARY KEY,"
                        + "version INT,"
                        + "tags LONGVARCHAR(32767)"
                        + ");");
                stmt.executeUpdate("CREATE CACHED TABLE  IF NOT EXISTS relations ("
                        + "relid BIGINT PRIMARY KEY,"
                        + "version INT,"
                        + "tags LONGVARCHAR(32767)"
                        + ");");
                stmt.executeUpdate("CREATE CACHED TABLE  IF NOT EXISTS relmembers ("
                        + "relid BIGINT NOT NULL,"
                        + "entityid BIGINT NOT NULL,"
                        + "entitytype SMALLINT NOT NULL,"
                        + "index INT NOT NULL,"
                        + "role VARCHAR(64),"
                        + "PRIMARY KEY (relid, index)"
                        + ");");
                //                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS relmembers ON relmembers (relid);");
                stmt.executeUpdate("CREATE INDEX IF NOT EXISTS memberrels ON relmembers (entityid, entitytype);");

            } finally {
                stmt.close();
            }
        } finally {
            aConnection.close();
        }
    }

    /**
     * Utility method for ensuring that the database driver is registered.
     */
    protected void loadDatabaseDriver() {
        if (!driverLoaded) {
            // Lock to ensure two threads don't try to load the driver at the same time.
            synchronized (H2DataSet.class) {
                // Check again to ensure another thread hasn't loaded the driver
                // while we waited for the lock.
                try {
                    Class.forName("org.h2.Driver");

                } catch (ClassNotFoundException e) {
                    throw new OsmosisRuntimeException(
                            "Unable to find database driver.", e);
                }

                driverLoaded = true;
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public void addNode(final Node aN) {
        try {
            myNodeCache.put(aN.getId(), aN);
            int i = 1;
            IConnection con = getConnection();
            try {
                PreparedStatement addNodeStmt = con.getAddNodeStmt();
                addNodeStmt.setLong(i++, aN.getId());
                addNodeStmt.setInt(i++, aN.getVersion());
                final int lat = FixedPrecisionCoordinateConvertor.convertToFixed(aN.getLatitude());
                addNodeStmt.setInt(i++, lat);
                final int lon = FixedPrecisionCoordinateConvertor.convertToFixed(aN.getLongitude());
                addNodeStmt.setInt(i++, lon);
                
                /* We normalize the node's lat and lon values for the z-curve
                 * index. We do this for lat and lon seperately so that the
                 * index can work more efficiently.
                 */
                final int fixedMinLat = FixedPrecisionCoordinateConvertor.convertToFixed(-Projection.MAX_LAT);
                final int fixedMinLon = FixedPrecisionCoordinateConvertor.convertToFixed(-Projection.MAX_LON);
                final int fixedMaxLat = FixedPrecisionCoordinateConvertor.convertToFixed(Projection.MAX_LAT);
                final int fixedMaxLon = FixedPrecisionCoordinateConvertor.convertToFixed(Projection.MAX_LON);

                final int latNormalized = MultiDimension.getInstance().normalize(2, lat, fixedMinLat, fixedMaxLat);
                final int lonNormalized = MultiDimension.getInstance().normalize(2, lon, fixedMinLon, fixedMaxLon);
                
                final long location = MultiDimension.getInstance().interleave(new int[]{latNormalized, lonNormalized}); 

                addNodeStmt.setLong(i++, location);
                addNodeStmt.setString(i++, serializeTags(aN.getTags()));
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
     * Helper-method to serialize a list of Tags into a single CLOB.
     * @param aTags the tags to serialize
     * @return the serialized tags
     */
    protected String serializeTags(final Collection<Tag> aTags) {
        StringBuilder retval = new StringBuilder();
        for (Tag tag : aTags) {
            String key = tag.getKey();
            String value = tag.getValue();
            //TODO: escape
            retval.append(key).append("==").append(value).append("##");
        }
        return retval.toString();
    }

    /**
     * Helper-method to deserialize a list of tags from a CLOB.
     * @param attrs addNodeStmt
     * @return the deserialized tags
     */
    protected Collection<Tag> deserializeTags(final String attrs) {
        String[] split = attrs.split("##"); //TODO: optimize this
        Collection<Tag> retval = new ArrayList<Tag>(split.length);
        for (String string : split) {
            String[] split2 = string.split("==");
            if (split2.length == 2) {
                String key = split2[0];
                String value = split2[1];
                retval.add(new Tag(key, value));
            }
        }
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    public void addRelation(final Relation aR) {
        try {
            IConnection connection = getConnection();
            try {
                PreparedStatement addRelStmt = connection.getAddRelationStmt();
                int i = 1;
                addRelStmt.setLong(i++, aR.getId());
                addRelStmt.setInt(i++, aR.getVersion());
                addRelStmt.setString(i++, serializeTags(aR.getTags()));
                addRelStmt.execute();

                PreparedStatement deleteRelationMemberStmt = connection.getDeleteRelationMemberStmt();
                deleteRelationMemberStmt.setLong(1, aR.getId());
                deleteRelationMemberStmt.execute();

                PreparedStatement addRelationMemberStmt = connection.getAddRelationMemberStmt();
                List<RelationMember> members = aR.getMembers();
                int index = 0;
                for (RelationMember member : members) {
                    i = 1;
                    addRelationMemberStmt.setLong(i++, aR.getId());
                    addRelationMemberStmt.setLong(i++, member.getMemberId());
                    addRelationMemberStmt.setShort(i++, (short) member.getMemberType().ordinal());
                    addRelationMemberStmt.setInt(i++, index++);
                    addRelationMemberStmt.setString(i++, member.getMemberRole());
                    addRelationMemberStmt.execute();
                }
                connection.getConnection().commit();
            } finally {
                returnConnection(connection);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot add relation", e);
        }

    }

    /**
     * {@inheritDoc}
     */
    public void addWay(final Way aW) {
        try {
            IConnection connection = getConnection();
            try {
                PreparedStatement addWayStmt = connection.getAddWayStmt();
                int i = 1;
                addWayStmt.setLong(i++, aW.getId());
                addWayStmt.setInt(i++, aW.getVersion());
                addWayStmt.setString(i++, serializeTags(aW.getTags()));
                addWayStmt.execute();

                PreparedStatement deleteWaysNodesStmt = connection.getDeleteWaysNodesStmt();
                deleteWaysNodesStmt.setLong(1, aW.getId());
                deleteWaysNodesStmt.execute();

                PreparedStatement addWayNodeStmt = connection.getAddWayNodeStmt();
                List<WayNode> wayNodes = aW.getWayNodes();
                int index = 0;
                for (WayNode wayNode : wayNodes) {
                    i = 1;
                    addWayNodeStmt.setLong(i++, aW.getId());
                    addWayNodeStmt.setLong(i++, wayNode.getNodeId());
                    addWayNodeStmt.setInt(i++, index++);
                    addWayNodeStmt.execute();
                    Node cached = myNodeCache.get(wayNode.getNodeId());
                    if (cached != null && cached instanceof ExtendedNode) {
                        ((ExtendedNode) cached).addReferencedWay(aW.getId());
                    }
                }
                connection.getConnection().commit();
            } finally {
                returnConnection(connection);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot add way", e);
        }

    }

    /**
     * {@inheritDoc}
     */
    public boolean containsNode(final Node aW) {
        Node cached = myNodeCache.get(aW.getId());
        if (cached != null) {
            return true;
        }
        try {
            IConnection connection = getConnection();
            try {
                PreparedStatement getNodeByIDStmt = connection.getGetNodeByIDStmt();
                getNodeByIDStmt.setLong(1, aW.getId());
                ResultSet rs = getNodeByIDStmt.executeQuery();
                try {
                    while (rs.next()) {
                        rs.close();
                        return true;
                    }
                } finally {
                    rs.close();
                }
            } finally {
                returnConnection(connection);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot check existance of node by ID", e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsRelation(final Relation aR) {
        try {
            IConnection connection = getConnection();
            try {
                PreparedStatement getRelationByIDStmt = connection.getGetRelationByID();
                getRelationByIDStmt.setLong(1, aR.getId());
                ResultSet rs = getRelationByIDStmt.executeQuery();
                try {
                    if (!rs.next()) {
                        return false;
                    }
                    return true;
                } finally {
                    rs.close();
                }
            } finally {
                returnConnection(connection);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot check existance of relation by id", e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsWay(final Way aW) {
        try {
            IConnection connection = getConnection();
            try {
                PreparedStatement getWayByIDStmt = connection.getGetWayByIDStmt();
                getWayByIDStmt.setLong(1, aW.getId());
                ResultSet rs = getWayByIDStmt.executeQuery();
                try {
                    if (!rs.next()) {
                        return false;
                    }

                    return true;
                } finally {
                    rs.close();
                }
            } finally {
                returnConnection(connection);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot check existance of way by id", e);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Node getNearestNode(final LatLon aLastGPSPos, final Selector aSelector) {

        try {
            double radius = SEARCHSTARTRADIUS;
            Iterator<Node> nodes = getNodes(new Bounds(aLastGPSPos.lat() - radius,
                    aLastGPSPos.lon() - radius,
                    aLastGPSPos.lat() + radius,
                    aLastGPSPos.lon() + radius));
            double minDist = Double.MAX_VALUE;
            Node minDistNode = null;
            while (nodes.hasNext()) {
                Node node = nodes.next();
                if (node == null) {
                    continue;
                }
                if (aSelector != null && !aSelector.isAllowed(this, node))
                    continue;
                LatLon pos = new LatLon(node.getLatitude(), node.getLongitude());
                double dist = Math.sqrt(pos.distance(aLastGPSPos));             // sqrt for comparison with radius

                if (dist < minDist && dist < radius) {
                    minDist = dist;
                    minDistNode = node;
                }
            }
            if (minDistNode != null) {
                return minDistNode;
            }
            while (radius < 2) {
                radius = radius * 2;
                nodes = getNodes(new Bounds(aLastGPSPos.lat() - radius,
                        aLastGPSPos.lon() - radius,
                        aLastGPSPos.lat() + radius,
                        aLastGPSPos.lon() + radius));
                while (nodes.hasNext()) {
                    Node node = nodes.next();
                    if (node == null) {
                        continue;
                    }
                    if (aSelector != null && !aSelector.isAllowed(this, node))
                        continue;
                    LatLon pos = new LatLon(node.getLatitude(), node.getLongitude());
                    double dist = Math.sqrt(pos.distance(aLastGPSPos));             // sqrt for comparison with radius

                    if (dist < minDist && dist < radius) {
                        minDist = dist;
                        minDistNode = node;
                    }
                }
                if (minDistNode != null) {
                    return minDistNode;
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot get NearestNode", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
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
     * This method only returns an extendedNode if one is in the cache.
     * @param connection the connection to use ({@link #returnConnection(IConnection)} is called automaticaly)
     * @param aStatement a prepared-statement to execute
     * @return the ExtendedNode of the first way returned or null
     * @throws SQLException may happen
     */
    private Node getSimpleNodeFromQuery(final IConnection connection,
            final PreparedStatement aStatement) throws SQLException {
        try {
            ResultSet rs = aStatement.executeQuery();
            try {
                while (rs.next()) {
                    double lat = FixedPrecisionCoordinateConvertor.convertToDouble(rs.getInt("lat"));
                    double lon = FixedPrecisionCoordinateConvertor.convertToDouble(rs.getInt("lon"));
                    long nodeID = rs.getLong("nodeid");
                    Node cached = myNodeCache.get(nodeID);
                    if (cached != null) {
                        return cached;
                    }
                    Node node = new Node(nodeID,
                            rs.getInt("version"),
                            new Date(),
                            OsmUser.NONE,
                            0,
                            lat,
                            lon);
                    String attrs = rs.getString("tags");
                    node.getTags().addAll(deserializeTags(attrs));
                    rs.close();

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
                    double lat = FixedPrecisionCoordinateConvertor.convertToDouble(rs.getInt("lat"));
                    double lon = FixedPrecisionCoordinateConvertor.convertToDouble(rs.getInt("lon"));
                    long nodeID = rs.getLong("nodeid");
                    Node cached = myNodeCache.get(nodeID);
                    if (cached != null) {
                        return cached;
                    }
                    ExtendedNode node = new ExtendedNode(nodeID,
                            rs.getInt("version"),
                            0,
                            lat,
                            lon);
                    String attrs = rs.getString("tags");
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

    /**
     * @param connection the connection to use ({@link #returnConnection(IConnection)} is called automatically)
     * @param aStatement a prepared-statement to execute
     * @return all ExtendedNodes, never null
     * @throws SQLException may happen
     */
    private List<Node> getNodesFromQuery(final IConnection connection,
            final PreparedStatement aStatement) throws SQLException {
        List<Node> retval = new LinkedList<Node>();
        try {
            ResultSet rs = aStatement.executeQuery();
            try {
                while (rs.next()) {
                    double lat = FixedPrecisionCoordinateConvertor.convertToDouble(rs.getInt("lat"));
                    double lon = FixedPrecisionCoordinateConvertor.convertToDouble(rs.getInt("lon"));
                    final long nodeID = rs.getLong("nodeid");
                    Node cached = myNodeCache.get(nodeID);
                    if (cached != null) {
                        retval.add(cached);
                        continue;
                    }
                    ExtendedNode node = new ExtendedNode(nodeID,
                            rs.getInt("version"),
                            0,
                            lat,
                            lon);
                    String attrs = rs.getString("tags");
                    node.getTags().addAll(deserializeTags(attrs));
                    rs.close();

                    PreparedStatement getWaysForNodeStmt = connection.getGetWaysForNodeStmt();
                    getWaysForNodeStmt.setLong(1, node.getId());
                    ResultSet rs2 = getWaysForNodeStmt.executeQuery();
                    try {
                        while (rs2.next()) {
                            node.addReferencedWay(rs2.getLong("wayid"));
                        }
                    } finally {
                        rs2.close();
                    }

                    PreparedStatement getRelationsForNodeStmt = connection.getGetRelationsForNodeStmt();
                    getRelationsForNodeStmt.setLong(1, node.getId());
                    rs2 = getRelationsForNodeStmt.executeQuery();
                    try {
                        while (rs2.next()) {
                            node.addReferencedRelation(rs2.getLong("relid"));
                        }
                    } finally {
                        rs2.close();
                    }

                    myNodeCache.put(node.getId(), node);
                    retval.add(node);
                }
            } finally {
                rs.close();
            }
        } finally {
            returnConnection(connection);
        }
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Node> getNodes(final Bounds aBoundingBox) {
        Collection<Node> retval = new LinkedList<Node>();
        try {
            IConnection connection = getConnection();
            try {
                final LatLon minLoc = aBoundingBox.getMin();
                final LatLon maxLoc = aBoundingBox.getMax();
                
                final int boxMinLat = FixedPrecisionCoordinateConvertor.convertToFixed(minLoc.lat());
                final int boxMinLon = FixedPrecisionCoordinateConvertor.convertToFixed(minLoc.lon());
                final int boxMaxLat = FixedPrecisionCoordinateConvertor.convertToFixed(maxLoc.lat());
                final int boxMaxLon = FixedPrecisionCoordinateConvertor.convertToFixed(maxLoc.lon());
                
                final int fixedMinLat = FixedPrecisionCoordinateConvertor.convertToFixed(-Projection.MAX_LAT);
                final int fixedMinLon = FixedPrecisionCoordinateConvertor.convertToFixed(-Projection.MAX_LON);
                final int fixedMaxLat = FixedPrecisionCoordinateConvertor.convertToFixed(Projection.MAX_LAT);
                final int fixedMaxLon = FixedPrecisionCoordinateConvertor.convertToFixed(Projection.MAX_LON);
                
                final int minLatNormalized = MultiDimension.getInstance().normalize(2, FixedPrecisionCoordinateConvertor.convertToFixed(minLoc.lat()), fixedMinLat, fixedMaxLat);
                final int minLonNormalized = MultiDimension.getInstance().normalize(2, FixedPrecisionCoordinateConvertor.convertToFixed(minLoc.lon()), fixedMinLon, fixedMaxLon);
                final int maxLatNormalized = MultiDimension.getInstance().normalize(2, FixedPrecisionCoordinateConvertor.convertToFixed(maxLoc.lat()), fixedMinLat, fixedMaxLat);
                final int maxLonNormalized = MultiDimension.getInstance().normalize(2, FixedPrecisionCoordinateConvertor.convertToFixed(maxLoc.lon()), fixedMinLon, fixedMaxLon);
                
                final long locationMin = MultiDimension.getInstance().interleave(new int[]{minLatNormalized, minLonNormalized});
                final long locationMax = MultiDimension.getInstance().interleave(new int[]{maxLatNormalized, maxLonNormalized});
                
                PreparedStatement pstmt = null;
                try {
                    String sql = "select * from nodes where (location between ? and ?) and (lat BETWEEN ? AND ?) AND (lon BETWEEN ? AND ?) ";
                    pstmt = connection.getConnection().prepareStatement(sql);
                    pstmt.setLong(1, locationMin);
                    pstmt.setLong(2, locationMax);
                    pstmt.setInt(3, boxMinLat);
                    pstmt.setInt(4, boxMaxLat);
                    pstmt.setInt(5, boxMinLon);
                    pstmt.setInt(6, boxMaxLon);
                    ResultSet rs = pstmt.executeQuery();
                    try {
                        while (rs.next()) {
                            double lat = FixedPrecisionCoordinateConvertor.convertToDouble(rs.getInt("lat"));
                            double lon = FixedPrecisionCoordinateConvertor.convertToDouble(rs.getInt("lon"));
                            ExtendedNode node = new ExtendedNode(rs.getLong("nodeid"),
                                    rs.getInt("version"),
                                    0,
                                    lat,
                                    lon);
                            String attrs = rs.getString("tags");
                            node.getTags().addAll(deserializeTags(attrs));
                            retval.add(node);

                            PreparedStatement getWaysForNodeStmt = connection.getGetWaysForNodeStmt();
                            getWaysForNodeStmt.setLong(1, node.getId());
                            ResultSet rs2 = getWaysForNodeStmt.executeQuery();
                            try {
                                while (rs2.next()) {
                                    node.addReferencedWay(rs2.getLong("wayid"));
                                }
                            } finally {
                                rs2.close();
                            }

                            PreparedStatement getRelationsForNodeStmt = connection.getGetRelationsForNodeStmt();
                            getRelationsForNodeStmt.setLong(1, node.getId());
                            rs2 = getRelationsForNodeStmt.executeQuery();
                            try {
                                while (rs2.next()) {
                                    node.addReferencedRelation(rs2.getLong("relid"));
                                }
                            } finally {
                                rs2.close();
                            }
                            myNodeCache.put(node.getId(), node);
                        }
                    } finally {
                        rs.close();
                    }
                } finally {
                    if(pstmt!=null){
                        pstmt.close();
                    }
                }
            } finally {
                returnConnection(connection);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot get nodes by area", e);
        }
        return retval.iterator();
    }


    
    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#getNodesByName(java.lang.String)
     */
    public Iterator<Node> getNodesByName(final String aLookFor) {
        return getNodesByTag(Tags.TAG_NAME, aLookFor);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Node> getNodesByTag(final String aKey, final String aValue) {
        try {
            IConnection connection = getConnection();
            PreparedStatement getNodesByTagStmt = connection.getGetNodesByTagStmt();
            getNodesByTagStmt.setString(1, "%" + aKey + "==" + aValue + "##%");
            return getNodesFromQuery(connection, getNodesByTagStmt).iterator();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot get way by id", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Relation getRelationByID(final long aRelationID) {
        try {
            IConnection connection = getConnection();
            try {
                PreparedStatement getRelationByIDStmt = connection.getGetRelationByID();
                getRelationByIDStmt.setLong(1, aRelationID);
                ResultSet rs = getRelationByIDStmt.executeQuery();
                try {
                    if (!rs.next()) {
                        return null;
                    }

                    Relation rel = new Relation(rs.getLong("relid"), rs.getInt("version"), new Date(), OsmUser.NONE, 0);
                    rel.getTags().addAll(deserializeTags(rs.getString("tags")));
                    PreparedStatement getMembersForRelation = connection.getGetMembersForRelation();
                    getMembersForRelation.setLong(1, aRelationID);
                    ResultSet rs2 = getMembersForRelation.executeQuery();
                    try {
                        while (rs2.next()) {
                            String role     = rs2.getString("role");
                            long entityid   = rs2.getLong("entityid");
                            short t         = rs2.getShort("entitytype");
                            EntityType type = EntityType.values()[t];
                            rel.getMembers().add(new RelationMember(entityid, type, role));
                        }
                    } finally {
                        rs2.close();
                    }
                    return rel;
                } finally {
                    rs.close();
                }
            } finally {
                returnConnection(connection);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot get relation by id", e);
        }
        return null;

    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#getRelations(org.openstreetmap.osm.data.coordinates.Bounds)
     */
    public Iterator<Relation> getRelations(final Bounds aBoundingBox) {
        // TODO Auto-generated method stub
        return new LinkedList<Relation>().iterator();
    }

    /**
     * {@inheritDoc}
     */
    public WayHelper getWayHelper() {
        return new WayHelper(this);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Way> getWays(final Bounds aBoundingBox) {
        Iterator<Node> nodes = getNodes(aBoundingBox);
        Set<Long> wayIDs = new HashSet<Long>();
        while (nodes.hasNext()) {
            ExtendedNode node = (ExtendedNode) nodes.next();
            wayIDs.addAll(node.getReferencedWayIDs());
        }
        List<Way> retval = new ArrayList<Way>(wayIDs.size());
        for (Long wayID : wayIDs) {
            Way way = getWaysByID(wayID);
            if (way == null) {
                LOG.log(Level.WARNING, "Cannot load way " + wayID
                        + " from database but a node in the database "
                        + "references it, so it must be there.");
                continue;
            }
            retval.add(way);
        }
        return retval.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public Way getWaysByID(final long aWayID) {
        try {
            IConnection connection = getConnection();
            PreparedStatement getWayByIDStmt = connection.getGetWayByIDStmt();
            getWayByIDStmt.setLong(1, aWayID);
            return getWayFromQuery(connection, getWayByIDStmt);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot get way by id", e);
        }
        return null;
    }

    /**
     * @param connection the connection to use ({@link #returnConnection(IConnection)} is called automaticaly)
     * @param aStatement a prepared-statement to execute
     * @return the ExtendedWay of the first way returned or null
     * @throws SQLException may happen
     */
    private Way getWayFromQuery(final IConnection connection,
            final PreparedStatement aStatement) throws SQLException {
        try {
            ResultSet rs = aStatement.executeQuery();
            try {
                if (!rs.next()) {
                    return null;
                }

                ExtendedWay way = new ExtendedWay(rs.getLong("wayid"), rs.getInt("version"));
                way.getTags().addAll(deserializeTags(rs.getString("tags")));

                PreparedStatement getNodesForWayStmt = connection.getGetNodesForWayStmt();
                getNodesForWayStmt.setLong(1, way.getId());
                ResultSet rs2 = getNodesForWayStmt.executeQuery();
                try {
                    while (rs2.next()) {
                        way.getWayNodes().add(new WayNode(rs2.getLong("nodeid")));
                    }
                } finally {
                    rs2.close();
                }

                PreparedStatement getRelationsForWayStmt = connection.getGetRelationsForWayStmt();
                getRelationsForWayStmt.setLong(1, way.getId());
                rs2 = getRelationsForWayStmt.executeQuery();
                try {
                    while (rs2.next()) {
                        way.addReferencedRelation(rs2.getLong("relid"));
                    }
                } finally {
                    rs2.close();
                }
                return way;
            } finally {
                rs.close();
            }
        } finally {
            returnConnection(connection);
        }
    }

    /**
     * @param connection the connection to use ({@link #returnConnection(IConnection)} is called automaticaly)
     * @param aStatement a prepared-statement to execute
     * @return all ExtendedWays, never null
     * @throws SQLException may happen
     */
    private List<Way> getWaysFromQuery(final IConnection connection,
            final PreparedStatement aStatement) throws SQLException {
        List<Way> retval = new LinkedList<Way>();
        try {
            ResultSet rs = aStatement.executeQuery();
            try {
                while (rs.next()) {

                    ExtendedWay way = new ExtendedWay(rs.getLong("wayid"), rs.getInt("version"));
                    way.getTags().addAll(deserializeTags(rs.getString("tags")));

                    PreparedStatement getNodesForWayStmt = connection.getGetNodesForWayStmt();
                    getNodesForWayStmt.setLong(1, way.getId());
                    ResultSet rs2 = getNodesForWayStmt.executeQuery();
                    try {
                        while (rs2.next()) {
                            way.getWayNodes().add(new WayNode(rs2.getLong("nodeid")));
                        }
                    } finally {
                        rs2.close();
                    }

                    PreparedStatement getRelationsForWayStmt = connection.getGetRelationsForWayStmt();
                    getRelationsForWayStmt.setLong(1, way.getId());
                    rs2 = getRelationsForWayStmt.executeQuery();
                    try {
                        while (rs2.next()) {
                            way.addReferencedRelation(rs2.getLong("relid"));
                        }
                    } finally {
                        rs2.close();
                    }
                    retval.add(way);
                }
            } finally {
                rs.close();
            }
        } finally {
            returnConnection(connection);
        }
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Way> getWaysByName(final String aLookFor, final Bounds aBoundingBox) {
        return getWaysByTag(Tags.TAG_NAME, aLookFor);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Way> getWaysByTag(final String aKey, final String aValue) {
        try {
            IConnection connection = getConnection();
            PreparedStatement getWayByTagStmt = connection.getGetWayByTagStmt();
            getWayByTagStmt.setString(1, "%" + aKey + "==" + aValue + "##%");
            return getWaysFromQuery(connection, getWayByTagStmt).iterator();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot get way by id", e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Way> getWaysForNode(final long aNodeID) {
        Set<Way> retval = new HashSet<Way>();
        try {
            Set<Long> wayids = new HashSet<Long>();
            Node cached = myNodeCache.get(aNodeID);
            if (cached != null && cached instanceof ExtendedNode) {
                wayids = ((ExtendedNode) cached).getReferencedWayIDs();
            } else {
                IConnection connection = getConnection();
                try {
                    PreparedStatement getWaysForNodeStmt = connection.getGetWaysForNodeStmt();
                    getWaysForNodeStmt.setLong(1, aNodeID);
                    ResultSet rs2 = getWaysForNodeStmt.executeQuery();
                    try {
                        while (rs2.next()) {
                            wayids.add(rs2.getLong("wayid"));
                        }
                    } finally {
                        rs2.close();
                    }
                } finally {
                    returnConnection(connection);
                }
            }
            for (Long id : wayids) {
                Way w = getWaysByID(id);
                if (w != null) {
                    retval.add(w);
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot get ways for node", e);
        }
        return retval.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public void removeNode(final Node aW) {
        try {
            myNodeCache.remove(aW.getId());
            IConnection connection = getConnection();
            try {
                PreparedStatement deleteNodeStmt = connection.getDeleteNodeStmt();
                deleteNodeStmt.setLong(1, aW.getId());
                deleteNodeStmt.execute();
            } finally {
                returnConnection(connection);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot remove node", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeRelation(final Relation aR) {
        try {
            IConnection connection = getConnection();
            try {
                PreparedStatement deleteRelationStmt = connection.getDeleteRelationStmt();
                deleteRelationStmt.setLong(1, aR.getId());
                deleteRelationStmt.execute();
                PreparedStatement deleteRelationMemberStmt = connection.getDeleteRelationMemberStmt();
                deleteRelationMemberStmt.setLong(1, aR.getId());
                deleteRelationMemberStmt.execute();
            } finally {
                returnConnection(connection);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot remove relation", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeWay(final Way aW) {
        try {
            IConnection connection = getConnection();
            try {
                PreparedStatement deleteWayStmt = connection.getDeleteWayStmt();
                deleteWayStmt.setLong(1, aW.getId());
                deleteWayStmt.execute();
            } finally {
                returnConnection(connection);
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot remove way", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        while (!myConnection.isEmpty()) {
            IConnection poll = myConnection.poll();
            if (poll == null) {
                return;
            }
            poll.close();
        }

    }

    /**
     * {@inheritDoc}
     */
    public ConfigurationSection getSettings() {
        return null;
    }

    /**
     * Test, if currently running an import.
     * @return <code >true</code>, if importing, <code >false</code> otherwise.
     */
    public boolean isImporting() {
        return myHintImporting;
    }
}

