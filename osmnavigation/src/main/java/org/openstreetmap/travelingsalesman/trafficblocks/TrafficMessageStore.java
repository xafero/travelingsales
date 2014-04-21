/**
 * TrafficMessageStore.java
 * created: 05.04.2009
 * (c) 2008 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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
package org.openstreetmap.travelingsalesman.trafficblocks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.h2.DatabaseContext;
import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.travelingsalesman.trafficblocks.tmc.TMCfromNMEAFilter;

/**
 * This is a database to store received messages about traffic
 * congestions.<br/>
 * Message-storage is independent of the source of the traffic-messages
 * (TMC, web-site, Car2Car, ...).<br/>
 * As they are often refered to in traffic related messages
 * this database also keeps an index of all map-entities that
 * are tagged with LocationCodes of the TMC-service.<br/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 * @see TMCLocationIndexer
 */
public final class TrafficMessageStore {
    /**
     * Interface for listeners about changed messages.
     */
    public interface TrafficMessagesListener {

        /**
         * @param aMessage the new message.
         */
        void trafficMessageRemoved(final TrafficMessage aMessage);

        /**
         * @param aMessage the new message.
         */
        void trafficMessageUpdated(final TrafficMessage aMessage);

        /**
         * @param aMessage the new message.
         */
        void trafficMessageAdded(final TrafficMessage aMessage);

        /**
         * @param aSource a new source of traffic messages. May support tuning or other manual controls.
         */
        void sourceAdded(final ITrafficMessageSource aSource);

        /**
         * @param aSource a no longer used source of traffic messages.
         */
        void sourceRemoved(final ITrafficMessageSource aSource);
    }

    /**
     * Our listeners.
     * @see TrafficMessagesListener
     */
    private Set<TrafficMessagesListener> myTrafficMessagesListeners = new HashSet<TrafficMessagesListener>();

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(TrafficMessageStore.class.getName());

    /**
     * Singleton-instance.
     */
    private static TrafficMessageStore myInstance = null;

    /**
     * @see #addTrafficMessageSource(ITrafficMessageSource).
     */
    private Set<ITrafficMessageSource> mySources = new HashSet<ITrafficMessageSource>();

    /**
     * Has the H2-driver been loaded already?
     */
    private static boolean driverLoaded = false;

    /**
     * Cache for {@link #getAllMessages(IDataSet)}.
     */
    private Collection<TrafficMessage> myCache = null;

//    public static void main(final String[] args) {
//        TrafficMessageStore.getInstance().indexTMCLocation(0, 0, 0, EntityType.Relation, 0);
//    }
    /**
     * private constructor as this is a singleton.
     */
    private TrafficMessageStore() {
        this.myDatabaseURL = Settings.getInstance().get("tmc.storepath", getDefaultURL());
        this.myConnection = getConnection();
    }

    /**
     * @param aTrafficMessagesListener a new listener
     */
    public void addTrafficMessagesListener(final TrafficMessagesListener aTrafficMessagesListener) {
        this.myTrafficMessagesListeners.add(aTrafficMessagesListener);
    }

