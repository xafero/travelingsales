/**
 * CitiesIndex.java
 * created: 31.01.2009 09:25:20
 * (c) 2009 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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
package org.openstreetmap.osm.data.searching.advancedAddressDB;

import java.awt.geom.Rectangle2D;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.NodeHelper;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.h2.DatabaseContext;
import org.openstreetmap.osm.data.searching.CityBounds;
import org.openstreetmap.osm.data.searching.CityPlace;
import org.openstreetmap.osm.data.searching.NameHelper;
import org.openstreetmap.osm.data.searching.Place;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.util.FixedPrecisionCoordinateConvertor;


/**
 * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * CitiesIndex.java<br/>
 * created: 31.01.2009 09:25:20 <br/>
 *<br/><br/>
 * All the code for the {@link AdvancedAddressDBPlaceFinder} regarding indexing and finding
 * cities.
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:AdvancedAddressDBPlaceFinder.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class CitiesIndex {

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(CitiesIndex.class.getName());


    /**
     * The default size of a place=city that has no
     * defining polygon but only a center-point.
     * See <a href="http://wiki.openstreetmap.org/index.php/Map_Features#Places">
     * Map_Features#Places on the OSM-Wiki</a> for details.
     * @see #getPlaceBounds(Node)
     */
    public static final double DEFAULT_CITY_SIZE = 0.9d;

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
    private static final double DEFAULT_VILLAGE_SIZE = 0.1d;

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
     * The main-class for which we are a helper.
     */
    private AbstractAddressDB myDatabase;

    /**
     * Cache for {@link #indexDatabase(IDataSet)}.
     */
    private PreparedStatement insetCityPolygon = null;

    /**
     * Cache for {@link #indexDatabase(IDataSet)}.
     */
    private PreparedStatement insetCityNode = null;


    /**
     * Cache for {@link #indexDatabase(IDataSet)}.
     */
    private PreparedStatement insetSuburbPolygon = null;


    /**
     * Cache for {@link #indexDatabase(IDataSet)}.
     */
    private PreparedStatement insetSuburbNode = null;


    /**
     * Create a new cities-index with the given main-class.
     * @param aDatabase the adressdb for wich we are a helper.
     */
    public CitiesIndex(final AbstractAddressDB aDatabase) {
        setDatabase(aDatabase);
    }

    /**
     * Add the given city-polygon to our index.
     * @param city the city to index
     */
    public void indexWay(final Way city) {

        DatabaseContext db = this.getDatabase().getDatabase();
        try {
            // add entry in the database
            int i = 1;
            String name = WayHelper.getTag(city, Tags.TAG_NAME);
            String placeType = WayHelper.getTag(city, Tags.TAG_PLACE);
            String boundary = WayHelper.getTag(city, Tags.TAG_BOUNDARY);
            String adminLevel = WayHelper.getTag(city, Tags.TAG_ADMIN_LEVEL);

            if (placeType == null && (boundary != null && adminLevel != null)) {
                if (boundary.equalsIgnoreCase("administrative")) {
                    if (adminLevel.equalsIgnoreCase("7")) {
                        placeType = "city";
                    } else if (adminLevel.equalsIgnoreCase("8")) {
                        placeType = "suburb";
                    }
                }
            }
            if (placeType == null) {
                return;
            }

            if (name == null) {
                LOG.log(Level.WARNING, "We found a city of type " + placeType + " with no name. Refusing to index it. wayID=" + city.getId());
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
            this.myDatabase.doCommitAsync();
        } catch (Exception e) {
            Level level = Level.SEVERE;
            if (e.getMessage().indexOf("Violation of unique constraint") != -1) {
                level = Level.INFO;
            }
            LOG.log(level, "Error while inserting city into the address-database wayID=" + city.getId(), e);
        }

        LOG.log(Level.FINE, "indexing way- suburb/city/way done");
    }

    /**
     * Index the given node if it is a city.
     * else ignore it.
     * @param city any way. Not null
     */
    public void indexNode(final Node city) {

        DatabaseContext db = getDatabase().getDatabase();
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
            this.getDatabase().doCommitAsync();
        } catch (SQLException e) {
            // we ignore already indexed values
            // it would cost more performance to check for existence first
            if (!e.getMessage().contains("duplicate value(s) for column(s)"))
                LOG.log(Level.SEVERE, "Error while inserting city into the address-database nodeID=" + city.getId(), e);
        }

        LOG.log(Level.FINE, "indexing node- suburb/city/way done");
    }



    /**
     * @param street the way to look for
     * @return a city that contains at lease a part of this way or null
     */
    protected Collection<CityBounds> getCitysForWay(final Way street) {
        Bounds bounds = WayHelper.getBoundsForWay(street, getDatabase().getMap());

        if (bounds == null)
            throw new IllegalArgumentException("null bounds found for way");
        LinkedList<CityBounds> retval = new LinkedList<CityBounds>();

        try {
            ResultSet cities = getDatabase().getDatabase().executeStreamingQuery(
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
                    Node node = getDatabase().getMap().getNodeByID(wayNode.getNodeId());
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
        }
        return retval;
    }

    /**
     * @param street the way to look for
     * @return all suburbs this street is in (never null)
     */
    protected Collection<CityBounds> getSuburbsForWay(final Way street) {
        if (street == null)
            throw new IllegalArgumentException("null way given");

        Bounds streetBounds = WayHelper.getBoundsForWay(street, getDatabase().getMap());
        if (streetBounds == null)
            return new LinkedList<CityBounds>();

        LinkedList<CityBounds> retval = new LinkedList<CityBounds>();

        try {
            ResultSet suburbs = getDatabase().getDatabase().executeStreamingQuery(
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
                    Node node = getDatabase().getMap().getNodeByID(wayNode.getNodeId());
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
        }
        return retval;
    }

    /**
     * @param bounds the area to search in
     * @return all suburbs in that area (never null)
     */
    private Collection<CityBounds> getSuburbs(final Bounds bounds) {
        if (bounds == null)
            throw new IllegalArgumentException("null bounds given");
        LinkedList<CityBounds> retval = new LinkedList<CityBounds>();

        try {
            ResultSet suburbs = getDatabase().getDatabase().executeStreamingQuery(
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
        }
        return retval;
    }

    /**
     * @param idType the type of the city
     * @param id the id of the map-element denoting the city
     * @return the bounds of the city-polygon
     */
    private CityBounds getPlaceBounds(final int idType, final long id) {
        if (idType == PLACETYPES.POLYGON.getCode()) {
            Way cityPolygon = this.getDatabase().getMap().getWaysByID(id);
            if (cityPolygon == null) {
                LOG.log(Level.SEVERE, "Cannot load bounds of city id=" + id + " type=" + idType + " because that way is not in the map.");
                return null;
            }
            return getPlaceBounds(cityPolygon);
        }

        if (idType == PLACETYPES.CENTERNODE.getCode()) {
            Node centerNode = this.getDatabase().getMap().getNodeByID(id);
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

        // see if the radius is explicitly tagged
        /**
         * TODO (Ticket #0000027): Tags=radius, place_radius, diameter or place_diameter
         * values may be like "750 m" or "2.5 km"
         */

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
            if (getDatabase().getMap() == null) {
                LOG.log(Level.SEVERE, "WE HAVE NO MAP in AddressDBPlaceFinder!");
            }
            Node node = getDatabase().getMap().getNodeByID(wayNode.getNodeId());
            if (node != null) {
                polygon.addPoint(node.getLatitude(), node.getLongitude());
                count++;
            }
        }
        final int minPolygonNodes = 3;
        if (count < minPolygonNodes) {
            return null;
        }
        return polygon;
    }

    /**
     * @return Returns the database.
     * @see #myDatabase
     */
    private AbstractAddressDB getDatabase() {
        return myDatabase;
    }


    /**
     * @param aDatabase The database to set.
     * @see #myDatabase
     */
    private void setDatabase(final AbstractAddressDB aDatabase) {
        if (aDatabase == null) {
            throw new IllegalArgumentException("null 'aDatabase' given!");
        }

        Object old = myDatabase;
        if (old == aDatabase) {
            return; // nothing has changed
        }
        this.myDatabase = aDatabase;
    }



    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "CitiesIndex@" + hashCode();
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
    }


    /**
     * Find all cities that match the given name.
     * @param aCityName the name
     * @return the cities found. Never null but may be empty.
     */
    public Collection<Place> findCities(final String aCityName) {
        Collection<Place> retval = new LinkedList<Place>();
        try {
            ResultSet cities = getDatabase().getDatabase().executeStreamingQuery(
                    "SELECT * FROM city WHERE defaultName like '"
                    + NameHelper.buildNameSearchSQLMatch(aCityName) + "'");

            while (cities.next()) {
                long id = cities.getLong("id");
                int idType = cities.getInt("idType");
                try {
                    LOG.log(Level.FINEST, "found a matching city id=" + id + " type=" + idType);
                    CityBounds cityBounds = getPlaceBounds(idType, id);

                    if (cityBounds == null) {
                        LOG.log(Level.SEVERE, "Cannot load bounds of city id=" + id + " type=" + idType + ". ignoring it.");
                        continue;
                    }
                    retval.add(new CityPlace(this.getDatabase(), cityBounds));
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Cannot load a city named '" + aCityName + "' - ignoring this city", e);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot load all cities named '" + aCityName + "'", e);
        }
        return retval;
    }

    /**
     * Find all suburbs and cities without suburbs that match the given name.
     * @param aCityName the name
     * @return the suburbs found and the cities with no suburbs. Never null but may be empty.
     */
    public Collection<Place> findSuburbsAndCities(final String aCityName) {
        Collection<Place> retval = new LinkedList<Place>();
        try {
            ResultSet cities = getDatabase().getDatabase().executeStreamingQuery(
                    "SELECT * FROM city WHERE defaultName like '"
                    + NameHelper.buildNameSearchSQLMatch(aCityName) + "'");

            while (cities.next()) {
                long id = cities.getLong("id");
                int idType = cities.getInt("idType");
                try {
                    LOG.log(Level.FINEST, "found a matching city id=" + id + " type=" + idType);
                    CityBounds cityBounds = getPlaceBounds(idType, id);

                    if (cityBounds == null) {
                        LOG.log(Level.SEVERE, "Cannot load bounds of city id=" + id + " type=" + idType + ". ignoring it.");
                        continue;
                    }
                    retval.add(new CityPlace(this.getDatabase(), cityBounds));
                    // get Suburbs for city
                    Collection<CityBounds> suburbs = getSuburbs(cityBounds);
                    LOG.log(Level.FINEST, "found a matching city id=" + id + " type=" + idType + " with " + suburbs.size() + " suburbs. cityBounds=" + cityBounds);

                    if (suburbs != null && suburbs.size() > 0) {
                        for (CityBounds bounds : suburbs) {
                            retval.add(new CityPlace(this.getDatabase(), cityBounds, bounds));
                        }
                    } else {
                        retval.add(new CityPlace(this.getDatabase(), cityBounds));

                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Cannot load a city named '" + aCityName + "' - ignoring this city", e);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot load all cities named '" + aCityName + "'", e);
        }
        return retval;
    }


    /**
     * Find all cities in the given rectangle.
     * @param aSearchArea the area to search
     * @return all cities, may be empty but not null.
     */
    public Collection<CityPlace> findCities(final Rectangle2D aSearchArea) {

        LOG.log(Level.FINEST, "call findPlaces"
                + "  ( Rectangle2D=" + aSearchArea + ")");
        Collection<CityPlace> retval = new LinkedList<CityPlace>();
        // search the DB for cities with this name and return their ways
        try {
            ResultSet cities = getDatabase().getDatabase().executeStreamingQuery(
                    "SELECT id, idType FROM city WHERE "
                    + "  centerLat < " + FixedPrecisionCoordinateConvertor.convertToFixed(aSearchArea.getMaxY())
                    + "  AND centerLat > " + FixedPrecisionCoordinateConvertor.convertToFixed(aSearchArea.getMinY())
                    + "  AND centerLon < " + FixedPrecisionCoordinateConvertor.convertToFixed(aSearchArea.getMaxX())
                    + "  AND centerLon > " + FixedPrecisionCoordinateConvertor.convertToFixed(aSearchArea.getMinX()));

                while (cities.next()) {
                    long id = cities.getLong("id");
                    int idType = cities.getInt("idType");
                    try {
                        LOG.log(Level.FINEST, "found a matching city id=" + id + " type=" + idType + " in area.");
                        CityBounds cityBounds = getPlaceBounds(idType, id);
                        if (cityBounds == null) {
                            LOG.log(Level.SEVERE, "Cannot load bounds of city id=" + id + " type=" + idType + ". ignoring it.");
                            continue;
                        }
                        CityPlace place = new CityPlace(getDatabase(), cityBounds);
                        retval.add(place);
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Cannot load city in area '" + aSearchArea + "'", e);
                    }

                }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot load all cities in area '" + aSearchArea + "'", e);
        }
        return retval;
    }


}


