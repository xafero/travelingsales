/**
 * GpsPanel.java
 * (c) 2008 by <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
 * This file is part of traveling_salesman by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 *
 *  traveling_salesman is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  traveling_salesman is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with traveling_salesman.  If not, see <http://www.gnu.org/licenses/>.
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

import java.awt.Color;
import java.awt.GridLayout;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import org.openstreetmap.osm.UnitConstants;
import org.openstreetmap.travelingsalesman.gps.IGPSProvider.IExtendedGPSListener;
import org.openstreetmap.travelingsalesman.gui.MainFrame;

/**
 * Class represent JPanel with GPS data. It's displaying parsed NMEA data from
 * GpsD, JGps, and other providers extends NMEA parser.
 *
 * Shall showing GPS state: no gps | off-fix | on-fix. GPS data: latitude, longitude,
 * altitude, date, time, speed, heading, bearing, ... tracked satellites and
 * precision values. Compass with graphical presentation of driving direction.
 *
 * Some code was acquired from JGPS project by Tomas Darmovzal (darmovzalt)
 * http://sourceforge.net/projects/jgps/
 *
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov </a>
 */
public class GpsPanel extends JPanel implements IExtendedGPSListener {

    /**
     * default serialID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * my logger for debug and error-output.
     */
    static final Logger LOG = Logger.getLogger(GpsPanel.class.getName());

    /**
     * Amount of digits after decimal point for coordinates 10^n.
     */
    protected static final float PRECISION = 1000;

    /**
     * Latitude label text.
     */
    private JLabel latitudeText = new JLabel(MainFrame.RESOURCE.getString("GPSPanel.Labels.Latitude") + " : ");

    /**
     * Longitude label text.
     */
    private JLabel longitudeText = new JLabel(MainFrame.RESOURCE.getString("GPSPanel.Labels.Longitude") + " : ");

    /**
     * Date label text.
     */
    private JLabel dateText = new JLabel(MainFrame.RESOURCE.getString("GPSPanel.Labels.Date") + " : ");

    /**
     * Time label text.
     */
    private JLabel timeText = new JLabel(MainFrame.RESOURCE.getString("GPSPanel.Labels.Time") + " : ");

    /**
     * Altitude label text.
     */
    private JLabel altitudeText = new JLabel(MainFrame.RESOURCE.getString("GPSPanel.Labels.Altitude") + " : ");

    /**
     * Speed label text.
     */
    private JLabel speedText = new JLabel(MainFrame.RESOURCE.getString("GPSPanel.Labels.Speed") + " : ");

    /**
     * Course label text.
     */
    private JLabel courseText = new JLabel(MainFrame.RESOURCE.getString("GPSPanel.Labels.Course") + " : ");

    /**
     * Fix quality label and value.
     */
    private JLabel fixQualityLabel = new JLabel("", JLabel.CENTER);

    /**
     * Satellites label text.
     */
    private JLabel usedSatellitesText = new JLabel(MainFrame.RESOURCE.getString("GPSPanel.Labels.Satellites") + " : ");

    /**
     * Used satellites label and value.
     */
    private JLabel usedSatellitesLabel = new JLabel("", JLabel.CENTER);

    /**
     * Used satellites label and value.
     */
    private JLabel dopLabel = new JLabel("", JLabel.CENTER);

    /**
     * Date and time formatter.
     */
    private DateFormat dateFormat, timeFormat;

    /**
     * Data values.
     */
    private JLabel latitudeValue = new JLabel("");
    /**
     * Data values.
     */
    private JLabel longitudeValue = new JLabel("");
    /**
     * Data values.
     */
    private JLabel dateValue = new JLabel("");
    /**
     * Data values.
     */
    private JLabel timeValue = new JLabel("");
    /**
     * Data values.
     */
    private JLabel speedValue = new JLabel("");
    /**
     * Data values.
     */
    private JLabel altitudeValue = new JLabel("");
    /**
     * Data values.
     */
    private JLabel courseValue = new JLabel("");

    /**
     * Simple constructor.
     */
    public GpsPanel() {
        final int columns = 4;
        this.setLayout(new GridLayout(0, columns));
        this.setBackground(UIManager.getColor("TaskPaneGroup.background"));
        this.add(fixQualityLabel);
        this.add(usedSatellitesText);
        this.add(usedSatellitesLabel);
        this.add(dopLabel);
        this.add(latitudeText);
        this.add(latitudeValue);
        this.add(longitudeText);
        this.add(longitudeValue);
        this.add(dateText);
        this.add(dateValue);
        this.add(timeText);
        this.add(timeValue);
        this.add(speedText);
        this.add(speedValue);
        this.add(courseText);
        this.add(courseValue);
        this.add(altitudeText);
        this.add(altitudeValue);
        this.dateFormat = DateFormat.getDateInstance(DateFormat.SHORT); //new SimpleDateFormat("yyyy.MM.dd");
        this.dateFormat.setTimeZone(TimeZone.getDefault());
        this.timeFormat = DateFormat.getTimeInstance(DateFormat.LONG); //new SimpleDateFormat("HH:mm:ss");
        this.timeFormat.setTimeZone(TimeZone.getDefault());
        LOG.log(Level.INFO, "Initialize TimeZone = " + this.timeFormat.getTimeZone());
    }

