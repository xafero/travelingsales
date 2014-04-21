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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
//import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedWay;
import org.openstreetmap.osm.data.searching.NameHelper;
import org.openstreetmap.osm.data.visitors.Visitor;

import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;


/**
 * DataSet is the data behind the application. It can consists of only a few
 * points up to the whole osm database. DataSet's can be merged together,
 * saved, (up/down/disk)loaded etc.
 *
 * Note, that DataSet is not an osm-primitive and so has no key association
 * but a few members to store some information.
 *
 * We make heavy use of iterators instead of collections
 * to allow for very large maps to be processed. At a later
 * time it may be very reasonable not to keep all the map
 * in memory but to use a persistency-framework like hibernate
 * using the j2ee2-persistency-api.
 *
 * @author imi
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class MemoryDataSet implements IDataSet {

    /**
     * my logger for debug and error-output.
     */
    //private static final Logger LOG = Logger.getLogger(MemoryDataSet.class.getName());

    /**
     * All nodes goes here, even when included in other data (ways etc).
     * This enables the instant conversion of the whole DataSet by iterating over
     * this data structure.
     */
    private Map<Long, Node> nodesByID = Collections.synchronizedMap(new HashMap<Long, Node>());

    /**
     * All ways (Streets etc.) in the DataSet.
     *
     * The nodes of this way must be objects from
     * {@link #nodesByID}} .
     */
    private Map<Long, Way> waysByID = Collections.synchronizedMap(new HashMap<Long, Way>());

    /**
     * All relations in the DataSet.
     * The nodes of this relation must be objects from
     * {@link #nodesByID}}
     * The ways of this relation must be objects from
     * {@link #wayByID}}
     * The sub-relations of this relation must be objects from
     * {@link #relationByID}}
     */
    private Map<Long, Relation> relationByID = Collections.synchronizedMap(new HashMap<Long, Relation>());

    /**
     * All ways (Streets etc.) in the DataSet indexed by node-id.
     */
    private Map<Long, List<Relation>> relationsByWayID = Collections.synchronizedMap(new HashMap<Long, List<Relation>>());

    /**
     * All ways (Streets etc.) in the DataSet indexed by relation-id.
     */
    private Map<Long, List<Way>> waysByNodeID = Collections.synchronizedMap(new HashMap<Long, List<Way>>());

    /**
     * The Helper-Functions for Segment.
     */
    private WayHelper myWayHelper = new WayHelper(this);

    /**
     * The Helper-Functions for Segment.
     */
    private NodeHelper myNodeHelper = new NodeHelper(this);



