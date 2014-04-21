/**
 * 
 */
package org.openstreetmap.travelingsalesman.gui;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.sql.SQLException;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.UIManager;

import org.openstreetmap.osm.SettingsDialog;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.hsqldb.DatabaseContext;
import org.openstreetmap.osm.data.searching.IAddressDBPlaceFinder;
import org.openstreetmap.osm.data.searching.advancedAddressDB.AdvancedAddressDBPlaceFinder;
import org.openstreetmap.travelingsalesman.actions.DetectGPSPortAction;
import org.openstreetmap.travelingsalesman.actions.DownloadMenu;
import org.openstreetmap.travelingsalesman.actions.FileBugInBrowserAction;
import org.openstreetmap.travelingsalesman.actions.LoadMapFileActionListener;
import org.openstreetmap.travelingsalesman.actions.LoadTrackFileAction;
import org.openstreetmap.travelingsalesman.actions.OpenWikiInBrowserAction;
import org.openstreetmap.travelingsalesman.actions.ReindexOsmBinAction;
import org.openstreetmap.travelingsalesman.actions.RunGarbageCollectionAction;
import org.openstreetmap.travelingsalesman.actions.ShowGPSFileControls;
import org.openstreetmap.travelingsalesman.actions.ShowTrafficMessagesAction;
import org.openstreetmap.travelingsalesman.gui.widgets.DebugLogPanel;
import org.openstreetmap.travelingsalesman.gui.wizard.ConfigWizard;

/**
 * Menu of Traveling Salesman.<br/>
 * Used in {@link MainFrame}.<br/>
 * It has been refactored into it�s own
 * class to reduce the complexity of
 * the {@link MainFrame}-class.<br/>
 * This class is not in the widgets-package
 * as it is not a reusable widget that can be
 * used outside of Traveling Salesman.
 */
public class TSMenuBar extends JMenuBar {

    /**
     * generated.
     */
    private static final long serialVersionUID = 8957965651780833550L;

    /**
     * The menu with the operations that manipulate the map.
     */
    private JMenu myMapMenu = null;

    /**
     * The menu with debug-operations.
     */
    private JMenu myDebugMenu = null;

    /**
     * The download-menu.
     */
    private JMenu myDownloadMenu;

    /**
     * Your usual help-menu.
     */
    private JMenu myHelpMenu;

    /**
     * Menu item for gps port detecting feature.
     */
    private JMenuItem myDetectGpsMenuItem;
    /**
     * Menu item for gps port detecting feature.
     */
    private JMenuItem myGPSFileControlsMenuItem;

    /**
     * MenuItem to download the currently visible area.
     */
    private JMenuItem myDownloadMapMenuItem = null;

    /**
     * MenuItem to load a map-file.
     */
    private JMenuItem myLoadMapFileMenuItem = null;


    /**
     * MenuItem to load a track-file.
     */
    private JMenuItem myLoadTrackFileMenuItem = null;

    /**
     * MenuItem to exit the application.
     */
    private JMenuItem myExitMenuItem = null;

    /**
     * Menu to edit preferences.
     */
    private JMenu mySettingsMenu = null;

    /**
     * MenuItem to edit preferences.
     */
    private JMenuItem myPreferencesMenuItem = null;

    /**
     * MenuItem to edit preferences.
     */
    private JMenuItem myConfigWizardMenuItem = null;

    /**
     * MenuItem to open the database for debugging.
     */
    private JMenuItem myDBManagerMenu = null;

    /**
     * MenuItem to show-debug-log.
     */
    private JMenuItem myDebugLogMenu = null;

    /**
     * MenuItem to force a garbage-collection -run.
     */
    private JMenuItem myGarbageCollectionMenuItem = null;

    /**
     * MenuItem to reindex the AddressData-database.
     * @see IAddressDBPlaceFinder
     */
    private JMenuItem myAddressReindexMenu = null;

    /**
     * MenuItem to reindex the map.
     * @see ReindexOsmBinAction
     */
    private JMenuItem myOsmBinIndexMenuItem = null;

//    /**
//     * The menu-item to open a database as a map.
//     */
//    private JMenuItem myDatabaseMenuItem = null;

