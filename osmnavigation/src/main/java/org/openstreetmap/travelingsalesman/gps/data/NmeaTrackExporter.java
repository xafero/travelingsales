/**
 * NmeaTrackExporter.java
 *  created: 21.01.2009
 * (c) 2009 by <a href="mailto:oleg.chubaryov@mail.ru">Oleg Chubaryov</a>
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.travelingsalesman.gps.IGPSProvider.IExtendedGPSListener;


/**
 * Saves listening gps data to .nmea text file.
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
 */
public class NmeaTrackExporter extends Thread implements IExtendedGPSListener {

    /**
     * Number of minutes a degree has.
     */
    private static final int MINUTESPERDEGREE = 60;


    /**
     * The thread-sleep time.
     */
    private static final int TWOHERTZ = 500;


    /**
     * my logger for debug and error-output.
     */
    protected static final Logger LOG = Logger.getLogger(NmeaTrackExporter.class.getName());


    /**
     * Writer buffer.
     */
    private BufferedWriter buffer;

    /**
     * String builder buffer.
     */
    private StringBuffer stringBuffer = new StringBuffer();

    /**
     * Last values.
     */
    private double lastLat, lastLon, lastAltitude, lastCourse, lastSpeed;

    /**
     * Last values.
     */
    private long lastDate, lastTime;

//    /**
//     * We are fixed now ?
//     */
//    private boolean fixed = true;

    /**
     * NMEA mode.
     */
    private enum Mode { GGA, RMC };

    /**
     * NMEA current mode.
     */
    private Mode mode = Mode.GGA;

//    public NmeaTrackExporter(final String filename) {
//        // detect directory for saving
//        // compose filename and open file for writing (logging)
//        this.setDaemon(true);
//        this.start();
//    }

    /**
     * No argument constructor for working as track logger.
     */
    public NmeaTrackExporter() {
        // detect directory for saving
        String dir = System.getProperty("user.home") + File.separator + ".openstreetmap" + File.separator + "track_log" + File.separator;
        File fdir = new File(dir);
        fdir.mkdirs();
        // compose filename and open file for writing (logging)
        // we could write one log-file per day.
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String filename = sdf.format(new Date());
            FileWriter writer = new FileWriter(dir + filename + ".nmea", true);
            buffer = new BufferedWriter(writer);
            this.setPriority(MIN_PRIORITY);
            this.setDaemon(true);
            this.start();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot write NMEA-sentence to log-file", e);
        }
    }

    /**
     * As long as we are not interrupted,
     * work on async-IO -requests.
     */
    public void run() {
        while (!isInterrupted()) {
            try {
                switch (mode) {
                case GGA:
                    if (lastTime > 0 && lastLat != 0 && lastLon != 0) {
                        stringBuffer.append("$GPGGA,").append(time(lastTime))
                                .append(',').append(lat(lastLat)).append(',')
                                .append(lon(lastLon)).append(',')
                                .append("1,,,").append(alt(lastAltitude))
                                .append(",,,,");
                        stringBuffer.append(checksum(stringBuffer.toString()));
                        mode = Mode.RMC;
                    }
                    break;
                case RMC:
                    if (lastDate > 0 && lastTime > 0 && lastLat != 0
                            && lastLon != 0 && lastSpeed != 0
                            && lastCourse != 0) {
                        stringBuffer.append("$GPRMC,").append(time(lastTime))
                                .append(",A,").append(lat(lastLat)).append(',')
                                .append(lon(lastLon)).append(',')
                                .append(String.format("%.1f", lastSpeed)
                                                .replace(',', '.')).append(',')
                                .append(String.format("%.1f", lastCourse)
                                                .replace(',', '.')).append(',')
                                .append(date(lastDate)).append(",,");
                        stringBuffer.append(checksum(stringBuffer.toString()));
                        mode = Mode.GGA;
                    }
                    break;
                    default:
                        break;
                }
                if (stringBuffer.length() > 0) {
                    buffer.write(stringBuffer.toString());
                    buffer.newLine();
                    buffer.flush();
                    stringBuffer.setLength(0);
                }
                Thread.sleep(TWOHERTZ);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // close all files
        try {
            buffer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates the checksum for a sentence.
     * @param sentence the sentence
     * @return asteriks string with calculated checksum
     */
    private static String checksum(final String sentence) {
        byte check = 0;
        // skip first '$' character
        for (int c = 1; c < sentence.length(); c++) {
          check = (byte) (check ^ sentence.charAt(c));
        }
        return "*" + String.format("%02x", check).toUpperCase();
    }

    /**
     * format an altitude.
     * @param anAltitude the altitude-value
     * @return the formated value
     */
    private String alt(final double anAltitude) {
        return String.format("%.1f", anAltitude).replace(',', '.') + ",M";
    }

    /**
     * Format a latitude.
     * @param lat the latitude to format
     * @return the formated latitude
     */
    private String lat(final double lat) {
        int degrees = Math.abs((int) (lat));
        float minutes = (float) ((Math.abs(lat) - degrees) * MINUTESPERDEGREE);
        String northSouth = ",S";
        if (lat > 0) {
            northSouth = ",N";
        }
        return String.format("%02d", degrees) + String.format("%02.4f", minutes).replace(',', '.') + northSouth;
    }

    /**
     * Format a longitude.
     * @param lon the longitude to format
     * @return the formated longitude
     */
    private String lon(final double lon) {
        int degrees = Math.abs((int) (lon));
        float minutes = (float) ((Math.abs(lon) - degrees) * MINUTESPERDEGREE);
        String eashWest = ",W";
        if (lon > 0) {
            eashWest = ",E";
        }
        return String.format("%03d", degrees) + String.format("%02.4f", minutes).replace(',', '.') + eashWest;
    }

    /**
     * Format a date.
     * @param aDate the date to format
     * @return the formated date
     */
    private String date(final long aDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy");
        //sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
        return sdf.format(new Date(aDate));
    }

    /**
     * Format a time.
     * @param aTime the time to format
     * @return the formated time
     */
    private String time(final long aTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
        sdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
        return sdf.format(new Date(aTime));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsDateTimeChanged(final long date, final long time) {
        lastTime = time;
        if (date > 0) {
            lastDate = date;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsLocationChanged(final double lat, final double lon) {
        lastLat = lat;
        lastLon = lon;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsAltitudeChanged(final double altitude) {
        lastAltitude = altitude;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsSpeedChanged(final double speed) {
        lastSpeed = speed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsCourseChanged(final double course) {
        lastCourse = course;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsDopChanged(final double hdop, final double vdop, final double pdop) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsFixQualityChanged(final int fixQuality) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsUsedSattelitesChanged(final int satellites) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsLocationLost() {
//        fixed = false;
        this.suspend();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsLocationObtained() {
//        fixed = true;
        this.resume();
    }

}
