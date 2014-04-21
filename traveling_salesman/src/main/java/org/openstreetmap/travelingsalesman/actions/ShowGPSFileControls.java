/**
 * ShowGPSFileControls.javaTransactionMenuAction.java
 * created: 03.03.2009
 * (c) 2008 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of Traveling Salesman by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  jgnucashLib-GPL is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  jgnucashLib-GPL is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with jgnucashLib-V1.  If not, see <http://www.gnu.org/licenses/>.
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

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import org.openstreetmap.travelingsalesman.gps.GPXFileProvider;
import org.openstreetmap.travelingsalesman.gps.IGPSProvider;
import org.openstreetmap.travelingsalesman.gui.MainFrame;

/**
 * Show a frame with controls to command the {@link GPXFileProvider} and
 * {@link NMEAFileProvider} (pause, faster, slower, change file, restart,...).
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */

public class ShowGPSFileControls extends JFrame implements Action {

    /**
     * generated.
     */
    private static final long serialVersionUID = 8406536502323351864L;

    /**
     * Our logger for debug- and error-output.
     */
    private static final Logger LOG = Logger.getLogger(ShowGPSFileControls.class.getName());

    /**
     * The IGPSProvider that we shall control.
     */
    private IGPSProvider myGPSProvider;

    /**
     * Initialize this action.
     * @param aGPSProvider the IGPSProvider that we shall control
     */
    public ShowGPSFileControls(final IGPSProvider aGPSProvider) {
        this.myGPSProvider = aGPSProvider;
        this.putValue(Action.NAME, MainFrame.RESOURCE.getString("Actions.ShowGPX.Label"));
        setTitle("GPX/NMEA-file");
        getContentPane().setLayout(new GridLayout(0, 1));
        getContentPane().add(getChangeFileButton());
        getContentPane().add(getSlowerButton());
        getContentPane().add(getPauseButton());
        getContentPane().add(getFasterButton());
    }


    /**
     * @return the button to halt/resume playing the gpx/nmea-file
     */
    private JButton getPauseButton() {
        final JButton pausebutton = new JButton("stop");
        pausebutton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent aE) {
                if (myGPSProvider instanceof GPXFileProvider) {
                    GPXFileProvider gpx = (GPXFileProvider) myGPSProvider;
                    if (!pausebutton.getText().equals(">")) {
                        gpx.setSleepBetweenFixes(Integer.MAX_VALUE);
                        pausebutton.setEnabled(false);
                    }
                }
            }
        }
        );
        return pausebutton;
    }

    /**
     * @return the button to make it drive faster.
     */
    private JButton getFasterButton() {
        JButton fasterbutton = new JButton(">>");
        fasterbutton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent aE) {
                if (myGPSProvider instanceof GPXFileProvider) {
                    GPXFileProvider gpx = (GPXFileProvider) myGPSProvider;
                    gpx.setSleepBetweenFixes(gpx.getSleepBetweenFixes() / 2);
                    LOG.info("sleep-time for gpx is now " + gpx.getSleepBetweenFixes() + "ms");
                } else {
                    LOG.info("unknown GPS-plugin");
                }
            }
        }
        );
        return fasterbutton;
    }

    /**
     * @return the button to make it drive slower.
     */
    private JButton getSlowerButton() {
        JButton slowerbutton = new JButton("<<");
        slowerbutton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent aE) {
                if (myGPSProvider instanceof GPXFileProvider) {
                    GPXFileProvider gpx = (GPXFileProvider) myGPSProvider;
                    gpx.setSleepBetweenFixes(gpx.getSleepBetweenFixes() * 2);
                    LOG.info("sleep-time for gpx is now " + gpx.getSleepBetweenFixes() + "ms");
                } else {
                    LOG.info("unknown GPS-plugin");
                }
            }
        }
        );
        return slowerbutton;
    }

    /**
     * @return the button to change to another file
     */
    private JButton getChangeFileButton() {
        JButton changeFileButton = new JButton("file...");
        changeFileButton.setEnabled(false);
        return changeFileButton;
    }


    /**
     * {@inheritDoc}
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(final ActionEvent aE) {
        this.pack();
        this.setVisible(true);
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
        return myGPSProvider instanceof GPXFileProvider;
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
