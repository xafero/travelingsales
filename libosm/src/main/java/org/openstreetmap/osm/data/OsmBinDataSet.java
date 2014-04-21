/**
 * OsmBinDataSet.java
 * created: 02.01.2009
 * (c) 2008 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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
package org.openstreetmap.osm.data;


//automatically created logger for debug and error -output
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.osmbin.v1_0.OsmBinDataSetV10;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;



/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * OsmBinDataSet.java<br/>
 * created: 02.01.2009 <br/>
 *<br/><br/>
 * <b>This {@link IDataSet} implements the OsmBin-file-format version 1.0 as laid out
 * <a href="http://wiki.openstreetmap.org/index.php?title=User:MarcusWolschon/osmbin_draft#version_1.0">here</a></b><br/>
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:OsmbinClasses.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class OsmBinDataSet implements IDataSet {

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(OsmBinDataSet.class
            .getName());

    /**
     * The directory where we expect the files as described in
     * <a href="http://wiki.openstreetmap.org/index.php/User:MarcusWolschon%5Cosmbin_draft">here</a>
     * to be present.
     */
    private File myDataDirectory;

    /**
     * Properties that describe the version of
     * the implementation to use.
     */
    private Properties myProperties;

    /**
     * The implementation of the OsmBinDataSet
     * to use.
     */
    private IDataSet myImplementation;

    /**
     * The given directory and the expected files
     * in it are created if they do not exist.
     * @param aDataDirectory the directory with our data.
     */
    public OsmBinDataSet(final File aDataDirectory) {
        super();
        setDataDirectory(aDataDirectory);
    }


    /**
     * Add a new way to this dataSet.
     * @param aWay the way to add
     */
    public void addWay(final Way aWay) {
        getImplementation().addWay(aWay);
    }

    /**
     * Add a new node to this dataSet.
     * @param aNode the way to add
     */
    public void addNode(final Node aNode) {
        if (aNode == null) {
            throw new IllegalArgumentException("null node given");
        }
        getImplementation().addNode(aNode);
    }

    /**
     * @param aWayID the id of the way.
     * @return the way or null
     */
    public Way getWaysByID(final long aWayID) {
        return getImplementation().getWaysByID(aWayID);
    }

    /**
     * @param aNodeID the id of the node.
     * @return the node or null
     */
    public Node getNodeByID(final long aNodeID) {
        return getImplementation().getNodeByID(aNodeID);
    }

    /**
     * Return all ways in this dataset that contain
     * this node.
     * @param aNodeID the node to look for
     * @return an iterator over the list
     */
    public Iterator<Way> getWaysForNode(final long aNodeID) {
        return getImplementation().getWaysForNode(aNodeID);
    }

    /**
     * @param aWay may be null (ignored then)
     */
    public void removeWay(final Way aWay) {
        if (aWay == null) {
            return;
        }
        getImplementation().removeWay(aWay);
    }

    /**
     * @param aNode may be null (ignored then)
     */
    public void removeNode(final Node aNode) {
        if (aNode == null) {
            return;
        }
        getImplementation().removeNode(aNode);
    }

    /**
     * @param aWay the way to look for
     * @return true if we contain this way
     */
    public boolean containsWay(final Way aWay) {
        if (aWay == null) {
            throw new IllegalArgumentException("null way given");
        }
        return getImplementation().containsWay(aWay);
    }
    /**
     * @param aNode the node to look for
     * @return true if we contain this node
     */
    public boolean containsNode(final Node aNode) {
        if (aNode == null) {
            throw new IllegalArgumentException("null node given");
        }
        return getImplementation().containsNode(aNode);
    }

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "OsmBinDataSet@" + hashCode();
    }

    /**
     * @return Returns the dataDirectory.
     * @see #myDataDirectory
     */
    public File getDataDirectory() {
        return this.myDataDirectory;
    }

    /**
     * @param aDataDirectory The dataDirectory to set.
     * @see #myDataDirectory
     */
    public void setDataDirectory(final File aDataDirectory) {
        if (aDataDirectory == null) {
            throw new IllegalArgumentException("null 'aDataDirectory' given!");
        }
        if (!aDataDirectory.exists()) {
            if (!aDataDirectory.mkdirs()) {
                throw new IllegalArgumentException("dataDirectory given that "
                        + "can not be created and does not exist.");
            }
        }
        this.myDataDirectory = aDataDirectory;
        if (this.myProperties != null) {
            this.myProperties = null;
        }
        if (this.myImplementation != null) {
            this.myImplementation = null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void addRelation(final Relation aRelation) {
        if (aRelation == null) {
            throw new IllegalArgumentException("null relation given");
        }
        getImplementation().addRelation(aRelation);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsRelation(final Relation aRelation) {
        if (aRelation == null) {
            throw new IllegalArgumentException("null relation given");
        }
        return getImplementation().containsRelation(aRelation);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Node getNearestNode(final LatLon aLastGPSPos, final Selector aSelector) {
        return getImplementation().getNearestNode(aLastGPSPos, aSelector);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Node> getNodes(final Bounds aBoundingBox) {
        if (aBoundingBox == null) {
            return getImplementation().getNodes(Bounds.WORLD);
        }
        return getImplementation().getNodes(aBoundingBox);
    }


    @Override
    public Iterator<Node> getNodesByName(final String aLookFor) {
        return getImplementation().getNodesByName(aLookFor);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Node> getNodesByTag(final String aKey, final String aValue) {
        return getImplementation().getNodesByTag(aKey, aValue);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Relation getRelationByID(final long aRelationID) {
        return getImplementation().getRelationByID(aRelationID);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Relation> getRelations(final Bounds aBoundingBox) {
        if (aBoundingBox == null) {
            return getImplementation().getRelations(Bounds.WORLD);
        }
        return getImplementation().getRelations(aBoundingBox);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public WayHelper getWayHelper() {
        return getImplementation().getWayHelper();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Way> getWays(final Bounds aBoundingBox) {
        if (aBoundingBox == null) {
            return getImplementation().getWays(Bounds.WORLD);
        }
        return getImplementation().getWays(aBoundingBox);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Way> getWaysByName(final String aLookFor, final Bounds aBoundingBox) {
        return getImplementation().getWaysByName(aLookFor, aBoundingBox);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Way> getWaysByTag(final String aKey, final String aValue) {
        return getImplementation().getWaysByTag(aKey, aValue);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void removeRelation(final Relation aRelation) {
        if (aRelation == null) {
            return;
        }
        getImplementation().removeRelation(aRelation);
    }


    /**
     * Close all files.
     */
    @Override
    public void shutdown() {
        if (this.myImplementation != null) {
            this.myImplementation.shutdown();
            this.myImplementation = null;
        }
    }


    /**
     * @return the properties
     */
    protected Properties getProperties() {
        if (this.myProperties == null) {
            this.myProperties = new Properties();
            File propFile = new File(getDataDirectory(), "osmbin.properties");
            if (propFile.exists()) {
                try {
                    this.myProperties.load(new InputStreamReader(new FileInputStream(propFile), "UTF8"));
                } catch (IOException e) {
                    LOG.log(Level.SEVERE, "Cannot read osmbin.properties. Assuming defaults", e);
                }
            } else {
                //if (getDataDirectory().list().length == 0) {
                if (!(new File(getDataDirectory(), "nodes.obm")).exists()) {
                    // if we start with an empty directory,
                    // use the newest version of the format
                    myProperties.setProperty("osmbin.version", "v1.1");
                    try {
                        myProperties.store(new OutputStreamWriter(new FileOutputStream(propFile), "UTF-8"), "");
                    } catch (IOException e) {
                        LOG.log(Level.SEVERE, "Cannot write osmbin.properties. Assuming defaults", e);
                    }
                }
            }
        }
        return myProperties;
    }


    /**
     * @return the implementation
     */
    protected IDataSet getImplementation() {
        if (this.myImplementation != null) {
            return this.myImplementation;
        }
        Properties properties = getProperties();
        String version = properties.getProperty("osmbin.version", "0.9");
        if (version.equalsIgnoreCase("1.1")
            || version.equalsIgnoreCase("v1.1")
            || version.equalsIgnoreCase("1.1")) {
            this.myImplementation = new OsmBinDataSetV10(getDataDirectory());
        } else {
            throw new IllegalArgumentException("Unsupported version of the OsmBin file-format '"
                    + version + "'");
        }
        return myImplementation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationSection getSettings() {
        return myImplementation.getSettings();
    }

}


