/**
 * JGPSProvider.java
 * created: 27.04.2008 22:23:49
 * (c) 2008 by <a href="mailto:oleg.chubaryov@mail.ru">Oleg Chubaryov</a>
 * This file is part of osmnavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.ConfigurationSetting;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.ConfigurationSetting.TYPES;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.travelingsalesman.gps.jgps.GPSChecker;
import org.openstreetmap.travelingsalesman.gps.jgps.RXTXConnection;
import org.openstreetmap.travelingsalesman.navigation.OsmNavigationConfigSection;
import org.openstreetmap.travelingsalesman.trafficblocks.tmc.TMCfromNMEAFilter;
import org.openstreetmap.travelingsalesman.trafficblocks.tmc.RDSTMCParser;

/**
 * JGPSProvider reads NMEA data from serial port,
 * and output to the NMEA parser.
 *
 * It uses sources from JGPS project by Tomas Darmovzal (darmovzalt)
 * Licensed under the GNU General Public License (GPL).
 * <a href="http://sourceforge.net/projects/jgps/">
 * http://sourceforge.net/projects/jgps/</a>
 *
 * with <a href="http://www.rxtx.org/">RXTX</a> native library providing serial
 * port communication. Licensed under the gnu LGPL license.
 * <a href="http://www.rxtx.org/">http://www.rxtx.org/</A>
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
 */
public class JGPSProvider extends NMEAParsingGPSProvider implements IPlugin {

    /**
     *  logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(JGPSProvider.class.getName());

    /**
     * How many ms we sleep between the gps requests.
     */
    //unused private static final int SLEEPMS = 1000;

    /**
     * How many ms we sleep between failed gps requests.
     */
    private static final int FAILEDSLEEPMS = 5000;

    /**
     * {@inheritDoc}
     */
    public ConfigurationSection getSettings() {
        ConfigurationSection retval = new ConfigurationSection("GPXFileProvider");
        retval.addSetting(new ConfigurationSetting("JGPSProvider.port",
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.JGPSProvider.port.title"),
                TYPES.STRING,
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.JGPSProvider.port.category"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.JGPSProvider.port.desc")));

