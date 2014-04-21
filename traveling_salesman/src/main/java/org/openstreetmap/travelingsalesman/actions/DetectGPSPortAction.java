/**
 * DetectGPSPortAction.java
 * created: 04.04.2009 18:56:57
 * (c) 2009 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of traveling_salesman by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
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
package org.openstreetmap.travelingsalesman.actions;

//other imports
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.openstreetmap.travelingsalesman.gps.JGPSProvider;
import org.openstreetmap.travelingsalesman.gps.jgps.GPSChecker;
import org.openstreetmap.travelingsalesman.gps.jgps.GpsFinder;
import org.openstreetmap.travelingsalesman.gui.MainFrame;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: Traveling Salesman<br/>
 * FileBugInBrowserAction<br/>
 * created: 15.11.2008 <br/>
 *<br/><br/>
 * <b>Action to detect the Serial Port the GPS-unit it attached to and configure the
 * {@link JGPSProvider}..</b>
 * @author  <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class DetectGPSPortAction implements Action {


    /**
     * Our logger for debug- and error-output.
     */
    private static final Logger LOG = Logger.getLogger(DetectGPSPortAction.class.getName());

    /**
     * a status-bar label for feedback.
     */
    private JLabel myStatusBar;

    /**
     * @param aStatusBarLabel a status-bar label for feedback
     */
    public DetectGPSPortAction(final JLabel aStatusBarLabel) {
        this.putValue(Action.NAME, MainFrame.RESOURCE.getString("Main.Menu.Debug.DetectGpsPort"));
        this.myStatusBar = aStatusBarLabel;
    }

    /**
     * ${@inheritDoc}.
     */
    public void actionPerformed(final ActionEvent arg0) {
        Thread detectThread = new Thread() {
            public void run() {
                if (myStatusBar != null) {
                    myStatusBar.setText(MainFrame.RESOURCE.getString("Main.Menu.Debug.DetectGpsPort.status.running"));
                }
                try {
                    if (!GPSChecker.isRxtxInstalled()) {
                        GPSChecker.installRxtx();
                    }
                    String ports = GpsFinder.findGpsPort();
                    if (ports.length() > 0) {
                        JOptionPane.showMessageDialog(null, "Detected serial ports with NMEA stream :\n" + ports);
                    } else {
                        JOptionPane.showMessageDialog(null, "Serial port with GPS is not detected.", "GPS is not detected", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Throwable e) {
                    LOG.log(Level.SEVERE, "Error detecting GPS-port", e);
                } finally {
                    if (myStatusBar != null) {
                        myStatusBar.setText(MainFrame.RESOURCE.getString("Main.Menu.Debug.DetectGpsPort.status.done"));
                    }
                }
            }
        };
        detectThread.start();
    }

    ////////////////////////////
    /**
     * Used for {@link PropertyChangeListener}s.
     */
    private PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

    /**
     * Used for {@link #putValue(String, Object)}.
     */
    private Map<String, Object> myValues = new HashMap<String, Object>();

    /**
     * ${@inheritDoc}.
     */
    public void addPropertyChangeListener(final PropertyChangeListener aListener) {
        this.myPropertyChangeSupport.addPropertyChangeListener(aListener);
    }


    /**
     * ${@inheritDoc}.
     */
    public Object getValue(final String aKey) {
        return myValues.get(aKey);
    }


    /**
     * ${@inheritDoc}.
     */
    public boolean isEnabled() {
        return true;
    }


    /**
     * ${@inheritDoc}.
     */
    public void putValue(final String aKey, final Object aValue) {
        Object old = myValues.put(aKey, aValue);
        myPropertyChangeSupport.firePropertyChange(aKey, old, aValue);
    }


    /**
     * ${@inheritDoc}.
     */
    public void removePropertyChangeListener(final PropertyChangeListener aListener) {
        this.myPropertyChangeSupport.removePropertyChangeListener(aListener);
    }


     /**
      * ${@inheritDoc}.
      */
     public void setEnabled(final boolean aB) {
        // ignored
    }

}

