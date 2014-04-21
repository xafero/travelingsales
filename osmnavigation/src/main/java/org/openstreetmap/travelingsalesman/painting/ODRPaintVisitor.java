/**
 * ODRPaintVisitor.java
 * created: 09.12.2007 07:23:00
 * This file is part of osmnavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
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
 */
package org.openstreetmap.travelingsalesman.painting;


//automatically created logger for debug and error -output
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.ConfigurationSetting;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.ConfigurationSetting.TYPES;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.LODDataSet;
import org.openstreetmap.osm.data.NodeHelper;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.travelingsalesman.navigation.OsmNavigationConfigSection;
import org.openstreetmap.travelingsalesman.painting.odr.IMapFeaturesSet;
import org.openstreetmap.travelingsalesman.painting.odr.JosmMapFeatures;
import org.openstreetmap.travelingsalesman.painting.odr.ODRVisualizationDataReader;
import org.openstreetmap.travelingsalesman.painting.odr.ODRWay;
import org.openstreetmap.travelingsalesman.painting.odr.ODRWayFactory;
import org.openstreetmap.travelingsalesman.painting.odr.PathWay;
import org.openstreetmap.travelingsalesman.painting.odr.ODR_WAY_TYPE;
import org.openstreetmap.travelingsalesman.painting.odr.PolygonWay;
import org.openstreetmap.travelingsalesman.routing.NameHelper;
import org.openstreetmap.travelingsalesman.routing.selectors.UsedTags;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * Project: osmnavigation<br/>
 * ODRPaintVisitor.java<br/>
 * created: 09.12.2007 07:23:00 <br/>
 *<br/><br/>
 * PaintVisitor similar to SimplePaintVisitor that draws nicer output.<br/>
 * It paints ways by building Pathes using {@link PolygonWay} for areas
 * and {@link PathWay} for polylines. They can have smooth corners
 * as opposed to the way the SimplePaintVisitor paints each way-segment
 * as a straight line.<br/>
 * For Nodes it is able to paint Icons for POIS using {@link IMapFeaturesSet}.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 * @author <a href="mailto:tdad@users.sourceforge.net">Sven Koehler</a>
 * @author <a href="mailto:combbs@users.sourceforge.net">Oleg Chubaryov</a>
 *
 * TODO do some performance optimizations
 *
 * TODO do something with nodes. maybe someone else should deliver filtered POI's directly instead of
 *      walking through all the nodes.
 *
 * TODO find an efficient way to select pin width, font size and other stuff in advance of
 *      the current zoom level
 */
public class ODRPaintVisitor extends BasePaintVisitor implements IPaintVisitor {

    /**
     * {@inheritDoc}
     */
    public ConfigurationSection getSettings() {
        ConfigurationSection retval = new ConfigurationSection("");
        retval.addSetting(new ConfigurationSetting(IMapFeaturesSet.class,
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.IMapFeaturesSet.title"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.IMapFeaturesSet.category"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.IMapFeaturesSet.desc")));
        retval.addSetting(new ConfigurationSetting("ODRPainter.LOD1ZoomLevel",
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.LOD1ZoomLevel.title"),
                TYPES.INTEGER,
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.LOD1ZoomLevel.category"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.LOD1ZoomLevel.desc")));
        retval.addSetting(new ConfigurationSetting("ODRPainter.LOD2ZoomLevel",
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.LOD2ZoomLevel.title"),
                TYPES.INTEGER,
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.LOD2ZoomLevel.category"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.LOD2ZoomLevel.desc")));
        retval.addSetting(new ConfigurationSetting("ODRPainter.LOD3ZoomLevel",
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.LOD3ZoomLevel.title"),
                TYPES.INTEGER,
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.LOD3ZoomLevel.category"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.LOD3ZoomLevel.desc")));

