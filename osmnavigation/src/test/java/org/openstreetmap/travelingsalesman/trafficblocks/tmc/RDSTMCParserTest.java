/**
 * 
 */
package org.openstreetmap.travelingsalesman.trafficblocks.tmc;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import junit.framework.TestCase;

/**
 * Test-cases for basic TMC-parsing.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class RDSTMCParserTest extends TestCase {

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test method for {@link org.openstreetmap.travelingsalesman.trafficblocks.tmc.RDSTMCParser#tmcBlockReceived(short, short, short, short)}.
     */
    public void testTmcBlockReceived() {
        RDSTMCParser subject = new RDSTMCParser();
        /**
         * TMC-MESSAGE-Single
         * 01
         * Herkunft-PI-Code:1101100100000100
         * Location:0011000011101010
         * Event:00010000011
         * Extent:010
         * Direction:0 (pn)
         * Diversion advise:0
         * Duration:100
         * 1101100100000100 | 1000010100101000 | 0010000010000011 | 0011000011101010
         * 1101100100000100 | 1000010100101000 | d=0 pn=0 extend=100 00010000011 | 0011000011101010
         * D904 | 8528 | 2083 | 30EA
         */
        final short case1block1 = (short) 0xD904;
        final short case1block2 = (short) 0x8528;
        final short case1block3 = (short) 0x2083;
        final short case1block4 = (short) 0x30EA;
        subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
        //TODO: assertions
    }

    /**
     * Test method for {@link org.openstreetmap.travelingsalesman.trafficblocks.tmc.RDSTMCParser#tmcBlockReceived(short, short, short, short)}.
     */
    public void testRDSBlockReceived() {
        setupLogging();

        RDSTMCParser subject = new RDSTMCParser();
//        { // Group 0A x=15 - unhandled X -code
//            final short case1block1 = (short) 0xD608;
//            final short case1block2 = (short) 0x042F;
//            final short case1block3 = (short) 0x8881;
//            final short case1block4 = (short) 0x474E;
//            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
//        }
//        { // Group 0A x=15 - unhandled X -code
//            final short case1block1 = (short) 0xD608;
//            final short case1block2 = (short) 0x042F;
//            final short case1block3 = (short) 0x9981;
//            final short case1block4 = (short) 0x5245;
//            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
//        }
//        { // Group 0A x=21 - unhandled X -code
//            final short case1block1 = (short) 0xD608;
//            final short case1block2 = (short) 0x0515;
//            final short case1block3 = (short) 0x1A4F;
//            final short case1block4 = (short) 0xEB2D;
//            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
//        }
//        {// FEIN: piCode=d210= (German, OEffentlich-rechtlich)state=BW-(national)
        // FEINER: Group is 2A - radio text (unhandled)
//            final short case1block1 = (short) 0xD210;
//            final short case1block2 = (short) 0x24F8;
//            final short case1block3 = (short) 0x2020;
//            final short case1block4 = (short) 0x2020;
//            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
//        }
//        { // FEINER: Group is 6A - inhouse application (unhandled)
//            final short case1block1 = (short) 0xD210;
//            final short case1block2 = (short) 0x64FF;
//            final short case1block3 = (short) 0x0707;
//            final short case1block4 = (short) 0x2008;
//            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
//        }
        {//multigroup-message, groupSequenceIndicator=3
            final short case1block1 = (short) 0xD301;
            final short case1block2 = (short) 0x8525;
            final short case1block3 = (short) 0x7281;
            final short case1block4 = (short) 0x88E7;
            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
        }
        {   // FEIN: type(A8) multiGroup-message - groupSequenceIndicator=0 continuityIndex=6
            final short case1block1 = (short) 0xD301;
            final short case1block2 = (short) 0x8526;
            final short case1block3 = (short) 0x0080;
            final short case1block4 = (short) 0x0000;
            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
        }
        {//???
         //location: POINTS.DAT 58;1;39816;P;3;1;;;18996;;408;;7103;;0;0;0;0;1;1;;;+00700175;+5144700;0;0
            // y15= true (true=first block of multi group message)
            // type(A8) multiGroup-message - groupSequenceIndicator=0 continuityIndex=3
            //  event   =  ("Richtungsfahrbahn gesperrt")
            final short case1block1 = (short) 0xD301;
            final short case1block2 = (short) 0x8523;
            final short case1block3 = (short) 0xC298;
            final short case1block4 = (short) 0x9B88;
            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
        }
//        {//0A x=15 - unhandled X -code
//            final short case1block1 = (short) 0xD301;
//            final short case1block2 = (short) 0x052F;
//            final short case1block3 = (short) 0x113C;
//            final short case1block4 = (short) 0x5720;
//            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
//        }
//        {//Group is 2A - radio text (unhandled)
//            final short case1block1 = (short) 0xD301;
//            final short case1block2 = (short) 0x25F5;
//            final short case1block3 = (short) 0x7320;
//            final short case1block4 = (short) 0x3137;
//            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
//        }
//        {//Group 0A x=12 - unhandled X -code
//            final short case1block1 = (short) 0xD301;
//            final short case1block2 = (short) 0x052C;
//            final short case1block3 = (short) 0x1141;
//            final short case1block4 = (short) 0x5357;
//            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
//        }
//        {//Group is 11A - unhanded group
//            final short case1block1 = (short) 0xD301;
//            final short case1block2 = (short) 0xB524;
//            final short case1block3 = (short) 0xFB71;
//            final short case1block4 = (short) 0x3402;
//            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
//        }
//        {//Group is 2A - radio text (unhandled)
//            final short case1block1 = (short) 0xD301;
//            final short case1block2 = (short) 0x252B;
//            final short case1block3 = (short) 0x11C3;
//            final short case1block4 = (short) 0x328D;
//            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
//        }
//        {//Group is 14A - unhanded group
//            final short case1block1 = (short) 0xD301;
//            final short case1block2 = (short) 0xE53D;
//            final short case1block3 = (short) 0x4800;
//            final short case1block4 = (short) 0xDB04;
//            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
//        }
        {// type(A8) multiGroup-message - groupSequenceIndicator=2 continuityIndex=5
            final short case1block1 = (short) 0xD301;
            final short case1block2 = (short) 0x8525;
            final short case1block3 = (short) 0x2E96;
            final short case1block4 = (short) 0x4441;
            subject.tmcBlockReceived(case1block1, case1block2, case1block3, case1block4);
        }
        //TODO: assertions
    }

    /**
     * Add logging for level FINE.
     */
    protected static void setupLogging() {
        Logger.getLogger("").setLevel(Level.ALL);
        Logger.getLogger("sun").setLevel(Level.WARNING);
        Logger.getLogger("com.sun").setLevel(Level.WARNING);
        Logger.getLogger("java").setLevel(Level.WARNING);
        Logger.getLogger("javax").setLevel(Level.WARNING);
        Logger.getLogger(RDSTMCParser.class.getName()).setLevel(Level.FINER);
        Logger rootLogger = Logger.getLogger("");

        // Remove any existing handlers.
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // add a debug-file
//        try {
//            Handler fileHandler = new FileHandler("e:\\libosm.osmbin.debug.log");
//            fileHandler.setLevel(Level.FINEST);
//            fileHandler.setFormatter(new SimpleFormatter());
//            Logger.getLogger("org.openstreetmap.osm.data.osmbin").addHandler(fileHandler);
//        } catch (Exception e) {
//            System.err.println("Could not create debug-log for tracing osmbin");
//            e.printStackTrace();
//        }

        // Add a new console handler.
        Handler consoleHandler = new StdoutConsoleHandler();
        consoleHandler.setLevel(Level.FINER);
        rootLogger.addHandler(consoleHandler);
    }

    /**
     * Test method for {@link org.openstreetmap.travelingsalesman.trafficblocks.tmc.RDSTMCParser#handleGroup8A(boolean, boolean, short, short, short, short)}.
     */
    public void testHandleGroup8A() {
    //TODO    fail("Not yet implemented");
    }

    /**
     * This <tt>Handler</tt> publishes log records to <tt>System.out</tt>.
     * By default the <tt>SimpleFormatter</tt> is used to generate brief summaries.
     * <p>
     * <b>Configuration:</b>
     * By default each <tt>ConsoleHandler</tt> is initialized using the following
     * <tt>LogManager</tt> configuration properties.  If properties are not defined
     * (or have invalid values) then the specified default values are used.
     * <ul>
     * <li>   java.util.logging.ConsoleHandler.level
     *    specifies the default level for the <tt>Handler</tt>
     *    (defaults to <tt>Level.INFO</tt>).
     * <li>   java.util.logging.ConsoleHandler.filter
     *    specifies the name of a <tt>Filter</tt> class to use
     *    (defaults to no <tt>Filter</tt>).
     * <li>   java.util.logging.ConsoleHandler.formatter
     *    specifies the name of a <tt>Formatter</tt> class to use
     *        (defaults to <tt>java.util.logging.SimpleFormatter</tt>).
     * <li>   java.util.logging.ConsoleHandler.encoding
     *    the name of the character set encoding to use (defaults to
     *    the default platform encoding).
     * </ul>
     * <p>
     * @version 1.13, 11/17/05
     * @since 1.4
     */

    public static class StdoutConsoleHandler extends StreamHandler {
        /**
         *  Private method to configure a ConsoleHandler.
         */
        private void configure() {
        setLevel(Level.INFO);
        setFilter(null);
        setFormatter(new SimpleFormatter());
        }

        /**
         * Create a <tt>ConsoleHandler</tt> for <tt>System.err</tt>.
         * <p>
         * The <tt>ConsoleHandler</tt> is configured based on
         * <tt>LogManager</tt> properties (or their default values).
         */
        public StdoutConsoleHandler() {
        configure();
        setOutputStream(System.out);
        }

        /**
         * Publish a <tt>LogRecord</tt>.
         * <p>
         * The logging request was made initially to a <tt>Logger</tt> object,
         * which initialized the <tt>LogRecord</tt> and forwarded it here.
         * <p>
         * @param  record  description of the log event. A null record is
         *                 silently ignored and is not published
         */
        public void publish(final LogRecord record) {
        super.publish(record);
        flush();
        }

        /**
         * Override <tt>StreamHandler.close</tt> to do a flush but not
         * to close the output stream.  That is, we do <b>not</b>
         * close <tt>System.err</tt>.
         */
        public void close() {
        flush();
        }
    }

}
