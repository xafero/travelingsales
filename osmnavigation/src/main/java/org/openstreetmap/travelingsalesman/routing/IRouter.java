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

import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.travelingsalesman.routing.metrics.IRoutingMetric;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;


/**
 * This interface is implemented by routing-algorithms.
 * Its implementations can route from a point to a Way (for Navigation)
 * or from a Node to a Way (for trip-planing).
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public interface IRouter extends IPlugin {

    /**
     * @param targetWay the {@link Way} we want to reach
     * @param startNode the {@link Node} we are now
     * @param selector (may be null) optional selector to determine unallowed roads.
     * @param aMap the map to route on
     * @return null or a list of Segments to use in order to reach the destination
     */
    Route route(final IDataSet aMap, final Way targetWay, final Node startNode,
                final IVehicle selector);

    /**
     * @param targetNode the {@link Node} we want to reach
     * @param startNode the {@link Node} we are now
     * @param selector (may be null) optional selector to determine unallowed roads.
     * @param aMap the map to route on
     * @return null or a list of Segments to use in order to reach the destination
     */
    Route route(final IDataSet aMap, final Node targetNode, final Node startNode,
                final IVehicle selector);

    /**
     * Add a listener to be informed about the progress made.
     * @param aListener the listener
     */
    void addProgressListener(final IProgressListener aListener);

    /**
     * @param aMetric the metric we are to optimize for
     */
    void setMetric(final IRoutingMetric aMetric);
}
