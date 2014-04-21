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
 *  Created: 08.11.2007
 */
package org.openstreetmap.osm.data;

import java.util.Iterator;
import java.util.LinkedList;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * This DataSet contains 2 child DataSets.
 * It redirects all queries to the first one and
 * if they cannot be answered there it uses the
 * second one.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class LoadingDataSet implements IDataSet {

    /**
     * We store everything loaded from {@link #myDataSource}
     * in here and ask every query here first.
     */
    private IDataSet myCachingDataSet = new MemoryDataSet();

    /**
     * Everything {@link #myCachingDataSet} cannot answer
     * is loaded from here.
     */
    private IDataSet myDataSource = new DBDataSet();

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#addNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public void addNode(final Node aW) {
        myDataSource.addNode(aW);
        cacheNode(aW);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#addRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public void addRelation(final Relation aR) {
        myCachingDataSet.addRelation(aR);
        myDataSource.addRelation(aR);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#addWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public void addWay(final Way aW) {
        myCachingDataSet.addWay(aW);
        myDataSource.addWay(aW);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#containsNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public boolean containsNode(final Node aW) {
        if (myCachingDataSet.containsNode(aW))
            return true;
        Node n = myDataSource.getNodeByID(aW.getId());
        if (n == null)
            return false;
        cacheNode(n);
        return true;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#containsRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public boolean containsRelation(final Relation aR) {
        if (myCachingDataSet.containsRelation(aR))
            return true;
        Relation n = myDataSource.getRelationByID(aR.getId());
        if (n == null)
            return false;
        myCachingDataSet.addRelation(n);
        return true;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#containsWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public boolean containsWay(final Way aW) {
        if (myCachingDataSet.containsWay(aW))
            return true;
        Way n = myDataSource.getWaysByID(aW.getId());
        if (n == null)
            return false;
        myCachingDataSet.addWay(n);
        return true;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getNearestNode(org.openstreetmap.osm.data.coordinates.LatLon, org.openstreetmap.osm.data.Selector)
     */
    public Node getNearestNode(final LatLon aLastGPSPos, final Selector aSelector) {
        Node n = myCachingDataSet.getNearestNode(aLastGPSPos, aSelector);
        if (n == null) {
            n = myDataSource.getNearestNode(aLastGPSPos, aSelector);
            if (n != null)
                cacheNode(n);
        }
        return n;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getNodeByID(long)
     */
    public Node getNodeByID(final long aNodeID) {
        Node n = myCachingDataSet.getNodeByID(aNodeID);
        if (n == null) {
            n = myDataSource.getNodeByID(aNodeID);
            if (n != null)
                cacheNode(n);
        }
        return n;
    }

    /**
     * We have to get all ways for each node cached
     * to get no inconsistencies when {@link #getWaysForNode(long)} is called.
     * @param n the node to add to {@link #myCachingDataSet}.
     */
    private void cacheNode(final Node n) {
        myCachingDataSet.addNode(n);

        Iterator<Way> ways = myDataSource.getWaysForNode(n.getId());
        if (ways != null && ways.hasNext()) {
            while (ways.hasNext()) {
                Way way = ways.next();
                myCachingDataSet.addWay(way);
            }
        }
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getNodes(org.openstreetmap.osm.data.coordinates.Bounds)
     */
    public Iterator<Node> getNodes(final Bounds aBoundingBox) {
        Iterator<Node> n = myCachingDataSet.getNodes(aBoundingBox);
        if (n == null || !n.hasNext()) {
            n = myDataSource.getNodes(aBoundingBox);
            if (n != null && n.hasNext()) {
                LinkedList<Node> nodes = new LinkedList<Node>();
                while (n.hasNext()) {
                    Node node = n.next();
                    nodes.add(node);
                    cacheNode(node);
                    return nodes.iterator();
                }
            }
        }
        return n;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getNodesByName(java.lang.String)
     */
    public Iterator<Node> getNodesByName(final String aLookFor) {
        Iterator<Node> n = myCachingDataSet.getNodesByName(aLookFor);
        if (n == null || !n.hasNext()) {
            n = myDataSource.getNodesByName(aLookFor);
            if (n != null && n.hasNext()) {
                LinkedList<Node> nodes = new LinkedList<Node>();
                while (n.hasNext()) {
                    Node node = n.next();
                    nodes.add(node);
                    cacheNode(node);
                    return nodes.iterator();
                }
            }
        }
        return n;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getRelationByID(long)
     */
    public Relation getRelationByID(final long aRelationID) {
        Relation n = myCachingDataSet.getRelationByID(aRelationID);
        if (n == null) {
            n = myDataSource.getRelationByID(aRelationID);
            if (n != null)
                myCachingDataSet.addRelation(n);
        }
        return n;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getRelations(org.openstreetmap.osm.data.coordinates.Bounds)
     */
    public Iterator<Relation> getRelations(final Bounds aBoundingBox) {
        Iterator<Relation> n = myCachingDataSet.getRelations(aBoundingBox);
        if (n == null || !n.hasNext()) {
            n = myDataSource.getRelations(aBoundingBox);
            if (n != null && n.hasNext()) {
                LinkedList<Relation> nodes = new LinkedList<Relation>();
                while (n.hasNext()) {
                    Relation node = n.next();
                    nodes.add(node);
                    myCachingDataSet.addRelation(node);
                    return nodes.iterator();
                }
            }
        }
        return n;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWayHelper()
     */
    public WayHelper getWayHelper() {
        return myCachingDataSet.getWayHelper();
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWays(org.openstreetmap.osm.data.coordinates.Bounds)
     */
    public Iterator<Way> getWays(final Bounds aBoundingBox) {
        Iterator<Way> n = myCachingDataSet.getWays(aBoundingBox);
        if (n == null || !n.hasNext()) {
            n = myDataSource.getWays(aBoundingBox);
            if (n != null && n.hasNext()) {
                LinkedList<Way> nodes = new LinkedList<Way>();
                while (n.hasNext()) {
                    Way node = n.next();
                    nodes.add(node);
                    myCachingDataSet.addWay(node);
                    return nodes.iterator();
                }
            }
        }
        return n;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWaysByID(long)
     */
    public Way getWaysByID(final long aWayID) {
        Way n = myCachingDataSet.getWaysByID(aWayID);
        if (n == null) {
            n = myDataSource.getWaysByID(aWayID);
            if (n != null)
                myCachingDataSet.addWay(n);
        }
        return n;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWaysByName(java.lang.String)
     */
    public Iterator<Way> getWaysByName(final String aLookFor, final Bounds boundingbox) {
        Iterator<Way> n = myCachingDataSet.getWaysByName(aLookFor, boundingbox);
        if (n == null || !n.hasNext()) {
            n = myDataSource.getWaysByName(aLookFor, boundingbox);
            if (n != null && n.hasNext()) {
                LinkedList<Way> nodes = new LinkedList<Way>();
                while (n.hasNext()) {
                    Way way = n.next();
                    nodes.add(way);
                    myCachingDataSet.addWay(way);
                }
                return nodes.iterator();
            }
        }
        return n;
    }

    /**
     * @param aKey a key to look for
     * @param aValue (may be null) a value for the give key to look for
     * @return all ways containing the given key (with the given value is specified)
     */
    public Iterator<Way> getWaysByTag(final String aKey, final String aValue) {
        Iterator<Way> n = myCachingDataSet.getWaysByTag(aKey, aValue);
        if (n == null || !n.hasNext()) {
            n = myDataSource.getWaysByTag(aKey, aValue);
            if (n != null && n.hasNext()) {
                LinkedList<Way> nodes = new LinkedList<Way>();
                while (n.hasNext()) {
                    Way way = n.next();
                    nodes.add(way);
                    myCachingDataSet.addWay(way);
                    return nodes.iterator();
                }
            }
        }
        return n;
    }

    /**
     * @param aKey a key to look for
     * @param aValue (may be null) a value for the give key to look for
     * @return all nodes containing the given key (with the given value is specified)
     */
    public Iterator<Node> getNodesByTag(final String aKey, final String aValue) {
        Iterator<Node> n = myCachingDataSet.getNodesByTag(aKey, aValue);
        if (n == null || !n.hasNext()) {
            n = myDataSource.getNodesByTag(aKey, aValue);
            if (n != null && n.hasNext()) {
                LinkedList<Node> nodes = new LinkedList<Node>();
                while (n.hasNext()) {
                    Node node = n.next();
                    nodes.add(node);
                    myCachingDataSet.addNode(node);
                    return nodes.iterator();
                }
            }
        }
        return n;
    }


    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWaysForNode(long)
     */
    public Iterator<Way> getWaysForNode(final long aNodeID) {
        Iterator<Way> n = myCachingDataSet.getWaysForNode(aNodeID);
        if (n == null || !n.hasNext()) {
            n = myDataSource.getWaysForNode(aNodeID);
            if (n != null && n.hasNext()) {
                LinkedList<Way> nodes = new LinkedList<Way>();
                while (n.hasNext()) {
                    Way node = n.next();
                    nodes.add(node);
                    myCachingDataSet.addWay(node);
                }
                return nodes.iterator();
            }
        }
        return n;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#removeNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public void removeNode(final Node aW) {
        myCachingDataSet.removeNode(aW);
        myDataSource.removeNode(aW);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#removeRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public void removeRelation(final Relation aR) {
        myCachingDataSet.removeRelation(aR);
        myDataSource.removeRelation(aR);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#removeWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public void removeWay(final Way aW) {
        myCachingDataSet.removeWay(aW);
        myDataSource.removeWay(aW);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationSection getSettings() {
        return null;
    }

}
