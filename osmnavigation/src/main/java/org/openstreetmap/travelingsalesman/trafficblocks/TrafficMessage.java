package org.openstreetmap.travelingsalesman.trafficblocks;

import java.io.Serializable;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;

/**
 * This class encapsulates a general,
 * non source-dependent traffic-message.
 */
public class TrafficMessage {
    /**
     * Types of traffic-obstruction.
     */
    public enum TYPES {
        TRAFFICJAM,
        SLOWTRAFFIC,
        ROADBLOCK,
        IGNORED
    }

    /**
     * Reserved key for {@link #getExtendedProperties()}.
     * The value is to be a LatLon -instance.
     */
    public static final String EXTPROPERTYLATLON = "latlon";

    /**
     * Reserved key for {@link #getExtendedProperties()}.
     * The value is to be a List&lt;Long&gt; of node-IDs that the
     * event extends to (from the primary location in the opposite
     * direction of the moving traffic).
     */
    public static final String EXTPROPERTYEXTENDSTONODEIDS = "extendsto:node";
    /**
     * Reserved key for {@link #getExtendedProperties()}.
     * The value is to be a List&lt;Long&gt; of way-IDs that the
     * event extends to (from the primary location in the opposite
     * direction of the moving traffic).
     */
    public static final String EXTPROPERTYEXTENDSTOWAYIDS = "extendsto:way";
    /**
     * Reserved key for {@link #getExtendedProperties()}.
     * The value is to be a List&lt;LatLong&gt; that the
     * event extends to (from the primary location in the opposite
     * direction of the moving traffic).
     */
    public static final String EXTPROPERTYEXTENDSTOLATLONS = "extendsto:latlon";

    /**
     * If no validUntil is given, 1h from now is assumed.
     * @param aEntity the primary location of this message
     * @param aMessage the text of the message
     * @param aValidUntil when this message is no longer valid (may be null)
     * @param aLength the affected road-length in meters
     * @param aType The type of this event
     */
    public TrafficMessage(final Entity aEntity,
                             final String aMessage,
                             final Date aValidUntil,
                             final TYPES aType,
                             final int aLength) {
        super();
        myEntity = aEntity;
        myMessage = aMessage;
        myValidUntil = aValidUntil;
        myType = aType;
        myLength = aLength;
        if (aEntity == null) {
            throw new IllegalArgumentException("null place given");
        }
        if (aMessage == null) {
            throw new IllegalArgumentException("null message given");
        }
        if (aValidUntil == null) {
            GregorianCalendar now = new GregorianCalendar();
            now.add(GregorianCalendar.HOUR, 1);
            this.myValidUntil = new Date(now.getTimeInMillis());
        }
    }
    /**
     * The primary location, where the event happens.
     */
    private Entity myEntity;
    /**
     * Long message.
     */
    private String myMessage;
    /**
     * When the event shall be deleted.
     */
    private Date   myValidUntil;
    /**
     * name->value -pairs with extended information.
     * @see #EXTPROPERTYLATLON
     * @see #EXTPROPERTYEXTENDSTONODEIDs
     * @see #EXTPROPERTYEXTENDSTOWAYIDs
     */
    private Map<String, Serializable> myExtendedProperties;

    /**
     * The type of the traffic-message.
     */
    private TYPES myType;

    /**
     * The length of the affected segment in meters.
     */
    private int myLength;

    /**
     * @return the validUntil
     */
    public Date getValidUntil() {
        return myValidUntil;
    }
    /**
     * @param aValidUntil the validUntil to set
     */
    public void setValidUntil(final Date aValidUntil) {
        myValidUntil = aValidUntil;
    }
    /**
     * @return the entity
     */
    public Entity getEntity() {
        return myEntity;
    }
    /**
     * @return the message
     */
    public String getMessage() {
        return myMessage;
    }
    /**
     * @return the extendedProperties
     */
    public Map<String, Serializable> getExtendedProperties() {
        if (myExtendedProperties == null) {
            myExtendedProperties = new HashMap<String, Serializable>();
        }
        return myExtendedProperties;
    }

    /**
     * @return The type of the traffic-message.
     */
    public TrafficMessage.TYPES getType() {
        return myType;
    }

    /**
     * @return The length of the affected segment in meters.
     */
    public int getLengthInMeters() {
        return myLength;
    }
}