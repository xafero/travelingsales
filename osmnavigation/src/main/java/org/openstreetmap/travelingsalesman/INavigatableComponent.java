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
package org.openstreetmap.travelingsalesman;

import java.awt.Point;
import java.beans.PropertyChangeListener;

import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.EastNorth;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.projection.Projection;

/**
 * An component that can be navigated by a mapmover. Used as map view and for the
 * zoomer in the download dialog.
 * Taken from he JOSM-Codebase.
 * @author imi
 *
 */
public interface INavigatableComponent {


    /**
     * The maximum zoom-level allowed.
     */
    int MAXZOOM = 32;

    /**
     * @return the map we render.
     */
    IDataSet getDataSet();

    /**
     * Return the point on the screen where this Coordinate would be.
     * @param aPoint The point, where this geopoint would be drawn.
     * @return The point on screen where "point" would be drawn, relative
     *              to the own top/left.
     */
    Point getPoint(final EastNorth aPoint);

    /**
     * @param x X-Pixelposition to get coordinate from
     * @param y Y-Pixelposition to get coordinate from
     *
     * @return Geographic unprojected coordinates from a specific pixel coordination
     *      on the screen.
     */
    LatLon getLatLon(final int x, final int y);

    /**
     * Return the current scale value.
     * The scale factor in x or y-units per pixel. This means, if scale = 10,
     * every physical pixel on screen are 10 x or 10 y units in the
     * northing/easting space of the projection.
     * @return The scale value currently used in display
     */
    double getScale();

    /**
     * @return Returns the center point. A copy is returned, so users cannot
     *              change the center by accessing the return value. Use zoomTo instead.
     */
    EastNorth getCenter();

    /**
     * @return Returns the left upper point. A copy is returned, so users cannot
     *              change the center by accessing the return value. Use zoomTo instead.
     */
    LatLon getLeftUpper();

    /**
     * Get the height of the map-area in pixels.
     * @return the height
     */
    int getHeight();

    /**
     * Get the width of the map-area in pixels.
     * @return width
     */
    int getWidth();

    /**
     * Add a new listener for property-changes.
     * Valid properties are at least "center" and "scale".
     * @param aProperty the property to register for
     * @param aListener the listener
     */
    void addPropertyChangeListener(final String aProperty, final PropertyChangeListener aListener);

    /**
     * @return the OSM-conforming zoom factor (0 for whole world, 1 for half, 2 for quarter...).
     */
    int getZoom();

    /**
     * Get the projection-method to use.
     * @return the projection
     */
    Projection getProjection();

    /**
     * Zoom to the given location at the given zoom-level.
     * @param aCenter the new center-point to display
     * @param aZoomLevel The scale factor in x or y-units per pixel. This means, if scale = 10,
     *        every physical pixel on screen are 10 x or 10 y units in the
     *        northing/easting space of the projection.
     */
    void zoomTo(final EastNorth aCenter, final double aZoomLevel);

    /**
     * @param x X-pixel-position to get coordinate from
     * @param y Y-pixel-position to get coordinate from
     *
     * @return Geographic coordinates from a specific pixel coordination
     *              on the screen.
     */
    EastNorth getEastNorth(int x, int y);

    /**
     * The current user-position to mark.
     * @return the currentPosition
     */
    LatLon getCurrentPosition();

    /**
     * The current user-position to mark.
     * @param aCurrentPosition the currentPosition to set
     */
    void setCurrentPosition(final LatLon aCurrentPosition);

    /**
     * Set node for highlight.
     * @param aSelectedNodePosition the position to highlight
     */
    void setSelectedNodePosition(final LatLon aSelectedNodePosition);

    /**
     * @return the nextManeuverPosition
     */
    LatLon getNextManeuverPosition();

    /**
     * @param aNextManeuverPosition the nextManeuverPosition to set
     */
    void setNextManeuverPosition(final LatLon aNextManeuverPosition);

    /**
     * @return the lat/lon of the visible area.
     */
    Bounds getMapBounds();

}
