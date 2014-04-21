package org.openstreetmap.travelingsalesman.gps;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for common GPS providers features.
 * @author coms
 */
public abstract class BaseGpsProvider implements IGPSProvider {

    /**
     * my logger for debug and error-output.
     */
    protected static final Logger LOG = Logger.getLogger(BaseGpsProvider.class.getName());
    /**
     * Our listeners for location-updates.
     */
    private Set<IGPSListener> myListeners = new HashSet<IGPSListener>();

    /**
     * Inform our listeners.
     * @param lat the latitude
     * @param lon the longitude
     */
    protected void sendLocationChanged(final double lat, final double lon) {
        LOG.log(Level.FINER, "gps: lat = " + lat + " lon = " + lon);
        for (IGPSListener listener : myListeners) {
            try {
                listener.gpsLocationChanged(lat, lon);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "[Exception] Problem while informing listener "
                        + "[" + listener.getClass().getName() + "] about a GPS location-change",
                             e);
            }
        }
    }

    /**
     * Inform our listeners if course (direction) was changed.
     * @param course new course in degrees. (0 is north)
     */
    protected void sendCourseChanged(final double course) {
        LOG.log(Level.FINER, "gps: course = " + course);
        for (IGPSListener listener : myListeners) {
            listener.gpsCourseChanged(course);
        }
    }

    /**
     * Inform our listeners.
     * @param date date in milliseconds, from 1970.01.01
     * @param time time in milliseconds in UTC
     */
    protected void sendDateTimeChanged(final long date, final long time) {
        LOG.log(Level.FINER, "gps: date = " + date + ", time = " + time);
        for (IGPSListener listener : myListeners) {
            if (listener instanceof IExtendedGPSListener) {
                ((IExtendedGPSListener) listener).gpsDateTimeChanged(date, time);
            }
        }
    }

    /**
     * Inform our listeners.
     * @param fixQuality new fix quality value
     */
    protected void sendFixQualityChanged(final int fixQuality) {
        LOG.log(Level.FINER, "gps: fixQuality = " + fixQuality);
        for (IGPSListener listener : myListeners) {
            if (listener instanceof IExtendedGPSListener) {
                ((IExtendedGPSListener) listener).gpsFixQualityChanged(fixQuality);
            }
        }
    }

    /**
     * Notify listener about gps fix was obtained.
     */
    protected void sendLocationObtained() {
        LOG.log(Level.FINER, "gps: fix obtained");
        for (IGPSListener listener : myListeners) {
            listener.gpsLocationObtained();
        }
    }

    /**
     * Notify listeners about gps fix was lost.
     */
    protected void sendLocationLost() {
        LOG.log(Level.FINER, "gps: fix losted");
        for (IGPSListener listener : myListeners) {
            listener.gpsLocationLost();
        }
    }

    /**
     * Notify listeners if amount of used satellites was changed.
     * @param usedSatellites new amount of used satellites
     */
    protected void sendUsedSatellitesChanged(final int usedSatellites) {
        LOG.log(Level.FINER, "gps: used satellites = " + usedSatellites);
        for (IGPSListener listener : myListeners) {
            if (listener instanceof IExtendedGPSListener) {
                ((IExtendedGPSListener) listener).gpsUsedSattelitesChanged(usedSatellites);
            }
        }
    }

    /**
     * Notify listeners if the dillusion of precision has changed.
     * @param hdop horizontal dillusion of precision
     * @param vdop vertical dillusion of precision
     * @param pdop positional dillusion of precision
     */
    protected void sendDopChanged(final double hdop, final double vdop, final double pdop) {
        LOG.log(Level.FINER, "gps: dilution of precision HDOP = " + hdop + ", VDOP = " + vdop + ", PDOP = " + pdop);
        for (IGPSListener listener : myListeners) {
            if (listener instanceof IExtendedGPSListener) {
                ((IExtendedGPSListener) listener).gpsDopChanged(hdop, vdop, pdop);
            }
        }
    }

    /**
     * Notify listeners if the altitude has changed.
     * @param altitude the new altitude
     */
    protected void sendAltitudeChanged(final double altitude) {
        LOG.log(Level.FINER, "gps: altitude = " + altitude);
        for (IGPSListener listener : myListeners) {
            if (listener instanceof IExtendedGPSListener) {
                ((IExtendedGPSListener) listener).gpsAltitudeChanged(altitude);
            }
        }
    }

    /**
     * Notify listeners if the speed has changed.
     * @param speed the new speed
     */
    protected void sendSpeedChanged(final double speed) {
        LOG.log(Level.FINER, "gps: speed = " + speed);
        for (IGPSListener listener : myListeners) {
            if (listener instanceof IExtendedGPSListener) {
                ((IExtendedGPSListener) listener).gpsSpeedChanged(speed);
            }
        }
    }

    /**
     * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider#addGPSListener(org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener)
     * @param aListener the new listener to add.
     */
    public void addGPSListener(final IGPSListener aListener) {
        this.myListeners.add(aListener);
    }

    /**
     * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider#removeGPSListener(org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener)
     * @param aListener the listener to remove.
     */
    public void removeGPSListener(final IGPSListener aListener) {
        this.myListeners.remove(aListener);
    }

    /**
     * @return the listeners
     */
    public Set<IGPSListener> getListeners() {
        return myListeners;
    }

}
