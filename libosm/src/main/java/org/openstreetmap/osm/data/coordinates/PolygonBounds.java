/**
 * PolygonBounds.java
 * created: 18.11.2007 21:26:33
 * (c) 2007 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of libosm by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  libosm is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  libosm is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with libosm.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.osm.data.coordinates;

//automatically created logger for debug and error -output
import java.util.logging.Logger;

//automatically created propertyChangeListener-Support
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;


/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * PolygonBounds.java<br/>
 * created: 18.11.2007 21:26:33 <br/>
 *<br/><br/>
 * These special bounds are denoted by a polygon instead of a simple bounding-box.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class PolygonBounds extends Bounds {

    /**
     * Automatically created logger for debug and error-output.
     */
    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(PolygonBounds.class
            .getName());

    /**
     * We use this path for inclusion-tests.
     */
    private GeneralPath myPolygonPath = new GeneralPath(Path2D.WIND_EVEN_ODD);

    /**
     * Add a point to this polygon.
     * @param point the point to add.
     */
    public void addPoint(final LatLon point) {
        addPoint(point.lat(), point.lon());
    }
    /**
     * Add a point to this polygon.
     * @param lat the latitude
     * @param lon the longitude
     */
    public void addPoint(final double lat, final double lon) {
        if (myPolygonPath.getCurrentPoint() ==  null) {
            myPolygonPath.moveTo(lat, lon);
        } else {
            myPolygonPath.lineTo(lat, lon);
        }
    }

    /**
     * @param aLatitude the latitude to test.
     * @param aLongitude the longitude to test.
     * @return true if the given coordinates are within this polygon
     */
    @Override
    public boolean contains(final double aLatitude, final double aLongitude) {
        myPolygonPath.closePath();
        return myPolygonPath.contains(aLatitude, aLongitude);
    }

    /**
     * @return the maximum lat and lon -coordinates
     */
    @Override
    public LatLon getMax() {
        myPolygonPath.closePath();
        Rectangle2D bounds2D = myPolygonPath.getBounds2D();
        return new LatLon(bounds2D.getMaxX(), bounds2D.getMaxY());
    }

    /**
     * @return the minimum lat and lon -coordinates
     */
    @Override
    public LatLon getMin() {
        myPolygonPath.closePath();
        Rectangle2D bounds2D = myPolygonPath.getBounds2D();
        return new LatLon(bounds2D.getMinX(), bounds2D.getMinY());
    }

    /**
     * @return the center of these bounds.
     */
    @Override
    public LatLon getCenter() {
        myPolygonPath.closePath();
        Rectangle2D bounds2D = myPolygonPath.getBounds2D();
        return new LatLon(bounds2D.getCenterX(), bounds2D.getCenterY());
    }

    /**
     * @return the center of these bounds.
     */
    @Override
    public LatLon center() {
        return getCenter();
    }

    /**
     * @return the maximum of the coordinate-distances of the max and min lat and lon-values.
     */
    @Override
    public double getSize() {
        myPolygonPath.closePath();
        Rectangle2D bounds2D = myPolygonPath.getBounds2D();
        return Math.max(bounds2D.getWidth(), bounds2D.getHeight());
    }


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
    public final void addPropertyChangeListener(
                                                final String propertyName,
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
    public final void removePropertyChangeListener(
                                                   final String propertyName,
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
    public String toString() {
        return "PolygonBounds@" + hashCode();
    }
}


