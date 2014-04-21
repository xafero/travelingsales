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
package org.openstreetmap.osm.data.searching;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.IDataSet;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * A trivial PlaceFinder.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class SimplePlaceFinder implements IPlaceFinder {


    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(SimplePlaceFinder.class.getName());

    /**
     * The map we are searching on.
     */
    private IDataSet myMap;

    /**
     * This plugin has no  settings, thus this method returns null
     * as described in {@link IPlugin#getSettings()}.
     * @return null
     */
    public ConfigurationSection getSettings() {
        return null;
    }

    /**
     * @param aSearchExpression a name. no wildcards or anything. fust a full name.
     * @return all places with that name
     * @see org.openstreetmap.osm.data.searching.IPlaceFinder#findPlaces(java.lang.String)
     */
    public Collection<Place> findPlaces(final String aSearchExpression) {
        long start = System.currentTimeMillis();
//      this is a trivial search and not very usefull
        Set<Place> retval = new HashSet<Place>();

        if (myMap == null)
            throw new IllegalArgumentException("no map given!");

        Iterator<Way> ways = myMap.getWaysByName(aSearchExpression, null);
        while (ways != null && ways.hasNext()) {
            Way w = ways.next();
            try {
                retval.add(new WayPlace(w, myMap));
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Exception while adding a search-result for the way with id=" + w.getId(), e);
            }
        }
        ways = null;

        Iterator<Node> nodes = myMap.getNodesByName(aSearchExpression);
        if (nodes != null) {
            while (nodes.hasNext()) {
                Node n = nodes.next();
                retval.add(new NodePlace(n, myMap));
            }
        }
        nodes = null;

        if (LOG.isLoggable(Level.FINE))
            LOG.log(Level.FINE, "SimplePlaceFinder took " + (System.currentTimeMillis() - start) + " ms to find "
                  + retval.size() + " results");

        // try to remove sub-expressions
        if (retval.size() == 0 && aSearchExpression.trim().contains(" ")) {
            String temp = aSearchExpression.trim();
            return findPlaces(temp.substring(temp.indexOf(' ')));
        }
        return retval;
    }

    /**
     * @param aMap The map we are searching on.
     * @see org.openstreetmap.osm.data.searching.IPlaceFinder#setMap(org.openstreetmap.osm.data.IDataSet)
     */
    public void setMap(final IDataSet aMap) {
        if (aMap == null)
            throw new IllegalArgumentException("null map given!");
        this.myMap = aMap;
    }

}
