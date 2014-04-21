package org.openstreetmap.travelingsalesman.painting.odr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Stack;
import java.util.logging.Logger;

import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * this is a factory class for different kind of ways.
 *
 * TODO make use of http://svn.openstreetmap.org/applications/rendering/mapnik/osm.xml to
 *      get colors of way types at certain zoom levels and other stuff.
 *
 * TODO to be able to use these rules, the conversion from scale values to scale denominator values
 *      has to be implemented (see: http://svn.openstreetmap.org/applications/rendering/mapnik/zoom-to-scale.txt)
 *      somewhere around INavigatableComponent.
 */
public final class ODRWayFactory {

    /**
     * Width for pathes.
     */
    private static final float PATH_WIDTH = 0.4f;

    /** Automatically created logger for debug and error-output. */
    private static final Logger LOG = Logger.getLogger(ODRWay.class.getName());

    /** first zoom step. */
    private static final float ZOOM_STEP_ONE = 10;
    /** first zoom step factor. */
    private static final float ZOOM_STEP_ONE_FACTOR = 1;
    /** second zoom step. */
    private static final float ZOOM_STEP_TWO = 12;
    /** second zoom step factor. */
    private static final float ZOOM_STEP_TWO_FACTOR = 1.2f;
    /** third zoom step. */
    private static final float ZOOM_STEP_THREE = 14;
    /** third zoom step factor. */
    private static final float ZOOM_STEP_THREE_FACTOR = 2;
    /** fourth zoom step. */
    private static final float ZOOM_STEP_FOUR = 16;
    /** fourth zoom step factor. */
    private static final float ZOOM_STEP_FOUR_FACTOR = 5;
    /** fifth zoom step. */
    private static final float ZOOM_STEP_FIVE = 20;
    /** fifth zoom step factor. */
    private static final float ZOOM_STEP_FIVE_FACTOR = 7;
    /** default zoom step factor. */
    private static final float ZOOM_STEP_DEFAULT_FACTOR = 5;
    /** base width for big ways. */
    private static final float BIG_WAY_BASE_WIDTH = 7;
    /** base width for primary ways. */
    private static final float PRIMARY_WAY_BASE_WIDTH = 7;
    /** base width for local ways. */
    private static final float LOCAL_WAY_BASE_WIDTH = 4;
    /** base width for smaller ways. */
    private static final float SMALLER_WAY_BASE_WIDTH = 2;
    /** base width for non-car ways. */
    private static final float NONCAR_WAY_BASE_WIDTH = 1;
    /** base width for default ways. */
    private static final float DEFAULT_WAY_BASE_WIDTH = 2;

    /**
     * this is a static factory class. nobody needs to instantiate it.
     */
    public ODRWayFactory() {
        init();
    }

    /**
     * set the current zoom level. the current zoom level is used to
     * calculate the width of an way.
     *
     * @param aCurrentZoomLevel the current zoom level
     */
    public void setCurrentZoomLevel(final int aCurrentZoomLevel) {
        currentZoomLevel = aCurrentZoomLevel;
    }

    /**
     * determines the correct type of the given way and returns an
     * instance of a fitting {@link ODRWay}.
     *
     * @param way way to wrap
     * @return {@link ODRWay} instance
     */
    public ODRWay getODRWay(final Way way) {
        ODRWay odrWay = null;
        ODR_WAY_TYPE type = ODR_WAY_TYPE.getWayType(way);

        switch (type) {
        case building:
        case parking:
        case leisure:
        case natural_water:
        case wood:
        case landuse:
            odrWay = new PolygonWay(way, type);
            break;
        case waterway:
        case railway:
        case railway_tram:
        case motorway:
        case highway:
        case trunk:
        case primary:
        case secondary:
        case tertiary:
        case residential:
        case service:
        case footway:
        case cycleway:
        case track:
        case pedestrian:
        case road:
        case path:
            odrWay = new PathWay(way, type);
            break;
        default:
            odrWay = null;
        }

        // TODO move following logic to Selector.isOneway
        if (odrWay != null) {
            odrWay.setOneway(WayHelper.isOneway(way));
//            odrWay.setBridge(WayHelper.isBridge(way));
        }
        return odrWay;
    }

    /**
     * returns a list of way types which describes the order
     * in which these way types should be painted.
     *
     * @return paint order list (z-index)
     */
    public Stack<ODR_WAY_TYPE> getPaintOrderList() {
        return paintOrderStack;
    }


    /**
     * constant for non-dashed lines.
     */
    private static final float[] NO_DASH = {1};

    /**
     * this method returns dash pattern,
     * for dashed lines rendered on the map.
     *
     * @param wayType way type
     * @return dash pattern (length of opaque and transparent section)
     * @see BasicStroke
     */
    public float[] getWayLineDashPattern(final ODR_WAY_TYPE wayType) {
        float width = getWayLineWidth(wayType);

        final int railwayOpaqueLength = 10;
        final int railwayTrasparentLength = 7;
        final int tramOpaqueLength = 7;
        final int tramTrasparentLength = 5;
        final int pathOpaqueLength = 10;
        final int pathTrasparentLength = 8;

        switch (wayType) {
        case railway:
            return new float[] {railwayOpaqueLength * width, railwayTrasparentLength * width};
        case railway_tram:
            return new float[] {tramOpaqueLength * width, tramTrasparentLength * width};
        case footway:
            return new float[] {1 * width, 2 * width};
        case path:
            return new float[] {pathOpaqueLength, pathTrasparentLength};
        default:
            return NO_DASH;
        }
    }

    /**
     * this method (tries to) calculates a reasonable
     * way widths for different zoom levels.
     *
     * @param wayType way type to calc width for
     * @return calced width
     */
    public float getWayLineWidth(final ODR_WAY_TYPE wayType) {
        float factor;
        if (currentZoomLevel <= ZOOM_STEP_ONE) factor = ZOOM_STEP_ONE_FACTOR;
        else if (currentZoomLevel <= ZOOM_STEP_TWO) factor = ZOOM_STEP_TWO_FACTOR;
        else if (currentZoomLevel <= ZOOM_STEP_THREE) factor = ZOOM_STEP_THREE_FACTOR;
        else if (currentZoomLevel <= ZOOM_STEP_FOUR) factor = ZOOM_STEP_FOUR_FACTOR;
        else if (currentZoomLevel >= ZOOM_STEP_FIVE) factor = ZOOM_STEP_FIVE_FACTOR;
        else factor = ZOOM_STEP_DEFAULT_FACTOR;

        float value;
        switch (wayType) {
        case building:
        case leisure:
        case parking:
        case natural_water:
        case wood:
            value = 1;
            break;
        case motorway:
        case highway:
        case trunk:
            value = BIG_WAY_BASE_WIDTH * factor;
            break;
        case primary:
            value = PRIMARY_WAY_BASE_WIDTH * factor;
            break;
        case secondary:
        case tertiary:
        case railway:
            value = LOCAL_WAY_BASE_WIDTH * factor;
            break;
        case residential:
        case service:
        case road:
        case waterway:
            value = SMALLER_WAY_BASE_WIDTH * factor;
            break;
        case railway_tram:
        case footway:
        case cycleway:
        case pedestrian:
        case track:
            value = NONCAR_WAY_BASE_WIDTH * factor;
            break;
        case path:
            value = PATH_WIDTH; // single pixel ways
            break;
        default:
            value = DEFAULT_WAY_BASE_WIDTH * factor;
        }
        return value;
    }

    /**
     * calculates the width of the stroke for a given way type.
     *
     * @param wayType way type to calc stroke width for.
     * @return calced stroke width
     */
    public float getWayStrokeWidth(final ODR_WAY_TYPE wayType) {
        return getWayLineWidth(wayType) + 1;
    }

    /**
     * Return true if way needs to be rendered with stroke.
     * @param wayType type of the way
     * @return true if way needs to be rendered with stroke.
     */
    public static boolean isStrokedWay(final ODR_WAY_TYPE wayType) {
        boolean value = true;
        switch (wayType) {
        case waterway:
        case natural_water:
        case wood:
        case path:
        case building:
        case parking:
            value = false;
            break;
        default:
            value = true;
        }
        return value;
    }

    /**
     * Return true if way needs to be rendered dashed.
     * @param aWayType the way to evaluate
     * @return true if way needs to be rendered dashed.
     */
    public static boolean isDashedWay(final ODR_WAY_TYPE aWayType) {
        return (aWayType == ODR_WAY_TYPE.railway
                || aWayType == ODR_WAY_TYPE.railway_tram
                || aWayType == ODR_WAY_TYPE.footway
                || aWayType == ODR_WAY_TYPE.path);
    }


    /**
     * determine the color of the given way type.
     *
     * @param wayType way type to determine color of
     * @return {@link Color}
     */
    public Color getWayColor(final ODR_WAY_TYPE wayType) {
        Color color;
        switch (wayType) {
        case building:
            color = Color.decode("0xCA9797");
            break;
        case leisure:
            color = Color.decode("0xc7f1a3");
            break;
        case railway:
            color = Color.decode("0x777777");
            break;
        case railway_tram:
            color = Color.decode("0x555555");
            break;
        case motorway:
            color = Color.decode("0x809BC0");
            break;
        case trunk:
            color = Color.decode("0x7FC97F");
            break;
        case primary:
            color = Color.decode("0xe46d71");
            break;
        case secondary:
            color = Color.decode("0xFDBF6F");
            break;
        case tertiary:
            color = Color.decode("0xf7f496");
            break;
        case residential:
        case service:
        case highway:
            color = Color.decode("0xFFFFFF");
            break;
        case footway:
            color = Color.decode("0xf68073");
            break;
        case cycleway:
            color = Color.decode("0xd1ead1");
            break;
        case track:
            color = Color.decode("0xd79331");
            break;
        case pedestrian:
            color = Color.decode("0xeeeeee");
            break;
        case road:
            color = Color.decode("0xaaaaaa");
            break;
        case path:
            color = Color.decode("0x000000");
            break;
        case natural_water:
        case waterway:
            color = Color.decode("0xb5d6f1");
            break;
        case parking:
            color = Color.decode("0xf7efb7");
            break;
        case wood:
        case landuse:
            color = Color.decode("0x72bf81");
            break;
        default:
            color = Color.BLACK;
        }

        return color;
    }

    /**
     * determine the color of the way stroke.
     *
     * @param wayType way type to determine stroke color of
     * @return {@link Color}
     */
    public Color getWayStrokeColor(final ODR_WAY_TYPE wayType) {
        Color color;
        switch (wayType) {
        case building:
            color = Color.decode("0xcccccc");
            break;
        case leisure:
            color = Color.decode("0x6fc18e");
            break;
        case footway:
            color = Color.decode("0xf3f1ed");
            break;
        default:
            color = Color.decode("0x777777");
        }

        return color;
    }

    /**
     * initialises internal data structures.
     */
    private void init() {
        paintOrderStack.push(ODR_WAY_TYPE.landuse);
        paintOrderStack.push(ODR_WAY_TYPE.wood);
        paintOrderStack.push(ODR_WAY_TYPE.natural_water);
        paintOrderStack.push(ODR_WAY_TYPE.parking);
        paintOrderStack.push(ODR_WAY_TYPE.leisure);
        paintOrderStack.push(ODR_WAY_TYPE.waterway);
        paintOrderStack.push(ODR_WAY_TYPE.pedestrian);
        paintOrderStack.push(ODR_WAY_TYPE.path);
        paintOrderStack.push(ODR_WAY_TYPE.track);
        paintOrderStack.push(ODR_WAY_TYPE.cycleway);
        paintOrderStack.push(ODR_WAY_TYPE.footway);
        paintOrderStack.push(ODR_WAY_TYPE.highway);
        paintOrderStack.push(ODR_WAY_TYPE.road);
        paintOrderStack.push(ODR_WAY_TYPE.service);
        paintOrderStack.push(ODR_WAY_TYPE.residential);
        paintOrderStack.push(ODR_WAY_TYPE.tertiary);
        paintOrderStack.push(ODR_WAY_TYPE.secondary);
        paintOrderStack.push(ODR_WAY_TYPE.primary);
        paintOrderStack.push(ODR_WAY_TYPE.railway_tram);
        paintOrderStack.push(ODR_WAY_TYPE.railway);
        paintOrderStack.push(ODR_WAY_TYPE.trunk);
        paintOrderStack.push(ODR_WAY_TYPE.motorway);
        paintOrderStack.push(ODR_WAY_TYPE.building);
    }

    /** the current zoom level. */
    private int currentZoomLevel;
    /** paint order list (z-index). */
    private final Stack<ODR_WAY_TYPE> paintOrderStack = new Stack<ODR_WAY_TYPE>();
}
