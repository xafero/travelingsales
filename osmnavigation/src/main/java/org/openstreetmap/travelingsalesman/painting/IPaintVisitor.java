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
 *  LibOSM is distributed in the hope that it will be useful,
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

package org.openstreetmap.travelingsalesman.painting;

import java.awt.Graphics2D;
import java.util.List;

import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.visitors.Visitor;
import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.gps.data.GpsTracksStorage;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.trafficblocks.TrafficMessage;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * A PaintVisitor is capable of drawing the map.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public interface IPaintVisitor extends Visitor, IPlugin {

    /**
     * Visit all elements of the
     * dataset in the right order.
     * @param data the dataset to visit
     * @param aGraphics where to draw to
     */
    void visitAll(final IDataSet data, final Graphics2D aGraphics);

    /**
     * Draw a small rectangle.
     * White if selected (as always) or red otherwise.
     *
     * @param n The node to draw.
     */
    void visit(final Node n);

    /**
     * Highlight the route to drive int he map.
     * @param routeSegments where to start and end
     * @param data the dataset to visit
     */
    void visitRoute(final IDataSet data, final List<RoutingStep> routeSegments);

    /**
     * @param aNextManeuverPosition the position to highlight where the next maneuver is required
     */
    void visitNextManeuverPosition(
            final LatLon aNextManeuverPosition);


    /**
     * @param storage the storage contains gps tracks.
     */
    void visitGpsTracks(final GpsTracksStorage storage);

    /**
     * @param aCurrentPosition The current user-position to mark.
     */
    void visitCurrentPosition(final LatLon aCurrentPosition, final double aCurrentCourse, final double aCurrentSpeed);

    /**
     * Highlight selected node.
     */
    void visitSelectedNode(final LatLon mySelectedNodePosition);

    /**
     * Draw a darkblue line for all segments.
     * @param w The way to draw.
     */
    void visit(final Way w);

    /**
     * @param aG the graphics to draw to
     */
    void setGraphics(final Graphics2D aG);

    /**
     * @param aPanel the component to get map and displayed coordinate-range + projection from.
     */
    void setNavigatableComponent(INavigatableComponent aPanel);

    /**
     * Paint the given traffic-message (traffic jam, road-obstruction, accident, ...).
     * @param aTrafficMessage the message to paint
     */
    void visitTrafficMessage(final TrafficMessage aTrafficMessage);

}
