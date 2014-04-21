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

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;


/**
 * Instances of this interface can determine if
 * a given Node, Segment or Way is allowed to route through.<br/>
 * What constitutes a oneway-segment for the given vehicle
 * (think pedestrians or bicycles) and what kind of vehicle it is.<br/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public interface IVehicle extends org.openstreetmap.osm.data.Selector, IPlugin {

    /**
     * @param node the Node to test.
     * @param aMap the map we operate on.
     * @return true if we are allowed to use that Node.
     * @see org.openstreetmap.osm.data.Selector#isAllowed(Node)
     */
    boolean isAllowed(final IDataSet aMap, final Node node);

    /**
     * @param way the way to test
     * @param aMap the map we operate on.
     * @return true if this is a oneway-street in the opposing direction
     */
    boolean isReverseOneway(final IDataSet aMap, final Way way);

    /**
     * @param way the way to test
     * @param aMap the map we operate on.
     * @return true if this is a oneway-street-
     */
    boolean isOneway(final IDataSet aMap, final Way way);

    /**
     * @param way the Way to test.
     * @param aMap the map we operate on.
     * @return true if we are allowed to use that Way.
     * @see org.openstreetmap.osm.data.Selector#isAllowed(Way)
     */
    boolean isAllowed(final IDataSet aMap, final Way way);

}
