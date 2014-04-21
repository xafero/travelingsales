/**
 * This file is part of LibOSM by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  LibOSM is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LibOSM is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LibOSM.  If not, see <http://www.gnu.org/licenses/>.
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

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.visitors.Visitor;
import org.openstreetmap.osm.io.HsqldbDatabaseLoader;
import org.openstreetmap.osm.io.IDatabaseLoader;
//import org.openstreetmap.osm.io.MysqlDatabaseLoader;

import org.openstreetmap.osmosis.core.database.DatabaseLoginCredentials;
import org.openstreetmap.osmosis.core.database.DatabasePreferences;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;

/**
 * This DataSet uses a database to read the map from.<br/>
 * TODO: implement removing elements
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class DBDataSet implements IDataSet {

    /**
     * In {@link #getNearestNode(LatLon, Selector)}
     * start with a area of 2x{@value #GETNEARESTNODESTARTAREA}
     * and double it's size {@link #GETNEARESTNODESTOPITERATION} times
     * before trying to load the whole planet.
     */
    private static final int GETNEARESTNODESTOPITERATION = 10;

    /**
     * In {@link #getNearestNode(LatLon, Selector)}
     * start with a area of 2x{@value #GETNEARESTNODESTARTAREA}
     * and double it's size {@link #GETNEARESTNODESTOPITERATION} times
     * before trying to load the whole planet.
     */
    private static final double GETNEARESTNODESTARTAREA = 0.01;

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(DBDataSet.class.getName());

