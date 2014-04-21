/**
 * GpsDProvider.java
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
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.ConfigurationSetting;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.ConfigurationSetting.TYPES;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.travelingsalesman.navigation.OsmNavigationConfigSection;

/**
 * IGPSProvider that connects to a GPSD.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class GpsDProvider extends NMEAParsingGPSProvider implements IPlugin {

    /**
     * Sleep this many milliseconds before doing another connection-attemps.
     */
    private static final int SLEEPMS = 5000;

    /**
     * The default-port of GPSd.
     */
    public static final int DEFAULTPORT = 2947;

    /**
     * my logger for debug and error-output.
     */
    static final Logger LOG = Logger.getLogger(GpsDProvider.class.getName());

    /**
     * {@inheritDoc}
     */
    public ConfigurationSection getSettings() {
        ConfigurationSection retval = new ConfigurationSection("GpsDProvider");
        retval.addSetting(new ConfigurationSetting("gpsd.address",
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.gpsd.host.title"),
                TYPES.STRING,
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.gpsd.category"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.gpsd.host.desc")));
        retval.addSetting(new ConfigurationSetting("gpsd.port",
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.gpsd.port.title"),
                TYPES.INTEGER,
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.gpsd.category"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.gpsd.port.desc")));

        return retval;
    }

    /**
     * Create a new GpsDProvider.
     * Start reading after the first listener
     * has been added.
     */
    public GpsDProvider() {
        Runnable run = new Runnable() {
            public void run() {

                String hostname = Settings.getInstance().get("gpsd.address", "127.0.0.1");

                while (!Thread.currentThread().isInterrupted()) {
                try {
                    InetAddress server = InetAddress.getByName(hostname);
                    //InetAddress server = InetAddress.getByName("127.0.0.1");
                    Socket sock = new Socket(server, Settings.getInstance().getInteger("gpsd.port", DEFAULTPORT));
                    sock.getOutputStream().write("R\n".getBytes("ASCII"));
                    LOG.log(Level.FINER, "Connected to gpsd");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));

                    String line = null;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("GPSD"))
                            continue;
                        parseNMEA(line);
                    }

                } catch (UnknownHostException e) {
                    LOG.log(Level.SEVERE, "Error opening/parsing gpsd - cannot resolve host '" + hostname + "'");
                } catch (ConnectException e) {
                    try {
                        Thread.sleep(SLEEPMS);
                    } catch (InterruptedException e1) {
                        break;
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Error opening/parsing gpsd", e);
                    try {
                        Thread.sleep(SLEEPMS);
                    } catch (InterruptedException e1) {
                        break;
                    }
                }
                }
            }
        };
        myReaderThread = new Thread(run);
        myReaderThread.setName("GPSD-Reader");
        myReaderThread.setDaemon(true);
    }


    /**
     * Our thread for asynchronous reading.
     */
    private Thread myReaderThread;

    /**
     * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider#addGPSListener(org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener)
     * @param aListener the new listener to add.
     */
    public void addGPSListener(final IGPSListener aListener) {
        super.addGPSListener(aListener);
        if (!myReaderThread.isAlive())
            myReaderThread.start();
    }
}
