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

import org.openstreetmap.osm.data.coordinates.PolygonBounds;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;

/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * AddressDBPlaceFinder.java<br/>
 * created: 08.03.2008 17:50:03 <br/>
 *<br/><br/>
 * An CityPlace is a special Place that references a city,
 * suburb, zip-code-region or other named bounds.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class CityPlace extends Place {

    /**
     * Our AddressDBPlaceFinder.java.
     * @see CityPlace
     */
    private final IAddressDBPlaceFinder myAddressFinder;

    /**
     * The region we represent.
     */
    private PolygonBounds myRegion;


    /**
     * @param pRegion the resulting region
     * @param aFinder the finder to look up zip-codes and other additional information on the way
     */
    public CityPlace(final IAddressDBPlaceFinder aFinder, final CityBounds pRegion) {
        super(pRegion.getName(), pRegion.center());
        this.myAddressFinder = aFinder;
        this.myRegion = pRegion;
        this.myCityName = pRegion.getName();
        this.mySubUrbName = null;
    }

    /**
     * @param pRegion the resulting region
     * @param pCity the city containing the region
     * @param aFinder the finder to look up zip-codes and other additional information on the way
     */
    public CityPlace(final IAddressDBPlaceFinder aFinder, final CityBounds pCity, final CityBounds pRegion) {
        super(pCity.getName() + " - " + pRegion.getName(), pRegion.center());
        this.myAddressFinder = aFinder;
        this.myRegion = pRegion;
        this.myCityName = pCity.getName();
        this.mySubUrbName = pRegion.getName();
    }

    /**
     * @return Returns the region.
     * @see #myRegion
     */
    public PolygonBounds getBounds() {
        return myRegion;
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
     * The zip-code this street is in.
     */
    private String myZipCode = null;


    /**
     * @return Returns the cityName.
     * @see #myCityName
     */
    public String getCityName() {
        return myCityName;
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

    /**
     * ${@inheritDoc}.
     */
    @Override
    public Node getResult() {
        // TODO use a selector
        return myAddressFinder.getMap().getNearestNode(myRegion.center(), null);
    }
}
