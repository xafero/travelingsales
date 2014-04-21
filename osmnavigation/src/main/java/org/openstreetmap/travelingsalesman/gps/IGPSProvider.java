/**
 * IGPSProvider.java
 * (c) 2007 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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
package org.openstreetmap.travelingsalesman.gps;

import org.openstreetmap.osm.Plugins.IPlugin;

/**
 * This interface is implemented by plugins that can
 * provide the current position of the user('s vehicle).
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public interface IGPSProvider extends IPlugin {

    /**
     * Interface for listeners to get informed about
     * changes of the location of the user.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    public interface IGPSListener {

        /**
         * The location of the user has changed.
         * @param lat the latitude
         * @param lon the longitude
         */
        void gpsLocationChanged(final double lat, final double lon);

        /**
         * We have no location-fix anymore.
         */
        void gpsLocationLost();

        /**
         * We are have location-fix.
         */
        void gpsLocationObtained();

        /**
         * GPS course over ground has changed.
         * If course can not derive from gps (NMEA) data directly,
         * it should be derived from difference latitude and longitude.
         *
         * @param course Track angle in degrees
         */
        void gpsCourseChanged(final double course);
    }

    /**
     * Interface for listeners to get informed about
     * changes of the location and other GPS data, parsed from various gps streams.
     * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
     */
    public interface IExtendedGPSListener extends IGPSListener {

        /**
         * GPS date and time has changed.
         * @param date the date
         * @param time the time
         */
        void gpsDateTimeChanged(final long date, final long time);

        /**
         * GPS fix quality has changed.
         * @param fixQuality 0 - invalid, 1 - GPS fix, 2 - DGPS fix.
         */
        void gpsFixQualityChanged(final int fixQuality);

        /**
         * GPS amount of tracked satellites changed.
         * @param satellites new amount of used / tracked satellites.
         */
        void gpsUsedSattelitesChanged(final int satellites);

        /**
         * GPS altitude has changed.
         * @param altitude new altitude in meters.
         */
        void gpsAltitudeChanged(final double altitude);

        /**
         * GPS Dilution of precision has changed.
         * @param hdop new horizontal dilution of precision
         * @param vdop new vertical dilution of precision
         * @param pdop new position dilution of precision
         */
        void gpsDopChanged(final double hdop, final double vdop, final double pdop);

        /**
         * GPS Speed over ground has changed.
         * @param speed new speed value in knots.
         */
        void gpsSpeedChanged(final double speed);

    }

    /**
     * Add a listener to get informed about
     * changes of the location of the user.
     * @param listener the observer
     */
    void addGPSListener(final IGPSListener listener);

    /**
     * Remove a listener from informed about changes.
     * @param listener the observer to remove
     */
    void removeGPSListener(final IGPSListener listener);
}
