/**
 * This file is part of Traveling-Salesman by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  Traveling-Salesman is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Traveling-Salesman is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Traveling-Salesman.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.gui.widgets;

import java.awt.Graphics2D;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.travelingsalesman.gps.IGPSProvider.IExtendedGPSListener;
import org.openstreetmap.travelingsalesman.gps.data.GpsTrack;
import org.openstreetmap.travelingsalesman.gps.data.GpsTracksStorage;
import org.openstreetmap.travelingsalesman.gps.data.ITracksChangeListener;
import org.openstreetmap.travelingsalesman.gps.data.TrackEvent;
import org.openstreetmap.travelingsalesman.routing.IRouteChangedListener;
import org.openstreetmap.travelingsalesman.routing.Route;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.trafficblocks.TrafficMessage;
import org.openstreetmap.travelingsalesman.trafficblocks.TrafficMessageStore;

/**
 * The MapPanel exists to render a rectangular area of the DataSet.
 * 
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class MapPanel extends SimpleMapPanel implements IRouteChangedListener,
		IExtendedGPSListener, ITracksChangeListener {
	/**
	 * generated.
	 */
	private static final long serialVersionUID = -3985929675757045218L;

	/**
	 * my logger for debug and error-output.
	 */
	private static final Logger LOG = Logger
			.getLogger(MapPanel.class.getName());

	/**
	 * The track that shall be marked at the map.
	 */
	private GpsTracksStorage myTracksStorage = null;

	/**
	 * The route that shall be marked in the map. (May be null.)
	 */
	private Route myCurrentRoute = null; // @jve:decl-index=0:

	/**
	 * The current user-position to mark. (May be null.)
	 */
	private LatLon myCurrentPosition = null; // @jve:decl-index=0:

	/**
	 * The course (heading) of user to show.
	 */
	private double myCurrentCourse = 0;

	/**
	 * The speed (velocity) of user/car to show.
	 */
	private double myCurrentSpeed = 0;

	/**
	 * The next position the user is supposed to act to mark. (May be null.)
	 */
	private LatLon myNextManeuverPosition = null; // @jve:decl-index=0:

	/**
	 * The selected node coords. (May be null.)
	 */
	private LatLon mySelectedNodePosition = null;

	/**
	 * This is the default constructor.
	 */
	public MapPanel() {
		super();
		initialize();
	}

	/**
	 * Paint all the vector-data that we draw on top of the actual map like the
	 * current location, the route,... .
	 * 
	 * @param g2d
	 *            what to paint to
	 */
	protected void paintAdditionalVectorData(final Graphics2D g2d) {
		if (getCurrentRoute() != null) {
			this.getMyPainter().setGraphics(g2d);
			List<RoutingStep> routeSegments = getCurrentRoute()
					.getRoutingSteps();
			LOG.log(Level.FINEST, "paint() visiting " + routeSegments.size()
					+ " route-segments");
			this.getMyPainter().visitRoute(getDataSet(), routeSegments);
		} else {
			LOG.log(Level.FINEST,
					"paint() visiting no route-segments (because we have no route)");
		}

		if (getCurrentPosition() != null) {
			this.getMyPainter().setGraphics(g2d);
			// if (autoRotationMode) {
			// this.getMyPainter().visitCurrentPosition(getCurrentPosition(), 0,
			// getCurrentSpeed());
			// } else {
			this.getMyPainter().visitCurrentPosition(getCurrentPosition(),
					getCurrentCourse(), getCurrentSpeed());
			// }
		}

		if (this.getNextManeuverPosition() != null) {
			this.getMyPainter().setGraphics(g2d);
			this.getMyPainter().visitNextManeuverPosition(
					this.getNextManeuverPosition());
		}

		if (myTracksStorage != null && myTracksStorage.size() > 0) {
			this.getMyPainter().setGraphics(g2d);
			this.getMyPainter().visitGpsTracks(myTracksStorage);
		}
		if (mySelectedNodePosition != null) {
			this.getMyPainter().setGraphics(g2d);
			this.getMyPainter().visitSelectedNode(mySelectedNodePosition);
		}

		Collection<TrafficMessage> allMessages = TrafficMessageStore
				.getInstance().getAllMessages(getDataSet());
		for (TrafficMessage trafficMessage : allMessages) {
			this.getMyPainter().setGraphics(g2d);
			this.getMyPainter().visitTrafficMessage(trafficMessage);
		}
	}

	/**
	 * Set my gps tracks storage.
	 * 
	 * @param aStorage
	 *            the storage.
	 */
	public void setGpsTracksStorage(final GpsTracksStorage aStorage) {
		myTracksStorage = aStorage;
	}

	/**
	 * Set node for highlight.
	 * 
	 * @param aSelectedNodePosition
	 *            node postion
	 */
	public void setSelectedNodePosition(final LatLon aSelectedNodePosition) {
		this.mySelectedNodePosition = aSelectedNodePosition;
	}

	/**
	 * @return the route we shall highlight in the map.
	 */
	public Route getCurrentRoute() {
		return myCurrentRoute;
	}

	/**
	 * @param aCurrentRoute
	 *            the route we shall highlight in the map.
	 */
	public void setCurrentRoute(final Route aCurrentRoute) {
		myCurrentRoute = aCurrentRoute;
		repaint();
	}

	/**
	 * The route to travel has changed.
	 * 
	 * @param newRoute
	 *            the new route or null if there is no route anymore.
	 */
	public void routeChanged(final Route newRoute) {
		setCurrentRoute(newRoute);
	}

	/**
	 * The current user-position to mark.
	 * 
	 * @return the currentPosition
	 */
	public LatLon getCurrentPosition() {
		return myCurrentPosition;
	}

	/**
	 * The current course. Uses for auto-rotation
	 * 
	 * @return the current course in degrees
	 */
	public double getCurrentCourse() {
		return myCurrentCourse;
	}

	/**
	 * The current user-position to mark.
	 * 
	 * @return the currentPosition
	 */
	public double getCurrentSpeed() {
		return myCurrentSpeed;
	}

	/**
	 * The current user-position to mark.
	 * 
	 * @param aCurrentPosition
	 *            the currentPosition to set
	 */
	public void setCurrentPosition(final LatLon aCurrentPosition) {
		if (aCurrentPosition == null) {
			throw new IllegalArgumentException("null current location given");
		}
		LatLon oldPos = myCurrentPosition;
		myCurrentPosition = aCurrentPosition;

		if (isAutoRotationMode() || isAutoCenterMode()) {
			zoomTo(getProjection().latlon2eastNorth(aCurrentPosition),
					getScale());
		} else {
			// repaint only of it was or now is in the bounding-box displayed
			Bounds mapBounds = getMapBounds();
			if (oldPos != null
					&& (mapBounds.contains(oldPos.lat(), oldPos.lon()) || mapBounds
							.contains(aCurrentPosition.lat(),
									aCurrentPosition.lon()))) {
				repaint();
			}
		}
	}

	/**
	 * @return the nextManeuverPosition
	 */
	public LatLon getNextManeuverPosition() {
		return myNextManeuverPosition;
	}

	/**
	 * @param aNextManeuverPosition
	 *            the nextManeuverPosition to set
	 */
	public void setNextManeuverPosition(final LatLon aNextManeuverPosition) {
		myNextManeuverPosition = aNextManeuverPosition;
		repaint();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener#gpsLocationChanged(double,
	 *      double)
	 */
	public void gpsLocationChanged(final double lat, final double lon) {
		setCurrentPosition(new LatLon(lat, lon));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider.IExtendedGPSListener#gpsSpeedChanged(double)
	 */
	public void gpsSpeedChanged(final double speed) {
		this.myCurrentSpeed = speed;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener#gpsCourseChanged(double)
	 */
	public void gpsCourseChanged(final double course) {
		this.myCurrentCourse = course;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener#gpsLocationLost()
	 */
	public void gpsLocationLost() {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener#gpsLocationObtained()
	 */
	public void gpsLocationObtained() {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider.IExtendedGPSListener#gpsAltitudeChanged(double)
	 */
	public void gpsAltitudeChanged(final double altitude) {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider.IExtendedGPSListener#gpsDateTimeChanged(long,
	 *      long)
	 */
	public void gpsDateTimeChanged(final long date, final long time) {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider.IExtendedGPSListener#gpsDopChanged(double,
	 *      double, double)
	 */
	public void gpsDopChanged(final double hdop, final double vdop,
			final double pdop) {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider.IExtendedGPSListener#gpsFixQualityChanged(int)
	 */
	public void gpsFixQualityChanged(final int fixQuality) {
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider.IExtendedGPSListener#gpsUsedSattelitesChanged(int)
	 */
	public void gpsUsedSattelitesChanged(final int satellites) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateTrack(final TrackEvent action, final GpsTrack track) {
		this.repaint();
	}

	/**
	 * ${@inheritDoc}.
	 */
	@Override
	public void noRouteFound() {
		routeChanged(null);
	}

}
