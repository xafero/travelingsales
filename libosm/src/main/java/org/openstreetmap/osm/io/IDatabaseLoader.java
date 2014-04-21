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
package org.openstreetmap.osm.io;

import org.openstreetmap.osm.data.MemoryDataSet;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;

/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * IDatabaseLoader.java<br/>
 * created: 2007 <br/>
 *<br/><br/>
 * This plugin is implemented by all classes that allow to load (and store) parts
 * of a map from/into a database. The implementations are database-specific while
 * this interface is not.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public interface IDatabaseLoader {

    /**
     * @return the file-content or null
     * @param minLat only the elements within/intersecting this bounding-box will be returned
     * @param maxLat only the elements within/intersecting this bounding-box will be returned
     * @param minLon only the elements within/intersecting this bounding-box will be returned
     * @param maxLon only the elements within/intersecting this bounding-box will be returned
     */
    MemoryDataSet loadBoundingBox(final double minLat,
            final double minLon, final double maxLat, final double maxLon);

    /**
     * @return the file-content or null
     */
    MemoryDataSet loadPlanet();

    /**
     * @return all nodes from the database.
     */
    ReleasableIterator<Node> loadAllNodes();

    /**
     * @return all nodes from the database that are within the bouding-box.
     * @param minLat only the elements within/intersecting this bounding-box will be returned
     * @param maxLat only the elements within/intersecting this bounding-box will be returned
     * @param minLon only the elements within/intersecting this bounding-box will be returned
     * @param maxLon only the elements within/intersecting this bounding-box will be returned
     */
    ReleasableIterator<Node> loadAllNodesInBoundingBox(
            final double minLat, final double minLon, final double maxLat,
            final double maxLon);

    /**
     * @param nodeID read only the one node with this ID.
     * @return the node
     */
    Node loadNode(final long nodeID);

    /**
     * @param relationID read only the one relation with this ID.
     * @return the node
     */
    Relation loadRelation(final long relationID);

    /**
     * Load all ways from the database.
     * @return the ways.
     */
    ReleasableIterator<Way> loadAllWays();

    /**
     * Load all ways from the database.
     * @return all ways from the database that are within the bouding-box.
     * @param minLat only the elements within/intersecting this bounding-box will be returned
     * @param maxLat only the elements within/intersecting this bounding-box will be returned
     * @param minLon only the elements within/intersecting this bounding-box will be returned
     * @param maxLon only the elements within/intersecting this bounding-box will be returned
     */
    ReleasableIterator<Way> loadAllWaysInBoundingBox(
            final double minLat, final double minLon, final double maxLat,
            final double maxLon);

    /**
     * Get the specified way.
     * @param aWayID the id of the way to load
     * @return the way
     */
    Way loadWay(final long aWayID);

    /**
     * Get all ways that contain the given node.
     * @param aNodeID the id of the node to load the ways for
     * @return the ways
     */
    ReleasableIterator<Way> loadWaysForNode(final long aNodeID);

    /**
     * @param aLookFor a name or ref to look for
     * @return all nodes with that name or ref
     */
    ReleasableIterator<Node> loadNodesByName(
            final String aLookFor);

    /**
     * @param aLookFor a name or ref to look for
     * @param minLat only the elements within/intersecting this bounding-box will be returned
     * @param maxLat only the elements within/intersecting this bounding-box will be returned
     * @param minLon only the elements within/intersecting this bounding-box will be returned
     * @param maxLon only the elements within/intersecting this bounding-box will be returned
     * @return all ways with that name or ref
     */
    ReleasableIterator<Way> loadWaysByName(final String aLookFor,
            final double minLat, final double minLon, final double maxLat,
            final double maxLon);

    /**
     * @param aLookFor a name or ref to look for
     * @return all ways with that name or ref
     */
    ReleasableIterator<Way> loadWaysByName(final String aLookFor);

    /**
     * @param aW add this node to the database
     */
    void createNode(final Node aW);

    /**
     * @param aNode add this way to the database
     */
    void createWay(final Way aNode);

    /**
     * @param aRelation add this relation to the database
     */
    void createRelation(final Relation aRelation);

    /**
     * @param aKey a key to look for
     * @param aValue (may be null) a value for the give key to look for
     * @return all ways containing the given key (with the given value is specified)
     */
    ReleasableIterator<Way> loadWaysByTag(final String aKey, final String aValue);

    /**
     * @param aKey a key to look for
     * @param aValue (may be null) a value for the give key to look for
     * @return all nodes containing the given key (with the given value is specified)
     */
    ReleasableIterator<Node> loadNodesByTag(final String aKey, final String aValue);

    /**
     * Commit and checkpoint the database.
     */
	void commit();

	/**
	 * Properly relase all ressources.
	 */
	void shutdown();

}
