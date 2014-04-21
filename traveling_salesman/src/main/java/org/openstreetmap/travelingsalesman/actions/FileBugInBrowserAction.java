/**
 * FileBugInBrowserAction.javaTransactionMenuAction.java
 * created: 15.11.2008
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

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.openstreetmap.travelingsalesman.gui.MainFrame;



/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: Traveling Salesman<br/>
 * FileBugInBrowserAction<br/>
 * created: 15.11.2008 <br/>
 *<br/><br/>
 * <b>Open the system-web-browser to file a bug-report.</b>
 * @author  <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class FileBugInBrowserAction implements Action {


    /**
     * Our logger for debug- and error-output.
     */
    private static final Logger LOG = Logger.getLogger(FileBugInBrowserAction.class.getName());

    /**
     * Initialize this action.
     */
    public FileBugInBrowserAction() {
        this.putValue(Action.NAME, "File bug-report...");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent aE) {
        try {
            final URL fileABugURL = new URL(
                    "http://travelingsales.sourceforge.net"
                    + "/bugs/bug_report_advanced_page.php?"
                    + "product_version=" + java.net.URLEncoder.encode(MainFrame.RESOURCE.getString("Main.Version"), "UTF8")
                    + "&build=unknown"
                    + "&platform=" + java.net.URLEncoder.encode(System.getProperty("os.arch"), "UTF8")
                    + "&os=" + java.net.URLEncoder.encode(System.getProperty("os.name"), "UTF8")
                    + "&os_build=" + java.net.URLEncoder.encode(System.getProperty("os.version"), "UTF8")
                    );
            showDocument(fileABugURL);
        } catch (MalformedURLException e) {
            LOG.log(Level.SEVERE, "Error, cannot launch web browser", e);
            JOptionPane.showMessageDialog(null, "Error, cannot launch web browser:\n"
                    + e.getLocalizedMessage());
        } catch (UnsupportedEncodingException e) {
            LOG.log(Level.SEVERE, "Error, UTF8 not supported", e);
            JOptionPane.showMessageDialog(null, "Error, UTF8 is not supported:\n"
                    + e.getLocalizedMessage());
        }
    }
    /**
     * reference to browser-starter.
     */
    private static Object myJNLPServiceManagerObject;

    /**

     * Open the given URL in the default-browser.
     * @param url the URL to open
     * @return false if it did not work
     */
    @SuppressWarnings("unchecked")
    public static boolean  showDocument(final URL url) {

        if (myJNLPServiceManagerObject == null) {
            myJNLPServiceManagerObject = getJNLPServiceManagerObject();
        }

        // we cannot use JNLP -> make an educated guess
        if (myJNLPServiceManagerObject == null) {
            try {
                String osName = System.getProperty("os.name");
                if (osName.startsWith("Mac OS")) {
                    Class fileMgr = Class.forName("com.apple.eio.FileManager");
                    Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] {String.class});
                    openURL.invoke(null, new Object[] {url});
                } else if (osName.startsWith("Windows")) {
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else { //assume Unix or Linux
                    String[] browsers = {"x-www-browser",
                            "firefox", "iceweasle",
                            "opera", "konqueror",
                            "epiphany", "mozilla", "netscape" };
                    String browser = null;
                    for (int count = 0;
                    count < browsers.length && browser == null;
                    count++) {
                        if (Runtime.getRuntime().exec(new String[] {"which",
                                browsers[count]}).waitFor() == 0) {
                            browser = browsers[count];
                        }
                        if (browser == null) {
                            return false;
                        } else {
                            Runtime.getRuntime().exec(new String[] {browser, url.toString()});
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Error attempting to launch web browser natively.", e);
                JOptionPane.showMessageDialog(null, "Error attempting to launch web browser:\n"
                        + e.getLocalizedMessage());
            }
        }



        if (myJNLPServiceManagerObject != null) {
            try {
                Method  method = myJNLPServiceManagerObject.getClass().getMethod("showDocument", new Class[] {URL.class});
                Boolean  resultBoolean = (Boolean)
                method.invoke(myJNLPServiceManagerObject, new Object[] {url});
                return resultBoolean.booleanValue();
            } catch (Exception  ex) {
                JOptionPane.showMessageDialog(null, "Error attempting to launch web browser:\n"
                        + ex.getLocalizedMessage());
            }
        }

        return false;
    }


    /**
     * @return instance of "javax.jnlp.ServiceManager" on platforms
     * that support it.
     */
    @SuppressWarnings("unchecked")
    private static Object getJNLPServiceManagerObject() {
        try {
            Class  serviceManagerClass = Class.forName("javax.jnlp.ServiceManager");
            Method lookupMethod        = serviceManagerClass.getMethod("lookup",
                    new Class[] {String.class});
            return lookupMethod.invoke(null, new Object[]{"javax.jnlp.BasicService"});
        } catch (Exception  ex) {
            LOG.log(Level.INFO, "Cannot instanciate javax.jnlp.ServiceManager "
                    + "- this platform seems not to support it.", ex);
            return null;
        }
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
