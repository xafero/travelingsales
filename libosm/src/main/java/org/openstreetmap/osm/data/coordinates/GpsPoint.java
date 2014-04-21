/**
 * This file is part of LibOSM.
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
package org.openstreetmap.osm.data.coordinates;

import java.text.ParseException;
import java.util.Date;

/**
 * This class represents a single gps-reading.
 * GPS-Readings are not part of the map (see {@link com.bretth.osmosis.core.domain.v0_5.Node} but
 * are raw-data.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 *
 */
public class GpsPoint {

    /**
     * The actual coordinates.
     */
    private final LatLon latlon;

    /**
     * The timestamp of the last modification/creation.
     */
    //private final Date time;

    /**
     * @param ll latitude and longtitude (The actual coordinates)
     * @param timestamp The timestamp of the last modification/creation.
     * @throws ParseException if the timestamp has a wrong format.
     */
    public GpsPoint(final LatLon ll, final String timestamp) throws ParseException {
        latlon = ll;
     //   this.time = Settings.TIMESTAMPFORMAT.parse(timestamp);
    }

    /**
     * @param ll latitude and longtitude (The actual coordinates)
     * @param timestamp The timestamp of the last modification/creation.
     * @throws ParseException if the timestamp has a wrong format.
     */
    public GpsPoint(final LatLon ll, final Date timestamp) throws ParseException {
        latlon = ll;
       // this.time = timestamp;
    }

    /**
     * @return The actual coordinates
     */
    public LatLon getLatlon() {
        return latlon;
    }

    /**
     * The coordinates eastined by the current projection.
     */
//    public final EastNorth getEastNorth() {
//        return Settings.getProjection().latlon2eastNorth(getLatlon().lat(), getLatlon().lon());
//    }
}