        return retval;
    }

    /**
     * Worker-daemon-thread.
     * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
     */
    private final class JGPSWorker implements Runnable {
        /**
         * The reader we are using.
         */
        private BufferedReader myReader;

        /**
         * @return "portname,baudrate".
         */
        private String getConfiguredPort() {
            String[] portNames = RXTXConnection.getPortNames();
            if (portNames == null || portNames.length == 0) {
                return null;
            }
            return Settings.getInstance().get(
                    "JGPSProvider.port",
                    portNames[0]
                    + ",4800");
        }

        /**
         * ${@inheritDoc}.
         */
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {

                    if (state == STATE.out_of_order) {
                        LOG.log(Level.INFO,
                        "RxTx worker-thread is in state 'out of order' . Aborting.");
//                      System.out.println("out of order state processing");
                        Thread.currentThread().interrupt();
                    }

                    if (state == STATE.start) {
//                      System.out.println("start state processing");
//                      check if rxtx library installed properly, install if not
                        if (!GPSChecker.isRxtxInstalled()) {
                            LOG.log(Level.INFO,
                                    "RxTx serial port library is not installed."
                                    + " Installation started...");
                            GPSChecker.installRxtx();
                        }
                        if (GPSChecker.isRxtxInstalled()) {
                            state = STATE.port_checking;
                        } else {
                            // stop thread
                            state = STATE.out_of_order;

                            LOG.log(Level.INFO,
                                    "RxTx serial port library is not installed. Aborting.");
                        }
                    }

                    if (state == STATE.port_checking) {
//                      System.out.println("port checking state processing");
                        // TODO check of correctness serial port connection string
                        // TODO check gps accessibility, stop thread if gps didn't not answer
                        String configuredPort = getConfiguredPort();
                        if (configuredPort != null && GPSChecker.portIsExist(configuredPort)) {
                            state = STATE.gps_checking;
                        } else {
                            LOG.log(Level.FINE,
                                    "RxTx serial port does not exist. Waiting until it does.");
                            try {
                                Thread.sleep(FAILEDSLEEPMS);
                            } catch (InterruptedException e) {
                                LOG.log(Level.WARNING, "JGPSProvider - "
                                        + "worker-thread was interrupted "
                                        + "while sleeping until port exists", e);
                            }
                        }
                    }

                    if (state == STATE.gps_checking) {
//                      System.out.println("gps checking state processing");
                        try {
                            if (rxtxConnection == null) {
                                LOG.log(Level.FINE,
                                "RxTx serial trying to open port " + getConfiguredPort() + ".");
                                rxtxConnection = RXTXConnection.open(
                                        getConfiguredPort());
                                this.myReader = null;
                                LOG.log(Level.INFO,
                                "RxTx serial port opened successfully.");

                            }
                            if (rxtxConnection.getInputStream().available() == 0) {
                                try {
                                    Thread.sleep(FAILEDSLEEPMS);
                                } catch (InterruptedException e1) {
                                    LOG.log(Level.WARNING, "JGPSProvider - "
                                            + "worker-thread was interrupted "
                                            + "while sleeping until input-stream"
                                            + " on GPS-port is available", e1);
                                }
                                state = STATE.port_checking;
                            } else {
                                state = STATE.working;
                            }
                        } catch (IOException e) {
                            state = STATE.port_checking;
                            LOG.log(Level.SEVERE, "JGPSProvider - "
                                    + "received an IOException while opening"
                                    + " GPS-port", e);
                        }
                    }

                    if (state == STATE.working) {
                        try {
                            // opening RXTX connection and reading NMEA data
                            if (myReader == null) {
                                myReader = TMCfromNMEAFilter.createTMCFilter(rxtxConnection.getInputStream(), rxtxConnection.getOutputStream(),
                                                                             new RDSTMCParser(), getConfiguredPort());
                            }
                            String line = null;
                            while (myReader.ready() && rxtxConnection.getInputStream().available() > 1) {
                                line = myReader.readLine();
                                if (line != null) {
                                    parseNMEA(line);
                                }
                            }
                        } catch (IOException e) {
                            LOG.log(Level.SEVERE, "Error reading NMEA input data stream", e);
                            state = STATE.port_checking;
                        }
                    }
                }
                LOG.log(Level.INFO, "JGPS worker thread interrupted -  aborting");
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error in JGPS. aborting", e);
                e.printStackTrace();
            }
        }
    }

    /**
     * The working states of this state-machine.
     */
    public enum STATE {
        /**
         * The first state.
         */
        start,
        /**
         * Checking if the serial port exists.
         */
        port_checking,
        /**
         * Checking if we can open the serial-port.
         */
        gps_checking,
        /**
         * waiting for nmea-lines and parsing them.
         */
        working,
        /**
         * Something failed. We are not operational.
         */
        out_of_order,
    }

    /**
     * The current state of this state-machine.
     */
    private static STATE state = STATE.start;

    /**
     * Our serial-connection.
     */
    private RXTXConnection rxtxConnection;


    /**
     * Create a new JGPSProvider.<br>
     * Start reading after the first listener<br>
     * has been added.<br>
     *
     * get the connection string for COM-port from Settings.<br/>
     * [portname],[baudrate]<br>
     * data bits: 8, stop bits: 1, parity: N<br/>
     * Example: COM1,4800<br>
     * or [portname],[baudrate],[databits],[stopbits],[parity]<br>
     * Example: COM1,4800,8,1,N<br>
     * use "/dev/ttyS0" instead "COM1", for linux based systems<br>
     */
    public JGPSProvider() {
        Runnable run = new JGPSWorker();
        myReaderThread = new Thread(run);
        myReaderThread.setName("JGPS-Reader");
        myReaderThread.setDaemon(true);
    }

    /**
     * Our thread for asynchronous reading.
     */
    private Thread myReaderThread;
    /**
     * Was {@link #myReaderThread} already started?
     */
    private boolean wasStarted = false;

    /**
     * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider#addGPSListener(org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener)
     * @param aListener the new listener to add.
     */
    public void addGPSListener(final IGPSListener aListener) {
        super.addGPSListener(aListener);
        if (!wasStarted) {
            myReaderThread.start();
            wasStarted = true;
        }
    }

}
