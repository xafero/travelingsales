/**
 * ExtendedNode.java
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

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * ExtendedNode.java<br/>
 * created: 02.01.2009<br/>
 * <b>This is an extended node-object that stores the ways and relations
 * it is a member of.</b>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class ExtendedNode extends Node {

    /**
     * The IDs of all ways we are a part of.
     */
    private Set<Long> myReferencedWayIDs;

    /**
     * The IDs of all relations we are a part of.
     */
    private Set<Long> myReferencedRelationIDs;

    /**
     * @param aId the ID of the node
     * @param aVersion the version of the way (given by the
     * OpenStreetMap-server.
     * @param aChangesetID id of the changeset this is a part of
     * @param aLatitude the location of the node
     * @param aLongitude the location of the node
     * @param aTags the tags for the new node
     */
    public ExtendedNode(final long aId,
                   final int aVersion,
                   final long aChangesetID,
                   final double aLatitude,
                   final double aLongitude,
                   final Collection<Tag> aTags) {
        super(aId, aVersion, new Date(), null, aChangesetID, aLatitude, aLongitude);
        super.getTags().addAll(aTags);
    }

    /**
     * @param aId the ID of the node
     * @param aVersion the version of the way (given by the
     * OpenStreetMap-server.
     * @param aChangesetID id of the changeset this is a part of
     * @param aLatitude the location of the node
     * @param aLongitude the location of the node
     */
    public ExtendedNode(final long aId,
                   final int aVersion,
                   final long aChangesetID,
                   final double aLatitude,
                   final double aLongitude) {
        super(aId, aVersion, new Date(), null, aChangesetID, aLatitude, aLongitude);
    }

    /**
     * Add a way to the list of all ways we are
     * a part of.
     * @param aWayID the id of the way
     */
    public void addReferencedWay(final long aWayID) {
        if (this.myReferencedWayIDs == null) {
            this.myReferencedWayIDs = new HashSet<Long>();
        }
        this.myReferencedWayIDs.add(aWayID);
    }

    /**
     * @return the referencedWayIDs
     */
    public Set<Long> getReferencedWayIDs() {
        if (this.myReferencedWayIDs == null) {
            this.myReferencedWayIDs = new HashSet<Long>();
        }
        return this.myReferencedWayIDs;
    }

    /**
     * @param aReferencedWayIDs the referencedWayIDs to set
     */
    public void setReferencedWayIDs(final Set<Long> aReferencedWayIDs) {
        this.myReferencedWayIDs = aReferencedWayIDs;
    }

    /**
     * Add a relation to the list of all relations we are
     * a part of.
     * @param aRelationID the id of the Relation
     */
    public void addReferencedRelation(final long aRelationID) {
        if (this.myReferencedRelationIDs == null) {
            this.myReferencedRelationIDs = new HashSet<Long>();
        }
        this.myReferencedRelationIDs.add(aRelationID);
    }

    /**
     * @return the referencedRelationIDs
     */
    public Set<Long> getReferencedRelationIDs() {
        if (this.myReferencedRelationIDs == null) {
            this.myReferencedRelationIDs = new HashSet<Long>();
        }
        return this.myReferencedRelationIDs;
    }

    /**
     * @param aReferencedRelationIDs the referencedRelationIDs to set
     */
    public void setReferencedRelationIDs(final Set<Long> aReferencedRelationIDs) {
        this.myReferencedRelationIDs = aReferencedRelationIDs;
    }
}