//    /**
//     * Create a DBDataSet for MySQL.
//     * @param credentials username and password for the database
//     */
//    public DBDataSet(final DatabaseLoginCredentials credentials) {
//        myCredentials = credentials;
//    }

    /**
     * Create a DBDataSet for HSQLDB..
     */
    public DBDataSet() {
        myCredentials = null;
    }

    /**
     * username and password for the database.
     */
    private DatabaseLoginCredentials myCredentials;

    /**
     * Cache for {@link #getLoader()}.
     */
    private IDatabaseLoader myLoader;

    /**
     * @return the MysqlDatabaseLoader we use to fetch map-data from the DB.
     */
    private IDatabaseLoader getLoader() {
        if (myLoader == null) {
            //if (myCredentials == null) {
                // HSQLDB
                myLoader = new HsqldbDatabaseLoader();
            //} //else {
              //  myLoader = new MysqlDatabaseLoader(myCredentials, new DatabasePreferences(true, false));
            //}
        }
        return myLoader;
    }


    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#addNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public void addNode(final Node aW) {
    	// optimistically try to insert the node
    	try {
			getLoader().createNode(aW);
			return;
		} catch (Exception e) {
		}

		// this Exception may have been due to the
		// node already existing. Test for this.
    	Node existingNode = getNodeByID(aW.getId());

        // update existing way if the new one is newer
        if (existingNode != null
        		&& existingNode.getTimestamp() != null
        		&& aW.getTimestamp() != null
        		&& existingNode.getTimestamp().before(aW.getTimestamp())) {
            removeNode(existingNode);
            existingNode = null;
        }

        // add missing way
        if (existingNode == null) {
            getLoader().createNode(aW);
        }
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#addRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public void addRelation(final Relation aRelation) {
    	// optimistically try to insert the relation
    	try {
			getLoader().createRelation(aRelation);
			return;
		} catch (Exception e) {
		}

		// this Exception may have been due to the
		// relation already existing. Test for this.
        Relation existingRelation = getRelationByID(aRelation.getId());

        // update existing way if the new one is newer
        if (existingRelation != null
        		&& existingRelation.getTimestamp() != null
        		&& aRelation.getTimestamp() != null         
        		&& existingRelation.getTimestamp().before(aRelation.getTimestamp())) {
            removeRelation(existingRelation);
            existingRelation = null;
        }

        // add missing way
        if (existingRelation == null) {
            getLoader().createRelation(aRelation);
        }

    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#addWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public void addWay(final Way aW) {
    	// optimistically try to insert the way
    	try {
			getLoader().createWay(aW);
			return;
		} catch (Exception e) {
		}

		// this Exception may have been due to the
		// way already existing. Test for this.
        Way existingWay = getWaysByID(aW.getId());

        // update existing way if the new one is newer
        if (existingWay != null
        		&& existingWay.getTimestamp() != null
        		&& aW.getTimestamp() != null
        		&& existingWay.getTimestamp().before(aW.getTimestamp())) {
            removeWay(existingWay);
            existingWay = null;
        }

        // add missing way
        if (existingWay == null) {
            getLoader().createWay(aW);
        }
    }

    /**
     * ${@inheritDoc}}.
     * @see org.openstreetmap.osm.data.IDataSet#containsNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public boolean containsNode(final Node aW) {
        return getNodeByID(aW.getId()) != null;
    }

    /**
     * ${@inheritDoc}}.
     * @see org.openstreetmap.osm.data.IDataSet#containsRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public boolean containsRelation(final Relation aR) {
        return getRelationByID(aR.getId()) != null;
    }

    /**
     * ${@inheritDoc}}.
     * @see org.openstreetmap.osm.data.IDataSet#containsWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public boolean containsWay(final Way aW) {
        return getWaysByID(aW.getId()) != null;
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getNearestNode(org.openstreetmap.osm.data.coordinates.LatLon, org.openstreetmap.osm.data.Selector)
     */
    public Node getNearestNode(final LatLon aLastGPSPos, final Selector aSelector) {
        LOG.log(Level.FINE, "using very slow testcode: getNearestNode");

        // load increasing areas until at least one node is found
        double area = GETNEARESTNODESTARTAREA;
        ReleasableIterator<Node> result = null;
        for (int i = 0; i < GETNEARESTNODESTOPITERATION; i++) {
            result = getLoader().loadAllNodesInBoundingBox(aLastGPSPos.lat() - area, aLastGPSPos.lon() - area,
                                                           aLastGPSPos.lat() + area, aLastGPSPos.lon() + area);
            if (result.hasNext())
                break;
            area *= 2;
        }
        // load all of the planet
        if (!result.hasNext()) {
            result = getLoader().loadAllNodes();
        }

        // find the nearest node
        double minDist = Double.MAX_VALUE;
        Node minDistNode = null;
        while (result.hasNext()) {
            Node node = result.next();
            if (aSelector != null && !aSelector.isAllowed(this, node))
                continue;
            LatLon pos = new LatLon(node.getLatitude(), node.getLongitude());
            double dist = pos.distance(aLastGPSPos);

            if (dist < minDist) {
                minDist = dist;
                minDistNode = node;
            }
        }
        return minDistNode;
    }

    /**
     * ${@inheritDoc}}.
     * @see org.openstreetmap.osm.data.IDataSet#getNodeByID(long)
     */
    public Node getNodeByID(final long aNodeID) {
        try {
            return getLoader().loadNode(aNodeID);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Exception in DBDataSet.getNodeById(" + aNodeID + ") - acting as if we did not have that node.", e);
            return null;
        }
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getNodesByName(java.lang.String)
     */
    public Iterator<Node> getNodesByName(final String aLookFor) {
        //LOG.log(Level.FINE, "using very slow testcode: getNodesByName");
        //return createLoader().parseOsm().getNodesByName(aLookFor);

        ReleasableIterator<Node> reader;
        reader = getLoader().loadNodesByName(aLookFor);
        return new ReleasingIterator<Node>(reader);
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getRelationByID(long)
     */
    public Relation getRelationByID(final long aRelationID) {
        try {
            return getLoader().loadRelation(aRelationID);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Exception in DBDataSet.getRelationByID(" + aRelationID + ") - acting as if we did not have that relation.", e);
            return null;
        }
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWayHelper()
     */
    public WayHelper getWayHelper() {
        return new WayHelper(this);
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWaysByID(long)
     */
    public Way getWaysByID(final long aWayID) {
        try {
            return getLoader().loadWay(aWayID);
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Exception in DBDataSet.getWaysByID(" + aWayID + ") - acting as if we did not have that way.", e);
            return null;
        }
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWaysByName(java.lang.String)
     */
    public Iterator<Way> getWaysByName(final String aLookFor, final Bounds aBoundingBox) {
        if (aBoundingBox != null) {
            return new ReleasingIterator<Way>(getLoader().loadWaysByName(aLookFor,
                    aBoundingBox.getMin().lat(), aBoundingBox.getMin().lon(),
                    aBoundingBox.getMax().lat(), aBoundingBox.getMax().lon()));
        } else {
            return new ReleasingIterator<Way>(getLoader().loadWaysByName(aLookFor));
        }
    }

    /**
     * @param aKey a key to look for
     * @param aValue (may be null) a value for the give key to look for
     * @return all ways containing the given key (with the given value is specified)
     */
    public Iterator<Way> getWaysByTag(final String aKey, final String aValue) {
        return new ReleasingIterator<Way>(getLoader().loadWaysByTag(aKey, aValue));
    }

    /**
     * @param aKey a key to look for
     * @param aValue (may be null) a value for the give key to look for
     * @return all nodes containing the given key (with the given value is specified)
     */
    public Iterator<Node> getNodesByTag(final String aKey, final String aValue) {
        return new ReleasingIterator<Node>(getLoader().loadNodesByTag(aKey, aValue));
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWaysForNode(long)
     */
    public Iterator<Way> getWaysForNode(final long aNodeID) {
        return new ReleasingIterator<Way>(getLoader().loadWaysForNode(aNodeID));
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#removeNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public void removeNode(final Node aW) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#removeRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public void removeRelation(final Relation aR) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#removeWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public void removeWay(final Way aW) {
        // TODO Auto-generated method stub

    }

    /**
     * Let the visitor visit all segments, then all ways, then all nodes.
     * @param aVisitor the visitor
     * @param boundingBox visit only what is or intersects this boundingBox.
     */
    public void visitAll(final Visitor aVisitor, final Bounds boundingBox) {
        for (final Iterator<Way> iter = this.getWays(boundingBox); iter.hasNext();) {
            Way element = iter.next();
            aVisitor.visit(element);
        }
        for (final Iterator<Node> iter = this.getNodes(boundingBox); iter.hasNext();) {
            Node element = iter.next();
            aVisitor.visit(element);
        }
        for (final Iterator<Relation> iter = this.getRelations(boundingBox); iter.hasNext();) {
            Relation element = iter.next();
            aVisitor.visit(element);
        }
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getNodes(org.openstreetmap.osm.data.coordinates.Bounds)
     */
    public Iterator<Node> getNodes(final Bounds aBoundingBox) {
        ReleasableIterator<Node> reader;
        reader = getLoader().loadAllNodesInBoundingBox(aBoundingBox.getMin().lat(), aBoundingBox.getMin().lon(),
                aBoundingBox.getMax().lat(), aBoundingBox.getMax().lon());
        return new ReleasingIterator<Node>(reader);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getRelations(org.openstreetmap.osm.data.coordinates.Bounds)
     */
    public Iterator<Relation> getRelations(final Bounds aBoundingBox) {
        return getLoader().loadBoundingBox(aBoundingBox.getMin().lat(), aBoundingBox.getMin().lon(),
                aBoundingBox.getMax().lat(), aBoundingBox.getMax().lon()).getRelations(aBoundingBox);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWays(org.openstreetmap.osm.data.coordinates.Bounds)
     */
    public Iterator<Way> getWays(final Bounds aBoundingBox) {
        ReleasableIterator<Way> reader;
        reader = getLoader().loadAllWaysInBoundingBox(aBoundingBox.getMin().lat(), aBoundingBox.getMin().lon(),
                aBoundingBox.getMax().lat(), aBoundingBox.getMax().lon());
        return new ReleasingIterator<Way>(reader);
    }

    /**
     * This is a helper-class that takes a {@link ReleasableIterator}
     * and calls it's {@link ReleasableIterator#release()} -function
     * when the last element has been reat.
     * @param <T> the type of the ReleasableIterator.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private static class ReleasingIterator<T> implements Iterator<T> {

        /**
         * The iterator we are a frontend for.
         */
        private ReleasableIterator<T> myIter;

        /**
         * @param aIter The iterator we are a frontend for.
         */
        public ReleasingIterator(final ReleasableIterator<T> aIter) {
            myIter = aIter;
        }

        /**
         * ${@inheritDoc}.
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            boolean hasNext = myIter.hasNext();
            if (!hasNext)
                myIter.release();
            return hasNext;
        }

        /**
         * ${@inheritDoc}.
         * @see java.util.Iterator#next()
         */
        public T next() {
            return myIter.next();
        }

        /**
         * ${@inheritDoc}.
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            myIter.remove();
        };
    }

    /**
     * Commit and checkpoint the datbase.
     */
    public void commit() {
        getLoader().commit();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        getLoader().shutdown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationSection getSettings() {
        return null;
    }

}
