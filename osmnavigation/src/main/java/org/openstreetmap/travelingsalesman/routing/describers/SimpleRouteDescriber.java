/**
 * This file is part of OSMNavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  OSMNavigation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  OSMNavigation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OSMNavigation.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.routing.describers;

import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.travelingsalesman.routing.NameHelper;
import org.openstreetmap.travelingsalesman.routing.Route;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.routing.selectors.UsedTags;
import org.openstreetmap.travelingsalesman.routing.speech.IVoiceSynth;
import org.openstreetmap.travelingsalesman.routing.speech.NoVoice;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * This is a tivial route-describer that does no
 * combining of segments ("follow this street for 2Km") nor
 * directions ("turn right") but simple names the segments
 * as best as it can.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class SimpleRouteDescriber implements IRouteDescriber, IPlugin {

    /**
     * Out resource-manager for i18n.
     */
    public static final ResourceBundle RESOURCE = ResourceBundle.getBundle(SimpleRouteDescriber.class.getName());

    /**
     * 180° .
     */
    private static final int HALFCIRCLE = 180;

    /**
     * Maximum degrees of a turn to make it a "slight turn".
     */
    private static final int SLIGHTTURN = 140;

    /**
     * Maximum degrees of a turn to make it a "turn".
     */
    private static final int NORMALTURN = 70;

    /**
     * Maximum degrees of a turn to make it a "hard turn".
     */
    private static final int HARDTURN = 60;

    /**
     * Maximum degrees of a turn to make it a "turn-around".
     */
    private static final int TURNAROUND = 10;

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(SimpleRouteDescriber.class.getName());

    /**
     * Our Speech-Synthesesis.
     */
    private IVoiceSynth myVoiceSynth;

    /**
     * The route we are describing.
     */
    private Route myRoute;

    /**
     * The step (in our simple case "the segment")
     * we are currently at.
     */
    private Route.RoutingStep myNextStep;

    /**
     * Has the current step been spoken via the
     * voice-synthesizer yet?
     */
    private boolean hasSpokenCurrentStep = false;
    /**
     * Distance in Km.
     * When we fal below this distance to the current routing step`s
     * location the first time, speak it and set
     * {@link #hasSpokenCurrentStep}.
     */
    private static final double SPEAKCURRENTSTEPDISTANCE = 0.2;

    /**
     * Our current location.
     */
    private Node myIntersectionNode;

    /**
     * Our current location.
     */
    private DrivingInstructionType myCurrentInstructionType;

    /**
     * The iterator giving us the current step
     * (in our simple case "the next segment").
     */
    private Iterator<Route.RoutingStep> myCurrentStepIterator;

    /**
     * @see #myNextStep
     */
    private RoutingStep myCurrentStep = null;

    /**
     */
    public SimpleRouteDescriber() {
        super();
        setRoute(null);
    }

    /**
     * This plugin has no  settings, thus this method returns null
     * as described in {@link IPlugin#getSettings()}.
     * @return null
     */
    public ConfigurationSection getSettings() {
        return null;
    }


    /**
     * @param aRoute The route we are describing.
     */
    public SimpleRouteDescriber(final Route aRoute) {
        super();
        setRoute(aRoute);
    }

    /**
     * Set the route to describe.
     * @param aRoute the new route (may be null)
     */
    public void setRoute(final Route aRoute) {
        this.myRoute = aRoute;
        if (aRoute == null) {
            this.myCurrentStepIterator = null;
            this.myIntersectionNode  = null;
        } else {
            this.myCurrentStepIterator = aRoute.getRoutingSteps().iterator();
            this.myIntersectionNode = aRoute.getStartNode();
            if (aRoute.getRoutingSteps().size() > 0) {
                this.myIntersectionNode = aRoute.getRoutingSteps().get(0).getEndNode();
            }
        }
        this.hasSpokenCurrentStep = false;
    }

    //=============================== describing

    /**
     * WARNNG: {@link #getCurrentStep()} may return another step then
     * the one given here if steps are combined because they involve
     * no acion.
     * @param aStep the step that is now nearest to the GPS-location
     * @return the instructions for the step
     */
    public String setCurrentStep(final RoutingStep aStep) {
        this.myCurrentStepIterator = myRoute.getRoutingSteps().iterator();
        String text = null;
        boolean hasSpoken = hasSpokenCurrentStep && (myCurrentStep == aStep);
        myNextStep = null;
        myCurrentStep = null;

        setRoute(getRoute());
        while (hasNextInstruction() && myCurrentStep != aStep) {
            text = getNextInstruction();
        }
        if (hasSpoken) {
            this.hasSpokenCurrentStep = hasSpoken;
        }
        return text;
    }

    /**
     * @return the driving-instruction to be read/shown to the user.
     * @see org.openstreetmap.travelingsalesman.routing.describers.IRouteDescriber#getNextInstruction()
     */
    public String getNextInstruction() {
        if (!hasNextInstruction()) {
            return null;
        }

        myCurrentStep = this.myNextStep;
        this.myNextStep = this.myCurrentStepIterator.next();
        this.hasSpokenCurrentStep = false;

        // combine routing-steps that have no possible action
        while (myCurrentStep != null && myNextStep != null
                && isSkippable(myRoute.getMap(), myCurrentStep, myNextStep)) {
            LOG.info("combining 2 routing-steps into 1 driving-instructions");
            myCurrentStep = this.myNextStep;
            this.myNextStep = null;
            if (this.myCurrentStepIterator.hasNext()) {
                this.myNextStep = this.myCurrentStepIterator.next();
            }
        }

        if (myCurrentStep == null) {
            if (myNextStep != null) {
                myCurrentStep = this.myNextStep;
                myNextStep = null;
                if (this.myCurrentStepIterator.hasNext()) {
                    this.myNextStep = this.myCurrentStepIterator.next();
                }
            } else {
                return null;
            }
        }

        this.myIntersectionNode = myCurrentStep.getEndNode();
        this.myCurrentInstructionType = DrivingInstructionType.OTHER;

        if (myNextStep != null) {
            // when to enter a roundabout?
            String currentJunctionType = WayHelper.getTag(myNextStep.getWay(), UsedTags.TAG_JUNCTION);
            if (currentJunctionType != null && currentJunctionType.equalsIgnoreCase("roundabout")) {
                Way roundabout = myNextStep.getWay();
                Node enter = myNextStep.getStartNode();
                Node exit = myNextStep.getEndNode();
                IDataSet map = myRoute.getMap();
                int exitNumber = countExits(map, roundabout, enter, exit);
                this.myCurrentInstructionType = DrivingInstructionType.ROUNDABOUT;
                String exitNumberStr = getExitNumber(exitNumber);
                return MessageFormat.format(RESOURCE.getString("enter_roundabout_EXIT"), exitNumberStr);
            }
        }
        String currentJunctionType = WayHelper.getTag(myCurrentStep.getWay(), UsedTags.TAG_JUNCTION);
        String upcommingHighwayType = "";
        if (myNextStep != null) {
            upcommingHighwayType = WayHelper.getTag(myNextStep.getWay(), Tags.TAG_HIGHWAY);
        }
        //----
        // are we exiting a roundabout?
        if (currentJunctionType != null && currentJunctionType.equalsIgnoreCase("roundabout")) {
            Way roundabout = myCurrentStep.getWay();
            Node enter = myCurrentStep.getStartNode();
            Node exit = myCurrentStep.getEndNode();
            IDataSet map = myRoute.getMap();
            int exitNumber = countExits(map, roundabout, enter, exit);
            this.myCurrentInstructionType = DrivingInstructionType.ROUNDABOUT;
            return MessageFormat.format(RESOURCE.getString("take_EXIT"), getExitNumber(exitNumber));
            //return getInstruction(this.myCurrentInstructionType, "roundabout", NameHelper.getNameForWay(myCurrentStep.getWay()));
        }
        this.myCurrentInstructionType = getDrivingInstructionType(myCurrentStep, myNextStep);
        if (myNextStep == null) {
            return getInstruction(this.myCurrentInstructionType, upcommingHighwayType, NameHelper.getNameForWay(myCurrentStep.getWay()));
        }
        return getInstruction(this.myCurrentInstructionType, upcommingHighwayType, NameHelper.getNameForWay(myNextStep.getWay()));
    }

    /**
     * Are these 2 steps just a way broken up into many ways?
     * (e.g. a long motorway?)
     * @param aMap the map of the route
     * @param aCurrentStep the step we would return
     * @param aNextStep the next step that is ahead
     * @return true if we can skip ahead to aNextStep
     */
    private boolean isSkippable(final IDataSet aMap,
            final RoutingStep aCurrentStep, final RoutingStep aNextStep) {
        try {
            Node endNode = aCurrentStep.getEndNode();
            Way way1 = aCurrentStep.getWay();
            Way way2 = aNextStep.getWay();
            String name1 = NameHelper.getNameForWay(way1);
            String name2 = NameHelper.getNameForWay(way2);
            if (name1 == null && name2 != null) {
                return false;
            }
            if (name1 != null && name2 == null) {
                return false;
            }
            if (name1 != null && name2 != null && !name1.equalsIgnoreCase(name2)) {
                return false;
            }
            Iterator<Way> waysForNode = aMap.getWaysForNode(endNode.getId());
            if (!waysForNode.hasNext()) {
                return false;
            }
            // this should be one of our ways, if it is another -> intersection
            Way way = waysForNode.next();
            if (!waysForNode.hasNext()
                    || (way.getId() != way1.getId() && way.getId() != aNextStep.getWay().getId())) {
                return false;
            }
            // this should be one of our ways, if it is another -> intersection
            way = waysForNode.next();
            if ((way.getId() != way1.getId() && way.getId() != aNextStep.getWay().getId())) {
                return false;
            }
            // if the node has > 2 ways it may be an intersection
            return !waysForNode.hasNext();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Cannot determine if we can combine 2"
                    + " routing-steps to one driving-instruction", e);
            return false;
        }
    }

    /**
     * @param aExitNumber the number/count of an exit of a roundabout or cloverleaf.
     * @return a translated string like "first exit"
     */
    private String getExitNumber(final int aExitNumber) {
        final int first = 1;
        final int second = 2;
        final int third = 3;
        switch (aExitNumber) {
        case 0      : return RESOURCE.getString("numbers/first_exit");
        case first  : return RESOURCE.getString("numbers/first_exit");
        case second : return RESOURCE.getString("numbers/second_exit");
        case third  : return RESOURCE.getString("numbers/third_exit");
        default     : return aExitNumber + "th";
        }
    }

    /**
     * Count the exits of a roundabout.
     * @param aMap the map itself
     * @param aRoundabout the roundabout itself
     * @param aEnter where we enter
     * @param aExit where we exit
     * @return the number of exits we pass
     */
    private int countExits(final IDataSet aMap, final Way aRoundabout, final Node aEnter, final Node aExit) {
        List<WayNode> wayNodes = aRoundabout.getWayNodes();
        int start = -1;
        int count = 0;
        for (int i = 0; i < wayNodes.size(); i++) {
            WayNode wayNode = wayNodes.get(i);
            if (wayNode.getNodeId() == aEnter.getId()) {
                start = i;
                break;
            }
        }
        for (int i = 0; i < wayNodes.size(); i++) {
            WayNode wayNode = wayNodes.get((i + start) % wayNodes.size());
            if (wayNode.getNodeId() == aExit.getId()) {
                return count;
            }
            // ignore wayNodes that have no way other then the roundabout
            Iterator<Way> ways = aMap.getWaysForNode(wayNode.getNodeId());
            while (ways.hasNext()) {
                Way way = (Way) ways.next();
                if (way.getId() == aRoundabout.getId()) {
                    continue;
                }
                // ignore oneways that are not exits
                if (WayHelper.isOneway(way)) {
                    String onewayTag = WayHelper.getTag(way, UsedTags.TAG_ONEWAY);
                    if (onewayTag != null && way.getWayNodes().get(0).getNodeId() == wayNode.getNodeId()) {
                        // the way starts at the roundabout
                        if (onewayTag.equals("-1")) {
                            continue;
                        }
                    }
                    if (onewayTag != null && way.getWayNodes().get(way.getWayNodes().size() - 1).getNodeId() == wayNode.getNodeId()) {
                        // the way ends at the roundabout
                        if (!onewayTag.equals("-1")) {
                            continue;
                        }
                    }
                }

                count++;
                break; // don't look at more ways on this roundabout-node, it IS an exit
            }
        }
        return count;
    }

    /**
     * @return true if there is a next driving-instruction.
     * @see org.openstreetmap.travelingsalesman.routing.describers.IRouteDescriber#hasNextInstruction()
     */
    public boolean hasNextInstruction() {
        return myCurrentStepIterator.hasNext();
    }

    /**
     * Example: "In 200m turn right onto XYZ road".
     * @return The current instruction in a location-dependent way.
     * @param aDistance distance to {@link #getCurrentInstructionLocation()} in kilometers.
     * @see #getNextInstruction()
     */
    public String getCurrentInstruction(final double aDistance) {
        if (myNextStep == null) {
            return "";
        }
        String sentence = getCurrentInstructionInternal(aDistance);
        //TODO: make a better choice of "when to speak" then by using a constant distance
        if (!this.hasSpokenCurrentStep && aDistance < SPEAKCURRENTSTEPDISTANCE) {
            this.hasSpokenCurrentStep = true;
            speak(sentence);
        }
        return sentence;
    }
    /**
     * Example: "In 200m turn right onto XYZ road".
     * @return The current instruction in a location-dependent way.
     * @param aDistance distance to {@link #getCurrentInstructionLocation()} in kilometers.
     * @see #getNextInstruction()
     */
    public String getCurrentInstructionInternal(final double aDistance) {
        if (myNextStep == null) {
            return "";
        }

//        String junctionType = WayHelper.getTag(myCurrentStep.getWay(), "junction");
//        String highwayType = WayHelper.getTag(myCurrentStep.getWay(), "highway");
//        if (junctionType != null && junctionType.equalsIgnoreCase("roundabout")) {
//            highwayType = "roundabout";
//        }
        String nextRoadName = NameHelper.getNameForWay(myNextStep.getWay());
        String upcommingJunctionType = WayHelper.getTag(myNextStep.getWay(), UsedTags.TAG_JUNCTION);
        String currentJunctionType = WayHelper.getTag(myCurrentStep.getWay(), UsedTags.TAG_JUNCTION);

        String unit = RESOURCE.getString("units/km");
        String distance = NumberFormat.getIntegerInstance().format(aDistance);
        if (aDistance < 1) {
            unit = RESOURCE.getString("units/m");
            final int kilo = 1000;
            distance = NumberFormat.getIntegerInstance().format(aDistance * kilo);
        }

        // are we entering a roundabout?
        if (myNextStep != null) {
            if (upcommingJunctionType != null && upcommingJunctionType.equalsIgnoreCase("roundabout")) {
                Way roundabout = myNextStep.getWay();
                Node enter = myNextStep.getStartNode();
                Node exit = myNextStep.getEndNode();
                IDataSet map = myRoute.getMap();
                int exitNumber = countExits(map, roundabout, enter, exit);
                String exitNumberStr = getExitNumber(exitNumber);
                this.myCurrentInstructionType = DrivingInstructionType.ROUNDABOUT;
                return MessageFormat.format(RESOURCE.getString("enter_roundabout_EXIT_DISTANCE"), distance, unit, exitNumberStr);
            }
        }

        // are we exiting a roundabout?
        if (currentJunctionType != null && currentJunctionType.equalsIgnoreCase("roundabout")) {
            Way roundabout = myCurrentStep.getWay();
            Node enter = myCurrentStep.getStartNode();
            Node exit = myCurrentStep.getEndNode();
            IDataSet map = myRoute.getMap();
            int exitNumber = countExits(map, roundabout, enter, exit);
            this.myCurrentInstructionType = DrivingInstructionType.ROUNDABOUT;
            return MessageFormat.format(RESOURCE.getString("take_EXIT"), getExitNumber(exitNumber));
        }
        switch (this.myCurrentInstructionType) {
        case GO_AHEAD:
            return MessageFormat.format(RESOURCE.getString("follow_NAME_DISTANCE"), distance, unit, nextRoadName);
        case KEEP_LEFT:
            return MessageFormat.format(RESOURCE.getString("turn_DIRECTION_DISTANCE"), distance, unit, RESOURCE.getString("direction/left/slight"));
        case KEEP_RIGHT:
            return MessageFormat.format(RESOURCE.getString("turn_DIRECTION_DISTANCE"), distance, unit, RESOURCE.getString("direction/right/slight"));
        case TURN_LEFT:
            return MessageFormat.format(RESOURCE.getString("turn_DIRECTION_DISTANCE"), distance, unit, RESOURCE.getString("direction/left"));
        case TURN_RIGHT:
            return MessageFormat.format(RESOURCE.getString("turn_DIRECTION_DISTANCE"), distance, unit, RESOURCE.getString("direction/right"));
        case TURN_LEFT_HARD:
            return MessageFormat.format(RESOURCE.getString("turn_DIRECTION_DISTANCE"), distance, unit, RESOURCE.getString("direction/left/hard"));
        case TURN_RIGHT_HARD:
            return MessageFormat.format(RESOURCE.getString("turn_DIRECTION_DISTANCE"), distance, unit, RESOURCE.getString("direction/right/hard"));
        case TURN_AROUND:
            return MessageFormat.format(RESOURCE.getString("turn_around_DISTANCE"), distance, unit);
        default:
            return "";
        }

    }

    /**
     * Return the proper instruction to be displayed to the user for
     * the given kind of instruciton.
     * @param aType the instruction
     * @param aHighwayType (optional) the value of the highway-tag of the next road
     * @param aNextRoadName the name of the next road
     * @return the instruction
     */
    private String getInstruction(final DrivingInstructionType aType, final String aHighwayType, final String aNextRoadName) {
            switch (aType) {
            case GO_AHEAD:
                return MessageFormat.format(RESOURCE.getString("follow_NAME"), aNextRoadName);
            case KEEP_LEFT:
                return MessageFormat.format(RESOURCE.getString("turn_DIRECTION"), RESOURCE.getString("direction/left/slight"));
            case KEEP_RIGHT:
                return MessageFormat.format(RESOURCE.getString("turn_DIRECTION"), RESOURCE.getString("direction/right/slight"));
            case TURN_LEFT:
                return MessageFormat.format(RESOURCE.getString("turn_DIRECTION"), RESOURCE.getString("direction/left"));
            case TURN_RIGHT:
                return MessageFormat.format(RESOURCE.getString("turn_DIRECTION"), RESOURCE.getString("direction/right"));
            case TURN_LEFT_HARD:
                return MessageFormat.format(RESOURCE.getString("turn_DIRECTION"), RESOURCE.getString("direction/left/hard"));
            case TURN_RIGHT_HARD:
                return MessageFormat.format(RESOURCE.getString("turn_DIRECTION"), RESOURCE.getString("direction/right/hard"));
            case TURN_AROUND:
                return RESOURCE.getString("turn_around");
            default:
                return "";
            }
    }

    /**
     * Return "follow", "turn right/left onto, ...
     * @param aNextStep where we are going
     * @param aCurrentStep where we come from (may be null)
     * @return the DrivingInstructionType
     */
    private DrivingInstructionType getDrivingInstructionType(final RoutingStep aCurrentStep, final RoutingStep aNextStep) {

        RoutingStep currentStep = aCurrentStep;
        RoutingStep nextStep = aNextStep;
        if (nextStep == null) {
            return DrivingInstructionType.GO_AHEAD; //TODO: "in XX meters, you have reached your destination";
        }
        // if we stay on the same road, ignore SLIGHTTURN
        boolean sameRoad = isSameStreet(currentStep, nextStep);

        if (aCurrentStep != null) {

            // defensive programming: what if last and current are mixed up
            if (currentStep.getStartNode().getId() == nextStep.getEndNode().getId()) {
                RoutingStep temp = currentStep;
                currentStep = nextStep;
                nextStep = temp;
                LOG.fine("SimpleRouteDescriber.getDrivingInstructionType() - switching currentStep and nextStep");
            }


            // this is where we come from
            double  px = currentStep.getStartNode().getLatitude();
            double  py = currentStep.getStartNode().getLongitude();

            // this id where we go to
            double  qx = nextStep.getEndNode().getLatitude();
            double  qy = nextStep.getEndNode().getLongitude();

            // this is where we are
            double  rootx = nextStep.getStartNode().getLatitude();
            double  rooty = nextStep.getStartNode().getLongitude();

            // normalize
            px -= rootx; qx -= rootx;
            py -= rooty; qy -= rooty;

            double angleTo = Math.atan2(qy , qx) / Math.PI * HALFCIRCLE;
            angleTo = modulo(angleTo);
            double angleFrom = Math.atan2(py , px) / Math.PI * HALFCIRCLE;
            angleFrom = modulo(angleFrom);
            double angle = angleTo - angleFrom;
            double angleDifference = angle;
            if (angleDifference < 0) {
                // to-[from]
                // 0-[180-0]   = [-180 - 0]    => [180-360] => [right ahead - right u-turn]
                // 0-[180-360] = [-180 - -360] => [180-0]   => [left  ahead - left  u-turn]
                angleDifference = angleDifference + 2 * HALFCIRCLE;
            }
            if (angleDifference > 2 * HALFCIRCLE) {
                angleDifference = angleDifference - 2 * HALFCIRCLE;
            }

            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "SimpleRouteDescriber - angleDifference=" + angleDifference + "° "
                        + "(from=" + (angleFrom) + "°)(to=" + (angleTo) + "°)");
            }

            ///////////////
            //180 = ahead
            // > 180 = right
            // < 180 = left
            ///////////////

            // < 10 || > 350
            if (angleDifference <  TURNAROUND  || angleDifference > 2 * HALFCIRCLE - TURNAROUND) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "SimpleRouteDescriber - angleDifference=" + angleDifference + "° "
                            + "is turnaround");
                }
                return DrivingInstructionType.TURN_AROUND;
            }

            // < 170
            // angle in [10-160]
            if (angleDifference < HALFCIRCLE - ((HALFCIRCLE - SLIGHTTURN) / 2)) { // angle = 0-180, 180=ahead
                // left
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "SimpleRouteDescriber - angleDifference=" + angleDifference + "° "
                            + "is left");
                }

                // angle > 140
                // angle in [140 - 160]
                if (angleDifference > SLIGHTTURN) {
                    if (sameRoad) {
                        return DrivingInstructionType.GO_AHEAD;
                    }
                    return DrivingInstructionType.KEEP_LEFT;
                }

                // angle in [135-140]
                if (angleDifference > NORMALTURN) {
                    return DrivingInstructionType.TURN_LEFT;
                }

                // angle in [60-135]
