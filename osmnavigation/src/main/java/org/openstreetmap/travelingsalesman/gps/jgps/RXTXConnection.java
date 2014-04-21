package org.openstreetmap.travelingsalesman.gps.jgps;

import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class was acquired from JGPS project by Tomas Darmovzal (darmovzalt).
 * http://sourceforge.net/projects/jgps/
 *
 * @author Tomas Darmovzal
 */
@SuppressWarnings("unchecked")
public class RXTXConnection {

    /**
     * Default port-speed.
     */
    private static final int DEFAULTSPEED = 38400;

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(RXTXConnection.class
            .getName());

    /**
     * Array of existing serial ports.
     */
    private static Map<String, CommPortIdentifier> portIds;

    /**
     * Serial port.
     */
    private SerialPort port;

    static {
        portIds = new HashMap<String, CommPortIdentifier>();
        Enumeration<CommPortIdentifier> pidenum = CommPortIdentifier.getPortIdentifiers();
        while (pidenum.hasMoreElements()) {
            CommPortIdentifier pid = (CommPortIdentifier) pidenum.nextElement();
            if (pid.getPortType() == CommPortIdentifier.PORT_SERIAL)
                portIds.put(pid.getName(), pid);
        }
    }

    /**
     * Constructor with default port speed of 4800bps and default-parameters (8N1).
     * @param portName port name
     * @throws IOException if we cannot open the port
     */
    public RXTXConnection(final String portName) throws IOException {
        this(portName, DEFAULTSPEED);
    }

    /**
     * Constructor with default port parameters.
     * @param portName port name
     * @param baudRate given bound rate
     * @throws IOException if the port cannot be opened
     */
    public RXTXConnection(final String portName, final int baudRate) throws IOException {
        this(portName, baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
    }

    /**
     * @param portName e.g. "COM5" or "ttyS3"
     * @param baudRate e.g. 9600
     * @param dataBits e.g. SerialPort.DATABITS_8
     * @param stopBits e.g. SerialPort.STOPBITS_1
     * @param parity e.g. SerialPort.PARITY_NONE
     * @throws IOException if we cannot open the port
     */
    public RXTXConnection(final String portName,
                          final int baudRate,
                          final int dataBits,
                          final int stopBits,
                          final int parity) throws IOException {
        this(portName, 0, baudRate, dataBits, stopBits, parity);
    }

    /**
     * @param portName e.g. "COM5" or "ttyS3"
     * @param timeout in milliseconds
     * @param baudRate e.g. 9600
     * @param dataBits e.g. SerialPort.DATABITS_8
     * @param stopBits e.g. SerialPort.STOPBITS_1
     * @param parity e.g. SerialPort.PARITY_NONE
     * @throws IOException if we cannot open the port
     */
    public RXTXConnection(final String portName,
                          final int timeout,
                          final int baudRate,
                          final int dataBits,
                          final int stopBits,
                          final int parity) throws IOException {
        try {
            Class.forName("gnu.io.SerialPort");
        } catch (ClassNotFoundException e) {
            throw new IOException("Bundle gnu.ui is not installed");
        }
        try {
            CommPortIdentifier pid = (CommPortIdentifier) portIds.get(portName);
            if (pid == null)
                throw new IOException("Serial port \"" + portName
                        + "\" does not exist (available serial ports are: "
                        + getPortNamesAsString() + ")");
            this.port = (SerialPort) pid.open("OSMNavigation/Traveling Salesman", timeout);
        } catch (PortInUseException e) {
            throw new IOException(e.getMessage(), e);
        }
        try {
            this.port.setSerialPortParams(baudRate, dataBits, stopBits, parity);
        } catch (UnsupportedCommOperationException e) {
            LOG.log(Level.WARNING, "(" + portName + ":" + baudRate + ", data=" + dataBits + ", parity=" + parity + ", stop=" + stopBits + ")" + e.getMessage(), e);
        }
//        try {
//            byte[] rdsactivation = new byte[]{(byte)0xFF, 0x56, 0x78, 0x78, 0x56, 0x0D, 0x0A};
//            LOG.info("Sending:" + Integer.toHexString(rdsactivation[0])
//                    + " " + Integer.toHexString(rdsactivation[1])
//                    + " " + Integer.toHexString(rdsactivation[2])
//                    + " " + Integer.toHexString(rdsactivation[3])
//                    + " " + Integer.toHexString(rdsactivation[4])
//                    + " " + Integer.toHexString(rdsactivation[5])
//                    + " " + Integer.toHexString(rdsactivation[6]) + " to acivate RDS");
//            this.port.getOutputStream().write(rdsactivation);
//            Thread.sleep(1000);
//            this.port.getOutputStream().write(rdsactivation);
//            Thread.sleep(1000);
//            byte rdsStation = 0x01; //87.6MHz NDR2
//            // 0x00=offf
//            // 0x01=87.60MHz
//            // 0xCD= 108.00MHz=max
//            byte[] rdstuning = new byte[]{(byte) 0xFF, 0x46, rdsStation, 0x78, 0x46, 0x0D, 0x0A};
//            this.port.getOutputStream().write(rdstuning);
//            Thread.sleep(1000);
//            this.port.getOutputStream().write(rdstuning);
//        } catch (Exception e) {
//            LOG.log(Level.WARNING, "Cannot enable TMC", e);
//        }
    }

    /**
     * {@inheritDoc}
     */
    public InputStream getInputStream() throws IOException {
        return this.port.getInputStream();
    }

    /**
     * {@inheritDoc}
     */
    public OutputStream getOutputStream() throws IOException {
        return this.port.getOutputStream();
    }

    /**
     * Close the underlying port.
     */
    public void close() {
        this.port.close();
    }

    /**
     * @return The names of all serial ports in the system as a comma-separated string
     */
    public static String getPortNamesAsString() {
        String[] portNames = getPortNames();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < portNames.length; i++) {
            if (i > 0)
                sb.append(", ");
            sb.append(portNames[i]);
        }
        return sb.toString();
    }

