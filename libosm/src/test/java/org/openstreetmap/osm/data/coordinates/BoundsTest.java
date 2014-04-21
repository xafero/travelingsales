/**
 * This file is part of LibOSM by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  LibOSM is free software: you can redistribute it and/or modify
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
 *  along with LibOSM.  If not, see <http://www.gnu.org/licenses/>.
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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * BoundsTest.java<br/>
 * created: 08.04.2008 14:31:55 <br/>
 *<br/><br/>
 * Unit-Test for the bounds-class.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class BoundsTest extends TestCase {

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(
            BoundsTest.class.getName());

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
    public String toString() {
        return "BoundsTest@" + hashCode();
    }

    /**
    * Test the constructor.
    * Bounds(final LatLon min, final LatLon max)
    */
    @Test
    public void testBoundsLatLonLatLon() {
        LatLon min = new LatLon(1.0, 0.0);
        LatLon max = new LatLon(2.0, 2.0);
        Bounds subject = new Bounds(min.lat(), max.lon(), max.lat(), min.lon());
        assertEquals(subject.getMin(), min);
        assertEquals(subject.getMax(), max);
    }

    /**
     * Test the constructor.
     * Bounds(final double lat0, final double lon0, final double lat1, final double lon1)
     */
    @Test
    public void testBoundsDoubleDoubleDoubleDouble() {
        LatLon min = new LatLon(1.0, 0.0);
        LatLon max = new LatLon(2.0, 2.0);
        Bounds subject = new Bounds(min, max);
        assertEquals(subject.getMin(), min);
        assertEquals(subject.getMax(), max);
    }

    /**
     *  Test the constructor.
     * Bounds()
     */
    @Test
    public void testBounds() {
        Bounds subject = new Bounds();
        assertNotNull(subject); //just test for the contructor not to throw ex.
        assertNotNull(subject.getMin());
        assertNotNull(subject.getMax());
    }

    /**
     *  Test the constructor.
     * Bounds(final LatLon pCenter, final double pRadius)
     * and
     * LatLon center() / LatLon getCenter()
     * and
     * boolean contains(final double aLatitude, final double aLongitude)
     */
    @Test
    public void testBoundsLatLonDouble() {
        LatLon center = new LatLon(1.0, 0.0);
        double radius = 1.0;
        Bounds subject = new Bounds(center, radius);
        assertEquals(subject.getMin(),
                  new LatLon(center.lat() - radius, center.lon() - radius));
        assertEquals(subject.getMax(),
                  new LatLon(center.lat() + radius, center.lon() + radius));
        assertEquals(center, subject.getCenter());
        assertEquals(center, subject.center());
        assertTrue(subject.contains(center.lat(), center.lon()));
    }

    /**
    *
    */
    @Test
    public void testToString() {
        Bounds subject = new Bounds();
//      just test for the contructor not to throw ex.
        assertNotNull(subject.toString());
    }
}


