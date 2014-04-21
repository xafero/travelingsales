/**
 * NMEAParsingGPSProvider.java
 * created: 18.11.2007 15:20:49
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;

import org.openstreetmap.osm.data.coordinates.LatLon;

/**
 * (c) 2007-2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: osmnavigation<br/>
 * NMEAParsingGPSProvider.java<br/>
 * created: 18.11.2007 15:20:49 <br/>
 * Base-class for all {@link IGPSProvider} that parse NMEA-data.
 * Some code was acquired from JGPS project by Tomas Darmovzal (darmovzalt)
 * http://sourceforge.net/projects/jgps/
 *
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>,
 * <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
 */
public abstract class NMEAParsingGPSProvider extends BaseGpsProvider implements IGPSProvider {

    /**
     * Unparsed last values for needing to notify listeners.
     * for GGA NMEA sentence.
     */
    private String lastTimeGGA = "";

    /**
     * Unparsed last values for needing to notify listeners.
     * for GGA NMEA sentence.
     */
    private String lastTimeRMC = "", lastDateRMC = "";

    /**
     * Unparsed last values for needing to notify listeners.
     * common for all NMEA sentences.
     */
    private String lastLatitude = "", lastLongitude = "",
            lastFixQuality = "", lastAltitude = "", lastUsedSatellites = "", lastHDOP = "",
            lastSpeed = "", lastCourse = "";

    /**
     * we are fixed ?
     */
    private boolean fixed = false;

    /**
     * We have speed and cource in NMEA sentences.
     */
    private boolean hasSpeed = false, hasCourse = false;

    /**
     * Temporary variables for derived speed calculation.
     */
    private byte fixCount = 0;

    /**
     * Lat/Lon for derived speed calculation.
     */
    private double speedLat, speedLon, prevSpeed;

    /**
     * Time for derived speed calculation.
     */
    private long prevTime;

    /**
     *
     */
    public NMEAParsingGPSProvider() {
        super();
    }

    /**
     * How many minutes does a hour have (for coordinates).
     */
    protected static final float MINUTESPERHOUR = 60.0F;

    /**
     * 1000.
     */
    private static final int MILLI = 1000;

    /**
     * Just an overridden ToString to return this classe's name and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "NMEAParsingGPSProvider@" + hashCode();
    }

    /**
     * Parse an nmea-line and calls:
     *  {@link #sendLocationChanged(double, double)}
     *  {@link #sendDateTimeChanged(long, long)}
     * if needed.
     *
     * @param aLine the line of the NMEA-protocol.
     *
     */
   protected void parseNMEA(final String aLine) {
        try {
            // http://www.kh-gps.de/nmea-faq.htm
            String[] elements = aLine.split(",");
            // $GPGGA,053226,5331.6281,N,11331.8071,W,2,10,0.80,708.4,M,-19.783,M,,*4E
            if (elements[0].equals("$GPGGA") || elements[0].equals("GPGGA")) {
                parseGGA(elements);
            }
            // recommend minimum sentence
            //$GPRMC,053226,A,5331.6281,N,11331.8071,W,0.2360,70.702,181007,,*03
            if (elements[0].equals("$GPRMC") || elements[0].equals("GPRMC")) {
                parseRMC(elements);
            }
        } catch (Exception e) {
            // ignored
            LOG.log(Level.FINEST, "Ignoring eror in parseNMEA(" + aLine + ")", e);
        }

    }

    /**
     * Parse RMC (recommended minimum) sentence.
     * @param elements NMEA elements of parsed sentence.
     */
    private void parseRMC(final String[] elements) {
        final int timeIndex = 1;
        final int fixStateIndex = 2;
        final int latIndex = 3;
        final int latHemisphereIndex = 4;
        final int lonIndex = 5;
        final int lonHemisphereIndex = 6;
        final int speedIndex = 7;
        final int courseIndex = 8;
        final int dateIndex = 9;
        // date and time parsing
        if (!lastTimeRMC.equals(elements[timeIndex]) || !lastDateRMC.equals(elements[dateIndex])) {
            long date = parseDate(elements[dateIndex]);
            long time = parseTime(elements[timeIndex]);
            sendDateTimeChanged(date, time);
            lastDateRMC = elements[dateIndex];
            lastTimeRMC = elements[timeIndex];
        }
        // lat/lon parsing
        //if (!lastLatitude.equals(elements[latIndex]) || !lastLongitude.equals(elements[lonIndex])) {
            double lat = parseLatitude(elements[latIndex], elements[latHemisphereIndex]);
            double lon = parseLongitude(elements[lonIndex], elements[lonHemisphereIndex]);
            sendLocationChanged(lat, lon);
            lastLatitude = elements[latIndex];
            lastLongitude = elements[lonIndex];
        //}
        // validity parsing
        boolean aFixed = parseFixed(elements[fixStateIndex]);
        if (fixed != aFixed) {
            if (aFixed) {
                sendLocationObtained();
            } else {
                sendLocationLost();
            }
            fixed = aFixed;
        }
        // speed parsing
        String strSpeed = elements[speedIndex];
        if (!lastSpeed.equals(strSpeed)) {
            double speed = parseDouble(strSpeed);
            sendSpeedChanged(speed);
            lastSpeed = strSpeed;
            if (!hasSpeed)
                hasSpeed = true;
        }
        // course parsing
        String strCourse = elements[courseIndex];
        if (!lastCourse.equals(strCourse)) {
            double course = parseDouble(strCourse);
            sendCourseChanged(course);
            lastCourse = strCourse;
            if (!hasCourse)
                hasCourse = true;
        }
    }

