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

import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;


/**
 * A time-based routing-metric is a function that can provide
 * the "cost" equivalent to an estimated travel-time of using
 * a given way or crossing a given node.<br/>
 * The most common metrics would be "length" resulting in the
 * shortest route and "time" resulting in the fastest.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public interface ITimeRoutingMetric extends IRoutingMetric {

    /**
     * @param aRoutingStep the step to calculate the speed for
     * @return the estimated speed in kilometers per hour
     */
    double getEstimatedSpeed(RoutingStep aRoutingStep);
}
