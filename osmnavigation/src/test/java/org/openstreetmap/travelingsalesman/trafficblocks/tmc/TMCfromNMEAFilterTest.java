/**
 * 
 */
package org.openstreetmap.travelingsalesman.trafficblocks.tmc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import org.openstreetmap.travelingsalesman.trafficblocks.tmc.TMCfromNMEAFilter.TMCListener;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class TMCfromNMEAFilterTest extends TestCase {

    /**
     * Counter used in {@link #testTestdataBin()}.
     */
    private static int myTestCountReceived = 0;

    /**
     * Read "testdata.bin" and parse it.
     * @throws IOException test failed
     */
    public void testTestdataBin() throws IOException {
        RDSTMCParserTest.setupLogging();
        InputStream testdata = getClass().getClassLoader().getResourceAsStream("org/openstreetmap/travelingsalesman/trafficblocks/tmc/gnctestdata.bin");
        BufferedReader subject = TMCfromNMEAFilter.createTMCFilter(testdata, null, new TMCListener() {

            @Override
            public void tmcBlockReceived(final int aBlock1,
                    final int aBlock2,
                    final int aBlock3,
                    final int aBlock4) {
                myTestCountReceived++;
                final int expectedBlock1Hex = 0xD904;
                final int expectedBlock1Dec = 55556;
                final int expectedBlock2Hex = 0x8528;
                final int expectedBlock2Dec = 34088;
                final int expectedBlock3Hex = 0x2083;
                final int expectedBlock3Dec = 8323;
                final int expectedBlock4Hex = 0x30E4;
                final int expectedBlock4Dec = 12516;
                assertEquals(expectedBlock1Hex, aBlock1);
                assertEquals(expectedBlock1Dec, aBlock1);
                assertEquals(expectedBlock2Hex, aBlock2);
                assertEquals(expectedBlock2Dec, aBlock2);
                assertEquals(expectedBlock3Hex, aBlock3);
                assertEquals(expectedBlock3Dec, aBlock3);
                assertEquals(expectedBlock4Hex, aBlock4);
                assertEquals(expectedBlock4Dec, aBlock4);
            }
        }, "testdata.bin");
        myTestCountReceived = 0;
        assertTrue(subject.read() < 0);
        assertEquals("messae in test-file not found by parser", 1, myTestCountReceived);
    }

    /**
     * Test method for {@link org.openstreetmap.travelingsalesman.trafficblocks.tmc.TMCfromNMEAFilter#decodeFrequencyCode(byte)}.
     */
    public void testDecodeFrequencyCode() {
        final byte code = (byte) 0x88;
        final BigDecimal expectedResult = new BigDecimal("101.1");
        assertTrue(expectedResult.equals(TMCfromNMEAFilter.decodeFrequencyCode(code)));
        assertTrue(expectedResult.equals(TMCfromNMEAFilter.decodeFrequencyCode(TMCfromNMEAFilter.encodeFrequencyCode(expectedResult))));
    }

    /**
     * Test method for {@link org.openstreetmap.travelingsalesman.trafficblocks.tmc.TMCfromNMEAFilter#decodeFrequencyCode(byte)}.
     */
    public void testDecodeFrequencyCode2() {
        final byte code = (byte) 0x08;
        final BigDecimal expectedResult = new BigDecimal("88.3");
        assertTrue(expectedResult.equals(TMCfromNMEAFilter.decodeFrequencyCode(code)));
        assertEquals(code, TMCfromNMEAFilter.encodeFrequencyCode(expectedResult));
        assertTrue(expectedResult.equals(TMCfromNMEAFilter.decodeFrequencyCode(TMCfromNMEAFilter.encodeFrequencyCode(expectedResult))));
    }

    /**
     * Test method for {@link org.openstreetmap.travelingsalesman.trafficblocks.tmc.TMCfromNMEAFilter#encodeFrequency(float)}.
     */
    public void testEncodeFrequencyCode() {
        final byte code = (byte) 0x88;
        final BigDecimal input = new BigDecimal("101.1");
        assertEquals(code, TMCfromNMEAFilter.encodeFrequencyCode(input));
    }

    /**
     * Test method for {@link org.openstreetmap.travelingsalesman.trafficblocks.tmc.TMCfromNMEAFilter#byteToHex(byte)}.
     */
    public void testByteToHex() {
        final int max = 0xFF;
        assertEquals("ff", TMCfromNMEAFilter.byteToHex((byte) max));
        assertEquals("0", TMCfromNMEAFilter.byteToHex((byte) 0x00));
    }

    /**
     * Test method for {@link org.openstreetmap.travelingsalesman.trafficblocks.tmc.TMCfromNMEAFilter#hexToByte(String)}.
     */
    public void testHexToByte() {
        final int max = 0xFF;
        assertEquals((byte) max, TMCfromNMEAFilter.hexToByte("FF"));
        assertEquals((byte) 0x00, TMCfromNMEAFilter.hexToByte("00"));
    }
}
