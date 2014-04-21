/**
 * 
 */
package org.openstreetmap.travelingsalesman.trafficblocks.tmc;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.travelingsalesman.trafficblocks.TrafficMessage;
import org.openstreetmap.travelingsalesman.trafficblocks.TrafficMessage.TYPES;

/**
 * Factory to look up and create a TMC-event for a given event-code.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class TMCEventFactory {

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(RDSTMCParser.class.getName());

    /**
     * All TMC-events (ISO 14819-2) with a translation
     * for the current language.
     */
    private ResourceBundle myEvents;

    /**
     * TrafficMessage.TYPE + length, ... for
     * all TMC-messages we understand.
     */
    private ResourceBundle myEventSemantics;

    /**
     * Properties-file with road-name and display-location
     * for TMC-events that are outside the map.
     */
    private ResourceBundle myLocationLists;

    /**
     * Constructor.
     */
    public TMCEventFactory() {
        this.myEvents = ResourceBundle.getBundle("org.openstreetmap.travelingsalesman.trafficblocks.tmc.Events");
        this.myLocationLists = ResourceBundle.getBundle("org.openstreetmap.travelingsalesman.trafficblocks.tmc.tmclocations");
        this.myEventSemantics = ResourceBundle.getBundle("org.openstreetmap.travelingsalesman.trafficblocks.tmc.Eventsemantics");
    }
    /**
     * The different natures defined in TMC that
     * an event can have.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    public static enum EVENTNATURES {
        /**
         * Message about a present situation.
         */
        information,
        /**
         * Message that predicts an event in the future.
         */
        forecast,
        /**
         * Message that is now shown.
         */
        silent
    };
    /**
     * A TMC-event with all we need to know about it.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    public static class TMCEvent extends TrafficMessage {
        /**
         * Event-code as transmitted in TMC.
         */
        private final int myEventCode;
        /**
         * English name of the event.
         */
        private final String myEventName;
        /**
         * Is the event dynamic or long lasting?
         * This has an effect on the interpretation
         * of the duration-value.
         */
        private final boolean isLongLasting;
        /**
         * Does the event affect both directions?
         */
        private final boolean isBothDirections;
        /**
         * The nature of this event.
         */
        private final EVENTNATURES myNature;
        /**
         * @param aEventCode Event-code as transmitted in TMC.
         * @param aEventName English name of the event.
         * @param aLongLasting Is the event dynamic or long lasting?
         * @param aBothDirections Does the event affect both directions?
         * @param aNature The nature of this event.
         * @param aDurationCode the given code of the duration of this event
         * @param aPlace where the event takes place
         * @param aType the type of this event
         * @param aLength the affected length of road
         */
        protected TMCEvent(final int aEventCode,
                           final String aEventName,
                           final boolean aLongLasting,
                           final boolean aBothDirections,
                           final EVENTNATURES aNature,
                           final int aDurationCode,
                           final Entity aPlace,
                           final TYPES aType,
                           final int aLength) {
            super(aPlace,
                  aEventName,
                  new Date(parseDuration(aNature, aLongLasting, aDurationCode, new GregorianCalendar()).getTimeInMillis()),
                  aType,
                  aLength);
            this.myEventCode = aEventCode;
            this.myEventName = aEventName;
            this.isLongLasting = aLongLasting;
            this.isBothDirections = aBothDirections;
            this.myNature = aNature;
        }
        /**
         * @return the eventCode
         */
        public int getEventCode() {
            return myEventCode;
        }
        /**
         * @return the eventName
         */
        public String getEventName() {
            return myEventName;
        }
        /**
         * @return the isLongLasting
         */
        public boolean isLongLasting() {
            return isLongLasting;
        }
        /**
         * @return the isBothDirections
         */
        public boolean isBothDirections() {
            return isBothDirections;
        }
        /**
         * @return the nature
         */
        public EVENTNATURES getNature() {
            return myNature;
        }
        /**
         * @return {@link #isBothDirections()}
         */
        public boolean getBothDirectionsAffected() {
            return isBothDirections;
        }

        /**
         * Find out until or for what time-duration the event will happen.
         * @param aNature nature of this event
         * @param isLongLasting is this a long lasting event?
         * @param aDuration the duration-code received
         * @param aDate time of the receiving
         * @return the calculated time or null
         */
        private static GregorianCalendar parseDuration(final EVENTNATURES aNature,
                                   final boolean isLongLasting,
                                   final int aDuration,
                                   final GregorianCalendar aDate) {
            if (aNature.equals(EVENTNATURES.silent)) {
                return null;
            }
            if (aDuration == 0) {
                // no explitit duration given.
                aDate.add(GregorianCalendar.HOUR, 1);
                return aDate;
            }
            if (isLongLasting) {
                if (aNature.equals(EVENTNATURES.information)) {
                    switch (aDuration) {
                    case 1 :
                        // next few hours
                        aDate.add(GregorianCalendar.HOUR, 5);
                        return aDate;
                    case 2 :
                        // for the rest of the day
                        aDate.set(GregorianCalendar.HOUR, 23);
                        aDate.set(GregorianCalendar.MINUTE, 59);
                        aDate.set(GregorianCalendar.SECOND, 59);
                        return aDate;
                    case 3 :
                        // until tomorrow evening
                        aDate.set(GregorianCalendar.HOUR, 18);
                        aDate.set(GregorianCalendar.MINUTE, 0);
                        aDate.set(GregorianCalendar.SECOND, 0);
                        aDate.add(GregorianCalendar.DAY_OF_MONTH, 1);
                        return aDate;
                    case 4 :
                        // for the rest of the week (specified as sunday)
                        if (aDate.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.SUNDAY) {
                            aDate.add(GregorianCalendar.WEEK_OF_YEAR, 1);
                        }
                        aDate.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.SUNDAY);
                        aDate.set(GregorianCalendar.HOUR, 23);
                        aDate.set(GregorianCalendar.MINUTE, 59);
                        aDate.set(GregorianCalendar.SECOND, 59);
                        return aDate;
                    case 5 :
                        // until the end of next week
                        aDate.add(GregorianCalendar.WEEK_OF_YEAR, 1);
                        if (aDate.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.SUNDAY) {
                            aDate.add(GregorianCalendar.WEEK_OF_YEAR, 1);
                        }
                        aDate.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.SUNDAY);
                        aDate.set(GregorianCalendar.HOUR, 23);
                        aDate.set(GregorianCalendar.MINUTE, 59);
                        aDate.set(GregorianCalendar.SECOND, 59);
                        return aDate;
                    case 6 :
                        // until the end of the month
                        aDate.set(GregorianCalendar.HOUR, 23);
                        aDate.set(GregorianCalendar.MINUTE, 59);
                        aDate.set(GregorianCalendar.SECOND, 59);
                        aDate.add(GregorianCalendar.MONTH, 1);
                        aDate.set(GregorianCalendar.DAY_OF_MONTH, 1);
                        aDate.add(GregorianCalendar.DAY_OF_MONTH, -1);
                        return aDate;
                    case 7 :
                        // for a long period
                        aDate.add(GregorianCalendar.MONTH, 2);
                        return aDate;
                    default:
                        LOG.warning("Illegal time-code " + aDuration + " given");
                        return null;
                    }
                } else {
                    switch (aDuration) {
                    case 1 :
                        // next few hours
                        aDate.add(GregorianCalendar.HOUR, 5);
                        return aDate;
                    case 2 :
                        // for the rest of the day
                        aDate.set(GregorianCalendar.HOUR, 23);
                        aDate.set(GregorianCalendar.MINUTE, 59);
                        aDate.set(GregorianCalendar.SECOND, 59);
                        return aDate;
                    case 3 :
                        // tomorrow
                        aDate.set(GregorianCalendar.HOUR, 23);
                        aDate.set(GregorianCalendar.MINUTE, 59);
                        aDate.set(GregorianCalendar.SECOND, 59);
                        aDate.add(GregorianCalendar.DAY_OF_MONTH, 1);
                        return aDate;
                    case 4 :
                        // the day after tomorrow
                        aDate.add(GregorianCalendar.DAY_OF_WEEK, 2);
                        aDate.set(GregorianCalendar.HOUR, 23);
                        aDate.set(GregorianCalendar.MINUTE, 59);
                        aDate.set(GregorianCalendar.SECOND, 59);
                        return aDate;
                    case 5 :
                     // this weekend
                        if (aDate.get(GregorianCalendar.DAY_OF_WEEK) == GregorianCalendar.SUNDAY) {
                            aDate.add(GregorianCalendar.WEEK_OF_YEAR, 1);
                        }
                        aDate.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.SUNDAY);
                        aDate.set(GregorianCalendar.HOUR, 23);
                        aDate.set(GregorianCalendar.MINUTE, 59);
                        aDate.set(GregorianCalendar.SECOND, 59);
                        return aDate;
                    case 6 :
                        // later this week
                        aDate.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.SUNDAY);
                        aDate.set(GregorianCalendar.HOUR, 23);
                        aDate.set(GregorianCalendar.MINUTE, 59);
                        aDate.set(GregorianCalendar.SECOND, 59);
                        return aDate;
                    case 7 :
                        // next week
                        aDate.add(GregorianCalendar.WEEK_OF_YEAR, 1);
                        aDate.set(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.SUNDAY);
                        aDate.set(GregorianCalendar.HOUR, 23);
                        aDate.set(GregorianCalendar.MINUTE, 59);
                        aDate.set(GregorianCalendar.SECOND, 59);
                        return aDate;
                    default:
                        LOG.warning("Illegal time-code " + aDuration + " given");
                        return null;
                    }
                }
            } else {
                // not long lasting
                if (aNature.equals(EVENTNATURES.information)) {
                    switch (aDuration) {
                    case 1 :
                        aDate.add(GregorianCalendar.MINUTE, 15);
                        return aDate;
                    case 2 :
                        aDate.add(GregorianCalendar.MINUTE, 30);
                        return aDate;
                    case 3 :
                        aDate.add(GregorianCalendar.HOUR, 1);
                        return aDate;
                    case 4 :
                        aDate.add(GregorianCalendar.HOUR, 2);
                        return aDate;
                    case 5 :
                        aDate.add(GregorianCalendar.HOUR, 3);
                        return aDate;
                    case 6 :
                        aDate.add(GregorianCalendar.HOUR, 4);
                        return aDate;
                    case 7 :
                        // for the rest of the day
                        aDate.set(GregorianCalendar.HOUR, 23);
                        aDate.set(GregorianCalendar.MINUTE, 59);
                        aDate.set(GregorianCalendar.SECOND, 59);
                        return aDate;
                    default:
                        LOG.warning("Illegal time-code " + aDuration + " given");
                        return null;
                    }
                } else {
                    switch (aDuration) {
                    case 1 :
                        aDate.add(GregorianCalendar.MINUTE, 15);
                        return aDate;
                    case 2 :
                        aDate.add(GregorianCalendar.MINUTE, 30);
                        return aDate;
                    case 3 :
                        aDate.add(GregorianCalendar.HOUR, 1);
                        return aDate;
                    case 4 :
                        aDate.add(GregorianCalendar.HOUR, 2);
                        return aDate;
                    case 5 :
                        aDate.add(GregorianCalendar.HOUR, 3);
                        return aDate;
                    case 6 :
                        aDate.add(GregorianCalendar.HOUR, 4);
                        return aDate;
                    case 7 :
                        // for the rest of the day
                        aDate.set(GregorianCalendar.HOUR, 23);
                        aDate.set(GregorianCalendar.MINUTE, 59);
                        aDate.set(GregorianCalendar.SECOND, 59);
                        return aDate;
                    default:
                        LOG.warning("Illegal time-code " + aDuration + " given");
                        return null;
                    }
                }
            }
        }

    }

    /**
     * @param aLocationCode a TMC location-code
     * @param aCountryID TMC CID
     * @param aTableID   TMC TABCD
     * @param anEventCode the event to lok up
     * @param aDurationCode encoded duration given in the message
     * @param aPlace where the event takes place
     * @param anExtend number of steps this event extends
     * @param aDirectionIsReverse true=extends reverse, false=extends forward
     * @return the TMCEvent or null if unknown.
     */
    public TMCEvent createEvent(final int aLocationCode,
                                final int aCountryID,
                                final int aTableID,
                                final int anEventCode,
                                final int aDurationCode,
                                final Entity aPlace,
                                final int anExtend,
                                final boolean aDirectionIsReverse) {

        String eventLine = null;
        String semantics = null;
        try {
            eventLine = myEvents.getString(Integer.toString(anEventCode));
            semantics = myEventSemantics.getString(Integer.toString(anEventCode));
        } catch (MissingResourceException e) {
            // ignoring
        }
        if (eventLine == null) {
            LOG.warning("Unknown TMC event 0x" + Integer.toHexString(anEventCode) + " not in ISO 14819-2");
            return null;
        }

        // parse our semantics for this event
        int aLength = 0;
        TYPES aType = TYPES.IGNORED;
        if (semantics != null) {
            String[] split = semantics.split(",");
            aType   = TYPES.valueOf(split[0]);
            aLength = Integer.parseInt(split[1]);
        }

        // parse the TMC event-list
        String[] columns = eventLine.split(";");
        String descriptionLocalized = columns[0];
        String t = columns[5]; // column T in ISO 14819-2 is the time-period
        // D=dynamic
        // L=long lasting
        boolean longLasting = t.equalsIgnoreCase("L");
        String directionality = columns[6];
        // 1=one direction
        // 2=both directions
        boolean bothDirections = directionality.equalsIgnoreCase("2");

        String urgency = columns[7];
        // U=urgend
        // X=extremely urgend

        String update_class = "";
        if (columns.length > 8) {
            update_class = columns[8];
        }

        String phrase_code = "";
        if (columns.length > 9) {
            phrase_code = columns[9];
        }
        String event_nature = "";
        if (columns.length > 3) {
            event_nature = columns[3];
        }
        // blank = information
        // F = forecast
        // S = silent
        EVENTNATURES nature = EVENTNATURES.information;
        if (event_nature.equalsIgnoreCase("F")) {
            nature = EVENTNATURES.forecast;
        } else if (event_nature.equalsIgnoreCase("S")) {
            nature = EVENTNATURES.silent;
        }

        descriptionLocalized = addLocationName(aLocationCode, aCountryID, aTableID) + descriptionLocalized;
        TMCEvent event = new TMCEvent(anEventCode, descriptionLocalized, longLasting, bothDirections, nature, aDurationCode, aPlace, aType , aLength);
        addExtendedLocation(aLocationCode, aCountryID, aTableID, event);
        addExtendedExtend(aLocationCode, aCountryID, aTableID, aDirectionIsReverse, anExtend, event);
        return event;
    }

    /**
     * Add the name of the location from the TMC locationList
     * if known.
     * @param aLocationCode the location
     * @param aCountryID the country of the location
     * @param aTableID the country specific table of the location
     * @return empty String or location-name.
     */
    private String addLocationName(final int aLocationCode,
            final int aCountryID, final int aTableID) {
        try {
            String streetName = myLocationLists.getString(aCountryID + "." + aTableID + "." + aLocationCode + ".name");
            return "(" + streetName + ")";
        } catch (Exception e) {
            LOG.fine("Location " + aCountryID + "." + aTableID + "." + aLocationCode + " not in tmclocations.properties");
        }
        return "";
    }

    /**
     * @param aLocationCode
     * @param aCountryID
     * @param aTableID
     * @param aDirectionIsReverse
     * @param aAnExtend
     * @param aEvent
     */
    private void addExtendedExtend(final int aLocationCode,
                                   final int aCountryID,
                                   final int aTableID,
                                   final boolean aDirectionIsReverse,
                                   final int aAnExtend,
                                   final TMCEvent aEvent) {
        LinkedList<LatLon> extendsLatLon = new LinkedList<LatLon>();
        long nextLocationCode = aLocationCode;
        String dir = ".next";
        if (aDirectionIsReverse) {
            dir = ".prev";
        }
        while (nextLocationCode != -1) {
            long temp = nextLocationCode;
            nextLocationCode = -1;
            try {
                String key = aCountryID + "." + aTableID + "." + temp + dir;
                nextLocationCode = Long.parseLong(myLocationLists.getString(key));
            } catch (Exception e) {
                LOG.fine("Next location after " + aCountryID + "." + aTableID + "." + aLocationCode + " not in tmclocations.properties");
            }
            if (nextLocationCode == -1) {
                break;
            }
            try {
                Double lat = Double.parseDouble(myLocationLists.getString(aCountryID + "." + aTableID + "." + nextLocationCode + ".lat"));
                Double lon = Double.parseDouble(myLocationLists.getString(aCountryID + "." + aTableID + "." + nextLocationCode + ".lon"));
                extendsLatLon.add(new LatLon(lat, lon));
            } catch (Exception e) {
                LOG.fine("Location " + aCountryID + "." + aTableID + "." + aLocationCode + " not in tmclocations.properties");
            }
        }
        aEvent.getExtendedProperties().put(TMCEvent.EXTPROPERTYEXTENDSTOLATLONS, extendsLatLon);
    }

    /**
     * @param aLocationCode
     * @param aCountryID
     * @param aTableID
     * @param event
     */
    private void addExtendedLocation(final int aLocationCode,
            final int aCountryID, final int aTableID, final TMCEvent event) {
        try {
            Double lat = Double.parseDouble(myLocationLists.getString(aCountryID + "." + aTableID + "." + aLocationCode + ".lat"));
            Double lon = Double.parseDouble(myLocationLists.getString(aCountryID + "." + aTableID + "." + aLocationCode + ".lon"));
            event.getExtendedProperties().put(TMCEvent.EXTPROPERTYLATLON, new LatLon(lat, lon));
        } catch (Exception e) {
            LOG.fine("Location " + aCountryID + "." + aTableID + "." + aLocationCode + " not in tmclocations.properties");
        }
    }
}
