package org.openstreetmap.travelingsalesman.trafficblocks.tmc;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.travelingsalesman.trafficblocks.TrafficMessageStore;
import org.openstreetmap.travelingsalesman.trafficblocks.ITrafficMessageSource.ITunableTrafficMessageSource;

/**
 * Input-stream that filters binary RDS and TMC -messages
 * in GNC- and Royaltek- encoding from an NMEA-stream.<br/>
 * GNC-encoding inserts a 10 byte block made up of a start-byte,
 * 8 data-bytes and a stop-byte at any location into the NMEA-data.<br/>
 * This Filter removes these GNC-blocks and outputs the original NMEA
 * -data.<br/>
 * If given an optional output-stream this class also sends the
 * proper commands too the receiver to implement a continuous automatic
 * tuning to the next radio-station that sends TMC-messages.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public final class TMCfromNMEAFilter extends InputStream implements ITunableTrafficMessageSource {

    /**
     * 0xFF.
     */
    private static final int FF = 0xFF;

    /**
     * Parameter-byte for GNC-command to tune to a specific frequency.
     * (given after the frequency-byte.)
     */
    private static final byte GNCTUNINGPARAM = hexToByte("0x78");

    /**
     * GNC-command to tune to a specific frequency.
     */
    private static final byte GNCTUNING = hexToByte("0x46");

    /**
     * Parameter-byte to GNC-command to activate TMC.
     */
    private static final byte GNCACTIVATEPARAM = hexToByte("0x78");

    /**
     * GNC-command to activate TMC.
     */
    private static final byte GNCACTIVATE = hexToByte("0x56");

    /**
     * Start-byte of GNC-commands.
     */
    private static final byte GNCSTART = hexToByte("0xFF");

    /**
     * Last byte of any GNC-command.
     */
    private static final byte GNCSTOP2 = hexToByte("0x0A");

    /**
     * Second to last byte of any GNC-command.
     */
    private static final byte GNCSTOP1 = hexToByte("0x0D");

    /**
     * Wait this many milliseconds after sending a command
     * to the RDS-receiver.
     */
    private static final int WAITAFTERCOMMAND = 50;

    /**
     * After receiving no RDS-TMC -message for this many
     * milliseconds, change the frequency.
     */
    private static final int TIMEOUT = 12000;

    /**
     * state-machine for tuning
     * = searching for a radio-stations
     * with RDS-TMC-data.
     */
    private enum STATE {
        /**
         * State where we did not receive a TMC-message
         * on this frequency and will change the frequency
         * after the next timeout.
         */
        SEARCHING,
        /**
         * We received a message and are thus tuned to
         * a radio-station sending RDS-TMC -data.
         */
        TUNED
    };
    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(TMCfromNMEAFilter.class.getName());

    /**
     * Our current state.
     */
    private STATE myState = STATE.TUNED;
    /**
     * The current frequency we should be tuned to.
     */
    private byte myCurrentFrequency = MINFREQUENCY;

    /**
     * Helper to support propertyChangeListeners.
     */
    private PropertyChangeSupport myPropertyChangeSupport = null;
