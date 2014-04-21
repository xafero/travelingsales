/**
 * TMCLocationIndexer.java
 * created: 05.04.2009
 * (c) 2008 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of osmnavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  osmnavigation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  osmnavigation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with osmnavigation.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.trafficblocks;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.data.visitors.Visitor;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * Look at the tags of the given elements and add them
 * to {@link TrafficMessageStore} if they contain a TMC-location-Code.
 */
public class TMCLocationIndexer implements Visitor {
    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(TMCLocationIndexer.class.getName());

    /**
     * Format of the tag for a LocationCode.
     * {1} = country-ID
     * {2} = table-ID
     * {3} = Point, Segment, Road or Area
     */
    private static final MessageFormat TMC_LOCID_TAG = new MessageFormat("TMC:cid_{0}:tabcd_{1}:{2}:LocationCode");

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(final Node aN) {
        visit(aN, EntityType.Node);
    }

    /**
     * Look at the given entity and if it contains a TMC LocationCode,
     * index it in {@link TrafficMessageStore}.
     * @param aN the entity to index
     * @param aType the entity-type
     */
    private void visit(final Entity aN, final EntityType aType) {
        Collection<Tag> tags = aN.getTags();
        for (Tag tag : tags) {
            if (tag == null || tag.getKey() == null || tag.getValue() == null) {
                continue;
            }
            if (!tag.getKey().toLowerCase().startsWith("tmc")) {
                continue;
            }
            if (tag.getKey().toLowerCase().indexOf("locationcode") == -1) {
                continue;
            }
            try {
                Object[] parsed = TMC_LOCID_TAG.parse(tag.getKey());
                //TODO: test this
                int countryID = Integer.parseInt(parsed[0].toString());
                int tableID   = Integer.parseInt(parsed[1].toString());
                String tmcElementType = parsed[2].toString();
                int locationCode = Integer.parseInt(tag.getValue());
                TrafficMessageStore.getInstance().indexTMCLocation(countryID, tableID, locationCode, tmcElementType, aType, aN.getId());
            } catch (ParseException e) {
                LOG.log(Level.FINEST, "(You can ignore this) Unable to index the TMC-location of map-element \"" + aN +  "\"", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(final Way aW) {
        visit(aW, EntityType.Way);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void visit(final Relation aR) {
        visit(aR, EntityType.Relation);
    }

}