    /**
     * @return The names of all serial ports in the system
     */
    public static String[] getPortNames() {
        if (!GPSChecker.isRxtxInstalled()) {
//            LOG.log(Level.INFO,
//                    "RxTx serial port library is not installed."
//                    + " Installation started...");
            GPSChecker.installRxtx();
        }
        Set portNames = portIds.keySet();
        String[] ret = new String[portNames.size()];
        Iterator iter = portNames.iterator();
        int count = 0;
        while (iter.hasNext()) {
            ret[count++] = (String) iter.next();
        }
        return ret;
    }

    /**
     * @param def has 2 formats: e.g. "COM5,57600,8,1,N" or "ttyS3,9600"
     * @return the connection, never null
     * @throws IOException if we cannot parse the definition
     */
    public static RXTXConnection open(final String def) throws IOException {
        StringTokenizer st = new StringTokenizer(def, ",");
        int count = st.countTokens();
        final int minParam = 2;
        final int maxParam = 5;
        if ((count < minParam) || (count > maxParam)) {
            throw new IOException("Open comm string must contain " + minParam + " to " + maxParam + " parameters");
        }
        String portName = st.nextToken();
        String baudRateStr = st.nextToken();
        int baudRate;
        try {
            baudRate = Integer.parseInt(baudRateStr);
        } catch (NumberFormatException e) {
            throw new IOException("Baud rate must be number: \"" + baudRateStr + "\"");
        }
        String dataBitsStr = "8";
        if (count > minParam) {
            dataBitsStr = st.nextToken().trim();
        }
        int dataBits;
        if ("5".equals(dataBitsStr)) {
            dataBits = SerialPort.DATABITS_5;
        } else if ("6".equals(dataBitsStr)) {
            dataBits = SerialPort.DATABITS_6;
        } else if ("7".equals(dataBitsStr)) {
            dataBits = SerialPort.DATABITS_7;
        } else if ("8".equals(dataBitsStr)) {
            dataBits = SerialPort.DATABITS_8;
        } else {
            throw new IOException("Data bits value may only be one of 5, 6, 7 or 8");
        }
        String stopBitsStr = "1";
        if (count > minParam + 1) {
            stopBitsStr = st.nextToken().trim();
        }
        int stopBits;
        if ("1".equals(stopBitsStr)) {
            stopBits = SerialPort.STOPBITS_1;
        } else if ("1.5".equals(stopBitsStr)) {
            stopBits = SerialPort.STOPBITS_1_5;
        } else if ("2".equals(stopBitsStr)) {
            stopBits = SerialPort.STOPBITS_2;
        } else {
            throw new IOException("Stop bits value may one be one of 1, 1.5 or 2");
        }

        String parityStr = "N";
        if (count > minParam + 2) {
            parityStr = st.nextToken().trim().toUpperCase();
        }
        int parity;
        if ("E".equals(parityStr)) {
            parity = SerialPort.PARITY_EVEN;
        } else if ("O".equals(parityStr)) {
            parity = SerialPort.PARITY_ODD;
        } else if ("N".equals(parityStr)) {
            parity = SerialPort.PARITY_NONE;
        } else if ("M".equals(parityStr)) {
            parity = SerialPort.PARITY_MARK;
        } else {
            throw new IOException("Parity value may only be one of E, O, N, M");
        }
        return new RXTXConnection(portName, baudRate, dataBits, stopBits, parity);
    }
}
