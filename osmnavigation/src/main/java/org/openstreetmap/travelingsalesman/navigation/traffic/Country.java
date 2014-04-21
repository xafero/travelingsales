/**
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
 *
 *  Created: 26.03.2008
 */
package org.openstreetmap.travelingsalesman.navigation.traffic;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.NodeHelper;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.filter.common.PolygonFileReader;
import org.openstreetmap.travelingsalesman.navigation.traffic.TrafficRuleManager.WayClasses;
import org.openstreetmap.travelingsalesman.routing.selectors.UsedTags;

/**
 * Helper class to store the most basic information about a country.<br/>
 * Specific countries may inherit from this class and override some rules.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class Country {
    /**
     * my logger for debug and error-output.
     */
    static final Logger LOG = Logger.getLogger(Country.class.getName());

    /**
     * Diameter of {@link #myContainsCache} in Latitude / Longitude -space.<br/>
     * Near the border the diameter is automatically reduced to
     * 1/2 or 1/4 of this size.
     * @see #contains(Node)
     */
    private static final double CACHEWIDTH = 0.5;

    /**
     * Factory-method to create a country.
     * @param aName human readable name of the country.
     * @param aCode ISO3166 -code
     * @param aMinLat bounding-box
     * @param aMaxLat bounding-box
     * @param aMinLon bounding-box
     * @param aMaxLon bounding-box
     * @return the country. May be a subclass of this class.
     */
    public static Country createCountry(final String aName,
            final String aCode,
            final double aMinLat,
            final double aMaxLat,
            final double aMinLon,
            final double aMaxLon) {
        // insert new countries that require their own logic here
        // example:
        // if (aCode.equals(aMinLat, aMaxLat, aMinLon, aMaxLon)) {
        //    return new CountryXY();
        // }
        return new Country(aName, aCode, aMinLat, aMaxLat, aMinLon, aMaxLon);
    }
    /**
     * @param aName human readable name of the country.
     * @param aCode ISO3166 -code
     * @param aMinLat bounding-box
     * @param aMaxLat bounding-box
     * @param aMinLon bounding-box
     * @param aMaxLon bounding-box
     */
    protected Country(final String aName,
            final String aCode,
            final double aMinLat,
            final double aMaxLat,
            final double aMinLon,
            final double aMaxLon) {
        super();
        myName = aName;
        myCode = aCode;
        minLat = aMinLat;
        maxLat = aMaxLat;
        minLon = aMinLon;
        maxLon = aMaxLon;
    }
    /**
     * Get the default maxspeed for the given way-type.
     * @param aWayClass the wayclass to evaluate
     * @param aWay the specific way. (required for some national rules)
     * @param aMap the map we operate on
     * @return -1 or the default maximum speed in kilometers per hour
     * @throws IOException if we cannot read our settings
     */
    public int getMaxspeed(final WayClasses aWayClass, final Way aWay, final IDataSet aMap) throws IOException {
        Properties properties = getProperties();

        if (properties.containsKey("maxspeed." + aWayClass.name() + ".incity")) {
            if (isInsideCity(aWay, aMap)) {
                return Integer.parseInt(properties.getProperty("maxspeed." + aWayClass.name() + ".incity"));
            }
        }
        if (properties.containsKey("maxspeed." + aWayClass.name())) {
            return Integer.parseInt(properties.getProperty("maxspeed." + aWayClass.name()));
        }
        if (properties.containsKey("maxspeed.default")) {
            return Integer.parseInt(properties.getProperty("maxspeed.default"));
        }
        if (properties.containsKey("maxspeed.default.incity")) {
            if (isInsideCity(aWay, aMap)) {
                return Integer.parseInt(properties.getProperty("maxspeed.default.incity"));
            }
            return Integer.parseInt(properties.getProperty("maxspeed.default"));
        }
        return -1;
    }

    /**
     * @param aWay the specific way. (required for some national rules)
     * @param aMap the map we operate on
     * @return true if the way is inside a city
     */
    public boolean isInsideCity(final Way aWay, final IDataSet aMap) {
        return TrafficRuleManager.isInsideCity(aWay, aMap);
    }
    /**
     * @return our human-readable name
     */
    public String getName() {
        return myName;
    }
    /**
     * human readable name of the country.
     */
    private String myName;
    /**
     * ISO-code of the coutry.
     * ISO3166 (all capital lettters)
     */
    private String myCode;
    /**
     * Bounding-box of the country.
     * (used to avoid expensive polygon-checking
     * for all countries that cannot possibly contain a given location.)
     */
    private double minLat;
    /**
     * Bounding-box of the country.
     * (used to avoid expensive polygon-checking
     * for all countries that cannot possibly contain a given location.)
     */
    private double maxLat;
    /**
     * Bounding-box of the country.
     * (used to avoid expensive polygon-checking
     * for all countries that cannot possibly contain a given location.)
     */
    private double minLon;
    /**
     * Bounding-box of the country.
     * (used to avoid expensive polygon-checking
     * for all countries that cannot possibly contain a given location.)
     */
    private double maxLon;
    /**
     * country-specific rules. Loaded on demand.
     */
    private Properties myProperties = null;
    /**
     * country-borders. Loaded on demand.
     */
    private Area myPolygon = null;
    /**
     * Cache for {@link #contains(Node)}.<br/>
     * May be null.<br/>
     * Bounding-Box that is definately contained
     * in the country.
     */
    private Rectangle2D myContainsCache;

    /**
     * Detect if the given node is contained in this country.
     * @param aNode the node to check
     * @param aWay (optional) a way the node belongs to
     * @return true if it is so
     */
    public boolean contains(final Node aNode, final Way aWay) {
        //test only one node of the way
        //first test the bounding box
        if (aNode.getLatitude() > this.maxLat) {
            return false;
        }
        if (aNode.getLatitude() < this.minLat) {
            return false;
        }
        if (aNode.getLongitude() > this.maxLon) {
            return false;
        }
        if (aNode.getLongitude() < this.minLon) {
            return false;
        }
        // test is_in before testing the polygon
        String isIn = NodeHelper.getTag(aNode, UsedTags.TAG_IS_IN_COUNTRY);
        if (isIn != null) {
            if (isIn.toLowerCase().contains(myName.toLowerCase())
             || isIn.contains(myCode.toUpperCase())) {
                return true;
            }
        }
       // test is_in before testing the polygon
        if (aWay != null) {
            isIn = WayHelper.getTag(aWay, UsedTags.TAG_IS_IN_COUNTRY);
            if (isIn != null) {
                if (isIn.toLowerCase().contains(myName.toLowerCase())
                        || isIn.contains(myCode.toUpperCase())) {
                    return true;
                }
            }
        }
        // test is_in before testing the polygon
        isIn = NodeHelper.getTag(aNode, Tags.TAG_IS_IN);
        if (isIn != null) {
            if (isIn.toLowerCase().contains(myName.toLowerCase())
             || isIn.contains(myCode.toUpperCase())) {
                return true;
            }
        }
        if (aWay != null) {
            isIn = WayHelper.getTag(aWay, Tags.TAG_IS_IN);
            if (isIn != null) {
                if (isIn.toLowerCase().contains(myName.toLowerCase())
                 || isIn.contains(myCode.toUpperCase())) {
                    return true;
                }
            }
        }
        // See if we are in the cached resule.
        if (this.myContainsCache != null
                && this.myContainsCache.contains(aNode.getLongitude(), aNode.getLatitude())) {
            return true;
        }

        //if inside, test the polygon (.poly -file or OSM-border-relation)
        Area polygon = getPolygon();
        if (polygon != null) {
            // Build a box of size CACHEWIDTH around the node and test that.
            // Cache the result.
            Rectangle2D cache = new Rectangle2D.Double(aNode.getLongitude() - CACHEWIDTH,
                    aNode.getLatitude() - CACHEWIDTH, CACHEWIDTH, CACHEWIDTH);
            if (polygon.contains(cache)) {
                this.myContainsCache = cache;
                return true;
            } else {
                // try a smaller area
                double width = (CACHEWIDTH / 2);
                cache = new Rectangle2D.Double(aNode.getLongitude() - width,
                        aNode.getLatitude() - width, width, width);
                if (polygon.contains(cache)) {
                    this.myContainsCache = cache;
                    return true;
                } else {
                    width = (width / 2);
                    cache = new Rectangle2D.Double(aNode.getLongitude() - width,
                            aNode.getLatitude() - width, width, width);
                    if (polygon.contains(cache)) {
                        this.myContainsCache = cache;
                        return true;
                    }
                }
            }

            return polygon.contains(aNode.getLongitude(), aNode.getLatitude());
        }
        return true;
    }

    /**
     * @return country-specific rules. Loaded on demand.
     * @throws IOException if we cannot load the properties
     */
    public Properties getProperties() throws IOException {
        if (this.myProperties == null) {
            this.myProperties = new Properties();
            this.myProperties.load(this.getClass().getClassLoader().getResourceAsStream("org/openstreetmap/travelingsalesman/navigation/traffic/"
                    + myCode + ".properties"));
        }
        return myProperties;
    }

    /**
     * Are we driving on the left side of the road in this country?
     * @return true if left
     */
    public boolean isDrivingOnLeftSide() {
        try {
            return getProperties().getProperty("driving.side", "right").equalsIgnoreCase("left");
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot load country-specific properties", e);
            return false;
        }
    }
    /**
     * @return the polygon
     */
    public Area getPolygon() {
        if (myPolygon == null) {
            synchronized (this) {
                try {
                    if (myPolygon == null) {
                        TrafficRuleManager.LOG.info("loading " + myCode + ".poly");
                        InputStream in = this.getClass().getClassLoader().getResourceAsStream("org/openstreetmap/travelingsalesman/navigation/traffic/"
                                + myCode + ".poly");
                        PolygonFileReader reader = new PolygonFileReader(in, myCode + ".poly");
                        myPolygon = reader.loadPolygon();
                        TrafficRuleManager.LOG.info("loading " + myCode + ".poly done");
                    }
                } catch (Exception e) {
                    TrafficRuleManager.LOG.log(Level.SEVERE, "Cannot load " + myCode + ".poly", e);
                }
            }
        }
        return myPolygon;
    }
}
