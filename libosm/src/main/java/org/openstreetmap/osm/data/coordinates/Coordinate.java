//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.osm.data.coordinates;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;




/**
 * Base class of points of both coordinate system.
 *
 * The variables are default package protected to allow routines in the data package
 * to access them directly.
 *
 * As the class itself is package protected too, it is not visible outside of the data
 * package. Routines there should only use LatLon or EastNorth
 *
 * @author imi
 */
public abstract class Coordinate {

    /**
     * used by {@link #hashCode()}}.
     */
    private static final int HASHY = 4096;

    /**
     * used by {@link #hashCode()}}.
     */
    private static final int HASHX = 65536;

    /**
     * Either easting or latitude.
     */
    private double myXCoord;

    /**
     * Either northing or longitude.
     */
    private double myYCoord;

    /**
     * Used ONLY for deserialization.
     */
    @Deprecated
    public Coordinate() {
        this.myXCoord = 0;
        this.myYCoord = 0;
    }

    /**
     * Construct the point with latitude / longitude values.
     * The x/y values are left uninitialized.
     *
     * @param px Latitude of the point.
     * @param py Longitude of the point.
     */
    Coordinate(final double px, final double py) {
        this.myXCoord = px;
        this.myYCoord = py;
    }

    /**
     * Return the squared distance of the northing/easting values between
     * this and the argument.
     *
     * @param other The other point to calculate the distance to.
     * @return The square of the distance between this and the other point,
     *         regarding to the x/y values.
     */
    public double distance(final Coordinate other) {
        return (myXCoord - other.myXCoord) * (myXCoord - other.myXCoord)
             + (myYCoord - other.myYCoord) * (myYCoord - other.myYCoord);
    }


    /**
     * Return the squared distance of the northing/easting values between
     * this and the argument.
     *
     * @param latA This point to calculate the distance from.
     * @param lonA This point to calculate the distance from.
     * @param latB The other point to calculate the distance to.
     * @param lonB The other point to calculate the distance to.
     * @return The square of the distance between this and the other point,
     *         regarding to the x/y values.
     */
    public static double distance(final double latA, final double lonA,
                                  final double latB, final double lonB) {
        return (latA - latB) * (latA - latB)
             + (lonA - lonB) * (lonA - lonB);
    }

    /**
     * Return the squared distance of the northing/easting values between
     * this and the argument.
     *
     * @param a This point to calculate the distance from.
     * @param b The other point to calculate the distance to.
     * @return The square of the distance between this and the other point,
     *         regarding to the x/y values.
     */
    public static double distance(final Node a, final Node b) {
        return distance(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
    }

    /**
     * Return the squared distance of the northing/easting values between
     * this and the argument.
     *
     * @param a This point to calculate the distance from.
     * @param b The other point to calculate the distance to.
     * @return The square of the distance between this and the other point,
     *         regarding to the x/y values.
     */
    public static double distance(final Node a, final Coordinate b) {
        return distance(a.getLatitude(), a.getLongitude(), b.getXCoord(), b.getYCoord());
    }

    /**
     * @return true if obj i a coordinate of the same position.
     * @param obj the coordinate to check against
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof Coordinate)) {
            return false;
        }
        return (myXCoord == ((Coordinate) obj).myXCoord
                &&
                myYCoord == ((Coordinate) obj).myYCoord);
    }

    /**
     * calculate the hash-code.
     * @return the hash-code.
     */
    @Override
    public int hashCode() {
        return (int) (myXCoord * HASHX
                   +  myYCoord * HASHY);
    }

    /**
     * @return Either easting or latitude
     */
    public double getXCoord() {
        return this.myXCoord;
    }

    /**
     * @return Either northing or longitude
     */
    public double getYCoord() {
        return this.myYCoord;
    }

    /**
     * ONLY TO BE USED IN DESERIALISATION.
     * @param aCoord the xCoord to set
     */
    protected void setXCoord(final double aCoord) {
        myXCoord = aCoord;
    }

    /**
     * ONLY TO BE USED IN DESERIALISATION.
     * @param aCoord the yCoord to set
     */
    protected void setYCoord(final double aCoord) {
        myYCoord = aCoord;
    }
}
