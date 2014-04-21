/**
 * AddressDBPlaceFinder.java
 * created: 18.11.2007 19:15:08
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
package org.openstreetmap.osm.data.searching;


//automatically created logger for debug and error -output
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//automatically created propertyChangeListener-Support
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.ConfigurationSetting;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.ConfigurationSetting.TYPES;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.NodeHelper;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.h2.DatabaseContext;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.util.FixedPrecisionCoordinateConvertor;


/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * AddressDBPlaceFinder.java<br/>
 * created: 18.11.2007 19:15:08 <br/>
 *<br/><br/>
 * This {@link IPlaceFinder} uses a local H2-database that stores an index
 * of all cities but not zip-codes and streets. (House-numbers are looked up
 * in the map directly.)
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class AddressDBPlaceFinder implements IAddressDBPlaceFinder {

    /**
     * Milliseconds for the thread started by {@link #doCommitAsync()}
     * before actually commiting ({@value #PRECOMMITSLEEPMILLIS} ms).
     */
    private static final int PRECOMMITSLEEPMILLIS = 60 * 1000;

    /**
     * Do not return more thene this many results
     * to a query. (e.g. looking for a large city
     * without specifying the street.)
     */
    private static final int MAXRESULTS = 20;

    /**
     * The default size of a place=city that has no
     * defining polygon but only a center-point.
     * See <a href="http://wiki.openstreetmap.org/index.php/Map_Features#Places">
     * Map_Features#Places on the OSM-Wiki</a> for details.
     * @see #getPlaceBounds(Node)
     */
    private static final double DEFAULT_CITY_SIZE = 0.8d;

    /**
     * The default size of a place=town that has no
     * defining polygon but only a center-point.
     * See <a href="http://wiki.openstreetmap.org/index.php/Map_Features#Places">
     * Map_Features#Places on the OSM-Wiki</a> for details.
     * @see #getPlaceBounds(Node)
     */
    private static final double DEFAULT_TOWN_SIZE = 0.4d;

    /**
     * The default size of a place=village that has no
     * defining polygon but only a center-point.
     * See <a href="http://wiki.openstreetmap.org/index.php/Map_Features#Places">
     * Map_Features#Places on the OSM-Wiki</a> for details.
     * @see #getPlaceBounds(Node)
     */
    private static final double DEFAULT_VILLAGE_SIZE = 0.2d;

    /**
     * The default size of a place=suburb that has no
     * defining polygon but only a center-point.
     * See <a href="http://wiki.openstreetmap.org/index.php/Map_Features#Places">
     * Map_Features#Places on the OSM-Wiki</a> for details.
     * @see #getPlaceBounds(Node)
     */
    private static final double DEFAULT_SUBURB_SIZE = 0.17d;

    /**
     * The default size of a place=hamlet that has no
     * defining polygon but only a center-point.
     * See <a href="http://wiki.openstreetmap.org/index.php/Map_Features#Places">
     * Map_Features#Places on the OSM-Wiki</a> for details.
     * @see #getPlaceBounds(Node)
     */
    private static final double DEFAULT_HAMLET_SIZE = 0.7d;

    /**
     * The default size of a place=* that has no
     * defining polygon but only a center-point and
     * is not defined by one of the other constants
     * See <a href="http://wiki.openstreetmap.org/index.php/Map_Features#Places">
     * Map_Features#Places on the OSM-Wiki</a> for details.
     * @see #getPlaceBounds(Node)
     */
    private static final double DEFAULT_PLACE_SIZE = 0.4d;

    /**
     * The map we search on.
     */
    private IDataSet myMap;

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger
            .getLogger(AddressDBPlaceFinder.class.getName());

    /**
     * The database we store cities and
     * roads in.
     */
    private static DatabaseContext myDatabase = null;


    /**
     * Construct this class and check the database.
     */
    public AddressDBPlaceFinder() {
    }

    /**
     * {@inheritDoc}
     */
    public ConfigurationSection getSettings() {
        ConfigurationSection retval = new ConfigurationSection("GpsDProvider");
        retval.addSetting(new ConfigurationSetting("libosm.places.default_size",
                Settings.RESOURCE.getString("libosm.places.default_size.title"), TYPES.STRING,
                Settings.RESOURCE.getString("libosm.places.default_size.category"),
                Settings.RESOURCE.getString("libosm.places.default_size.desc")));
        retval.addSetting(new ConfigurationSetting("libosm.places.hamlet.default_size",
                Settings.RESOURCE.getString("libosm.places.hamlet.default_size.title"), TYPES.STRING,
                Settings.RESOURCE.getString("libosm.places.hamlet.default_size.category"),
                Settings.RESOURCE.getString("libosm.places.hamlet.default_size.desc")));
        retval.addSetting(new ConfigurationSetting("libosm.places.suburb.default_size",
                Settings.RESOURCE.getString("libosm.places.suburb.default_size.title"), TYPES.STRING,
                Settings.RESOURCE.getString("libosm.places.suburb.default_size.category"),
                Settings.RESOURCE.getString("libosm.places.suburb.default_size.desc")));
        retval.addSetting(new ConfigurationSetting("libosm.places.village.default_size",
                Settings.RESOURCE.getString("libosm.places.village.default_size.title"), TYPES.STRING,
                Settings.RESOURCE.getString("libosm.places.village.default_size.category"),
                Settings.RESOURCE.getString("libosm.places.village.default_size.desc")));
        retval.addSetting(new ConfigurationSetting("libosm.places.town.default_size",
                Settings.RESOURCE.getString("libosm.places.town.default_size.title"), TYPES.STRING,
                Settings.RESOURCE.getString("libosm.places.town.default_size.category"),
                Settings.RESOURCE.getString("libosm.places.town.default_size.desc")));
        retval.addSetting(new ConfigurationSetting("libosm.places.city.default_size",
                Settings.RESOURCE.getString("libosm.places.city.default_size.title"), TYPES.STRING,
                Settings.RESOURCE.getString("libosm.places.city.default_size.category"),
                Settings.RESOURCE.getString("libosm.places.city.default_size.desc")));
        return retval;

    }

    /**
     * @return the directory where we store all tiles.
     */
    private String getDBDirName() {
        // we use the directory of the tiles from the FileTileDataSet.
        return Settings.getInstance().get("map.dir",
                Settings.getInstance().get("tiledMapCache.dir", System.getProperty("user.home")
                + File.separator + ".openstreetmap" + File.separator + "map" + File.separator));
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
                        LOG.log(Level.FINEST, "Commit-thread has been interrupted.");
                    }
                    try {
                        getDatabase().commit();
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
     * @return This is the database where we store index.
     */
    protected DatabaseContext getDatabase() {
        if (myDatabase == null) {
            myDatabase =     new DatabaseContext("jdbc:h2:file:" + getDBDirName() + "streets") {

                /**
                 * the schema-version required.
                 */
                private static final int ADDRESSDBSCHEMAVERSION = 3;

                /**
                 * @return the schema-version required.
                 */
                @Override
                public int getSchemaVersion() {
                    LOG.log(Level.FINE, "returning required schema-version of " + ADDRESSDBSCHEMAVERSION);
                    return ADDRESSDBSCHEMAVERSION;
                }

                /**
                 * Create or update the table-schema of
                 * the database.
                 * @throws SQLException in case something happens
                 * @param con the database-connection opened
                 */
                @Override
                protected void createSchema(final Connection con) throws SQLException {

                        Statement stmt = con.createStatement();

                        LOG.log(Level.INFO, "Creating new address-database. in " + getDBDirName());

                        stmt.executeUpdate(
                                "DROP TABLE schema_info IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE schema_info ("
                              + "         version int default NULL"
                              + "     )");

                        stmt.execute(
                                "INSERT INTO schema_info VALUES (" + ADDRESSDBSCHEMAVERSION + ")");

                        stmt.execute(
                                "DROP TABLE city IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE city (\n"
                              + "  id        BIGINT    NOT NULL,\n"
                              + "  idType      INT     NOT NULL,\n" //0=way 1=node, 2=relation
                              + "  defaultName varchar NOT NULL,\n"
                              + "  centerLat BIGINT    NOT NULL,\n" // center of the city
                              + "  centerLon BIGINT    NOT NULL,\n"
                              + "  radius    BIGINT    NOT NULL,\n" // size of bounding-box
                              + "  PRIMARY KEY  (id, idType));\n"
                              + "CREATE INDEX city_name_idx ON city (defaultName);\n");

                        stmt.execute(
                                "DROP TABLE suburb IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE suburb (\n"
                              + "  id        BIGINT    NOT NULL,\n"
                              + "  idType      INT     NOT NULL,\n" //0=way 1=node, 2=relation
                              + "  defaultName varchar NOT NULL,\n"
                              + "  centerLat BIGINT    NOT NULL,\n" // center of the city
                              + "  centerLon BIGINT    NOT NULL,\n"
                              + "  radius    BIGINT    NOT NULL,\n" // size of bounding-box
                              + "  PRIMARY KEY  (id, idType));\n"
                              + "CREATE INDEX suburb_name_idx ON suburb (defaultName);\n");
                }

            };
        }

        return myDatabase;
    }

    /**
     * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
     * Project: libosm<br/>
     * AddressDBPlaceFinder.java<br/>
     * created: 24.11.2007 21:11:27 <br/>
     *<br/><br/>
     * We know about these 2 types of places.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private enum PLACETYPES {
        /**
         * Polygon-type places have a bounding-polygon
         * that describes their shape and size..
         */
        POLYGON {
            /**
             * @return 0
             */
            public int getCode() {
                return 0;
            }
        },
        /**
         * Centernode-type places have a node
         * and we are guessing a bounding-box they
         * may contain.
         */
        CENTERNODE {
            /**
             * @return 1
             */
            public int getCode() {
                return 1;
            }
        };

        /**
         * @return the code we represent this placetype with in the database.
         */
        public abstract int getCode();
        };

    /**
     * Cache for {@link #indexDatabase(IDataSet)}.
     */
    private PreparedStatement insetCityPolygon = null;

    /**
     * Cache for {@link #indexDatabase(IDataSet)}.
     */
    private PreparedStatement insetSuburbPolygon = null;

    /**
     * Cache for {@link #indexDatabase(IDataSet)}.
     */
    private PreparedStatement insetCityNode = null;

    /**
     * Cache for {@link #indexDatabase(IDataSet)}.
     */
    private PreparedStatement insetSuburbNode = null;

    /**
     * Index all cities in the given dataset.
     * @param aMap the dataset to index.
     */
    public void indexDatabase(final IDataSet aMap) {
        if (aMap == null)
            throw new IllegalArgumentException("null map given!");
        if (myMap == null)
            myMap = aMap;


        // iterate over all cities
        LOG.log(Level.FINE, "indexing cities...");
        Iterator<Way> waysByTag = aMap.getWaysByTag("place", null);
        // cities with a bounding polygon
        if (waysByTag == null) {
            LOG.log(Level.SEVERE, "getWaysByTag(tag='place') returned null!");
        } else
        for (Iterator<Way> cities = waysByTag; cities.hasNext();) {
            Way city = cities.next();
            indexWay(city);
        }

     // cities with a central node
        for (Iterator<Node> cities = aMap.getNodesByTag("place", null); cities.hasNext();) {
            Node city = cities.next();
            indexNode(city);

        }
        getDatabase().commit();
        LOG.log(Level.FINE, "indexing cities...DONE");

        //aMap.getRelationByTag(???, null); -> cities defined as a relation


    }

    /**
     * Index the given node if it is a city.
     * else ignore it.
     * @param city any way. Not null
     */
    public void indexNode(final Node city) {
        DatabaseContext db = getDatabase();
        if (insetCityNode == null)
            insetCityNode = db.prepareStatement("MERGE INTO city (id, idType, defaultName, centerLat, centerLon, radius)"
                + " VALUES (?, " + PLACETYPES.CENTERNODE.getCode() + ", ?, ?, ?, ?)");
        if (insetSuburbNode == null)
            insetSuburbNode = db.prepareStatement("MERGE INTO Suburb (id, idType, defaultName, centerLat, centerLon, radius)"
                + " VALUES (?, " + PLACETYPES.CENTERNODE.getCode() + ", ?, ?, ?, ?)");
        try {
            // add entry in the database
            int i = 1;
            String name = WayHelper.getTag(city, Tags.TAG_NAME);
            String placeType = WayHelper.getTag(city, Tags.TAG_PLACE);

            if (placeType == null) {
                return;
            }

            if (name == null) {
                LOG.log(Level.WARNING, "We found a city with no name. Refusing to index it. nodeID=" + city.getId());
                return;
            }

//          find out what type of place we have here
            PreparedStatement statement = insetCityNode;
            if (placeType != null) {
                placeType = placeType.toLowerCase();
                if (placeType.equals("suburb")) {
                    LOG.log(Level.FINE, "indexing node-suburb'" + name + "' nodeid=" + city.getId());
                    statement = insetSuburbNode;
                } else if (placeType.equals("island")) {
                    LOG.log(Level.FINE, "not indexing node-island '" + name + "' nodeid=" + city.getId());
                    return;
                } else {
                    LOG.log(Level.FINE, "indexing node-city '" + name + "' for nodeID=" + city.getId());
                }
            } else {
                LOG.log(Level.FINE, "indexing typeless node-city '" + name + "' for nodeID=" + city.getId());
            }

            statement.setLong(i++, city.getId());
            statement.setString(i++, NameHelper.normalizeName(name));
            CityBounds bounds = getPlaceBounds(city);

            LatLon center = new LatLon(city.getLatitude(), city.getLongitude()); // bounds.getCenter();
            statement.setLong(i++, FixedPrecisionCoordinateConvertor.convertToFixed(center.lat()));
            statement.setLong(i++, FixedPrecisionCoordinateConvertor.convertToFixed(center.lon()));
            statement.setLong(i++, FixedPrecisionCoordinateConvertor.convertToFixed(bounds.getSize()));

            statement.clearWarnings();
            if (statement.executeUpdate() != 1)
                LOG.log(Level.SEVERE, "Error while inserting city into the address-database nodeID=" + city.getId()
                        + "\nwarnings=" + insetCityNode.getWarnings());
            doCommitAsync();
        } catch (SQLException e) {
            // we ignore already indexed values
            // it would cost more performance to check for existence first
            if (!e.getMessage().contains("duplicate value(s) for column(s)"))
                LOG.log(Level.SEVERE, "Error while inserting city into the address-database nodeID=" + city.getId(), e);
        }

        LOG.log(Level.FINE, "indexing node- suburb/city/way done");
    }

    /**
     * Index the given closed way if it is a city.
     * else ignore it.
     * @param city any way. Not null
     */
    public void indexWay(final Way city) {

        DatabaseContext db = getDatabase();
        try {
            // add entry in the database
            int i = 1;
            String name = WayHelper.getTag(city, Tags.TAG_NAME);
            String placeType = WayHelper.getTag(city, Tags.TAG_PLACE);

            if (placeType == null) {
                return;
            }

            if (name == null) {
                LOG.log(Level.WARNING, "We found a city with no name. Refusing to index it. wayID=" + city.getId());
                return;
            }

            // find out what type of place we have here
            if (insetCityPolygon == null)
                insetCityPolygon = db.prepareStatement("MERGE INTO city (id, idType, defaultName, centerLat, centerLon, radius) "
                    + "VALUES (?, " + PLACETYPES.POLYGON.getCode() + ", ?, ?, ?, ?)");
            PreparedStatement statement = insetCityPolygon;
            if (placeType != null) {
                placeType = placeType.toLowerCase();
                if (placeType.equals("suburb")) {
                    LOG.log(Level.FINE, "indexing suburb'" + name + "' wayId=" + city.getId());
                    if (insetSuburbPolygon == null)
                        insetSuburbPolygon = db.prepareStatement("MERGE INTO Suburb (id, idType, defaultName, centerLat, centerLon, radius)"
                            + " VALUES (?, " + PLACETYPES.POLYGON.getCode() + ", ?, ?, ?, ?)");
                    statement = insetSuburbPolygon;
                } else if (placeType.equals("island")) {
                    LOG.log(Level.FINE, "not indexing island '" + name + "' wayId=" + city.getId());
                    return;
                } else {
                    LOG.log(Level.FINE, "indexing way-city '" + name + "' for wayID=" + city.getId());
                }
            } else {
                LOG.log(Level.FINE, "indexing typeless way-city '" + name + "' for wayID=" + city.getId());
            }

            statement.setLong(i++, city.getId());
            statement.setString(i++, NameHelper.normalizeName(name));

            Bounds bounds = getPlaceBounds(city);
            if (bounds == null) {
                LOG.log(Level.WARNING, "City described by bounding-polygon has no bounds");
                return;
            }

            LatLon center = bounds.getCenter();
            double radius = bounds.getSize();
            statement.setLong(i++, FixedPrecisionCoordinateConvertor.convertToFixed(center.lat()));
            statement.setLong(i++, FixedPrecisionCoordinateConvertor.convertToFixed(center.lon()));
            statement.setLong(i++, FixedPrecisionCoordinateConvertor.convertToFixed(radius));

            statement.clearWarnings();
            if (statement.executeUpdate() != 1)
                LOG.log(Level.SEVERE, "Error while inserting city into the address-database wayID=" + city.getId()
                        + "\nwarnings=" + insetCityPolygon.getWarnings());
            doCommitAsync();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error while inserting city into the address-database wayID=" + city.getId(), e);
        }

        LOG.log(Level.FINE, "indexing way- suburb/city/way done");
    }

    /**
     * @param idType the type of the city
     * @param id the id of the map-element denoting the city
     * @return the bounds of the city-polygon
     */
    private CityBounds getPlaceBounds(final int idType, final long id) {
        if (idType == PLACETYPES.POLYGON.getCode()) {
            Way cityPolygon = myMap.getWaysByID(id);
            if (cityPolygon == null) {
                LOG.log(Level.SEVERE, "Cannot load bounds of city id=" + id + " type=" + idType + " because that way is not in the map.");
                return null;
            }
            return getPlaceBounds(cityPolygon);
        }

        if (idType == PLACETYPES.CENTERNODE.getCode()) {
            Node centerNode = myMap.getNodeByID(id);
            if (centerNode == null) {
                LOG.log(Level.SEVERE, "Cannot load bounds of city id=" + id + " type=" + idType + " because that node is not in the map.");
                return null;
            }
            return getPlaceBounds(centerNode);
        }

        LOG.log(Level.SEVERE, "Cannot load bounds of city id=" + id + " type=" + idType + " because we do not recognite the type-constant.");
        return null;
    }

    /**
     * Get a named, estimated Bounds-object for the given place represented
     * by a center-point instead of an exact polygon.<br/>
     * See <a href="http://wiki.openstreetmap.org/index.php/Map_Features#Places">
     * Map_Features#Places on the OSM-Wiki</a> for details on the tags evaluated.
     * @param cityNode a node denoting the center of the city
     * @return the bounds of the estimated polygon
     */
    private CityBounds getPlaceBounds(final Node cityNode) {
        CityBounds retval = new CityBounds(NodeHelper.getTag(cityNode, Tags.TAG_NAME));
        double size = Settings.getInstance().getDouble("libosm.places.default_size", DEFAULT_PLACE_SIZE) / 2;
        String placeType = NodeHelper.getTag(cityNode, Tags.TAG_PLACE);
        if (placeType != null) {

            if (placeType.toLowerCase().equals("hamlet")) {
                size = Settings.getInstance().getDouble("libosm.places.hamlet.default_size", DEFAULT_HAMLET_SIZE) / 2;
            } else
            if (placeType.toLowerCase().equals("suburb")) {
                size = Settings.getInstance().getDouble("libosm.places.suburb.default_size", DEFAULT_SUBURB_SIZE) / 2;
            } else
            if (placeType.toLowerCase().equals("village")) {
                size = Settings.getInstance().getDouble("libosm.places.village.default_size", DEFAULT_VILLAGE_SIZE) / 2;
            } else
            if (placeType.toLowerCase().equals("town")) {
                    size = Settings.getInstance().getDouble("libosm.places.town.default_size", DEFAULT_TOWN_SIZE) / 2;
            } else
            if (placeType.toLowerCase().equals("city")) {
                    size = Settings.getInstance().getDouble("libosm.places.city.default_size", DEFAULT_CITY_SIZE) / 2;
            }
        }

        retval.addPoint(cityNode.getLatitude() - size, cityNode.getLongitude() - size);
        retval.addPoint(cityNode.getLatitude() - size, cityNode.getLongitude() + size);
        retval.addPoint(cityNode.getLatitude() + size, cityNode.getLongitude() + size);
        retval.addPoint(cityNode.getLatitude() + size, cityNode.getLongitude() - size);
        return retval;
    }

    /**
     * @param city the city-polygon
     * @return the bounds of the city-polygon
     */
    private CityBounds getPlaceBounds(final Way city) {
     // find the city-bounds
        CityBounds polygon = new CityBounds(WayHelper.getTag(city, Tags.TAG_NAME));
        List<WayNode> nodeRefs = city.getWayNodes();
        int count = 0;
        for (WayNode wayNode : nodeRefs) {
            if (wayNode == null) {
                LOG.log(Level.SEVERE, "Way " + city.getId() + " has a null wayNode in it!");
                continue;
            }
            if (myMap == null) {
                LOG.log(Level.SEVERE, "WE HAVE NO MAP in AddressDBPlaceFinder!");
            }
            Node node = myMap.getNodeByID(wayNode.getNodeId());
            if (node != null) {
                polygon.addPoint(node.getLatitude(), node.getLongitude());
            }
            count++;
        }
        final int minPolygonNodes = 3;
        if (count < minPolygonNodes) {
            return null;
        }
        return polygon;
    }

    /**
     * support for firing PropertyChangeEvents.
     * (gets initialized only if we really have listeners)
     */
    private volatile PropertyChangeSupport myPropertyChange = null;

    /**
     * Returned value may be null if we never had listeners.
     * @return Our support for firing PropertyChangeEvents
     */
    protected PropertyChangeSupport getPropertyChangeSupport() {
        return myPropertyChange;
    }

    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     *
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property.  The listener
     * will be invoked only when a call on firePropertyChange names that
     * specific property.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(
                                                final String propertyName,
                                                final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The PropertyChangeListener to be removed
     */
    public final void removePropertyChangeListener(
                                                   final String propertyName,
                                                   final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(propertyName,
                    listener);
        }
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     *
     * @param listener  The PropertyChangeListener to be removed
     */
    public synchronized void removePropertyChangeListener(
                                                          final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(listener);
        }
    }

    //-------------------------------------------------------

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "AddressDBPlaceFinder@" + hashCode();
    }

    /**
     * ${@inheritDoc}.
     */
    public Collection<Place> findAddress(final String pHouseNr, final String pStreet,
            final String pCity, final String pZipCode,
            final String pCountry) {
        LOG.log(Level.FINEST, "call findAddress"
        		+ "  ( HouseNr = " + (pHouseNr != null ? pHouseNr : "null")
                + ", Street = " + (pStreet != null ? pStreet : "null")
                + ", City = " + (pCity != null ? pCity : "null")
                + ", ZipCode = " + (pZipCode != null ? pZipCode : "null")
                + ", Country = " + (pCountry != null ? pCountry : "null") + ")");
        Collection<Place> retval = new LinkedList<Place>();

        if (pCity != null && pCity.trim().length() > 0) {
            // search the DB for cities with this name and return their ways
            ResultSet cities = null;
            try {
                cities = getDatabase().executeStreamingQuery(
                        "SELECT * FROM city WHERE defaultName like '"
                        + NameHelper.buildNameSearchSQLMatch(pCity) + "'");

                while (cities.next()) {
                    long id = cities.getLong("id");
                    int idType = cities.getInt("idType");
                    try {
                        LOG.log(Level.FINEST, "found a matching city id=" + id + " type=" + idType + ". Looking for streets...");
                        CityBounds cityBounds = getPlaceBounds(idType, id);

                        if (cityBounds == null) {
                            LOG.log(Level.SEVERE, "Cannot load bounds of city id=" + id + " type=" + idType + ". ignoring it.");
                            continue;
                        }

                        //TODO: does this city contain the given zip-code?

                        // get Suburbs for city
                        Collection<CityBounds> suburbs = getSuburbs(cityBounds);
                        LOG.log(Level.FINEST, "found a matching city id=" + id + " type=" + idType + " with " + suburbs.size() + " suburbs. cityBounds=" + cityBounds);

                        // get all ways within the city that match the pStreet
                        Iterator<Way> streets = null;
                        if (pStreet != null && pStreet.length() > 0) {

                            // fill in suburb and zip-code, so the user
                            // can identify the street (s)he is looking
                            // for in the search-result.

                            streets = myMap.getWaysByName(pStreet, cityBounds);

                        } else {

                            //if the user is only looking for a city-name,
                            // do not return all streets of it.

                            if (suburbs != null && suburbs.size() > 0) {
                                for (CityBounds bounds : suburbs) {
                                    retval.add(new CityPlace(this, cityBounds, bounds));
                                }
                            } else {
                                retval.add(new CityPlace(this, cityBounds));

                            }
                            streets = null;
                            //streets = myMap.getWays(cityBounds);
                        }

                        while (streets != null && streets.hasNext()) {
                            Way street = streets.next();
                            try {
                                LOG.log(Level.FINEST, "found a matching city id=" + id + " type=" + idType + " with a matching street id=" + street.getId());
                                //TODO: is this street in the given zip-code?

                                String subUrbName = null;
                                for (CityBounds suburb : suburbs) {
                                    for (WayNode wayNode : street.getWayNodes()) {
                                        Node node = myMap.getNodeByID(wayNode.getNodeId());
                                        if (node != null) {
                                            if (suburb.contains(node.getLatitude(), node.getLongitude())) {
                                                subUrbName = suburb.getName();
                                                break;
                                            }
                                        }
                                    }
                                    if (subUrbName != null)
                                        break;
                                }

                                retval.add(new AddressPlace(this, street, cityBounds.getName(), subUrbName));

                                if (retval.size() > MAXRESULTS)
                                    break;
                            } catch (Exception e) {
                                LOG.log(Level.SEVERE, "Cannot load way for city id=" + id + " type=" + idType + " named '" + pCity + "' - ignoring this way", e);
                            }
                        }
                        if (retval.size() > MAXRESULTS)
                            break;
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Cannot load a city named '" + pCity + "' - ignoring this city", e);
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot load all cities named '" + pCity + "'", e);
            } finally {
                if (cities != null) {
                    try {
                        cities.getStatement().close();
                    } catch (SQLException e) {
                        LOG.log(Level.WARNING, "Cannot close statement after loading all cities named '" + pCity + "'", e);
                    }
                    try {
                        cities.close();
                    } catch (SQLException e) {
                        LOG.log(Level.WARNING, "Cannot close resultset after loading all cities named '" + pCity + "'", e);
                    }
                }
            }

        } else {
            // search all ways and return them as AddressPlaces
            Iterator<Way> streets = myMap.getWaysByName(pStreet, null);

            while (streets.hasNext()) {
                Way street = streets.next();
                try {
                    retval.add(new AddressPlace(this, street, getCityNameForWay(street), getSuburbNameForWay(street)));
                } catch (RuntimeException e) {
                    LOG.log(Level.WARNING, "Exception while adding a search-result for the way with id=" + street.getId(), e);
                }
            }

        }
        return retval;
    }

    /**
     * ${@inheritDoc}.
     */
    public Collection<Place> findPlaces(final String pSearchExpression) {
        return findAddress(null, pSearchExpression, null, null, null);
    }

    /**
     * ${@inheritDoc}.
     */
    public void setMap(final IDataSet pMap) {
        if (pMap == null)
            throw new IllegalArgumentException("null map given");

        this.myMap = pMap;
        //indexDatabase(myMap);
    }

    /**
     * @param bounds the area to search in
     * @return all suburbs in that area (never null)
     */
    private Collection<CityBounds> getSuburbs(final Bounds bounds) {
        if (bounds == null)
            throw new IllegalArgumentException("null bounds given");
        LinkedList<CityBounds> retval = new LinkedList<CityBounds>();

        ResultSet suburbs = null;
        try {
            suburbs = getDatabase().executeStreamingQuery(
                    "SELECT * FROM suburb WHERE "
                    + " (centerLat + radius > " + FixedPrecisionCoordinateConvertor.convertToFixed(bounds.getMin().lat()) + ") AND"
                    + " (centerLat - radius < " + FixedPrecisionCoordinateConvertor.convertToFixed(bounds.getMax().lat()) + ") AND"
                    + " (centerLon + radius > " + FixedPrecisionCoordinateConvertor.convertToFixed(bounds.getMin().lon()) + ") AND"
                    + " (centerLon - radius < " + FixedPrecisionCoordinateConvertor.convertToFixed(bounds.getMax().lon()) + ")");
//DEBUG: 10,10 radius=0.07  min=9.8,9.8 max=10.19,10.19
            while (suburbs.next()) {
                long id = suburbs.getLong("id");
                int idType = suburbs.getInt("idType");
                CityBounds suburbBounds = getPlaceBounds(idType, id);
                if (suburbBounds == null) {
                    continue;
                }
                LatLon min = suburbBounds.getMin();
                LatLon max = suburbBounds.getMax();
                if (bounds.contains(min.lat(), min.lon())
                  || bounds.contains(min.lat(), max.lon())
                  || bounds.contains(max.lat(), min.lon())
                  || bounds.contains(max.lat(), max.lon())) {
                    retval.add(suburbBounds);
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "[SQLException] Problem in "
                       + getClass().getName()
                       + " - cannot load suburbs for city",
                         e);
        } finally {
            if (suburbs != null) {
                try {
                    suburbs.getStatement().close();
                } catch (SQLException e) {
                    LOG.log(Level.WARNING, "Cannot close statement after loading all suburbs of a city", e);
                }
                try {
                    suburbs.close();
                } catch (SQLException e) {
                    LOG.log(Level.WARNING, "Cannot close resultset after loading all suburbs of a city", e);
                }
            }
        }
        return retval;
    }


    /**
     * @param street a way
     * @return bounds that contain all points of that way.
     */
    private Bounds getBoundsForWay(final Way street) {
        CityBounds bounds = new CityBounds(WayHelper.getTag(street, Tags.TAG_NAME));
        for (WayNode wayNode : street.getWayNodes()) {
            Node node = myMap.getNodeByID(wayNode.getNodeId());
            if (node != null) {
                bounds.addPoint(node.getLatitude(), node.getLongitude());
            }
        }
        return bounds;
    }

    /**
     * @param street the way to look for
     * @return a city that contains at lease a part of this way or null
     */
    protected Collection<CityBounds> getCitysForWay(final Way street) {
        Bounds bounds = getBoundsForWay(street);

        if (bounds == null)
            throw new IllegalArgumentException("null bounds found for way");
        LinkedList<CityBounds> retval = new LinkedList<CityBounds>();

        ResultSet cities = null;
        try {
            cities = getDatabase().executeStreamingQuery(
                    "SELECT * FROM city WHERE "
                    + " (centerLat + radius > " + FixedPrecisionCoordinateConvertor.convertToFixed(bounds.getMin().lat()) + ") AND"
                    + " (centerLat - radius < " + FixedPrecisionCoordinateConvertor.convertToFixed(bounds.getMax().lat()) + ") AND"
                    + " (centerLon + radius > " + FixedPrecisionCoordinateConvertor.convertToFixed(bounds.getMin().lon()) + ") AND"
                    + " (centerLon - radius < " + FixedPrecisionCoordinateConvertor.convertToFixed(bounds.getMax().lon()) + ")");

            while (cities.next()) {
                long id = cities.getLong("id");
                int idType = cities.getInt("idType");
                CityBounds cityBounds = getPlaceBounds(idType, id);

//              does the city really contain at least one node of the way?
                for (WayNode wayNode : street.getWayNodes()) {
                    Node node = myMap.getNodeByID(wayNode.getNodeId());
                    if (node != null) {
                        if (cityBounds.contains(node.getLatitude(), node.getLongitude())) {
                            retval.add(cityBounds);
                            break;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "[SQLException] Problem in "
                       + getClass().getName()
                       + " - cannot load city for way",
                         e);
        } finally {
            if (cities != null) {
                try {
                    cities.getStatement().close();
                } catch (SQLException e) {
                    LOG.log(Level.WARNING, "Cannot close statement after loading all cities for a way", e);
                }
                try {
                    cities.close();
                } catch (SQLException e) {
                    LOG.log(Level.WARNING, "Cannot close resultset after loading all cities for a way", e);
                }
            }
        }
        return retval;
    }

    /**
     * @param street the way to look for
     * @return the name of one suburb this way is in or null
     */
    public String getCityNameForWay(final Way street) {
        String cityName = null;
        try {
            Collection<CityBounds> cities = getCitysForWay(street);
            if (cities.size() > 0)
                cityName = cities.iterator().next().getName();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while getting cities that contain way " + street.getId(), e);
        }
        return cityName;
    }

    /**
     * @param street the way to look for
     * @return the name of one suburb this way is in or null
     */
    public String getSuburbNameForWay(final Way street) {
        String suburbName = null;
        try {
            Collection<CityBounds> suburbs = getSuburbsForWay(street);
            if (suburbs.size() > 0)
                suburbName = suburbs.iterator().next().getName();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while getting suburbs that contain way " + street.getId(), e);
        }
        return suburbName;
    }

    /**
     * @param street the way to look for
     * @return all suburbs this street is in (never null)
     */
    protected Collection<CityBounds> getSuburbsForWay(final Way street) {
        if (street == null)
            throw new IllegalArgumentException("null way given");

        Bounds streetBounds = getBoundsForWay(street);
        if (streetBounds == null)
            return new LinkedList<CityBounds>();

        LinkedList<CityBounds> retval = new LinkedList<CityBounds>();

        ResultSet suburbs = null;
        try {
            suburbs = getDatabase().executeStreamingQuery(
                    "SELECT * FROM suburb WHERE "
                    + " (centerLat + radius > " + FixedPrecisionCoordinateConvertor.convertToFixed(streetBounds.getMin().lat()) + ") AND"
                    + " (centerLat - radius < " + FixedPrecisionCoordinateConvertor.convertToFixed(streetBounds.getMax().lat()) + ") AND"
                    + " (centerLon + radius > " + FixedPrecisionCoordinateConvertor.convertToFixed(streetBounds.getMin().lon()) + ") AND"
                    + " (centerLon - radius < " + FixedPrecisionCoordinateConvertor.convertToFixed(streetBounds.getMax().lon()) + ")");

            while (suburbs.next()) {
                long id = suburbs.getLong("id");
                int idType = suburbs.getInt("idType");
                CityBounds suburbBounds = getPlaceBounds(idType, id);

                // does the suburb really contain at least one node of the way?
                for (WayNode wayNode : street.getWayNodes()) {
                    Node node = myMap.getNodeByID(wayNode.getNodeId());
                    if (node != null) {
                        if (suburbBounds.contains(node.getLatitude(), node.getLongitude())) {
                            retval.add(suburbBounds);
                            break;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "[SQLException] Problem in "
                       + getClass().getName()
                       + " - cannot load city for way",
                         e);
        } finally {
            if (suburbs != null) {
                try {
                    suburbs.getStatement().close();
                } catch (SQLException e) {
                    LOG.log(Level.WARNING, "Cannot close statement after loading all suburbs of a city for a way ", e);
                }
                try {
                    suburbs.close();
                } catch (SQLException e) {
                    LOG.log(Level.WARNING, "Cannot close resultset after loading all suburbs of a city for a way ", e);
                }
            }
        }
        return retval;
    }


    /**
     * @param pWay the way to look for
     * @return a zip-code that contains at lease a part of this way or null
     */
    public String getZipCodeNameForWay(final Way pWay) {
        if (pWay == null) {
            throw new IllegalArgumentException("null way given");
        }
        String zip = WayHelper.getTag(pWay, Tags.TAG_ADDR_POSTALCODE);
        if (zip == null) {
            zip = WayHelper.getTag(pWay, Tags.TAG_ADDR_POSTALCODE2);
        }
        return zip;

        /*Bounds streetBounds = getBoundsForWay(pWay);
        if (streetBounds == null)
            return new LinkedList<CityBounds>();

        return getZipCodes(streetBounds);*/
        //TODO implement getting zip-coe by area
    }

    /**
     * @return Returns the map.
     * @see #myMap
     */
    public IDataSet getMap() {
        return myMap;
    }

    /**
     * Checkpoint the database.
     */
    public void checkpointDB() {
         getDatabase().executeStatement("CHECKPOINT DEFRAG");
    }

    /**
     * If we have a database-connection,
     * commit and properly close it.
     */
    public static void shutdown() {
        if (myDatabase != null) {
            myDatabase.commit();
            myDatabase.executeStatement("SHUTDOWN COMPACT");
            myDatabase.release();
            myDatabase = null;
        }

    }

    /**
     * {@inheritDoc}
     */
    /* @Override */ // TODO: Check override!
    public Collection<CityPlace> findPlaces(final Rectangle2D aSearchArea) {
        LOG.log(Level.FINEST, "call findPlaces"
                + "  ( Rectangle2D=" + aSearchArea + ")");
        Collection<CityPlace> retval = new LinkedList<CityPlace>();
        // search the DB for cities with this name and return their ways
        ResultSet cities = null;
        try {
            cities = getDatabase().executeStreamingQuery(
                    "SELECT * FROM city WHERE "
                    + "  centerLat < " + FixedPrecisionCoordinateConvertor.convertToFixed(aSearchArea.getMaxY())
                    + "  AND centerLat > " + FixedPrecisionCoordinateConvertor.convertToFixed(aSearchArea.getMinY())
                    + "  AND centerLon < " + FixedPrecisionCoordinateConvertor.convertToFixed(aSearchArea.getMaxX())
                    + "  AND centerLon > " + FixedPrecisionCoordinateConvertor.convertToFixed(aSearchArea.getMinX()));

                while (cities.next()) {
                    long id = cities.getLong("id");
                    int idType = cities.getInt("idType");
                    try {
                        LOG.log(Level.FINEST, "found a matching city id=" + id + " type=" + idType + ". Looking for streets...");
                        CityBounds cityBounds = getPlaceBounds(idType, id);

                        if (cityBounds == null) {
                            LOG.log(Level.SEVERE, "Cannot load bounds of city id=" + id + " type=" + idType + ". ignoring it.");
                            continue;
                        }
                        CityPlace place = new CityPlace(this, cityBounds);
                        retval.add(place);
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Cannot load city in area '" + aSearchArea + "'", e);
                    }

                }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot load all cities in area '" + aSearchArea + "'", e);
        } finally {
            if (cities != null) {
                try {
                    cities.getStatement().close();
                } catch (SQLException e) {
                    LOG.log(Level.WARNING, "Cannot close statement after loading all cities in area", e);
                }
                try {
                    cities.close();
                } catch (SQLException e) {
                    LOG.log(Level.WARNING, "Cannot close resultset after loading all cities in area", e);
                }
            }
        }
        return retval;
    }

}
