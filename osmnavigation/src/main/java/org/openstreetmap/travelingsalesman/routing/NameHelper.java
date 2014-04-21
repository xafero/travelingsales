/**
 * This file is part of OSMNavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  OSMNavigation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  OSMNavigation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OSMNavigation.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.routing;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedNode;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.travelingsalesman.navigation.OsmNavigationConfigSection;


/**
 * Helper-Class to get names for intersections, streets,...
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public final class NameHelper {

    /**
     * Utility-Classes need no public constructor.
     *
     */
    private NameHelper() {
    }

    /**
     * @param node the node to get a name for
     * @param aMap the map we operate on
     * @return the name or null
     */
    public static String getNameForNode(final IDataSet aMap, final Entity node) {

        // 1. does the node have a name?
        String name = getNameForWay(node);
        if (name != null) {
            return name;
        }

        Set<String> nerbyNames = new HashSet<String>();

        name = getIntersectionName(nerbyNames);
        if (name != null) {
            return name;
        }

        // 2. do the ways here have a name?
        Iterator<Way> ways = null;
        if (node instanceof ExtendedNode) {
            ExtendedNode xnode = (ExtendedNode) node;
            Set<Long> referencedWayIDs = xnode.getReferencedWayIDs();
            Set<Way> ways2 = new HashSet<Way>();
            for (Long wayId : referencedWayIDs) {
                ways2.add(aMap.getWaysByID(wayId));
            }
            ways = ways2.iterator();
        }
        if (ways == null) {
            ways = aMap.getWaysForNode(node.getId());
        }
        while (ways != null && ways.hasNext()) {
            name = getNameForWay(ways.next());
            if (name != null) {
                nerbyNames.add(name);
            }
        }
        name = getIntersectionName(nerbyNames);
        if (name != null) {
            return name;
        }

        return null;
    }

    /**
     * Return the name, ref, nat_ref or int_ref -attribute
     * of a node.
     * @param aWay the Way
     * @return the name or nill
     */
    public static String getNameForWay(final Entity aWay) {

        // try the default-name
        String name = WayHelper.getTag(aWay, Tags.TAG_NAME);
        if (name != null) {
            return name;
        }

        // try national names
        Collection<Tag> tagList = aWay.getTags();
        for (Tag tag : tagList) {
            String key = tag.getKey();
            if (key.startsWith(Tags.TAG_NAME + ":")) {
                return tag.getValue();
            }
        }

        // fall back to reference-numbers of motorways,...
        name = WayHelper.getTag(aWay, Tags.TAG_REF);
        if (name != null) {
            return name;
        }

        name = WayHelper.getTag(aWay, Tags.TAG_NAT_REF);
        if (name != null) {
            return name;
        }

        name = WayHelper.getTag(aWay, Tags.TAG_INT_REF);
        if (name != null) {
            return name;
        }

        name = WayHelper.getTag(aWay, Tags.TAG_LOC_REF);
        if (name != null) {
            return name;
        }
        return null;
    }

    /**
     * @param nerbyNames the names of the streets (ways or segments) crossing here
     * @return the name for the location
     */
    protected static String getIntersectionName(final Set<String> nerbyNames) {
        if (nerbyNames.size() == 0) {
            return null;
        }
        if (nerbyNames.size() == 1) {
            return "Node on '" + nerbyNames.iterator().next() + "'";
        }

        StringBuilder sb = new StringBuilder(OsmNavigationConfigSection.RESOURCE.getString("namebuilding.cornerprefix") + " ");
        for (String name : nerbyNames) {
            sb.append(" '").append(name).append("'");
        }
        return sb.toString();
    }

}
