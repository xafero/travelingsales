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

import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

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
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public interface IDataSet extends IPlugin {

    /**
     * Add a new way to this dataSet.
     * @param w the way to add
     */
    void addWay(final Way w);

    /**
     * @param w the way to look for
     * @return true if we contain this way
     */
    boolean containsWay(final Way w);

    /**
     * This method is allowed to return more ways then
     * just the ones asked for.
     * @return all ways we know (be carefull, can be MANY)
     * @param boundingBox visit only what is or intersects this boundingBox.
     */
    Iterator<Way> getWays(final Bounds boundingBox);

    /**
     * @param w may be null (ignored then)
     */
    void removeWay(final Way w);

    /**
     * @return the wayHelper
     */
    WayHelper getWayHelper();

    /**
     * Add a new relation to this dataSet.
     * @param r the relation to add
     */
    void addRelation(final Relation r);

    /**
     * @param r the relation to look for
     * @return true if we contain this relation
     */
    boolean containsRelation(final Relation r);

    /**
     * This methos is allowed to return more ways then
     * just the ones asked for.
     * @return all relations we know (be carefull, can be MANY)
     * @param boundingBox visit only what is or intersects this boundingBox.
     */
    Iterator<Relation> getRelations(final Bounds boundingBox);

    /**
     * @param aRelationID the id of the way.
     * @return the relation or null
     */
    Relation getRelationByID(final long aRelationID);

    /**
     * Does only remove the relation and not relations
     * that contain this one, nor ways or segments
     * in the relaiton.
     * @param r may be null (ignored then)
     */
    void removeRelation(final Relation r);

    /**
     * Add a new node to this dataSet.
     * @param w the way to add
     */
    void addNode(final Node w);

    /**
     * @param w the node to look for
     * @return true if we contain this node
     */
    boolean containsNode(final Node w);

    /**
     * @return all nodes we know (be carefull, can be MANY)
     * @param boundingBox visit only what is or intersects this boundingBox.
     */
    Iterator<Node> getNodes(final Bounds boundingBox);

    /**
     * @param aWayID the id of the way.
     * @return the way or null
     */
    Way getWaysByID(final long aWayID);

    /**
     * @param lookFor the way-name to look for (ignores cases but does not check for alternative spellings)
     * @param boundingBox give only what is or intersects this boundingBox. (may be null)
     * @return all known ways with that name
     */
    Iterator<Way> getWaysByName(final String lookFor, final Bounds boundingBox);

    /**
     * @param aKey a key to look for
     * @param aValue (may be null) a value for the give key to look for
     * @return all ways containing the given key (with the given value is specified)
     */
    Iterator<Way> getWaysByTag(final String aKey, final String aValue);

    /**
     * @param aKey a key to look for
     * @param aValue (may be null) a value for the give key to look for
     * @return all nodes containing the given key (with the given value is specified)
     */
    Iterator<Node> getNodesByTag(final String aKey, final String aValue);

    /**
     * @param aNodeID the id of the node.
     * @return the node or null
     */
    Node getNodeByID(final long aNodeID);

    /**
     * @param w node be null (ignored then)
     */
    void removeNode(final Node w);

    /**
     * Return all ways in this dataset that contain
     * this node.
     * @param nodeID the node to look for
     * @return an iterator over the list
     */
    Iterator<Way> getWaysForNode(final long nodeID);

    /**
     * @param lookFor the node-name to look for (ignores cases but does not check for alternative spellings)
     * @return all known nodes with that name
     */
    Iterator<Node> getNodesByName(final String lookFor);

    /**
     * @param aLastGPSPos the location to compare with
     * @param aSelector (may be null) a selector to tell what nodes are to be applicable.
     * @return the node with the minimum distance to the given LatLon.
     */
    Node getNearestNode(final LatLon aLastGPSPos, final Selector aSelector);

    /**
     * Properly release all ressources.
     */
    void shutdown();

}
