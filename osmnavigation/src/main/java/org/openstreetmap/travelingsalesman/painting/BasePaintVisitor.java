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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.UnitConstants;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.LODDataSet;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.visitors.Visitor;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.gps.data.GpsTrack;
import org.openstreetmap.travelingsalesman.gps.data.GpsTrackpoint;
import org.openstreetmap.travelingsalesman.gps.data.GpsTracksStorage;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.trafficblocks.TrafficMessage;

/**
 * The BasePaintVisitor simplifies the development of
 * custom PaintVisitors.
 * It provides for styles, LOD, ways and already
 * draws routes and current location.
 * Heavily Adapted from the Josm-Sourcecode.
 * @author imi
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public abstract class BasePaintVisitor implements Visitor, IPaintVisitor {

    /**
     * Show a direction-arrow when we are faster then
     * this many kilometers per hour.
     */
    private static final double MINSPEEDTOSHOWDIRECTION = 4d;

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(BasePaintVisitor.class.getName());

    /**
     * Width of the stroke when drawing the navigation. route.
     */
    private static final float ROUTESEGMENTSTROKEWIDTH = 12.0f;

    /**
     * Every zoom-level less then this will be considered
     * "far away" and only thin lines are drawn.
     */
    static final int FARAWAYZOOM = 12;

    /**
     * The environment to paint to.
     */
    private Graphics2D g;

    /**
     * MapView to get screen coordinates.
     */
    private INavigatableComponent nc;
    /**
     * The map we work on.
     */
    private IDataSet myMap;
    /**
     * helper-constant for doing math.
     */
    private static final double PHI = Math.toRadians(20);

    /**
     * the style to use for drawing the route to drive.
     */
    private Style routeStyle = new Style(new Color(Color.BLACK.getRed(),
                                                    Color.BLACK.getGreen(),
                                                    Color.BLACK.getBlue(),
                                                    Byte.MAX_VALUE), ROUTESEGMENTSTROKEWIDTH);
    /**
     * the style to use for drawing the circle indicating the position
     * of the next driving-instruction on the current route.
     */
    private Style currentRouteStepStyle = new Style(Color.RED, ROUTESEGMENTSTROKEWIDTH / 2);

    /**
     * the style to use for drawing the circle indicating the current gps-position.
     */
    private Style currentPositionStyle = new Style(Color.GREEN, ROUTESEGMENTSTROKEWIDTH / 2);

    /**
     * the style to use for drawing the gps track.
     */
    private Style currentTrackStyle = new Style(Color.BLUE, ROUTESEGMENTSTROKEWIDTH / (2 + 2 + 2 + 2));

    /**
     * The color to use for drawing nodes.
     */
    private Color nodeColor;

    /**
     * The background-color to use.
     */
    private Color backgroundColor = Color.LIGHT_GRAY;

    /**
     * The style to use to draw the currently drawn {@link Way}-segment.
     */
    private Style segmentStyle;

    /**
     * Draw subsequent segments of same color as one Path.
     */
    private Style currentColor = null;

    /**
             * This class encapsulates a dawing-style for a road.
             * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
             */
            protected static class Style {

                /**
                 * The color to draw in.
                 */
                private Color myColor;

                /**
                 * The line-thickness to draw width.
                 */
                private float myLineThickness = 1.0f;

                /**
                 * Cache of the stroke we are using.
                 */
                private volatile BasicStroke myStroke = null;

                /**
                 * Cache of the stroke we are using
                 * if the road is far away.
                 */
                private static final BasicStroke LODSTROKE = new BasicStroke(1);

                /**
                 * @param aColor The color to draw in.
                 */
                public Style(final Color aColor) {
                    myColor = aColor;
                }

                /**
                 * @param aColor The color to draw in.
                 * @param aLineThickness The line-thickness to draw width.
                 */
                public Style(final Color aColor, final float aLineThickness) {
                    myColor = aColor;
                    myLineThickness = aLineThickness;
                }

                /**
                 * Draw the given path in this style.
                 * @param aPath the path
                 * @param g where to draw to
                 * @param isFarAway if true everything will be drawn 1px wide
                 */
                public void drawPath(final GeneralPath aPath, final Graphics2D g, final boolean isFarAway) {
                    Stroke  oldStroke = g.getStroke();

                    if (isFarAway && (myLineThickness != ROUTESEGMENTSTROKEWIDTH)) {
                        try {
                            g.setStroke(LODSTROKE);
                            g.setColor(getColor());
                            g.draw(aPath);
                            return;
                        } finally {
                            g.setStroke(oldStroke);
                        }
                    }

                    try {
                        g.setStroke(getStroke());
                        g.setColor(getColor());
                        g.draw(aPath);
                    } finally {
                        g.setStroke(oldStroke);
                    }
                }

                /**
                 * @return the color
                 */
                public Color getColor() {
                    return myColor;
                }

                /**
                 * @return the stroke
                 */
                protected BasicStroke getStroke() {
                    if (myStroke == null)
                        myStroke = new BasicStroke(myLineThickness);
                    return myStroke;
                }

                /**
                 * @return the brush-thickness
                 */
                protected float getLineThickness() {
                    return myLineThickness;
                }
            }

    /**
             * This class encapsulates a dawing-style for a road that
             * is to be drawn with a border.
             * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
             */
            protected static class BorderedStyle extends Style {

                /**
                 * The stroke that is to be used for the border.
                 */
                private volatile BasicStroke myBorderStroke = null;

                /**
                 * @param aColor The color to draw in.
                 * @param aLineThickness The line-thickness to draw width.
                 */
                public BorderedStyle(final Color aColor, final float aLineThickness) {
                    super(aColor, aLineThickness);
                }

                /**
                 * Draw the given path in this style.
                 * @param aPath the path
                 * @param g where to draw to
                 * @param isFarAway if true everything will be drawn 1px wide
                 */
                @Override
                public void drawPath(final GeneralPath aPath, final Graphics2D g, final boolean isFarAway) {

                    if (!isFarAway) {
                        if (myBorderStroke == null)
                            myBorderStroke =  new BasicStroke(getLineThickness() + 2.0f);

                        Stroke  oldStroke = g.getStroke();
                        try {
                                g.setStroke(myBorderStroke);
                                g.setColor(Color.DARK_GRAY.brighter());
                                g.draw(aPath);
                        } finally {
                            g.setStroke(oldStroke);
                        }
                    }
                    super.drawPath(aPath, g, isFarAway);

                }
            }

            /**
             * This class encapsulates a dawing-style for a filled
             * area.
             * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
             */
            protected static class AreaStyle extends Style {

                /**
                 * @param aColor The color to draw the area in.
                 */
                public AreaStyle(final Color aColor) {
                    super(aColor);
                }

                /**
                 * Draw the given path in this style.
                 * @param aPath the path
                 * @param g where to draw to
                 * @param isFarAway if true everything will be drawn 1px wide
                 */
                @Override
                public void drawPath(final GeneralPath aPath, final Graphics2D g, final boolean isFarAway) {
                    Stroke  oldStroke = g.getStroke();

                    try {
                        g.setStroke(getStroke());
                        g.setColor(getColor());
                        g.fill(aPath);
                    } finally {
                        g.setStroke(oldStroke);
                    }
                }
            }

    /**
     * Remember the current polygon we paint, so as not
     * to do many drawLine-calls.
     */
    private GeneralPath currrentPath = null;
    /**
     * Same as currentPath but to be drawn with a 1px-wide black brush.
     */
    private GeneralPath currrentDirectionArrowPath = null;
    /**
     * Length of the arrows showing the direction
     * in oneway-streets.
     */
    private static final int DIRECTIONARROWLENGTH = 10;

    /**
     * Constructor that does nothing.
     */
    public BasePaintVisitor() {
        super();
    }

    /**
     * @param aG The environment to paint to.
     */
    public void setGraphics(final Graphics2D aG) {
        this.g = aG;
        if (Settings.getInstance().getBoolean("Painter.AntiAliasing", true)) {
            getGraphics2D().addRenderingHints(
                    new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON));
        } else {
            getGraphics2D().addRenderingHints(
                    new RenderingHints(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_OFF));
        }
    }

    /**
     * ${@inheritDoc}.
     */
    public void visitRoute(final IDataSet data, final List<RoutingStep> routeSegments) {
        this.myMap = data;
//        for (RoutingStep segment : routeSegments) {
//            visitRouteSegment(data, segment);
//        }
        GeneralPath aPath = new GeneralPath();
        if (routeSegments.isEmpty()) {
            return;
        }
        boolean isFirst = true;
        int lastX = Integer.MIN_VALUE;
        int lastY = Integer.MIN_VALUE;
        for (RoutingStep routingStep : routeSegments) {
            List<WayNode> wayNodes = routingStep.getNodes();
            for (WayNode wayNode : wayNodes) {
                Node node = data.getNodeByID(wayNode.getNodeId());
                if (node == null) {
                    continue;
                }
                Point next = node2Point(node);
                if (isFirst) {
                    aPath.moveTo(next.x, next.y);
                    lastX = next.x;
                    lastY = next.y;
                    isFirst = false;
                } else {
                    if (lastX != next.x || lastY != next.y) {
                        aPath.lineTo(next.x, next.y);
                    }
                    lastX = next.x;
                    lastY = next.y;
                }
          }
        }
        routeStyle.drawPath(aPath, getGraphics2D(), false);
    }