//                if (angleDifference > HARDTURN) {
                    return DrivingInstructionType.TURN_LEFT_HARD;
//                }
//
//                if (LOG.isLoggable(Level.FINER)) {
//                    LOG.log(Level.FINER, "SimpleRouteDescriber - angleDifference=" + angleDifference + "° "
//                            + "is left but no left-turn");
//                }
            }

            // 306 => 360 - 306 = 54
            double invertedAngle = 2 * HALFCIRCLE - angleDifference; // 180..360=>180..0
            // > 190
            // angle in [200-350]
            if (angleDifference > HALFCIRCLE + ((HALFCIRCLE - SLIGHTTURN) / 2)) { // angle = 180+epsilon-360, 180=ahead
                // right
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "SimpleRouteDescriber - angleDifference=" + angleDifference + "° "
                            + "is right invertedAngle=" + invertedAngle);
                }

                // (360-x) > 140 => 220 > x
                // angle in [200-220]
                if (invertedAngle > SLIGHTTURN) {
                    if (sameRoad) {
                        return DrivingInstructionType.GO_AHEAD;
                    }
                    return DrivingInstructionType.KEEP_RIGHT;
                }

                // (360-x) > 70 => 290 > x
                // angle in [200-290]
                if (invertedAngle > NORMALTURN) {
                    return DrivingInstructionType.TURN_RIGHT;
                }

                // (360-x) > 60 => 300 > x
                // angle in [200-300]
                //if (invertedAngle > HARDTURN) {
                    return DrivingInstructionType.TURN_RIGHT;
