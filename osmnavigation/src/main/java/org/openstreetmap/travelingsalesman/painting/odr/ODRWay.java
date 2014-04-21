/**
 * ODRWay.java created: 09.12.2007 07:23:00 This file is part of osmnavigation
 * by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 *
 * osmnavigation is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * osmnavigation is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * osmnavigation. If not, see <http://www.gnu.org/licenses/>.
 *
 * ********************************** Editing this file: -For consistent
 * code-quality this file should be checked with the checkstyle-ruleset enclosed
 * in this project. -After the design of this file has settled it should get
 * it's own JUnit-Test that shall be executed regularly. It is best to write the
 * test-case BEFORE writing this class and to run it on every build as a
 * regression-test.
 */
package org.openstreetmap.travelingsalesman.painting.odr;

import java.awt.Point;
import java.awt.Shape;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.travelingsalesman.routing.NameHelper;

import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * This is the base class for ways. inheriting classes should set their
 * geometrical representation and use the given Graphics to paint
 * themselves on it.<br/>
 * The usual implementations are {@link PathWay} for polylines
 * and {@link PolygonWay} for area-polygons.
 *
 * @author <a href="mailto:tdad@users.sourceforge.net">Sven Koehler</a>
 */
public abstract class ODRWay {

    /**
     * @param aWay
     *            the way we are painting.
     * @param aWayType
     *            the type of this way.
     */
    public ODRWay(final Way aWay, final ODR_WAY_TYPE aWayType) {
        myWay = aWay;
        myWayType = aWayType;
        if (aWay == null) {
            throw new IllegalArgumentException("null way given");
        }
        setName(NameHelper.getNameForWay(aWay));
    }

    /**
     *  @return the shape this way is representing
     */
    public abstract Shape getWayShape();

    /**
     * adds a single waypoint.
     *
     * @param wayPoint way point to add
     */
    public void addWayPoint(final Point wayPoint) {
        myWayPoints.add(wayPoint);
    }

    /**
     * @param aName
     *            the name to set
     */
    public void setName(final String aName) {
        myName = aName;
    }

    /**
     * determines the name of a way.
     *
     * @return name of the way. if the way has no name tag, null is returned.
     */
    public String getName() {
        if (myName == null) {
            myName = NameHelper.getNameForWay(getWay());
        }

        return myName;
    }

    /**
     * @return the wayType
     */
    public ODR_WAY_TYPE getWayType() {
        return myWayType;
    }

    /**
     *  @return my way points.
     */
    public List<Point> getWayPoints() {
        return myWayPoints;
    }

    /**
     * @return the oneway
     */
    public boolean isOneway() {
        return isOneway;
    }

    /**
     * @param myOneway the oneway to set
     */
    public void setOneway(final boolean myOneway) {
        this.isOneway = myOneway;
    }

    /**
     * @return the bridge?
     */
    public boolean isBridge() {
        return isBridge;
    }

    /**
     * @param myBridge the bridge to set
     */
    public void setBridge(final boolean myBridge) {
        this.isBridge = myBridge;
    }

    /**
     * @return the way
     */
    public Way getWay() {
        return myWay;
    }

    /**
     * the name of this way (if it has one).
     */
    private String myName = null;
    /**
     * is oneway road.
     */
    private boolean isOneway = false;
        /**
     * is bridge?.
     */
    private boolean isBridge = false;
    /**
     * the way itself.
     */
    private final Way myWay;

    /**
     * The points of the road-curve.
     */
    private final List<Point> myWayPoints = new LinkedList<Point>();
    /**
     * my way type.
     */
    private final ODR_WAY_TYPE myWayType;
}