//    /**
//     * ${@inheritDoc}.
//     */
//    public void visitRouteSegment(final IDataSet data, final RoutingStep step) {
//       this.myMap = data;
//
//       List<WayNode> wayNodes = step.getNodes();
//       LOG.log(Level.FINEST, "visitRouteSegment() of " + wayNodes.size() + " nodes");
//       WayNode last = null;
//       for (WayNode node : wayNodes) {
//           if (last != null) {
//               LOG.log(Level.FINEST, "visitRouteSegment() - drawing a route-segment");
//             drawSegment(last.getNodeId(), node.getNodeId(), routeStyle, false);
//           }
//           last = node;
//       }
//
//       displaySegments(routeStyle); // flush
//
////       List<WayNode> wayNodes = step.getWay().getWayNodeList();
////       boolean started = false;
////       WayNode last = null;
////       for (WayNode node : wayNodes) {
////
////           if (started && last != null)
////               drawSegment(last.getNodeId(), node.getNodeId(), routeStyle, false);
////
////           if (started && (node.getNodeId() == step.getEndNode().getId() || node.getNodeId() == step.getStartNode().getId()))
////               break;
////
////           if ((!started) && (node.getNodeId() == step.getEndNode().getId() || node.getNodeId() == step.getStartNode().getId())) {
////               started = true;
////           }
////
////           last = node;
////       }
//    }

    /**
     * @param aNavComp MapView to get screen coordinates.
     */
    public void setNavigatableComponent(final INavigatableComponent aNavComp) {
        this.nc = aNavComp;
    }

    /**
     * ${@inheritDoc}.
     */
    public void visitNextManeuverPosition(final LatLon aNextManeuverPosition) {
        Point point = this.nc.getPoint(this.nc.getProjection().latlon2eastNorth(
                aNextManeuverPosition.lat(),
                aNextManeuverPosition.lon()));
        Graphics2D g2d = (Graphics2D) g;
        Stroke oldStroke = g2d.getStroke();
        Color oldColor = g2d.getColor();

        try {
            g2d.setColor(this.currentRouteStepStyle.getColor());
            g2d.setStroke(this.currentRouteStepStyle.getStroke());
            int diameter = (int) ROUTESEGMENTSTROKEWIDTH * 2;
            g2d.drawOval(point.x - diameter / 2, point.y - diameter / 2, diameter, diameter);
        } finally {
            g2d.setStroke(oldStroke);
            g2d.setColor(oldColor);
        }

    }

    /**
     * Draw gps tracks from given storage.
     * @param storage storage contains tracks for draw.
     */
    @SuppressWarnings("unchecked")
    public void visitGpsTracks(final GpsTracksStorage storage) {
        LOG.log(Level.FINE, "painting gps tracks");
        Graphics2D g2d = (Graphics2D) g;
        Stroke oldStroke = g2d.getStroke();
        Color oldColor = g2d.getColor();
        try {
//            g2d.setColor(this.currentTrackStyle.getColor());
            g2d.setStroke(this.currentTrackStyle.getStroke());
            for (Enumeration<GpsTrack> it = (Enumeration<GpsTrack>) storage.elements(); it.hasMoreElements();) {
                GpsTrack gpsTrack = it.nextElement();
                g2d.setColor((Color) storage.getProperty(gpsTrack, "color"));
                int nPoints = gpsTrack.size();
                int[] xPoints = new int[nPoints];
                int[] yPoints = new int[nPoints];
                int i = 0;
                for (final GpsTrackpoint trackpoint : gpsTrack) {
                    Point point = this.nc.getPoint(this.nc.getProjection()
                            .latlon2eastNorth(trackpoint.getLatlon().lat(),
                                    trackpoint.getLatlon().lon()));
                    xPoints[i] = point.x;
                    yPoints[i] = point.y;
                    i++;
                }
                g2d.drawPolyline(xPoints, yPoints, nPoints);
            }
        } finally {
            g2d.setStroke(oldStroke);
            g2d.setColor(oldColor);
        }
    }

    /**
     * ${@inheritDoc}.
     */
    public void visitCurrentPosition(final LatLon aCurrentPosition, final double aCurrentCourse, final double aCurrentSpeed) {
        Point point = this.nc.getPoint(this.nc.getProjection().latlon2eastNorth(
                aCurrentPosition.lat(),
                aCurrentPosition.lon()));
        Graphics2D g2d = (Graphics2D) g;
        Stroke oldStroke = g2d.getStroke();
        Color oldColor = g2d.getColor();

        try {
            g2d.setColor(this.currentPositionStyle.getColor());
            g2d.setStroke(this.currentPositionStyle.getStroke());
            int diameter = (int) ROUTESEGMENTSTROKEWIDTH * 2;
            // draw oriented triangle if user have movements (speed >= 4.0)
            int xCenter = point.x;
            int yCenter = point.y;
            if (aCurrentSpeed * UnitConstants.MILES_TO_KM >= MINSPEEDTOSHOWDIRECTION) {
                final int quarter = 4;
                int cosQ = (int) (diameter / quarter * Math.cos(Math.toRadians(aCurrentCourse)));
                int sinQ = (int) (diameter / quarter * Math.sin(Math.toRadians(aCurrentCourse)));
                int[] x = {xCenter + sinQ * 2,
                           xCenter + cosQ - sinQ,
                           xCenter - cosQ - sinQ};
                int[] y = {yCenter - cosQ * 2,
                           yCenter + sinQ + cosQ,
                           yCenter - sinQ + cosQ};
                g2d.drawPolygon(x, y, x.length);
            } else {
                // draw circle if user/car stopped (speed < 4)
                g2d.drawOval(xCenter - diameter / 2, yCenter - diameter / 2, diameter, diameter);
            }
            LOG.log(Level.FINEST, "painting current position at: [" + xCenter + ", " + yCenter + "] diameter=" + diameter
                    + " (width=" + g2d.getClipBounds().width + " height=" + g2d.getClipBounds().height + ")");
        } finally {
            g2d.setStroke(oldStroke);
            g2d.setColor(oldColor);
        }
    }

    /**
     * Helper-function to give he screen-coordinates for a node.
     * @param n the node
     * @return the coordinated on screen.
     */
    protected Point node2Point(final Node n) {
        return nc.getPoint(Settings.getProjection().latlon2eastNorth(n.getLatitude(), n.getLongitude()));
    }

    /**
     * Helper-function to give he screen-coordinates for a node.
     * @param nodeID the id of a node
     * @return null or the coordinated on screen.
     */
    protected Point node2Point(final long nodeID) {
        try {
            Node node = myMap.getNodeByID(nodeID);
            if (node == null)
                return null;
            return node2Point(node);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[Exception] Problem in "
                       + getClass().getName() + ":node2Point(nodeID=" + nodeID + ")",
                         e);
            return null;
        }
    }

    /**
     * Draw an number of the order of the segment within the parents way.
     * @param fromID the nodeID where to start
     * @param toID the nodeID where to end
     * @param orderNumber the number to draw
     */
    protected void drawOrderNumber(final long fromID, final long toID, final int orderNumber) {
        final int xOffset    = -4;
        final int yOffset    =  4;
        final int rectWidth  = 12;
        final int rectHeight = 14;
        final int charWidth  =  8;

        int strlen = ("" + orderNumber).length();
        Point p1 = node2Point(fromID);
        Point p2 = node2Point(toID);
        int x = (p1.x + p2.x) / 2 + xOffset * strlen;
        int y = (p1.y + p2.y) / 2 + yOffset;

        Rectangle screen = g.getClipBounds();
        if (screen.contains(x, y)) {
            Color c = g.getColor();
            g.setColor(backgroundColor);
            g.fillRect(x - 1, y - rectWidth, charWidth * strlen + 1, rectHeight);
            g.setColor(c);
            g.drawString("" + orderNumber, x, y);
        }
    }

    /**
     * Draw the node as small rectangle with the given color.
     *
     * @param n The node to draw.
     * @param color The color of the node.
     */
    public void drawNode(final Node n, final Color color) {
        if (n == null)
            throw new IllegalArgumentException("null node give");
        Point p = node2Point(n);
        if (p == null) {
            LOG.log(Level.SEVERE, "Could not get 2D-coordinates for node " + n.getId());
            return;
        }
        g.setColor(color);
        Rectangle screen = g.getClipBounds();

        if (screen == null || screen.contains(p.x, p.y))
            g.drawRect(p.x - 1, p.y - 1, 2, 2);
    }

    /**
     * Draw a line with the given color.
     * @param fromID the nodeID where to start
     * @param toID the nodeID where to end
     * @param  col the color to use
     * @param showDirection true to show direction-arrows
     */
    protected void drawSegment(final long fromID, final long toID, final Style col, final boolean showDirection) {

        if (col == null)
            throw new IllegalArgumentException("null color given");

        /*if (ls.incomplete)
            return;*/
        if (col != currentColor) {
            displaySegments(col);
        }

        Point p1 = node2Point(fromID);
        if (p1 == null)
            return; // ignore nodes that are not in the database
        Point p2 = node2Point(toID);
        if (p2 == null)
            return; // ignore nodes that are not in the database

        Rectangle screen = g.getClipBounds();
        Line2D line = new Line2D.Double(p1.x, p1.y, p2.x, p2.y);
        if (screen == null
            || screen.contains(p1.x, p1.y, p2.x, p2.y)
            || screen.intersectsLine(line)) { //TODO: this is not reliable
            if (currrentPath == null)
                currrentPath = new GeneralPath();

            currrentPath.moveTo(p1.x, p1.y);
            currrentPath.lineTo(p2.x, p2.y);

            if (showDirection) {
                double t = Math.atan2(p2.y - p1.y, p2.x - p1.x) + Math.PI;

                if (currrentDirectionArrowPath == null)
                    currrentDirectionArrowPath = new GeneralPath();

                int middleX = p2.x / 2 + p1.x / 2;
                int middleY = p2.y / 2 + p1.y / 2;
                currrentDirectionArrowPath.moveTo(middleX, middleY);

                currrentDirectionArrowPath.lineTo(
                        (int) (middleX + DIRECTIONARROWLENGTH * Math.cos(t - PHI)),
                        (int) (middleY + DIRECTIONARROWLENGTH * Math.sin(t - PHI)));
                currrentDirectionArrowPath.moveTo(
                        (int) (middleX + DIRECTIONARROWLENGTH * Math.cos(t + PHI)),
                        (int) (middleY + DIRECTIONARROWLENGTH * Math.sin(t + PHI)));
                currrentDirectionArrowPath.lineTo(middleX, middleY);
            }
        }
    }

    /**
     * Draw the {@link #currrentPath} and THEN set the new
     * color as the currentColor.
     * @param newColor  he new color
     */
    protected void displaySegments(final Style newColor) {

        if (currrentPath != null) {
            if (currentColor == null) {
                if (newColor == null)
                    throw new IllegalArgumentException("no newColor given");
                currentColor = newColor;
            }
            LOG.log(Level.FINEST, "actually painting segments");
            currentColor.drawPath(currrentPath, (Graphics2D) g, this.nc.getZoom() < FARAWAYZOOM);

            currrentPath = null;
            currentColor = newColor;
        }

        if (currrentDirectionArrowPath != null) {

            if (this.nc.getZoom() >= FARAWAYZOOM) {
                g.setColor(Color.BLACK);
                ((Graphics2D) g).draw(currrentDirectionArrowPath);
            }

            currrentDirectionArrowPath = null;
        }
    }

    /**
     * Get a color-value from the JOSM-preferences.
     * @param colName the name of the setting
     * @param def the default-value
     * @return the color
     */
    public Color getPreferencesColor(final String colName, final Color def) {
        String colStr = Settings.getPreferences().get("color." + colName);
        if (colStr.equals("")) {
            return def;
        }
        return ColorHelper.html2color(colStr);
    }

    /**
     * @return Returns the map.
     * @see #myMap
     */
    public IDataSet getMap() {
        return this.myMap;
    }

    /**
     * @param pMap The map to set.
     * @see #myMap
     */
    public void setMap(final IDataSet pMap) {
        if (pMap == null) {
            throw new IllegalArgumentException("null 'pMap' given!");
        }

        // use a simplified map below zoom=10
        final int lod1MinZoom = 8;
        final int lod2MinZoom = 6;
        final int lod3MinZoom = 4;
        if (getNavigatableComponent() != null
         && getNavigatableComponent().getZoom() <= lod1MinZoom
         && pMap instanceof LODDataSet) {
            this.myMap = ((LODDataSet) pMap).getLOD1DataSet();
            if (getNavigatableComponent().getZoom() <= lod2MinZoom) {
                this.myMap = ((LODDataSet) pMap).getLOD2DataSet();
            }
            if (getNavigatableComponent().getZoom() <= lod3MinZoom) {
                this.myMap = ((LODDataSet) pMap).getLOD3DataSet();
            }
        } else {
            this.myMap = pMap;
        }
    }

    /**
     * @return Returns the g.
     * @see #g
     */
    public Graphics2D getGraphics2D() {
        return this.g;
    }

    /**
     * @return Returns the nc.
     * @see #nc
     */
    public INavigatableComponent getNavigatableComponent() {
        return this.nc;
    }

    /**
     * @return Returns the nodeColor.
     * @see #nodeColor
     */
    public Color getNodeColor() {
        return this.nodeColor;
    }

    /**
     * @return Returns the backgroundColor.
     * @see #backgroundColor
     */
    public Color getBackgroundColor() {
        return this.backgroundColor;
    }

    /**
     * @return Returns the segmentStyle.
     * @see #segmentStyle
     */
    public Style getSegmentStyle() {
        return this.segmentStyle;
    }

    /**
     * @param pBackgroundColor The backgroundColor to set.
     * @see #backgroundColor
     */
    public void setBackgroundColor(final Color pBackgroundColor) {
        if (pBackgroundColor == null) {
            throw new IllegalArgumentException("null 'pBackgroundColor' given!");
        }
        this.backgroundColor = pBackgroundColor;
    }

    /**
     * @param pSegmentStyle The segmentStyle to set.
     * @see #segmentStyle
     */
    public void setSegmentStyle(final Style pSegmentStyle) {
        if (pSegmentStyle == null) {
            throw new IllegalArgumentException("null 'pSegmentStyle' given!");
        }
        this.segmentStyle = pSegmentStyle;
    }

    /**
     * @param pNodeColor The nodeColor to set.
     * @see #nodeColor
     */
    public void setNodeColor(final Color pNodeColor) {
        if (pNodeColor == null) {
            throw new IllegalArgumentException("null 'pNodeColor' given!");
        }
        this.nodeColor = pNodeColor;
    }

    /**
     * {@inheritDoc}
     */
    public void visitSelectedNode(final LatLon mySelectedNodePosition) {
        Graphics2D g2d = (Graphics2D) g;
//        Stroke oldStroke = g2d.getStroke();
        Color oldColor = g2d.getColor();
        Composite oldComposite = g2d.getComposite();
        final int d = 14;
        final int r = d / 2;
        try {
            Point point = this.nc.getPoint(this.nc.getProjection().latlon2eastNorth(
                    mySelectedNodePosition.lat(),
                    mySelectedNodePosition.lon()));
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1 / (float) 2));
            g.setColor(Color.BLUE);
            g.drawOval(point.x - r - 1, point.y - r - 1, d + 2, d + 2);
            g.setColor(Color.YELLOW);
            g.fillOval(point.x - r, point.y - r, d, d);
        } finally {
            g.setColor(oldColor);
            g.setComposite(oldComposite);
        }
    }


    /**
     * Cache for {@link #visitTrafficMessage(TrafficMessage)}.
     */
    private BufferedImage myTrafficMessageImage = null;
    /**
     * Paint the given traffic-message (traffic jam, road-obstruction, accident, ...).
     * @param aTrafficMessage the message to paint
     */
    @SuppressWarnings("unchecked")
    public void visitTrafficMessage(final TrafficMessage aTrafficMessage) {
        // ignored
        if (myTrafficMessageImage == null) {
            myTrafficMessageImage = ImageResources.getImage("accident.png");
            if (myTrafficMessageImage == null) {
                LOG.warning("Image accident.png missing");
                return;
            }
        }
        LatLon location = (LatLon) aTrafficMessage.getExtendedProperties().get(TrafficMessage.EXTPROPERTYLATLON);
        Point point = null;
        if (location != null) {
            point = nc.getPoint(Settings.getProjection().latlon2eastNorth(location));
        } else if (aTrafficMessage.getEntity() != null
                && aTrafficMessage.getEntity().getType().equals(EntityType.Node)) {
            Node n = (Node) aTrafficMessage.getEntity();
            point = node2Point(n);
        }
        Rectangle screen = g.getClipBounds();
        if (point != null) {
            if (screen.contains(point)) {
                getGraphics2D().drawImage(myTrafficMessageImage, point.x, point.y, null);
            }
        }

        List<LatLon> locations = (List<LatLon>) aTrafficMessage.getExtendedProperties().get(TrafficMessage.EXTPROPERTYEXTENDSTOLATLONS);
        if (locations != null) {
            for (LatLon latLon : locations) {
                point = nc.getPoint(Settings.getProjection().latlon2eastNorth(latLon));
                if (screen.contains(point)) {
                    getGraphics2D().drawImage(myTrafficMessageImage, point.x, point.y, null);
                }
            }
        }
    }

}
