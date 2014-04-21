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

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.LODDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.travelingsalesman.routing.selectors.Motorcar;
import org.openstreetmap.travelingsalesman.routing.selectors.UsedTags;
//import org.openstreetmap.travelingsalesman.painting.BasePaintVisitor.AreaStyle;
//import org.openstreetmap.travelingsalesman.painting.BasePaintVisitor.BorderedStyle;
//import org.openstreetmap.travelingsalesman.painting.BasePaintVisitor.Style;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

    /**
     * A visitor that paint a simple scheme of every primitive it visits to a
     * previous set graphic environment.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    public class SimplePaintVisitor extends BasePaintVisitor {

        /**
         * How many pixels wide to draw a default-way.
         */
        private static final int DEFAULTWAYWIDTH = 3;

        /**
         * How many pixels wide to draw a motorway.
         */
        private static final int MOTORWAYROADWIDTH = 6;

        /**
         * How many pixels wide to draw a trunk-road.
         */
        private static final int TRUNKROADWIDTH = 5;

        /**
         * How many pixels wide to draw a secondary road.
         */
        private static final int SECONDARYROADWIDTH = 4;

        /**
         * How many pixels wide to draw a link to a motorway.
         */
        private static final int MOTORWAYLINKROADWIDTH = 6;

        /**
         * How many pixels wide to draw a link to a primary road.
         */
        private static final int PRIMARYLINKROADWIDTH = 4;

        /**
         * How many pixels wide to draw a primary road.
         */
        private static final int PRIMARYROADWIDTH = 4;


        /**
         * How many pixels wide to draw a residential road.
         */
        private static final int RESIDENTIALROADWIDTH = 3;


        /**
         * my logger for debug and error-output.
         */
        private static final Logger LOG = Logger.getLogger(SimplePaintVisitor.class.getName());


        /**
         * Default-Style for a way.
         */
        private Style dfltWayColor = new BorderedStyle(Color.white, DEFAULTWAYWIDTH);

        /**
         * Default-Style for a waterway.
         */
        private Style defaultWaterColor = new Style(Color.blue.brighter(), DEFAULTWAYWIDTH);

        /**
         * This map maps the value of the "highway"-tag onto a {@link Style}
         * to draw it with.
         */
        private static final HashMap<String, Style> WAYSTYLEBYHIGHWAY = new HashMap<String, Style>();

        /**
         * This map maps the value of the "natural"-tag onto a {@link Style}
         * to draw it with.
         */
        private static final HashMap<String, Style> WAYSTYLEBYNATURAL = new HashMap<String, Style>();

        /**
         * This map maps the value of the "landuse"-tag onto a {@link Style}
         * to draw it with.
         */
        private static final HashMap<String, Style> WAYSTYLEBYLANDUSE = new HashMap<String, Style>();

        static {
            WAYSTYLEBYHIGHWAY.put("motorway", new BorderedStyle(Color.blue.brighter(), MOTORWAYROADWIDTH));
            WAYSTYLEBYHIGHWAY.put("motorway_link", new Style(Color.blue.brighter(), MOTORWAYLINKROADWIDTH));
            WAYSTYLEBYHIGHWAY.put("trunk", new BorderedStyle(Color.blue.brighter().brighter(), TRUNKROADWIDTH));

            WAYSTYLEBYHIGHWAY.put("primary", new Style(Color.RED.brighter(), PRIMARYROADWIDTH));
            WAYSTYLEBYHIGHWAY.put("primary_link", new Style(Color.RED.brighter(), PRIMARYLINKROADWIDTH));

            WAYSTYLEBYHIGHWAY.put("secondary", new BorderedStyle(Color.orange.brighter(), SECONDARYROADWIDTH));
            WAYSTYLEBYHIGHWAY.put("residential", new BorderedStyle(Color.white, RESIDENTIALROADWIDTH));

            Color forbiddenRoad = new Color(1f, 1f, 1f, 1f / 2);

            WAYSTYLEBYHIGHWAY.put("footway", new Style(forbiddenRoad, 2));
            WAYSTYLEBYHIGHWAY.put("cycleway", new Style(forbiddenRoad, 2));
            WAYSTYLEBYHIGHWAY.put("steps", new Style(forbiddenRoad, 2));

            WAYSTYLEBYNATURAL.put("water", new AreaStyle(new Color(Color.blue.brighter().getRed(),
                                                               Color.blue.brighter().getGreen(),
                                                               Color.blue.brighter().getBlue(),
                                                               Color.TRANSLUCENT / 2)));
            WAYSTYLEBYNATURAL.put("wood", new AreaStyle(new Color(Color.green.getRed(), Color.green.getGreen(), Color.green.getBlue(), Color.TRANSLUCENT / 2)));
            WAYSTYLEBYNATURAL.put("forest", new AreaStyle(new Color(Color.green.getRed(), Color.green.getGreen(), Color.green.getBlue(), Color.TRANSLUCENT / 2)));
            WAYSTYLEBYLANDUSE.put("forest", new AreaStyle(new Color(Color.green.getRed(), Color.green.getGreen(), Color.green.getBlue(), Color.TRANSLUCENT / 2)));
            WAYSTYLEBYLANDUSE.put("wood", new AreaStyle(new Color(Color.green.getRed(), Color.green.getGreen(), Color.green.getBlue(), Color.TRANSLUCENT / 2)));
            WAYSTYLEBYNATURAL.put("lang", new AreaStyle(new Color(Color.yellow.getRed(), Color.yellow.getGreen(), Color.yellow.getBlue(), Color.TRANSLUCENT / 2)));
        }

        /**
         * Show the number a segment of a way
         * between 2 nodes has.
         */
        private boolean showOrderNumber;

        /**
         * Create a new SimplePaintVisitor and load all settings.
         */
        public SimplePaintVisitor() {
            setNodeColor(getPreferencesColor("node", Color.RED.brighter()));
            setSegmentStyle(new Style(getPreferencesColor("segment", new Color(0, Byte.MAX_VALUE + 1, 0))));
            setBackgroundColor(getPreferencesColor("background", Color.BLACK));
            showOrderNumber = Settings.getPreferences().getBoolean("draw.segment.order_number");
        }

        /**
         * This plugin has no  settings, thus this method returns null
         * as described in {@link IPlugin#getSettings()}.
         * @return null
         */
        public ConfigurationSection getSettings() {
            return null;
        }

        /**
         * Visit all elements of the
         * dataset in the right order.
         * @param aData the dataset to visit
         * @param aGraphics where to draw to
         * @see org.openstreetmap.travelingsalesman.gui.widgets.IPaintVisitor#visitAll(org.openstreetmap.osm.data.MemoryDataSet, java.awt.Graphics)
         */
        public void visitAll(final IDataSet aData, final Graphics2D aGraphics) {

            //////////////////////////////////
            // use lower-zoom -map or fall back
            // to another renderer.

            IDataSet data = aData;
            final int lod1ZoomLevel = 7;
            final int lod2ZoomLevel = 5;
            final int lod3ZoomLevel = 3;
            if (getNavigatableComponent().getZoom() <= lod1ZoomLevel) {
                if (data instanceof LODDataSet) {
                    if (getNavigatableComponent().getZoom() <= lod3ZoomLevel) {
                        data = ((LODDataSet) data).getLOD3DataSet();
                    } else if (getNavigatableComponent().getZoom() <= lod2ZoomLevel) {
                        data = ((LODDataSet) data).getLOD2DataSet();
                    } else {
                        data = ((LODDataSet) data).getLOD1DataSet();
                    }
                } else {
                    LOG.log(Level.WARNING, "SimplePaintVisitor was ordered to "
                            + "paint at zoom=" + lod1ZoomLevel + " with a "
                            + "LODDataSet. This may be slow.");
                }
            }

            //////////////////////////////////
            // prepare

            this.setGraphics(aGraphics);
            this.setMap(data);

            long start = System.currentTimeMillis();
            long start2  = start;

            //////////////////////////////////
            // paint ways

            int waysCount = 0;
            Bounds mapBounds = this.getNavigatableComponent().getMapBounds();
            if (mapBounds == Bounds.WORLD) {
                LatLon min = getNavigatableComponent().getLatLon(0, 0);
                LatLon max = getNavigatableComponent().getLatLon(getNavigatableComponent().getWidth(), getNavigatableComponent().getHeight());
                LOG.log(Level.SEVERE, "internal error. mapBounds is world. Calculating sensible mapbounds\n"
                        + "\tmin=" + min + "\n"
                        + "\tmax=" + max);
                mapBounds = new Bounds(min, max);
            }
            for (final Iterator<Way> iter = data.getWays(mapBounds); iter.hasNext();) {
                Way element = iter.next();
                visit(element);
                waysCount++;
            }
            if (LOG.isLoggable(Level.FINEST))
                LOG.log(Level.FINEST, "visiting all " + waysCount + " ways took " + (System.currentTimeMillis() - start2) + "ms");

            start2 = System.currentTimeMillis();
            displaySegments(null);  // Flush segment cache before nodes
            int nodesCount = 0;
            for (final Iterator<Node> iter = data.getNodes(mapBounds); iter.hasNext();) {
                Node element = iter.next();
                visit(element);
                nodesCount++;
            }
            displaySegments(null);
            if (LOG.isLoggable(Level.FINEST))
                LOG.log(Level.FINEST, "visiting all " + nodesCount + " nodes took " + (System.currentTimeMillis() - start2)
                        + "ms total time to render=" + (System.currentTimeMillis() - start));
        }

        /**
         * @see org.openstreetmap.travelingsalesman.gui.widgets.IPaintVisitor#visit(org.openstreetmap.osmosis.core.domain.v0_5.Node)
         * @param n the node to paint
         */
        public void visit(final Node n) {
            LOG.log(Level.FINEST, "visitingn ode " + n);
            drawNode(n, getNodeColor());
        }

        /**
         * @param r The Relation we visit.
         */
        public void visit(final Relation r) {
            //relations are not rendered.
        }

        /**
         * @see org.openstreetmap.travelingsalesman.gui.widgets.IPaintVisitor#visit(org.openstreetmap.osmosis.core.domain.v0_5.Way)
         * @param w the way to paint
         */
        public void visit(final Way w) {
            LOG.log(Level.FINEST, "visiting way " + w);
            Style wayColor = dfltWayColor;

            // get color by type
            String highway = WayHelper.getTag(w, Tags.TAG_HIGHWAY);
            if (highway != null) {
                Style style = WAYSTYLEBYHIGHWAY.get(highway);
                if (style != null)
                    wayColor = style;
            } else if (WayHelper.getTag(w, UsedTags.TAG_WATERWAY) != null) {
                wayColor = defaultWaterColor;
            } else {
                String natural = WayHelper.getTag(w, UsedTags.TAG_NATURAL);
                if (natural != null) {
                    Style style = WAYSTYLEBYNATURAL.get(natural);
                    if (style != null)
                        wayColor = style;
                } else {
                    String landuse = WayHelper.getTag(w, UsedTags.TAG_LANDUSE);
                    if (landuse != null) {
                        Style style = WAYSTYLEBYLANDUSE.get(landuse);
                        if (style != null)
                            wayColor = style;
                    }
                }
            }

            //-------

            boolean oneWay = (new Motorcar()).isOneway(new MemoryDataSet(), w);

            int orderNumber = 0;
            long lastNodeID = -1;
            boolean hasLastNodeID = false;
            for (WayNode node : w.getWayNodes()) {
                if (!hasLastNodeID) {
                    lastNodeID = node.getNodeId();
                    hasLastNodeID = true;
                    continue;
                }
                orderNumber++;
                drawSegment(lastNodeID, node.getNodeId(), wayColor, oneWay);
                if (showOrderNumber) {
                   drawOrderNumber(lastNodeID, node.getNodeId(), orderNumber);
                }

                lastNodeID = node.getNodeId();
            }
        }

    }
