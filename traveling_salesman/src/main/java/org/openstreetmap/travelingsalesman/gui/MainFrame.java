/**
 * MainFrame.java
 * This file is part of Traveling-Salesman by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  Traveling-Salesman is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Traveling-Salesman is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Traveling-Salesman.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.DBDataSet;
import org.openstreetmap.osm.data.DataSetFilter;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.Selector;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.projection.Projection;
import org.openstreetmap.osm.data.searching.AddressDBPlaceFinder;
import org.openstreetmap.osm.data.searching.IAddressDBPlaceFinder;
import org.openstreetmap.osm.data.searching.IPlaceFinder;
import org.openstreetmap.osm.data.searching.InetPlaceFinder;
import org.openstreetmap.osm.data.searching.Place;
import org.openstreetmap.osm.data.searching.advancedAddressDB.AdvancedAddressDBPlaceFinder;
import org.openstreetmap.osm.data.visitors.BoundingXYVisitor;
import org.openstreetmap.osm.io.BoundingBoxDownloader;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.actions.AddDestinationAction;
import org.openstreetmap.travelingsalesman.actions.LoadMapFileActionListener;
import org.openstreetmap.travelingsalesman.actions.OpenInJosmAction;
import org.openstreetmap.travelingsalesman.actions.RefreshVisibleTilesAction;
import org.openstreetmap.travelingsalesman.actions.SetOdrPainter;
import org.openstreetmap.travelingsalesman.actions.SetTilePainter;
import org.openstreetmap.travelingsalesman.actions.ShowTrailAction;
import org.openstreetmap.travelingsalesman.gps.DummyGPSProvider;
import org.openstreetmap.travelingsalesman.gps.IGPSProvider;
import org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener;
import org.openstreetmap.travelingsalesman.gps.data.NmeaTrackExporter;
import org.openstreetmap.travelingsalesman.gps.gpsdemulation.MiniGPSD;
import org.openstreetmap.travelingsalesman.gui.widgets.GpsPanel;
//import org.openstreetmap.travelingsalesman.gui.widgets.IntersectionPanel;
import org.openstreetmap.travelingsalesman.gui.widgets.IsometricMapPanel;
import org.openstreetmap.travelingsalesman.gui.widgets.JShowToolTipsMenuItem;
import org.openstreetmap.travelingsalesman.gui.widgets.MapPanel;
import org.openstreetmap.travelingsalesman.gui.widgets.PlaceFinderPanel;
import org.openstreetmap.travelingsalesman.gui.widgets.RouteInstructionPanel;
import org.openstreetmap.travelingsalesman.gui.widgets.SimpleMapPanel;
import org.openstreetmap.travelingsalesman.gui.widgets.TracksPanel;
import org.openstreetmap.travelingsalesman.navigation.NavigationManager;
import org.openstreetmap.travelingsalesman.routing.IProgressListener;
import org.openstreetmap.travelingsalesman.routing.IRouteChangedListener;
import org.openstreetmap.travelingsalesman.routing.IVehicle;
import org.openstreetmap.travelingsalesman.routing.Route;
import org.openstreetmap.travelingsalesman.routing.selectors.Motorcar;
import org.openstreetmap.travelingsalesman.routing.selectors.UsedTags;
import org.openstreetmap.travelingsalesman.trafficblocks.TMCLocationIndexer;

import sun.awt.VerticalBagLayout;

import com.l2fprod.common.swing.JTaskPane;
import com.l2fprod.common.swing.JTaskPaneGroup;
import com.l2fprod.common.swing.plaf.LookAndFeelAddons;
import com.l2fprod.common.swing.plaf.windows.WindowsLookAndFeelAddons;
import com.l2fprod.common.util.ResourceManager;

/**
 * The main-frame of the Traveling Salesman -application.
 * 
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a> See also:
 *         {@link TSMenuBar}
 */
public class MainFrame extends JFrame implements IRouteChangedListener {

	/**
	 * Size of the buffer when reading from a URL.
	 */
	// private static final int READBUFFERSIZE = 255;

	/**
	 * The default used for the zoom-level in {@link #jumpTo(Place)}.
	 */
	private static final double DEFAULTZOOMLEVEL = 6.4E-6;

	/**
	 * The default width to download between each start+target.
	 */
	private static final double DEFAULTSTRIPDOWNLOADWIDTH = 0.125d;

	/**
	 * Default main windows width.
	 */
	public static final int DEFAULTMAINFRAMEWIDTH = 750;

	/**
	 * Default main windows height.
	 */
	public static final int DEFAULTMAINFRAMEHEIGHT = 550;

	/**
	 * Default autosave value.
	 */
	private static final boolean DEFAULTRESTORESTATEVALUE = true;

	/**
	 * my logger for debug and error-output.
	 */
	private static final Logger LOG = Logger.getLogger(MainFrame.class
			.getName());

	/**
	 * Out resource-manager for i18n.
	 */
	public static final ResourceManager RESOURCE = ResourceManager
			.get(MainFrame.class);

	/**
	 * For serializing his frame.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The Panel that hosts everything except the menubar.
	 */
	private JPanel jContentPane = null;

	/**
	 * The panel that draws the map.
	 */
	private MapPanel mapPanel = null;

	/**
	 * The panel that draws the map.
	 */
	private IsometricMapPanel mapPanelIsometric = null;

	/**
	 * The NavigationManager to simplify implementing the calculation and
	 * updating of the route as the driver moves along.
	 */
	private NavigationManager myNavigationManager = new NavigationManager();

	/**
	 * We are capable of emulating a GPSD for other applications.
	 */
	private MiniGPSD myGPSDemulation = new MiniGPSD();

	/**
	 * The plugin that provides our position.
	 */
	private IGPSProvider myGPSProvider = null;

	/**
	 * The gps track logger to nmea file.
	 */
	private NmeaTrackExporter myGpsTrackLogger = null;

	/**
	 * The task-pane that make up the left side of the UI.
	 */
	private JTaskPane leftPanel = null;

	/**
	 * The panel will all components used for navigtion.
	 */
	private JPanel navigationPanel = null;

	/**
	 * The panel with the placefinder and placesToGo.
	 */
	// private JPanel navigationResultsPanel = null;

	/**
	 * The Place-Finder -component allows to search for targets to navigate to.
	 */
	private PlaceFinderPanel placeFinder = null;

	/**
	 * The list with the targets to navigate to.
	 */
	private JList placesToGo = null;

	/**
	 * The button to start the routing.
	 */
	private JButton routeButton = null;

	/**
	 * The map-data we operate on.
	 */
	private IDataSet osmData; // @jve:decl-index=0:

	/**
	 * The button to download map-parts.
	 */
	private JButton downloadButton = null;

	/**
	 * The button to center on the current position.
	 */
	private JButton centerOnGPSButton = null;

	/**
	 * The button to switch-on auto-center mode.
	 */
	private JToggleButton autoCenterModeButton = null;

	/**
	 * The button to switch-on auto-center mode.
	 */
	private JToggleButton autoRotationModeButton = null;

	/**
	 * The button to show trail track.
	 */
	private JToggleButton showTrailButton = null;

	/**
	 * The panel with the driving-instructions.
	 */
	private RouteInstructionPanel routeInstructionsPanel = null;

	// /**
	// * The panel with the nearest intersection on the route..
	// */
	// private IntersectionPanel intersectionPanel = null;

	/**
	 * The panel with the gps data parsed from NMEA stream.
	 */
	private GpsPanel myGpsPanel = null;

	/**
	 * The panel with loaded tracks.
	 */
	private TracksPanel myTracksPanel = null;

	/**
	 * The menubar.
	 */
	private TSMenuBar myMainMenu = null;
	/**
	 * The selector says what roads we can drive on.
	 */
	private IVehicle myVehicle = Settings.getInstance().getPluginProxy(
			IVehicle.class, Motorcar.class.getName());