    /**
     * The menu-item to open a frame with all traffic-messages received.
     */
    private JMenuItem myShowTrafficMessagesMenuItem;

    /**
     * The main-Window of Traveling Salesman that we belong to.
     */
    private MainFrame myMainFrame;

    /**
     * Construct the menu-bar with it�s submenus.
     * @param aMainFrame The main-Window of Traveling Salesman that we belong to.
     */
    public TSMenuBar(final MainFrame aMainFrame) {
        this.myMainFrame = aMainFrame;

        setName("mainMenu");
//      use the same background-color that the task-pane uses at the bottom
        if (UIManager.getBoolean("TaskPane.useGradient")) {
            setBackground(UIManager.getColor("TaskPane.backgroundGradientStart"));
        } else {
            setBackground(UIManager.getColor("TaskPane.background"));
        }
        add(getMapMenu());
        add(getSettingsMenu());
        add(getDownloadMenu());
        add(Box.createHorizontalGlue());
        add(getHelpMenu());
        add(getDebugMenu());
    }


    /**
     * This method initializes MapMenu.
     *
     * @return javax.swing.JMenu
     */
    private JMenu getMapMenu() {
        if (myMapMenu == null) {
            myMapMenu = new JMenu();
            myMapMenu.setName("Map");
            myMapMenu.setText(MainFrame.RESOURCE.getString("Main.Menu.Map"));
            myMapMenu.setMnemonic(KeyEvent.VK_M);
            myMapMenu.add(getDownloadMapMenuItem());
            myMapMenu.add(getLoadMapFileMenuItem());
            myMapMenu.add(getLoadTrackFileMenuItem());
            //no longer used myMapMenu.add(getDatabaseMenuItem());
            myMapMenu.add(getExitMenuItem());
        }
        return myMapMenu;
    }

    /**
     * This method initializes PreferencesMenu.
     *
     * @return javax.swing.JMenu
     */
    private JMenu getSettingsMenu() {
        if (mySettingsMenu == null) {
            mySettingsMenu = new JMenu();
            mySettingsMenu.setName("settings");
            mySettingsMenu.setText(MainFrame.RESOURCE.getString("Main.Menu.Settings"));
            mySettingsMenu.setToolTipText(MainFrame.RESOURCE.getString("Main.Menu.Settings.ToolTip"));
            mySettingsMenu.add(getConfigWizardMenuItem());
            mySettingsMenu.add(getPreferencesMenuItem());
        }
        return mySettingsMenu;
    }

    /**
     * @return the download-menu.
     */
    private JMenu getDownloadMenu() {
        if (myDownloadMenu == null) {
            myDownloadMenu = new JMenu(MainFrame.RESOURCE.getString("Main.Menu.Download"));
            myDownloadMenu.add(new DownloadMenu(getMainFrame(), "http://download.geofabrik.de/osm/europe/germany/", "Germany", false));
            myDownloadMenu.add(new DownloadMenu(getMainFrame(), "http://download.geofabrik.de/osm/europe/", "Europe", false));
            myDownloadMenu.add(new DownloadMenu(getMainFrame(), "http://downloads.cloudmade.com/", "World", false));
        }
        return myDownloadMenu;
    }

    /**
     * @return the help-menu.
     */
    @Override
    public JMenu getHelpMenu() {
        if (myHelpMenu == null) {
            myHelpMenu = new JMenu(MainFrame.RESOURCE.getString("Main.Menu.Help"));
            myHelpMenu.add(new JMenuItem(new FileBugInBrowserAction()));
            myHelpMenu.add(new JMenuItem(new OpenWikiInBrowserAction()));
        }
        return myHelpMenu;
    }

    /**
     * This method initializes MapMenu.
     *
     * @return javax.swing.JMenu
     */
    private JMenu getDebugMenu() {
        if (myDebugMenu == null) {
            myDebugMenu = new JMenu();
            myDebugMenu.setName("Debug");
            myDebugMenu.setText(MainFrame.RESOURCE.getString("Main.Menu.Debug"));
            myDebugMenu.setMnemonic(KeyEvent.VK_D);
            myDebugMenu.add(getDebugLogMenuItem());
            myDebugMenu.add(getDBManagerMenuItem());
            myDebugMenu.add(getAddressIndexMenuItem());
            myDebugMenu.add(getOsmBinIndexMenuItem());
            myDebugMenu.add(getGarbageCollectionMenuItem());
            myDebugMenu.add(getDetectGpsPortMenuItem());
            myDebugMenu.add(getShowGPSFileControlsMenuItem());
            myDebugMenu.add(getShowTrafficMessagesMenuItem());
        }
        return myDebugMenu;
    }