//    /**
//     * Tune to this frequency to turn the device off.
//     */
//    private static final byte OFFFREQUENCY = (byte) 0x00;
    /**
     * First frequency to try. (87.60MHz)
     */
    private static final byte MINFREQUENCY = Byte.MIN_VALUE + 1;
    /**
     * The frequency in MHz that {@link #MINFREQUENCY}
     * represents.
     */
    private static final BigDecimal MINFREQUENCYMHZ = new BigDecimal("87.5");
    /**
     * An increase in the frequency-code by 1 means an
     * increase in this many MHZ.
     * @see #MINFREQUENCYMHZ
     */
    private static final BigDecimal FREQUENCYMHZINCREMENT = new BigDecimal("0.1");
    /**
     * Last frequency to try. (108.00MHz)
     */
    private static final byte MAXFREQUENCY = (byte) 0xCD;

    /**
     * Thread for doing the timing in tuning.
     */
    private Thread myTuningStrategy = new Thread() {
        public void run() {
            setName("TMC from NMEA - TuningStrategy");
            while (true) {
                try {
                    boolean isSearching = true;
                    synchronized (TMCfromNMEAFilter.this) {
                        isSearching = (myState == STATE.SEARCHING);
                    }
                    if (isSearching) {
                        LOG.info("(" + Thread.currentThread().getId() + ")No RDS(TMC?)-message received, tuning to 0x"
                                + byteToHex(myCurrentFrequency)
                                + "=" + getFrequency() + "MHz");
                        byte[] rdsactivation = new byte[]{(byte) GNCSTART, GNCACTIVATE, GNCACTIVATEPARAM, GNCACTIVATEPARAM, GNCACTIVATE, GNCSTOP1, GNCSTOP2};
                        //                    LOG.info("Sending:" + Integer.toHexString(rdsactivation[0])
                        //                            + " " + Integer.toHexString(rdsactivation[1])
                        //                            + " " + Integer.toHexString(rdsactivation[2])
                        //                            + " " + Integer.toHexString(rdsactivation[3])
                        //                            + " " + Integer.toHexString(rdsactivation[4])
                        //                            + " " + Integer.toHexString(rdsactivation[5])
                        //                            + " " + Integer.toHexString(rdsactivation[6]) + " to acivate RDS");
                        myOutputStream.write(rdsactivation);

                        //For Royaltek -devices: set the device to scan upward for stations with TMC-messages
                        myWriter.write("$PSRF126,2*3C");
                        myWriter.write("$PSRF123,1,1*27");
                        myWriter.flush();
                        Thread.sleep(WAITAFTERCOMMAND);
                        myOutputStream.write(rdsactivation);
                        Thread.sleep(WAITAFTERCOMMAND);

                        //tune to next frequency
                        byte newFrequency = myCurrentFrequency;
                        newFrequency++;
                        if (myCurrentFrequency == MAXFREQUENCY) {
                            newFrequency = MINFREQUENCY;
                        }
                        Thread.sleep(WAITAFTERCOMMAND);
                        setFrequencyCode(newFrequency);
                        //test: auto-tuning command from 0x01 up
//                        myOutputStream.write(new byte[]{GNCSTART,
//                                hexToByte("0x79"),
//                                hexToByte("0x01"),
//                                hexToByte("0x01"),
//                                hexToByte("0x79"),
//                                GNCSTOP1,
//                                GNCSTOP2});

                        LOG.info("Waiting for TMC-message...");
                        sleep(TIMEOUT);
                    } else {
                        LOG.fine("(" + Thread.currentThread().getId() + ")RDS(TMC?)-message received, staying at frequency 0x"
                                + Integer.toHexString(((int) myCurrentFrequency + Byte.MIN_VALUE) & FF)
                                + "=" + getFrequency() + "MHz");
                        LOG.info("Waiting for TMC-message...");
                        myState = STATE.SEARCHING;
                        sleep(TIMEOUT);
                        sleep(TIMEOUT);
                        sleep(TIMEOUT); // if we received a message on this channel, we wait 3x as long for a new one
                        sleep(TIMEOUT);
                        sleep(TIMEOUT);
                        sleep(TIMEOUT); // if we received a message on this channel, we wait 3x as long for a new one
                        sleep(TIMEOUT);
                        sleep(TIMEOUT);
                        sleep(TIMEOUT); // if we received a message on this channel, we wait 3x as long for a new one
                    }
                } catch (InterruptedException e) {
                    LOG.log(Level.FINE, "I was interrupted. Presumably because I was told by the receiver that"
                            + " there is no TMC at this frequency, tuning to another frequency");
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Cannot enable TMC/tune to another frequency", e);
                }
            }
        }
    };
    /**
     * '?'.
     */
    private static final byte MARKER = "?".getBytes()[0];
    /**
     * Number of user-bytes in a GNC-block.
     */
    private static final int GNCBLOCKLENGTH = 8;

    /**
     * The stream where the NMEA+RDS comes from.
     */
    private InputStream myInputStream;
    /**
     * (May be null)
     * The stream where we write commands for tuning to.
     */
    private OutputStream myOutputStream;
    /**
     * A writer on top of
     * {@link #myOutputStream} to give
     * tuning-commands for Roayltek -type
     * devices too.
     */
    private OutputStreamWriter myWriter;

    /**
     * This is a reader we put over {@link #myInputStream}
     * to read GNC2.0 -type binary TMC-messages
     * and Royaltek -type ascii TMC-messages.
     */
    private BufferedReader myReader;

    /**
     * The name of the port, file, server,... we
     * are connected to.
     */
    private String myPortName;

    /**
     * Create a reader that filters out GNC-type and Royaltek-type RDS-TMC -messages
     * and handles them. Only the remaining NMEA remains to be read from it.
     * @param aInputStream The stream where the NMEA+RDS comes from.
     * @param aOutputStream (may be null) stream to write commands for tuning and for activating rds-tmc to
     * @param aListener a listener for the filtered out TMC-messaged
     * @param aPortName The name of the port  we are connected to.
     * @return a reader for the pure NMEA data with RDS-TMC already filtered out
     */
    public static BufferedReader createTMCFilter(final InputStream aInputStream, final OutputStream aOutputStream, final TMCListener aListener, final String aPortName) {
        TMCfromNMEAFilter in = new TMCfromNMEAFilter(aInputStream, aOutputStream, aPortName);
        in.addTMCListener(aListener);
        return in.getReader();
    }

    /**
     * The name of the port, file, server,... we
     * are connected to.
     * @return the name of the port
     */
    public String getPortName() {
        return this.myPortName;
    }

    /**
     * @param aInputStream The stream where the NMEA+RDS comes from.
     * @param aOutputStream (may be null) stream to write commands for tuning and for activating rds-tmc to
     * @param aPortName The name of the port  we are connected to.
     */
    private TMCfromNMEAFilter(final InputStream aInputStream, final OutputStream aOutputStream, final String aPortName) {
        super();
        this.myInputStream = aInputStream;
        this.myOutputStream = aOutputStream;
        this.myPortName = aPortName;

        //start thread for continued tuning
        // to radio-stations with RDS-TMC -data.
        if (aOutputStream != null) {
            this.myWriter = new OutputStreamWriter(this.myOutputStream);
            myTuningStrategy.setDaemon(true);
            myTuningStrategy.setName("RDS-TMC in GNC2.0 - tuning timer");
            myTuningStrategy.start();
            //TODO: test auto-tuning
//            try {
//                byte[] rdsactivation = new byte[]{ GNCSTART, GNCACTIVATE, GNCACTIVATEPARAM, GNCACTIVATEPARAM, GNCACTIVATE, GNCSTOP1, GNCSTOP2};
//                aOutputStream.write(rdsactivation);
//                aOutputStream.write(new byte[]{hexToByte("0xFF"),
//                        hexToByte("0x79"),
//                        hexToByte("0x01"),
//                        hexToByte("0x01"),
//                        hexToByte("0x79")});
//            } catch (IOException e) {
//                e.printStackTrace(); //TODO: testcode
//            }

            // FF 79 xx 01 79 scan from xx up
        }
        TrafficMessageStore.getInstance().addTrafficMessageSource(this);
    }

    /**
     * ${@inheritDoc}.
     */
    public int available() throws IOException {
        return this.myInputStream.available();
    }

    /**
     * {@inheritDoc}
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
        int b = this.myInputStream.read();
        if (b < 0) {
            return b; // EOF
        }
        while (((byte) b) == MARKER) {
            int reat = 1;
            byte[] gncBlock = new byte[GNCBLOCKLENGTH + 2];
            gncBlock[0] = (byte) b;
            while (reat < gncBlock.length) {
                reat += this.myInputStream.read(gncBlock, reat, gncBlock.length - reat);
            }
            LOG.finer("GNS 2.0 received: 0x"
                    + byteToHex(gncBlock[0])
                    + byteToHex(gncBlock[1])
                    + "-"
                    + byteToHex(gncBlock[2])
                    + byteToHex(gncBlock[3])
                    + "-"
                    + byteToHex(gncBlock[4])
                    + byteToHex(gncBlock[5])
                    + "-"
                    + byteToHex(gncBlock[6])
                    + byteToHex(gncBlock[7])
                    + "-"
                    + byteToHex(gncBlock[8])
                    + byteToHex(gncBlock[9]));
            // filter out responses to our tuning-commands
            if (gncBlock[1] != 0 || gncBlock[2] != 0) {
                final int bytelength = 8;
                final int blockMask = 0xFFFF;
                final int byteMask = 0xFF;
                int i = 1;
                tmcBlockReceived(
                        (((int) gncBlock[i++] & byteMask) << bytelength | ((int) gncBlock[i++] & byteMask)) & blockMask,
                        (((int) gncBlock[i++] & byteMask) << bytelength | ((int) gncBlock[i++] & byteMask)) & blockMask,
                        (((int) gncBlock[i++] & byteMask) << bytelength | ((int) gncBlock[i++] & byteMask)) & blockMask,
                        (((int) gncBlock[i++] & byteMask) << bytelength | ((int) gncBlock[i++] & byteMask)) & blockMask
                       );
            } else {
                /*
                 * from http://www.capuzza.com/detail.php?ID=123764
Wird der Receiver auf eine Frequenz eingestellt, auf der keine RDS-Daten uebertragen werden, gibt er folgenden 10-Byte-Block (anstelle der RDS-Daten) aus:

  0x3F | 0x00 | 0x00 | 0x46 | X | Y | Z | 0x00 | 0x00 | 0x3F

Wobei X die Frequenz darstellt (X / 10 + 87.5) MHz,
Y und Z sind entweder 0x00 | 0x00 wenn kein Sender empfangen wird
oder 0x01 | 0x55 wenn ein Sender empfangen wird, der keine RDS-Daten ausgibt
(z.B. weil die Empfangsstaerke nicht ausreichend ist).
                 */
                final int  markerPos = 3;
                final int  endMarker1Pos = 7;
                final int  endMarker2Pos = 8;
                if (gncBlock[markerPos] == GNCTUNING
                    && gncBlock[endMarker1Pos] == 0
                    && gncBlock[endMarker2Pos] == 0) {
                    final int pos46 = 3;
                    final int xpos = 4;
                    final int ypos = 5;
                    final int zpos = 6;
                    byte byte46 = gncBlock[pos46];
                    byte x = gncBlock[xpos];
                    byte y = gncBlock[ypos];
                    byte z = gncBlock[zpos];
                    if (x == myCurrentFrequency) {
                        LOG.fine("Receiver tells us there is no TMC-data at this frequency (byte 3=0x"
                                + byteToHex(byte46) + " should be 0x46, y=0x"
                                + byteToHex(y) + " should be 0x01, z=0x"
                                + byteToHex(z) + " should be 0x55)");
                        if (myTuningStrategy != null) {
                            myTuningStrategy.interrupt();
                        }
                    } else {
                        LOG.fine("Receiver tells us there is no TMC-data at some wrong frequency "
                                + "0x" + Integer.toHexString(x) + " vs " + Integer.toHexString(myCurrentFrequency));
                    }
                }
                int i = 1;
                LOG.info("received: " + Integer.toHexString(gncBlock[i++]) + Integer.toHexString(gncBlock[i++])
                        + "-" + Integer.toHexString(gncBlock[i++]) + Integer.toHexString(gncBlock[i++])
                        + "-" + Integer.toHexString(gncBlock[i++]) + Integer.toHexString(gncBlock[i++])
                        + "-" + Integer.toHexString(gncBlock[i++]) + Integer.toHexString(gncBlock[i++]));
            }
            b = myInputStream.read();
        }
        return b;
    }

    /**
     * A TMC-message was received either from a GNC-type or a Roayaltek-type device.
     * @param aBlock1 first 2byte block
     * @param aBlock2 second 2byte block
     * @param aBlock3 third 2byte block
     * @param aBlock4 fourth 2byte block
     */
    private void tmcBlockReceived(final int aBlock1, final int aBlock2, final int aBlock3, final int aBlock4) {
        LOG.info("(thread: " + Thread.currentThread().getId() + "=" + Thread.currentThread().getName() + " at "
                + getFrequency() + "MHz) RDS(TMC?)-message received ("
                + myTMCListeners.size() + " listeners)");
        synchronized (this) {
            this.myState = STATE.TUNED; // we received a message, thus we are tuned.
        }
        for (TMCListener listener : myTMCListeners) {
            try {
                listener.tmcBlockReceived(aBlock1, aBlock2, aBlock3, aBlock4);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Exception while informing TMC-listeners.", e);
            }
        }
    }
    /**
     * @param aListener A {@link TMCListener}s that want to be informed about TMC-bocks reat.
     */
    public void addTMCListener(final TMCListener aListener) {
        this.myTMCListeners.add(aListener);
    }

    /**
     * Our {@link TMCListener}s that want to be informed about TMC-bocks reat.
     */
    private Set<TMCListener> myTMCListeners = new HashSet<TMCListener>();

    /**
     * Our {@link TMCListener}s that want to be informed about TMC-bocks reat.
     */
    public static interface TMCListener {
        /**
         * A TMC-block has been received.
         * @param block1 the first block
         * @param block2 the first block
         * @param block3 the first block
         * @param block4 the first block
         */
      void tmcBlockReceived(int block1,
                            int block2,
                            int block3,
                            int block4);
    }

    /**
     * @return the reader
     */
    protected BufferedReader getReader() {
        if (myReader == null) {
            myReader = new RoyaltekFilteringReader(new BufferedReader(new InputStreamReader(this)));
        }
        return myReader;
    }

    /**
     * @return the frequency we are currently tuned to in MHz.
     */
    public BigDecimal getFrequency() {
        // http://www.capuzza.com/detail.php?ID=123764
        // 0x00 = Byte.MIN_VALUE = 87.5MHz
        BigDecimal retval = decodeFrequencyCode(this.myCurrentFrequency);
//        LOG.info("getFrequency()=" + retval);
        return retval;
    }

    /**
     * @param aFrequencyCode a frequency-byte as used by the GNC 2.0 -protocoll
     * @return the frequency in MHz
     */
    protected static BigDecimal decodeFrequencyCode(final byte aFrequencyCode) {
        final int byteMask = 0xFF;
        int frequencyCode = ((int) aFrequencyCode) & byteMask;
        BigDecimal retval = new BigDecimal(frequencyCode);
        retval = retval.multiply(FREQUENCYMHZINCREMENT);
        retval = retval.add(MINFREQUENCYMHZ);
        return retval;
    }

    /**
     * @param aFrequencyMHz the frequency to tune to in MHz.
     * @throws IOException if we cannot write to the device
     * @throws InterruptedException if our mandatory sleep after sending a command is interrupted
     */
    public void setFrequency(final BigDecimal aFrequencyMHz) throws IOException, InterruptedException {
        //LOG.info("setFrequency(" + aFrequencyMHz + " MHz)");
        byte frequencyCode = encodeFrequencyCode(aFrequencyMHz);
        setFrequencyCode(frequencyCode);
    }

    /**
     * @param aFrequencyMHz a frequency in MHz
     * @return a code-byte as used  by the GNC 2.0 -protocol
     */
    protected static byte encodeFrequencyCode(final BigDecimal aFrequencyMHz) {
        BigDecimal newCode = aFrequencyMHz.subtract(MINFREQUENCYMHZ);
        newCode = newCode.divide(FREQUENCYMHZINCREMENT);
        byte frequencyCode = (byte) newCode.intValue();
        return frequencyCode;
    }

    /**
     * @param aFrequencyCode the code of the frequency to tune to.
     * @throws IOException if we cannot write to the device
     * @throws InterruptedException if our mandatory sleep after sending a command is interrupted
     */
    public void setFrequencyCode(final byte aFrequencyCode) throws IOException, InterruptedException {
        this.myCurrentFrequency = aFrequencyCode;
        LOG.info("setFrequencyCode(" + aFrequencyCode + "=0x"
                + byteToHex(aFrequencyCode) + ")=" + getFrequency() + " MHz");
        byte[] rdstuning = new byte[]{(byte) GNCSTART, GNCTUNING, this.myCurrentFrequency, GNCTUNINGPARAM, GNCTUNING, GNCSTOP1, GNCSTOP2};
        myOutputStream.write(rdstuning);
        synchronized (TMCfromNMEAFilter.this) {
            myState = STATE.SEARCHING;
        }
        synchronized (myOutputStream) {
            myOutputStream.write(rdstuning);
            myOutputStream.flush();
            Thread.sleep(WAITAFTERCOMMAND);
        }
        if (myPropertyChangeSupport != null) {
            myPropertyChangeSupport.firePropertyChange("frequencyCode", null, aFrequencyCode);
            myPropertyChangeSupport.firePropertyChange("frequency", null, getFrequency());
        }
    }

    /**
     * @param aFrequencyCode a byte
     * @return the hexdacimal representation as an unsigned value
     */
    protected static String byteToHex(final byte aFrequencyCode) {
        final int byteMask = 0xFF;
        return Integer.toHexString(((int) aFrequencyCode) & byteMask);
    }
    /**
     * Input may be in the format "FF" or "0xFF".
     * @param aHexNum the hexdacimal representation as an unsigned value
     * @return the apropriate byte-value
     */
    protected static byte hexToByte(final String aHexNum) {
        int i = Integer.MIN_VALUE;
        final int hex = 16;
        if (aHexNum.startsWith("0x")) {
            i = Integer.parseInt(aHexNum.substring(2), hex);
        } else {
            i = Integer.parseInt(aHexNum, hex);
        }
        return (byte) i;
    }

    /**
     * @return the code of the frequency we are tuned to.
     */
    public byte getFrequencyCode() {
        return myCurrentFrequency;
    }

    /**
     * Add a PropertyChangeListener for a specific property.  The listener
     * will be invoked only when a call on firePropertyChange names that
     * specific property.
     * The same listener object may be added more than once.  For each
     * property,  the listener will be invoked the number of times it was added
     * for that property.
     * If <code>propertyName</code> or <code>listener</code> is null, no
     * exception is thrown and no action is taken.
     *
     * @param aPropertyName  The name of the property to listen on. "frequency" or "frequencycode"
     * @param aListener  The PropertyChangeListener to be added
     */

    public void addPropertyChangeListener(
                final String aPropertyName,
                final PropertyChangeListener aListener) {
        if (this.myPropertyChangeSupport == null) {
            this.myPropertyChangeSupport = new PropertyChangeSupport(this);
        }
        this.myPropertyChangeSupport.addPropertyChangeListener(aPropertyName, aListener);
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     * If <code>listener</code> was added more than once to the same event
     * source, it will be notified one less time after being removed.
     * If <code>listener</code> is null, or was never added, no exception is
     * thrown and no action is taken.
     *
     * @param aListener  The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(
                final PropertyChangeListener aListener) {
        if (this.myPropertyChangeSupport != null) {
            this.myPropertyChangeSupport.removePropertyChangeListener(aListener);
        }
    }

    /**
     * Reader that filters Royaltek-style RDS-TMC messages from the
     * stream.<br/>
     * You are supposed to use only the {@link #readLine()} -method.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    protected class RoyaltekFilteringReader extends BufferedReader {

        /**
         * @param aBufferedReader where to read the nmea+royaltek from
         */
        public RoyaltekFilteringReader(final Reader aBufferedReader) {
            super(aBufferedReader);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String readLine() throws IOException {
            // TODO test this with a real receiver
            String line = super.readLine();
            if (line == null) {
                return line;
            }
            // TMC-data encapsulated in NMEA by Royaltek GPS-receivers
            // $RTTMC,[0,2,4,8,15],block1,block2,block3,block4*checksum
            if (line.startsWith("$RTTMC") || line.startsWith("RTTMC")) {
                String[] elements = line.split(",");
                parseRTTMC(elements);
            }
            // we ignore RTRDS and RTEFTXT for now

            return line;
        }

        /**
         * Parse TMC-data encasulated in NMEA by Royaltek GPS-receivers.
         * <ul>
         *  <li> $RTTMC</li>
         *  <li> [0,2,4,8,15] = block-type</li>
         *  <li> block1</li>
         *  <li> block2</li>
         *  <li> block3</li>
         *  <li> block4*checksum</li>
         * </ul>
         * @param elements NMEA elements of parsed sentence.
         */
        private void parseRTTMC(final String[] elements) {
            final int hex = 16;
            final int block1pos = 2;
            final int block2pos = block1pos + 1;
            final int block3pos = block2pos + 1;
            final int block4pos = block3pos + 1;
            final int blocklength = 4;
            try {
                int block1 = Integer.parseInt(elements[block1pos].substring(0, blocklength), hex);
                int block2 = Integer.parseInt(elements[block2pos].substring(0, blocklength), hex);
                int block3 = Integer.parseInt(elements[block3pos].substring(0, blocklength), hex);
                int block4 = Integer.parseInt(elements[block4pos].substring(0, blocklength), hex);
                //we may add a checksum-check here too
                tmcBlockReceived(block1, block2, block3, block4);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Cannot parse Royaltek-TMC -data (RTTMC) embedded in NMEA", e);
            }
        }

    }

    /**
     * {@inheritDoc}.
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        super.close();
        TrafficMessageStore.getInstance().removeTrafficMessageSource(this);
    }

}
