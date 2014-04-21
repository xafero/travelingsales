//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.osm.data.coordinates;


/**
 * Northern, Easting of the projected coordinates.
 *
 * This class is immutable.
 *
 * @author Imi
 */
public class EastNorth extends Coordinate {

    /**
     *
     * @param east Easting of the projected coordinates.
     * @param north Northern of the projected coordinates.
     */
    public EastNorth(final double east, final double north) {
        super(east, north);
    }

    /**
     *
     * @return Easting of the projected coordinates.
     */
    public double east() {
        return super.getXCoord();
    }

    /**
     *
     * @return Northern of the projected coordinates.
     */
    public double north() {
        return super.getYCoord();
    }

    /**
     * @return A string-representation of our coordinates.
     */
    @Override
    public String toString() {
        return "EastNorth[e=" + east() + ", n=" + north() + "]";
    }
}
