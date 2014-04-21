/**
 * RunGarbageCollectionAction.java
 * created: 22.12.2009
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
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JLabel;
import org.openstreetmap.travelingsalesman.gui.MainFrame;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: Traveling Salesman<br/>
 * RunGarbageCollectionAction<br/>
 * created: 22.12.2009 <br/>
 *<br/><br/>
 * <b>Manually force a run of the garbage-collecion.</b>
 * @author  <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class RunGarbageCollectionAction implements Action {


    /**
     * Our logger for debug- and error-output.
     */
    private static final Logger LOG = Logger.getLogger(RunGarbageCollectionAction.class.getName());

    /**
     * a status-bar label for feedback.
     */
    private JLabel myStatusBar;

    /**
     * @param aStatusBarLabel a status-bar label for feedback
     */
    public RunGarbageCollectionAction(final JLabel aStatusBarLabel) {
        this.putValue(Action.NAME, MainFrame.RESOURCE.getString("Main.Menu.Debug.ForceGarbageCollection"));
        this.myStatusBar = aStatusBarLabel;
    }

    /**
     * ${@inheritDoc}.
     */
    public void actionPerformed(final ActionEvent arg0) {
        Thread gcThread = new Thread() {
            public void run() {
                if (myStatusBar != null) {
                    myStatusBar.setText(MainFrame.RESOURCE.getString("Main.Menu.Debug.ForceGarbageCollection.status.running"));
                }
                try {
                    System.gc();
                } finally {
                    if (myStatusBar != null) {
                        myStatusBar.setText(MainFrame.RESOURCE.getString("Main.Menu.Debug.ForceGarbageCollection.status.done"));
                    }
                }
            }
        };
        gcThread.start();
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