    /**
     * Parse GGA sentence.
     * @param elements NMEA elements of parsed sentence.
     */
    private void parseGGA(final String[] elements) {
        final int timeIndex = 1;
        final int latIndex = 2;
        final int latHemisphereIndex = 3;
        final int lonIndex = 4;
        final int lonHemisphereIndex = 5;
        final int fixQualityIndex = 6;
        final int usedSatellitesIndex = 7;
        final int hdopIndex = 8;
        final int altitudeIndex = 9;
        final int altitudeUnitsIndex = 10;
        // parse time
        long time = 0;
        String strTime = elements[timeIndex];
        if (!lastTimeGGA.equals(strTime)) {
            time = parseTime(strTime);
            if (lastDateRMC.equals("")) {
                sendDateTimeChanged(0, time);
            } else {
                sendDateTimeChanged(parseDate(lastDateRMC), time);
            }
            lastTimeGGA = strTime;
        }
        // parse lat, lon
        String strLatitude = elements[latIndex];
        String strLongitude = elements[lonIndex];
        if (lastLatitude.equals("") || lastLongitude.equals("")) {
            lastLatitude = strLatitude;
            lastLongitude = strLongitude;
        }
        if (!lastLatitude.equals(strLatitude) || !lastLongitude.equals(strLongitude)) {
            double lat1 = parseLatitude(strLatitude, elements[latHemisphereIndex]);
            double lon1 = parseLongitude(strLongitude, elements[lonHemisphereIndex]);
            sendLocationChanged(lat1, lon1);
            // check if course changed, send changed course notification
            if (!hasCourse) {
                double lat2 = parseLatitude(lastLatitude, elements[latHemisphereIndex]);
                double lon2 = parseLongitude(lastLongitude, elements[lonHemisphereIndex]);
                double course = LatLon.deriveCourse(lat1, lon1, lat2, lon2);
                if (lastCourse.equals("") || (parseDouble(lastCourse) != course)) {
                    sendCourseChanged(course);
                    lastCourse = Double.toString(course);
                }
            }
            if (!hasSpeed && time > 0) {
                // calculate speed if source havn't it.
                // due to big error we recalc it every 4 fixes.
                // TODO add derived speed calculation with vertical moving.
                final int requiredFixed = 3;
                if (++fixCount > requiredFixed) {
                    final double factor = 1943.84;
                    int dDist = (int) Math.round(LatLon.distanceInMeters(new LatLon(lat1, lon1), new LatLon(speedLat, speedLon)) * factor); // in meters
                    double speed = dDist / (time - prevTime); // in knots
                    speedLat = lat1;
                    speedLon = lon1;
                    prevTime = time;
                    fixCount = 0;
                    if (Math.abs(prevSpeed - speed) > 1) {
                        sendSpeedChanged(speed);
                        prevSpeed = speed;
                    }
                }
            }
            lastLatitude = strLatitude;
            lastLongitude = strLongitude;
        }
        // parse fix quality
        String strFixQuality = elements[fixQualityIndex];
        if (!lastFixQuality.equals(strFixQuality)) {
            int fixQuality = parseInt(strFixQuality);
            sendFixQualityChanged(fixQuality);
            lastFixQuality = strFixQuality;
        }
        // parse fixed?
        boolean aFixed = parseFixed(strFixQuality);
        if (fixed != aFixed) {
            if (aFixed) {
                sendLocationObtained();
            } else {
                sendLocationLost();
            }
            fixed = aFixed;
        }
        // parse amount of used satellites
        String strUsedSatellites = elements[usedSatellitesIndex];
        if (!lastUsedSatellites.equals(strUsedSatellites)) {
            int usedSatellites = parseInt(strUsedSatellites);
            sendUsedSatellitesChanged(usedSatellites);
            lastUsedSatellites = strUsedSatellites;
        }
        // parse hdop
        String strHdop = elements[hdopIndex];
        if (!lastHDOP.equals(strHdop)) {
            double hdop = parseDouble(strHdop);
            sendDopChanged(hdop, 0d, 0d);
            lastHDOP = strHdop;
        }
        // parse altitude
        String strAltitude = elements[altitudeIndex];
        String strAltitudeUnits = elements[altitudeUnitsIndex];
        if (!lastAltitude.equals(strAltitude) && strAltitudeUnits.equalsIgnoreCase("M")) {
            double altitude = parseDouble(strAltitude);
            sendAltitudeChanged(altitude);
            lastAltitude = strAltitude;
        }
        // http://www.kh-gps.de/nmea-faq.htm
    }


