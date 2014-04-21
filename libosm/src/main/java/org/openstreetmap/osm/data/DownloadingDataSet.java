/**
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
 *
 *  Created: 09.11.2009
 */

package org.openstreetmap.osm.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.ConfigurationSetting;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.ConfigurationSetting.TYPES;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.io.DataSetSink;
import org.openstreetmap.osm.io.URLDownloader;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;


/**
 * TODO: This class in unfinished! Bounding-Box downloads are missing.
 * This dataset downloads whatever is asked from it
 * from the OpenStreetMap-server or the OSM-XAPI.
 * @see     
 * @see http://wiki.openstreetmap.org/wiki/XAPI
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class DownloadingDataSet implements IDataSet {

    /**
     * The key for {@link Settings} for the base-URL we use
     * for OpenStreetMapAPI-accesses.
     */
    private static final String APIBASEURSETTING = "DownloadingDataSet.APIBaseURL";
    /**
     * Default-value for {@link DownloadingDataSet#APIBASEURSETTING}.
     */
    private static final String DEFAULTAPIBASEURL = "http://api.openstreetmap.org/api/0.6";

    /**
     * The key for {@link Settings} for the base-URL we use
     * for OpenStreetMapAPI-accesses.
     */
    private static final String XAPIBASEURSETTING = "DownloadingDataSet.XAPIBaseURL";
    /**
     * Default-value for {@link DownloadingDataSet#XAPIBASEURSETTING}.
     */
    private static final String DEFAULTXAPIBASEURL = "http://osmxapi.hypercube.telascience.org/api/0.6";

    /**
     * Here we cache things to avoid downloads.
     */
    private MemoryDataSet myCache = new MemoryDataSet();

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(FileTileDataSet.class.getName());

    /**
     * @return the cache
     */
    public MemoryDataSet getCache() {
        return myCache;
    }

    /**
     * @param aCache the cache to set
     */
    public void setCache(final MemoryDataSet aCache) {
        if (aCache == null) {
            throw new IllegalArgumentException("null cache given");
        }
        this.myCache = aCache;
    }

    /**
     * Open a new Changeset on the OpenStreetMap API-server to upload
     * changes..
     * @param aComments at least created_by" and "comment" should be populated
     * @param aUserName the username to use
     * @param aPassword the password to use
     * @return the changeset id
     * @see #uploadChange(int, String)
     * @see #uploadChange(int, String)
     * @see #closeChangeset(int)
     * @throws IOException if anything happens
     */
    public int openChangeset(final Collection<Tag> aComments, final String aUserName, final String aPassword) throws IOException {
        URL url = new URL(Settings.getInstance().get(APIBASEURSETTING, DEFAULTAPIBASEURL) + "/changeset/create");
        System.err.println("DEBUG: URL= " + url.toString());
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();

        // we do not use Authenticator.setDefault() here to stay thread-safe.
        httpCon.setRequestProperty("Authorization", "Basic "
                + (new sun.misc.BASE64Encoder()).encode(
                        (aUserName + ":" + aPassword).getBytes()));


        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("PUT");
        OutputStreamWriter out = new OutputStreamWriter(
            httpCon.getOutputStream());
        out.write("<osm version=\"0.6\" generator=\"Traveling Salesman - LibOSM\">\n\t<changeset>\n");
        for (Tag tag : aComments) { //TODO: escape illegal characters
            out.write("\t\t<tag k=\"" + tag.getKey()
                    + "\" v=\"" + tag.getValue() + "\"/>\n");
        }
        out.write("\t</changeset>\n</osm>");
        out.close();

        int responseCode = httpCon.getResponseCode();
        LOG.info("response-code to opening changeset: " + responseCode);
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Http-Status-code is not 200 but"
                    + httpCon.getResponseCode()
                    + " \"" + httpCon.getResponseMessage()
                    + "\" Error=" + httpCon.getHeaderField("Error"));
        }

        InputStreamReader in = new InputStreamReader(httpCon.getInputStream());
        char[] buffer = new char[Byte.MAX_VALUE];
        int len = in.read(buffer);
        int changeset = Integer.parseInt(new String(buffer, 0, len));
        System.err.println("DEBUG: changeset= " + changeset);
        return changeset;
    }

    /**
     * Upload changes to OpenStreetMap.
     * @param aChangeset see {@link #openChangeset(Collection, String, String)}
     * @param anOSCFile the changes to upload
     * @param aUserName the username to use
     * @param aPassword the password to use
     * @throws IOException if anything happens
     * @see {@link #closeChangeset(int)}
     */
    public void uploadChange(final int aChangeset, final File anOSCFile, final String aUserName, final String aPassword) throws IOException {
        Reader fr = new InputStreamReader(new FileInputStream(anOSCFile), "UTF8");
        StringBuilder sb = readAll(fr);
        uploadChange(aChangeset, sb.toString(), aUserName, aPassword);
    }

    /**
     * Read this reader into a String.
     * @param aReader where to read from
     * @return the content
     * @throws IOException if we cannot read
     */
    private StringBuilder readAll(final Reader aReader) throws IOException {
        char[] buffer = new char[Byte.MAX_VALUE];
        int reat = -1;
        StringBuilder sb = new StringBuilder();
        while ((reat = aReader.read(buffer)) >= 0) {
            sb.append(buffer, 0, reat);
        }
        aReader.close();
        return sb;
    }

    /**
     * Upload changes to OpenStreetMap.
     * @param aChangeset see {@link #openChangeset(Collection, String, String)}
     * @param anOSCFile the changes to upload
     * @param aUserName the username to use
     * @param aPassword the password to use
     * @throws IOException if anything happens
     * @see {@link #closeChangeset(int)}
     */
    public void uploadChange(final int aChangeset, final String anOSCFile, final String aUserName, final String aPassword) throws IOException {
        URL url = new URL(Settings.getInstance().get(APIBASEURSETTING, DEFAULTAPIBASEURL) + "/changeset/" + aChangeset + "/upload");
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("POST");

        // we do not use Authenticator.setDefault() here to stay thread-safe.
        httpCon.setRequestProperty("Authorization", "Basic "
                + (new sun.misc.BASE64Encoder()).encode(
                        (aUserName + ":" + aPassword).getBytes()));

        OutputStream out = httpCon.getOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(out, "UTF8");
        System.out.println("changeset we got uploading:\n" + anOSCFile);
        String modified = anOSCFile.replaceAll("changeset=\"[0-9]*\"", "changeset=\"" + aChangeset + "\"");
        System.out.println("changeset we are uploading:\n" + modified);
        writer.write(modified);
        writer.close();
        int responseCode = httpCon.getResponseCode();
        LOG.info("response-code to changeset: " + responseCode);
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Http-Status-code is not 200 but "
                    + httpCon.getResponseCode()
                    + " \"" + httpCon.getResponseMessage()
                    + "\" Error=" + httpCon.getHeaderField("Error"));
        }
    }

    /**
     * Close and commit a changeset on the OpenStreetMap API-server.
     * @param aChangeset the id of the changeset to close
     * @param aUserName the username to use
     * @param aPassword the password to use
     * @throws IOException if anything happens
     * @see {@link #openChangeset(Collection, String, String)}
     */
    public void closeChangeset(final int aChangeset, final String aUserName, final String aPassword) throws IOException {
        URL url = new URL(Settings.getInstance().get(APIBASEURSETTING, DEFAULTAPIBASEURL) + "/changeset/" + aChangeset + "/close");
        System.err.println("DEBUG: URL= " + url.toString());
        HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("PUT");

        // we do not use Authenticator.setDefault() here to stay thread-safe.
        httpCon.setRequestProperty("Authorization", "Basic "
                + (new sun.misc.BASE64Encoder()).encode(
                        (aUserName + ":" + aPassword).getBytes()));

        httpCon.setRequestProperty(
                "Content-Type", "application/x-www-form-urlencoded");
        httpCon.connect();
        int responseCode = httpCon.getResponseCode();
        LOG.info("response-code to closing of changeset: " + responseCode);
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException("Http-Status-code is not 200 but "
                    + httpCon.getResponseCode()
                    + " \"" + httpCon.getResponseMessage()
                    + "\" Error=" + httpCon.getHeaderField("Error"));
        }
    }

    /**
     * Fetch whatever entities that URL sends.
     * @param aURL the url to fetch
     * @return may only be null in case of error. Empty dataset if nothing is reat.
     */
    protected MemoryDataSet getFromURL(final String aURL) {
        System.out.println(aURL);
        try {
            URLDownloader downloader = new URLDownloader(new URL(aURL));
            downloader.setSink(new DataSetSink(myCache));
            downloader.run();
            //download members of relations and ways
            return myCache;
        } catch (Exception e) {
            System.err.println("URL was: " + aURL);
            System.err.println("#nodes so far    : " + myCache.getNodesCount());
            System.err.println("#ways so far     : " + myCache.getWaysCount());
            System.err.println("#relations so far: " + myCache.getRelationsCount());
            e.printStackTrace();
            return null;
        } catch (OutOfMemoryError e) {
            System.err.println("URL was: " + aURL);
            System.err.println("#nodes so far    : " + myCache.getNodesCount());
            System.err.println("#ways so far     : " + myCache.getWaysCount());
            System.err.println("#relations so far: " + myCache.getRelationsCount());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#addNode(org.openstreetmap.osmosis.core.domain.v0_6.Node)
     */
    @Override
    public void addNode(final Node aW) {
        throw new IllegalStateException("uploading not supported yet");
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#addRelation(org.openstreetmap.osmosis.core.domain.v0_6.Relation)
     */
    @Override
    public void addRelation(final Relation aR) {
        throw new IllegalStateException("uploading not supported yet");
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#addWay(org.openstreetmap.osmosis.core.domain.v0_6.Way)
     */
    @Override
    public void addWay(final Way aW) {
        throw new IllegalStateException("uploading not supported yet");
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#containsNode(org.openstreetmap.osmosis.core.domain.v0_6.Node)
     */
    @Override
    public boolean containsNode(final Node aW) {
        return getNodeByID(aW.getId()) != null;
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#containsRelation(org.openstreetmap.osmosis.core.domain.v0_6.Relation)
     */
    @Override
    public boolean containsRelation(final Relation aR) {
        return getRelationByID(aR.getId()) != null;
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#containsWay(org.openstreetmap.osmosis.core.domain.v0_6.Way)
     */
    @Override
    public boolean containsWay(final Way aW) {
        return getWaysByID(aW.getId()) != null;
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.osm.data.IDataSet#getNearestNode(org.openstreetmap.osm.data.coordinates.LatLon, org.openstreetmap.osm.data.Selector)
     */
    @Override
    public Node getNearestNode(final LatLon aLastGPSPos, final Selector aSelector) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#getNodeByID(long)
     */
    @Override
    public Node getNodeByID(final long aNodeID) {
        Node retval = myCache.getNodeByID(aNodeID);
        if (retval != null) {
            return retval;
        }
        return getFromURL(Settings.getInstance().get(APIBASEURSETTING, DEFAULTAPIBASEURL) + "/node/" + aNodeID).getNodeByID(aNodeID);
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.osm.data.IDataSet#getNodes(org.openstreetmap.osm.data.coordinates.Bounds)
     */
    @Override
    public Iterator<Node> getNodes(final Bounds aBoundingBox) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#getNodesByName(java.lang.String)
     */
    @Override
    public Iterator<Node> getNodesByName(final String aLookFor) {
        return getFromURL(Settings.getInstance().get(XAPIBASEURSETTING, DEFAULTXAPIBASEURL)
                + "/node[name|ref|name:de|short_name:de=" + URLEncoder.encode(aLookFor) + "]").getNodesByName(aLookFor);
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#getNodesByTag(java.lang.String, java.lang.String)
     */
    @Override
    public Iterator<Node> getNodesByTag(final String aKey, final String aValue) {
        return getFromURL(Settings.getInstance().get(XAPIBASEURSETTING, DEFAULTXAPIBASEURL)
                + "/nodes[" + URLEncoder.encode(aKey) + "=" + URLEncoder.encode(aValue) + "]").getNodesByTag(aKey, aValue);
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#getRelationByID(long)
     */
    @Override
    public Relation getRelationByID(final long aRelationID) {
        Relation retval = myCache.getRelationByID(aRelationID);
        if (retval != null) {
            return retval;
        }
        return getFromURL(Settings.getInstance().get(APIBASEURSETTING, DEFAULTAPIBASEURL) + "/relation/" + aRelationID + "/full").getRelationByID(aRelationID);
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.osm.data.IDataSet#getRelations(org.openstreetmap.osm.data.coordinates.Bounds)
     */
    @Override
    public Iterator<Relation> getRelations(final Bounds aBoundingBox) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.osm.data.IDataSet#getWayHelper()
     */
    @Override
    public WayHelper getWayHelper() {
        return new WayHelper(this);
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.osm.data.IDataSet#getWays(org.openstreetmap.osm.data.coordinates.Bounds)
     */
    @Override
    public Iterator<Way> getWays(final Bounds aBoundingBox) {
        // TODO Auto-generated method stub
        // [bbox=left,bottom,right,top]
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#getWaysByID(long)
     */
    @Override
    public Way getWaysByID(final long aWayID) {
        Way retval = myCache.getWaysByID(aWayID);
        if (retval != null) {
            return retval;
        }
        return getFromURL(Settings.getInstance().get(APIBASEURSETTING, DEFAULTAPIBASEURL) + "/way/" + aWayID + "/full").getWaysByID(aWayID);
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#getWaysByName(java.lang.String, org.openstreetmap.osm.data.coordinates.Bounds)
     */
    @Override
    public Iterator<Way> getWaysByName(final String aLookFor, final Bounds aBoundingBox) {
        return getFromURL(Settings.getInstance().get(XAPIBASEURSETTING, DEFAULTXAPIBASEURL)
                + "/relation[name|ref|name:de|short_name:de=" + URLEncoder.encode(aLookFor) + "]"
                //TODO + "[bbox=left,bottom,right,top]" 
                ).getWaysByName(aLookFor, aBoundingBox);
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#getWaysByTag(java.lang.String, java.lang.String)
     */
    @Override
    public Iterator<Way> getWaysByTag(final String aKey, final String aValue) {
        return getFromURL(Settings.getInstance().get(XAPIBASEURSETTING, DEFAULTXAPIBASEURL)
                + "/relation[" + URLEncoder.encode(aKey) + "=" + URLEncoder.encode(aValue) + "]").getWaysByTag(aKey, aValue);
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.osm.data.IDataSet#getWaysForNode(long)
     */
    @Override
    public Iterator<Way> getWaysForNode(final long aNodeID) {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#removeNode(org.openstreetmap.osmosis.core.domain.v0_6.Node)
     */
    @Override
    public void removeNode(final Node aW) {
        throw new IllegalStateException("this operation is not possible");
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#removeRelation(org.openstreetmap.osmosis.core.domain.v0_6.Relation)
     */
    @Override
    public void removeRelation(final Relation aR) {
        throw new IllegalStateException("this operation is not possible");
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#removeWay(org.openstreetmap.osmosis.core.domain.v0_6.Way)
     */
    @Override
    public void removeWay(final Way aW) {
        throw new IllegalStateException("this operation is not possible");
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#shutdown()
     */
    @Override
    public void shutdown() {
        // nothing to do
    }

    /* (non-Javadoc)
     * @see org.openstreetmap.osm.Plugins.IPlugin#getSettings()
     */
    @Override
    public ConfigurationSection getSettings() {
        ConfigurationSection retval = new ConfigurationSection("DownloadingDataSet");

        ConfigurationSetting apiBaseURL = new ConfigurationSetting(APIBASEURSETTING, DEFAULTAPIBASEURL, TYPES.STRING);
        apiBaseURL.setKey("plugin.DownloadingDataSet.APIBaseURL");
        apiBaseURL.setRequiredRestart(false);
        retval.addSetting(apiBaseURL);

        ConfigurationSetting xapiBaseURL = new ConfigurationSetting(XAPIBASEURSETTING, DEFAULTXAPIBASEURL, TYPES.STRING);
        xapiBaseURL.setKey("plugin.DownloadingDataSet.XAPIBaseURL");
        xapiBaseURL.setRequiredRestart(false);
        retval.addSetting(xapiBaseURL);

        return retval;
    }

}
