/**
 * AddressPlace.java
 * created: 24.11.2007 17:27:24
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

import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.WayHelper;

import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * AddressPlace.java<br/>
 * created: 18.11.2007 20:53:51 <br/>
 *<br/><br/>
 * An AddressPlace is a special WayPlace that references a street
 * but also knows about the city and zip-code this street is in
 * and house-numbers in this street.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class AddressPlace extends WayPlace {

    /**
     * Our AddressDBPlaceFinder.java.
     * @see AddressPlace
     */
    private final IAddressDBPlaceFinder myAddressFinder;

    /**
     * @param pStreet the resulting street
     * @param aFinder the finder to look up zip-codes and other additional information on the way
     */
    public AddressPlace(final IAddressDBPlaceFinder aFinder, final Way pStreet) {
        this(aFinder, pStreet, aFinder.getCityNameForWay(pStreet), aFinder.getSuburbNameForWay(pStreet));
    }

    /**
     * @param pStreet the resulting street (may not be null)
     * @param pCityName the city the street is in (may be null)
     * @param pSuburbName the name if the suburb in the city (may be null)
     * @param aFinder the finder to look up zip-codes and other additional information on the way
     */
    public AddressPlace(final IAddressDBPlaceFinder aFinder, final Way pStreet, final String pCityName, final String pSuburbName) {
        super(pStreet, aFinder.getMap());
        myAddressFinder = aFinder;
        myStreetName = WayHelper.getTag(pStreet, Tags.TAG_NAME);
        myCityName = pCityName;
        myZipCode = myAddressFinder.getZipCodeNameForWay(pStreet);
        mySubUrbName = pSuburbName;
        myHouseNr = null;

        String name = myStreetName;
        if (myCityName != null)
            name = myCityName + ", " + name;
        if (mySubUrbName != null)
            name = name + "(" + mySubUrbName + ")";
        if (myZipCode != null)
            name = myZipCode + " " + name;
        if (myHouseNr != null)
            name = name + " " + myHouseNr;
        setDisplayName(name);
    }

    /**
     * The city this street is in.
     */
    private String myCityName;
    /**
     * The suburb of the city this street is in.
     */
    private String mySubUrbName;

    /**
     * The name of this street.
     */
    private String myStreetName;

    /**
     * The zip-code this street is in.
     */
    private String myZipCode = null;

    /**
     * The house-number in this street or the range of all house-numbers.
     */
    private String myHouseNr = null;

    /**
     * @return Returns the cityName.
     * @see #myCityName
     */
    public String getCityName() {
        return myCityName;
    }

    /**
     * @return Returns the houseNr.
     * @see #myHouseNr
     */
    public String getHouseNr() {
        return myHouseNr;
    }

    /**
     * @return Returns the streetName.
     * @see #myStreetName
     */
    public String getStreetName() {
        return myStreetName;
    }

    /**
     * @return Returns the subUrbName.
     * @see #mySubUrbName
     */
    public String getSubUrbName() {
        return mySubUrbName;
    }

    /**
     * @return Returns the zipCode.
     * @see #myZipCode
     */
    public String getZipCode() {
        return myZipCode;
    }
}