   /**
    * Parse GPS on-fix state, from GGA and RMC sentences.
    * @param strFixed string representation
    * @return on-fix? state
    */
    private boolean parseFixed(final String strFixed) {
        if (strFixed.equals("1") || strFixed.equals("2") || strFixed.equals("A")) {
            return true;
        } else if (strFixed.equals("0") || strFixed.equals("V")) {
            return false;
        }
        return false;
    }

    /**
     * Convert between GPS-string-representation and double.
     * @param aLatitude "dd.ddddd"
     * @param aHemisphere "N" or "S"
     * @return the value as a signed double.
     */
    private double parseLatitude(final String aLatitude, final String aHemisphere) {
        final int commaIndex = 2;
        double degrees = Double.parseDouble(aLatitude.substring(0, commaIndex));
        double minutes = Double.parseDouble(aLatitude.substring(commaIndex, aLatitude.length()));
        double result = degrees + minutes / MINUTESPERHOUR;
        if (aHemisphere.equals("N"))
            return  result;
        return -result;
    }

    /**
     * Convert between GPS-string-representation and double.
     * @param aLongitude "ddd.dddd"
     * @param aHemisphere "E" or "W"
     * @return the value as a signed double.
     */
    private double parseLongitude(final String aLongitude, final String aHemisphere) {
        final int commaIndex = 3;
        double degrees = Double.parseDouble(aLongitude.substring(0, commaIndex));
        double minutes = Double.parseDouble(aLongitude.substring(commaIndex, aLongitude.length()));
        double result = degrees + minutes / MINUTESPERHOUR;
        if (aHemisphere.equals("E"))
            return  result;
        return -result;

    }

    /**
     * Convert between GPS-string representation and long.
     * @param aTimeStr "hhmmss" in UTC
     * @return time in UTC in milliseconds.
     */
    private long parseTime(final String aTimeStr) {
        final int hourChars = 2;
        final int minuteChars = 2;
        int hour = Integer.parseInt(aTimeStr.substring(0, hourChars));
        int min = Integer.parseInt(aTimeStr.substring(hourChars, hourChars + minuteChars));
        int ms = (int) (Float.parseFloat(aTimeStr.substring(hourChars + minuteChars)) * MILLI);
        return (long) (((hour * MINUTESPERHOUR) + min) * MINUTESPERHOUR * MILLI) + ms;
    }

    /**
     * Convert between GPS-string representation and long.
     *
     * @param aDateStr
     *            date in "ddMMyy" format in UTC
     * @return date in milliseconds in UTC.
     */
    private long parseDate(final String aDateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
        try {
            return (sdf.parse(aDateStr)).getTime();
        } catch (ParseException e) {
            LOG.log(Level.WARNING, "Error in NMEA date parsing : " + aDateStr);
        }
        return 0;
    }

    /**
     * Convert string representation of gps to int
     * 1. fix quality value.
     *      0 - invalid fix; 1 - GPS fix; 2 - DGPS fix;
     * 2. Amount of used/tracked satellites.
     * @param strInt string value
     * @return int parsed value
     */
    private int parseInt(final String strInt) {
        return Integer.decode(strInt);
    }

    /**
     * Convert string representation of gps to double
     * 1. horizontal, vertical, position dilution of precision
     * 2. altitude value
     * @param strDouble string value
     * @return double parsed value
     */
    private double parseDouble(final String strDouble) {
        return Double.parseDouble(strDouble);
    }
}
