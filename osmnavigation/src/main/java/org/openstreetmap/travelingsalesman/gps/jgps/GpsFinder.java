/**
 * GpsFinder.java
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
package org.openstreetmap.travelingsalesman.gps.jgps;
// derived from SUN's examples in the javax.comm package
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Check every accessible serial port and wait for NMEA ($GP..) sequence.
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov </a>
 */

public final class GpsFinder implements SerialPortEventListener {
    /**
     * Our logger for debug- and error-output.
     */
    private static final Logger LOG = Logger.getLogger(GpsFinder.class.getName());

    /**
     * port identifier.
     */
    private static CommPortIdentifier portId;

    /**
     * list of accessible ports.
     */
    private static Enumeration<CommPortIdentifier> portList;

    /**
     * Input stream for NMEA sequence.
     */
    private InputStream inputStream;

    /**
     * Serial port.
     */
    private SerialPort serialPort;

    /**
     * True if gps NMEA sequence is founded.
     */
    private boolean gpsFound;

    /**
     * Readers threads array.
     */
    private static ArrayList<GpsFinder> readers;

    /**
     * Serial port baud rates.
     */
    private static final int[] PORTSPEEDS = {/*2400, 4800, */9600, 38400, 57600, 115600};

    /**
     * Resulted output string.
     */
    private static StringBuffer result = new StringBuffer();

    /**
     * Check every accessible serial port and wait for NMEA ($GP..) sequence.
     * @return serial port string e.g. "COM5,4800" or "null" if not detected.
     */
    @SuppressWarnings("unchecked")
    public static String findGpsPort() {
        try {
            portList = CommPortIdentifier.getPortIdentifiers();
            readers = new ArrayList<GpsFinder>();
            // create tester threads
            while (portList.hasMoreElements()) {
                portId = (CommPortIdentifier) portList.nextElement();
                if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                    LOG.info("Found port: " + portId.getName());
                    GpsFinder reader = new GpsFinder();
                    readers.add(reader);
                }
            }
            for (int i = 0; i < PORTSPEEDS.length; i++) {
                setReadersBaudRate(PORTSPEEDS[i]);
                LOG.info("start probing ports at " + PORTSPEEDS[i] + " for a GPS-device ...");
                wait3s();
                printFoundGPS();
            }
            LOG.info("...done searching for GPS devices");
            closeReaders();
            readers = null;
            String res = result.toString();
            result = new StringBuffer();
            return res;
        } catch (java.lang.NoClassDefFoundError e) {
            LOG.log(Level.SEVERE, "Unable to install JavaComm-library(rxtx).", e);
            return null;
        }
    }

    /**
     * close all serial-port readers.
     */
    private static void closeReaders() {
        // finalize threads
        for (GpsFinder reader : readers) {
            if (reader == null) {
                continue;
            }
            if (reader.serialPort != null) {
                reader.serialPort.removeEventListener();
                reader.serialPort.close();
            }
            reader = null;
        }
    }

    /**
     * Print the found ports to {@link #result}.
     */
    private static void printFoundGPS() {
        for (GpsFinder reader : readers) {
            if (reader.gpsFound) {
                LOG.info("GPS was found at : " + reader.serialPort.getName() + ","
                        + reader.serialPort.getBaudRate() + " bps");
                result.append("\n ");
                result.append(reader.serialPort.getName());
                result.append(",");
                result.append(reader.serialPort.getBaudRate());
                reader.gpsFound = false;
            }
        }
    }

    /**
     * Wait for 3 seconds.
     */
    private static void wait3s() {
        final int threeSeconds = 3000;
        try {
            Thread.sleep(threeSeconds);
        } catch (InterruptedException e) {
            // ignore being interrupted
            LOG.finest("sleep interrupted while waiting for a GPS-device to answer");
        }
    }

    /**
     * Change port baud rate.
     * @param baudRate new baud rate
     */
    private static void setReadersBaudRate(final int baudRate) {
        for (GpsFinder reader : readers) {
            try {
                if (reader.serialPort != null) {
                    reader.serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8,
                            SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
                }
            } catch (UnsupportedCommOperationException e) {
                LOG.warning("Cannot set serial-port to " + baudRate + "bps");
            }
        }
    }

    /**
     * Private constructor for finder thread.
     */
    private GpsFinder() {
        // initalize serial port
        try {
            final int wait = 2000;
            serialPort = (SerialPort) portId.open("GpsFinder", wait);
            inputStream = serialPort.getInputStream();
        } catch (PortInUseException e) {
            LOG.warning("cannot probe serial-port "
            + portId.getName() + " it is un use by "
            + e.currentOwner);
            return;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "cannot probe serial-port "
            + portId.getName(), e);
            return;
        }
        try {
            serialPort.addEventListener(this);
        } catch (TooManyListenersException e) {
            LOG.log(Level.WARNING, "cannot probe serial-port "
                    + portId.getName(), e);
            return;
        }
        // activate the DATA_AVAILABLE notifier
        serialPort.notifyOnDataAvailable(true);
        try {
            // set port parameters
            serialPort.setSerialPortParams(PORTSPEEDS[0], SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        } catch (UnsupportedCommOperationException e) {
            LOG.log(Level.WARNING, "cannot probe serial-port "
                + portId.getName() + " at " + PORTSPEEDS[0], e);

        }
    }

    /**
     * Serial port event listener.
     * @param event an event
     */
    public void serialEvent(final SerialPortEvent event) {
        switch (event.getEventType()) {
        case SerialPortEvent.BI:
        case SerialPortEvent.OE:
        case SerialPortEvent.FE:
        case SerialPortEvent.PE:
        case SerialPortEvent.CD:
        case SerialPortEvent.CTS:
        case SerialPortEvent.DSR:
        case SerialPortEvent.RI:
        case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
            break;
        case SerialPortEvent.DATA_AVAILABLE:
            // we get here if data has been received
            String line = "";
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                // read data
                while (inputStream.available() > 0) {
                    while (reader.ready()) {
                        line = reader.readLine();
                    }
                }
                LOG.finer("Read: " + line);
                if (!gpsFound) {
                    gpsFound = isGpsStream(line);
                }
            } catch (IOException e) {
                LOG.log(Level.FINE, "IOException in serial-port -event", e);
            }
            break;
            default:
                break;
        }
    }

    /**
     * Parse string and detect NMEA sequence.
     * @param line String for parsing.
     * @return true if it is NMEA sequence.
     */
    private static boolean isGpsStream(final String line) {
        if (line == null) {
            return false;
        }
        return (line.startsWith("$GP")
                || line.startsWith("GP"));
    }
}