        retval.addSetting(new ConfigurationSetting("Painter.AntiAliasing",
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.AntiAliasing.title"),
                TYPES.BOOLEAN,
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.AntiAliasing.category"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.AntiAliasing.desc")));
        retval.addSetting(new ConfigurationSetting("Painter.OffScreenCachingStrategy",
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.OffScreenCachingStrategy.title"),
                TYPES.BOOLEAN,
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.OffScreenCachingStrategy.category"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.OffScreenCachingStrategy.desc")));
        retval.addSetting(new ConfigurationSetting("Painter.OffScreenCachingSize",
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.OffScreenCachingSize.title"),
                TYPES.DOUBLE,
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.OffScreenCachingSize.category"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.OffScreenCachingSize.desc")));

        return retval;
    }

    /**
     * Create and initialize a new
     * ODRPaintVisitor.
     */
    public ODRPaintVisitor() {

        // if this file is not found, a default-file will be loaded via
        // reflection
        ODRVisualizationDataReader configDataReader = new ODRVisualizationDataReader();
        File file = new File(
                System.getProperty("user.home") + File.separator
                + ".libosm" + File.separator
                +  "way-visualization-data.xml");
        configDataReader.setOdrVisualizationDataFile(file);
        try {
            configDataReader.readData();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Cannot load configuration for chosen renderer ODRPaintVisitor from " + file.getAbsolutePath(), e);
        }
    }

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG  = Logger.getLogger(ODRPaintVisitor.class.getName());

    //------------------------ support for propertyChangeListeners ------------------

    /**
     * support for firing PropertyChangeEvents.
     * (gets initialized only if we really have listeners)
     */
    private volatile PropertyChangeSupport myPropertyChange = null;

    /**
     * Returned value may be null if we never had listeners.
     * @return Our support for firing PropertyChangeEvents
     */
    protected PropertyChangeSupport getPropertyChangeSupport() {
        return myPropertyChange;
    }

    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     *
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(
            final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property.  The listener
     * will be invoked only when a call on firePropertyChange names that
     * specific property.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(final String propertyName,
            final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The PropertyChangeListener to be removed
     */
    public final void removePropertyChangeListener(final String propertyName,
            final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(propertyName,
                    listener);
        }
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     *
     * @param listener  The PropertyChangeListener to be removed
     */
    public synchronized void removePropertyChangeListener(
            final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(listener);
        }
    }

    //-------------------------------------------------------

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    @Override
    public String toString() {
        return "ODRPaintVisitor@" + hashCode();
    }

    /**
     * Extract value of "name" tag from given node.
     *
     * @param aNode a node
     * @return text string with the name
     */
    private String getNodeCaption(final Node aNode) {
        return NameHelper.getNameForNode(getMap(), aNode);
    }

    /**
     * ${@inheritDoc}.
     */
    public void visit(final Node node) {

        // paint housenumbers
        if (WayHelper.getTag(node, UsedTags.TAG_ADDR_HOUSENUMBER) != null
               || WayHelper.getTag(node, UsedTags.TAG_ADDR_HOUSENAME) != null) {
            String nr = WayHelper.getTag(node, UsedTags.TAG_ADDR_HOUSENUMBER);
            String hname = WayHelper.getTag(node, UsedTags.TAG_ADDR_HOUSENAME);
            if (nr == null) {
                nr = "";
            }
            if (hname == null) {
                hname = "";
            }

            final int fontsize = 7; // should be very small to not distract
            getGraphics2D().setFont(new Font("", Font.ITALIC, fontsize));
            Point point = node2Point(node);
            getGraphics2D().drawString((nr + " " + hname).trim(), point.x, point.y);
        }
        // paint icons for special nodes
        NODETYPE nodeType = NODETYPE.getNodeType(node);
        switch(nodeType) {
        case city:
            mapFeatures.paintCity(node2Point(node), getNodeCaption(node), getGraphics2D());
            break;
        case town:
            mapFeatures.paintTown(node2Point(node), getNodeCaption(node), getGraphics2D());
            break;
        case hamlet:
            mapFeatures.paintHamlet(node2Point(node), getNodeCaption(node), getGraphics2D());
            break;
        case suburb:
            mapFeatures.paintSuburb(node2Point(node), getNodeCaption(node), getGraphics2D());
            break;
        case village:
            mapFeatures.paintVillage(node2Point(node), getNodeCaption(node), getGraphics2D());
            break;
        case aerodrome:
            mapFeatures.paintAerodrome(node2Point(node), getGraphics2D());
            break;
        case railway_station:
            mapFeatures.paintRailwayStation(node2Point(node), getGraphics2D());
            break;
        case bus_station:
            mapFeatures.paintBusStation(node2Point(node), getGraphics2D());
            break;
        case bus_stop:
            mapFeatures.paintBusStop(node2Point(node), getGraphics2D());
            break;
        case tram_stop:
            mapFeatures.paintTramStop(node2Point(node), getGraphics2D());
            break;
        case subway_entrance:
            mapFeatures.paintSubwayEntrance(node2Point(node), getGraphics2D());
            break;
        case parking:
            mapFeatures.paintParking(node2Point(node), getGraphics2D());
            break;
        case bicycle_parking:
            mapFeatures.paintBicycleParking(node2Point(node), getGraphics2D());
            break;
        case fuel:
            mapFeatures.paintFuel(node2Point(node), getGraphics2D());
            break;
        case car_sharing:
            mapFeatures.paintCarSharing(node2Point(node), getGraphics2D());
            break;
        case pharmacy:
            mapFeatures.paintPharmacy(node2Point(node), getGraphics2D());
            break;
        case hospital:
            mapFeatures.paintHospital(node2Point(node), getGraphics2D());
            break;
        case toilets:
            mapFeatures.paintToilets(node2Point(node), getGraphics2D());
            break;
        case university:
            mapFeatures.paintUniversity(node2Point(node), getGraphics2D());
            break;
        case school:
            mapFeatures.paintSchool(node2Point(node), getGraphics2D());
            break;
        case place_of_worship:
            mapFeatures.paintPlaceOfWorship(node2Point(node), getGraphics2D());
            break;
        case courthouse:
            mapFeatures.paintCourthouse(node2Point(node), getGraphics2D());
            break;
        case monument:
            mapFeatures.paintMonument(node2Point(node), getGraphics2D());
            break;
        case atm:
            mapFeatures.paintATM(node2Point(node), getGraphics2D());
            break;
        case bank:
            mapFeatures.paintBank(node2Point(node), getGraphics2D());
            break;
        case fountain:
            mapFeatures.paintFountain(node2Point(node), getGraphics2D());
            break;
        case police:
            mapFeatures.paintPolice(node2Point(node), getGraphics2D());
            break;
        case post_office:
            mapFeatures.paintPostOffice(node2Point(node), getGraphics2D());
            break;
        case post_box:
            mapFeatures.paintPostBox(node2Point(node), getGraphics2D());
            break;
        case amenity:
            mapFeatures.paintAmenity(node2Point(node), NodeHelper.getTag(node, UsedTags.TAG_AMENITY).toLowerCase(), getGraphics2D());
            break;
        case shop:
            mapFeatures.paintShop(node2Point(node), NodeHelper.getTag(node, UsedTags.TAG_SHOP).toLowerCase(), getGraphics2D());
            break;
        case tourism:
            mapFeatures.paintTourism(node2Point(node), NodeHelper.getTag(node, UsedTags.TAG_TOURISM).toLowerCase(), getGraphics2D());
            break;
        case ignore:
            break;
        case unknown:
        default:
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Found unknown node type:");
                for (Tag tag : node.getTags()) {
                    LOG.log(Level.FINE, tag.getKey() + " => " + tag.getValue());
                }
            }
        return;
        }
    }

    /**
     * ${@inheritDoc}.
     */
    public void visit(final Way way) {
        visitWay(way);
    }
    /**
     * Visit the way.
     * @param aWay way for visiting.
     * @return false wenn wir diesen Way nicht zeichnen können
     */
    public boolean visitWay(final Way aWay) {

        ODRWay odrWay = wayFactory.getODRWay(aWay);
        if (odrWay == null) {
            String tags = "";
            if (aWay.getTags().size() > 0) {
                tags = aWay.getTags().iterator().next().getKey() + "=" + aWay.getTags().iterator().next().getValue();
            }
            LOG.finest("way ignored because: factory cannot create odrway (#"
                    + aWay.getId() + " #tags="
                    + aWay.getTags().size() + " #nodes="
                    + aWay.getWayNodes().size() + ", highway=" + WayHelper.getTag(aWay, Tags.TAG_HIGHWAY)
                    + ", name=" + NameHelper.getNameForWay(aWay)
                    + " " + tags);
            return false;
        }
        if (odrWay == null || odrWay.getWayType() == ODR_WAY_TYPE.ignore) {
            LOG.finest("way ignored because: way " + odrWay.getName()  + " is to be ignored");
            return false;
        }

        int count = 0;
        for (WayNode wayNode : aWay.getWayNodes()) {
            Point pixelCoords = node2Point(wayNode.getNodeId());
            if (pixelCoords == null) {
                continue;
            }

            odrWay.addWayPoint(pixelCoords);
            count++;
        }

        if (count < 2) {
            if (aWay.getWayNodes().size() > 1) {
                LOG.finer("Ignoring way '" + odrWay.getName() + "' because we get no coordinates for it's nodes");
            }

            LOG.finest("way ignored because: way " + odrWay.getName()  + " has less then 2 nodes");
            return false;
        }

        HashSet<ODRWay> waysSet = waysToPaint.get(odrWay.getWayType());
        if (waysSet == null) {
            waysSet = new HashSet<ODRWay>();
            waysToPaint.put(odrWay.getWayType(), waysSet);
        }
        waysSet.add(odrWay);
        return true;
    }

    /**
     * all my ways by their way types.
     */
    private final Hashtable<ODR_WAY_TYPE, HashSet<ODRWay>> waysToPaint = new Hashtable<ODR_WAY_TYPE, HashSet<ODRWay>>();

    /**
     * my fallback painter for low zoom levels.
     */
    private IPaintVisitor fallbackPainter = new SmoothTilePainter();

    /**
     * my fallback painter for missing map-areas.
     */
    private IPaintVisitor noDataPainter = new SmoothTilePainter();

    /**
     * ${@inheritDoc}.
     */
    public void visitAll(final IDataSet aData, final Graphics2D aGraphics) {

        //////////////////////////////////
        // use lower-zoom -map or fall back
        // to another renderer.

        try {
            IDataSet data = aData;
            final int lod1ZoomLevel = Settings.getInstance().getInteger("ODRPainter.LOD1ZoomLevel", 13);
            final int lod2ZoomLevel = Settings.getInstance().getInteger("ODRPainter.LOD2ZoomLevel", 8);
            final int lod3ZoomLevel = Settings.getInstance().getInteger("ODRPainter.LOD3ZoomLevel", 3);
            int lodLevel = 0;
            if (getNavigatableComponent().getZoom() <= lod1ZoomLevel) {
                if (data instanceof LODDataSet) {
                    if (getNavigatableComponent().getZoom() <= lod3ZoomLevel) {
                        data = ((LODDataSet) data).getLOD3DataSet();
                        lodLevel = 3;
                    } else if (getNavigatableComponent().getZoom() <= lod2ZoomLevel) {
                        data = ((LODDataSet) data).getLOD2DataSet();
                        lodLevel = 2;
                    } else {
                        data = ((LODDataSet) data).getLOD1DataSet();
                        lodLevel = 1;
                    }
                } else {
                    LOG.log(Level.WARNING, "ODRPaintVisitor is currently not able to paint at "
                            + "lower zoom levels than " + lod1ZoomLevel + " (current zoom level is "
                            + getNavigatableComponent().getZoom() + ").\n"
                            + " Using fallback painter: " + fallbackPainter.getClass().getSimpleName() + ".");

                    fallbackPainter.setNavigatableComponent(getNavigatableComponent());
                    fallbackPainter.visitAll(data, aGraphics);

                    return;
                }
            }

            //////////////////////////////////
            // prepare
            setMap(data);
            setGraphics(aGraphics);
            wayFactory.setCurrentZoomLevel(getNavigatableComponent().getZoom());

            long start = System.currentTimeMillis();
            long start2  = start;

            //////////////////////////////////
            // get ways
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "<visitAll zoom=\"" + getNavigatableComponent().getZoom() + "\">");
            } else if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "visitAll() at zoom " + getNavigatableComponent().getZoom());
            }

            int waysCount = 0;
            int ignoredWaysCount = 0;
            for (final Iterator<Way> iter = data.getWays(this.getNavigatableComponent().getMapBounds()); iter.hasNext();) {
                Way element = iter.next();
                boolean notIgnored = visitWay(element);
                waysCount++;
                if (!notIgnored) {
                    ignoredWaysCount++;
                }
            }

            if (waysCount == 0) {
                if (LOG.isLoggable(Level.INFO)) {
                    LOG.log(Level.INFO, "No ways available for the visible area."
                            + " Do you need to download some data?");
                }
                noDataPainter.setNavigatableComponent(getNavigatableComponent());
                noDataPainter.visitAll(data, aGraphics);
                return;
            }

            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "</visitAll>");
            }

            paintWays();

            //if (LOG.isLoggable(Level.FINEST))
            LOG.log(Level.INFO, "visiting all " + waysCount + " ways (" + ignoredWaysCount + " ignored) took " + (System.currentTimeMillis() - start2) + "ms");

            start2 = System.currentTimeMillis();

            final int minZoomForOneWayArrows = 13;

            if (getNavigatableComponent().getZoom() > minZoomForOneWayArrows) {
                paintOneWaysArrows();
            }
            paintWayNames();
            LOG.log(Level.INFO, "painting ways names took " + (System.currentTimeMillis() - start2) + "ms");

            start2 = System.currentTimeMillis();

            //////////////////////////////////
            // get and paint nodes
            int nodesCount = 0;
            if (getNavigatableComponent().getZoom() > lod2ZoomLevel) {
                for (final Iterator<Node> iter = data.getNodes(this.getNavigatableComponent().getMapBounds()); iter.hasNext();) {
                    Node element = iter.next();
                    visit(element);
                    nodesCount++;
                }
            }
            LOG.log(Level.INFO, "visiting all " + nodesCount + " nodes took " + (System.currentTimeMillis() - start2)
                    + "ms total time to render=" + (System.currentTimeMillis() - start)
                    + "ms at LOD-level " + lodLevel + " zoom="  + getNavigatableComponent().getZoom());

            /////////////////////////////////
            // clean up
            waysToPaint.clear();
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot paint map", e);
        }

    }

    /**
     * paints all the ways in their order with their colors.
     */
    private void paintWays() {
        ListIterator<ODR_WAY_TYPE> wayTypeIterator = wayFactory.getPaintOrderList().listIterator();
        while (wayTypeIterator.hasNext()) {
            ODR_WAY_TYPE wayType = wayTypeIterator.next();
            HashSet<ODRWay> waySet = waysToPaint.get(wayType);
            if (waySet == null || waySet.size() == 0) {
                continue;
            }
            LOG.finer("painting " + waySet.size() + " ways of type " + wayType.name());
            float wayLineWidth = wayFactory.getWayLineWidth(wayType);
            Graphics2D g = getGraphics2D();


            if (wayLineWidth > 0) {
                GeneralPath wayTypePath = new GeneralPath();
                for (ODRWay way : waySet) {
                    try {
                        Shape shape = way.getWayShape();
                        if (shape == null)
                            continue;

                        if (shape instanceof Polygon) {
                            g.setColor(wayFactory.getWayColor(wayType));
                            g.fillPolygon((Polygon) shape);
                        }
                        wayTypePath.append(shape, false);
                        if (way instanceof PathWay) {
                            PathWay path = (PathWay) way;
                            LOG.finest("way '" + way.getName() + "' of type " + wayType.name() + " has " + path.getWayPoints().size() + " points");
                        }
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Exception while painting wayin ODRPaintVisitor", e);
                    }
                }

                Shape wayShape;
                Shape wayStroke;
                if (wayFactory.isDashedWay(wayType)) {
                    float[] pattern = wayFactory.getWayLineDashPattern(wayType);
                    wayShape = new BasicStroke(wayFactory.getWayLineWidth(wayType), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 20f, pattern, 0f).createStrokedShape(wayTypePath);
                } else {
                    wayShape = new BasicStroke(wayFactory.getWayLineWidth(wayType), BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER).createStrokedShape(wayTypePath);
                }
                if (ODRWayFactory.isStrokedWay(wayType)) {
                    wayStroke = new BasicStroke(wayFactory.getWayStrokeWidth(wayType), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER).createStrokedShape(wayTypePath);
                    g.setColor(wayFactory.getWayStrokeColor(wayType));
                    g.draw(wayStroke);
                }
                g.setColor(wayFactory.getWayColor(wayType));
                g.fill(wayShape);
            }
        }
    }

    /**
     * Paint arrows for oneway roads.
     * @see #allOnewayRoads
     */
    private void paintOneWaysArrows() {
        HashSet<ODRWay> allOnewayRoads = new HashSet<ODRWay>();
        Graphics2D g = getGraphics2D();
        for (ODR_WAY_TYPE wayType : waysToPaint.keySet()) {
            HashSet<ODRWay> waySet = waysToPaint.get(wayType);
            for (ODRWay way : waySet) {
                // add oneway roads
                if (way.isOneway()) {
                    allOnewayRoads.add(way);
                }
            }
            // draw arrows at oneway roads
            final int directionArrowLenght = 10;
            final float phi = 0.4f;
            final float deltaAlphaThreshold = 1.5f;
            final int distanceThreshold = 150;
            g.setColor(Color.BLUE);
            for (ODRWay way : allOnewayRoads) {
                GeneralPath currrentDirectionArrowPath = new GeneralPath();
                List<Point> wayPointsList = way.getWayPoints();
                if (wayPointsList.size() < 2)
                    continue;
                ListIterator<Point> wayPointIterator = wayPointsList
                        .listIterator();
                Point segmentStart = wayPointIterator.next();
                float prevAngle = 0;
                int prevX = 0, prevY = 0;
                boolean isFirst = true;
                while (wayPointIterator.hasNext()) {
                    Point segmentEnd = wayPointIterator.next();
                    float angle = calculateSegmentAngle(segmentEnd,
                            segmentStart);
                    int middleX = segmentEnd.x / 2 + segmentStart.x / 2;
                    int middleY = segmentEnd.y / 2 + segmentStart.y / 2;
                    // draw arrow only if its far from previsios or have a
                    // significant direction change
                    float dAngle = (float) Math.abs(prevAngle - angle);
                    if (dAngle > Math.PI)
                        dAngle -= Math.PI;
                    int dist = (int) Math.sqrt((prevX - middleX)
                            * (prevX - middleX) + (prevY - middleY)
                            * (prevY - middleY));
                    if (isFirst || dAngle > deltaAlphaThreshold
                            || dist > distanceThreshold) {
                        currrentDirectionArrowPath.moveTo(middleX, middleY);
                        currrentDirectionArrowPath.lineTo((int) (middleX + 2
                                * directionArrowLenght * Math.cos(angle)),
                                (int) (middleY + 2 * directionArrowLenght
                                        * Math.sin(angle)));
                        currrentDirectionArrowPath.moveTo(middleX, middleY);
                        currrentDirectionArrowPath.lineTo(
                                (int) (middleX + directionArrowLenght
                                        * Math.cos(angle - phi)),
                                (int) (middleY + directionArrowLenght
                                        * Math.sin(angle - phi)));
                        currrentDirectionArrowPath.moveTo(
                                (int) (middleX + directionArrowLenght
                                        * Math.cos(angle + phi)),
                                (int) (middleY + directionArrowLenght
                                        * Math.sin(angle + phi)));
                        currrentDirectionArrowPath.lineTo(middleX, middleY);
                        ((Graphics2D) g).draw(currrentDirectionArrowPath);
                        prevX = middleX;
                        prevY = middleY;
                        prevAngle = angle;
                        isFirst = false;
                    }
                    segmentStart = segmentEnd;
                }
            }
        }
    }

    /**
     * Paint all the names of the ways.
     */
    private void paintWayNames() {
        Graphics2D g = getGraphics2D();
        g.setColor(Color.BLACK);
        final int fontMaximizer = 0;
        int zoom = getNavigatableComponent().getZoom();

        for (ODR_WAY_TYPE wayType : waysToPaint.keySet()) {
            HashSet<ODRWay> allWaysWithNames = new HashSet<ODRWay>();
            // when zooming out, start to hide the
            // name of lower leven streets
            if (zoom < wayType.getMinZoomWithName()) {
                continue;
            }
            g.setFont(new Font("", wayType.getFontStyle(), zoom
                            + fontMaximizer));
            FontMetrics fm = g.getFontMetrics();

            HashSet<ODRWay> waySet = waysToPaint.get(wayType);
            for (ODRWay odrWay : waySet) {
                Way way = odrWay.getWay();
                if (NameHelper.getNameForWay(way) != null
                        || WayHelper.getTag(way, UsedTags.TAG_ADDR_HOUSENUMBER) != null
                        || WayHelper.getTag(way, UsedTags.TAG_ADDR_HOUSENAME) != null) {
                    allWaysWithNames.add(odrWay);
                    continue;
                }
            }

            // name all ways with names :)
            for (ODRWay way : allWaysWithNames) {
                List<Point> wayPointsList = way.getWayPoints();
                if (wayPointsList.size() < 2)
                    continue;
                if (WayHelper.isPolygon(way.getWay())) {
                    
                    paintPolygonalWay(g, fm, way);
                } else {

                    paintPolylineWay(g, fm, way, wayPointsList);
                }
            }
        }

    }

    /**
     * Paint a way that is not a closed polygon.
     * @param g graphics to draw to
     * @param fm metrics of the univesaly used font
     * @param way the way to paint
     * @param wayPointsList the nodes of the way
     */
    private void paintPolylineWay(final Graphics2D g,
                                  final FontMetrics fm,
                                  final ODRWay way,
                                  final List<Point> wayPointsList) {
        // we'll start somewhere after the first quarter
        final int sectionStart = 4;
        int index = (int) Math.floor(wayPointsList.size()
                / sectionStart);
        final float maxAngleDeviance = 0.1f;
        ListIterator<Point> wayPointIterator = wayPointsList
                .listIterator(index);

        /**
         * another approach to paint around corners // collect some
         * basic data final String wayName = way.getName(); int
         * currentNameIndexPosition = 0;
         *
         * Point nextEndPoint = null; Point currentSegmentStart =
         * wayPointIterator.next(); Point currentSegmentEnd =
         * wayPointIterator.next(); float currentSegmentAngle =
         * calculateSegmentAngle(currentSegmentStart,
         * currentSegmentEnd);
         *
         * while (currentNameIndexPosition < wayName.length() &&
         * wayPointIterator.hasNext()) { while
         * (wayPointIterator.hasNext()) { Point nextPoint =
         * wayPointIterator.next(); float nextAngle =
         * calculateSegmentAngle(currentSegmentStart, nextPoint); if
         * (nextAngle == currentSegmentAngle) { currentSegmentEnd =
         * nextPoint; } else { nextEndPoint = nextPoint; break; } }
         *
         * // we don't want our names upside down if
         * (currentSegmentStart.x > currentSegmentEnd.x) {
         * currentSegmentAngle =
         * calculateSegmentAngle(currentSegmentEnd,
         * currentSegmentStart); }
         *
         * final int segmentLength =
         * calculateSegmentLength(currentSegmentStart,
         * currentSegmentEnd); int segementLengthLeft =
         * segmentLength; StringBuilder segmentNameBuilder = new
         * StringBuilder();
         *
         * if (nextEndPoint != null) {
         * segmentNameBuilder.append(' '); segementLengthLeft -=
         * fm.charWidth(' '); }
         *
         * while (segementLengthLeft > 0 && currentNameIndexPosition
         * < wayName.length()) { char c =
         * wayName.charAt(currentNameIndexPosition);
         * currentNameIndexPosition++; segementLengthLeft -=
         * fm.charWidth(c); segmentNameBuilder.append(c); }
         *
         * // do a little centering final int wayWidthDivisor = 3;
         * final int x = currentSegmentStart.x - (int)
         * Math.floor((wayFactory.getWayLineWidth(way.getWayType())
         * / 2)); final int y = currentSegmentStart.y + (int)
         * Math.ceil((wayFactory.getWayLineWidth(way.getWayType()) /
         * wayWidthDivisor));
         *
         * AffineTransform origTransform = g.getTransform();
         * g.rotate(currentSegmentAngle, currentSegmentStart.x,
         * currentSegmentStart.y);
         * g.drawString(segmentNameBuilder.toString(), x, y);
         * g.setTransform(origTransform);
         *
         * if (nextEndPoint != null) { currentSegmentStart =
         * currentSegmentEnd; currentSegmentEnd = nextEndPoint;
         * currentSegmentAngle =
         * calculateSegmentAngle(currentSegmentStart,
         * currentSegmentEnd); nextEndPoint = null; } else break; }
         * end another try
         */

        // let's find ourselves a way segment to write the name on
        Point segmentStart = wayPointIterator.next();
        Point segmentEnd = wayPointIterator.next();
        float angle = calculateSegmentAngle(segmentStart,
                segmentEnd);

        while (wayPointIterator.hasNext()) {
            Point nextPoint = wayPointIterator.next();
            float nextAngle = calculateSegmentAngle(segmentStart,
                    nextPoint);
            if (Math.abs(angle - nextAngle) < maxAngleDeviance) {
                segmentEnd = nextPoint;
            }
        }

        // we do not wan't our street names upside down
        if (segmentStart.x > segmentEnd.x) {
            Point tmp = segmentStart;
            segmentStart = segmentEnd;
            segmentEnd = tmp;
            angle = calculateSegmentAngle(segmentStart, segmentEnd);
        }

        // do we have enough space?
        int segmentLenght = calculateSegmentLength(segmentStart,
                segmentEnd);
        if (way.getName() != null) {
            int streetNameLength = fm.stringWidth(way.getName());
            if (streetNameLength > segmentLenght
                    && LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "Segment is not long enough ("
                        + segmentLenght + ") for street name ("
                        + streetNameLength + "), this will overlap!");
            }

            StringBuilder wayNamePartBuilder = new StringBuilder();
            int currentNamePosition = 0;
            int leftSegmentLenght = segmentLenght;

            while (leftSegmentLenght > 0
                    && currentNamePosition < way.getName().length()) {
                char c = way.getName().charAt(currentNamePosition);
                leftSegmentLenght -= fm.charWidth(c);
                wayNamePartBuilder.append(c);
                currentNamePosition++;
            }

            // do a little centering
            final int wayWidthDivisor = 3;
            int x = segmentStart.x
            - (int) Math.floor((wayFactory.getWayLineWidth(way
                    .getWayType()) / 2));
            int y = segmentStart.y
            + (int) Math.ceil((wayFactory.getWayLineWidth(way
                    .getWayType()) / wayWidthDivisor));
            // do a transformation backup
            AffineTransform origTransform = g.getTransform();
            // draw the street name
            g.rotate(angle, segmentStart.x, segmentStart.y);
            // g.drawString(way.getName(), x, y);
            g.drawString(wayNamePartBuilder.toString(), x, y);
            // restore orginal transformation
            g.setTransform(origTransform);
        }
    }

    /**
     * Paint a way that is a closed polygon.
     * @param g graphics to draw to
     * @param fm metrics of the univesaly used font
     * @param way the way to paint
     */
    private void paintPolygonalWay(final Graphics2D g, final FontMetrics fm, final ODRWay way) {
        // draw caption on polygon area
        Point center = polygonCenter(way.getWayPoints());
        Dimension dims = polygonDimension(way.getWayPoints());
        // get the Name and house-number of the building/area
        StringBuffer sb = new StringBuffer();
        String name = NameHelper.getNameForWay(way.getWay());
        String housenumber = WayHelper.getTag(way.getWay(), UsedTags.TAG_ADDR_HOUSENUMBER);
        String housename = WayHelper.getTag(way.getWay(), UsedTags.TAG_ADDR_HOUSENAME);
        if (name != null) {
            sb.append(name);
        }
        if (housename != null) {
            if (sb.length() > 0)
                sb.append(", "); //TODO add muti line breaker
            sb.append(housename);
        }
        if (housenumber != null) {
            if (sb.length() > 0)
                sb.append(", "); //TODO add muti line breaker
            sb.append(housenumber);
        }
        String text = sb.toString();
        // determine the screen-area required for rendering the name
        int nameWidth = fm.stringWidth(text);
        int nameHeight = fm.getHeight();
        if (nameWidth > dims.width) {
            if (dims.width == 0) {
                dims.width = 1;
            }
            int preLines = (int) Math.ceil((double) nameWidth / dims.width);
            int charsInLine = (int) Math.ceil(text.length() / preLines);
            int maxLines = (int) Math.ceil((double) dims.height / fm.getHeight());
            int lines = Math.min(preLines, maxLines);
            text = text.substring(0, lines * charsInLine);
            for (int i = 0; i < lines; i++) {
                String l = text.substring(
                        i * charsInLine,
                        i != (lines - 1) ? (i + 1) * charsInLine
                                : text.length()).trim();
                if ((lines % 2) == 1) {
                    g.drawString(l, center.x - fm.stringWidth(l) / 2,
                            center.y + fm.getAscent() / 2 - (lines / 2 - i) * nameHeight);
                } else {
                    g.drawString(l, center.x - fm.stringWidth(l) / 2,
                      center.y - (lines / 2 - i - 1) * nameHeight);
                }
            }
        } else {
            g.drawString(text, center.x - nameWidth / 2, center.y + fm.getAscent() / 2);
        }
    }

    /**
     * Function to calculate dimension box of polygon.
     * @param points points in the polygon
     * @return height and width of polygon
     */
    public static Dimension polygonDimension(final List<Point> points) {
        int maxX = Integer.MIN_VALUE, minX = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE, minY = Integer.MAX_VALUE;
        for (Point p : points) {
            if (maxX < p.x) maxX = p.x;
            if (minX > p.x) minX = p.x;
            if (maxY < p.y) maxY = p.y;
            if (minY > p.y) minY = p.y;
        }
        return new Dimension(maxX - minX, maxY - minY);
    }

    /**
     * Function to calculate the center of mass for a given polygon, according
     * ot the algorithm defined at
     * http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/ .
     *
     * @param points points in the polygon
     * @return point that is the center of polygon (x, y)
     */
    public static Point polygonCenter(final List<Point> points) {
        int x = 0, y = 0;
        for (Point p : points) {
            x += p.x;
            y += p.y;
        }
        int size = points.size();
        Point lastPoint = points.get(points.size() - 1);
        if (points.get(0).equals(lastPoint)) {
            // don't include last point to calculation for closed polygons
            size--;
            x -= lastPoint.x;
            y -= lastPoint.y;
        }
        x = x / size;
        y = y / size;
        return new Point(x, y);
    }

    /**
     * calculates the angle of the line described by the given points in
     * respect of the null vector.
     *
     * @param start start point of the line
     * @param end end point of the line
     * @return angle.
     */
    private float calculateSegmentAngle(final Point start, final Point end) {
        return (float) Math.atan2(end.y - start.y, end.x - start.x);
    }

    /**
     *
     * calculates the length of a line described by the given points using the
     * Pythagoras theorem.
     *
     * @param start start point of the line
     * @param end end point of the line
     * @return segment length
     */
    private int calculateSegmentLength(final Point start, final Point end) {
        return Math.round((float) Math.sqrt(Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2)));
    }

    /**
     * ${@inheritDoc}.
     *
     * @param aCurrentPosition The current user-position to mark.
     */
    public void visitCurrentPosition(final LatLon aCurrentPosition, final double aCurrentCourse, final double aCurrentSpeed) {
        super.visitCurrentPosition(aCurrentPosition, aCurrentCourse, aCurrentSpeed);
    }

    /**
     * ${@inheritDoc}.
     */
    public void visitNextManeuverPosition(final LatLon aNextManeuverPosition) {
        super.visitNextManeuverPosition(aNextManeuverPosition);
    }

    /**
     * ${@inheritDoc}.
     */
    public void visit(final Relation aR) {
        // ignored
    }

    /** my way factory. */
    private final ODRWayFactory wayFactory = new ODRWayFactory();

    /** my poi painter. */
    private final IMapFeaturesSet mapFeatures = Settings.getInstance().getPlugin(IMapFeaturesSet.class, JosmMapFeatures.class.getName());

    /**
     * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
     * Project: osmnavigation<br/>
     * ODRPaintVisitor.java<br/>
     * created: 09.12.2007 08:16:28 <br/>
     *<br/><br/>
     * The types of nodes we know how to draw.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     * @author <a href="mailto:tdad@users.sourceforge.net">Sven Koehler</a>
     */
    public enum NODETYPE {
        /**
         * aeroways.
         */
        aerodrome,
        /**
         * a railway-station represented by a node.
         */
        railway_station,
        bus_station,
        bus_stop,
        tram_stop,
        subway_entrance,
        parking,
        bicycle_parking,
        fuel,
        car_sharing,
        /**
         * pharmacy.
         */
        pharmacy,
        hospital,
        /**
         * shops.
         */
        shop, // for all kind of shops
        /**
         * amenities.
         */
        amenity, // for all kind of amenity don't included in other node types
        /**
         * tourism.
         */
        tourism, // for all kind of tourism node
        /**
         * public.
         */
        toilets,
        university,
        school,
        place_of_worship,
        courthouse,
        /**
         * tourism.
         */
        monument,
        post_office,
        post_box,
        /**
         * money.
         */
        atm,
        bank,
        /**
         * others features.
         */
        fountain,
        police,
        /**
         * places.
         */
        city,
        town,
        hamlet,
        suburb,
        village,
        /**
         * we do now know.
         */
        unknown,
        /**
         * we do not wanna paint this one.
         */
        ignore;
        // TODO amenity (car_sharing, ...)

        /**
         * @param node the node to analyze.
         * @return the NODE_YPE for the given node.
         */
        public static NODETYPE getNodeType(final Node node) {
            if (node == null) {
                return ignore; // ???
            }

            Collection<Tag> tagList = node.getTags();

            if (tagList.size() == 0) {
                return ignore;
            }

            for (Tag tag : tagList) {
                if (tag.getKey() == null) {
                    continue;
                }
                if (tag.getValue() == null) {
                    continue;
                }

                if (tag.getKey().equalsIgnoreCase(Tags.TAG_PLACE)) {
                    if (tag.getValue().equalsIgnoreCase("city")) return city;
                    if (tag.getValue().equalsIgnoreCase("town")) return town;
                    if (tag.getValue().equalsIgnoreCase("hamlet")) return hamlet;
                    if (tag.getValue().equalsIgnoreCase("suburb")) return suburb;
                    if (tag.getValue().equalsIgnoreCase("village")) return village;
                    return ignore;
                }
                if (tag.getKey().equalsIgnoreCase("highway")) {
                    if (tag.getValue().equalsIgnoreCase("bus_stop")) return bus_stop;
                    return ignore;
                }
                if (tag.getKey().equalsIgnoreCase("aeroway")) {
                    if (tag.getValue().equalsIgnoreCase("aerodrome")) return aerodrome;
                    return ignore;
                }
                if (tag.getKey().equalsIgnoreCase("railway")) {
                    if (tag.getValue().equalsIgnoreCase("tram_stop")) return tram_stop;
                    if (tag.getValue().equalsIgnoreCase("station")) return railway_station;
                    if (tag.getValue().equalsIgnoreCase("subway_entrance")) return subway_entrance;
                    return ignore;
                }
                if (tag.getKey().equalsIgnoreCase(UsedTags.TAG_AMENITY)) {
                    if (tag.getValue().equalsIgnoreCase("pharmacy")) return pharmacy;
                    if (tag.getValue().equalsIgnoreCase("hospital")) return hospital;
                    if (tag.getValue().equalsIgnoreCase("parking")) return parking;
                    if (tag.getValue().equalsIgnoreCase("fuel")) return fuel;
                    if (tag.getValue().equalsIgnoreCase("car_sharing")) return car_sharing;
                    if (tag.getValue().equalsIgnoreCase("bus_station")) return bus_station;
                    if (tag.getValue().equalsIgnoreCase("toilets")) return toilets;
                    if (tag.getValue().equalsIgnoreCase("university")) return university;
                    if (tag.getValue().equalsIgnoreCase("school")) return school;
                    if (tag.getValue().equalsIgnoreCase("place_of_worship")) return place_of_worship;
                    if (tag.getValue().equalsIgnoreCase("atm")) return atm;
                    if (tag.getValue().equalsIgnoreCase("bank")) return bank;
                    if (tag.getValue().equalsIgnoreCase("bicycle_parking")) return bicycle_parking;
                    if (tag.getValue().equalsIgnoreCase("courthouse")) return courthouse;
                    if (tag.getValue().equalsIgnoreCase("fountain")) return fountain;
                    if (tag.getValue().equalsIgnoreCase("police")) return police;
                    if (tag.getValue().equalsIgnoreCase("post_office")) return post_office;
                    if (tag.getValue().equalsIgnoreCase("post_box")) return post_box;
                    return amenity;
                }
                if (tag.getKey().equalsIgnoreCase(UsedTags.TAG_SHOP)) {
                    return shop;
                }
                if (tag.getKey().equalsIgnoreCase(UsedTags.TAG_TOURISM)) {
                    return tourism;
                }
                if (tag.getKey().equalsIgnoreCase("historic")) {
                    if (tag.getValue().equalsIgnoreCase("monument")) return monument;
                    return ignore;
                }
            }
            return ignore;
        }
    }

    /**
     * Get my fallback painter for low zoom-levels.
     * @return Returns the fallbackPainter.
     * @see #fallbackPainter
     */
    public IPaintVisitor getFallbackPainter() {
        return fallbackPainter;
    }

    /**
     *  Set my fallback painter for low zoom-levels.
     * @param aFallbackPainter The fallbackPainter to set.
     * @see #fallbackPainter
     */
    public void setFallbackPainter(final IPaintVisitor aFallbackPainter) {
        if (aFallbackPainter == null) {
            throw new IllegalArgumentException("null 'aFallbackPainter' given!");
        }

        Object old = fallbackPainter;
        if (old == aFallbackPainter) {
            return; // nothing has changed
        }
        fallbackPainter = aFallbackPainter;
        // <<insert code to react further to this change here
        PropertyChangeSupport propertyChangeFirer = getPropertyChangeSupport();
        if (propertyChangeFirer != null) {
            propertyChangeFirer.firePropertyChange("aFallbackPainter", old,
                    aFallbackPainter);
        }
    }

    /**
     * Get my fallback painter for missing map-areas.
     * @return Returns the noDataPainter.
     * @see #noDataPainter
     */
    public IPaintVisitor getNoDataPainter() {
        return noDataPainter;
    }

    /**
     *  Set my fallback painter for missing map-areas.
     * @param aNoDataPainter The noDataPainter to set.
     * @see #noDataPainter
     */
    public void setNoDataPainter(final IPaintVisitor aNoDataPainter) {
        if (aNoDataPainter == null) {
            throw new IllegalArgumentException("null 'aNoDataPainter' given!");
        }

        Object old = noDataPainter;
        if (old == aNoDataPainter) {
            return; // nothing has changed
        }
        noDataPainter = aNoDataPainter;
        // <<insert code to react further to this change here
        PropertyChangeSupport propertyChangeFirer = getPropertyChangeSupport();
        if (propertyChangeFirer != null) {
            propertyChangeFirer.firePropertyChange("aNoDataPainter", old,
                    aNoDataPainter);
        }
    }

}


