/**
 * WaysIndex.java
 * created: 31.01.2009 10:41:41
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


//automatically created logger for debug and error -output
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.NodeHelper;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.h2.DatabaseContext;
import org.openstreetmap.osm.data.searching.HouseNumberHelper;
import org.openstreetmap.osm.data.searching.NameHelper;
import org.openstreetmap.osm.data.searching.NodePlace;
import org.openstreetmap.osm.data.searching.Place;
import org.openstreetmap.osm.data.searching.WayPlace;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.util.FixedPrecisionCoordinateConvertor;


/**
 * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * WaysIndex.java<br/>
 * created: 31.01.2009 10:41:41 <br/>
 *<br/><br/>
 * All the code for the {@link AdvancedAddressDBPlaceFinder} regarding indexing and finding
 * streets.
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:AdvancedAddressDBPlaceFinder.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class WaysIndex {

    /**
     * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
     * Project: libosm<br/>
     * WaysIndex.java<br/>
     * created: 14.03.2009 15:52:26 <br/>
     *<br/><br/>
     * <b>Search-Result for an Interpolated House-Number that has no corresponding node in the map.</b>
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    public class HouseNumberedLocation extends Place {
        /**
         * The exact Location of the interplated place.
         */
        private LatLon myLocation;
        /**
         * The map we operate on.
         */
        private IDataSet myMap;
        /**
         * The street we belong to.
         */
        private Street myWay;
        /**
         * The housenumber that was interpolated.
         */
        private String myHouseNumber;
        /**
         * @param aLocation The exact Location of the interplated place.
         * @param aMap The map we operate on.
         * @param aWay The street we belong to.
         * @param aHouseNumber The housenumber that was interpolated.
         */
       public HouseNumberedLocation(final LatLon aLocation,
                final IDataSet aMap,
                final Street aWay,
                final String aHouseNumber) {
           super(aWay.getName() + " " + aHouseNumber, aLocation);
            this.myLocation = aLocation;
            this.myMap = aMap;
            this.myWay = aWay;
            this.myHouseNumber = aHouseNumber;
        }

    /**
         * Just an overridden ToString to return this classe's name
         * and hashCode.
         * @return className and hashCode
         */
        public String toString() {
            return myWay.getName() + " " + myHouseNumber;
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public Node getResult() {
            Set<Long> wayNodeIDs = myWay.getWayNodeIDs();
            Node nearestNodeOnStreet = null;
            double dist = Double.MAX_VALUE;
            for (Long nodID : wayNodeIDs) {
                Node node = myMap.getNodeByID(nodID);
                if (node != null) {
                    double nodeDist = LatLon.dist(myLocation.lat(), myLocation.lon(), node.getLatitude(), node.getLongitude());
                    if (nodeDist < dist) {
                        dist = nodeDist;
                        nearestNodeOnStreet = node;
                    }
                }
            }
            if (nearestNodeOnStreet != null) {
                return nearestNodeOnStreet;
            }
            return myMap.getNearestNode(myLocation, null);
        }
    }

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(WaysIndex.class.getName());


    /**
     * The map we are operating on.
     */
    private AbstractAddressDB myDatabase;

    /**
     * Cache for {@link #indexDatabase(IDataSet)}.
     */
    private PreparedStatement insetStreetPolyline = null;


    /**
     * @param aDatabase the {@link AbstractAddressDB} we are a helper for
     */
    public WaysIndex(final AbstractAddressDB aDatabase) {
        setDatabase(aDatabase);
    }

    /**
     * Add the given street to the index.
     * @param aStreet the street to index
     */
    public void indexWay(final Way aStreet) {

        DatabaseContext db = this.getDatabase().getDatabase();
        try {
            // add entry in the database
            int i = 1;
            String name = WayHelper.getTag(aStreet, Tags.TAG_NAME);
            String highwayType = WayHelper.getTag(aStreet, Tags.TAG_HIGHWAY);

            if (highwayType == null) {
                return;
            }

            if (name == null) {
                LOG.log(Level.FINER, "We found a street with no name. Refusing to index it. wayID=" + aStreet.getId());
                return;
            }

            // find out what type of place we have here
            if (insetStreetPolyline == null)
                insetStreetPolyline = db.prepareStatement("MERGE INTO street (id, defaultName, centerLat, centerLon, radius) "
                    + "VALUES (?, ?, ?, ?, ?)");
            PreparedStatement statement = insetStreetPolyline;

            statement.setLong(i++, aStreet.getId());
            statement.setString(i++, NameHelper.normalizeName(name));

            Bounds bounds = WayHelper.getBoundsForWay(aStreet, getDatabase().getMap());
            if (bounds == null) {
                LOG.log(Level.WARNING, "Street with " + aStreet.getWayNodes().size() + " nodes has no bounds");
                return;
            }

            LatLon center = bounds.getCenter();
            double radius = bounds.getSize();
            statement.setLong(i++, FixedPrecisionCoordinateConvertor.convertToFixed(center.lat()));
            statement.setLong(i++, FixedPrecisionCoordinateConvertor.convertToFixed(center.lon()));
            statement.setLong(i++, FixedPrecisionCoordinateConvertor.convertToFixed(radius));

            statement.clearWarnings();
            if (statement.executeUpdate() != 1)
                LOG.log(Level.SEVERE, "Error while inserting street into the address-database wayID=" + aStreet.getId()
                        + "\nwarnings=" + insetStreetPolyline.getWarnings());
            this.myDatabase.doCommitAsync();
        } catch (Exception e) {
            if (e.getMessage().indexOf("Violation of unique constraint") != -1) {
                LOG.log(Level.FINE, "We seem to have indexed this way before. (vioolation of unique-constraint.) This is Okay.");
            } else {
                LOG.log(Level.SEVERE, "Error while inserting street into the address-database wayID=" + aStreet.getId(), e);
            }
        }

        LOG.log(Level.FINE, "indexing street done");
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
        return "WaysIndex@" + hashCode();
    }

    /**
     * Find all streets matching this name in the given bounds.
     * @param aHouseNb optional house-number
     * @param aStreet the street-name
     * @param aBounds the bounds to look in
     * @return all such streets in the index
     */
    public Collection<Place> findStreets(final String aHouseNb,
                                                   final String aStreet,
                                                   final Bounds aBounds) {
        Collection<Street> foundWays = new LinkedList<Street>();
        try {
            ResultSet cities = getDatabase().getDatabase().executeStreamingQuery(
                    "SELECT * FROM street WHERE defaultName like '"
                    + NameHelper.buildNameSearchSQLMatch(aStreet) + "' AND"
                    + " (centerLat + radius > " + FixedPrecisionCoordinateConvertor.convertToFixed(aBounds.getMin().lat()) + ") AND"
                    + " (centerLat - radius < " + FixedPrecisionCoordinateConvertor.convertToFixed(aBounds.getMax().lat()) + ") AND"
                    + " (centerLon + radius > " + FixedPrecisionCoordinateConvertor.convertToFixed(aBounds.getMin().lon()) + ") AND"
                    + " (centerLon - radius < " + FixedPrecisionCoordinateConvertor.convertToFixed(aBounds.getMax().lon()) + ")");

            while (cities.next()) {
                long id = cities.getLong("id");
                try {
                    LOG.log(Level.FINEST, "found a matching street id=" + id);
                    Way way = getDatabase().getMap().getWaysByID(id);
                    if (way == null) {
                        LOG.log(Level.SEVERE, "A way with id=" + id + " we once indexed no longer exists in the map. ignoring it.");
                        continue;
                    }
                    if (way.getWayNodes().size() < 1) {
                        LOG.log(Level.SEVERE, "A way with id=" + id + " has no nodes. ignoring it.");
                        continue;
                    }
                    foundWays.add(new Street(way));
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Cannot load a street named '" + aStreet + "' - ignoring this street", e);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot load all street named '" + aStreet + "'", e);
        }

        // combine ways that are parts of the same street
        try {
            boolean didCombine = true;
            while (didCombine) {
                didCombine = false;
                Collection<Street> combinedWays = new LinkedList<Street>();
                for (Street newWay : foundWays) {
                    boolean dontAdd = false;
                    for (Street oldWay : combinedWays) {
                        if (isSameStreet(oldWay, newWay)) {
                            dontAdd = true;
                            oldWay.add(newWay);
                            didCombine = true;
                        }
                    }
                    if (!dontAdd) {
                        combinedWays.add(newWay);
                    }
                }
                foundWays = combinedWays;
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE,  "Internel error while combining ways that"
                                 + " belong to the same street in search-results.");
        }

        Collection<Place> retval = new LinkedList<Place>();
        for (Street way : foundWays) {
            Place place = findHouseNumber(way, aHouseNb);
            if (place != null) {
                retval.add(place);
            }
        }
        return retval;
    }

    /**
     * Try your best to find the place for that house-number.
     * If not found but not known for the house-number to not exist,
     * return a {@link WayPlace}.
     * @param way the way to return
     * @param aHouseNb house-number. May be null or empty
     * @return may return null if it is known that this nhouse-number does not exist for this street
     */
    protected Place findHouseNumber(final Street way, final String aHouseNb) {
        if (aHouseNb == null || aHouseNb.trim().length() == 0) {
            return new WayPlace(way.getWays().iterator().next(), getDatabase().getMap());
        }
        try {
            Bounds streetBounds = way.getBounds(getDatabase().getMap());

            // We need all the nodes that are tagged "addr:housenumber"
            // They may be the correct house-number or the start or end or an interpolated section
            List<Node> houseNumberNodes = HouseNumberHelper.getHouseNumberedNodes(getDatabase().getMap(), streetBounds, way.getName(), way.getWayIDs());
            for (Node node : houseNumberNodes) {
                String houseNR = NodeHelper.getTag(node, "addr:housenumber");
                if (houseNR == null) {
                    houseNR = NodeHelper.getTag(node, "addr:housename");
                    if (houseNR == null) {
                        continue;
                    }
                }
                if (houseNR.equalsIgnoreCase(aHouseNb)) {
                    // we found a node that represents this exact house-number
                    return new HouseNumberedPlace(node, getDatabase().getMap(), way);
                }
            }

            // find house-number in Building-Poylgons
            List<Way> houseNumberAreas = HouseNumberHelper.getHouseNumberedPolygons(getDatabase().getMap(), streetBounds, way.getName(), way.getWayIDs());
            for (Way area : houseNumberAreas) {
                String houseNR = WayHelper.getTag(area, Tags.TAG_ADDR_HOUSENUMBER);
                if (houseNR == null) {
                    houseNR = WayHelper.getTag(area, Tags.TAG_ADDR_HOUSENAME);
                    if (houseNR == null) {
                        continue;
                    }
                }
                if (houseNR.equalsIgnoreCase(aHouseNb)) {
                    // we found a node that represents this exact house-number
                    return new HouseAreaPlace(area, getDatabase().getMap(), way);
                }
            }

         // find house-number in interpolated ones
            try {
                int givenHouseNr = Integer.parseInt(aHouseNb);

                // find the house-number before and after ours
                for (Node node : houseNumberNodes) {
                    String houseNR = NodeHelper.getTag(node, "addr:housenumber");
                    try {
                        int startHouseNr = Integer.parseInt(houseNR);
                        if (startHouseNr > givenHouseNr) {
                            // start-number is past ours
                            continue;
                        }
                        Iterator<Way> interpolationWays =  HouseNumberHelper.getInterpolationWays(getDatabase().getMap(), node, way.getName(), way.getWayIDs());
                        while (interpolationWays.hasNext()) {
                            Way interpolationWay = interpolationWays.next();
                            LatLon location = HouseNumberHelper.findHouseNrInInterpolation(getDatabase().getMap(), givenHouseNr, node, startHouseNr, interpolationWay);
                            if (location != null) {
                                return new HouseNumberedLocation(location, getDatabase().getMap(), way, aHouseNb);
                            }
                        }
                    } catch (NumberFormatException x) {
                        //ignore non-numerical house-numbers
                    }
                }
            } catch (NumberFormatException x) {
                //ignore non-numerical house-numbers
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Proble while looking for house-number '" + aHouseNb + "' of Street '" + way.getName() + "'", e);
        }
        return new WayPlace(way.getWays().iterator().next(), getDatabase().getMap());
    }

    /**
     * @param aOldWay one street to test
     * @param aNewWay the other street to test with
     * @return true if both streets are connected
     */
    private boolean isSameStreet(final Street aOldWay, final Street aNewWay) {
        if (!aOldWay.getName().equalsIgnoreCase(aNewWay.getName())) {
            return false;
        }
        Set<Long> wayNodeIDs = aOldWay.getWayNodeIDs();
        for (Long id : wayNodeIDs) {
            if (aNewWay.getWayNodeIDs().contains(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
     * Project: libosm<br/>
     * WaysIndex.java<br/>
     * created: 14.03.2009 15:41:48 <br/>
     *<br/><br/>
     * <b>A place represented by a house-numnber</b>
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    public static final class HouseAreaPlace extends WayPlace {
        /**
         * The street.
         */
        private final Street myWay;

        /**
         * @param myHousePolygon  The Polygon with the house-number.
         * @param aMap The map we are operating on
         * @param aWay The street.
         */
        public HouseAreaPlace(final Way myHousePolygon, final IDataSet aMap, final Street aWay) {
            super(myHousePolygon, aMap);
            myWay = aWay;
        }

        /**
         * ${@inheritDoc}.
         */
        public String toString() {
            return myWay.getName() + " " + WayHelper.getTag(getWay(), Tags.TAG_ADDR_HOUSENUMBER);
        }
    }
    /**
     * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
     * Project: libosm<br/>
     * WaysIndex.java<br/>
     * created: 14.03.2009 15:41:48 <br/>
     *<br/><br/>
     * <b>A place represented by a house-numnber</b>
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    public static final class HouseNumberedPlace extends NodePlace {
        /**
         * The street.
         */
        private final Street myWay;

        /**
         * @param aNode  The Node with the house-number.
         * @param aMap The map we are operating on
         * @param aWay The street.
         */
        public HouseNumberedPlace(final Node aNode, final IDataSet aMap, final Street aWay) {
            super(aNode, aMap);
            myWay = aWay;
        }

        /**
         * ${@inheritDoc}.
         */
        public String toString() {
            return myWay.getName() + " " + NodeHelper.getTag(getNode(), "addr:housenumber");
        }
    }

    /**
     * Helper-class to group all {@link Way}s that are
     * part of the same street (equal name and shaared wayNodes).
     *
     */
    private static class Street {
        /**
         * The name of the street.
         */
        private String myName;
        /**
         * All {@link WayNode}s of the ways that make up this street.
         */
        private Set<Long> myWayNodeIDs = new HashSet<Long>();
        /**
         * All ways that make up this street.
         */
        private Collection<Way> myWays = new LinkedList<Way>();
        /**
         * Make a new Street.
         * @param initialWay the way to start with
         */
        public Street(final Way initialWay) {
            myName = WayHelper.getTag(initialWay, Tags.TAG_NAME);
            myWays.add(initialWay);
            addWayNodes(initialWay);
        }
        /**
         * @param aMap the map to get the nodes
         * @return the bounds of all ways making up this street.
         */
        public Bounds getBounds(final IDataSet aMap) {
            WayHelper wayHelper = aMap.getWayHelper();
            return wayHelper.getWayBounds(this.myWays);
        }
        /**
         * Combine 2 streets.
         * @param other the other street.
         */
        public  void add(final Street other) {
            myWayNodeIDs.addAll(other.myWayNodeIDs);
            myWays.addAll(other.myWays);
        }
        /**
         * @param initialWay the way whos wayNodes to add
         */
        private void addWayNodes(final Way initialWay) {
            List<WayNode> wayNodes = initialWay.getWayNodes();
            for (WayNode wayNode : wayNodes) {
                myWayNodeIDs.add(wayNode.getNodeId());
            }
        }
        /**
         * @return the wayNodeIDs
         */
        public Set<Long> getWayNodeIDs() {
            return myWayNodeIDs;
        }
        /**
         * @return the ways
         */
        public Collection<Way> getWays() {
            return myWays;
        }
        /**
         * @return the ways
         */
        public Collection<Long> getWayIDs() {
            ArrayList<Long> ids = new ArrayList<Long>();
            for (Way way : getWays()) {
                ids.add(way.getId());
            }
            return ids;
        }
        /**
         * @return the name
         */
        public String getName() {
            return myName;
        }
    }
}


