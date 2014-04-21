/**
 * NearestStreetSelector.java
 * created: 14.03.2009 18:33:29
 * (c) 2009 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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
package org.openstreetmap.osm.data.searching;


//automatically created logger for debug and error -output
import java.util.Iterator;
import java.util.logging.Logger;

import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.Selector;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;


/**
 * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * NearestStreetSelector.java<br/>
 * created: 14.03.2009 18:33:29 <br/>
 *<br/><br/>
 * <b>Selector that matches ways and nodes that are part of a NAMED way that is a street except for one given start-node
 * that will never match.</b>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class NearestStreetSelector implements Selector {

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(NearestStreetSelector.class.getName());


    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "NearestStreetSelector";
    }

    /**
     * ${@inheritDoc}.
     */
    @Override 
    public boolean isAllowed(final IDataSet aMap, final Node aNode) {
    	// LOG.info("isAllowed: "+aMap+", "+aNode);
    	
        Iterator<Way> ways = aMap.getWaysForNode(aNode.getId());
        if (ways != null) {
            while (ways.hasNext()) {
                if (isAllowed(aMap, ways.next())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public boolean isAllowed(final IDataSet aMap, final Way aWay) {
        return (WayHelper.getTag(aWay, Tags.TAG_HIGHWAY) != null)
            && (WayHelper.getTag(aWay, Tags.TAG_NAME) != null);
    }

    /**
     * ${@inheritDoc}.
     */
    @Override 
    public boolean isAllowed(final IDataSet aMap, final Relation aRelation) {
        return false;
    }


}


