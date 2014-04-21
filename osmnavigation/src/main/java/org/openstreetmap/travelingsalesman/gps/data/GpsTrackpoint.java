/**
 * GpsTrackpoint.java
 * (c) 2009
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
 */
package org.openstreetmap.travelingsalesman.gps.data;

import java.text.ParseException;
import java.util.Date;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.coordinates.LatLon;

/**
 * This class represents a single gps-trackpoint. Immutable.
 *
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
 */
public class GpsTrackpoint {

    /**
     * The actual coordinates.
     */
    private final LatLon latlon;

    /**
     * The timestamp of the last modification/creation.
     */
    private final Date time;

    /**
     * The altitude in meters over sea.
     */
    private final double altitude;

    /**
     * The speed in nautical miles per hour.
     */
    private final double speed;

    /**
     * The course over ground, in degrees. 0 is to north.
     */
    private final double course;

    /**
     * @param ll
     *            latitude and longtitude (The actual coordinates)
     * @param timestamp
     *            The timestamp of the last modification/creation.
     * @throws ParseException
     *             if the timestamp has a wrong format.
     */
    public GpsTrackpoint(final LatLon ll, final String timestamp)
            throws ParseException {
        this.latlon = ll;
        this.time = Settings.TIMESTAMPFORMAT.parse(timestamp);
        this.altitude = 0;
        this.speed = 0;
        this.course = 0;
    }

    /**
     * @param ll
     *            latitude and longtitude (The actual coordinates)
     * @param timestamp
     *            The timestamp of the last modification/creation.
     */
    public GpsTrackpoint(final LatLon ll, final Date timestamp) {
        this.latlon = ll;
        this.time = timestamp;
        this.altitude = 0;
        this.speed = 0;
        this.course = 0;
    }

    /**
     * @param ll
     *            latitude and longtitude (The actual coordinates)
     * @param timestamp
     *            The timestamp of the last modification/creation.
     * @param anAltitude
     *            The altitude.
     */
    public GpsTrackpoint(final LatLon ll, final Date timestamp,
            final double anAltitude) {
        this.latlon = ll;
        this.time = timestamp;
        this.altitude = anAltitude;
        this.speed = 0;
        this.course = 0;
    }

    /**
     * @param aLatLon the location of the trackpoint
     * @param aTime the time
     * @param anAltitude geometric altitude
     * @param aSpeed speed
     * @param aCourse direction of movement
     */
    public GpsTrackpoint(final LatLon aLatLon, final Date aTime, final double anAltitude,
            final double aSpeed, final double aCourse) {
        this.latlon = aLatLon;
        this.time = aTime;
        this.altitude = anAltitude;
        this.speed = aSpeed;
        this.course = aCourse;
    }

    /**
     * @return The actual coordinates
     */
    public LatLon getLatlon() {
        return latlon;
    }

    /**
     *
     * @return The actual timestamp of point.
     */
    public Date getTime() {
        return time;
    }

    /**
     * @return the altitude
     */
    public double getAltitude() {
        return altitude;
    }

    /**
     * @return the speed
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * @return the course
     */
    public double getCourse() {
        return course;
    }

    /**
     * The coordinates eastined by the current projection.
     */
    // public final EastNorth getEastNorth() {
    // return Settings.getProjection().latlon2eastNorth(getLatlon().lat(),
    // getLatlon().lon());
    // }
}
