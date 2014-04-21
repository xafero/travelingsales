/**
 * URLDownloader.java
 * created: 09.04.2009 18:53:27
 * (c) 2009 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of libosm by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  libosm is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  libosm is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with libosm.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.osm.io;


//automatically created logger for debug and error -output
import java.util.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
/*import org.openstreetmap.osmosis.core.migrate.MigrateV05ToV06;*/ // TODO: Check migrate!
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.v0_6.impl.OsmHandler;


/**
 * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * URLDownloader.java<br/>
 * created: 09.04.2009 18:53:27 <br/>
 *<br/><br/>
 * <b>Osmosis-source that reads XML from a URL!</b>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class URLDownloader implements RunnableSource {

    /**
     * Retry at most this many times after a 501.
     */
    private static final int MAXRETRYCOUNT = 3;


    /**
     * Sleep this many milliseconds before retrying
     * after a 501.
     */
    private static final int RETRYSLEEP = 30000;


    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(URLDownloader.class
            .getName());


//    /**
//     * The http-response-code for OK.
//     */
//    private static final int RESPONSECODE_OK = 200;


    /**
     * My logger for debug- and error-output.
     */
    private static Logger log = Logger.getLogger(URLDownloader.class.getName());


//    /**
//     * The timeout we use for the  HttpURLConnection.
//     */
//    private static final int TIMEOUT = 15000;


    /**
     * Where to deliver the loaded data.
     */
    private Sink mySink;



    /**
     * The url to download.
     */
    private URL myUrl;

    /**
     * The http connection used to retrieve data.
     */
    private HttpURLConnection myActiveConnection;

    /**
     * The stream providing response data.
     */
    private InputStream responseStream;

    /**
     * Creates a new instance with the specified url.
     *@param aUrl
     *            the url to download (eg.
     *            http://www.openstreetmap.org/api/0.6/relation/1234/full).
     */
    public URLDownloader(final URL aUrl) {
        this.myUrl = aUrl;
    }


    /**
     * {@inheritDoc}
     */
    public void setSink(final Sink aSink) {
        this.mySink = aSink;
    }

    /**
     * Cleans up any resources remaining after completion.
     */
    private void cleanup() {
        if (myActiveConnection != null) {
            try {
                myActiveConnection.disconnect();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unable to disconnect.", e);
            }
            myActiveConnection = null;
        }

        if (responseStream != null) {
            try {

                responseStream.close();
            } catch (IOException e) {
                log.log(Level.SEVERE, "Unable to close response stream.", e);
            }
            responseStream = null;
        }
    }


    /**
     * Creates a new SAX parser.
     *
     * @return The newly created SAX parser.
     */
    private SAXParser createParser() {
        try {
            return SAXParserFactory.newInstance().newSAXParser();

        } catch (ParserConfigurationException e) {
            throw new OsmosisRuntimeException("Unable to create SAX Parser.", e);
        } catch (SAXException e) {
            throw new OsmosisRuntimeException("Unable to create SAX Parser.", e);
        }
    }

    /**
     * We retry at most 3 times if we receive a 501.
     */
    private int myRetryCount = 0;

    /**
     * Reads all data from the server and send it to the {@link Sink}.
     */
    public void run() {
        try {
            SAXParser parser = createParser();
            InputStream inputStream = myUrl.openStream();


            try {
                // try to guess if it is an old 0.5 source and migrate
                if (myUrl.getPath().contains("/0.5/") && !myUrl.getPath().contains("/0.6/")) {
                    /*MigrateV05ToV06 migrate = new MigrateV05ToV06();
                    migrate.setSink(mySink);*/ // TODO: Check migrate!
                	Sink migrate = null;
                    parser.parse(inputStream, new org.openstreetmap.osmosis.xml.v0_6.impl.OsmHandler(migrate, true));
                } else {
                    parser.parse(inputStream, new OsmHandler(mySink, true));
                }
            } finally {
                inputStream.close();
                inputStream = null;
            }

            mySink.complete();

        } catch (SAXParseException e) {
            throw new OsmosisRuntimeException(
                    "Unable to parse xml"
                    + ".  publicId=(" + e.getPublicId()
                    + "), systemId=(" + e.getSystemId()
                    + "), lineNumber=" + e.getLineNumber()
                    + ", columnNumber=" + e.getColumnNumber() + ".",
                    e);
        } catch (SAXException e) {
            throw new OsmosisRuntimeException("Unable to parse XML.", e);
        } catch (IOException e) {
            if (myRetryCount < MAXRETRYCOUNT && e.getMessage().indexOf("Server returned HTTP response code: 501 ") != -1) {
                myRetryCount++;
                LOG.log(Level.WARNING, "Received responde-code 501. waiting 30 seconds and trying again.");
                try {
                    Thread.sleep(RETRYSLEEP);
                } catch (InterruptedException e1) {
                    LOG.log(Level.SEVERE, "[InterruptedException] Problem in "
                               + getClass().getName(),
                                 e1);
                }
                run();
            }
            throw new OsmosisRuntimeException("Unable to read XML.", e);
        } finally {
            mySink.release();

            cleanup();
        }
    }

//    /**
//     * Open a connection to the given url and return a reader on the input
//     * stream from that connection.
//     *
//     * @param pUrlStr
//     *            The exact url to connect to.
//     * @return An reader reading the input stream (servers answer) or
//     *         <code>null</code>.
//     * @throws IOException
//     *             on io-errors
//     */
//    private InputStream getInputStream(final String pUrlStr) throws IOException {
//        URL url;
//        int responseCode;
//        String encoding;
//
//        url = new URL(pUrlStr);
//        myActiveConnection = (HttpURLConnection) url.openConnection();
//
//        myActiveConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
//
//        responseCode = myActiveConnection.getResponseCode();
//
//        if (responseCode != RESPONSECODE_OK) {
//            String message;
//            String apiErrorMessage;
//
//            apiErrorMessage = myActiveConnection.getHeaderField("Error");
//
//            if (apiErrorMessage != null) {
//                message = "Received API HTTP response code " + responseCode
//                + " with message \"" + apiErrorMessage
//                + "\" for URL \"" + pUrlStr + "\".";
//            } else {
//                message = "Received API HTTP response code " + responseCode
//                + " for URL \"" + pUrlStr + "\".";
//            }
//
//            throw new OsmosisRuntimeException(message);
//        }
//
//        myActiveConnection.setConnectTimeout(TIMEOUT);
//
//        encoding = myActiveConnection.getContentEncoding();
//
//        responseStream = myActiveConnection.getInputStream();
//        if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
//            responseStream = new MultiMemberGZIPInputStream(responseStream);
//        } else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
//            responseStream = new InflaterInputStream(responseStream, new Inflater(true));
//        }
//
//        return responseStream;
//    }
}
