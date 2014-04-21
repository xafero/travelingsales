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
 *  Created: 25.03.2008
 */
package org.openstreetmap.travelingsalesman.navigation.traffic;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.UnitConstants;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.searching.CityPlace;
import org.openstreetmap.osm.data.searching.IAddressDBPlaceFinder;
import org.openstreetmap.osm.data.searching.IPlaceFinder;
import org.openstreetmap.osm.data.searching.advancedAddressDB.AdvancedAddressDBPlaceFinder;
import org.openstreetmap.osm.data.searching.advancedAddressDB.CitiesIndex;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.travelingsalesman.routing.selectors.UsedTags;

/**
 * This class manages general and national/regional traffic-rules.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public final class TrafficRuleManager {

    /**
     * The minimum area (in Lat/Lon -space) to search for a city a node may be contained in.
     * @see #isInsideCity(Way, IDataSet)
     */
    private static final double CITIESSEARCHAREA = CitiesIndex.DEFAULT_CITY_SIZE;

    /**
     * The maximum area (in Lat/Lon -space) to search for a city a node may be contained in.
     * @see #isInsideCity(Way, IDataSet)
     */
    private static final double CITIESCACHEAREA = CITIESSEARCHAREA * 2;


    /**
     * my logger for debug and error-output.
     */
    static final Logger LOG = Logger.getLogger(TrafficRuleManager.class.getName());


    /**
     * Address-database used to determine nearby cities.
     */
    private static IAddressDBPlaceFinder myCities = null;


    /**
     * Utility-classes have no default-constructor.
     */
    private TrafficRuleManager() {
    }

    /**
     * Given a way, return the maximum speed we
     * can drive on it.
     * @param aWay the way to test
     * @param aMap the map we operate on
     * @return the maxspeed in kilometers per hour.
     */
    public static int getMaxspeed(final Way aWay, final IDataSet aMap) {
        int maxspeed = parseMaxspeedTag(aWay);
        if (maxspeed > -1) {
            return maxspeed;
        }
        String highway = WayHelper.getTag(aWay, Tags.TAG_HIGHWAY);
        if (highway != null) {
            WayClasses wayClass = WayClasses.getWayClass(highway);
            if (wayClass != null) {
                Country country = getCountry(aWay, aMap);
                return getNationalMaxspeed(country, wayClass, aWay, aMap);
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Return the maxspeed for a given highway-type
     * in a given cfountry.
     * If unknown Integer.MAX_VALUE is returned.
     * @param aCountry the country
     * @param aWayClass the highway-type
     * @param aWay the specific way. (required for some national rules)
     * @param aMap the map we operate on
     * @return the maxspeed in km per hour or a default maxspeed
     */
    private static int getNationalMaxspeed(final Country aCountry, final WayClasses aWayClass, final Way aWay, final IDataSet aMap) {

        if (aCountry != null) {
            try {
                int maxspeed = aCountry.getMaxspeed(aWayClass, aWay, aMap);
                if (maxspeed > -1) {
                    return maxspeed;
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot load maxspeed for country " + aCountry.getName(), e);
            }
        }
        // hardcoded fallback
        final int fallbackLink = 80;
        final int fallbackTrunk = 130;
        final int fallbackDefault = 100;
        final int fallbackCityDefault = 50;
        final int fallbackLivingStreet = 10;
        boolean isInCity = false;
        if (aCountry != null) {
            isInCity = aCountry.isInsideCity(aWay, aMap);
        } else {
            isInCity = TrafficRuleManager.isInsideCity(aWay, aMap);
        }
        if (isInCity
                && !aWayClass.equals(WayClasses.motorway_link)
                && !aWayClass.equals(WayClasses.motorway)
                && !aWayClass.equals(WayClasses.living_street)) {
            return fallbackCityDefault;
        }
        switch (aWayClass) {
        case motorway: return Integer.MAX_VALUE;
        case motorway_link: return fallbackLink;
        case trunk: return fallbackTrunk;
        case trunk_link: return fallbackLink;
        case living_street: return fallbackLivingStreet;
        case residential: return fallbackCityDefault; // assumed to be inside a city
        default: return fallbackDefault;
        }
    }

    /**
     * Determine what country a road is in.
     * @param aWay the way to check
     * @param aMap the map we operate on
     * @return the country
     */
    private static Country getCountry(final Way aWay, final IDataSet aMap) {
        if (aMap == null) {
            return DEFAULTCOUNTRY;
        }
        Node node = getNodeFromWay(aWay, aMap);
        if (node == null) {
            return myLastCountry;
        }
        if (myLastCountry.contains(node, aWay)) {
            return myLastCountry;
        }
        for (Country c : ALLCOUNTRIES) {
            if (c.contains(node, aWay)) {
                myLastCountry = c;
                return c;
            }
        }
        return DEFAULTCOUNTRY;
    }
    /**
     * The default-country if no other matches.
     */
    private static final Country DEFAULTCOUNTRY = Country.createCountry("Default", "C", 0, 0, 0, 0);

    /**
     * The last country we found something to be in.
     */
    private static Country myLastCountry = DEFAULTCOUNTRY;


    /**
     * Cache for {@link #isInsideCity(Way, IDataSet)}.
     */
    private static Collection<CityPlace> myCitiesCache;


    /**
     * Cache for {@link #isInsideCity(Way, IDataSet)}.
     */
    private static Rectangle2D myCitiesAreaCache;

    /**
     * All countries we know.<br/>
     * Must NOT include  the {@link #DEFAULTCOUNTRY} as comparison
     * against its ISO-code "C" may give lots of false positives.
     */
    private static final Set<Country> ALLCOUNTRIES  = new HashSet<Country>();
    static {
        Properties prop = new Properties();
        try {
            prop.load(TrafficRuleManager.class.getClassLoader().getResourceAsStream("org/openstreetmap/travelingsalesman/navigation/traffic/countries.properties"));
            Enumeration<Object> keys = prop.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                if (key.indexOf('.') != -1) {
                    continue;
                }
                String iso = prop.getProperty(key);
                try {
                    String name = prop.getProperty(key + ".name");
                    double maxLat = Double.parseDouble(prop.getProperty(key + ".maxlat"));
                    double maxLon = Double.parseDouble(prop.getProperty(key + ".maxlon"));
                    double minLat = Double.parseDouble(prop.getProperty(key + ".minlat"));
                    double minLon = Double.parseDouble(prop.getProperty(key + ".minlon"));
                    ALLCOUNTRIES.add(Country.createCountry(name, iso, minLat, maxLat, minLon, maxLon));
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Cannot load traffic-rules for country " + iso, e);
                }
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot load traffic-rules for countries", e);
        }
    }

    /**
     * This is the default rule to use if we have no national rule to use.
     * @see Country#isInsideCity(Way, IDataSet)
     * @param aWay the specific way. (required for some national rules)
     * @param aMap the map we operate on
     * @return true if the way is inside a city
     */
    public static boolean isInsideCity(final Way aWay, final IDataSet aMap) {
        String city = WayHelper.getTag(aWay, Tags.TAG_IS_IN_CITY);
        if (city != null) {
            return city.length() > 0 && !city.equalsIgnoreCase("none");
        }
        if (aMap == null) {
            throw new IllegalArgumentException("null map given");
        }

        // make cached area-query to AddressDB to get all nearby places
        Node node = getNodeFromWay(aWay, aMap);
        if (node == null) {
            return false;
        }
        Collection<CityPlace> places = TrafficRuleManager.myCitiesCache;
        Rectangle2D searchArea       = TrafficRuleManager.myCitiesAreaCache;
        Rectangle2D minimumSearchArea = new Rectangle2D.Double(node.getLongitude() - CITIESSEARCHAREA / 2,
                                                               node.getLatitude() - CITIESSEARCHAREA / 2,
                                                               CITIESSEARCHAREA, CITIESSEARCHAREA);
        if (places == null || !searchArea.contains(minimumSearchArea)) {
            searchArea = new Rectangle2D.Double(node.getLongitude() - CITIESCACHEAREA / 2,
                                                node.getLatitude() - CITIESCACHEAREA / 2,
                                                CITIESCACHEAREA, CITIESCACHEAREA);
            places = getCities(aMap).findPlaces(searchArea);
            TrafficRuleManager.myCitiesCache = places;
            TrafficRuleManager.myCitiesAreaCache = searchArea;
        } else {
            LOG.finer("TrafficRuleManager - we are inside the cached search-area for cities");
        }

        for (CityPlace place : places) {
            if (place.getBounds().contains(node.getLatitude(), node.getLongitude())) {
                LOG.fine("given street is in the city of '" + place.getCityName() + "'");
                return true;
            }
        }
        return false;
    }

    /**
     * Get the first node of the way that we can load.
     * @param aWay the way to get a representative node for
     * @param aMap the map we are using
     * @return may be null
     */
    private static Node getNodeFromWay(final Way aWay, final IDataSet aMap) {
        if (aMap == null) {
            return null;
        }
        List<WayNode> wayNodes = aWay.getWayNodes();
        for (WayNode wayNode : wayNodes) {
            if (wayNode == null) {
                continue;
            }
            Node node = aMap.getNodeByID(wayNode.getNodeId());
            if (node != null) {
                return node;
            }
        }
        return null;
    }

    /**
     * @param aMap the map we operate on
     * @return the cities
     */
    public static IAddressDBPlaceFinder getCities(final IDataSet aMap) {
        if (aMap == null) {
            throw new IllegalArgumentException("null map given");
        }
        if (myCities == null) {
            IPlaceFinder finder = Settings.getInstance().getPlugin(IPlaceFinder.class, AdvancedAddressDBPlaceFinder.class.getName());
            if (finder instanceof IAddressDBPlaceFinder) {
                myCities = (IAddressDBPlaceFinder) finder;
            } else {
                myCities = new AdvancedAddressDBPlaceFinder();
            }
        }
        myCities.setMap(aMap);
        return myCities;
    }

    /**
     * If this way is indiviidually tagged with a maxspeed,
     * parse it and reutrn it.
     * @return the maxspeed in km/h or -1
     * @param aWay the way to check
     */
    private static int parseMaxspeedTag(final Way aWay) {
        String tag = WayHelper.getTag(aWay, UsedTags.TAG_MAXSPEED);
        if (tag == null) {
            return -1;
        }
        tag = tag.toLowerCase();
        try {
            if (tag.indexOf("mph") != -1) {
                return (int) (UnitConstants.USMILES_TO_KM * Integer.parseInt(tag.replace("mph", "").trim()));
            }
            return Integer.parseInt(tag.replace("kmh", "").replace("km/h", "").replace("kph", "").trim());
        } catch (NumberFormatException e) {
            LOG.log(Level.WARNING, "Illegal value for maxspeed tagged to a road. '" + tag + "'", e);
        }
        return -1;
    }

    /**
     * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
     * Project: osmnavigation<br/>
     * StaticFastestRouteMetric.java<br/>
     * created: 18.11.2007 13:58:54 <br/>
     * The road-types we know of.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    enum WayClasses {
        /**
         * A motorway is the fastest known road.
         */
        motorway,
        /**
         * Link to enter/exit a {@link #motorway}.
         */
        motorway_link,
        /**
         * A road that connects {@link #primary}-roads to
         * {@link #motorway}s.
         */
        trunk,
        /**
         * Link to enter/exit a {@link #trunk}.
         */
        trunk_link,
        /**
         * A very fast road. Next class below {@link #motorway}.
         */
        primary,
        /**
         * Link to enter/exit a {@link #primary}.
         */
        primary_link,
        /**
         * A road of the usual secondary road-network
         * connecting all cities.
         */
        secondary,
        /**
         * Link to enter/exit a {@link #secondary}.
         */
        secondary_link,
        /**
         * A road of the usual tertiary road-network
         * connecting all villages.
         */
        tertiary,
        /**
         * A road inside a city/village where people
         * live. Usually heavily speed-limited.
         */
        residential,
        /**
         * The lowest kind of road in the conventional
         * road-network. This does not denote roads that
         * have not yet been classified.
         */
        unclassified,
        /**
         * A street for children to play on,... .
         */
        living_street;

        /**
         * The value to return from {@link #getFactor(String)}
         * for an unknown type of road.
         */
        public static final int UNKNOWNROADFACTOR = 100;

        /**
         * Get the factor to multiple
         * with the length of the way to get
         * the cost of traveling it.
         * @param highway the value of the highway-attribute
         * @return a value between 0 and 15 or 100 if unknown class
         */
        public static int getFactor(final String highway) {
            for (WayClasses wayClass : WayClasses.values()) {

                if (wayClass.name().equalsIgnoreCase(highway))
                    return wayClass.ordinal();
            }
            return UNKNOWNROADFACTOR;
        }

        /**
         * @param highway the value of the highway-attribute
         * @return the wayclass for it or null
         */
        public static WayClasses getWayClass(final String highway) {
            for (WayClasses wayClass : WayClasses.values()) {

                if (wayClass.name().equalsIgnoreCase(highway))
                    return wayClass;
            }
            return null;
        };
    }
}