	/**
	 * The selector says what features will show at the rendered map.
	 */
	private Selector myMapFeaturesSelector = new UsedTags();

	/**
	 * A panel {@link #placesToGo}.
	 */
	private JPanel myPlacesToGoPanel = null;

	/**
	 * The label to display next to {@link #myStatusBarLabel} in
	 * {@link #myStatusBar}.
	 */
	private JProgressBar myProgressBar = null;

	/**
	 * The status-bar with the {@link #myStatusBarLabel} and
	 * {@link #myProgressBar} to display at the bottom of the screen.
	 */
	private JPanel myStatusBar = null;

	/**
	 * The label to display next to {@link #myProgressBar} in
	 * {@link #myStatusBar}.
	 */
	private JLabel myStatusBarLabel = null;

	/**
	 * the JTabbedPane that contains. the mapPanels.
	 * 
	 * @see #mapPanel
	 * @see #mapPanelIsometric
	 */
	private JTabbedPane mapPanels;

	/**
	 * This is the default constructor.
	 */
	public MainFrame() {
		super();
		initialize();
		LOG.log(Level.INFO,
				"[JRE] Java version : " + System.getProperty("java.version"));
		LOG.log(Level.INFO,
				"[JRE] Java home : " + System.getProperty("java.home"));
	}

