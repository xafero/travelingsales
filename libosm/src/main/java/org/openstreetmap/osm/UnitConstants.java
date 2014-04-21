package org.openstreetmap.osm;

/**
 * Inteface class contains units constants and conversion multipliers.
 * @author coms
 */
public interface UnitConstants {

    /**
     * Multilier for conversion nautical miles to kilometers.
     * km in one mile.
     */
    double MILES_TO_KM = 1.852;

    /**
     * Multplier for conversion kilometers to nautical miles coversion.
     * miles in one km.
     */
    double KM_TO_MILES = 0.539956;
    /**
     * Multplier for conversion from US-miles to kilometers.
     */
    double USMILES_TO_KM = 1.609344;

    /**
     * Milli* to * = 1000.
     */
    int MILLI = 1000;

    /**
     * 60 seconds to the minute.
     */
    int SECONDSPERMINUTE = 60;

    /**
     * 60 minutes to the hour.
     */
    int MINUTESPERHOUR = 60;

    /**
     * 1000.
     */
    int KILO = 1000;

    // define some constants
    /**
     * Minutes per degree.
     */
    int MINUTES_PER_DEGREE = 60;
}
