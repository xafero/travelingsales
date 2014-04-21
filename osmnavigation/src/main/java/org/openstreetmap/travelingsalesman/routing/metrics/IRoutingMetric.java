/**
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
 *
 *  Created: 08.11.2007
 */
package org.openstreetmap.travelingsalesman.routing.metrics;

import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;

/**
 * A routing-metric is a function that can provide
 * the "cost" of using a given way or crossing a given node.<br/>
 * The most common metrics would be "length" resulting in the
 * shortest route and "time" resulting in the fastest.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public interface IRoutingMetric extends IPlugin {

    /**
     * @param aMap the map we operate on
     */
    void setMap(final IDataSet aMap);

    /**
     * Calculate the cost of taveling the given way from
     * start to end.<br/>
     * Start and End are guaranteed to be on the way.<br/>
     * Start and End are never equal.
     * @param step the way-segment to test
     * @return a cost. Guaranteed to be >=0.
     */
    double getCost(final RoutingStep step);

    /**
     * Calculate the cost of crossing the give intersection.<br/>
     * This method is ONLY called when switching from one way
     * to another.
     * @param crossing the crossing we tage
     * @param from the way+node we come from
     * @param to the way+node we go to
     * @return a cost. Guaranteed to be >=0.
     */
    double getCost(final Node crossing, final RoutingStep from, final RoutingStep to);
}