	/**
	 * This method initializes this.
	 */
	private void initialize() {
		UIManager.put("win.xpstyle.name", "luna");
		// Workaround for Sun-Bug 4226498 from 1999
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4226498
		UIManager.put("ProgressBar.selectionBackground", Color.DARK_GRAY);
		// UIManager.put("ProgressBar.selectionForeground", Color.BLACK);
		try {
			LookAndFeelAddons.setAddon(WindowsLookAndFeelAddons.class);
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Cannot set Look&Feel!", e);
		}

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle(RESOURCE.getString("Main.Frame.Title") + "-"
				+ RESOURCE.getString("Main.Version"));
		myGPSProvider = Settings.getInstance().getPluginProxy(
				IGPSProvider.class, DummyGPSProvider.class.getName());

		this.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(final WindowEvent winEvt) {
				Settings settings = Settings.getInstance();
				if (settings.getBoolean("traveling-salesman.restoreLastState",
						DEFAULTRESTORESTATEVALUE)) {
					LOG.log(Level.FINE,
							"TravelingSalesman is closing. Saving state...");
					settings.put("state.MainFrame.width",
							String.valueOf(winEvt.getWindow().getWidth()));
					settings.put("state.MainFrame.height",
							String.valueOf(winEvt.getWindow().getHeight()));
					settings.put("state.MainFrame.x",
							String.valueOf(winEvt.getWindow().getX()));
					settings.put("state.MainFrame.y",
							String.valueOf(winEvt.getWindow().getY()));
					// settings.put("state.MapPanel.east",
					// String.valueOf(MainFrame.this.getMapPanel().getCenter().east()));
					// settings.put("state.MapPanel.north",
					// String.valueOf(MainFrame.this.getMapPanel().getCenter().north()));
					final LatLon mapPanelCenter = getMapPanel().getProjection()
							.eastNorth2latlon(getMapPanel().getCenter());
					settings.put("state.MapPanel.latitude",
							String.valueOf(mapPanelCenter.lat()));
					settings.put("state.MapPanel.longitude",
							String.valueOf(mapPanelCenter.lon()));
					settings.put(
							"state.MapPanel.scale",
							String.valueOf(getMapPanel().getScale()
									* getMapPanel().getProjection()
											.scaleFactor()));
					// settings.put("state.MapPanelIsometric.east",
					// String.valueOf(MainFrame.this.getMapPanelIsometric().getCenter().east()));
					// settings.put("state.MapPanelIsometric.north",
					// String.valueOf(MainFrame.this.getMapPanelIsometric().getCenter().north()));
					final LatLon isometricPanelCenter = getMapPanelIsometric()
							.getProjection().eastNorth2latlon(
									getMapPanelIsometric().getCenter());
					settings.put("state.MapPanelIsometric.latitude",
							String.valueOf(isometricPanelCenter.lat()));
					settings.put("state.MapPanelIsometric.longitude",
							String.valueOf(isometricPanelCenter.lon()));
					settings.put("state.MapPanelIsometric.scale",
							String.valueOf(getMapPanelIsometric().getScale()));
					settings.put("state.MapPanel.selectedMap",
							String.valueOf(getMapPanels().getSelectedIndex()));
				}

				final IDataSet currentData = getOsmData();
				if (currentData != null) {
					currentData.shutdown();
				}
				try {
					IPlaceFinder finder = Settings.getInstance()
							.getPlugin(IPlaceFinder.class,
									InetPlaceFinder.class.getName());
					if (finder instanceof AdvancedAddressDBPlaceFinder) {
						AdvancedAddressDBPlaceFinder.shutdown();
					} else if (finder instanceof AddressDBPlaceFinder) {
						AddressDBPlaceFinder.shutdown();
					}
				} catch (Exception e) {
					LOG.log(Level.SEVERE, "Cannot shut down Address-Database.",
							e);
				}

				System.exit(0);
			}
		});

		Settings settings = Settings.getInstance();
		int width = DEFAULTMAINFRAMEWIDTH;
		int height = DEFAULTMAINFRAMEHEIGHT;
		int x = 0;
		int y = 0;
		if (settings.getBoolean("traveling-salesman.restoreLastState",
				DEFAULTRESTORESTATEVALUE)) {
			width = settings.getInteger("state.MainFrame.width",
					DEFAULTMAINFRAMEWIDTH);
			height = settings.getInteger("state.MainFrame.height",
					DEFAULTMAINFRAMEHEIGHT);
			x = settings.getInteger("state.MainFrame.x", 0);
			y = settings.getInteger("state.MainFrame.y", 0);
		}
		this.setPreferredSize(new Dimension(width, height));
		this.setBounds(x, y, width, height);
		this.setJMenuBar(getMainMenu());
		this.setContentPane(getJContentPane());
		this.pack();

		myGPSProvider.addGPSListener(new IGPSListener() {

			public void gpsLocationChanged(final double aLat, final double aLon) {
				getMapPanel().setCurrentPosition(new LatLon(aLat, aLon));
				getMapPanelIsometric().setCurrentPosition(
						new LatLon(aLat, aLon));
				getCenterOnGPSButton().setEnabled(true);
				getAutoCenterModeButton().setEnabled(true);
				getAutoRotateModeButton().setEnabled(true);
			}

			public void gpsLocationLost() {
				// getMapPanel().setCurrentPosition(null);
				getCenterOnGPSButton().setEnabled(false);
				getAutoCenterModeButton().setEnabled(false);
				getAutoRotateModeButton().setEnabled(false);
			}

			public void gpsLocationObtained() {
				// Startup Nmea gps logger
				if (Settings.getInstance().getBoolean(
						"traveling-salesman.nmeaLogger", true)
						&& myGpsTrackLogger == null) {
					myGpsTrackLogger = new NmeaTrackExporter();
					myGPSProvider.addGPSListener(myGpsTrackLogger);
				}
			}

			public void gpsCourseChanged(final double course) {
				// ignored
			}
		});
		this.myGPSProvider.addGPSListener(getRouteInstructionsPanel());
		// this.myGPSProvider.addGPSListener(getIntersectionPanel());

		// GPSD-emulation
		this.myGPSProvider.addGPSListener(myGPSDemulation);

		// navigation-system
		this.myNavigationManager.setSelector(myVehicle);
		this.myGPSProvider.addGPSListener(myNavigationManager);
		this.myNavigationManager.addProgressListener(new IProgressListener() {
			public void progressMade(final double aDone, final double aTotal,
					final Node here) {
				getJProgressBar().setMaximum(Integer.MAX_VALUE);
				getJProgressBar().setValue(
						(int) (aDone / aTotal * Integer.MAX_VALUE));

				getStatusBarLabel()
						.setText(
								MessageFormat.format(
										RESOURCE.getString("Main.StatusBar.RoutingPercentDone"),
										new Object[] { NumberFormat
												.getPercentInstance().format(
														aDone / aTotal) }));

				// show the user what we do while we are routing.
				// Helps with debugging routers too.
				if (here != null) {
					getMapPanel()
							.setNextManeuverPosition(
									new LatLon(here.getLatitude(), here
											.getLongitude()));
				}
			}
		});

		// user-interface
		this.myNavigationManager.addRouteChangedListener(this);
		this.myGPSProvider.addGPSListener(mapPanel);
		// this.myGPSProvider.addGPSListener(mapPanelIsometric);
		// TODO Clock synchronizer
		// ClockSynchronizer cs = new ClockSynchronizer(myGPSProvider);
		// cs.synchronizeClock();
	}

	/**
	 * This method initializes jContentPane.
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			jContentPane = new JPanel();
			jContentPane.setLayout(new BorderLayout(0, 0));
			// use the same background-color that the task-pane uses
			jContentPane.setBackground(UIManager
					.getColor("TaskPane.background"));
			jContentPane.add(getMapPanels(), BorderLayout.CENTER);
			jContentPane
					.add(new JScrollPane(getLeftPanel()), BorderLayout.WEST);
			// component is added once we have a route
			// jContentPane.add(getRouteInstructionsPanel(),
			// BorderLayout.NORTH);
			jContentPane.add(getStatusBar(), BorderLayout.SOUTH);
		}
		return jContentPane;
	}

	/**
	 * This method initializes mapPanelIsometric.
	 * 
	 * @return the MapPanel.
	 */
	private MapPanel getMapPanelIsometric() {
		if (mapPanelIsometric == null) {
			mapPanelIsometric = new IsometricMapPanel();
			// use the same background-color that the task-pane uses
			// mapPanel.setBackground(UIManager.getColor("TaskPane.background"));
			mapPanelIsometric.setLayout(new BorderLayout());
			mapPanelIsometric.setDataSet(getOsmData());
			if (Settings.getInstance().getBoolean(
					"traveling-salesman.restoreLastState",
					DEFAULTRESTORESTATEVALUE)) {
				// final double east =
				// Settings.getInstance().getDouble("state.MapPanelIsometric.east"
				// , mapPanelIsometric.getCenter().east());
				// final double north =
				// Settings.getInstance().getDouble("state.MapPanelIsometric.north",
				// mapPanelIsometric.getCenter().north());
				final double latitude = Settings.getInstance().getDouble(
						"state.MapPanelIsometric.latitude",
						IsometricMapPanel.DEFAULTCENTER.lat());
				final double longitude = Settings.getInstance().getDouble(
						"state.MapPanelIsometric.longitude",
						IsometricMapPanel.DEFAULTCENTER.lon());
				final double scale = Settings.getInstance().getDouble(
						"state.MapPanelIsometric.scale",
						mapPanelIsometric.getScale());
				mapPanelIsometric.zoomTo(mapPanelIsometric.getProjection()
						.latlon2eastNorth(latitude, longitude), scale);
			}
			this.myNavigationManager.addRouteChangedListener(mapPanelIsometric);
		}
		return mapPanelIsometric;
	}

	/**
	 * @return the JTabbedPane that contains. the mapPanels.
	 * @see #mapPanel
	 * @see #mapPanelIsometric
	 */
	private JTabbedPane getMapPanels() {
		if (this.mapPanels == null) {
			this.mapPanels = new JTabbedPane();
			// Isometric needs more debugging
			// this.mapPanels.addTab(RESOURCE.getString("Main.IsometricMap")",
			// getMapPanelIsometric());
			this.mapPanels.addTab(RESOURCE.getString("Main.PlanarMap"),
					getMapPanel());
			if (Settings.getInstance().getBoolean(
					"traveling-salesman.restoreLastState",
					DEFAULTRESTORESTATEVALUE)) {
				this.mapPanels.setSelectedIndex(Settings.getInstance()
						.getInteger("state.MapPanel.selectedMap", 0));
			} else {
				this.mapPanels.setSelectedIndex(0);
			}
		}

		return this.mapPanels;
	}

	/**
	 * This method initializes mapPanel.
	 * 
	 * @return the MapPanel.
	 */
	protected MapPanel getMapPanel() {
		if (mapPanel == null) {
			mapPanel = new MapPanel();
			// use the same background-color that the task-pane uses
			// mapPanel.setBackground(UIManager.getColor("TaskPane.background"));
			// mapPanel.setLayout(null);
			mapPanel.setLayout(new BorderLayout());
			mapPanel.setDataSet(getOsmData());
			final Settings settings = Settings.getInstance();
			if (settings.getBoolean("traveling-salesman.restoreLastState",
					DEFAULTRESTORESTATEVALUE)) {
				// final double east =
				// Settings.getInstance().getDouble("state.MapPanel.east",
				// mapPanel.getCenter().east());
				// final double north =
				// Settings.getInstance().getDouble("state.MapPanel.north",
				// mapPanel.getCenter().north());
				final double latitude = settings.getDouble(
						"state.MapPanel.latitude",
						SimpleMapPanel.DEFAULTCENTER.lat());
				final double longitude = settings.getDouble(
						"state.MapPanel.longitude",
						SimpleMapPanel.DEFAULTCENTER.lon());
				LatLon center = new LatLon(latitude, longitude);
				if (center.isOutSideWorld()) {
					center = SimpleMapPanel.DEFAULTCENTER;
				}
				// carefull this is multiplied scale value
				double scale = settings.getDouble("state.MapPanel.scale",
						mapPanel.getScale()
								* Settings.getProjection().scaleFactor());
				if (scale > MapMover.MAXSCALE) {
					scale = MapMover.MAXSCALE;
				} else if (scale < MapMover.MINSCALE) {
					scale = MapMover.MINSCALE;
				}
				mapPanel.zoomTo(
						mapPanel.getProjection().latlon2eastNorth(center),
						scale / mapPanel.getProjection().scaleFactor());
			}
			this.myNavigationManager.addRouteChangedListener(mapPanel);
			mapPanel.setGpsTracksStorage(getTracksPanel().getTracksStorage());
			getTracksPanel().getTracksStorage().addTracksChangeListener(
					mapPanel);
			mapPanel.addMouseListener(new MouseAdapter() {

				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * java.awt.event.MouseAdapter#mouseReleased(java.awt.event.
				 * MouseEvent)
				 */
				@Override
				public void mouseClicked(final MouseEvent aE) {
					// if (aE.isPopupTrigger()) {
					JPopupMenu contextMenu = new JPopupMenu();
					AddDestinationAction addDestinationAction = new AddDestinationAction(
							mapPanel, myNavigationManager.getSelector(),
							getPlaceFinder());
					addDestinationAction.mouseReleased(aE);
					contextMenu.add(new JMenuItem(addDestinationAction));
					contextMenu.add(new JShowToolTipsMenuItem());
					contextMenu.show(mapPanel, aE.getX(), aE.getY());
					// }
				}
			});
		}
		return mapPanel;
	}

	/**
	 * This method initializes leftPanel.
	 * 
	 * @return javax.swing.JPanel
	 */
	private JTaskPane getLeftPanel() {
		if (leftPanel == null) {
			leftPanel = new JTaskPane();

			JTaskPaneGroup navigationGroup = new JTaskPaneGroup();
			navigationGroup.setTitle(RESOURCE
					.getString("Main.TaskGroups.Navigation.Name"));
			navigationGroup.setToolTipText(RESOURCE
					.getString("Main.TaskGroups.Navigation.ToolTip"));
			navigationGroup.setSpecial(true);
			navigationGroup.setExpanded(true);
			/*
			 * systemGroup.setToolTipText(RESOURCE
			 * .getString("Main.tasks.systemGroup.tooltip"));
			 * systemGroup.setIcon(new ImageIcon(TaskPaneMain.class
			 * .getResource("icons/tasks-email.png")));
			 */
			Container contentPane = navigationGroup.getContentPane();
			// contentPane.setBackground(TASKPANEBGCOLOR);
			contentPane.removeAll();
			contentPane.add(getNavigationPanel());
			getNavigationPanel().setBackground(contentPane.getBackground());
			leftPanel.add(navigationGroup);

			JTaskPaneGroup gpsGroup = new JTaskPaneGroup() {
				/**
				 * Default serial ID.
				 */
				private static final long serialVersionUID = 1L;

				// we don't listen provider, if gps panel is not showing data
				@Override
				public void setExpanded(final boolean expanded) {
					if (myGPSProvider != null) {
						if (expanded) {
							myGPSProvider.addGPSListener(getGpsPanel());
						} else {
							myGPSProvider.removeGPSListener(getGpsPanel());
						}
					}
					super.setExpanded(expanded);
				}
			};
			gpsGroup.setTitle(RESOURCE.getString("Main.TaskGroups.GPS.Name"));
			gpsGroup.setToolTipText(RESOURCE
					.getString("Main.TaskGroups.GPS.ToolTip"));
			gpsGroup.setExpanded(false);
			gpsGroup.add(getGpsPanel());
			leftPanel.add(gpsGroup);

			JTaskPaneGroup editingGroup = new JTaskPaneGroup();
			// contentPane = navigationGroup.getContentPane();
			editingGroup.setTitle(RESOURCE
					.getString("Main.TaskGroups.MapEditing.Name"));
			editingGroup.setToolTipText(RESOURCE
					.getString("Main.TaskGroups.MapEditing.ToolTip"));
			editingGroup.add(new SetTilePainter());
			editingGroup.add(new SetOdrPainter());
			editingGroup.add(new OpenInJosmAction(getActiveMapPanel()));
			editingGroup
					.add(new RefreshVisibleTilesAction(getActiveMapPanel()));
			/*
			 * systemGroup.add(makeAction(RESOURCE.getString("Main.tasks.email"),
			 * "", "icons/tasks-email.png"));
			 */
			editingGroup.setExpanded(false);
			leftPanel.add(editingGroup);

			JTaskPaneGroup tracksGroup = new JTaskPaneGroup();
			tracksGroup.setTitle("Tracks");
			// editingGroup.setTitle(RESOURCE.getString("Main.TaskGroups.MapEditing.Name"));
			// editingGroup.setToolTipText(RESOURCE.getString("Main.TaskGroups.MapEditing.ToolTip"));
			tracksGroup.add(getTracksPanel());
			/*
			 * systemGroup.add(makeAction(RESOURCE.getString("Main.tasks.email"),
			 * "", "icons/tasks-email.png"));
			 */
			tracksGroup.setExpanded(false);
			leftPanel.add(tracksGroup);
		}
		return leftPanel;
	}

	/**
	 * Initialization for GPS data panel.
	 * 
	 * @return GpsPanel
	 */
	private GpsPanel getGpsPanel() {
		if (myGpsPanel == null) {
			myGpsPanel = new GpsPanel();
		}
		return myGpsPanel;
	}

	/**
	 * Initialization for GPS data panel.
	 * 
	 * @return GpsPanel
	 */
	protected TracksPanel getTracksPanel() {
		if (myTracksPanel == null) {
			myTracksPanel = new TracksPanel();
		}
		return myTracksPanel;
	}

	/**
	 * Return places to go panel with buttos and scrolls.
	 * 
	 * @return panel
	 */
	private JPanel getPlacesToGoPanel() {
		if (myPlacesToGoPanel == null) {
			myPlacesToGoPanel = new JPanel(new BorderLayout());
			myPlacesToGoPanel.setBackground(UIManager
					.getColor("TaskPaneGroup.background"));
			myPlacesToGoPanel.add(new JScrollPane(getPlacesToGo()),
					BorderLayout.CENTER);
			final int buttonsRowCount = 3;
			JPanel buttons = new JPanel(new GridLayout(buttonsRowCount, 1));
			// Remove button
			JButton removeButton = new JButton("X");
			removeButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent arg0) {
					int index = getPlacesToGo().getSelectedIndex();
					if (getPlacesToGo().getSelectedValue() != null) {
						((DefaultListModel) getPlacesToGo().getModel())
								.remove(index);
						int size = ((DefaultListModel) getPlacesToGo()
								.getModel()).getSize();
						if (size > 0 && index < size) {
							getPlacesToGo().setSelectedIndex(index);
						} else {
							if (index >= size) {
								getPlacesToGo().setSelectedIndex(index - 1);
							}
						}
						if (getPlacesToGo().getModel().getSize() < 1)
							routeButton.setEnabled(false);
					}
				}
			});
			// Move up button
			JButton upButton = new JButton("\u2191");
			upButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent arg0) {
					int index = getPlacesToGo().getSelectedIndex();
					if (getPlacesToGo().getSelectedValue() != null && index > 0) {
						DefaultListModel model = (DefaultListModel) getPlacesToGo()
								.getModel();
						Place place = (Place) model.remove(index);
						model.add(index - 1, place);
						getPlacesToGo().setSelectedIndex(index - 1);
					}
				}
			});
			// Move down button
			JButton downButton = new JButton("\u2193");
			downButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent arg0) {
					int index = getPlacesToGo().getSelectedIndex();
					if (getPlacesToGo().getSelectedValue() != null
							&& index < getPlacesToGo().getModel().getSize() - 1) {
						DefaultListModel model = (DefaultListModel) getPlacesToGo()
								.getModel();
						Place place = (Place) model.remove(index);
						model.add(index + 1, place);
						getPlacesToGo().setSelectedIndex(index + 1);
					}
				}
			});
			buttons.add(upButton);
			buttons.add(removeButton);
			buttons.add(downButton);
			myPlacesToGoPanel.add(buttons, BorderLayout.EAST);
		}
		return myPlacesToGoPanel;
	}

	/**
	 * This method initializes navigationPanel.
	 * 
	 * @return javax.swing.JPanel.
	 */
	private JPanel getNavigationPanel() {
		if (navigationPanel == null) {
			navigationPanel = new JPanel(new VerticalBagLayout());
			navigationPanel.add(getPlaceFinder());
			navigationPanel.add(getPlacesToGoPanel());
			JPanel buttons = new JPanel();
			buttons.setBackground(UIManager
					.getColor("TaskPaneGroup.background"));
			final int rows = 3;
			buttons.setLayout(new GridLayout(rows, 2));
			buttons.add(getRouteButton(), null);
			buttons.add(getDownloadButton(), null);
			buttons.add(getCenterOnGPSButton(), null);
			buttons.add(getShowTrailButton(), null);
			buttons.add(getAutoCenterModeButton(), null);
			buttons.add(getAutoRotateModeButton(), null);
			navigationPanel.add(buttons);
			// setup minimum size for correct displaying with selected layout
			int minimumWidth = getPlaceFinder().getPreferredSize().width;
			int minimumHeight = getPlaceFinder().getPreferredSize().height
					+ getPlacesToGoPanel().getPreferredSize().height
					+ buttons.getPreferredSize().height;
			Dimension dimension = new Dimension(minimumWidth, minimumHeight);
			navigationPanel.setMinimumSize(dimension);
		}
		return navigationPanel;
	}

	/**
	 * This method initializes placesToGo.
	 * 
	 * @return javax.swing.JList.
	 */
	private JList getPlacesToGo() {
		if (placesToGo == null) {
			final int placesToGoDefaultSize = 200;
			placesToGo = new JList(new DefaultListModel());

			// show the place the user selected
			placesToGo.addListSelectionListener(new ListSelectionListener() {
				public void valueChanged(final ListSelectionEvent arg0) {
					Place place = (Place) placesToGo.getSelectedValue();
					if (place != null) {
						jumpTo(place);
					}
				}
			});

			// remove the selected target uppon the key DELETE being typed
			placesToGo.addKeyListener(new KeyAdapter() {
				public void keyPressed(final KeyEvent key) {
					if (key.getKeyCode() == KeyEvent.VK_DELETE
							&& placesToGo.getSelectedValue() != null) {
						((DefaultListModel) placesToGo.getModel())
								.remove(placesToGo.getSelectedIndex());
						if (placesToGo.getModel().getSize() < 1) {
							routeButton.setEnabled(true);
						}
					}
				}
			});
			final int placesToGOLength = 3;
			placesToGo.setVisibleRowCount(placesToGOLength);
			placesToGo.setPreferredSize(new Dimension(placesToGoDefaultSize,
					placesToGoDefaultSize));
		}
		return placesToGo;
	}

	/**
	 * This method initializes navigationResultsPanel.
	 * 
	 * @return javax.swing.JPanel
	 */
	// private JPanel getNavigationResultsPanel() {
	// if (navigationResultsPanel == null) {
	// GridBagConstraints gridBagConstraints = new GridBagConstraints();
	// gridBagConstraints.fill = GridBagConstraints.BOTH;
	// gridBagConstraints.weighty = 1.0;
	// gridBagConstraints.weightx = 1.0;
	// navigationResultsPanel = new JPanel();
	// navigationResultsPanel.setLayout(new GridBagLayout());
	// navigationResultsPanel.setBorder(new BevelBorder(2));
	// }
	// return navigationResultsPanel;
	// }

	/**
	 * The user wants to preview a place.
	 * 
	 * @param aPlace
	 *            the place found
	 */
	public void jumpTo(final Place aPlace) {
		if (aPlace == null)
			throw new IllegalArgumentException("null place given");
		double jumpToZoomLevel = Settings.getInstance().getDouble(
				"traveling_salesman.DefaultZoomLevel", DEFAULTZOOMLEVEL);
		if (jumpToZoomLevel == 0) {
			jumpToZoomLevel = DEFAULTZOOMLEVEL;
		}
		Projection projection = getActiveMapPanel().getProjection();
		JTabbedPane allMapPanels = getMapPanels();
		for (int tab = 0; tab < allMapPanels.getTabCount(); tab++) {
			INavigatableComponent map = (INavigatableComponent) allMapPanels
					.getComponent(tab);
			if (map == null) {
				continue;
			}
			map.zoomTo(
					projection.latlon2eastNorth(aPlace.getLatitude(),
							aPlace.getLongitude()), jumpToZoomLevel);
		}
		// getActiveMapPanel().zoomTo(projection.latlon2eastNorth(aPlace.getLatitude(),
		// aPlace.getLongitude()), jumpToZoomLevel);
	}

	/**
	 * This method initializes fromTextField.
	 * 
	 * @return javax.swing.JTextField
	 */
	private PlaceFinderPanel getPlaceFinder() {
		if (placeFinder == null) {
			PlaceFinderPanel.PlaceFoundListener myPlaceFoundListener = new TSPlaceFoundListener();
			placeFinder = new PlaceFinderPanel(getOsmData(),
					myPlaceFoundListener, getStatusBarLabel());
		}
		return placeFinder;
	}

	/**
	 * This method initializes routeButton.
	 * 
	 * @return the "route me"-button
	 */
	private JButton getRouteButton() {
		if (routeButton == null) {
			routeButton = new JButton(
					RESOURCE.getString("Main.Buttons.RouteMe"));
			routeButton.setEnabled(false);
			routeButton.addActionListener(new ActionListener() {

				public void actionPerformed(final ActionEvent arg0) {

					if (!routeButton.getText().equals(
							RESOURCE.getString("Main.Buttons.CancelRouting"))) {
						routeButton.setText(RESOURCE
								.getString("Main.Buttons.CancelRouting"));
						doRouting();
					} else {
						myNavigationManager.setDestinations(
								new LinkedList<Place>(), false);
						routeButton.setText(RESOURCE
								.getString("Main.Buttons.RouteMe"));
					}

				}
			});
		}
		return routeButton;
	}

	/**
	 * @return the osmData
	 */
	public IDataSet getOsmData() {
		return this.osmData;
	}

	/**
	 * @param aOsmData
	 *            the osmData to set
	 */
	public void setOsmData(final IDataSet aOsmData) {

		// if the dataset is stored in memory, filter it
		// to only contain the relevant data
		if (myMapFeaturesSelector != null && aOsmData instanceof MemoryDataSet) {
			DataSetFilter filter = new DataSetFilter(myMapFeaturesSelector);
			this.osmData = filter.filterDataSet((MemoryDataSet) aOsmData);
		} else {
			this.osmData = aOsmData;
		}

		myNavigationManager.setMap(this.osmData);
		// empty list means no route
		myNavigationManager.setDestinations(new LinkedList<Place>(), false);
		getMapPanel().setDataSet(this.osmData);
		getMapPanelIsometric().setDataSet(this.osmData);
		getPlaceFinder().setMap(this.osmData);
	}

	/***
	 * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und
	 * Beratung</a>.<br/>
	 * Project: traveling_salesman<br/>
	 * MainFrame.java<br/>
	 * created: 04.04.2009 18:39:45 <br/>
	 * <br/>
	 * <br/>
	 * <b>Used only on {@link #getPlaceFinder()} </b>
	 * 
	 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
	 */
	private final class TSPlaceFoundListener implements
			PlaceFinderPanel.PlaceFoundListener {

		/**
		 * ${@inheritDoc}.
		 */
		public void placeFound(final Place aPlace) {
			DefaultListModel model = (DefaultListModel) placesToGo.getModel();
			if (!model.contains(aPlace))
				(model).addElement(aPlace);
			int size = placesToGo.getModel().getSize();
			if (size > 0 && getActiveMapPanel().getCurrentPosition() != null) {
				// if size>0 and we have a gps-fix
				routeButton.setEnabled(true);
			}
			if (size > 1) {
				routeButton.setEnabled(true);

				final Place placeA = (Place) placesToGo.getModel()
						.getElementAt(size - 2);
				final Place placeB = (Place) placesToGo.getModel()
						.getElementAt(size - 1);

				boolean downloadStrip = Settings.getInstance().getBoolean(
						"traveling-salesman.stripDownload.automatic", true);
				if (downloadStrip) {
					Thread downloadThread = new Thread("download strip") {
						public void run() {
							double width = Settings.getInstance().getDouble(
									"traveling-salesman.stripDownload.width",
									DEFAULTSTRIPDOWNLOADWIDTH);
							downloadStrip(placeA.getLatitude(),
									placeA.getLongitude(),
									placeB.getLatitude(),
									placeB.getLongitude(), width);
						}
					};
					downloadThread.setDaemon(true);
					downloadThread.start();
				}
			}
		}

		/**
		 * ${@inheritDoc}.
		 */
		public void placePreview(final Place aPlace) {
			jumpTo(aPlace);
		}
	}

	/**
	 * Inner class for map "download-button" listeners. Enabled state depends
	 * from current scale and OSM server restrictions.
	 * 
	 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov </a>
	 */
	class ScaleChangeListener implements PropertyChangeListener {

		/**
		 * Disable dialog elements if MapPanel scale changed.
		 * 
		 * @param evt
		 *            the event
		 */
		public void propertyChange(final PropertyChangeEvent evt) {
			getDownloadButton().setEnabled(isDownloadableVisibleArea());
			getMainMenu().getDownloadMapMenuItem().setEnabled(
					isDownloadableVisibleArea());
		}
	}

	/**
	 * This method initializes the download-button..
	 * 
	 * @return the "download"-button.
	 */
	private JButton getDownloadButton() {
		if (downloadButton == null) {
			downloadButton = new JButton();
			downloadButton
					.setText(RESOURCE.getString("Main.Menu.Map.Download"));
			downloadButton.setToolTipText(RESOURCE
					.getString("Main.Menu.Map.Download.ToolTip"));
			downloadButton.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent arg0) {
					downloadVisibleArea();
				}
			});
			ScaleChangeListener scaleChangeListener = new ScaleChangeListener();
			getMapPanel().addPropertyChangeListener("scale",
					scaleChangeListener);
			getMapPanelIsometric().addPropertyChangeListener("scale",
					scaleChangeListener);
		}
		return downloadButton;
	}

	/**
	 * This method initializes the CenterOnGPS-button..
	 * 
	 * @return the "CenterOnGPS"-button.
	 */
	private JButton getCenterOnGPSButton() {
		if (centerOnGPSButton == null) {
			centerOnGPSButton = new JButton();
			centerOnGPSButton.setText(RESOURCE
					.getString("Main.Buttons.CenterOnGPS"));
			centerOnGPSButton.setEnabled(false);
			centerOnGPSButton.setFocusable(false);
			centerOnGPSButton.addActionListener(new ActionListener() {

				public void actionPerformed(final ActionEvent arg0) {
					INavigatableComponent mapPanel2 = getActiveMapPanel();
					if (mapPanel2.getCurrentPosition() == null) {
						LOG.log(Level.SEVERE,
								"cannot center on GPS-ocation because we have no current location");
					} else {
						mapPanel2.zoomTo(
								mapPanel2.getProjection().latlon2eastNorth(
										mapPanel2.getCurrentPosition()),
								mapPanel2.getScale());
					}
				}
			});
		}
		return centerOnGPSButton;
	}

	/**
	 * This method initializes the AutoCenterMode-button..
	 * 
	 * @return the "AutoCenterMode"-button.
	 */
	private JToggleButton getAutoCenterModeButton() {
		if (autoCenterModeButton == null) {
			autoCenterModeButton = new JToggleButton();
			autoCenterModeButton.setText(RESOURCE
					.getString("Main.Buttons.AutoCenterMode"));
			autoCenterModeButton.setEnabled(false);
			autoCenterModeButton.setFocusable(false);
			autoCenterModeButton.addActionListener(new ActionListener() {

				public void actionPerformed(final ActionEvent arg0) {
					INavigatableComponent mapPanel2 = getActiveMapPanel();
					if (mapPanel2.getCurrentPosition() == null) {
						LOG.log(Level.SEVERE,
								"cannot center on GPS-ocation because we have no current location");
					} else {
						mapPanel2.zoomTo(
								mapPanel2.getProjection().latlon2eastNorth(
										mapPanel2.getCurrentPosition()),
								mapPanel2.getScale());
						if (mapPanel2 instanceof MapPanel) {
							if (getAutoCenterModeButton().isSelected()) {
								((MapPanel) mapPanel2).setAutoCenterMode(true);
							} else {
								((MapPanel) mapPanel2).setAutoCenterMode(false);
							}
						}
					}
				}
			});
		}
		return autoCenterModeButton;
	}

	/**
	 * This method initializes the AutoRotateMode-button..
	 * 
	 * @return the "AutoCenterMode"-button.
	 */
	private JToggleButton getAutoRotateModeButton() {
		if (autoRotationModeButton == null) {
			autoRotationModeButton = new JToggleButton();
			autoRotationModeButton.setText(RESOURCE
					.getString("Main.Buttons.AutoRotationMode"));
			autoRotationModeButton.setEnabled(false);
			autoRotationModeButton.setFocusable(false);
			autoRotationModeButton.addActionListener(new ActionListener() {

				public void actionPerformed(final ActionEvent arg0) {
					INavigatableComponent mapPanel2 = getActiveMapPanel();
					if (mapPanel2.getCurrentPosition() == null) {
						LOG.log(Level.SEVERE,
								"cannot center on GPS-ocation because we have no current location");
					} else {
						mapPanel2.zoomTo(
								mapPanel2.getProjection().latlon2eastNorth(
										mapPanel2.getCurrentPosition()),
								mapPanel2.getScale());
						if (mapPanel2 instanceof MapPanel) {
							if (getAutoRotateModeButton().isSelected()) {
								((MapPanel) mapPanel2)
										.setAutoRotationMode(true);
							} else {
								((MapPanel) mapPanel2)
										.setAutoRotationMode(false);
							}
						}
					}
				}
			});
		}
		return autoRotationModeButton;
	}

	/**
	 * This method initializes the "Show trail" switch.
	 * 
	 * @return the "ShowTrail"-switch.
	 */
	private JToggleButton getShowTrailButton() {
		if (showTrailButton == null) {
			showTrailButton = new JToggleButton(new ShowTrailAction(
					getTracksPanel().getTracksStorage(), myGPSProvider));
			showTrailButton.setFocusable(false);
		}
		return showTrailButton;
	}

	// /**
	// * This method lazy initializes IntersectionPanel.
	// * @return the IntersectionPanel
	// */
	// private IntersectionPanel getIntersectionPanel() {
	// if (intersectionPanel == null) {
	// intersectionPanel = new IntersectionPanel(null, (JPanel)
	// getActiveMapPanel());
	// this.myNavigationManager.addRouteChangedListener(intersectionPanel);
	// this.myNavigationManager.addRoutingStepListener(intersectionPanel);
	// }
	// return intersectionPanel;
	// }

	/**
	 * This method initializes routeInstructionsPanel.
	 * 
	 * @return the RouteInstructionPanel
	 */
	private RouteInstructionPanel getRouteInstructionsPanel() {
		if (routeInstructionsPanel == null) {
			routeInstructionsPanel = new RouteInstructionPanel(null);
			// routeInstructionsPanel.setLayout(new GridBagLayout());
			// use the same background-color that the task-pane uses at the
			// bottom
			if (UIManager.getBoolean("TaskPane.useGradient")) {
				routeInstructionsPanel.setBackground(UIManager
						.getColor("TaskPane.backgroundGradientStart"));
			} else {
				routeInstructionsPanel.setBackground(UIManager
						.getColor("TaskPane.background"));
			}
			routeInstructionsPanel
					.addMapPanel((INavigatableComponent) getMapPanel());
			routeInstructionsPanel
					.addMapPanel((INavigatableComponent) getMapPanelIsometric());
			this.myNavigationManager
					.addRouteChangedListener(routeInstructionsPanel);
			this.myNavigationManager
					.addRoutingStepListener(routeInstructionsPanel);
		}
		return routeInstructionsPanel;
	}

	/**
	 * Download a strip of land between A and B that is "width" wide.
	 * 
	 * @param nodeA
	 *            the start
	 * @param nodeB
	 *            the target
	 * @param width
	 *            the width (in the same unit as lat and lon);
	 */
	public void downloadStrip(final Node nodeA, final Node nodeB,
			final double width) {
		downloadStrip(nodeA.getLatitude(), nodeA.getLongitude(),
				nodeB.getLatitude(), nodeB.getLongitude(), width);
	}

	/**
	 * Download a strip of land between A and B that is "width" wide.
	 * 
	 * @param latA
	 *            the start
	 * @param lonA
	 *            the start
	 * @param latB
	 *            the target
	 * @param lonB
	 *            the target
	 * @param width
	 *            the width (in the same unit as lat and lon);
	 */
	public void downloadStrip(final double latA, final double lonA,
			final double latB, final double lonB, final double width) {
		LOG.fine("downloadStrip(width=" + width + ") called");

		if (width == 0) {
			LOG.info("NOT downloading strip because the download is disabled");
			// download disabled
			return;
		}

		double latDiff = latB - latA;
		double lonDiff = lonB - lonA;
		if (Math.max(latDiff, lonDiff) > Settings.getInstance().getDouble(
				"traveling-salesman.maxStripLength", 0.5)) {
			LOG.info("NOT downloading strip because the strip-length is > maxStripLength");
			return;
		}

		if (Math.abs(latDiff) > Math.abs(lonDiff)) {
			int steps = (int) Math.ceil(Math.abs(latDiff) / width);
			double latStep = width;
			double lonStep = lonDiff / steps;
			for (int i = 0; i < steps; i++) {
				downloadArea(latA + i * latStep, lonA + i * lonStep, latA
						+ (i + 1) * latStep, lonA + (i + 1) * lonStep, true,
						false);
			}
		} else {
			int steps = (int) Math.ceil(Math.abs(lonDiff) / width);
			double latStep = latDiff / steps;
			double lonStep = width;
			for (int i = 0; i < steps; i++) {
				downloadArea(latA + i * latStep, lonA + i * lonStep, latA
						+ (i + 1) * latStep, lonA + (i + 1) * lonStep, true,
						false);
			}
		}
	}

	/**
	 * Download a are of land between A and B.
	 * 
	 * @param latA
	 *            the start
	 * @param lonA
	 *            the start
	 * @param latB
	 *            the target
	 * @param lonB
	 *            the target
	 * @param silent
	 *            if true, we log as INFO instead of SEVERE
	 * @param zoomToDownloadArea
	 *            update the view to zoom to the area after importing it
	 */
	public void downloadArea(final double latA, final double lonA,
			final double latB, final double lonB, final boolean silent,
			final boolean zoomToDownloadArea) {
		BoundingBoxDownloader downloader = new BoundingBoxDownloader(latA,
				lonA, latB, lonB);
		// setOsmData(downloader.parseOsm());

		// merge the data
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		try {
			getStatusBarLabel()
					.setText(
							RESOURCE.getString("Main.Menu.Map.OpenFile.Status.loadingMap"));
			getJProgressBar().setMaximum(2);
			getJProgressBar().setValue(0);
			DataSetFilter filter = new DataSetFilter(myMapFeaturesSelector);
			MemoryDataSet newData = filter.filterDataSet(downloader.parseOsm());
			getJProgressBar().setValue(1);
			addMap(newData, zoomToDownloadArea);
			getJProgressBar().setValue(2);
			getStatusBarLabel().setText("");
		} catch (Throwable x) {
			if (silent) {
				LOG.log(Level.INFO, "Exception while loading URL:", x);
			} else {
				LOG.log(Level.SEVERE, "Exception while loading URL:", x);
				getStatusBarLabel()
						.setText(
								MainFrame.RESOURCE
										.getString("Main.Menu.Map.OpenFile.Status.Errorhappened"));

				JOptionPane
						.showMessageDialog(
								null,
								MainFrame.RESOURCE
										.getString("Main.Menu.Map.OpenFile.Status.Errorhappened")
										+ "\n" + x.getLocalizedMessage(),
								"Error", JOptionPane.ERROR_MESSAGE);
			}
		} finally {
			setCursor(Cursor.getDefaultCursor());
			getJProgressBar().setIndeterminate(false);
			getJProgressBar().setValue(getJProgressBar().getMaximum());
		}
	}

	/**
	 * Check size of visible area with restriction of OpenStreetMap server.
	 * 
	 * @return true if visible may be downloaded.
	 */
	private boolean isDownloadableVisibleArea() {
		final double maximumAreaSize = 0.25d;
		Bounds bounds = getActiveMapPanel().getMapBounds();
		double dLat = bounds.getMax().lat() - bounds.getMin().lat();
		double dLon = bounds.getMax().lon() - bounds.getMin().lon();
		return maximumAreaSize >= (dLat * dLon);
	}

	/**
	 * Download the visible area from the OpenStreetMap-server.
	 */
	public void downloadVisibleArea() {
		if (isDownloadableVisibleArea()) {
			Bounds bounds = getActiveMapPanel().getMapBounds();
			LOG.log(Level.INFO,
					"downloading: " + (bounds.getMin().lat()) + ", "
							+ (bounds.getMin().lon()) + " - "
							+ (bounds.getMax().lat()) + ", "
							+ (bounds.getMax().lon()));
			downloadArea(bounds.getMin().lat(), bounds.getMin().lon(), bounds
					.getMax().lat(), bounds.getMax().lon(), false, false);
		} else {
			LOG.log(Level.INFO,
					"could not download visible area, cause OSM server restrictions.");
		}
	}

	/**
	 * 
	 * @return The currently visible MapPanel.
	 * @see #mapPanel
	 * @see #mapPanelIsometric
	 */
	private INavigatableComponent getActiveMapPanel() {
		JTabbedPane mapPanelTabs = getMapPanels();
		INavigatableComponent currentMapPanel = (INavigatableComponent) mapPanelTabs
				.getSelectedComponent();
		if (currentMapPanel == null) {
			mapPanelTabs.setSelectedIndex(0);
			return (INavigatableComponent) mapPanelTabs.getSelectedComponent();
		}
		return currentMapPanel;
	}

	/**
	 * Load the given file as the new map.
	 * 
	 * @param aSelectedFile
	 *            the file
	 * @throws IOException
	 *             on IO-problems
	 */
	public void loadMapFile(final URL aSelectedFile) throws IOException {
		Bounds bounds = LoadMapFileActionListener.loadMapURL((Frame) this,
				aSelectedFile, getStatusBarLabel(), getJProgressBar(),
				getOsmData());
		if (bounds != null) {
			getMapPanel().zoomTo(
					getMapPanel().getProjection().latlon2eastNorth(
							bounds.center()), getMapPanel().getScale());
		}
	}

	/**
	 * Add a map that we can fully contain in memory. This also means we can
	 * show a progress-bar because we know how many elements will comm from the
	 * map.
	 * 
	 * @param newData
	 *            add this map's data to our curent map
	 * @param zoomToDownloadArea
	 *            update the view to zoom to the area after importing it
	 */
	private void addMap(final MemoryDataSet newData,
			final boolean zoomToDownloadArea) {
		IDataSet oldData = getOsmData();
		final int percent = 100;
		final int steps = 4;
		getJProgressBar().setMaximum(steps * percent);
		getJProgressBar().setValue(percent);
		UsedTags selector = null;

		// if the entities are tagged with TMC LocationCodes, index these
		TMCLocationIndexer tmcIndexer = new TMCLocationIndexer();
		if (Settings.getInstance().getBoolean("filtermap", false)) {
			selector = new UsedTags();
		}

		int count = newData.getNodesCount();
		int done = 0;
		getStatusBarLabel()
				.setText(
						MessageFormat.format(
								RESOURCE.getString("Main.Menu.Map.OpenFile.Status.loadingNodes"),
								new Object[] { count }));
		long start = System.currentTimeMillis();
		for (Iterator<Node> nodes = newData.getNodes(Bounds.WORLD); nodes
				.hasNext();) {
			Node node = nodes.next();
			if (selector == null) {
				oldData.addNode(node);
				tmcIndexer.visit(node);
			} else if (selector.isAllowed(oldData, node)) {
				oldData.addNode(selector.filterNode(node));
				tmcIndexer.visit(node);
			}
			done++;
			getJProgressBar().setValue(percent + ((percent * done) / count));
		}
		long endNodes = System.currentTimeMillis();
		int step = 2;
		getJProgressBar().setValue(step * percent);

		count = newData.getWaysCount();
		done = 0;
		getStatusBarLabel()
				.setText(
						MessageFormat.format(
								RESOURCE.getString("Main.Menu.Map.OpenFile.Status.loadingWays"),
								new Object[] { count }));
		for (Iterator<Way> ways = newData.getWays(Bounds.WORLD); ways.hasNext();) {
			Way way = ways.next();
			if (selector == null) {
				oldData.addWay(way);
				tmcIndexer.visit(way);
			} else if (selector.isAllowed(oldData, way)) {
				oldData.addWay(selector.filterWay(way));
				tmcIndexer.visit(way);
			}
			done++;
			getJProgressBar().setValue(
					(step * percent) + ((percent * done) / count));
		}
		long endWays = System.currentTimeMillis();
		step++;
		getJProgressBar().setValue(step * percent);

		count = newData.getRelationsCount();
		done = 0;
		getStatusBarLabel()
				.setText(
						MessageFormat.format(
								RESOURCE.getString("Main.Menu.Map.OpenFile.Status.loadingRelations"),
								new Object[] { count }));
		for (Iterator<Relation> rel = newData.getRelations(Bounds.WORLD); rel
				.hasNext();) {
			Relation relation = rel.next();
			if (selector == null || selector.isAllowed(oldData, relation)) {
				oldData.addRelation(relation);
				tmcIndexer.visit(relation);
			}
			done++;
			getJProgressBar().setValue(
					(step * percent) + ((percent * done) / count));
		}
		long endRelations = System.currentTimeMillis();
		step++;

		// if the old AdressDB is used, index with it, else index with the
		// advanced one.
		IAddressDBPlaceFinder addresses = null;
		IPlaceFinder finder = Settings.getInstance().getPlugin(
				IPlaceFinder.class, InetPlaceFinder.class.getName());
		if (finder instanceof IAddressDBPlaceFinder) {
			addresses = (IAddressDBPlaceFinder) finder;
		} else {
			addresses = new AdvancedAddressDBPlaceFinder();
		}
		addresses.setMap(oldData);

		getStatusBarLabel().setText(
				RESOURCE.getString("Main.Menu.Map.OpenFile.Status.indexing"));
		addresses.indexDatabase(newData);
		addresses.checkpointDB();
		if (oldData instanceof DBDataSet) {
			((DBDataSet) oldData).commit();
		}
		getJProgressBar().setValue(step * percent);
		long endIndexing = System.currentTimeMillis();

		// zoom to loaded area
		BoundingXYVisitor boundsV = new BoundingXYVisitor(newData);
		newData.visitAll(boundsV, Bounds.WORLD);

		Bounds bounds = boundsV.getBounds();
		if (bounds != null) {
			getMapPanel().recalculateCenterScale(bounds);
			getMapPanelIsometric().recalculateCenterScale(boundsV.getBounds());
		}

		long endBounds = System.currentTimeMillis();
		LOG.log(Level.INFO,
				"Imported new map-data in:\n" + "\t" + newData.getNodesCount()
						+ " nodes in " + (endNodes - start) + "ms\n" + "\t"
						+ newData.getWaysCount() + " ways in "
						+ (endWays - endNodes) + "ms\n" + "\t"
						+ newData.getRelationsCount() + " relations in "
						+ (endRelations - endWays) + "ms\n"
						+ "\tindexing cities in "
						+ (endIndexing - endRelations) + "ms\n"
						+ "\tcalculating bounds in "
						+ (endBounds - endIndexing) + "ms\n" + "\tsum "
						+ (endBounds - start) + "ms\n");
	}

	/**
	 * This method initializes MainMenu.
	 * 
	 * @return javax.swing.JMenuBar.
	 */
	private TSMenuBar getMainMenu() {
		if (myMainMenu == null) {
			myMainMenu = new TSMenuBar(this);
			myMainMenu.getExitMenuItem().addActionListener(
					new java.awt.event.ActionListener() {
						public void actionPerformed(final ActionEvent e) {
							MainFrame.this
									.processWindowEvent(new WindowEvent(
											MainFrame.this,
											WindowEvent.WINDOW_CLOSING));
						}
					});
		}
		return myMainMenu;
	}

	/**
	 * This method initializes jProgressBar.
	 * 
	 * @return javax.swing.JProgressBar
	 */
	protected JProgressBar getJProgressBar() {
		if (myProgressBar == null) {
			myProgressBar = new JProgressBar();
			myProgressBar.setBorderPainted(false);
			// use the same background-color that the task-pane uses at the
			// bottom
			if (UIManager.getBoolean("TaskPane.useGradient")) {
				myProgressBar.setBackground(UIManager
						.getColor("TaskPane.backgroundGradientEnd"));
			} else {
				myProgressBar.setBackground(UIManager
						.getColor("TaskPane.background"));
			}
		}
		return myProgressBar;
	}

	/**
	 * This method initializes jProgressBar.
	 * 
	 * @return javax.swing.JProgressBar
	 */
	public JLabel getStatusBarLabel() {
		if (myStatusBarLabel == null) {
			myStatusBarLabel = new JLabel();
			// use the same background-color that the task-pane uses at the
			// bottom
			if (UIManager.getBoolean("TaskPane.useGradient")) {
				myStatusBarLabel.setBackground(UIManager
						.getColor("TaskPane.backgroundGradientEnd"));
			} else {
				myStatusBarLabel.setBackground(UIManager
						.getColor("TaskPane.background"));
			}
		}
		return myStatusBarLabel;
	}

	/**
	 * This method initializes jProgressBar.
	 * 
	 * @return javax.swing.JProgressBar
	 */
	private JPanel getStatusBar() {
		if (myStatusBar == null) {
			myStatusBar = new JPanel();
			// use the same background-color that the task-pane uses at the
			// bottom
			if (UIManager.getBoolean("TaskPane.useGradient")) {
				myStatusBar.setBackground(UIManager
						.getColor("TaskPane.backgroundGradientEnd"));
			} else {
				myStatusBar.setBackground(UIManager
						.getColor("TaskPane.background"));
			}
			myStatusBar.setLayout(new GridLayout(1, 2));
			myStatusBar.add(getStatusBarLabel());
			myStatusBar.add(getJProgressBar());
		}
		return myStatusBar;
	}

	/**
	 * Initialize {@link #myNavigationManager} and make it route to all
	 * {@link #placesToGo}.
	 */
	private void doRouting() {
		// A selector that forbids footways and cycleways.
		this.myNavigationManager.setSelector(myVehicle);

		List<Place> myDestinations = new LinkedList<Place>();
		ListModel placesModel = getPlacesToGo().getModel();
		for (int i = 0; i < placesModel.getSize(); i++)
			myDestinations.add((Place) placesModel.getElementAt(i));

		getJProgressBar().setMaximum(Integer.MAX_VALUE);
		getJProgressBar().setValue(0);
		getStatusBarLabel().setText("routing...");

		myNavigationManager.setDestinations(myDestinations,
				myNavigationManager.getLastGPSPos() != null);
	}

	/**
	 * We have a new route. Thus route-calculation is finished.
	 * 
	 * @param aNewRoute
	 *            the new route
	 */
	public void routeChanged(final Route aNewRoute) {
		/*
		 * SwingUtilities.invokeLater(new Runnable() { public void run() {
		 */
		getRouteButton().setText(RESOURCE.getString("Main.Buttons.RouteMe"));
		getJProgressBar().setValue(Integer.MAX_VALUE);
		getStatusBarLabel().setText(
				RESOURCE.getString("Main.StatusBar.RoutingDone"));
		/*
		 * } });
		 */

		jContentPane.remove(getRouteInstructionsPanel());
		if (aNewRoute != null) {
			jContentPane.add(getRouteInstructionsPanel(), BorderLayout.NORTH);
		}
	}

	/**
	 * ${@inheritDoc}.
	 */
	@Override
	public void noRouteFound() {
		getRouteButton().setText(RESOURCE.getString("Main.Buttons.RouteMe"));
		getJProgressBar().setValue(Integer.MAX_VALUE);
		getStatusBarLabel().setText(
				RESOURCE.getString("Main.StatusBar.RoutingFailed"));
		jContentPane.remove(getRouteInstructionsPanel());
	}

	/**
	 * @return The plugin that provides our position.
	 */
	public IGPSProvider getGPSProvider() {
		return myGPSProvider;
	}

}