    /**
     * Change widget text if GPS location is changed.
     *
     * @param lat Latitude
     * @param lon Longitude
     *
     * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener#gpsLocationChanged(double, double)
     */
    public void gpsLocationChanged(final double lat, final double lon) {
        this.latitudeValue.setText(this.formatCoordinate(lat, MainFrame.RESOURCE.getChar("GPSPanel.compass.N"), MainFrame.RESOURCE.getChar("GPSPanel.compass.S")));
        this.longitudeValue.setText(this.formatCoordinate(lon, MainFrame.RESOURCE.getChar("GPSPanel.compass.E"), MainFrame.RESOURCE.getChar("GPSPanel.compass.W")));
    }

    /**
     * Converts coordinates from double to string representation.
     * e.g. from <tt>45.45454545 E</tt> degrees to <tt>45\u00b027.273' E</tt>
     *
     * @param f degree decimal value
     * @param posDue Northern or Eastern hemisphere symbol
     * @param negDue Southern or Western hemisphere symbol
     * @return formatted string representation
     */
    protected String formatCoordinate(final double f, final char posDue, final char negDue) {
        char due = negDue;
        if (f > 0) {
            due = posDue;
        }
        double val = (double) Math.abs(f);
        int deg = (int) val;
        double min = (val - deg) * UnitConstants.MINUTES_PER_DEGREE;
        String minf = Math.round(min * PRECISION) / PRECISION + "'" + due;

        String degStr = "" + deg;
        if (degStr.length() < 2) {
            degStr = "0" + degStr;
        }
        String minStr =  "" + minf;
        if (minStr.length() < 2) {
            minStr = "0" + minStr;
        }
        return degStr + "\u00b0 " + minStr;
    }

    /**
     * Invoke if NMEA stream was lost.
     */
    public void gpsLocationLost() {
        // change icon represented connected, not connected, on-fix
        fixQualityLabel.setOpaque(true);
        fixQualityLabel.setBackground(Color.RED);
        fixQualityLabel.setForeground(Color.BLACK);
        fixQualityLabel.setText(MainFrame.RESOURCE.getString("GPSPanel.Labels.NoFix"));
    }

    /**
     * Change widget text if GPS date or time was changed.
     *
     * @param date Date
     * @param time Time
     *
     * @see org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener#gpsLocationChanged(double, double)
     */
    public void gpsDateTimeChanged(final long date, final long time) {
        String dateStr = "";
        if (date > 0) {
            dateStr = this.dateFormat.format(new Date(date));
        }
        String timeStr = this.timeFormat.format(new Date(time));
        this.dateValue.setText(dateStr);
        this.timeValue.setText(timeStr);
    }

    /**
     * Change gps fix quality string.
     * @param fixQuality fix quality value
     */
    public void gpsFixQualityChanged(final int fixQuality) {
        fixQualityLabel.setOpaque(true);
        if (fixQuality == 0) {
            fixQualityLabel.setBackground(Color.RED);
            fixQualityLabel.setForeground(Color.BLACK);
            fixQualityLabel.setText(MainFrame.RESOURCE.getString("GPSPanel.Labels.NoFix"));
        } else if (fixQuality == 1) {
            fixQualityLabel.setForeground(Color.BLACK);
            fixQualityLabel.setBackground(Color.GREEN);
            fixQualityLabel.setText(MainFrame.RESOURCE.getString("GPSPanel.Labels.GPSFix"));
        } else if (fixQuality == 2) {
            fixQualityLabel.setForeground(Color.BLACK);
            fixQualityLabel.setBackground(Color.BLUE);
            fixQualityLabel.setText(MainFrame.RESOURCE.getString("GPSPanel.Labels.DGPSFix"));
        }
    }

    /**
     * ${@inheritDoc}.
     */
    public void gpsLocationObtained() {
        fixQualityLabel.setForeground(Color.BLACK);
        fixQualityLabel.setBackground(Color.GREEN);
        fixQualityLabel.setText(MainFrame.RESOURCE.getString("GPSPanel.Labels.GPSFix"));
    }

    /**
     * ${@inheritDoc}.
     */
    public void gpsAltitudeChanged(final double altitude) {
        this.altitudeValue.setText(Math.round(altitude) + " m");
    }

    /**
     * ${@inheritDoc}.
     */
    public void gpsCourseChanged(final double course) {
        this.courseValue.setText(Math.round(course) + "\u00b0 ");
    }

    /**
     * ${@inheritDoc}.
     */
    public void gpsSpeedChanged(final double speed) {
        this.speedValue.setText(Math.round(speed * UnitConstants.MILES_TO_KM) + " km/h");
    }

    /**
     * ${@inheritDoc}.
     */
    public void gpsDopChanged(final double hdop, final double vdop, final double pdop) {
        // HDOP/PDOP/VDOP are untranslated names
        if (pdop > 0) {
            dopLabel.setText(pdop + " PDOP");
        } else if (vdop > 0) {
            dopLabel.setText(vdop + " VDOP");
        } else if (hdop > 0) {
            dopLabel.setText(hdop + " HDOP");
        }
    }

    /**
     * ${@inheritDoc}.
     */
    public void gpsUsedSattelitesChanged(final int satellites) {
        usedSatellitesLabel.setText(satellites + "");
    }
}
