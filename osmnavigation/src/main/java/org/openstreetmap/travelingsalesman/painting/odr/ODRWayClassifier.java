package org.openstreetmap.travelingsalesman.painting.odr;

import java.awt.Color;
import java.util.HashSet;
import java.util.Hashtable;

import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * This class is filled with values from the odr-way-visualization-data.xml.
 *
 * It is able to classify a {@link Way} and deliver values for the correct
 * painting of the corresponding {@link ODRWay}.
 *
 * @author <a href="mailto:tdad@users.sourceforge.net">Sven Koehler</a>
 * @see ODRVisualizationDataReader
 */
public class ODRWayClassifier {

//    /** determines the way type. */
//    private String type;
//    /** minimal zoom level this way type is displayed. */
//    private int minZoomLevel;
//    /** maximal zoom level this way type is displayed. */
//    private int maxZoomLevel;
//    /** color in which this way is drawn. */
//    private Color strokeColor;
//    /** color of the way casing. */
//    private Color outlineColor;
//    /** should this way be painted dashed. */
//    private boolean dashed;
//    /** the {@link ODRWay} which paints this way. */
//    private Class<ODRWay> wayImplementation;
//    /** dash array. */
//    private float[] dashArray;
    /** the width of the stroke for every zoom level. */
    private final Hashtable<Integer, Float> zoomLevelWidths = new Hashtable<Integer, Float>();
    /** the key/value pairs, representing this way. */
    private final HashSet<Hashtable<String, HashSet<String>>> keyValuePairs = new HashSet<Hashtable<String, HashSet<String>>>();

    /**
     * adds a new key/value pair which defines this type of way.
     *
     * @param key key to add
     * @param value value to add
     */
    public void addKeyValuePair(final String key, final String value) {
        boolean inserted = false;
        for (Hashtable<String, HashSet<String>> kvp : keyValuePairs) {
            for (String ckey : kvp.keySet()) {
                if (ckey.equals(key)) {
                    kvp.get(ckey).add(value);
                    inserted = true;
                    break;
                }
            }
            if (inserted) break;
        }

        if (!inserted) {
            Hashtable<String, HashSet<String>> table = new Hashtable<String, HashSet<String>>();
            table.put(key, new HashSet<String>());
            if (value != null) {
                table.get(key).add(value);
            }

            keyValuePairs.add(table);
        }
    }

    /**
     * adds a new stroke width value for a certain zoom level.
     *
     * @param zoomLevel .
     * @param width .
     */
    public void addZoomLevelWidth(final int zoomLevel, final float width) {
        zoomLevelWidths.put(zoomLevel, width);
    }

    /**
     * @param aType the type to set
     */
    public void setType(final String aType) {
//        this.type = aType;
    }

    /**
     * @param aMinZoomLevel the minZoomLevel to set
     */
    public void setMinZoomLevel(final int aMinZoomLevel) {
//        this.minZoomLevel = aMinZoomLevel;
    }

    /**
     * @param aMaxZoomLevel the maxZoomLevel to set
     */
    public void setMaxZoomLevel(final int aMaxZoomLevel) {
//        this.maxZoomLevel = aMaxZoomLevel;
    }

    /**
     * @param aStrokeColor the strokeColor to set
     */
    public void setStrokeColor(final Color aStrokeColor) {
//        this.strokeColor = aStrokeColor;
    }

    /**
     * @param anOutlineColor the outlineColor to set
     */
    public void setOutlineColor(final Color anOutlineColor) {
//        this.outlineColor = anOutlineColor;
    }

    /**
     * @param aDashed the dashed to set
     */
    public void setDashed(final boolean aDashed) {
//        this.dashed = aDashed;
    }

    /**
     * @param aWayImplementation the wayImplementation to set
     */
    public void setWayImplementation(final Class<ODRWay> aWayImplementation) {
//        this.wayImplementation = aWayImplementation;
    }

    /**
     * @param aDashArray the dashArray to set
     */
    public void setDashArray(final float[] aDashArray) {
//        this.dashArray = aDashArray;
    }
}