    /**
     * @return the menu-item to open a frame with all traffic-messages received.
     */
    private JMenuItem getShowTrafficMessagesMenuItem() {
        if (this.myShowTrafficMessagesMenuItem == null) {
            this.myShowTrafficMessagesMenuItem = new JMenuItem(new ShowTrafficMessagesAction(getMainFrame().getMapPanel()));
        }
        return this.myShowTrafficMessagesMenuItem;
    }

    /**
     * @return The MenuItem to show the current log.
     */
    private JMenuItem getDebugLogMenuItem() {
        if (myDebugLogMenu == null) {
            myDebugLogMenu = new JMenuItem();
            myDebugLogMenu.setName("Debug-Log");
            myDebugLogMenu.setText(MainFrame.RESOURCE.getString("Main.Menu.Debug.Log"));
            myDebugLogMenu.addActionListener(new ActionListener() {

                public void actionPerformed(final ActionEvent arg0) {
                    JFrame f = new JFrame(MainFrame.RESOURCE.getString("Main.Menu.Debug.Log"));
                    f.getContentPane().add(new DebugLogPanel());
                    f.pack();
                    f.setVisible(true);
                }
            });
        }
        return myDebugLogMenu;
    }

    /**
     * @return The MenuItem to force a garbage-collection -run.
     */
    private JMenuItem getGarbageCollectionMenuItem() {
        if (myGarbageCollectionMenuItem == null) {
            myGarbageCollectionMenuItem = new JMenuItem(new RunGarbageCollectionAction(getMainFrame().getStatusBarLabel()));
        }
        return myGarbageCollectionMenuItem;
    }

    /**
     * @return The MenuItem to show the current database.
     */
    private JMenuItem getDBManagerMenuItem() {
        if (myDBManagerMenu == null) {
            myDBManagerMenu = new JMenuItem();
            myDBManagerMenu.setName("DBManager");
            myDBManagerMenu.setText(MainFrame.RESOURCE.getString("Main.Menu.Debug.DBManager"));
            myDBManagerMenu.addActionListener(new ActionListener() {

                public void actionPerformed(final ActionEvent arg0) {
                    (new Thread() {
                        public void run() {
                            /*org.hsqldb.util.*/ // TODO: Check equal functionality! 
                        	try {
								org.h2.tools.Console.main(new String[] {"--driver", "org.hsqldb.jdbcDriver",
								                                                   "--url", DatabaseContext.getDefaultURL(),
								                                                   "--user", "sa",
								                                                   "--password", ""});
							} catch (SQLException e) {
								throw new RuntimeException(e);
							}
                        }
                    }).start();
                }
            });
        }
        return myDBManagerMenu;
    }

    /**
     * @return The MenuItem to show the current database.
     */
    private JMenuItem getOsmBinIndexMenuItem() {
        if (myOsmBinIndexMenuItem == null) {
            myOsmBinIndexMenuItem = new JMenuItem();
            myOsmBinIndexMenuItem.setName("reindex map");
            myOsmBinIndexMenuItem.setText(MainFrame.RESOURCE.getString("Main.Menu.Debug.ReindexMap"));
            myOsmBinIndexMenuItem.addActionListener(new ReindexOsmBinAction(getMainFrame()));
        }
        return myOsmBinIndexMenuItem;
    }

