/**
 * DebugLogPanel.java
 * created: 19.01.2008 09:09:18
 * (c) 2008 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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
package org.openstreetmap.travelingsalesman.gui.widgets;

//other imports

//automatically created logger for debug and error -output
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

//automatically created propertyChangeListener-Support
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLEditorKit;

import org.openstreetmap.travelingsalesman.gui.MainFrame;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: traveling_salesman<br/>
 * DebugLogPanel.java<br/>
 * created: 19.01.2008 09:09:18 <br/>
 *<br/><br/>
 * This panel is to show the logging-output for debugging.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class DebugLogPanel extends JPanel {

    /**
     * Prefered size of this panel.
     */
    private static final Dimension PREFERRED_SIZE = new Dimension(1000, 400);

    /**
     * generated.
     */
    private static final long serialVersionUID = 8071290727935083730L;

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(
                                            DebugLogPanel.class.getName());

    /**
     * The area we are logging to.
     */
    private JTextPane myLogArea = new JTextPane();
    /**
     * The area we are logging to.
     */
    private JTextPane myInstructionsArea = new JTextPane();

    /**
     * Initialize the panel.
     */
    public DebugLogPanel() {
        this.setLayout(new BorderLayout());
        this.myInstructionsArea.setEditorKit(new HTMLEditorKit());
        this.myInstructionsArea.setText(MainFrame.RESOURCE.getString("DebugLogPanel.Instructions"));
        this.myLogArea.setText("");
        this.myLogArea.setStyledDocument(new DefaultStyledDocument());
        this.myLogArea.setPreferredSize(PREFERRED_SIZE);
        this.add(this.myInstructionsArea, BorderLayout.NORTH);
        this.add(new JScrollPane(this.myLogArea), BorderLayout.CENTER);

        Logger.getLogger("").addHandler(new Handler() {

            @Override
            public void close() {
                try {
                    myLogArea.getDocument().insertString(myLogArea.getDocument().getLength(), "\n***LOGGING STOPPED***", null);
                } catch (BadLocationException e) {
                    LOG.log(Level.SEVERE, "[BadLocationException] Problem in "
                               + getClass().getName(),
                                 e);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void publish(final LogRecord aRecord) {
                try {
                    // decide color or message
                    SimpleAttributeSet attrs = new SimpleAttributeSet();
                    if (aRecord.getLevel() == Level.FINEST)
                        StyleConstants.setForeground(attrs, Color.LIGHT_GRAY);
                    if (aRecord.getLevel() == Level.FINER)
                        StyleConstants.setForeground(attrs, Color.GRAY);
                    if (aRecord.getLevel() == Level.FINE)
                        StyleConstants.setForeground(attrs, Color.GRAY);
                    if (aRecord.getLevel() == Level.WARNING)
                        StyleConstants.setForeground(attrs, Color.ORANGE);
                    if (aRecord.getLevel() == Level.SEVERE)
                          StyleConstants.setForeground(attrs, Color.RED);

                    // print message
                    myLogArea.getDocument().insertString(
                            myLogArea.getDocument().getLength(),
                            "\n" + aRecord.getLevel() + " "
                            + "in " + aRecord.getSourceClassName() + ":" + aRecord.getSourceMethodName() + "(...) "
                            + aRecord.getMessage(), attrs);

                    // print stack-trace
                    if (aRecord.getThrown() != null) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        aRecord.getThrown().printStackTrace(pw);
                        myLogArea.getDocument().insertString(
                                myLogArea.getDocument().getLength(),
                                "\n" + sw.getBuffer(), attrs);
                    }
                } catch (BadLocationException e) {
                    LOG.log(Level.SEVERE, "[BadLocationException] Problem in "
                               + getClass().getName(),
                                 e);
                }
            }
            });
    }

    //------------------------ support for propertyChangeListeners ------------------

    /**
     * support for firing PropertyChangeEvents.
     * (gets initialized only if we really have listeners)
     */
    private volatile PropertyChangeSupport myPropertyChange = null;

    /**
     * Returned value may be null if we never had listeners.
     * @return Our support for firing PropertyChangeEvents
     */
    protected PropertyChangeSupport getPropertyChangeSupport() {
        return myPropertyChange;
    }

    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     *
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(
            final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property.  The listener
     * will be invoked only when a call on firePropertyChange names that
     * specific property.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(final String propertyName,
            final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The PropertyChangeListener to be removed
     */
    public final void removePropertyChangeListener(final String propertyName,
            final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(propertyName,
                    listener);
        }
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     *
     * @param listener  The PropertyChangeListener to be removed
     */
    public synchronized void removePropertyChangeListener(
            final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(listener);
        }
    }

    //-------------------------------------------------------

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "DebugLogPanel@" + hashCode();
    }
}