    /**
     * Utility method for ensuring that the database driver is registered.
     */
    private static void loadDatabaseDriver() {
        if (!driverLoaded) {
            // Lock to ensure two threads don't try to load the driver at the same time.
            synchronized (DatabaseContext.class) {
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
     * Our database-connection.
     */
    private Connection myConnection;

    /**
     * If no database connection is open, a new connection is opened. The
     * database connection is then returned.
     *
     * @return The database connection.
     */
    private synchronized Connection getConnection() {
        if (myConnection == null) {

            loadDatabaseDriver();

            try {
                myConnection = DriverManager.getConnection(myDatabaseURL);

                myConnection.setAutoCommit(false);
                checkSchema(myConnection);

            } catch (SQLException e) {
                throw new OsmosisRuntimeException(
                        "Unable to establish a database connection.", e);
            }
        }

        return myConnection;
    }


    /**
     * Create our tables.
     * @param aConnection the connection to use
     */
    private void checkSchema(final Connection aConnection) {

        try {
            Statement stmt = aConnection.createStatement();
            try {
                stmt.executeUpdate(
                        "CREATE CACHED TABLE trafficmessages ("
                        + "         osm_primary_location_type int,\n"
                        + "         osm_primary_location_id   BIGINT,\n"
                        + "         message VARCHAR,\n"
                        + "         valid_until datetime,\n"
                        + "         type int,\n"
                        + "         length int,\n"
                        + "         provider int,\n"
                        + "         extinfo VARBINARY,\n"
                        + "  PRIMARY KEY  (osm_primary_location_type, osm_primary_location_id, valid_until, provider)"
                        + "     )");
            } catch (SQLException e) {
                if (e.getMessage().contains("already exists")) {
                    return;
                }
                //            if (e.getMessage().indexOf("object name already exists") != -1) {
                //                return;
                //            }
                LOG.log(Level.SEVERE, "Cannot create database of TMC-locations", e);
            }
            try {
                stmt.executeUpdate(
                        "CREATE CACHED TABLE tmc_locations ("
                        + "         countryid int,\n"  // the country
                        + "         tableid int,\n"    // each country may have multiple location-tables
                        + "         locationid int,\n" // the location itself
                        + "         type       varchar,\n" // "Point", "Line" or "Area"
                        + "         osm_entity_type int,\n"   // node, way or relation
                        + "         osm_entity_id BIGINT,\n"  // node-id, way-id or relation-id
                        + "         PRIMARY KEY  (countryid, tableid, locationid, osm_entity_type, osm_entity_id)"
                        + "     );"
                        + "CREATE INDEX tmc_locations_byid ON tmc_locations (countryid, tableid, locationid);\n");

            } catch (SQLException e) {
                if (e.getMessage().indexOf("Table already exists") != -1) {
                    return;
                }
                //            if (e.getMessage().indexOf("object name already exists") != -1) {
                //                return;
                //            }
                LOG.log(Level.SEVERE, "Cannot create database of TMC-locations", e);
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot create database of TMC-locations", e);
        }


    }


    /**
     * the JDBC-URL of the  database.
     */
    private String myDatabaseURL;

    /**
     * @return the JDBC-URL of the default database.
     */
    public static String getDefaultURL() {
        return "jdbc:h2:file:"
        + getDBDirName()
        + "trafficmessages";
    }
    /**
     * @return the directory where we store all tiles.
     */
    private static String getDBDirName() {
        // we use the directory of the tiles from the FileTileDataSet.
        String dir = Settings.getInstance().get("map.dir",
                           Settings.getInstance().get("tiledMapCache.dir",
                           System.getProperty("user.home") + File.separator + ".openstreetmap" + File.separator + "map")
                           );
        if (!dir.endsWith(File.separator)) {
            dir += File.separator;
        }
        return dir;
    }


    /**
     * @return the singleton-instance
     */
    public static synchronized TrafficMessageStore getInstance() {
        if (myInstance == null) {
            myInstance = new TrafficMessageStore();
        }
        return myInstance;
    }
    /**
     * @param aMessage the message
     */
    public void addMessage(final TrafficMessage aMessage) {
        try {
            if (myCache == null) {
                myCache = Collections.synchronizedSet(new HashSet<TrafficMessage>());
            }
            Connection connection = getConnection();
            removeMessage(aMessage);
            myCache.add(aMessage);
            PreparedStatement insert = connection.prepareStatement(
                    "MERGE INTO trafficmessages "
                    + "(osm_primary_location_type, osm_primary_location_id, "
                    + " message, valid_until, provider, type, length, extinfo) "
                    + "VALUES (?, ?, ?, ?, 0, ?, ?, ?)");
            int i = 1;
            insert.setInt(i++, aMessage.getEntity().getType().ordinal());
            insert.setLong(i++, aMessage.getEntity().getId());
            insert.setString(i++, aMessage.getMessage());
            insert.setTimestamp(i++, new java.sql.Timestamp(aMessage.getValidUntil().getTime()));
            insert.setInt(i++, aMessage.getType().ordinal());
            insert.setInt(i++, aMessage.getLengthInMeters());

            Map<String, Serializable> ext = aMessage.getExtendedProperties();
            ByteArrayOutputStream blobData = new ByteArrayOutputStream();
            ObjectOutputStream blobOut = new ObjectOutputStream(blobData);
            blobOut.writeObject(ext);
            blobOut.close();
            byte[] blobBytes = blobData.toByteArray();
            //Blob blob = connection.createBlob();
            //blob.setBytes(0, blobBytes);
            //insert.setBlob(i++, blob);
            insert.setBytes(i++, blobBytes);
            insert.execute();
            connection.createStatement().execute("CHECKPOINT");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot add traffic-message", e);
        }
        for (TrafficMessagesListener listener : this.myTrafficMessagesListeners) {
            try {
                listener.trafficMessageAdded(aMessage);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Internal error while informing listener about new traffic-message", e);
            }
        }
    }

    /**
     * We use this for messages at unknown locations.
     */
    public static final Entity UNKNOWNLOCATION = new Node(0, 0, new Date(), OsmUser.NONE, 0, 0, 0);
    static {
        UNKNOWNLOCATION.getTags().add(new Tag("name", "unknown"));
    }

    /**
     * Get all currently valid traffic-messages.
     * @param aMap the map we operate on
     * @return all messages
     */
    @SuppressWarnings("unchecked")
    public Collection<TrafficMessage> getAllMessages(final IDataSet aMap) {
        if (myCache != null) {
            Iterator<TrafficMessage> iterator = myCache.iterator();
            boolean removed = true;
            while (removed) {
                removed = false;
                while (iterator.hasNext()) {
                    TrafficMessage next = iterator.next();
                    if (next.getValidUntil().before(new Date())) {
                        iterator.remove();
                        removed = true;
                        removeMessage(next);
                        break;
                    }
                }
            }
            return myCache;
        }

        try {
            myCache = new HashSet<TrafficMessage>();
            Connection connection = getConnection();
            PreparedStatement select = connection.prepareStatement(
                    "SELECT * FROM trafficmessages ");
            ResultSet rs = select.executeQuery();
            try {
                Date now = new Date();
                  while (rs.next()) {
                    try {
                        String msg = rs.getString("message");
                        int    osmType = rs.getInt("osm_primary_location_type");
                        long   osmID   = rs.getLong("osm_primary_location_id");
                        java.sql.Timestamp until = rs.getTimestamp("valid_until");
                        Entity osmEntity = null;
                        if (osmType == EntityType.Node.ordinal()) {
                            osmEntity = aMap.getNodeByID(osmID);
                        } else if (osmType == EntityType.Way.ordinal()) {
                            osmEntity = aMap.getWaysByID(osmID);
                        } else if (osmType == EntityType.Relation.ordinal()) {
                            osmEntity = aMap.getRelationByID(osmID);
                        }
                        if (osmEntity == null) {
                            LOG.info("map-entity missing");
                            osmEntity = UNKNOWNLOCATION;
                        }
                        int typei = rs.getInt("type");
                        TrafficMessage.TYPES tmType = TrafficMessage.TYPES.values()[typei];
                        int length = rs.getInt("length");
                        TrafficMessage ret = new TrafficMessage(osmEntity, msg, until, tmType, length);
                        if (ret.getValidUntil().before(now)) {
                            removeMessage(ret);
                        } else {
                            myCache.add(ret);
                        }
                        Blob blob = rs.getBlob("extinfo");
                        if (blob != null) {
                            ObjectInputStream in = new ObjectInputStream(blob.getBinaryStream());
                            Map<String, Serializable> extInfo = (Map<String, Serializable>) in.readObject();
                            ret.getExtendedProperties().putAll(extInfo);
                        }
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Cannot load single TMC-location to the database", e);
                    }
                }
            } finally {
                rs.close();
            }
            return myCache;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot load TMC-location to the database", e);
        }
        return null;
    }
    /**
     * Remove a traffic-message.
     * @param aMessage the message
     */
    public void removeMessage(final TrafficMessage aMessage) {
        try {
            if (myCache != null) {
                myCache.remove(aMessage);
            }
            //TODO: allow identifying messages to overwrite as described in ISO14819-1 section 6.4 
            Connection connection = getConnection();
            PreparedStatement remove = connection.prepareStatement(
                    "DELETE FROM trafficmessages "
                    + "WHERE osm_primary_location_type = ? AND "
                    + "osm_primary_location_id = ?");
            int i = 1;
            if (aMessage.getEntity() == UNKNOWNLOCATION) {
                // allow multiple messages at the unknown location
                remove = connection.prepareStatement(
                        "DELETE FROM trafficmessages "
                        + "WHERE message = ? AND osm_primary_location_type = ? AND "
                        + "osm_primary_location_id = ?");
                remove.setString(i++, aMessage.getMessage());
            }
            remove.setInt(i++, aMessage.getEntity().getType().ordinal());
            remove.setLong(i++, aMessage.getEntity().getId());
            remove.execute();
            connection.createStatement().execute("CHECKPOINT");
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot add traffic-message", e);
        }

        for (TrafficMessagesListener listener : this.myTrafficMessagesListeners) {
            try {
                listener.trafficMessageRemoved(aMessage);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Internal error while informing listener about removed traffic-message", e);
            }
        }
    }
    /**
     * Add a map-entity that is refered by using a TMC location-code
     * to our database.
     * @param aCountryID    the country the code is from
     * @param aTableID      the country-specific table the code is from
     * @param aLocationCode the location-code
     * @param aTmcElementType "Point", "Line" or "Area"
     * @param aType the type of map-entity (Node, Way, Relation)
     * @param aId the ID of the map-entity
     * @see #getLocation(int, int, int, IDataSet)
     */
    public void indexTMCLocation(final int aCountryID,
                                 final int aTableID,
                                 final int aLocationCode,
                                 final String aTmcElementType,
                                 final EntityType aType,
                                 final long aId) {
        try {
            Connection connection = getConnection();
            PreparedStatement insert = connection.prepareStatement(
                    "MERGE INTO tmc_locations "
                    + "(countryid, tableid, locationid, type, osm_entity_type, osm_entity_id) "
                    + "VALUES (?, ?, ?, ?, ?, ?)");
            int i = 1;
            insert.setInt(i++, aCountryID);
            insert.setInt(i++, aTableID);
            insert.setInt(i++, aLocationCode);
            insert.setString(i++, aTmcElementType);
            insert.setInt(i++, aType.ordinal());
            insert.setLong(i++, aId);
            insert.execute();
            connection.createStatement().execute("CHECKPOINT");
        } catch (SQLException e) {
            if (e.getMessage().indexOf("duplicate value") != -1) {
                LOG.log(Level.FINER, "Cannot add TMC-location to the database - it is already in there", e);
            } else {
                LOG.log(Level.SEVERE, "Cannot add TMC-location to the database", e);
            }
        }

    }
    /**
     * Get all map-entities in the map with the given TMC location-code.
     * @param aCountryID    the country the code is from
     * @param aTableID      the country-specific table the code is from
     * @param aLocationCode the location-code
     * @param aMap          the map to get entities from by their ID.
     * @param aBothDirections true if both directions are affected
     * @param aSteps        how many steps (next/prev-LocationCode) to go in a direction
     * @param aDirectionIsReverse what direction to go in
     * @return the entities
     */
    public Collection<TMCLocation> getLocation(final int aCountryID,
            final int aTableID,
            final int aLocationCode,
            final boolean aDirectionIsReverse,
            final int aSteps,
            final IDataSet aMap,
            final boolean aBothDirections) {
        int directionAttrValue = 2;
        if (!aBothDirections) {
            if (aDirectionIsReverse) {
                directionAttrValue = 1;
            } else {
                directionAttrValue = 0;
            }
        }
        Collection<TMCLocation> location = getLocation(aCountryID, aTableID, aLocationCode, aMap, directionAttrValue);
        if (location == null || location.size() == 0) {
            LOG.warning("Location in map missing. Returning aproximate location");
            return new LinkedList<TMCLocation>();
        }
        for (int step = 0; step < aSteps; step++) {
            int nextLocation = -1;
            for (TMCLocation location2 : location) {
                int next = -1;
                if (aDirectionIsReverse) {
                    next = location2.getPrevLocationID();
                } else {
                    next = location2.getNextLocationID();
                }
                if (next != -1) {
                    nextLocation = next;
                    break;
                }
            }
            if (nextLocation == -1) {
                LOG.warning("Next location in TMC missing. Returning aproximate location");
                return location;
            }
            Collection<TMCLocation> temp = location;
            location = getLocation(aCountryID, aTableID, nextLocation, aMap, directionAttrValue);
            if (location == null) {
                LOG.warning("Next location in map missing. Returning aproximate location");
                return temp;
            }
        }
        return location;
    }
    /**
     * Get all map-entities in the map with the given TMC location-code.
     * @param aCountryID    the country the code is from
     * @param aTableID      the country-specific table the code is from
     * @param aLocationCode the location-code
     * @param aMap          the map to get entities from by their ID.
     * @param aDirectionAttrValue  (optional) value that must be present in the TMC:direction -attribute of the entity (if applicable)
     * @return the entities
     */
    public Collection<TMCLocation> getLocation(final int aCountryID,
            final int aTableID,
            final int aLocationCode,
            final IDataSet aMap,
            final int aDirectionAttrValue) {
        try {
            Connection connection = getConnection();
            PreparedStatement select = connection.prepareStatement(
                    "SELECT * FROM tmc_locations WHERE "
                    + "countryid = ? AND  tableid = ? AND locationid = ?");
            int i = 1;
            select.setInt(i++, aCountryID);
            select.setInt(i++, aTableID);
            select.setInt(i++, aLocationCode);
            ResultSet rs = select.executeQuery();
            Collection<TMCLocation> retval = new LinkedList<TMCLocation>();
            try {
                while (rs.next()) {
                    try {
                        String tmcType = rs.getString("type");
                        int    osmType = rs.getInt("osm_entity_type");
                        long   osmID   = rs.getLong("osm_entity_id");
                        Entity osmEntity = null;
                        if (osmType == EntityType.Node.ordinal()) {
                            osmEntity = aMap.getNodeByID(osmID);
                        } else if (osmType == EntityType.Way.ordinal()) {
                            osmEntity = aMap.getWaysByID(osmID);
                        } else if (osmType == EntityType.Relation.ordinal()) {
                            osmEntity = aMap.getRelationByID(osmID);
                        }
                        if (osmEntity == null) {
                            LOG.info("map-entity missing");
                            continue;
                        }
                        TMCLocation location = new TMCLocation(osmEntity, aCountryID, aTableID, tmcType);
                        if (aDirectionAttrValue != 2) {
                            if (location.getDirction() != 2
                                    && location.getDirction() != aDirectionAttrValue) {
                                continue;
                            }
                        }
                        retval.add(location);
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Cannot load single TMC-location to the database", e);
                    }
                }
            } finally {
                rs.close();
            }
            return retval;
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot load TMC-location to the database", e);
        }
        return null;
    }

    /**
     * Helper-class describing one map-entity that has a
     * TMC location-code.
     */
    public static class TMCLocation {
        /**
         * @param aEntity The tagged entity.
         * @param aCountryCode The country-Id of the locationCode.
         * @param aTableCode The country-specific locatonTableID of the locationCode.
         * @param aEntityKind "Point", "Line" or "Area".
         */
        protected TMCLocation(final Entity aEntity,
                              final int aCountryCode,
                              final int aTableCode,
                              final String aEntityKind) {
            super();
            myEntity = aEntity;
            myTMCCountryCode = aCountryCode;
            myTMCTableCode = aTableCode;
            myTMCEntityKind = aEntityKind;
        }
        /**
         * The tagged entity.
         */
        private Entity     myEntity;
        /**
         * The country-Id of the locationCode.
         */
        private int myTMCCountryCode;
        /**
         * The country-specific locatonTableID of the locationCode.
         */
        private int myTMCTableCode;
        /**
         * "Point", "Line" or "Area".
         */
        private String myTMCEntityKind;
//        /**
//         * The location-code we have.
//         */
//        private int myTMCLocationCode;
        /**
         * Format of the tag for a next LocationCode.
         * {1} = country-ID
         * {2} = table-ID
         * {3} = Point, Segment, Road or Area
         */
        private static final String TMC_NEXTLOCID_TAG = "TMC:cid_{0}:tabcd_{1}:{2}:Next{3}LocationCode";
        /**
         * Format of the tag for a previous LocationCode.
         * {1} = country-ID
         * {2} = table-ID
         * {3} = Point, Segment, Road or Area
         */
        private static final String TMC_PREVCLOCID_TAG = "TMC:cid_{0}:tabcd_{1}:{2}:Prev{2}LocationCode";
        /**
         * Format of the tag for a direction a  LocationCode applies to (empty=both, 0=forward, 1=reverse).
         * {1} = country-ID
         * {2} = table-ID
         * {3} = Point, Segment, Road or Area
         */
        private static final String TMC_DIRECTION_TAG = "TMC:cid_{0}:tabcd_{1}:{2}:Direction";

        /**
         * Location are chained. Get the LocationID of the next location.
         * @return -1 if none
         */
        public int getNextLocationID() {
            try {
                String tag = WayHelper.getTag(myEntity.getTags(), MessageFormat.format(TMC_NEXTLOCID_TAG, new Object[]{myTMCCountryCode, myTMCTableCode, myTMCEntityKind}));
                if (tag == null) {
                    return -1;
                }
                return Integer.parseInt(tag);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Entity " + myEntity + " has illegal TMC:NextLocationCode -tag. Ignoring.", e);
                return -1;
            }
        }
        /**
         * Location are chained. Get the LocationID of the previous location.
         * @return -1 if none
         */
        public int getPrevLocationID() {
            try {
                String tagName = MessageFormat.format(TMC_PREVCLOCID_TAG, new Object[]{myTMCCountryCode, myTMCTableCode, myTMCEntityKind});
                String tag = WayHelper.getTag(myEntity.getTags(), tagName);
                if (tag == null) {
                    return -1;
                }
                return Integer.parseInt(tag);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Entity " + myEntity + " has illegal TMC:PrevLocationCode -tag. Ignoring.", e);
                return -1;
            }
        }
        /**
         * Location may be valid only for one direction.
         * Return 0 for "forward", 1 for "revese" and 2 for "both".
         * @return 2 if none
         */
        public int getDirction() {
            try {
                String tag = WayHelper.getTag(myEntity.getTags(), MessageFormat.format(TMC_DIRECTION_TAG, new Object[]{myTMCCountryCode, myTMCTableCode, myTMCEntityKind}));
                if (tag == null) {
                    return 2;
                }
                return Integer.parseInt(tag);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Entity " + myEntity + " has illegal TMC:PrevLocationCode -tag. Ignoring.", e);
                return 2;
            }
        }
        /**
         * @return {@link #myEntity}
         */
        public Entity getEntity() {
            return myEntity;
        }
    }

    /**
     * Register another source of Traffic Messages.
     * This may be used my a GUI and is not required
     * for the operation of this class.
     * @param aSource the source to register.
     */
    public void addTrafficMessageSource(final ITrafficMessageSource aSource) {
        this.mySources.add(aSource);
        for (TrafficMessagesListener listener : this.myTrafficMessagesListeners) {
            try {
                listener.sourceAdded(aSource);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Internal error while informing listener about "
                        + "an added source of TMC information", e);
            }
        }
    }

    /**
     * @see #addTrafficMessageSource(ITrafficMessageSource).
     * @return the sources
     */
    public Set<ITrafficMessageSource> getTrafficMessageSources() {
        return Collections.unmodifiableSet(mySources);
    }

    /**
     * @see #addTrafficMessageSource(ITrafficMessageSource).
     * @param aSource the source to remove from our list.
     */
    public void removeTrafficMessageSource(final TMCfromNMEAFilter aSource) {
        this.mySources.remove(aSource);
        for (TrafficMessagesListener listener : this.myTrafficMessagesListeners) {
            try {
                listener.sourceRemoved(aSource);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Internal error while informing listener about "
                        + "a removed source of TMC information", e);
            }
        }
    }
}