    /**
     * @return The MenuItem to show the current database.
     */
    private JMenuItem getAddressIndexMenuItem() {
        if (myAddressReindexMenu == null) {
            myAddressReindexMenu = new JMenuItem();
            myAddressReindexMenu.setName("reindex addresses");
            myAddressReindexMenu.setText(MainFrame.RESOURCE.getString("Main.Menu.Debug.ReindexAddresses"));
            myAddressReindexMenu.addActionListener(new ActionListener() {

                public void actionPerformed(final ActionEvent arg0) {
                    (new Thread() {
                        public void run() {
                            IAddressDBPlaceFinder addresses = new AdvancedAddressDBPlaceFinder();
                            IDataSet map = getMainFrame().getOsmData();
                            addresses.setMap(map);
                            if (map != null)
                                addresses.indexDatabase(map);
                        }
                    }).start();
                }
            });
        }
        return myAddressReindexMenu;
    }

    /**
     * @return Show controls to influence NMEA/GPX-reading.
     */
    private JMenuItem getShowGPSFileControlsMenuItem() {
        if (myGPSFileControlsMenuItem == null) {
            myGPSFileControlsMenuItem = new JMenuItem(new ShowGPSFileControls(getMainFrame().getGPSProvider()));
        }
        return myGPSFileControlsMenuItem;
    }

    /**
     * @return Show detected gps port in console.
     */
    private JMenuItem getDetectGpsPortMenuItem() {
        if (myDetectGpsMenuItem == null) {
            myDetectGpsMenuItem = new JMenuItem(new DetectGPSPortAction(getMainFrame().getStatusBarLabel()));
        }
        return myDetectGpsMenuItem;
    }

    /**
     * This method initializes DownloadMapMenuItem.
     *
     * @return javax.swing.JMenuItem
     */
    protected JMenuItem getDownloadMapMenuItem() {
        if (myDownloadMapMenuItem == null) {
            myDownloadMapMenuItem = new JMenuItem();
            myDownloadMapMenuItem.setMnemonic(KeyEvent.VK_D);
            myDownloadMapMenuItem.setText(MainFrame.RESOURCE.getString("Main.Menu.Map.Download"));
            myDownloadMapMenuItem.setToolTipText(MainFrame.RESOURCE.getString("Main.Menu.Map.Download.ToolTip"));
            myDownloadMapMenuItem.setName("download map");
            myDownloadMapMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(final ActionEvent arg0) {
                    (new Thread() {
                        public void run() {
                            getMainFrame().downloadVisibleArea();
                        }
                    }).start();
                }
            });
        }
        return myDownloadMapMenuItem;
    }

    /**
     * This method initializes LoadMapFileMenuItem.
     *
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getLoadMapFileMenuItem() {
        if (myLoadMapFileMenuItem == null) {
            myLoadMapFileMenuItem = new JMenuItem();
            myLoadMapFileMenuItem.setMnemonic(KeyEvent.VK_O);
            myLoadMapFileMenuItem.setText(MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile"));
            myLoadMapFileMenuItem.setToolTipText(MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.ToolTip"));
            myLoadMapFileMenuItem.setName("load map file");
            myLoadMapFileMenuItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(final ActionEvent aE) {
                    ActionListener delegate = new LoadMapFileActionListener((Frame) getMainFrame(),
                                                                                    getMainFrame().getStatusBarLabel(),
                                                                                    getMainFrame().getJProgressBar(),
                                                                                    getMainFrame().getOsmData());
                    delegate.actionPerformed(aE);
                }
            });
        }
        return myLoadMapFileMenuItem;
    }

    /**
     * This method initializes LoadTrackFileMenuItem.
     *
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getLoadTrackFileMenuItem() {
        if (myLoadTrackFileMenuItem == null) {
            myLoadTrackFileMenuItem = new JMenuItem(new LoadTrackFileAction(getMainFrame().getTracksPanel().getTracksStorage()));
            myLoadTrackFileMenuItem.setMnemonic(KeyEvent.VK_T);
        }
        return myLoadTrackFileMenuItem;
    }

    /**
     * This method initializes ExitMenuItem.
     *
     * @return javax.swing.JMenuItem
     */
    protected JMenuItem getExitMenuItem() {
        if (myExitMenuItem == null) {
            myExitMenuItem = new JMenuItem();
            myExitMenuItem.setMnemonic(KeyEvent.VK_X);
            myExitMenuItem.setText(MainFrame.RESOURCE.getString("Main.Menu.Map.Exit"));
            myExitMenuItem.setName("exit");
            // the action-listener will be added by MainFrame#getMainMenu()
        }
        return myExitMenuItem;
    }

    /**
     * This method initializes configWizardMenuItem.
     *
     * @return javax.swing.JMenu
     */
    private JMenuItem getConfigWizardMenuItem() {
        if (myConfigWizardMenuItem == null) {
            myConfigWizardMenuItem = new JMenuItem();
            myConfigWizardMenuItem.setName("configwizard");
            myConfigWizardMenuItem.setText(MainFrame.RESOURCE.getString("Main.Menu.ConfigWizard"));
            myConfigWizardMenuItem.setToolTipText(MainFrame.RESOURCE.getString("Main.Menu.ConfigWizard.ToolTip"));
            myConfigWizardMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    IDataSet oldData = getMainFrame().getOsmData();
                    getMainFrame().setOsmData(new MemoryDataSet());
                    if (oldData != null) {
                        oldData.shutdown();
                    }
                    ConfigWizard wiz = new ConfigWizard();
                    wiz.setModal(true);
                    wiz.pack();
                    wiz.setVisible(true);
                    if (wiz.getDataSet() != null) {
                        getMainFrame().setOsmData(wiz.getDataSet());
                    }
                }
            });
        }
        return myConfigWizardMenuItem;
    }

    /**
     * This method initializes PreferencesMenu.
     *
     * @return javax.swing.JMenu
     */
    private JMenuItem getPreferencesMenuItem() {
        if (myPreferencesMenuItem == null) {
            myPreferencesMenuItem = new JMenuItem();
            myPreferencesMenuItem.setName("preferences");
            myPreferencesMenuItem.setText(MainFrame.RESOURCE.getString("Main.Menu.Preferences"));
            myPreferencesMenuItem.setToolTipText(MainFrame.RESOURCE.getString("Main.Menu.Preferences.ToolTip"));
            myPreferencesMenuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(final ActionEvent e) {
                    SettingsDialog dlg = new SettingsDialog();
                    dlg.setVisible(true);
                }
            });
        }
        return myPreferencesMenuItem;
    }