//    /**
//     * @return A collection containing all primitives of the dataset. The
//     * data is ordered after: first comes nodes, then segments, then ways.
//     * Ordering in between the categories is not guaranteed.
//     */
//    public List<Entity> allPrimitives() {
//        List<Entity> o = new LinkedList<Entity>();
//        o.addAll(this.nodesByID.values());
//        o.addAll(waysByID.values());
//        return o;
//    }

    /**
     * @return all ways we know (be carefull, can be MANY)
     * @param boundingBox visit only what is or intersects this boundingBox.
     */
    public Iterator<Way> getWays(final Bounds boundingBox) {
        LinkedList<Way> retval = new LinkedList<Way>();
        for (Way way : waysByID.values()) {
            for (WayNode wayNode : way.getWayNodes()) {
                Node node = getNodeByID(wayNode.getNodeId());
                if (node != null
                 && boundingBox.contains(node.getLatitude(), node.getLongitude())) {
                    retval.add(way);
                    break;
                }
            }
        }
        return retval.iterator();
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#addWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public void addWay(final Way w) {
        if (w == null) {
            throw new IllegalArgumentException("null way given");
        }

//      check if the way is newer first
        Way oldWay = getWaysByID(w.getId());
        if (oldWay != null) {
            if (w.getTimestamp() == null || oldWay.getTimestamp() == null || !oldWay.getTimestamp().after(w.getTimestamp())) {
                removeWay(oldWay);
            } else {
                return;
            }
        }

        Way temp = this.waysByID.put(w.getId(), w);

//      add by segments
        for (WayNode nodeRef : w.getWayNodes()) {

            List<Way> list = this.waysByNodeID.get(nodeRef.getNodeId());
            if (list == null) {
                list = new LinkedList<Way>();
                this.waysByNodeID.put(nodeRef.getNodeId(), list);
            }
            list.add(w);
        }
        assert temp == null;
    }

    /**
     * @param w may be null (ignored then)
     */
    public void removeWay(final Way w) {
        this.waysByID.remove(w.getId());

//      remove by segments
        for (WayNode nodeRef : w.getWayNodes()) {

            List<Way> list = this.waysByNodeID.get(nodeRef.getNodeId());
            if (list != null) {
                list.remove(w);
            }
        }
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#containsWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public boolean containsWay(final Way w) {
        return waysByID.containsKey(w.getId());
    }

    /**
     * @return all relation we know (be carefull, can be MANY)
     * @param boundingBox visit only what is or intersects this boundingBox.
     */
    public Iterator<Relation> getRelations(final Bounds boundingBox) {
        return this.relationByID.values().iterator();
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#addRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public void addRelation(final Relation r) {
        if (r == null) {
            throw new IllegalArgumentException("null relation given");
        }

//      check if the way is newer first
        Relation oldRelation = getRelationByID(r.getId());
        if (oldRelation != null) {
            if (r.getTimestamp() == null || oldRelation.getTimestamp() == null || !oldRelation.getTimestamp().after(r.getTimestamp())) {
                removeRelation(oldRelation);
            } else {
                return;
            }
        }

        Relation temp = this.relationByID.put(r.getId(), r);
        assert temp == null;
        for (RelationMember member : r.getMembers()) {
         if (member.getMemberType() == EntityType.Way) {
             List<Relation> list = this.relationsByWayID.get(member.getMemberId());
             if (list == null) {
                 list = new LinkedList<Relation>();
                 this.relationsByWayID.put(member.getMemberId(), list);
             }
             list.add(r);
         }
        }
    }

    /**
     * Does only remove the relation and not relations
     * that contain this one, nor ways or segments
     * in the relaiton.
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

        List<RelationMember> members = r.getMembers();
        for (RelationMember relationMember : members) {
            if (relationMember.getMemberType() == EntityType.Way) {

                List<Relation> list = this.relationsByWayID.get(relationMember.getMemberId());
                if (list != null) {
                    list.remove(r);
                }
                }
        }
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#containsRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public boolean containsRelation(final Relation r) {
        return relationByID.containsKey(r.getId());
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getRelationByID(long)
     */
    public Relation getRelationByID(final long aRelationID) {
        return this.relationByID.get(aRelationID);
    }


    /**
     * @param lookFor the way-name to look for (ignores cases but does not check for alternative spellings)
     * @param boundingbox return only what is in the bounding-box
     * @return all known ways with that name
     */
    public Iterator<Way> getWaysByName(final String lookFor, final Bounds boundingbox) {
//TODO: index non-uniquely by name
        String lookForRegEx = null;
        LinkedList<Way> retval = new LinkedList<Way>();
        for (Way way : this.waysByID.values()) {

            if (lookForRegEx == null)
                lookForRegEx = NameHelper.buildNameSearchRegexp(lookFor);

            String name = WayHelper.getTag(way, Tags.TAG_NAME);  // e.g. "XYZSteet"
            if (name != null && NameHelper.normalizeName(name).matches(lookForRegEx)) {
                retval.add(way);
                continue;
            }
            name = WayHelper.getTag(way, Tags.TAG_REF); // e.g. "A81"
            if (name != null && name.equalsIgnoreCase(lookFor)) {
                retval.add(way);
                continue;
            }
            name = WayHelper.getTag(way, Tags.TAG_NAT_REF); // e.g. "B31a"
            if (name != null && name.equalsIgnoreCase(lookFor)) {
                retval.add(way);
                continue;
            }
            name = WayHelper.getTag(way, Tags.TAG_INT_REF); //e.g. "E312"
            if (name != null && name.equalsIgnoreCase(lookFor)) {
                retval.add(way);
                continue;
            }
        }

//      filter by bounding-box
        if (boundingbox != null) {
            Set<Way> retval2 = new HashSet<Way>();
            for (Way way : retval) {
                for (WayNode wayNode : way.getWayNodes()) {
                    Node node = getNodeByID(wayNode.getNodeId());
                    if (node != null && boundingbox.contains(node.getLatitude(), node.getLongitude())) {
                        retval2.add(way);
                        break;
                    }
                }
            }
            return retval2.iterator();
        }

        return retval.iterator();
    }

    /**
     * @param lookFor the node-name to look for (ignores cases but does not check for alternative spellings)
     * @return all known nodes with that name
     */
    public Iterator<Node> getNodesByName(final String lookFor) {
//TODO: index non-uniquely by name
        LinkedList<Node> retval = new LinkedList<Node>();
        for (Node node : this.nodesByID.values()) {
            String name = WayHelper.getTag(node, Tags.TAG_NAME);  // e.g. "XYZSteet"
            if (name != null && name.equalsIgnoreCase(lookFor)) {
                retval.add(node);
                continue;
            }
            name = WayHelper.getTag(node, Tags.TAG_REF); // e.g. "A81"
            if (name != null && name.equalsIgnoreCase(lookFor)) {
                retval.add(node);
                continue;
            }
            name = WayHelper.getTag(node, Tags.TAG_NAT_REF); // e.g. "B31a"
            if (name != null && name.equalsIgnoreCase(lookFor)) {
                retval.add(node);
                continue;
            }
            name = WayHelper.getTag(node, Tags.TAG_INT_REF); //e.g. "E312"
            if (name != null && name.equalsIgnoreCase(lookFor)) {
                retval.add(node);
                continue;
            }
        }

        return retval.iterator();
    }

    /**
     * @return the number of nodes in the map.
     */
    public int getNodesCount() {
        return nodesByID.size();
    }

    /**
     * @return the number of ways in the map.
     */
    public int getWaysCount() {
        return waysByID.size();
    }

    /**
     * @return the number of relations in the map.
     */
    public int getRelationsCount() {
        return relationByID.size();
    }

    /**
     * @return all nodes we know (be carefull, can be MANY)
     * @param boundingBox visit only what is or intersects this boundingBox.
     */
    public Iterator<Node> getNodes(final Bounds boundingBox) {
        if (boundingBox == null || boundingBox == Bounds.WORLD)
            return this.nodesByID.values().iterator();

        Collection<Node> allNodes = this.nodesByID.values();
        List<Node> retval = new LinkedList<Node>();
        for (Node node : allNodes) {
            if (boundingBox.contains(node.getLatitude(), node.getLongitude()))
                retval.add(node);
        }
        return retval.iterator();
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#addNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public void addNode(final Node w) {
        if (w == null) {
            throw new IllegalArgumentException("null node given");
        }

//      check if the node is newer first
        Node oldNode = getNodeByID(w.getId());
        if (oldNode != null) {
            if (w.getTimestamp() == null || oldNode.getTimestamp() == null || !oldNode.getTimestamp().after(w.getTimestamp())) {
                removeNode(oldNode);
            } else {
                return;
            }
        }

        Node temp = this.nodesByID.put(w.getId(), w);
        assert temp == null;
    }

    /**
     * @param w node be null (ignored then)
     */
    public void removeNode(final Node w) {
        this.nodesByID.remove(w.getId());
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#containsNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public boolean containsNode(final Node w) {
        return nodesByID.containsKey(w.getId());
    }

    /**
     * Return all ways in this dataset that contain
     * this node.
     * @param nodeID the node to look for
     * @return an iterator over the list
     */
    public Iterator<Way> getWaysForNode(final long nodeID) {
        List<Way> list = this.waysByNodeID.get(nodeID);
        if (list == null) {
            //return (new ArrayList<Way>(list)).iterator();
            return (new LinkedList<Way>()).iterator();
        }
        //return (new LinkedList<Way>()).iterator();
        ArrayList<Way> retval = new ArrayList<Way>(list.size());
        for (Way way : list) {
            retval.add(wayToExtendedWay(way));
        }
        return retval.iterator();
    };


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
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWaysByID(long)
     */
    public Way getWaysByID(final long aWayID) {
        Way way = this.waysByID.get(aWayID);
        return wayToExtendedWay(way);
    }

    /**
     * @param way the way to complete
     * @return an extendedway
     */
    private ExtendedWay wayToExtendedWay(final Way way) {
        if (way != null) {
            ExtendedWay eWay = new ExtendedWay(way.getId(), way.getVersion(),
                    way.getTags(), way.getWayNodes());
            List<Relation> list = this.relationsByWayID.get(way.getId());
            if (list != null) {
                for (Relation relation : list) {
                    eWay.getReferencedRelationIDs().add(relation.getId());
                }
            }
            return eWay;
        }
        return null;
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getNodeByID(long)
     */
    public Node getNodeByID(final long aNodeID) {
        return this.nodesByID.get(aNodeID);
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
     * @param aLastGPSPos
     * @param aSelector
     * @return the node with the minimum distance to the given LatLon.
     */
    public Node getNearestNode(final LatLon aLastGPSPos, final Selector aSelector) {

        double minDist = Double.MAX_VALUE;
        Node minDistNode = null;
        for (Node node : nodesByID.values()) {
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
     * @param aKey a key to look for
     * @param aValue (may be null) a value for the give key to look for
     * @return all ways containing the given key (with the given value is specified)
     */
    public Iterator<Way> getWaysByTag(final String aKey, final String aValue) {

        LinkedList<Way> retval = new LinkedList<Way>();
        for (Way way : this.waysByID.values()) {
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
     * @param aKey a key to look for
     * @param aValue (may be null) a value for the give key to look for
     * @return all nodes containing the given key (with the given value is specified)
     */
    public Iterator<Node> getNodesByTag(final String aKey, final String aValue) {

        LinkedList<Node> retval = new LinkedList<Node>();
        for (Node node : this.nodesByID.values()) {
            String value = NodeHelper.getTag(node, aKey);  // e.g. "XYZSteet"
            if (value != null) {
                if (aValue != null && !value.equals(aValue))
                    continue;
                retval.add(node);
            }
        }

        return retval.iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        this.nodesByID.clear();
        this.relationByID.clear();
        this.waysByID.clear();
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
