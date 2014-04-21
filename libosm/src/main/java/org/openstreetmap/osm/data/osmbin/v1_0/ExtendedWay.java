/**
 * ExtendedWay.java
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
import java.util.List;
import java.util.Set;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * ExtendedWay.java<br/>
 * created: 02.01.2009<br/>
 * <b>This is an extended node-object that stores the ways and relations
 * it is a member of.</b>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class ExtendedWay extends Way {

    /**
     * @param aId the id of the way.
     * @param aVersion the version of the way (given by the
     * OpenStreetMap-server.
     * @param tags the tags for this way
     * @param wayNodes the nodes for this way
     */
    public ExtendedWay(final long aId, final int aVersion, final Collection<Tag> tags, final List<WayNode> wayNodes) {
        super(aId, aVersion, new Date(), null, 0);
        super.getTags().addAll(tags);
        super.getWayNodes().addAll(wayNodes);
    }

    /**
     * @param aId the id of the way.
     * @param aVersion the version of the way (given by the
     * OpenStreetMap-server.
     */
    public ExtendedWay(final long aId, final int aVersion) {
        super(aId, aVersion, new Date(), null, 0);
    }

    /**
     * The IDs of all relations we are a part of.
     */
    private Set<Long> myReferencedRelationIDs;
    private double myMinLatitude = Double.MAX_VALUE;
    private double myMinLongitude = Double.MAX_VALUE;
    private double myMaxLatitude = Double.MIN_VALUE;
    private double myMaxLongitude = Double.MIN_VALUE;

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
        if (myReferencedRelationIDs == null) {
            myReferencedRelationIDs = new HashSet<Long>();
        }
        return myReferencedRelationIDs;
    }

    /**
     * @param aReferencedRelationIDs the referencedRelationIDs to set
     */
    public void setReferencedRelationIDs(final Set<Long> aReferencedRelationIDs) {
        myReferencedRelationIDs = aReferencedRelationIDs;
    }

    /**
     * @return the minLatitude
     */
    public double getMinLatitude() {
        return this.myMinLatitude;
    }

    /**
     * @return the minLongitude
     */
    public double getMinLongitude() {
        return myMinLongitude;
    }

    /**
     * @param aMinLongitude the minLongitude to set
     */
    public void setMinLongitude(final double aMinLongitude) {
        myMinLongitude = aMinLongitude;
    }

    /**
     * @return the maxLatitude
     */
    public double getMaxLatitude() {
        return myMaxLatitude;
    }

    /**
     * @param aMaxLatitude the maxLatitude to set
     */
    public void setMaxLatitude(final double aMaxLatitude) {
        myMaxLatitude = aMaxLatitude;
    }

    /**
     * @return the maxLongitude
     */
    public double getMaxLongitude() {
        return myMaxLongitude;
    }

    /**
     * @param aMaxLongitude the maxLongitude to set
     */
    public void setMaxLongitude(final double aMaxLongitude) {
        myMaxLongitude = aMaxLongitude;
    }

    /**
     * @param aMinLatitude the minLatitude to set
     */
    public void setMinLatitude(final double aMinLatitude) {
        myMinLatitude = aMinLatitude;
    }
}