//    /**
//     * This method initializes DatabaseMenuItem.
//     *
//     * @return javax.swing.JMenuItem
//     */
//    private JMenuItem getDatabaseMenuItem() {
//        if (myDatabaseMenuItem == null) {
//            myDatabaseMenuItem = new JMenuItem();
//            myDatabaseMenuItem.setText(MainFrame.RESOURCE.getString("Main.Menu.Map.OpenDB"));
//            myDatabaseMenuItem.addActionListener(new java.awt.event.ActionListener() {
//                public void actionPerformed(final java.awt.event.ActionEvent e) {
//                    Settings prefs = Settings.getInstance();
//                    String host = prefs.get("database.host", "localhost");
//                    host = JOptionPane.showInputDialog(MainFrame.RESOURCE.getString("Main.Menu.Map.OpenDB.HostQuestion"), host);
//                    prefs.put("database.host", host);
//
//                    String name = prefs.get("database.name", "osm");
//                    name = JOptionPane.showInputDialog(MainFrame.RESOURCE.getString("Main.Menu.Map.OpenDB.NameQuestion"), name);
//                    prefs.put("database.name", name);
//
//                    String user = prefs.get("database.user", "osm");
//                    user = JOptionPane.showInputDialog(MainFrame.RESOURCE.getString("Main.Menu.Map.OpenDB.UserQuestion"), user);
//                    prefs.put("database.user", user);
//
//                    String password = prefs.get("database.password", "");
//                    password = JOptionPane.showInputDialog(MainFrame.RESOURCE.getString("Main.Menu.Map.OpenDB.PasswordQuestion"), password);
//                    prefs.put("database.password", password);
//
//                    DatabaseLoginCredentials credentials = new DatabaseLoginCredentials(host, name, user, password, false, false);
//                    getMainFrame().setOsmData(new CachingDataSet(new DBDataSet(credentials)));
//                }
//            });
//        }
//        return myDatabaseMenuItem;
//    }


    /**
     * @return The main-Window of Traveling Salesman that we belong to.
     */
    public MainFrame getMainFrame() {
        return myMainFrame;
    }
}
