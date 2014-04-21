//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.osm.data.projection;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.coordinates.EastNorth;
import org.openstreetmap.osm.data.coordinates.LatLon;

/**
 * Implement Mercator Projection code, coded after documentation
 * from wikipedia.
 *
 * The center of the mercator projection is always the 0 grad
 * coordinate.
 *
 * @author imi
 */
public class Mercator implements Projection {

    /**
     * 1/4 of Pi.
     */
    private static final double QUARTERPI = Math.PI / (2 + 2);

    /**
     * 180°.
     */
    private static final int C_180 = 180;

    /**
     * 360°.
     */
    private static final int C_360 = 360;

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
        return new EastNorth(
                lon * Math.PI / C_180,
                Math.log(Math.tan(QUARTERPI
                        + lat * Math.PI / C_360
                )));
    }

    /**
     * ${@inheritDoc}.
     */
    public LatLon eastNorth2latlon(final EastNorth p) {
        return new LatLon(
                Math.atan(Math.sinh(p.north())) * C_180 / Math.PI,
                p.east() * C_180 / Math.PI);
    }


    /**
     * @return "Mercator".
     */
    @Override public String toString() {
        return "Mercator";
    }

    /**
     * @return "mercator".
     */
    public String getCacheDirectoryName() {
        return "mercator";
    }

    /**
     * @return 1/(pi/2)
     */
    public double scaleFactor() {
        return 1 / Math.PI / 2;
    }

    /**
     * ${@inheritDoc}.
     */
    public EastNorth latlon2eastNorth(final LatLon pLatLon) {
        return latlon2eastNorth(pLatLon.lat(), pLatLon.lon());
    }
}
