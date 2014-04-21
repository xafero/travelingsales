/**
 * NMEAFileProvider.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.ConfigurationSetting;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.ConfigurationSetting.TYPES;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.travelingsalesman.navigation.OsmNavigationConfigSection;
import org.openstreetmap.travelingsalesman.trafficblocks.tmc.TMCfromNMEAFilter;
import org.openstreetmap.travelingsalesman.trafficblocks.tmc.RDSTMCParser;

/**
 * IGPSProvider that reads a local file in NMEA-format.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class NMEAFileProvider extends NMEAParsingGPSProvider implements IGPSProvider, IPlugin {

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(NMEAParsingGPSProvider.class.getName());

    /**
     * Set to 300ms because almost all GPS-receivers
     * give location-fixes at a 1Hz-intervall and output
     * 3 lines per fix.
     */
    private static final int DEFAULT_SLEEP_BETWEEN_FIXES = 300;

     /**
      * How many ms we sleep between the nmea-lines.
      */
    private int sleepBetweenFixes = DEFAULT_SLEEP_BETWEEN_FIXES;

    /**
     * {@inheritDoc}
     */
    public ConfigurationSection getSettings() {
        ConfigurationSection retval = new ConfigurationSection("GPXFileProvider");
        retval.addSetting(new ConfigurationSetting("NMEAFileProvider.filename",
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.NMEAFileProvider_filename.title"),
                TYPES.FILE,
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.NMEAFileProvider_filename.category"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.NMEAFileProvider_filename.desc")));

        return retval;
    }

    /**
     * How many ms we sleep between nmea-lines.
     * @param milliseconds pause in ms
     */
    public void setSleepBetweenFixes(final int milliseconds) {
        sleepBetweenFixes = milliseconds;
    }

    /**
     * Parse given NMEA-file at full speed.
     * @param file file for parsing
     */
    public NMEAFileProvider(final File file) {
        this.setSleepBetweenFixes(0);
        parseFile(file);
    }

    /**
     * Create a new GPXFileProvider.
     * Start reading after the first listener
     * has been added.
     */
    public NMEAFileProvider() {
        this.setSleepBetweenFixes(DEFAULT_SLEEP_BETWEEN_FIXES);
        File file = new File(Settings.getInstance().get("NMEAFileProvider.filename", "/tmp/test.nmea"));
        parseFile(file);
    }

    /**
     * Parse NMEA file in separate thread.
     * @param file NMEA-file.
     */
    private void parseFile(final File file) {
        Runnable run = new Runnable() {
            public void run() {
                if (!file.exists()) {
                    LOG.warning("File " + file.getAbsolutePath() + " does not exist");
                    return;
                }
                try {
                    BufferedReader buffer = TMCfromNMEAFilter.createTMCFilter(new FileInputStream(file), null, new RDSTMCParser(), file.getName());
                    String line = null;
                    while ((line = buffer.readLine()) != null) {
                        parseNMEA(line);
                        Thread.sleep(sleepBetweenFixes);
                    }
                    sendLocationLost();
                    buffer.close();
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error opening/parsing nmea-log-file", e);
                }
            }
        };
        myReaderThread = new Thread(run);
        myReaderThread.setDaemon(true);
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

}
