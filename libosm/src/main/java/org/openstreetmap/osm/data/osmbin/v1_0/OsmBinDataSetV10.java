/**
 * OsmBinDataSetV10.java
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
package org.openstreetmap.osm.data.osmbin.v1_0;


//automatically created logger for debug and error -output
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//automatically created propertyChangeListener-Support
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.Selector;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.osmbin.AttrNames;
import org.openstreetmap.osm.data.osmbin.GeoIndexFile;
import org.openstreetmap.osm.data.osmbin.IDIndexFile;
import org.openstreetmap.osm.data.osmbin.IGeoIndexFile;
import org.openstreetmap.osm.data.osmbin.IIDIndexFile;
import org.openstreetmap.osm.data.osmbin.v1_0.NodesFile;
import org.openstreetmap.osm.data.osmbin.v1_0.RelationsFile;
import org.openstreetmap.osm.data.osmbin.v1_0.WaysFile;
import org.openstreetmap.osm.data.searching.NameHelper;

import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.util.FixedPrecisionCoordinateConvertor;



/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * OsmBinDataSetV10.java<br/>
 * created: 02.01.2009<br/>
 *<br/><br/>
 * <b>This {@link IDataSet} implements the OsmBin-file-format version 1.0 as laid out
 * <a href="http://wiki.openstreetmap.org/index.php?title=User:MarcusWolschon/osmbin_draft#version_1.0">here</a></b><br/>
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:OsmbinClasses.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class OsmBinDataSetV10 implements IDataSet {

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(OsmBinDataSetV10.class
            .getName());

    /**
     * In {@link #getNearestNode(LatLon, Selector)} search
     * in increasing circles starting with this radius.
     */
    private static final double SEARCHSTARTRADIUS = 0.0001d;

    /**
     * The directory where we expect the files as described in
     * <a href="http://wiki.openstreetmap.org/index.php/User:MarcusWolschon%5Cosmbin_draft">here</a>
     * to be present.
     */
    private File myDataDirectory;

    /**
     * The nodes.obm -file.
     */
    private NodesFile nodesFile;

    /**
     * The nodes.idx -file.
     */
    private IIDIndexFile nodesIndex;

    /**
     * The nodes.id2 -file.
     */
    private IGeoIndexFile nodes2DIndex;

    /**
     * The ways.obm -file.
     */
    private WaysFile waysFile;

    /**
     * The ways.idx -file.
     */
    private IDIndexFile waysIndex;

    /**
     * The relations.obm -file.
     */
    private RelationsFile relationsFile;

    /**
     * The relations.idx -file.
     */
    private IDIndexFile relationsIndex;

    /**
     * The attrnames.txt -file.
     */
    private AttrNames attrNamesFile;

    /**
     * The given directory and the expected files
     * in it are created if they do not exist.
     * @param aDataDirectory the directory with our data.
     */
    public OsmBinDataSetV10(final File aDataDirectory) {
        super();
        setDataDirectory(aDataDirectory);
    }


    /**
     * Add a new way to this dataSet.
     * @param aWay the way to add
     */
    public void addWay(final Way aWay) {
        try {
            WaysFile ways = getWaysFile();
            ExtendedWay way = null;
            if (aWay instanceof ExtendedWay) {
                way = (ExtendedWay) aWay;
            } else {
                ExtendedWay existingWay = null;
                try {
                    existingWay = ways.readWay(aWay.getId());
                    if (existingWay != null && existingWay.getVersion() > aWay.getVersion()) {
                        LOG.fine("ignoring way " + aWay.getId()
                                + " that is an older version " + aWay.getVersion()
                                + " then the one we have" + existingWay.getVersion());
                        return;
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Cannot check existing way "
                            + aWay.getId() + " to have a lower vesion number.");
                }

                way = new ExtendedWay(aWay.getId(), aWay.getVersion(), aWay.getTags(), aWay.getWayNodes());
                if (existingWay != null) {
                    way.setReferencedRelationIDs(existingWay.getReferencedRelationIDs());
                }
            }
            NodesFile nodes = getNodesFile();
            double minLat = Double.MAX_VALUE;
            double minLon = Double.MAX_VALUE;
            double maxLat = Double.MIN_VALUE;
            double maxLon = Double.MIN_VALUE;
            List<WayNode> wayNodes = way.getWayNodes();
            for (WayNode wayNode : wayNodes) {
                long nodeRecordNumber = nodes.findRecordForNode(wayNode.getNodeId());
                if (nodeRecordNumber < 0) {
                    continue; // we don't have this node in our db
                }
                try {
                    ExtendedNode node = nodes.readNode(wayNode.getNodeId(), nodeRecordNumber);
                    if (node != null) {
                        assert node.getId() == wayNode.getNodeId() : "node reat from location "
                            + nodeRecordNumber + " that was returned by findRecordForNode("
                            + wayNode.getNodeId() + ") is a wrong node with id " + node.getId();
                        node.addReferencedWay(aWay.getId());
                        nodes.writeNode(node, nodeRecordNumber);
                        minLat = Math.min(minLat, node.getLatitude());
                        maxLat = Math.max(maxLat, node.getLatitude());
                        minLon = Math.min(minLon, node.getLongitude());
                        maxLon = Math.max(maxLon, node.getLongitude());
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Cannot load a node " + wayNode.getNodeId()
                            + " as part of a way " + aWay.getId() + "!", e);
                }
            }
            way.setMinLatitude(minLat);
            way.setMaxLatitude(maxLat);
            way.setMinLongitude(minLon);
            way.setMaxLongitude(maxLon);
            ways.writeWay(way);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "[IOException] Problem in "
                       + getClass().getName(),
                         e);
            IllegalStateException weDontWork = new IllegalStateException(e.getMessage());
            weDontWork.initCause(e);
            throw weDontWork;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void addRelation(final Relation aRelation) {
        try {
            RelationsFile relations = getRelationsFile();
            WaysFile ways = getWaysFile();
            NodesFile nodes = getNodesFile();
            //we do not yet store the bounding-boxes of relations
//            double minLat = Double.MAX_VALUE;
//            double minLon = Double.MAX_VALUE;
//            double maxLat = Double.MIN_VALUE;
//            double maxLon = Double.MIN_VALUE;
            for (RelationMember member : aRelation.getMembers()) {
                if (member.getMemberType() == EntityType.Node) {
                    long nodeRecordNumber = nodes.findRecordForNode(member.getMemberId());
                    if (nodeRecordNumber < 0) {
                        continue; // we don't have this node in our db
                    }
                    ExtendedNode node = nodes.readNode(member.getMemberId(), nodeRecordNumber);
                    if (node != null) {
                        assert node.getId() == member.getMemberId() : "node reat from location "
                            + nodeRecordNumber + " that was returned by findRecordForNode("
                            + member.getMemberId() + ") is a wrong node with id " + node.getId();
                        node.addReferencedRelation(aRelation.getId());
                        nodes.writeNode(node, nodeRecordNumber);
//                        minLat = Math.min(minLat, node.getLatitude());
//                        maxLat = Math.max(maxLat, node.getLatitude());
//                        minLon = Math.min(minLon, node.getLongitude());
//                        maxLon = Math.max(maxLon, node.getLongitude());
                    }
                }

                if (member.getMemberType() == EntityType.Way) {
                    long wayRecordNumber = ways.findRecordForWay(member.getMemberId());
                    if (wayRecordNumber < 0) {
                        continue; // we don't have this node in our db
                    }
                    ExtendedWay way = ways.readWay(member.getMemberId(), wayRecordNumber);
                    if (way != null) {
                        assert way.getId() == member.getMemberId() : "way reat from location "
                            + wayRecordNumber + " that was returned by findRecordForWay("
                            + member.getMemberId() + ") is a wrong node with id " + way.getId();
                        way.addReferencedRelation(aRelation.getId());
                        ways.writeWay(way, wayRecordNumber);
//                        minLat = Math.min(minLat, way.getMinLatitude());
//                        maxLat = Math.max(maxLat, way.getMaxLatitude());
//                        minLon = Math.min(minLon, way.getMinLongitude());
//                        maxLon = Math.max(maxLon, way.getMaxLongitude());
                    }
                }

//                if (member.getMemberType() == EntityType.Relation) {
//                    //we do not yet store back-references in relations
//                }
            }
            relations.writeRelation(aRelation);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "[IOException] Problem in "
                       + getClass().getName(),
                         e);
            IllegalStateException weDontWork = new IllegalStateException(e.getMessage());
            weDontWork.initCause(e);
            throw weDontWork;
        }

    }

    /**
     * Add a new node to this dataSet.
     * @param aNode the way to add
     */
    public void addNode(final Node aNode) {
        try {
            ExtendedNode node = null;
            NodesFile nodes = getNodesFile();
            long recordNR = nodes.findRecordForNode(aNode.getId());
            if (aNode instanceof ExtendedNode) {
                node = (ExtendedNode) aNode;
            } else {
                ExtendedNode existingNode = null;
                try {
                    existingNode = nodes.readNode(aNode.getId(), recordNR);
                    if (existingNode != null && existingNode.getVersion() > aNode.getVersion()) {
                        LOG.fine("ignoring node " + aNode.getId()
                                + " that is an older version " + aNode.getVersion()
                                + " then the one we have" + existingNode.getVersion());
                        return;
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Cannot check existing node "
                            + aNode.getId() + " in record "
                            + recordNR + " to have a lower vesion number.");
                }
                node = new ExtendedNode(aNode.getId(), aNode.getVersion(), aNode.getChangesetId(), aNode.getLatitude(), aNode.getLongitude(), aNode.getTags());
                if (existingNode != null) {
                    node.setReferencedWayIDs(existingNode.getReferencedWayIDs());
                    node.setReferencedRelationIDs(existingNode.getReferencedRelationIDs());
                }
            }
            nodes.writeNode(node, recordNR);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[Exception] Problem in "
                       + getClass().getName(),
                         e);
            IllegalStateException weDontWork = new IllegalStateException(e.getMessage());
            weDontWork.initCause(e);
            throw weDontWork;
        }
    }




    /**
     * @param aWayID the id of the way.
     * @return the way or null
     */
    public Way getWaysByID(final long aWayID) {
        try {
            WaysFile ways = getWaysFile();
            ExtendedWay way = ways.readWay(aWayID);
            assert way == null || way.getWayNodes() != null;
            return way;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "[IOException] Problem in "
                       + getClass().getName(),
                         e);
            IllegalStateException weDontWork = new IllegalStateException(e.getMessage());
            weDontWork.initCause(e);
            throw weDontWork;
        }
    }

    /**
     * @param aNodeID the id of the node.
     * @return the node or null
     */
    public Node getNodeByID(final long aNodeID) {
        try {
            NodesFile nodes = getNodesFile();
            return nodes.readNode(aNodeID);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "[IOException] Problem in "
                       + getClass().getName(),
                         e);
            IllegalStateException weDontWork = new IllegalStateException(e.getMessage());
            weDontWork.initCause(e);
            throw weDontWork;
        }
    }

    /**
     * Return all ways in this dataset that contain
     * this node.
     * @param aNodeID the node to look for
     * @return an iterator over the list
     */
    public Iterator<Way> getWaysForNode(final long aNodeID) {
        try {
            NodesFile nodes = getNodesFile();
            ExtendedNode node = nodes.readNode(aNodeID);
            if (node == null) {
                LOG.warning("getWaysForNode(#" + aNodeID + ") - node not found");
                return null;
            }
            Set<Long> ways = node.getReferencedWayIDs();
            //LOG.info("node #" + aNodeID + " contains ways: " + Arrays.toString(ways.toArray()));
            List<Way> retval = new ArrayList<Way>(ways.size());
            for (Long wayID : ways) {
                try {
                    Way way = getWaysByID(wayID);
                    if (way != null) {
                        retval.add(way);
                    } else {
                        LOG.info("node #" + aNodeID + " has ways: " + wayID + " but we do not have that way in the DB");
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "[Exception] Cannot load way for node "
                            + aNodeID, e);
                } catch (OutOfMemoryError e) {
                    LOG.log(Level.SEVERE, "[Exception] Cannot load way for node "
                            + aNodeID, e);
                }
            }
            return retval.iterator();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "[IOException] Problem in "
                       + getClass().getName() + ":getWaysForNode(" + aNodeID + ")",
                         e);
            IllegalStateException weDontWork = new IllegalStateException(e.getMessage());
            weDontWork.initCause(e);
            throw weDontWork;
        }
    }

    /**
     * @param aWay may be null (ignored then)
     */
    public void removeWay(final Way aWay) {
        try {
            WaysFile ways = getWaysFile();
            NodesFile nodes = getNodesFile();
            ways.removeWay(aWay);
            for (WayNode wayNode : aWay.getWayNodes()) {
                long recordNr = nodes.findRecordForNode(wayNode.getNodeId());
                ExtendedNode node = nodes.readNode(wayNode.getNodeId(), recordNr);
                if (node != null && node.getReferencedWayIDs().remove(aWay.getId())) {
                    nodes.writeNode(node, recordNr);
                }
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "[IOException] Problem in "
                       + getClass().getName(),
                         e);
            IllegalStateException weDontWork = new IllegalStateException(e.getMessage());
            weDontWork.initCause(e);
            throw weDontWork;
        }
    }

    /**
     * @param w node be null (ignored then)
     */
    public void removeNode(final Node w) {
        try {
            NodesFile nodes = getNodesFile();
            Iterator<Way> waysForNode = getWaysForNode(w.getId());
            nodes.removeNode(w);
            // update min+max lon+lat of ways containing this node
            if (waysForNode != null) {
                while (waysForNode.hasNext()) {
                    Way way = waysForNode.next();
                    if (way != null) {
                        addWay(way);
                    }
                }
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "[IOException] Problem in "
                       + getClass().getName(),
                         e);
            IllegalStateException weDontWork = new IllegalStateException(e.getMessage());
            weDontWork.initCause(e);
            throw weDontWork;
        }
    }

    /**
     * @param aWay the way to look for
     * @return true if we contain this way
     */
    public boolean containsWay(final Way aWay) {
        try {
            WaysFile ways = getWaysFile();
            return ways.findRecordForWay(aWay.getId()) >= 0;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "[IOException] Problem in "
                       + getClass().getName(),
                         e);
            IllegalStateException weDontWork = new IllegalStateException(e.getMessage());
            weDontWork.initCause(e);
            throw weDontWork;
        }
    }
    /**
     * @param w the node to look for
     * @return true if we contain this node
     */
    public boolean containsNode(final Node w) {
        try {
            NodesFile nodes = getNodesFile();
            return nodes.findRecordForNode(w.getId()) >= 0;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "[IOException] Problem in "
                       + getClass().getName(),
                         e);
            IllegalStateException weDontWork = new IllegalStateException(e.getMessage());
            weDontWork.initCause(e);
            throw weDontWork;
        }
    }
    //------------------------ support for propertyChangeListeners ------------------

    /**
     * support for firing PropertyChangeEvents.
     * (gets initialized only if we really have listeners)
     */
    private volatile PropertyChangeSupport myPropertyChange = null;

    /**
     * Returned value may be null if we never had listeners.
     * @return Our support for firing PropertyChangeEvents
     */
    protected PropertyChangeSupport getPropertyChangeSupport() {
        return myPropertyChange;
    }

    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     *
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property.  The listener
     * will be invoked only when a call on firePropertyChange names that
     * specific property.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(final String propertyName,
                                                final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The PropertyChangeListener to be removed
     */
    public final void removePropertyChangeListener(final String propertyName,
                                                   final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(propertyName,
                    listener);
        }
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     *
     * @param listener  The PropertyChangeListener to be removed
     */
    public synchronized void removePropertyChangeListener(final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(listener);
        }
    }

    //-------------------------------------------------------

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

        Object old = this.myDataDirectory;
        if (old == aDataDirectory) {
            return; // nothing has changed
        }
        this.myDataDirectory = aDataDirectory;
        // <<insert code to react further to this change here
        if (this.attrNamesFile != null) {
            try {
                this.attrNamesFile.close();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Cannot close attribute-names", e);
            }
            this.attrNamesFile = null;
        }
        if (this.nodesFile != null) {
            try {
                this.nodesFile.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close nodes.obm", e);
            }
            this.nodesFile = null;
        }
        if (this.waysFile != null) {
            try {
                this.waysFile.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close ways.obm", e);
            }
            this.waysFile = null;
        }
        if (this.relationsFile != null) {
            try {
                this.relationsFile.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close relations.obm", e);
            }
            this.relationsFile = null;
        }
        this.attrNamesFile = null;
        if (this.nodesIndex != null) {
            try {
                this.nodesIndex.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close nodex-index", e);
            }
            this.nodesIndex = null;
        }
        if (this.waysIndex != null) {
            try {
                this.waysIndex.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close ways-index", e);
            }
            this.waysIndex = null;
        }
        if (this.relationsIndex != null) {
            try {
                this.relationsIndex.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close relations-index", e);
            }
            this.relationsIndex = null;
        }
        PropertyChangeSupport propertyChangeFirer = getPropertyChangeSupport();
        if (propertyChangeFirer != null) {
            propertyChangeFirer.firePropertyChange("aDataDirectory", old,
                    aDataDirectory);
        }
    }

    /**
     * @return Returns the attrNamesFile.
     * @throws IOException if we cannot create the file (if needed to).
     * @see #nodesFile
     */
    protected AttrNames getAttrNamesFile() throws IOException {
        if (this.attrNamesFile == null) {
            this.attrNamesFile = new AttrNames(new File(getDataDirectory(), "attrnames.txt"));
        }
        return this.attrNamesFile;
    }

    /**
     * @return Returns the nodesFile.
     * @throws IOException if we cannot create the file (if needed to).
     * @see #nodesFile
     */
    protected NodesFile getNodesFile() throws IOException {
        if (this.nodesFile == null) {
            this.nodesFile = new NodesFile(new File(getDataDirectory(), "nodes.obm"), getAttrNamesFile(), getNodesIndex(),  getNodes2DIndex());
        }
        return this.nodesFile;
    }




    /**
     * @return the nodes-index by ID.
     * @throws IOException if we cannot create the file (if needed to).
     */
    protected IIDIndexFile getNodesIndex() throws IOException {
        if (this.nodesIndex == null) {
            this.nodesIndex = new IDIndexFile(new File(getDataDirectory(), "nodes.idx"));
        }
        return this.nodesIndex;
    }

    /**
     * @return the nodes-index by ID.
     * @throws IOException if we cannot create the file (if needed to).
     */
    protected IGeoIndexFile getNodes2DIndex() throws IOException {
        if (this.nodes2DIndex == null) {
            this.nodes2DIndex = new GeoIndexFile(new File(getDataDirectory(), "nodes.id2"));
        }
        return this.nodes2DIndex;
    }

    /**
     * @return Returns the waysFile.
     * @throws IOException if we cannot create the file (if needed to).
     * @see #nodesFile
     */
    protected WaysFile getWaysFile() throws IOException {
        if (this.waysFile == null) {
            this.waysFile = new WaysFile(new File(getDataDirectory(), "ways.obm"), getAttrNamesFile(), getWaysIndex());
        }
        return this.waysFile;
    }




    /**
     * @return the ways-index by ID.
     * @throws IOException if we cannot create the file (if needed to).
     */
    protected IDIndexFile getWaysIndex() throws IOException {
        if (this.waysIndex == null) {
            this.waysIndex = new IDIndexFile(new File(getDataDirectory(), "ways.idx"));
        }
        return this.waysIndex;
    }

    /**
     * @return Returns the relationsFile.
     * @throws IOException if we cannot create the file (if needed to).
     * @see #nodesFile
     */
    protected RelationsFile getRelationsFile() throws IOException {
        if (this.relationsFile == null) {
            this.relationsFile = new RelationsFile(new File(getDataDirectory(), "relations.obm"), getAttrNamesFile(), getRelationsIndex());
        }
        return this.relationsFile;
    }




    /**
     * @return the relations-index by ID.
     * @throws IOException if we cannot create the file (if needed to).
     */
    protected IDIndexFile getRelationsIndex() throws IOException {
        if (this.relationsIndex == null) {
            this.relationsIndex = new IDIndexFile(new File(getDataDirectory(), "relations.idx"));
        }
        return this.relationsIndex;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsRelation(final Relation aR) {
        try {
            RelationsFile relations = getRelationsFile();
            return relations.findRecordForRelation(aR.getId()) >= 0;
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "[IOException] Problem in "
                       + getClass().getName(),
                         e);
            IllegalStateException weDontWork = new IllegalStateException(e.getMessage());
            weDontWork.initCause(e);
            throw weDontWork;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Node getNearestNode(final LatLon aLastGPSPos, final Selector aSelector) {
        try {
            double radius = SEARCHSTARTRADIUS;
            Set<Long> nodes = getNodes2DIndex().get(FixedPrecisionCoordinateConvertor.convertToFixed(aLastGPSPos.lat() - radius),
                    FixedPrecisionCoordinateConvertor.convertToFixed(aLastGPSPos.lon() - radius),
                    FixedPrecisionCoordinateConvertor.convertToFixed(aLastGPSPos.lat() + radius),
                    FixedPrecisionCoordinateConvertor.convertToFixed(aLastGPSPos.lon() + radius));
            double minDist = Double.MAX_VALUE;
            Node minDistNode = null;
            for (Long nodeRecord : nodes) {
                if (nodeRecord == null || nodeRecord.longValue() == Long.MIN_VALUE) {
                    continue;
                }
                Node node = getNodesFile().readNode(Long.MIN_VALUE, nodeRecord);
                if (aSelector != null && !aSelector.isAllowed(this, node))
                    continue;
                if (node == null) {
                    LOG.severe("node with record-number " + nodeRecord + " returned byy 2D-index does not exist");
                    continue;
                }
                LatLon pos = new LatLon(node.getLatitude(), node.getLongitude());
                double dist = pos.distance(aLastGPSPos);

                if (dist < minDist) {
                    minDist = dist;
                    minDistNode = node;
                }
            }
            if (minDistNode != null) {
                return minDistNode;
            }
            while (radius < 2) {
                radius = radius * 2;
                nodes = getNodes2DIndex().get(FixedPrecisionCoordinateConvertor.convertToFixed(aLastGPSPos.lat() - radius),
                        FixedPrecisionCoordinateConvertor.convertToFixed(aLastGPSPos.lon() - radius),
                        FixedPrecisionCoordinateConvertor.convertToFixed(aLastGPSPos.lat() + radius),
                        FixedPrecisionCoordinateConvertor.convertToFixed(aLastGPSPos.lon() + radius));
                for (Long nodeRecord : nodes) {
                    if (nodeRecord == null || nodeRecord.longValue() == Long.MIN_VALUE) {
                        continue;
                    }
                    Node node = getNodesFile().readNode(Long.MIN_VALUE, nodeRecord);
                    if (aSelector != null && !aSelector.isAllowed(this, node))
                        continue;
                    if (node == null) {
                        LOG.severe("node with record-number " + nodeRecord + " returned byy 2D-index does not exist");
                        continue;
                    }
                    LatLon pos = new LatLon(node.getLatitude(), node.getLongitude());
                    double dist = pos.distance(aLastGPSPos);

                    if (dist < minDist) {
                        minDist = dist;
                        minDistNode = node;
                    }
                }
                if (minDistNode != null) {
                    return minDistNode;
                }
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot get NearestNode", e);
        }
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Node> getNodes(final Bounds aBoundingBox) {
        Bounds bounds = aBoundingBox;
        if (bounds == null) {
            bounds = Bounds.WORLD;
        }
        try {
            if (bounds.equals(Bounds.WORLD)) {
                return getNodesFile().getallNodes();
            }
            IGeoIndexFile geoIndex = getNodes2DIndex();
            if (geoIndex == null) {
                LOG.log(Level.SEVERE, "Geo-indexing not implemented yet, getNodes(bounds) returns null");
                return new LinkedList<Node>().iterator();
            }
            Set<Long> nodeRecords = geoIndex.get(FixedPrecisionCoordinateConvertor.convertToFixed(bounds.getMin().lat()),
                    FixedPrecisionCoordinateConvertor.convertToFixed(bounds.getMin().lon()),
                    FixedPrecisionCoordinateConvertor.convertToFixed(bounds.getMax().lat()),
                    FixedPrecisionCoordinateConvertor.convertToFixed(bounds.getMax().lon()));
            Map<Long, Node> retval = new HashMap<Long, Node>();
            NodesFile nodes = getNodesFile();
            for (Long nodeRecord : nodeRecords) {
                if (nodeRecord == null || nodeRecord.longValue() == Long.MIN_VALUE) {
                    continue;
                }
                Node node = nodes.readNode(Long.MIN_VALUE, nodeRecord.longValue());
                if (node != null) {
                    if (node.getId() == Integer.MIN_VALUE) {
                        LOG.severe("bounding-box-query to nodes-file returned a now-empty node-record");
                    } else {
                        retval.put(node.getId(), node);
                    }
                }
            }
            return retval.values().iterator();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot get nodes by geographic bounds.", e);
            return new LinkedList<Node>().iterator();
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Node> getNodesByName(final String aLookFor) {
        return getNodesByTag(Tags.TAG_NAME, aLookFor);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Node> getNodesByTag(final String aKey, final String aValue) {
        LinkedList<Node> retval = new LinkedList<Node>();
        Iterator<Node> nodes = this.getNodes(Bounds.WORLD);
        while (nodes.hasNext()) {
            Node way = nodes.next();
            String value = WayHelper.getTag(way, aKey);  // e.g. "XYZSteet"
            if (value != null) {
                if (aValue != null && !value.equals(aValue))
                    continue;
                retval.add(way);
            }
        }

        return retval.iterator();

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Relation getRelationByID(final long aRelationID) {
        try {
            RelationsFile relations = getRelationsFile();
            return relations.readRelation(aRelationID);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "[IOException] Problem in "
                       + getClass().getName(),
                         e);
            IllegalStateException weDontWork = new IllegalStateException(e.getMessage());
            weDontWork.initCause(e);
            throw weDontWork;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Relation> getRelations(final Bounds aBoundingBox) {
        Bounds bounds = aBoundingBox;
        if (bounds == null) {
            bounds = Bounds.WORLD;
        }
        try {
            if (bounds.equals(Bounds.WORLD)) {
                return getRelationsFile().getallRelations();
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot get all relations", e);
            return new LinkedList<Relation>().iterator();
        }
        // TODO Auto-generated method stub
        return new LinkedList<Relation>().iterator();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public WayHelper getWayHelper() {
        return new WayHelper(this);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Way> getWays(final Bounds aBoundingBox) {
        Bounds bounds = aBoundingBox;
        if (bounds == null) {
            bounds = Bounds.WORLD;
        }
        try {
            if (bounds.equals(Bounds.WORLD)) {
                return getWaysFile().getallWays();
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot get all ways", e);
            return new LinkedList<Way>().iterator();
        }
        Iterator<Node> nodes = getNodes(bounds);
        Map<Long, Way> retval = new HashMap<Long, Way>();
        int nodesCount = 0;
        int nodesWithWithWaysCount = 0;
        while (nodes.hasNext()) {
            Node node = nodes.next();
            nodesCount++;
            try {
                if (node instanceof ExtendedNode) {
                    ExtendedNode nodeX = (ExtendedNode) node;
                    Set<Long> ways = nodeX.getReferencedWayIDs();
                    for (Long wayID : ways) {
                        Way way = getWaysByID(wayID);
                        if (way == null) {
                            LOG.severe("ExtendedNode " + node.getId()
                                    + " is referenced by a way "
                                    + wayID + " that does not exist ");
                        } else {
                            retval.put(way.getId(), way);
                        }
                    }
                } else {
                    Iterator<Way> ways = getWaysForNode(node.getId());
                    while (ways.hasNext()) {
                        nodesWithWithWaysCount++;
                        Way way = ways.next();
                        retval.put(way.getId(), way);
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "[Exception] Cannot load way for node",
                        e);
            } catch (OutOfMemoryError e) {
                LOG.log(Level.SEVERE, "[Exception] Cannot load way for node",
                        e);
            }
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("getWays(Bounds) returns " + retval.values().size() + " ways from "
                    + nodesCount + " nodes (" + nodesWithWithWaysCount + " had ways) in bounds");
        }
        return retval.values().iterator();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Way> getWaysByName(final String aLookFor, final Bounds aBoundingBox) {
        Iterator<Way> ways = getWays(aBoundingBox);
        String lookForRegEx = null;
        LinkedList<Way> retval = new LinkedList<Way>();
        while (ways.hasNext()) {
            Way way = ways.next();
            if (lookForRegEx == null)
                lookForRegEx = NameHelper.buildNameSearchRegexp(aLookFor);

            String name = WayHelper.getTag(way, Tags.TAG_NAME);  // e.g. "XYZSteet"
            if (name != null && NameHelper.normalizeName(name).matches(lookForRegEx)) {
                retval.add(way);
                continue;
            }
            name = WayHelper.getTag(way, Tags.TAG_REF); // e.g. "A81"
            if (name != null && name.equalsIgnoreCase(aLookFor)) {
                retval.add(way);
                continue;
            }
            name = WayHelper.getTag(way, Tags.TAG_NAT_REF); // e.g. "B31a"
            if (name != null && name.equalsIgnoreCase(aLookFor)) {
                retval.add(way);
                continue;
            }
            name = WayHelper.getTag(way, Tags.TAG_INT_REF); //e.g. "E312"
            if (name != null && name.equalsIgnoreCase(aLookFor)) {
                retval.add(way);
                continue;
            }
        }
        return retval.iterator();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Way> getWaysByTag(final String aKey, final String aValue) {
        LinkedList<Way> retval = new LinkedList<Way>();
        Iterator<Way> ways = this.getWays(Bounds.WORLD);
        while (ways.hasNext()) {
            Way way = ways.next();
            String value = WayHelper.getTag(way, aKey);  // e.g. "XYZSteet"
            if (value != null) {
                if (aValue != null && !value.equals(aValue))
                    continue;
                retval.add(way);
            }
        }

        return retval.iterator();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void removeRelation(final Relation aRelation) {
        try {
            RelationsFile relations = getRelationsFile();
            relations.removeRelation(aRelation);
            NodesFile nodes = getNodesFile();
            WaysFile  ways  = getWaysFile();
            for (RelationMember member : aRelation.getMembers()) {
                if (member.getMemberType() == EntityType.Node) {
                    long recordNr = nodes.findRecordForNode(member.getMemberId());
                    ExtendedNode node = nodes.readNode(member.getMemberId(), recordNr);
                    if (node != null && node.getReferencedRelationIDs().remove(aRelation.getId())) {
                        nodes.writeNode(node, recordNr);
                    }
                }

                if (member.getMemberType() == EntityType.Way) {
                    long recordNr = ways.findRecordForWay(member.getMemberId());
                    ExtendedWay way = ways.readWay(member.getMemberId(), recordNr);
                    if (way != null && way.getReferencedRelationIDs().remove(aRelation.getId())) {
                        ways.writeWay(way, recordNr);
                    }
                }

//                if (member.getMemberType() == EntityType.Relation) {
//                    //we do not store back-references of relations yet
//                }

            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "[IOException] Problem in "
                       + getClass().getName(),
                         e);
            IllegalStateException weDontWork = new IllegalStateException(e.getMessage());
            weDontWork.initCause(e);
            throw weDontWork;
        }
    }


    /**
     * Close all files.
     */
    @Override
    public void shutdown() {
        if (this.relationsFile != null) {
            try {
                this.relationsFile.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close relations.obm", e);
            }
            this.waysFile = null;
        }
        if (this.waysFile != null) {
            try {
                this.waysFile.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close ways.obm", e);
            }
            this.waysFile = null;
        }
        if (this.nodesFile != null) {
            try {
                this.nodesFile.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close nodes.obm", e);
            }
            this.nodesFile = null;
        }
        if (this.relationsIndex != null) {
            try {
                this.relationsIndex.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close relations-index", e);
            }
            this.relationsIndex = null;
        }
        if (this.waysIndex != null) {
            try {
                this.waysIndex.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close ways-index", e);
            }
            this.waysIndex = null;
        }
        if (this.nodesIndex != null) {
            try {
                this.nodesIndex.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close nodes-index", e);
            }
            this.nodesIndex = null;
        }

        if (this.nodes2DIndex != null) {
            try {
                this.nodes2DIndex.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close nodes-2Dindex", e);
            }
            this.nodes2DIndex = null;
        }
        if (this.attrNamesFile != null) {
            try {
                this.attrNamesFile.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot close attrnames.txt", e);
            }
            this.attrNamesFile = null;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationSection getSettings() {
        return null;
    }

}


