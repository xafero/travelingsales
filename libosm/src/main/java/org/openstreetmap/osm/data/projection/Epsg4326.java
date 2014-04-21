//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.osm.data.projection;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.coordinates.EastNorth;
import org.openstreetmap.osm.data.coordinates.LatLon;

/**
 * Directly use latitude / longitude values as x/y.
 *
 * @author imi
 */
public class Epsg4326 implements Projection {

    /**
     * 360 degrees.
     */
    private static final int DEGREES360 = 360;

    /**
     * This plugin has no  settings, thus this method returns null
     * as described in {@link IPlugin#getSettings()}.
     * @return null
     */
    public ConfigurationSection getSettings() {
        return null;
    }

    /**
     * ${@inheritDoc}.
     */
    public EastNorth latlon2eastNorth(final double lat, final double lon) {
        return new EastNorth(lon, lat);
    }

    /**
     * ${@inheritDoc}.
     */
    public LatLon eastNorth2latlon(final EastNorth p) {
        return new LatLon(p.north(), p.east());
    }

    /**
     * @return "EPSG:4326".
     */
    @Override
    public String toString() {
        return "EPSG:4326";
    }

    /**
     * @return "epsg4326".
     */
    public String getCacheDirectoryName() {
        return "epsg4326";
    }

    /**
     * @return 1/360 .
     */
    public double scaleFactor() {
        return 1.0 / DEGREES360;
    }

    /**
     * ${@inheritDoc}.
     */
    public EastNorth latlon2eastNorth(final LatLon pLatLon) {
        return latlon2eastNorth(pLatLon.lat(), pLatLon.lon());
    }
}
