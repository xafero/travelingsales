/**
 * RDSTMCParseer.java
 * created: 03.2009
 * (c) 2008 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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
package org.openstreetmap.travelingsalesman.trafficblocks.tmc;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.travelingsalesman.navigation.NavigationManager;
import org.openstreetmap.travelingsalesman.trafficblocks.TrafficMessageStore;
import org.openstreetmap.travelingsalesman.trafficblocks.TrafficMessageStore.TMCLocation;
import org.openstreetmap.travelingsalesman.trafficblocks.tmc.TMCfromNMEAFilter.TMCListener;
import org.openstreetmap.travelingsalesman.trafficblocks.tmc.TMCEventFactory.TMCEvent;

/**
 * This class parses received binary RDS-TMC messages and hands them over for further processing.<br/>
 * If multiple receivers exist, each one of them must have it's own instance
 * of this class as we need to store state-information about the local radion-station
 * to correctly interpret messages.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class RDSTMCParser implements TMCListener {
    /**
     * Charset used in TMC-messages.
     * @see #myTMCServiceProviderName
     */
    private static final Charset CHARSET = Charset.forName("ASCII");

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(RDSTMCParser.class.getName());

    /**
     * Number of bytes excluding checksum in a
     * TMC-block.
     */
    public static final int TMCBLOCKLENGTH = 8;

    /**
     * Factory for {@link TMCEvent}s given an event-code.
     */
    private TMCEventFactory myTMCEventFactory = new TMCEventFactory();

    /**
     * CI of the last MGM received (to piece them together again).
     */
    private int myLastMultigroupMesageContinuityIndex = 0;

    /**
     * Last LTN received in a group 3A-message.
     * @see #handleGroup3A(boolean, boolean, int, int, int, int)
     */
    private int myLocationTableNumber = -1;

    /**
     * Last country-ID decoded with a group 3A-message.
     */
    private int myCountryID = -1;

    /**
     * M-bit (Mode of transmission)
     * of the last Group 3A-message.<br/>
     * This indicates how the station informs about predictable
     * gaps during wich no 8A-messages are transmitted to allow
     * us to tune to other frequencies.<br/>
     * false = Basis mode (ISO14819-1 section 7.5.2.3.1)<br/>
     * false = Enhanced mode (ISO14819-1 section 7.5.2.3.2)<br/>
     */
    private boolean myModeOfTransmission;

    /**
     * Parameter for {@link #myModeOfTransmission}.
     * If myModeOfTransmission = false then
     * this indicates how many milliseconds
     * after an 8A-message no other 8A-message is to
     * be started.<br/>
     * <ul>
     *  <li>0 = gap=3  =&gt; 2.85 8A-messages per second</li>
     *  <li>1 = gap=5  =&gt; 1.90 8A-messages per second</li>
     *  <li>2 = gap=8  =&gt; 1.27 8A-messages per second</li>
     *  <li>3 = gap=11 =&gt; 0.95 8A-messages per second</li>
     * </ul>
     * @see #myModeOfTransmission
     */
    private int myGap;

    /**
     * Transmitted name of the TMC service provider.
     * @see #handleGroup3A(boolean, boolean, int, int, int, int, char).
     */
    private char[] myTMCServiceProviderName = new char[] {'?', '?', '?', '?', '?', '?', '?', '?'};

    /**
     *ID of the current TMC service.
     */
    private int mySID;

    /**
     * If a new part of a TMC multigroup-messages
     * comes in with this continuityIndex, it contains
     * additional detail for {@link #myLastMsg}.<br/>
     * It must be ignored if the {@link #myLastMsgSequenceIndex}
     * indicates that we missed a message.
     */
    private int myLastMsgContinuityIndex;
    /**
     * If a new part of a TMC multigroup-messages
     * comes in with the {@link #myLastMsgContinuityIndex}, it contains
     * additional detail for {@link #myLastMsg}.<br/>
     * It must be ignored if the {@link #myLastMsgSequenceIndex}
     * indicates that we missed a message.
     */
    private int myLastMsgSequenceIndex;

    /**
     * Last multigroup message. In case additional
     * groups for this message arrive.
     */
    private TMCEventFactory.TMCEvent myLastMsg;

    /**
     * The NavigationManager we use as a reference to our
     * map and current location after being initialized.
     */
    private static NavigationManager myNavigationManager;

    /**
     * We will not handle messages until initialized.
     * @param aNavigationManager our reference to the current map.
     */
    public static void initialize(final NavigationManager aNavigationManager) {
        myNavigationManager = aNavigationManager;
    }
    /**
     * {@inheritDoc}
     * @see org.openstreetmap.travelingsalesman.trafficblocks.tmc.TMCfromNMEAFilter.TMCListener#tmcBlockReceived(byte[], int, int)
     * @param block1 the first block
     * @param block2 the first block
     * @param block3 the first block
     * @param block4 the first block
     */
    @Override
    public void tmcBlockReceived(final int aBlock1,
                                 final int aBlock2,
                                 final int aBlock3,
                                 final int aBlock4) {
        final int blockMask      = 0xffff;
        final int block1 = aBlock1 & blockMask;
        final int block2 = aBlock2 & blockMask;
        final int block3 = aBlock3 & blockMask;
        final int block4 = aBlock4 & blockMask;
        final int groupCodeMask  = 0xF000; // 12-15
        final int groupCodeShift = 12;
        final int b0Mask         = 0x1000;
        final int tpMask         = 0x800;
        final int ptyMask        = 0x3e0;
        final int ptyShift       = 5;
        final int xMask          = 0x1f; //X4-X0
        final int xShift         = 0;

        int     piCode = block1;
        int     groupCode =  ((block2 & groupCodeMask) >> groupCodeShift);
        boolean b0        =  ((block2 & b0Mask) != 0);
        boolean tp        =  ((block2 & tpMask) != 0);
        int     pty       =  ((block2 & ptyMask) >> ptyShift);
        int     x         =  ((block2 & xMask)   >> xShift);
        final int groupCode0A = 0;
        final int groupCode1A = 1;
        final int groupCode2A = 2;
        final int groupCode3A = 3;
        final int groupCode4A = 4;
        final int groupCode5A = 5;
        final int groupCode6A = 6;
        final int groupCode7A = 7;
        final int groupCode8A = 8;
        final int groupCode9A = 9;
        final int groupCode10A = 10;
        final int groupCode15A = 15;
        String pi = Integer.toHexString(piCode);

        LOG.info("TMC-message received: "
                + Integer.toString(block1, 2)
                + "(" + Integer.toHexString(block1) + ")"
                + "-"
                + Integer.toString(block2, 2)
                + "(" + Integer.toHexString(block2) + ")"
                + "-"
                + Integer.toString(block3, 2)
                + "(" + Integer.toHexString(block3) + ")"
                + "-"
                + Integer.toString(block4, 2)
                + "(" + Integer.toHexString(block4) + ") "
                + "groupCode=" + groupCode + " "
                + "PI=" + pi);

        // Block A always carries the PI
        // = program identifier
        if (LOG.isLoggable(Level.FINE)) {
            decodePI(pi);
        }

        if (groupCode == groupCode0A) {
            LOG.finer("Group is 0A");
            handleGroup0A(block2, block3, block4, x);
        } else if (groupCode == groupCode1A) {
            LOG.finer("Group is 1A - extended country code or emergency warning system (unhandled)");
            // variant 7 = emergency warning system
            /*
             * Extended country codes (ECC) may optionally be transmitted in Type 1A groups
             * ( the last eight bits in Block 3 of Variant 0) to render the country/area
             * identification in bits 1 to 4 of the PI code unique.
             */
        } else if (groupCode == groupCode2A) {
            LOG.finer("Group is 2A - radio text (unhandled)");
        } else if (groupCode == groupCode3A) {
            LOG.finer("Group is 3A");
            handleGroup3A(b0, tp, pty, x, block3, block4, pi.charAt(0));
        } else if (groupCode == groupCode4A) {
            LOG.finer("Group is 4A - date and time (unhandled)");
        } else if (groupCode == groupCode5A) {
            LOG.finer("Group is 5A - transparent data channel (unhandled)");
        } else if (groupCode == groupCode6A) {
            LOG.finer("Group is 6A - inhouse application (unhandled)");
        } else if (groupCode == groupCode7A) {
            LOG.finer("Group is 7A - radio paging (unhandled)");
        } else if (groupCode == groupCode8A) {
            LOG.finer("Group is 8A");
            handleGroup8A(b0, tp, pty, x, block3, block4);
        } else if (groupCode == groupCode9A) {
            LOG.finer("Group is 9A - emergency warning system (unhandled)");
        } else if (groupCode == groupCode10A) {
            LOG.finer("Group is 10A - program type name (unhandled)");
            // see ftp://ftp.rds.org.uk/pub/acrobat/rbds.pdf
        } else if (groupCode == groupCode15A) {
            LOG.finer("Group is 15A - Fast basic tuning and switching information");
            boolean c0 = (block2 & 1) == 1;
            handleGroup15A(c0, block3, block4);
        } else {
            LOG.info("Group is " + groupCode + "A - unhanded group");
            // http://www.qsl.net/dk7in/RDS.html
            // 2 => Radiotext
            // 4 => clocktime
        }
    }

    /**
     * Handle group 15A-mesasages. (Station-Name as an alternative
     * to group 0A.)
     * See ftp://ftp.rds.org.uk/pub/acrobat/rbds.pdf for details.
     * @param aC0 first bit of block 2
     * @param aBlock3 block 3 = characters 0+1/4+5
     * @param aBlock4 block 3 = characters 2+3/6+7
     */
    private void handleGroup15A(final boolean aC0, final int aBlock3, final int aBlock4) {
        final int bytelen = 8;
        final int byteMask = 0xFF;
        byte[] ascii = new byte[]{
                (byte) (aBlock3 >> bytelen) , (byte) (aBlock3 & byteMask),
                (byte) (aBlock4 >> bytelen) , (byte) (aBlock4 & byteMask)};
        CharBuffer decoded = CHARSET.decode(ByteBuffer.wrap(ascii));

        LOG.info("Group 15A radio station name: c0=" + aC0 + ", 4 characters="
                + decoded.toString()
                + " (0x" + Integer.toHexString(ascii[0] & byteMask)
                + Integer.toHexString(ascii[1] & byteMask)
                + Integer.toHexString(ascii[2] & byteMask)
                + Integer.toHexString(ascii[3] & byteMask)+ ")");
        //c0=false => characters 0-4
        //c0=true =>  characters 5-7

        if (aC0) {
            
        } else {
            
        }
    }
    /**
     * Decode and log group 0A -messages.
     * (station-name, alternative frequencies,...)
     * @param block2 second of the 4 RDS-blocks
     * @param block3 third of the 4 RDS-blocks
     * @param block4 last of the 4 RDS-blocks
     * @param x the x-bits of block 2
     */
    private void handleGroup0A(final int block2, final int block3,
            final int block4, final int x) {
        // groupCode=0 => stationName (last Byte of block1=index, Block4=2 characters)
        final int bytelen = 8;
        final int byteMask = 0xFF;
        final int x4StationName = 4;
        final int x5StationName = 5;
        final int x6AlternativeFreq = 6;

        if (x == x4StationName || x == x5StationName) {
            int offset = (block2 & byteMask);
            byte[] ascii = new byte[]{(byte) (block4 >> bytelen) , (byte) (block4 & byteMask)};
            CharBuffer decoded = CHARSET.decode(ByteBuffer.wrap(ascii));
            LOG.info("Group 0A radio station name: x=" + x + " index=" + offset + ", 2 characters=" + decoded.toString()
                    + " (0x" + Integer.toHexString(ascii[0] & byteMask)
                    + Integer.toHexString(ascii[1] & byteMask) + ")");
            byte[] ascii2 = new byte[]{(byte) (block3 >> bytelen) , (byte) (block3 & byteMask)};
            CharBuffer decoded2 = CHARSET.decode(ByteBuffer.wrap(ascii2));
            LOG.info("Group 0A and x=4/5 => radio station name: test: 2 characters in block 3 are=" + decoded2.toString()
                    + " (0x" + Integer.toHexString(ascii2[0] & byteMask)
                    + Integer.toHexString(ascii2[1] & byteMask) + ")");
        } else if (x == x6AlternativeFreq) {
            int alternativeFrequency1 = (block3 & byteMask);
            int alternativeFrequency2 = (block3 >> bytelen);
            LOG.info("Group 0A and x=6 => Alternative RDS frequency:" + decodeAF(alternativeFrequency1) + " and " + decodeAF(alternativeFrequency2));
        } else {
            LOG.info("Group 0A x=" + x + " - unhandled X -code");
        }
    }

    /**
     * Decide an alternative frequency transmitted
     * by RDS capable radio-stations.
     * @param af the ac-code of 1 frequency (not 2)
     * @return a humap readable description
     */
    private String decodeAF(final int af) {
        final int afterLast = 250;
        final int emptySlot = 205;
        final int noAF = 224;
        final float minFreq = 87.6f;
        final float divider = 10.0f;
        if (af == 0) {
            return "not used";
        }
        if (af > 0 && af < emptySlot) {
            return (minFreq + (af / divider)) + " MHz";
        }
        if (af == emptySlot) {
            return "empty slot";
        }
        if (af > emptySlot && af < noAF) {
            return "not assigned";
        }
        if (af == noAF) {
            return "no alternative frequencies";
        }
        if (af > noAF && af < afterLast) {
            return (af - noAF) + " alternative frequencies following...";
        }
        if (af > afterLast) {
            return "not assigned";
        }
        return null;
    }
    /**
     * @param aPI the programme identifier as a hex-number
     */
    private void decodePI(final String aPI) {
        final int piLength = 4;
        String pi = aPI;
        while (pi.length() < piLength) {
            pi = "0" + pi;
        }

        //see http://www.ebu.ch/en/technical/metadata/specifications/rds_country_codes.php
        String piCountry = "(unknown country)";
        String piRange = "(unknown range)";
        if (pi.charAt(0) == '4') {
            piCountry = "(may be Switzerland, Vatican, ...)";
        } else if (pi.charAt(0) == 'a') {
            piCountry = "(may be Austria, ...)";
        } else if (pi.charAt(0) == 'd') {
            piCountry = "(German, OEffentlich-rechtlich)";
            if (pi.charAt(1) != '1' && pi.charAt(1) != '2') {
                switch(pi.charAt(2)) {
                case '1' : piCountry += "state=Bayern"; break;
                case '6' : piCountry += "state=Hessen"; break;
                case 'a' : piCountry += "state=Rheinland-Pfalz"; break;
                case '2' : piCountry += "(landesweit, zweites Programm)"; break;
                default : piCountry += "state=undefined code"; break;
                }//TODO: '0'
            } else {
                switch(pi.charAt(2)) {
                case '1' : piCountry += "state=BW"; break;
                case '2' : piCountry += "state=Bayern"; break;
                case '3' : piCountry += "state=Berlin"; break;
                case '4' : piCountry += "state=Brandenburg"; break;
                case '5' : piCountry += "state=Bremen+Bremerhaven"; break;
                case '6' : piCountry += "state=Hamburg"; break;
                case '7' : piCountry += "state=M-V"; break;
                case '8' : piCountry += "state=Niedersachsen"; break;
                case '9' : piCountry += "state=NRW"; break;
                case 'a' : piCountry += "state=Rheinland-Pfalz"; break;
                case 'b' : piCountry += "state=Saarland"; break;
                case 'c' : piCountry += "state=Sachsen"; break;
                case 'd' : piCountry += "state=Sachsen-Anhalt"; break;
                case 'e' : piCountry += "state=Schleswig-Holstein"; break;
                case 'f' : piCountry += "state=Tueringen"; break;
                default: piCountry += "state=n/a";
                }
            }
        } else if (pi.charAt(0) == '1') {
            piCountry = "(German, Privatsender)";
            switch(pi.charAt(2)) {
            case '1' : piCountry += "state=BW"; break;
            case '2' : piCountry += "state=Bayern"; break;
            case '3' : piCountry += "state=Berlin"; break;
            case '4' : piCountry += "state=Brandenburg"; break;
            case '5' : piCountry += "state=Bremen+Bremerhaven"; break;
            case '6' : piCountry += "state=Hamburg"; break;
            case '7' : piCountry += "state=M-V"; break;
            case '8' : piCountry += "state=Niedersachsen"; break;
            case '9' : piCountry += "state=NRW"; break;
            case 'a' : piCountry += "state=Rheinland-Pfalz"; break;
            case 'b' : piCountry += "state=Saarland"; break;
            case 'c' : piCountry += "state=Sachsen"; break;
            case 'd' : piCountry += "state=Sachsen-Anhalt"; break;
            case 'e' : piCountry += "state=Schleswig-Holstein"; break;
            case 'f' : piCountry += "state=Tueringen"; break;
            default: piCountry += "state=n/a";
            }
        }
        switch (pi.charAt(1)) {
        case '0' : piRange = "(local)"; break;
        case '1' : piRange = "(international)"; break;
        case '2' : piRange = "(national)"; break;
        case '3' : piRange = "(supranational)"; break;
        default : piRange = "(regional " + pi.charAt(1) + ")"; break;
        }
        LOG.fine("piCode=" + pi + "= " +  piCountry + "-" + piRange);
    }

    /**
     * Handle messages of type 3A.
     * @param aB0   the B0-bit
     * @param aTp   the Tp-bit
     * @param aPty  the Pty-value
     * @param aX    the group-specific bits of block2
     * @param aBlock3 third block of the 4 blocks of a tmc-message
     * @param aBlock4 fourth block of the 4 blocks of a tmc-message
     * @param aCCD   the first 4 bits of the PI
     */
    private void handleGroup3A(
            final boolean aB0,
            final boolean aTp,
            final int aPty,
            final int aX,
            final int aBlock3,
            final int aBlock4,
            final char aCCD) {
        final int y15Mask = 0x8000;
        final int y14Mask = 0x4000;
        final int y13Mask = 0x2000;
        final int y12Mask = 0x1000;
        final int y5Mask = 0x0010;
        final int y3Mask = 0x0008;
        final int y2Mask = 0x0004;
        final int y1Mask = 0x0002;
        final int y0Mask = 0x0001;
        final int gapMask = y12Mask | y13Mask;
        final int gapShift = 12;
        final int y4Mask = 0xF;
        final int ltnMask = 0xFC0;
        final int ltnShift = 6;
        final int sidMask = 0xFE0; // Y11-Y5
        final int sidShift = 5;

        LOG.finer("Group 3A\n"
                + "\tblock3=" + Integer.toBinaryString(aBlock3) + "\n"
                + "\tblock4=" + Integer.toBinaryString(aBlock4));
        boolean y15 = (aBlock3 & y15Mask) != 0;
        boolean y14 = (aBlock3 & y14Mask) != 0;
        boolean y5 = (aBlock3 & y5Mask) != 0;
        boolean y4 = (aBlock3 & y4Mask) != 0;
        boolean y3 = (aBlock3 & y3Mask) != 0;
        boolean y2 = (aBlock3 & y2Mask) != 0;
        boolean y1 = (aBlock3 & y1Mask) != 0;
        boolean y0 = (aBlock3 & y0Mask) != 0;

        // this is a message to tell us what location-table is used by this radio-station
        if (!y15 && !y14) {
            // LTN = Y11-Y5
            int locationTableNumber = (aBlock3 & ltnMask) >> ltnShift;
            LOG.fine("Group 3A - LocationTableNumber(LTN)=" + locationTableNumber);

            // AFI = Y5 (Alternate Frequency Indicator)
            // AFI=1 means that all radio-station sending this PI (programme identifier)
            //       carry the same TMC-Service
            LOG.finer("Group 3A - alternative frequency indicator (AFI)=" + y5);

            // M   = Y4 (Mode of transmission)
            this.myModeOfTransmission = y4; //(aBlock3 & y4Mask) != 0;
            if (this.myModeOfTransmission) {
                LOG.finer("Group 3A - enhanced gap-mode is used");
            } else {
                LOG.finer("Group 3A - basic gap-mode is used");
            }

            // MGS (Message Geographical Scope):
            // MGS: I = Y3 (International INTERROAD messages)
            //        = this station gives at least the major events in nearby countries
            LOG.finer("Group 3A - MSG - carries INTERROAD messages=" + y3);
            // MGS: N = Y2 (National)
            //        = station gives at least major events of all of the country
            LOG.finer("Group 3A - MSG - carries national messages=" + y2);
            // MGS: R = Y1 (Regional)
            //        = gives messages for all major roads in the region
            LOG.finer("Group 3A - MSG - carries regional messages=" + y1);
            // MGS: U = Y0 (Urban)
            //        = covers at least all major axes, ring roads of the area.
            LOG.finer("Group 3A - MSG - carries urban messages=" + y0);

            this.myLocationTableNumber  = locationTableNumber;
            int newCountryID = -1;
            //this.myNavigationManager
            switch(aCCD) {
            case 'd' :
                newCountryID = 58; //TODO: check location, this may be Lybia or Serbia too
                   break;
            default:
                newCountryID = -1;
            }
            if (this.myCountryID != newCountryID) {
                reset();
            }
            this.myCountryID = newCountryID;

        } else if (!y15 && y14) {
            // SID in Y11-Y5  (Service identifier)
            int newSID = (aBlock3 & sidMask) >> sidShift;
            if (this.mySID != newSID) {
                reset();
            }
            this.mySID = newSID;
            LOG.finer("Service Identifier = TMC operator (SID)=" + this.mySID);
            //     SID=0 general service
            //     else SID = unique(within Country Code CC and extended Country Code ECC)
            //                identifier within this nation

            // G   in Y13-Y12 (Gap parameter)
            this.myGap = (aBlock3 & gapMask) >> gapShift;

//            if (this.myModeOfTransmission) {
//                // see ISO14819-1 section 7.5.2.3.2
//                // Td = Y0-Y1 (delay time)
//                // Tw = Y2-Y3 (window time)
//                // Ta = Y3-Y4 (activity time)
//            } else {
//                // Y0-Y4 = reserved for future use
//            }
        } else {
            LOG.finer("unknown 3A-message y15=" + y15 + " y14=" + y14);
        }
    }

    /**
     * Reset station-specific values after a
     * change in country or TMC-service is detected.
     */
    private void reset() {
        this.myLocationTableNumber = -1;
        this.myModeOfTransmission = false;
        this.myGap = 0;
        this.myTMCServiceProviderName = new char[] {'?', '?', '?', '?', '?', '?', '?', '?'};
    }
    /**
     * Handle messages of type 8A (traffic-messages).
     * @param aB0   the B0-bit
     * @param aTp   the Tp-bit
     * @param aPty  the Pty-value
     * @param aX    (X4-X0) the group-specific bits of block2
     * @param aBlock3 third block of the 4 blocks of a tmc-message
     * @param aBlock4 fourth block of the 4 blocks of a tmc-message
     */
    protected void handleGroup8A(final boolean aB0,
                               final boolean aTp,
                               final int aPty,
                               final int aX,
                               final int aBlock3,
                               final int aBlock4) {
        // x = block2 (x0=lsb)
        // y = block3 (y0=lsb)
        // z = block4 (z0=lsb)

        final int y15Mask = 0x8000;
        final int y14Mask = 0x4000;
        final int y13Mask = 0x2000;
        final int y12Mask = 0x1000;
        final int x4Mask = 16;
        final int x3Mask = 8;
        final int x2Mask = 4;
        final int x1Mask = 2;
        final int x0Mask = 1;

        LOG.fine(" x= " +  Integer.toString(aX, 2) + "\n"
               + " x4= " +  ((aX & x4Mask) != 0) + " (true=tuning info or future use)\n"
               + " x3= " +  ((aX & x3Mask) != 0) + " (true=single group message)\n"
               + " y15= " +  ((aBlock3 & y15Mask) != 0) + " (true=first block of multi group message)");
        if ((aX & x4Mask) == 1) {
            handleGroup8Atuning(aX, aBlock3, aBlock4);
            return;
        }
        if ((aX & x3Mask) == 0) {
            LOG.fine("type(A8) multiGroup-message");
            // is multigroup-message

            //x2-x0 = continuity index
            final int continuityIndexMask = x0Mask | x1Mask | x2Mask;
            final int groupSequenceIndicatorMask = y12Mask | y13Mask;
            final int groupSequenceIndicatorShift = 12;
//            final int secondGroupMask = y14Mask;

            // check if we are continuing the same message
            int continuityIndex = aX & continuityIndexMask;
            int groupSequenceIndicator = (aBlock3 & groupSequenceIndicatorMask) >> groupSequenceIndicatorShift;
            if (continuityIndex == 0) {
                // this is a service-message.
                LOG.fine("type(A8) service-message");
                return;
            }
            if (continuityIndex == continuityIndexMask) {
                // this is a service-message.
                LOG.fine("type(A8) reserved message");
                return;
            }
            if (this.myLastMsgContinuityIndex != continuityIndex) {
                this.myLastMsgContinuityIndex = continuityIndex;
                this.myLastMsg = null;
            }
            LOG.fine("type(A8) multiGroup-message - groupSequenceIndicator=" + groupSequenceIndicator + " continuityIndex=" + continuityIndex);

            if ((aBlock3 & y15Mask) != 0) {

                LOG.fine("type(A8) multiGroup-message - first block");
                this.myLastMsgSequenceIndex = groupSequenceIndicator;
                // first group of a multigroup-message
                // encoding as per ISO14819-1 section 7.6
                // x = block2 (x0=lsb)
                // y = block3 (y0=lsb)
                // z = block4 (z0=lsb)
                // Continuity Index = X2, X1, X0
                // direction = y14

                // except for the duration, multigroup-messages arre
                // encoded alike in the first block
                TMCEvent tmcEvent = parseTMCEvent(aBlock3, aBlock4, 0);
                this.myLastMsg = tmcEvent;

//                String untilStr = "none";
//                if (tmcEvent != null) {
//                    untilStr = DateFormat.getTimeInstance().format(tmcEvent.getValidUntil());
//                }
                String tmcEventStr = "null";
                if (tmcEvent != null) {
                    tmcEventStr = tmcEvent.getEventName();
                }
                LOG.fine("type(A8) multiGroup-message: \n"
                      // + " duration= " +  Integer.toString(duration, 2) + "=" + untilStr + "\n"
//                       + " location= " +  Integer.toString(location, 2) + "=" + location + "\n"
                       + " event   = " /*+  Integer.toString(event,    2) + "=" + event*/ + " (" + tmcEventStr + ")\n"
                       /*+ " extend  = " +  Integer.toString(extend,   2) + "=" + extend + " steps in direction " + direction + " true=negative"*/);

                if (tmcEvent != null) {
                    TrafficMessageStore.getInstance().addMessage(tmcEvent);
                }
            } else {
                if (myLastMsg == null) {
                    return;
                }
//                boolean isSecondGroup = (aBlock3 & secondGroupMask) != 0;
                // subsequent group
                //TODO log some real messages to understand the groupSequenceIndicator
                //TODO: implement the combining of additional groups
                // 4bit labels defining the content (ISO 14819-1 section 5.5)
                // each label is followed by 0-16 bits of data
                // ...this is an ugly part of the standard
                // payload = ((aBlock3 & Y11-Y0) <<16) | aBlock4
            }
        }

        // type(A8) singleGroup-message
        if ((aX & x3Mask) != 0 && (aX & x4Mask) == 0) {
            // encoding as per ISO14819-1 section 5.3
            // x = block2 (x0=lsb)
            // y = block3 (y0=lsb)
            // z = block4 (z0=lsb)
            // duration  = x2 (msb) - x0(lsb)
            final int durationMask = 0x7;
            int duration      = (aX & durationMask);

            TMCEvent tmcEvent = parseTMCEvent(aBlock3, aBlock4, duration);
            String untilStr = "none";
            if (tmcEvent != null) {
                untilStr = DateFormat.getTimeInstance().format(tmcEvent.getValidUntil());
            }
            String tmcEventStr = "null";
            if (tmcEvent != null) {
                tmcEventStr = tmcEvent.getEventName();
            }
            LOG.fine("type(A8) singleGroup-message: \n"
                   + " duration= " +  Integer.toString(duration, 2) + "=" + untilStr + "\n"
//                   + " location= " +  Integer.toString(location, 2) + "=" + location + "\n"
                   + " event   = " /*+  Integer.toString(event,    2) + "=" + event*/ + " (" + tmcEventStr + ")\n"
                   /*+ " extend  = " +  Integer.toString(extend,   2) + "=" + extend + " steps"*/);
            if (tmcEvent != null) {
                TrafficMessageStore.getInstance().addMessage(tmcEvent);
            }
        }
    }

    /**
     * Parse a given single-group event or the first group of a multigroup event.<br/>
     * Side effect: the event is added to the {@link TrafficMessageStore}.
     * @param aBlock3 third block of the 4 blocks of a tmc-message
     * @param aBlock4 fourth block of the 4 blocks of a tmc-message
     * @param duration decoded duration (give 0 for multigroup).
     * @return null or the created event.
     */
    private TMCEvent parseTMCEvent(final int aBlock3, final int aBlock4, final int duration) {

        // diversion = y15
        //final int diversionMask = 0x8000;
        //boolean diversion = (aBlock3 & diversionMask) != 0;

        // direction = y14
        final int pnMask        = 0x4000;
        boolean direction       = (aBlock3 & pnMask) != 0;

        // extend    = y13(msb) - y11(lsb)
        final int extendMask    = 0x3800;
        final int extendShift   = 11;
        int extend      = ((aBlock3 & extendMask) >> extendShift);

        // location  = z15(msb) - z0 (lsb) = block4
        int location = aBlock4;
        // TABCD is given by the last group 3A-message, CID if given by current location and COUNTRIES.DAT (that maps CCD(e.g. D) from PI to CID(e.g. 58)
        int cid   = myCountryID;
        int tabcd = myLocationTableNumber;
        NavigationManager manager = myNavigationManager;
        Collection<TMCLocation> osmLocation = null;
        if (manager != null && cid != -1 && tabcd != -1) {
            osmLocation = TrafficMessageStore.getInstance()
                    .getLocation(cid, tabcd, location, direction, extend, manager.getMapForRouters(), false/*TODOtmcEvent.getBothDirectionsAffected()*/);
        }

        // event     = y10(msb) - y0 (lsb) = block3
        final int eventMask     = 0x7FF; //11111111111
        int event       =   (aBlock3 & eventMask);
        TMCEvent tmcEvent = null;
        if (osmLocation != null && osmLocation.size() > 0) {
            tmcEvent = myTMCEventFactory.createEvent(location, cid, tabcd, event, duration, osmLocation.iterator().next().getEntity(), extend, direction);
        } else {
            //DEBUG
            tmcEvent = myTMCEventFactory.createEvent(location, cid, tabcd, event, duration, TrafficMessageStore.UNKNOWNLOCATION, extend, direction);
        }
        return tmcEvent;
    }
    /**
     * Handle ISO14819-1 section 7.5.3.2 tuning information (X4==1 in group 8A-messages).
     * @param aX    (X4-X0) the group-specific bits of block2
     * @param aBlock3 third block of the 4 blocks of a tmc-message
     * @param aBlock4 fourth block of the 4 blocks of a tmc-message
     */
    private void handleGroup8Atuning(final int aX, final int aBlock3, final int aBlock4) {
        final int x0mask = 1;
        final int x1mask = 2;
        final int x2mask = 4;
        final int x3mask = 8;

        final int bytelen = 8;
        final int byteMask = 0xFF;

        final int variantMask = x0mask | x1mask | x2mask | x3mask;
        final int variantName1 = 4;
        final int variantName2 = 5;
        final int variantAlternateFrequency = 6;
        final int variantMappedFrequencyPair = 7;
        final int variantAlternatePI = 8;
        final int variantOtherNetwork = 9;

        int variant = aX & variantMask;
        // handle ISO14819-1 section 7.5.3.2 tuning information (x4=1)
        switch (variant) {
        case variantName1 :
            byte[] ascii = new byte[]{(byte) (aBlock3 >> bytelen) , (byte) (aBlock3 & byteMask),
                                      (byte) (aBlock4 >> bytelen) , (byte) (aBlock4 & byteMask)};
            CharBuffer decoded = CHARSET.decode(ByteBuffer.wrap(ascii));
            int i = 0;
            this.myTMCServiceProviderName[i] = decoded.charAt(i); i++;
            this.myTMCServiceProviderName[i] = decoded.charAt(i); i++;
            this.myTMCServiceProviderName[i] = decoded.charAt(i); i++;
            this.myTMCServiceProviderName[i] = decoded.charAt(i); i++;
            break;
        case variantName2 :
            byte[] ascii2 = new byte[]{(byte) (aBlock3 >> bytelen) , (byte) (aBlock3 & byteMask),
                                       (byte) (aBlock4 >> bytelen) , (byte) (aBlock4 & byteMask)};
            CharBuffer decoded2 = CHARSET.decode(ByteBuffer.wrap(ascii2));
            int j = 0;
            int offset = 2 * 2; // 2 blocks of 2 characters each in variantName1
            this.myTMCServiceProviderName[offset + j] = decoded2.charAt(j);  j++;
            this.myTMCServiceProviderName[offset + j] = decoded2.charAt(j); j++;
            this.myTMCServiceProviderName[offset + j] = decoded2.charAt(j); j++;
            this.myTMCServiceProviderName[offset + j] = decoded2.charAt(j); j++;
            LOG.fine("TMC provider-name='" + new String(this.myTMCServiceProviderName) + "'");
            break;
        case variantAlternateFrequency :
            break; //TODO
        case variantMappedFrequencyPair :
            break; //TODO
        case variantAlternatePI :
            break; //TODO
        case variantOtherNetwork :
            break; //TODO
        default: return; // variants 0-3 and 10-15 are reserved for future use
        }
    }

}
