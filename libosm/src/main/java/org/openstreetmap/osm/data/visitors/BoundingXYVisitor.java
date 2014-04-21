package org.openstreetmap.osm.data.visitors;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.EastNorth;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * Calculates the total bounding rectangle of a serie of OsmPrimitives, using the
 * EastNorth values as reference.
 * @author imi
 */
public class BoundingXYVisitor implements Visitor {

    /**
     * The max and min up to now.
     */
    private EastNorth min;

    /**
     * The max and min up to now.
     */
    private EastNorth max;

    /**
     * our map.
     */
    private IDataSet myMap;

    /**
     * @param aMap our map
     */
    public BoundingXYVisitor(final IDataSet aMap) {
        super();
        myMap = aMap;
    }

    /**
     * @param n The Node we visit.
     */
    public void visit(final Node n) {
        EastNorth en = Settings.getProjection().latlon2eastNorth(n.getLatitude(), n.getLongitude());
        visit(en);
    }

    /**
     * @param r ignored
     */
    public void visit(final Relation r) {
        // ignored
    }


    /**
     * @param w ignored
     */
    public void visit(final Way w) {
        // ignored
    }

    /**
     * @param eastNorth a coordinate to compare to
     * {@link #min}} and {@link #max}}.
     */
    public void visit(final EastNorth eastNorth) {
        if (eastNorth != null) {
            if (min == null) {
                min = eastNorth;
            } else if (eastNorth.east() < min.east()) {
                min = new EastNorth(eastNorth.east(), min.north());
            }

            if (min == null) {
                min = eastNorth;
            } else if (eastNorth.north() < min.north()) {
                min = new EastNorth(min.east(), eastNorth.north());
            }

            if (max == null) {
                max = eastNorth;
            } else if (eastNorth.east() > max.east()) {
                max = new EastNorth(eastNorth.east(), max.north());
            }

            if (max == null) {
                max = eastNorth;
            } else if (eastNorth.north() > max.north()) {
                max = new EastNorth(max.east(), eastNorth.north());
            }
        }
    }

    /**
     * @return The bounding box or <code>null</code> if no coordinates have passed
     */
    public Bounds getBounds() {
        if (min == null || max == null)
            return null;
        return new Bounds(Settings.getProjection().eastNorth2latlon(min), Settings.getProjection().eastNorth2latlon(max));
    }

    /**
     * @return the min (may be null if no node has been visited yet)
     */
    public EastNorth getMin() {
        return min;
    }

    /**
     * @param aMin the min to set
     *  (not null for not destroying the bounding-box -contract about all visited nodes)
     */
    public void setMin(final EastNorth aMin) {
        if (aMin == null)
            throw new IllegalArgumentException("null min-location given!");
        min = aMin;
    }

    /**
     * @return the map
     */
    public IDataSet getMap() {
        return myMap;
    }

    /**
     * @param aMap the map to set
     */
    public void setMap(final IDataSet aMap) {
        if (aMap == null)
            throw new IllegalArgumentException("null map given!");
        myMap = aMap;
    }

    /**
     * @return the max (may be null if no node has been visited yet)
     */
    public EastNorth getMax() {
        return max;
    }

    /**
     * @param aMax the max to set
     *  (not null for not destroying the bounding-box -contract about all visited nodes)
     */
    public void setMax(final EastNorth aMax) {
        if (aMax == null)
            throw new IllegalArgumentException("null max-location given!");
        max = aMax;
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public String toString() {
        return "[BoundingXYVisitor: " + this.min + "," + this.max + "," + myMap + "]";
    }
}
