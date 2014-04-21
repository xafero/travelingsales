/**
 * GpsTrack.java
 *  created: 08.01.2009
 * (c) 2009 by <a href="mailto:oleg.chubaryov@mail.ru">Oleg Chubaryov</a>
 * This file is part of osmnavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  osmnavigation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  osmnavigation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with osmnavigation.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.gps.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.travelingsalesman.gps.IGPSProvider.IExtendedGPSListener;

/**
 * This class represents a single gps-track.
 *
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
 */
public class GpsTrack implements IExtendedGPSListener, Iterable<GpsTrackpoint>, Iterator<GpsTrackpoint> {

    /**
     * array of points. always in sorted state by the timestamp.
     */
    private List<GpsTrackpoint> trackpoints = Collections.synchronizedList(new ArrayList<GpsTrackpoint>());

    /**
     * Name of the track.
     */
    private String name;

    /**
     * current position for iterating.
     */
    private int index = 0;

    /**
     * Current date and time for realtime tracking.
     */
    private Date currentTime;

    /**
     * last date and time.
     */
    private long lastDate, lastTime;

    /**
     * last altitude.
     */
    private double currentAltitude;

    /**
     * last speed.
     */
    private double currentSpeed;

    /**
     * last course.
     */
    private double currentCourse;


    /**
     * Construct empty track.
     */
    public GpsTrack() {
    }

    /**
     * @return number of trackpoint in the track
     */
    public int size() {
        return this.trackpoints.size();
    }

    /**
     * Add new point to the existing track. Can be at the middle.
     *
     * @param trackpoint
     *            new trackpoint
     */
    public void addTrackpoint(final GpsTrackpoint trackpoint) {
        // add to the end.
        if (trackpoint.getTime() != null) {
            if (this.trackpoints.isEmpty()
                    || this.trackpoints.get(this.trackpoints.size() - 1).getTime()
                    .before(trackpoint.getTime())) {
                this.trackpoints.add(trackpoint);
            } else {
                int i = 0;
                GpsTrackpoint tp = this.trackpoints.get(i);
                while (tp.getTime().before(trackpoint.getTime())
                        && this.trackpoints.size() > i) {
                    tp = this.trackpoints.get(i++);
                }
                this.trackpoints.add(i, trackpoint);
            }

        } else {
            // add to the end of list if trackpoint haven't time
            this.trackpoints.add(trackpoint);
        }
    }

    /**
     * Current track has next element.
     *
     * @return true if next trackpoint exist
     */
    public boolean hasNext() {
        return index < this.trackpoints.size();
    }

    /**
     * Get next trackpoint of this track.
     *
     * @return next trackpoint
     */
    public GpsTrackpoint next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return this.trackpoints.get(index++);
    }

    /**
     * Remove current trackpoint from track.
     */
    public void remove() {
        this.trackpoints.remove(index);
    }

    /**
     * @return an iterator over all {@link GpsTrackpoint}.
     */
    @Override
    public Iterator<GpsTrackpoint> iterator() {
        return this.trackpoints.iterator();
    }

    /**
     * @param aName the name to set
     */
    public void setName(final String aName) {
        this.name = aName;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Add trackpoint in case of realtime tracking.
     *
     * @param lat
     *            the latitude
     * @param lon
     *            the longitude
     */
    public void gpsLocationChanged(final double lat, final double lon) {
        GpsTrackpoint trackpoint = new GpsTrackpoint(new LatLon(lat, lon),
                currentTime, currentAltitude, currentSpeed, currentCourse);
        this.addTrackpoint(trackpoint);
    }

    /**
     * save date and time for realtime tracking or track loading from file.
     *
     * @param date
     *            the date
     * @param time
     *            the time
     */
    public void gpsDateTimeChanged(final long date, final long time) {
        if (date > 0) {
            lastDate = date;
        }
        if (time > 0) {
            lastTime = time;
        }
        this.currentTime = new Date(lastDate + lastTime);
    }

    /**
     * {@inheritDoc}
     */
    public void gpsAltitudeChanged(final double altitude) {
        this.currentAltitude = altitude;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsSpeedChanged(final double speed) {
        this.currentSpeed = speed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsCourseChanged(final double course) {
        this.currentCourse = course;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsLocationLost() {
        // ignored

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsLocationObtained() {
        // ignored

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsDopChanged(final double hdop, final double vdop, final double pdop) {
        // ignored

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsFixQualityChanged(final int fixQuality) {
        // ignored

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsUsedSattelitesChanged(final int satellites) {
        // ignored

    }
}
