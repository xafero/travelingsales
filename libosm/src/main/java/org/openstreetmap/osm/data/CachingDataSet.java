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

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.visitors.Visitor;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;


/**
 * A CachingDataSet stores its data in SoftReferences.
 * Thus the data may be garbage-collected at any time.
 * It contains a {@link #myBackingDataSet} that is asked
 * uppon a cache-miss..
 *
 * We guarantee that for any node that is present
 * all ways and relations containing it are also present.
 *
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class CachingDataSet implements IDataSet {

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(CachingDataSet.class.getName());

    /**
     * The size of the bounding-box to read
     * around nodes that have been requested.
     */
    private static final double DEFAULTREADAHEADSIZE = 0.02;

    /**
     * All nodes goes here, even when included in other data (ways etc).
     * This enables the instant conversion of the whole DataSet by iterating over
     * this data structure.
     */
    private HashMap<Long, SoftReference<Node>> nodesByID = new HashMap<Long, SoftReference<Node>>();

//    private Stack<Node> leastRecentlyUsedNodes = new Stack<Node>();

    /**
     * This method gets nodes as they are
     * returned to fill an LRU-list.
     * @param n the node returned.
     */
    private void putLeastRecentlyUsed(final Node n) {
  /*      if (n == null)
            return;

        if (leastRecentlyUsedNodes.size() == 1024)
            leastRecentlyUsedNodes.remove(0);
        leastRecentlyUsedNodes.push(n);*/
    }

    /**
     * All ways (Streets etc.) in the DataSet.
     *
     * The nodes of this way must be objects from
     * {@link #nodesByID}
     */
    private HashMap<Long, SoftReference<Way>> waysByID = new HashMap<Long, SoftReference<Way>>();

    /**
     * All relations in the DataSet.
     * The nodes of this relation must be objects from
     * {@link #nodesByID}}
     * The ways of this relation must be objects from
     * {@link #wayByID}}
     * The sub-relations of this relation must be objects from
     * {@link #relationByID}}
     */
    private HashMap<Long, SoftReference<Relation>> relationByID = new HashMap<Long, SoftReference<Relation>>();

    /**
     * All ways (Streets etc.) in the DataSet indexed by node-id.
     * These lists are strong-references to the ways. Thus
     * as long as the entry exists,
     */
    private HashMap<Long, SoftReference<List<Way>>> waysByNodeID = new HashMap<Long, SoftReference<List<Way>>>();

    /**
     * The Helper-Functions for Segment.
     */
    private WayHelper myWayHelper = new WayHelper(this);

    /**
     * The Helper-Functions for Segment.
     */
    private NodeHelper myNodeHelper = new NodeHelper(this);

    /**
     * If the cache cannot answer a request,
     * the backingDataSet does.
     */
    private IDataSet myBackingDataSet;

    /**
     * @return If the cache cannot answer a request,
     *         the backingDataSet does.
     */
    protected IDataSet getBackingDataSet() {
        return myBackingDataSet;
    }

    /**
     * Create a new CachingDataSet that uses
     * the given IDataSet to answer questions that
     * cannot be answered from the cache.
     * @param backingDataSet the IDataSet to back us
     */
    public CachingDataSet(final IDataSet backingDataSet) {
        if (backingDataSet == null)
            throw new IllegalArgumentException("null backing-data.set given!");
        this.myBackingDataSet = backingDataSet;
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#addWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public void addWay(final Way w) {
        if (w == null) {
            throw new IllegalArgumentException("null way given");
        }
        getBackingDataSet().addWay(w);

        cacheWay(w);
    }

    /**
     * @param w the way to add to the cache
     */
    private void cacheWay(final Way w) {
        this.waysByID.put(w.getId(), new SoftReference<Way>(w));

//      add by segments
        for (WayNode nodeRef : w.getWayNodes()) {

            SoftReference<List<Way>> listRef = this.waysByNodeID.get(nodeRef.getNodeId());
            if (listRef != null) {
                List<Way> list = listRef.get();
                if (list != null) {
                    list.add(w);
                } else {
                    // the list has been garbage-collected
                    //list = getWaysForNode_internal(nodeRef.getNodeId());
                    //list.add(w);

                    // remove the stale SoftReference
                    this.waysByNodeID.remove(nodeRef.getNodeId());
                }
            } else  {
                List<Way> list = new LinkedList<Way>();
                list.add(w);
                this.waysByNodeID.put(nodeRef.getNodeId(), new SoftReference<List<Way>>(list));
            }
        }
    }

    /**
     * @param w may be null (ignored then)
     */
    public void removeWay(final Way w) {
        getBackingDataSet().removeWay(w);

        this.waysByID.remove(w.getId());

//      remove by segments
        for (WayNode nodeRef : w.getWayNodes()) {

            SoftReference<List<Way>> listRef = this.waysByNodeID.get(nodeRef.getNodeId());
            if (listRef != null) {
                List<Way> list = listRef.get();
                if (list != null) {
                    list.remove(w);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#containsWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public boolean containsWay(final Way w) {
        if (waysByID.containsKey(w.getId()))
            return true;
        return getBackingDataSet().containsWay(w);
    }

    /**
     * @return all ways we know (be carefull, can be MANY)
     * @param boundingBox visit only what is or intersects this boundingBox.
     */
    public Iterator<Way> getWays(final Bounds boundingBox) {

        return new Iterator<Way>() {
            private Iterator<Way> nodes = getBackingDataSet().getWays(boundingBox);

            public boolean hasNext() {
                return nodes.hasNext();
            }

            public Way next() {
                Way n = nodes.next();
                cacheWay(n);
                return n;
            }

            public void remove() {
                throw new IllegalArgumentException("removing is not allowed");
            }
        };
    }


    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#addRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public void addRelation(final Relation r) {
        if (r == null) {
            throw new IllegalArgumentException("null way given");
        }

        getBackingDataSet().addRelation(r);

        cacheRelation(r);
    }

    /**
     * @param r the relation to add to our cache
     */
    private void cacheRelation(final Relation r) {
        this.relationByID.put(r.getId(), new SoftReference<Relation>(r));
    }

    /**
     * Does only remove the relation and not relations
     * that contain this one, nor ways or segments
     * in the relatiton.
     * @param r may be null (ignored then)
     */
    public void removeRelation(final Relation r) {
        this.relationByID.remove(r.getId());
//        for (Relation parentRelation : relationByID.values()) {
//            List<RelationMember> memberList = parentRelation.getMemberList();
//            for (RelationMember member : memberList) {
//                if (member.getMemberType() == EntityType.Relation)
//                    if (member.getMemberId() == r.getId())
//                        removeRelation(getRelationByID(r.getId()));
//            }
//        }
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#containsRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public boolean containsRelation(final Relation r) {
        if (relationByID.containsKey(r.getId()))
            return true;
        return getBackingDataSet().containsRelation(r);
    }

    /**
     * @return all relations we know (be carefull, can be MANY)
     * @param boundingBox visit only what is or intersects this boundingBox.
     */
    public Iterator<Relation> getRelations(final Bounds boundingBox) {

        return new Iterator<Relation>() {
            private Iterator<Relation> nodes = getBackingDataSet().getRelations(boundingBox);

            public boolean hasNext() {
                return nodes.hasNext();
            }

            public Relation next() {
                Relation n = nodes.next();
                cacheRelation(n);
                return n;
            }

            public void remove() {
                throw new IllegalArgumentException("removing is not allowed");
            }
        };
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#getRelationByID(long)
     */
    public Relation getRelationByID(final long aRelationID) {
        SoftReference<Relation> ref = this.relationByID.get(aRelationID);
        if (ref != null) {
            Relation rel = ref.get();
            if (rel != null)
                return rel;
        }
        Relation rel = getBackingDataSet().getRelationByID(aRelationID);
        if (rel != null)
            cacheRelation(rel);
        return rel;
    }


    /**
     * @param lookFor the way-name to look for (ignores cases but does not check for alternative spellings)
     * @param boundingBox give only what is or intersects this boundingBox. (may be null)
     * @return all known ways with that name
     */
    public Iterator<Way> getWaysByName(final String lookFor, final Bounds boundingBox) {
        return getBackingDataSet().getWaysByName(lookFor, boundingBox);
    }

    /**
     * @param aKey a key to look for
     * @param aValue (may be null) a value for the give key to look for
     * @return all ways containing the given key (with the given value is specified)
     */
    public Iterator<Way> getWaysByTag(final String aKey, final String aValue) {
        return getBackingDataSet().getWaysByTag(aKey, aValue);
    }

    /**
     * @param aKey a key to look for
     * @param aValue (may be null) a value for the give key to look for
     * @return all nodes containing the given key (with the given value is specified)
     */
    public Iterator<Node> getNodesByTag(final String aKey, final String aValue) {
        return getBackingDataSet().getNodesByTag(aKey, aValue);
    }

    /**
     * @param lookFor the node-name to look for (ignores cases but does not check for alternative spellings)
     * @return all known nodes with that name
     */
    public Iterator<Node> getNodesByName(final String lookFor) {
        return getBackingDataSet().getNodesByName(lookFor);
    }

    /**
     * @return the number of nodes in the map.
     */
//    public int getNodesCount() {
//        return nodesByID.size();
//    }


    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#addNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public void addNode(final Node w) {
        if (w == null) {
            throw new IllegalArgumentException("null node given");
        }
        getBackingDataSet().addNode(w);
        cacheNode(w);
    }

    /**
     * @param w the node to add to our cache
     */
    private void cacheNode(final Node w) {
        this.nodesByID.put(w.getId(), new SoftReference<Node>(w));
        putLeastRecentlyUsed(w);
    }

    /**
     * @param w node be null (ignored then)
     */
    public void removeNode(final Node w) {
        getBackingDataSet().removeNode(w);
        this.nodesByID.remove(w.getId());
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#containsNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public boolean containsNode(final Node w) {
        if (nodesByID.containsKey(w.getId()))
            return true;
        return getBackingDataSet().containsNode(w);
    }

    /**
     * @return all nodes we know (be carefull, can be MANY)
     * @param boundingBox visit only what is or intersects this boundingBox.
     */
    public Iterator<Node> getNodes(final Bounds boundingBox) {

        return new Iterator<Node>() {
            private Iterator<Node> nodes = getBackingDataSet().getNodes(boundingBox);

            public boolean hasNext() {
                return nodes.hasNext();
            }

            public Node next() {
                Node n = nodes.next();
                cacheNode(n);
                return n;
            }

            public void remove() {
                throw new IllegalArgumentException("removing is not allowed");
            }
        };
    }

    /**
     * Return all ways in this dataset that contain
     * this node.
     * @param nodeID the node to look for
     * @return an iterator over the list
     */
    public Iterator<Way> getWaysForNode(final long nodeID) {
        return getWaysForNodeInternal(nodeID).iterator();
    }

    /**
     * Return all ways in this dataset that contain
     * this node.
     * @param nodeID the node to look for
     * @return an iterator over the list
     */
    protected List<Way> getWaysForNodeInternal(final long nodeID) {
        SoftReference<List<Way>> listRef = this.waysByNodeID.get(nodeID);
        if (listRef != null) {
            List<Way> list = listRef.get();
            if (list != null) {
                return list;
            }
        }

        // the list has been garbage-collected
        // we need to build it anew
        List<Way> list = new LinkedList<Way>();
        Iterator<Way> waysForNode = getBackingDataSet().getWaysForNode(nodeID);
        while (waysForNode.hasNext()) {
            Way next = waysForNode.next();
            list.add(next);
            waysByID.put(next.getId(), new SoftReference<Way>(next));
        }
        this.waysByNodeID.put(nodeID, new SoftReference<List<Way>>(list));
        return list;
    };


    /**
     * Let the visitor visit all segments, then all ways, then all nodes.
     * @param aVisitor the visitor
     */
//    public void visitAll(final Visitor aVisitor) {
//        for (final Iterator<Way> iter = this.getWays(); iter.hasNext();) {
//            Way element = iter.next();
//            aVisitor.visit(element);
//        }
//        for (final Iterator<Node> iter = this.getNodes(); iter.hasNext();) {
//            Node element = iter.next();
//            aVisitor.visit(element);
//        }
//        for (final Iterator<Relation> iter = this.getRelations(); iter.hasNext();) {
//            Relation element = iter.next();
//            aVisitor.visit(element);
//        }
//    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#getWaysByID(long)
     */
    public Way getWaysByID(final long aWayID) {
        SoftReference<Way> ref = this.waysByID.get(aWayID);
        if (ref != null) {
            Way w = ref.get();
            if (w != null)
                return w;
        }
        Way w = getBackingDataSet().getWaysByID(aWayID);
        if (w != null)
            this.waysByID.put(w.getId(), new SoftReference<Way>(w));
        return w;
    }

    /**
     * For statistics.
     */
    private long hits = 0;

    /**
     * For statistics.
     */
    private long misses = 0;
    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.IDataSet#getNodeByID(long)
     */
    public Node getNodeByID(final long aNodeID) {
        SoftReference<Node> ref = this.nodesByID.get(aNodeID);
        if (ref != null) {
            Node n = ref.get();
            if (n != null) {
                putLeastRecentlyUsed(n);
                hits++;
                LOG.log(Level.FINEST, "CAchingDataSet.getNodeById(" + aNodeID + ") = " + n + " (from Cache)");
                return n;
            }
//            LOG.log(Level.FINEST, "getNodeByID(" + aNodeID + ") - requested node has been garbage-collected");
//            for (Node n : leastRecentlyUsedNodes) {
//                if (n.getId() == aNodeID) {
//                    LOG.log(Level.FINEST, "getNodeByID(" + aNodeID + ") - requested node has been garbage-collected BUT was in the LRU");
//                    return n;
//                }
//            }
        }
        misses++;
        Node n = getBackingDataSet().getNodeByID(aNodeID);
        if (n != null) {
//            long start = System.currentTimeMillis();
            cacheNode(n);

            // read a bounding-box around that node because
            // it is very likely that other nodes in that area will
            // be requested and huge numbers of getNodeByID that
            // each get down to the DB are very slow.
            Iterator<Node> area = getNodes(new Bounds(n.getLatitude() - DEFAULTREADAHEADSIZE, n.getLongitude() - DEFAULTREADAHEADSIZE,
                                                      n.getLatitude() + DEFAULTREADAHEADSIZE, n.getLongitude() + DEFAULTREADAHEADSIZE));
            int count = 0;
            while (area.hasNext()) {
                area.next();
                count++;
            }
//            if (LOG.isLoggable(Level.FINEST))
//                LOG.log(Level.FINEST, "getNodeByID(" + aNodeID + ") - caching the area with "
//            + count + " nodes took " + (System.currentTimeMillis() - start) + "ms "
//            + "LRU-size=" + leastRecentlyUsedNodes.size() + " misses=" + (100.0*misses/(hits+misses))+"%");
        }
        putLeastRecentlyUsed(n);
        LOG.log(Level.FINEST, "CAchingDataSet.getNodeById(" + aNodeID + ") = " + n + " (from backingDataSet)");
        return n;
    }

    /**
     * @return the modeHelper
     */
    public NodeHelper getNodeHelper() {
        return myNodeHelper;
    }

    /**
     * @return the wayHelper
     */
    public WayHelper getWayHelper() {
        return myWayHelper;
    }

    /**
     * {@inheritDoc}.
     */
    public Node getNearestNode(final LatLon aLastGPSPos, final Selector aSelector) {

        Node n = getBackingDataSet().getNearestNode(aLastGPSPos, aSelector);
        if (n != null)
            cacheNode(n);
        return n;
/*
        double minDist = Double.MAX_VALUE;
        Node minDistNode = null;
        for (Node node : nodesByID.values()) {
            if (aSelector != null && !aSelector.isAllowed(node))
                continue;
            LatLon pos = new LatLon(node.getLatitude(), node.getLongitude());
            double dist = pos.distance(aLastGPSPos);

            if (dist < minDist) {
                minDist = dist;
                minDistNode = node;
            }
        }

        return minDistNode;*/
    }

    /**
     * Let the visitor visit all segments, then all ways, then all nodes.
     * @param aVisitor the visitor
     * @param boundingBox visit only what is or intersects this boundingBox.
     */
    public void visitAll(final Visitor aVisitor, final Bounds boundingBox) {
        for (final Iterator<Way> iter = getBackingDataSet().getWays(boundingBox); iter.hasNext();) {
            Way element = iter.next();
            cacheWay(element);
            aVisitor.visit(element);
        }
        for (final Iterator<Node> iter = getBackingDataSet().getNodes(boundingBox); iter.hasNext();) {
            Node element = iter.next();
            cacheNode(element);
            aVisitor.visit(element);
        }
        for (final Iterator<Relation> iter = getBackingDataSet().getRelations(boundingBox); iter.hasNext();) {
            Relation element = iter.next();
            cacheRelation(element);
            aVisitor.visit(element);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        getBackingDataSet().shutdown();
        this.nodesByID.clear();
        this.waysByID.clear();
        this.relationByID.clear();
        this.waysByNodeID.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationSection getSettings() {
        return null;
    }
}
