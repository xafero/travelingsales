/**
 * GPXFileProvider.java
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

import java.io.File;
import java.io.FileInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Stack;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.ConfigurationSetting;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.ConfigurationSetting.TYPES;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.travelingsalesman.navigation.OsmNavigationConfigSection;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * IGPSProvider that reads a local file.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class GPXFileProvider extends BaseGpsProvider implements IGPSProvider, IPlugin {

    /**
     * How many ms we sleep between the nmea-lines.<br/>
     * Set to 1000ms because almost all GPS-receivers
     * give location-fixes at a 1Hz-intervall.
     */
    private int sleepBetweenFixes = MILLI;


    /**
     * How many minutes does a hour have (for coordinates).
     */
    protected static final int MINUTESPERHOUR = 60;

    /**
     * 1000.
     */
    public static final int MILLI = 1000;

    /**
     * How many ms we sleep between gpx-lines.
     * @param milliseconds pause in ms
     */
    public void setSleepBetweenFixes(final int milliseconds) {
        this.sleepBetweenFixes = milliseconds;
    }

    /**
     * {@inheritDoc}
     */
    public ConfigurationSection getSettings() {
        ConfigurationSection retval = new ConfigurationSection("GPXFileProvider");
        retval.addSetting(new ConfigurationSetting("GPXFileProvider.filename",
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.GPXFileProvider_filename.title"),
                TYPES.FILE,
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.GPXFileProvider_filename.category"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.GPXFileProvider_filename.desc")));

        return retval;
    }

    /**
     * Helper-class to parse the XML using SAX.
     *  @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private final class MyContentHandler extends DefaultHandler {

        /**
         * Buffer for XML elements values.
         */
        private StringBuffer buffer = new StringBuffer();

        /**
         * Context XML path for current element value.
         */
        private Stack<String> context = new Stack<String>();

        /**
         * Temporary variables for parsing.
         */
        private double lat, lon, altitude, course, speed;

        /**
         * Temporary variables for parsing.
         */
        private long date, time;

        /**
         * Flags presents source .gpx file have an speed and cog attributes.
         * Else its calculated.
         */
        private boolean hasSpeed, hasCourse;

        /**
         * Previous values needs to calculate speed and course.
         */
        private long prevTime;

        /**
         * Previous values needs to calculate speed and course.
         */
        private double prevLat, prevLon;

        /**
         * Previous values needs to calculate speed and course.
         */
        private double speedLat, speedLon, prevSpeed, prevCourse;

        /**
         * Fix counter for average speed calculation.
         */
        private int fixCount;

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void startElement(final String namespaceURI,
                                 final String localName,   // local name
                                 final String qName,       // qualified name
                                 final Attributes attrs) throws SAXException {
            try {
                LOG.log(Level.FINER, "parsing gpx-log-file: <" + qName + ">");
                this.buffer.setLength(0);
                this.context.push(qName);
                // tracks delimiter for various segment
                if (qName.equals("trkseg")) {
//                    System.out.println("seg-start");
                    sendLocationObtained();
                }
                if (qName.equals("trkpt")) {
                    lat = Double.parseDouble(attrs.getValue("lat"));
                    lon = Double.parseDouble(attrs.getValue("lon"));
                    LOG.log(Level.FINER, "parsing gpx-log-file: Trackpoint lat="
                            + lat + " lon=" + lon);
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Exception while reading GPX-log. skipping.", e);
            }
        }

        /**
         * Parse trackpoint elements.
         * <ele></ele>
         * <time></time>
         * ...
         * @param elementPath the full qualified name for given element (e.g. "/gpx/trk/trkseg/trkpt/time")
         * @param elementValue string value for given element (e.g. "2009-01-01T14:27:53Z")
         */
        private void parseTrackpointElement(final String elementPath, final String elementValue) {
            LOG.log(Level.FINER, "parsing gpx-log-file: element path = " + elementPath + " value = " + elementValue);
            if (elementPath.equals("/gpx/trk/trkseg/trkpt/time")) {
                final int dateEnd = 10;
                final int timeEnd = 19;
                date = parseDate(elementValue.substring(0, dateEnd));
                time = parseTime(elementValue.substring(dateEnd + 1, timeEnd));
                sendDateTimeChanged(date, time);
            }
            if (elementPath.equals("/gpx/trk/trkseg/trkpt/ele")) {
                altitude = parseDouble(elementValue);
                sendAltitudeChanged(altitude);
            }
            if (elementPath.equals("/gpx/trk/trkseg/trkpt/speed")) {
                if (!hasSpeed) {
                    hasSpeed = true;
                }
                speed = parseDouble(elementValue);
                final double speedFactor = 1.944d;
                sendSpeedChanged(speed * speedFactor);
            }
            if (elementPath.equals("/gpx/trk/trkseg/trkpt/cog") || elementPath.equals("/gpx/trk/trkseg/trkpt/course")) {
                if (!hasCourse) {
                    hasCourse = true;
                }
                course = parseDouble(elementValue);
                sendCourseChanged(course);
            }
        }

        /**
         * Convert between GPX-string date and time representation and long.
         * @param aDateStr "yyyy-MM-ddThh:mm:ssZ"
         * @return time in UTC in milliseconds.
         */
        private long parseDate(final String aDateStr) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            try {
                return (sdf.parse(aDateStr)).getTime();
            } catch (ParseException e) {
                LOG.log(Level.WARNING, "Error in GPX date parsing : " + aDateStr);
            }
            return 0;
        }

        /**
         * Convert between GPX-string date and time representation and long.
         * @param aTimeStr "yyyy-MM-ddThh:mm:ssZ"
         * @return time in UTC in milliseconds.
         */
        private long parseTime(final String aTimeStr) {
            final int hourChars = 2;
            final int minuteChars = 2;
            int hour = Integer.parseInt(aTimeStr.substring(0, hourChars));
            int min = Integer.parseInt(aTimeStr.substring(hourChars + 1, hourChars + minuteChars + 1));
            int ms = (int) (Float.parseFloat(aTimeStr.substring(hourChars + minuteChars + 2, hourChars + minuteChars + 2 + 2)) * MILLI);
            return (long) (((hour * MINUTESPERHOUR) + min) * MINUTESPERHOUR * MILLI) + ms;
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

        /**
         * Invokes at the end of each xml element.
         * @param uri the namespace
         * @param local the local name of the element
         * @param qName the name of the element
         * @throws SAXException if we cannot parse
         */
        public void endElement(final String uri, final String local, final String qName)
                throws SAXException {
            this.flushText();
            this.context.pop();
            try {
                // tracks delimiter for various segment
                if (qName.equals("trkseg")) {
//                    System.out.println("seg-end");
                    sendLocationLost();
                }
                // end of one trackpoint
                if (qName.equals("trkpt")) {
                    if (!hasSpeed) {
                        // calculate speed if source gpx havn't it.
                        // due to big error we recalc it every 4 fixes.
                        // TODO add derived speed calculation with vertical moving.
                        final int minFixCount = 3;
                        if (++fixCount > minFixCount) {
                            final double factor = 1943.84; //TODO: move this to UnitConstants
                            double distance = LatLon.distanceInMeters(new LatLon(lat, lon), new LatLon(speedLat, speedLon));
                            int dDist = (int) Math.round(distance * factor); // in meters
                            speed = dDist / (time - prevTime); // in knots (mi per hour)
                            speedLat = lat;
                            speedLon = lon;
                            prevTime = time;
                            fixCount = 0;
                            if (Math.abs(prevSpeed - speed) > 1) {
                                sendSpeedChanged(speed);
                                prevSpeed = speed;
                            }
                        }
                    }
                    if (!hasCourse) {
                        course = LatLon.deriveCourse(lat, lon, prevLat, prevLon);
                        prevLat = lat;
                        prevLon = lon;
                        if (Math.abs(prevCourse - course) > 1) {
                            sendCourseChanged(course);
                            prevCourse = course;
                        }
                    }
                    sendLocationChanged(lat, lon);
                    Thread.sleep(sleepBetweenFixes);
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE,
                        "Exception while reading GPX-log. skipping.", e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void characters(final char[] ch, final int start, final int length)
                throws SAXException {
            this.buffer.append(ch, start, length);
        }

        /**
         * Clear XML buffer and execute gpx elements value parser.
         */
        protected void flushText() {
            if (this.buffer.length() > 0) {
                StringBuffer path = new StringBuffer();
                for (String pathElement : this.context) {
                    path.append("/" + pathElement);
                }
                parseTrackpointElement(path.toString(), this.buffer.toString());
            }
            this.buffer.setLength(0);
         }


        /**
         * {@inheritDoc}
         */
        public void error(final SAXParseException aE) throws SAXException {
            LOG.log(Level.SEVERE, "Error while reading GPX-log. skipping.", aE);
            super.error(aE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void fatalError(final SAXParseException aE) throws SAXException {
            LOG.log(Level.SEVERE, "Fatal error while reading GPX-log. skipping.", aE);
            super.fatalError(aE);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void warning(final SAXParseException aE) throws SAXException {
            LOG.log(Level.WARNING, "Warning while reading GPX-log. skipping.", aE);
            super.warning(aE);
        }
    }

    /**
     * Parse given file at full speed.
     * @param file file for parsing
     */
    public GPXFileProvider(final File file) {
        this.setSleepBetweenFixes(0);
        parseFile(file);
    }

    /**
     * Create a new GPXFileProvider.
     * Start reading after the first listener
     * has been added.
     */
    public GPXFileProvider() {
        this.setSleepBetweenFixes(sleepBetweenFixes);
        File file = new File(Settings.getInstance().get("GPXFileProvider.filename", "/tmp/test.gpx"));
        parseFile(file);
    }

    /**
     * Parse GPX file in separated thread.
     * @param file GPX-file.
     */
    private void parseFile(final File file) {
        Runnable run = new Runnable() {
            public void run() {
                try {
                    LOG.log(Level.INFO, "starting to parse gpx-log-file '"
                            + file.getAbsolutePath() + "' ("
                            + file.length() + " bytes)");
                    if (!file.exists()) {
                        LOG.log(Level.SEVERE, "gpx-log-file '"
                                + file.getAbsolutePath() + "' does not exist.");
                        return;
                    }
                    SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
                    DefaultHandler handler = new MyContentHandler();
                    if (file.getName().toLowerCase().endsWith(".gz")) {
                        saxParser.parse(new GZIPInputStream(new FileInputStream(file)), handler);
                    } else {
                        saxParser.parse(file, handler);
                    }
                    LOG.log(Level.INFO, "DONE parsing gpx-log-file");
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error opening/parsing gpx-log-file", e);
                }
            }
        };
        myReaderThread = new Thread(run);
    }

    /**
     * Our thread for asynchronous reading.
     */
    private Thread myReaderThread;

    /**
     * Has {@link #myReaderThread} been started yet?
     */
    private boolean myReaderThreadStarted = false;

    /**
     * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider#addGPSListener(org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener)
     * @param aListener the new listener to add.
     */
    public void addGPSListener(final IGPSListener aListener) {
        super.addGPSListener(aListener);
        if (!myReaderThreadStarted) {
            myReaderThreadStarted = true;
            myReaderThread.start();
        }
    }

    /**
     * @return the sleepBetweenFixes
     */
    public int getSleepBetweenFixes() {
        return this.sleepBetweenFixes;
    }

}