//                }
//                if (LOG.isLoggable(Level.FINER)) {
//                    LOG.log(Level.FINER, "SimpleRouteDescriber - angleDifference=" + angleDifference + "° "
//                            + "is right but no right-turn");
//                }
            }


            if (LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, "SimpleRouteDescriber - angleDifference=" + angleDifference + "° "
                        + "is no turn, so it is go-ahead");
            }

            return DrivingInstructionType.GO_AHEAD;
        }

        return DrivingInstructionType.GO_AHEAD;
    }

    /**
     * @param currentStep the current step taken
     * @param nextStep the next step to take
     * @return true if both ways belong to the same street
     */
    private boolean isSameStreet(final RoutingStep currentStep, final RoutingStep nextStep) {
        if (nextStep == null) {
            return false;
        }
        if (currentStep.getWay().getId() == nextStep.getWay().getId()) {
            return true;
        }
        String nameA = NameHelper.getNameForWay(currentStep.getWay());
        String nameB = NameHelper.getNameForWay(nextStep.getWay());
        if (nameA != null && nameB != null &&  nameA.equalsIgnoreCase(nameB)) {
            return true;
        }
        return false;
    }

    /**
     * @param anAngle angle in degrees
     * @return the angle in the range of 0..360 degrees
     */
    private double modulo(final double anAngle) {
        double angleFrom = anAngle;
        while (angleFrom < 2 * HALFCIRCLE) {
            angleFrom += 2 * HALFCIRCLE;
        }
        while (angleFrom >= 2 * HALFCIRCLE) {
            angleFrom -= 2 * HALFCIRCLE;
        }
        return angleFrom;
    }

    /**
     * ${@inheritDoc}.
     */
    public Node getCurrentInstructionLocation() {
        return myIntersectionNode;
    }

    /**
     * @return Returns the type of the current driving-instruction.
     * @see #myCurrentInstructionType
     */
    public DrivingInstructionType getCurrentInstructionType() {
        return this.myCurrentInstructionType;
    }

    //=============================== getters/setters

    /**
     * ${@inheritDoc}.
     */
    public Route getRoute() {
        return this.myRoute;
    }


    /**
     * @return The RoutingStep the current instruction belongs to.
     */
    public RoutingStep getCurrentStep() {
        return this.myCurrentStep;
    }
    /**
     * @param aSentence the sentence to speak.
     */
    private void speak(final String aSentence) {
        try {
            if (myVoiceSynth == null) {
                myVoiceSynth = Settings.getInstance().getPluginProxy(IVoiceSynth.class, NoVoice.class.getName());
            }
            myVoiceSynth.speak(aSentence);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot do voice-synthesesis", e);
        }
    }

}
