/**
 * DownloadMenu.java
 * created: 25.12.2007 18:53:28
 * (c) 2007 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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


//automatically created logger for debug and error -output
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//automatically created propertyChangeListener-Support
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.travelingsalesman.gui.MainFrame;

/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: traveling_salesman<br/>
 * DownloadMenu.java<br/>
 * created: 25.12.2007 18:53:28 <br/>
 *<br/><br/>
 * This menu gets the mini-planets from a wiki-page
 * and offers to download them.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class DownloadMenu extends JMenu implements MenuListener {

    /**
     * Executor for our {@link SubMenuLoader}, to make sure we do not hit the ftp-servers
     * too hard.
     */
    private static final ThreadPoolExecutor SUBMENULOADEREXECUTOR = new ThreadPoolExecutor(1, 2, 1, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(Short.MAX_VALUE));

    /**
     * Runnable we use for async-download of the list of sub-directories
     * on the download-sites.
     */
    private final class SubMenuLoader implements Runnable {

        /**
         * @param aLoadChildren shall children be loaded recursively and asynchronously?
         */
        public SubMenuLoader(final boolean aLoadChildren) {
            super();
            myLoadChildren = aLoadChildren;
        }

        /**
         * shall children be loaded recursively and asynchronously?
         */
        private boolean myLoadChildren;

        /**
         * {@inheritDoc}
         */
        public void run() {
            try {
                //download
                URL url = new URL(myListURL);
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        url.openStream()));
                String line = null;

                Pattern folderPattern = Pattern.compile(".*DIR.*<a href=.(.*)/.>(.*)</a>.*");
                Pattern folderPatternCloudmake = Pattern.compile(".*<li><a href=./(.*)#breadcrumbs. class=.folder.>(.*)</a></li>.*");
                Pattern filePatternCloudmake = Pattern.compile(".*<a href=.(.*).bz2. class=.default.>(.*).osm(.*).bz2</a>&nbsp;<span class=\"file-size\">(.*)</span>.*");
                while ((line = reader.readLine()) != null) {
                    //----------- handle cloudmake-website
                    Matcher matcher = folderPattern.matcher(line);
                    if (matcher.matches()) {
                        String dirUrl = myListURL + matcher.group(1) + "/";
                        String name = matcher.group(2);
                        if (name.equalsIgnoreCase("Parent Directory")) {
                            continue;
                        }
                        DownloadMenu.this.add(new DownloadMenu(DownloadMenu.this.myMainFrame, dirUrl, name, myLoadChildren));
                        continue;
                    }
                    matcher = folderPatternCloudmake.matcher(line);
                    if (matcher.matches()) {
                        String dirUrl = myListURL.substring(0, myListURL.indexOf(".com/") + ".com/".length()) + matcher.group(1);
                        String name = matcher.group(2);
                        DownloadMenu.this.add(new DownloadMenu(DownloadMenu.this.myMainFrame, dirUrl, name, myLoadChildren));
                        continue;
                    }

                    matcher = filePatternCloudmake.matcher(line);
                    if (matcher.matches()) {
                        String fileUrl = myListURL.substring(0, myListURL.indexOf(".com") + ".com".length()) + matcher.group(1) + ".bz2";
                        final int typeAt = 3;
                        final int nameAt = 2;
                        String type = matcher.group(typeAt);
                        String name = matcher.group(nameAt);
                        if (type.length() > 0) {
                            if (type.startsWith(".")) {
                                type = type.substring(1);
                            }
                            name += "-" + type;
                        }
                        JMenuItem subMenu = new JMenuItem(name);
                        subMenu.addActionListener(new DownloadActionListener(fileUrl,
                                name));
                        subMenu.putClientProperty("URL", fileUrl);
                        add(subMenu);
                        continue;
                    }

                    //----------- handle geofabrik-website
                    // ignore all lines that do not contain a link
                    int index = line.indexOf("<a href=\"");
                    if (index < 0) {
                        continue;
                    }
                    index += "<a href=\"".length();

                    int index2 = line.indexOf("</a");
                    if (index2 < 0) {
                        continue;
                    }

                    int index1 = line.indexOf(".osm.bz2\">");
                    if (index1 < 0) {
                        continue;
                    }
                    index1 += ".osm.bz2".length();

                    String fileUrl = line.substring(index, index1);
                    if (!fileUrl.contains(".osm"))
                        continue;
                    if (!fileUrl.startsWith("http"))
                        fileUrl = myListURL + fileUrl;

                    index1 += "\">".length();
                    String fileName = line.substring(index1, index2);

                    JMenuItem subMenu = new JMenuItem(fileName);
                    subMenu.addActionListener(new DownloadActionListener(fileUrl,
                            fileName));
                    subMenu.putClientProperty("URL", fileUrl);
                    add(subMenu);
                }
            } catch (Exception e) {
                //e.printStackTrace();
                LOG.log(Level.SEVERE, "[Exception] Problem in "
                        + getClass().getName(), e);
            }
            LOG.info("Done with async download of list of downloadable maps for " + getText() + "...");
            remove(isLoadingMenuItem);
            if (myTreeNode != null) {
                myTreeNode.doneLoading();
            }
        }
    }

    /**
     * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
     * Project: traveling_salesman<br/>
     * DownloadMenu.java<br/>
     * created: 25.12.2007 19:42:53 <br/>
     *<br/><br/>
     * ActionListener for the sub-menu-items.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private final class DownloadActionListener implements ActionListener {
        /**
         * the url to download.
         */
        private String myFileUrl;

        /**
         * @param aFileUrl the url to download
         * @param aFileName the name given to that file
         */
        public DownloadActionListener(final String aFileUrl, final String aFileName) {
            this.myFileUrl = aFileUrl;
        }

        /**
         * Asyncronously download the given URL into
         * {@link #DownloadMenu#myMainFrame}
         * ${@inheritDoc}.
         */
        public void actionPerformed(final ActionEvent arg0) {
            Thread t = new Thread() {
                public void run() {
                    try {
                        myMainFrame.loadMapFile(new URL(myFileUrl));
                    } catch (Exception e) {
                        //e.printStackTrace();
                        LOG.log(Level.SEVERE,
                                "[Exception] Problem in "
                                        + getClass().getName(), e);
                    }
                }
            };
            t.start();
        }
    }


    /**
     * Our DownloadMenu.java.
     * @see long
     */
    private static final long serialVersionUID = 1L;

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger  LOG = Logger.getLogger(
                                                DownloadMenu.class.getName());

    /**
     * The frame we are working with.
     */
    private MainFrame myMainFrame;

    /**
     * The url with pointers to the downloadable files.
     */
    private String myListURL =
        "http://download.geofabrik.de/osm/europe/germany/";

    /**
     * Runnable we use to asynchronously load our sub-menus.
     */
    private Runnable loadSubMenus;

    /**
     * @see #loadSubMenus.
     */
    private Future<?> loadSubMenusFuture;

    /**
     * Menu item to show while we are loading the submenus.
     */
    private JMenuItem isLoadingMenuItem = new JMenuItem(MainFrame.RESOURCE.getString("Main.Menu.Download.updating"));

    /**
     * Create the menu-item.
     * @param aMainFrame The frame we are working with.
     * @param aListURL the URL with the .osm.bz2-files to offer.
     * @param name the name to display for this menu
     * @param loadChildren automatically load the children
     */
    public DownloadMenu(final MainFrame aMainFrame, final String aListURL, final String name, final boolean loadChildren) {
        super(name);
        super.addMenuListener(this);
        this.myMainFrame = aMainFrame;
        this.myListURL = aListURL;
        if (loadChildren) {
            menuSelected(null);
        }
    }

    /**
     * Create the menu-item.
     * @param aListURL the URL with the .osm.bz2-files to offer.
     * @param name the name to display for this menu
     */
    public DownloadMenu(final String aListURL, final String name) {
        super(name);
        super.addMenuListener(this);
        this.myListURL = aListURL;
    }

    /**
     * ${@inheritDoc}.
     */
    public void menuCanceled(final MenuEvent arg0) {
       // ignored
    }

    /**
     * ${@inheritDoc}.
     */
    public void menuDeselected(final MenuEvent arg0) {
        // ignored
    }


    /**
     * Populate the sub-menus if needed.
     * ${@inheritDoc}.
     */
    public void menuSelected(final MenuEvent arg0) {

        if (getSubElements().length > 0) {
            return;
        }
        synchronized (this) {
            if (loadSubMenus == null) {
                loadSubMenus = new SubMenuLoader(false);
                Future<?> submited = SUBMENULOADEREXECUTOR.submit(loadSubMenus);
                this.loadSubMenusFuture = submited;
                add(isLoadingMenuItem);
            }
        }

    }

    /**
     * Get a name->value -mapping of all URLs.
     * @return name->URL
     */
    public Map<String, URL> getAllURLs() {
        LOG.info("Downloading list of downloadable maps for " + getText() + "...");
        Map<String, URL> retval = new HashMap<String, URL>();
        Runnable runnable = new SubMenuLoader(false);
        runnable.run();

        for (Component sub : getMenuComponents()) {
            // recurse down
            if (sub instanceof DownloadMenu) {
                DownloadMenu sub2 = (DownloadMenu) sub;
                String name2 = sub2.getText();
                try {
                    Set<Entry<String, URL>> entrySet = sub2.getAllURLs().entrySet();
                    for (Entry<String, URL> entry : entrySet) {
                        if (entry.getKey() != null) {
                            retval.put(name2, entry.getValue());
                        } else {
                            retval.put(name2 + ">" + entry.getKey(), entry.getValue());
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Could not download the list of "
                            + "downloadable maps for " + name2, e);
                }
                continue;
            }
            // handle leaf
            if (sub instanceof JMenuItem) {
                JMenuItem sub2 = (JMenuItem) sub;
                Object url = sub2.getClientProperty("URL");
                if (url != null && url instanceof String) {
                    try {
                        retval.put(sub2.getName(), new URL(url.toString()));
                    } catch (MalformedURLException e) {
                        LOG.log(Level.WARNING, "", e);
                    }
                }
            }
        }
        return retval;
    }

    /**
     * @see #getTreeNode()
     */
    private DownloadingTreeNode myTreeNode = null;

    /**
     * @param aParent the parent of this TreeNode
     * @param aTreeModel model of the tree (DefaultMutableTreeNode does not inform the model about changes)
     * @return this menu as a TreeNode
     */
    public DefaultMutableTreeNode getTreeNode(final TreeNode aParent, final DefaultTreeModel aTreeModel) {
        if (myTreeNode == null) {
            myTreeNode = new DownloadingTreeNode(aTreeModel);
            if (loadSubMenusFuture != null && loadSubMenusFuture.isDone()) {
                myTreeNode.doneLoading();
            }
        }
        return myTreeNode;
    }
    /**
     * Helper-class.
     */
    private class DownloadingTreeNode extends DefaultMutableTreeNode {
        /**
         * generated.
         */
        private static final long serialVersionUID = -589212648447117307L;
        /**
         * Node to show while we are loading.
         */
        private DefaultMutableTreeNode isLoadingTreeNode = null;
        /**
         * model of the tree (DefaultMutableTreeNode does not inform the model about changes).
         */
        private DefaultTreeModel myTreeModel;
        /**
         * Constructor.
         * @param aTreeModel model of the tree (DefaultMutableTreeNode does not inform the model about changes)
         */
        public DownloadingTreeNode(final DefaultTreeModel aTreeModel) {
            super(getText(), true);
            this.myTreeModel = aTreeModel;
        }
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings("unchecked")
        @Override
        public Enumeration children() {
            getChildCount();
            return super.children();
        }
        /**
         * Are we currently loading?
         */
        private boolean isInProgress = false;
        /**
         * {@inheritDoc}
         */
        @Override
        public int getChildCount() {

            if (isInProgress) {
                return super.getChildCount();
            }
            synchronized (this) {
                if (loadSubMenusFuture == null) {
                    isInProgress = true;
                    try {
                        // we have not been loaded
                        LOG.info("Starting async download of list of downloadable maps for " + getText() + "...");
                        loadSubMenus = new SubMenuLoader(false);
                        loadSubMenusFuture = SUBMENULOADEREXECUTOR.submit(loadSubMenus);
                        super.removeAllChildren();
                        isLoadingTreeNode = new DefaultMutableTreeNode(MainFrame.RESOURCE.getString("Main.Menu.Download.updating"));
                        super.add(isLoadingTreeNode);
                    } finally {
                        isInProgress = false;
                    }
                    return super.getChildCount();
                }
                if (!loadSubMenusFuture.isDone()) {
                    // we are loading
                    if (super.getChildCount() == 0) {
                        isInProgress = true;
                        isLoadingTreeNode = new DefaultMutableTreeNode(MainFrame.RESOURCE.getString("Main.Menu.Download.updating"));
                        super.add(isLoadingTreeNode);
                        isInProgress = false;
                    }
                    return super.getChildCount();
                }
            } // synchronized
            return super.getChildCount();
        }
        /**
         * Called by {@link SubMenuLoader}.
         */
        private void doneLoading() {
            isInProgress = true;
            try {
                if (isLoadingTreeNode != null) {
                    myTreeModel.removeNodeFromParent(isLoadingTreeNode);
                    //super.remove(isLoadingTreeNode);
                    isLoadingTreeNode = null;
                }
                Component[] menuComponents = getMenuComponents();
                for (Component component : menuComponents) {
                    if (component instanceof DownloadMenu) {
                        DownloadMenu dl = (DownloadMenu) component;
                        //super.add(dl.getTreeNode(this, this.myTreeModel));
                        myTreeModel.insertNodeInto(dl.getTreeNode(this, this.myTreeModel),
                                this, getChildCount());
                    } else {
                        JMenuItem item = (JMenuItem) component;
                        Object url = item.getClientProperty("URL");
                        if (url != null && url instanceof String) {
                            try {
                                URL url2 = new URL(url.toString());
                                myTreeModel.insertNodeInto(new LeafTreeNode(this, item.getText(), url2),
                                        this, getChildCount());
                                //super.add(new LeafTreeNode(this, item.getText(), new URL(url.toString())));
                            } catch (Exception e) {
                                LOG.log(Level.WARNING, "", e);
                            }
                        }
                    }
                }
            } finally {
                isInProgress = false;
            }
//            TreeModelListener[] treeModelListeners = myTreeModel.getTreeModelListeners();
//            for (TreeModelListener treeModelListener : treeModelListeners) {
//                TreeModelEvent event = new TreeModelEvent(this, getPath());
//                treeModelListener.treeNodesChanged(event);
//            }
        }
    }

    /**
     * TreeNode for our leafs when using {@link DownloadMenu#getTreeNode(TreeNode)}.
     */
    public static class LeafTreeNode extends DefaultMutableTreeNode {

        /**
         * generated.
         */
        private static final long serialVersionUID = -7615974558397564780L;
//        /**
//         * Our parent tree-node.
//         */
//        private TreeNode myParent;
        /**
         * What to display as this leafs name.
         */
        private String myName;
        /**
         * The download-URL we represent.
         */
        private URL myUrl;
        /**
         * @param aTreeNode Our parent tree-node.
         * @param aName What to display as this leafs name.
         * @param aUrl The download-URL we represent.
         */
        public LeafTreeNode(final TreeNode aTreeNode, final String aName, final URL aUrl) {
            super(aName, false);
            if (aUrl == null) {
                throw new IllegalArgumentException("null URL given");
            }
            if (aName == null) {
                throw new IllegalArgumentException("null name given");
            }
//            this.myParent = aTreeNode;
            this.myName = aName;
            this.myUrl = aUrl;
        }
//        /**
//         * {@inheritDoc}
//         */
//        @Override
//        public TreeNode getParent() {
//            return myParent;
//        }
        /**
         * {@inheritDoc}
         */
        public String toString() {
            return myName;
        }
        /**
         * @return the URL
         */
        public URL getURL() {
            return myUrl;
        }
    }
}
