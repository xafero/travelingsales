/**
 * 
 */
package org.openstreetmap.travelingsalesman.gui.widgets;

import java.awt.Color;
import java.awt.GridLayout;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.UnitConstants;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.travelingsalesman.gps.IGPSProvider.IExtendedGPSListener;
import org.openstreetmap.travelingsalesman.routing.Route;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.routing.describers.SimpleRouteDescriber;
import org.openstreetmap.travelingsalesman.routing.metrics.IRoutingMetric;
import org.openstreetmap.travelingsalesman.routing.metrics.ITimeRoutingMetric;
import org.openstreetmap.travelingsalesman.routing.metrics.StaticFastestRouteMetric;

/**
 * This panel displays the ETA and other data derived from metrics.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class MetricsPanel extends JPanel implements IExtendedGPSListener {

    /**
     * Start-value for {@link #myCurrentSpeed}.
     */
    private static final int DEFAULTCURRENTSPEED = 100;

    /**
     * generated.
     */
    private static final long serialVersionUID = 332541853587054637L;

    /**
     * The label that shows the distance to the next waypoint.
     */
    private JLabel myDistanceToNextWaypointLabel = new JLabel("-- Km");
    /**
     * The label that shows the time left to the next waypoint.
     */
    private JLabel myTimeToNextWaypointLabel = new JLabel("-- min");
    /**
     * Location of the next driving-action.
     */
    private LatLon myNextIntersectionLocation;
    /**
     * My last known Latitude.
     */
    private double myLastLon;
    /**
     * My last known Longitude.
     */
    private double myLastLat;
    /**
     * The step in {@link #myRoute} we are currently driving.
     */
    private RoutingStep myCurrentRoutingStep;
    /**
     * The metric we use to determine travel-times.
     */
    private ITimeRoutingMetric myMetric = new StaticFastestRouteMetric();
    /**
     * The route we are currently driving.
     */
    private Route myRoute;
    /**
     * The label showing the distance to the next destination (not the next driving instruction).
     */
    private JLabel myDistanceToDestinationLabel = new JLabel("--- Km");
    /**
     * The label showing the time when we will arrive.
     */
    private JLabel myEtaLabel = new JLabel("--:--");
    /**
     * The label showing the time to the next destination (not the next driving instruction).
     */
    private JLabel myTimeToDestinationLabel = new JLabel("--:--");
    /**
     * Current speed in Kilometers per hour.
     */
    private double myCurrentSpeed = DEFAULTCURRENTSPEED;

    /**
     * Initialize the panel.
     */
    public MetricsPanel() {
        initUI();
        // if it is compatible, use the metric preferred by the user
        IRoutingMetric metric = Settings.getInstance().getPlugin(IRoutingMetric.class, StaticFastestRouteMetric.class.getName());
        if (metric instanceof ITimeRoutingMetric) {
            this.myMetric = (ITimeRoutingMetric) metric;
        }
    }

    /**
     * Create the UI-elements.
     */
    private void initUI() {
        setLayout(new GridLayout(0, 2));

        add(new JLabel("ETA"));
        add(myEtaLabel);

        add(new JLabel("Km left"));
        add(myDistanceToDestinationLabel);

        add(new JLabel("Time left"));
        add(myTimeToDestinationLabel);

        add(new JLabel("next waypoint"));
        add(myTimeToNextWaypointLabel);

        add(new JLabel("next waypoint"));
        add(myDistanceToNextWaypointLabel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsLocationChanged(final double aLat, final double aLon) {
        if (aLat == 0 && aLon == 0) {
            return;
        }
        this.myLastLat = aLat;
        this.myLastLon = aLon;
        // TODO we should use the distance on the road instead of a straight line
        if (this.myNextIntersectionLocation == null) {
            return;
        }
        LatLon next = this.myNextIntersectionLocation;
        LatLon cur = new LatLon(aLat, aLon);
        final int kilo = 1000;
        double distance = (double) (LatLon.distanceInMeters(cur, next)) / kilo;
        long timeInMilliseconds = getStepTime(distance, myCurrentRoutingStep);
        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        DateFormat   timeFormat   = DateFormat.getTimeInstance(DateFormat.SHORT);
        if (distance < 1) {
            this.myDistanceToNextWaypointLabel.setText(numberFormat.format(distance * kilo) + "m");
        } else {
            this.myDistanceToNextWaypointLabel.setText(numberFormat.format(distance) + "Km");
        }
        this.myTimeToNextWaypointLabel.setText(formatTime(timeInMilliseconds, timeFormat));
        List<RoutingStep> routingSteps = this.myRoute.getRoutingSteps();
        boolean started = false;
        for (RoutingStep routingStep : routingSteps) {
            if (started) {
                //TODO: cache these
//                double stepDistance = getDistanceInMeters(routingStep) / kilo;
                double stepDistance = routingStep.distanceInMeters() / kilo;
                distance += stepDistance;
                timeInMilliseconds += getStepTime(stepDistance, routingStep);
            }
            if (routingStep == this.myCurrentRoutingStep) {
                started = true; // ignore routing-steps before the current one
                // and ignore the current step as it is already summed
            }
        }
        if (distance < 1) {
            this.myDistanceToDestinationLabel.setText(numberFormat.format(distance * kilo) + "m");
        } else {
            this.myDistanceToDestinationLabel.setText(numberFormat.format(distance) + "Km");
        }
        this.myTimeToDestinationLabel.setText(formatTime(timeInMilliseconds, timeFormat));
        this.myEtaLabel.setText(timeFormat.format(new Date(timeInMilliseconds + System.currentTimeMillis())));
    }

    /**
     * @param aStepDistance the length of the routing-step in meters
     * @param aRoutingStep the routing-step this is about
     * @return the time in millisecons to drive that distance
     */
    private long getStepTime(final double aStepDistance, final RoutingStep aRoutingStep) {
        final long millisecondsPerHour =  UnitConstants.MINUTESPERHOUR
                                         * UnitConstants.SECONDSPERMINUTE
                                         * UnitConstants.MILLI;
        final int maxSensibleSpeed = 300;
        double speed = this.myCurrentSpeed; //  speed in kilometers per hour
        if (speed > maxSensibleSpeed || aRoutingStep != this.myCurrentRoutingStep) {
            speed = this.myMetric.getEstimatedSpeed(aRoutingStep);
        }
//        System.out.println(aStepDistance + "km / " + speed + "km/h="
//                + (aStepDistance / speed) + "h ="
//                + ((long) ((aStepDistance * millisecondsPerHour) / speed)) + "ms");
        return (long) ((aStepDistance * millisecondsPerHour) / speed);
    }

    /**
     * @param timeInMilliseconds the time to format
     * @param timeFormat the default TimeFormat to use for longer times
     * @return the time-length formated for the current locale
     */
    private String formatTime(final long timeInMilliseconds, final DateFormat timeFormat) {
        final int maxSeconds = 90;
        final int maxMinutes = 90;
        final int maxHours = 8;
        final int maxHours2 = 20;
        long inSeconds = timeInMilliseconds / UnitConstants.MILLI;
        if (inSeconds < maxSeconds) {
            if (inSeconds == 1) {
                return "1 " + SimpleRouteDescriber.RESOURCE.getString("time/second/singular");
            }
            return inSeconds + " " + SimpleRouteDescriber.RESOURCE.getString("time/second/plural");
        }
        long inMinutes = inSeconds / UnitConstants.SECONDSPERMINUTE;
        if (inMinutes < maxMinutes) {
            return inMinutes + " "
            + SimpleRouteDescriber.RESOURCE.getString("time/minute/short");
        }
        int inHours =  (int) inMinutes / UnitConstants.MINUTESPERHOUR;
        if (inHours < maxHours) {
            long minutes = inMinutes - (UnitConstants.MINUTESPERHOUR * inHours);
            String minutesStr = " " + minutes + " ";
            if (minutes == 0) {
                minutesStr = "";
            } else {
                minutesStr += SimpleRouteDescriber.RESOURCE.getString("time/minute/short");
            }
            return inHours + SimpleRouteDescriber.RESOURCE.getString("time/hour/short")
                     + " " + minutesStr;
        }
        if (inHours < maxHours2) {
            return inHours + SimpleRouteDescriber.RESOURCE.getString("time/hour/short");
        }
        return timeFormat.format(new Date(timeInMilliseconds));
    }

    /**
     * Sum the distance to drive of a routing-step.
     * @param aRoutingStep the step to analyse
     * @return the length of it
     */
    private double getDistanceInMeters(final RoutingStep aRoutingStep) {
        List<WayNode> nodes = aRoutingStep.getNodes();
        LatLon lastNode = null;
        double distance = 0;
        IDataSet map = this.myRoute.getMap();
        for (WayNode wayNode : nodes) {
            if (lastNode == null) {
                Node node = map.getNodeByID(wayNode.getNodeId());
                if (node != null) {
                    lastNode = new LatLon(node.getLatitude(), node.getLongitude());
                }
                continue;
            }

            Node node = map.getNodeByID(wayNode.getNodeId());
            if (node != null) {
                LatLon cur = new LatLon(node.getLatitude(), node.getLongitude());
                distance += LatLon.distanceInMeters(lastNode, cur);
                lastNode = cur;
            }
        }
        return distance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsLocationLost() {
        this.myLastLat = 0;
        this.myLastLon = 0;
        this.myDistanceToNextWaypointLabel.setText("no GPS");//TODO: l10n
        this.myTimeToNextWaypointLabel.setText("no GPS");
        this.myDistanceToDestinationLabel.setText("no GPS");
        this.myTimeToDestinationLabel.setText("no GPS");
        this.myEtaLabel.setText("no GPS");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsLocationObtained() {
        // ignored
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public void gpsAltitudeChanged(final double aAltitude) {
        // ignored
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public void gpsDateTimeChanged(final long aDate, final long aTime) {
        // ignored
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public void gpsDopChanged(final double aHdop, final double aVdop, final double aPdop) {
        // ignored
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public void gpsFixQualityChanged(final int aFixQuality) {
        // ignored
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public void gpsSpeedChanged(final double aSpeed) {
        this.myCurrentSpeed = aSpeed * UnitConstants.MILES_TO_KM;
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public void gpsUsedSattelitesChanged(final int aSatellites) {
        // ignored
    }

    /**
     * @return the nextIntersectionLocation
     */
    public LatLon getNextIntersectionLocation() {
        return myNextIntersectionLocation;
    }

    /**
     * @param aNextIntersectionLocation the nextIntersectionLocation to set
     * @param aRoute the route we are traveling
     * @param aCurrentStep the current routing step
     */
    public void setNextIntersectionLocation(final LatLon aNextIntersectionLocation, final RoutingStep aCurrentStep, final Route aRoute) {
        this.myNextIntersectionLocation = aNextIntersectionLocation;
        this.myCurrentRoutingStep = aCurrentStep;
        this.myRoute = aRoute;
        if (aRoute != null) {
            this.myMetric.setMap(aRoute.getMap());
        }
        this.gpsLocationChanged(myLastLat, myLastLon);
    }
    /**
     * ${@inheritDoc}.
     */
    @Override
    public void setBackground(final Color aBg) {
        // work around a bug with transparency in swing
        if (this.myDistanceToNextWaypointLabel != null) {
            this.myDistanceToNextWaypointLabel.setBackground(aBg);
        }
        if (this.myTimeToNextWaypointLabel != null) {
            this.myTimeToNextWaypointLabel.setBackground(aBg);
        }
        super.setBackground(aBg);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void gpsCourseChanged(final double aCourse) {
        // ignored
    }


}
