/**
 * This file is part of LibOSM by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  LibOSM is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LibOSM is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LibOSM.  If not, see <http://www.gnu.org/licenses/>.
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

import org.openstreetmap.osm.data.coordinates.LatLon;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;



/**
 * This class represents a place found via an {@link IPlaceFinder}.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public abstract class Place {

    /**
     * The name that shall be displayed.
     */
    private String myDisplayName;

    /**
     * @return The node to return.
     */
    public abstract Node getResult();

    /**
     * the latitude.
     */
    private double myLatitude;

    /**
     * the longitude.
     */
    private double myLongitude;

    /**
     * @return the display-name.
     */
    @Override
    public String toString() {
        return myDisplayName;
    }

    /**
     * @param location the location of the place
     * @param aDisplayName The name that shall be displayed.
     */
    public Place(final String aDisplayName, final LatLon location) {
        this(aDisplayName, location.lat(), location.lon());
    }
    /**
     * @param lat the latitude
     * @param lon the longitude
     * @param aDisplayName The name that shall be displayed.
     */
    public Place(final String aDisplayName, final double lat, final double lon) {
        super();
        myDisplayName = aDisplayName;
        myLatitude = lat;
        myLongitude = lon;
    }

    /**
     * @return the latitude
     */
    public double getLatitude() {
        return myLatitude;
    }

    /**
     * @return the longitude
     */
    public double getLongitude() {
        return myLongitude;
    }

    /**
     * @param pDisplayName The displayName to set.
     * @see #myDisplayName
     */
    protected void setDisplayName(final String pDisplayName) {
        if (pDisplayName == null) {
            throw new IllegalArgumentException("null 'displayName' given!");
        }

        this.myDisplayName = pDisplayName;
    }

}
