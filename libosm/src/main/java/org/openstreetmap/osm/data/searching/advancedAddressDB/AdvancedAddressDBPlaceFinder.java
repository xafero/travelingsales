/**
 * AdvancedAddressDBPlaceFinder.java
 * created: 31.01.2009
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
package org.openstreetmap.osm.data.searching.advancedAddressDB;


//automatically created logger for debug and error -output
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.ConfigurationSetting;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.ConfigurationSetting.TYPES;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.NodeHelper;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.searching.CityBounds;
import org.openstreetmap.osm.data.searching.CityPlace;
import org.openstreetmap.osm.data.searching.IAddressDBPlaceFinder;
import org.openstreetmap.osm.data.searching.Place;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;


/**
 * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * AdvancedAddressDBPlaceFinder.java<br/>
 * created: 31.01.2009 <br/>
 *<br/><br/>
 * This {@link IPlaceFinder} uses a local H2-database that stores an index
 * of all cities, zip-codes and streets. (House-numbers are looked up
 * in the map directly.)
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:AdvancedAddressDBPlaceFinder.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class AdvancedAddressDBPlaceFinder extends AbstractAddressDB implements IAddressDBPlaceFinder {

    /**
     * All the code for indexing and searching for cities.
     */
    private CitiesIndex myCitiesIndex = new CitiesIndex(this);

    /**
     * All the code for indexing and searching for streets.
     */
    private WaysIndex myStreetsIndex = new WaysIndex(this);

    /**
     * Construct this class and check the database.
     */
    public AdvancedAddressDBPlaceFinder() {
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
     * Index all cities in the given dataset.
     * @param aMap the dataset to index.
     */
    public void indexDatabase(final IDataSet aMap) {
        if (aMap == null) {
            throw new IllegalArgumentException("null map given!");
        }
        if (getMap() == null) {
            setMap(aMap);
        }


        // iterate over all cities
        LOG.log(Level.FINE, "indexing cities in database...");
        Iterator<Way> waysByTag = aMap.getWaysByTag("place", null);
        // cities with a bounding polygon
        if (waysByTag == null) {
            LOG.log(Level.SEVERE, "getWaysByTag(tag='place') returned null!");
        } else
        for (Iterator<Way> cities = waysByTag; cities.hasNext();) {
            Way city = cities.next();
            this.myCitiesIndex.indexWay(city);
        }

        // iterate over all streets
        LOG.log(Level.FINE, "indexing streets in database...");
        waysByTag = aMap.getWaysByTag("highway", null);
        // cities with a bounding polygon
        if (waysByTag == null) {
            LOG.log(Level.SEVERE, "getWaysByTag(tag='highway') returned null!");
        } else
        for (Iterator<Way> streets = waysByTag; streets.hasNext();) {
            Way street = streets.next();
            this.myStreetsIndex.indexWay(street);
        }

         // cities with a central node
        for (Iterator<Node> cities = aMap.getNodesByTag("place", null); cities.hasNext();) {
            Node city = cities.next();
            this.myCitiesIndex.indexNode(city);

        }
        getDatabase().commit();
        LOG.log(Level.FINE, "indexing database...DONE");

        //aMap.getRelationByTag(???, null); -> cities defined as a relation


    }

    /**
     * Index the given node if it is a city.
     * else ignore it.
     * @param city any way. Not null
     */
    public void indexNode(final Node city) {

        // add entry in the database
        String placeType = NodeHelper.getTag(city, "place");


        // is this a city?
        if (placeType != null) {
            LOG.log(Level.FINE, "indexing node- suburb/city: " + NodeHelper.getTag(city, Tags.TAG_NAME));
            this.myCitiesIndex.indexNode(city);
        }
    }

    /**
     * Index the given closed way if it is a city.
     * else ignore it.
     * @param aWay any way. Not null
     */
    public void indexWay(final Way aWay) {

        try {
            // add entry in the database
            String highway = WayHelper.getTag(aWay, Tags.TAG_HIGHWAY);
            String placeType = WayHelper.getTag(aWay, Tags.TAG_PLACE);

            // is this a street?
            if (highway != null) {
                this.myStreetsIndex.indexWay(aWay);
            }

            // is this a city?
            if (placeType != null) {
                this.myCitiesIndex.indexWay(aWay);
            }


            if (highway != null || placeType != null) {
                LOG.log(Level.FINE, "indexing way done");
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error while indexing way into the address-database wayID=" + aWay.getId(), e);
        }
    }


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

            // search the DB for cities with this name and return their suburbs
            retval = this.myCitiesIndex.findSuburbsAndCities(pCity);
            if (pStreet != null && pStreet.length() > 0) {
                Collection<Place> matchingStreets = new LinkedList<Place>();
                Collection<Place> potentialStreets = this.myStreetsIndex.findStreets(pHouseNr, pStreet, Bounds.WORLD);
                for (Place city2 : retval) {
                    CityPlace city = (CityPlace) city2;
                    for (Place place : potentialStreets) {
                        if (city.getBounds().contains(place.getLatitude(), place.getLongitude())) {
                            matchingStreets.add(place);
                        }
                    }
//                    matchingStreets.addAll(this.myStreetsIndex.findStreets(pHouseNr, pStreet, city.getBounds()));
                }

                if (matchingStreets.size() != 0) {
                    // we found such street in a matching city/suburb.
                    // return it
                    return matchingStreets;
                }
            }
            return retval;

        } else { // if no city is given
            return this.myStreetsIndex.findStreets(pHouseNr, pStreet, Bounds.WORLD);
        }
    }

    /**
     * ${@inheritDoc}.
     */
    public Collection<Place> findPlaces(final String pSearchExpression) {
        return findAddress(null, pSearchExpression, null, null, null);
    }







    /**
     * @param street the way to look for
     * @return the name of one suburb this way is in or null
     */
    public String getCityNameForWay(final Way street) {
        String cityName = null;
        try {
            Collection<CityBounds> cities = this.myCitiesIndex.getCitysForWay(street);
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
            Collection<CityBounds> suburbs = this.myCitiesIndex.getSuburbsForWay(street);
            if (suburbs.size() > 0)
                suburbName = suburbs.iterator().next().getName();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while getting suburbs that contain way " + street.getId(), e);
        }
        return suburbName;
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
     * {@inheritDoc}
     */
    /* @Override */ // TODO: Check override!
    public Collection<CityPlace> findPlaces(final Rectangle2D aSearchArea) {
        return this.myCitiesIndex.findCities(aSearchArea);
    }

}
