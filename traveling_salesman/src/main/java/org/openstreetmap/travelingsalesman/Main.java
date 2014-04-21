/**
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
package org.openstreetmap.travelingsalesman;


import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JTextArea;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.DBDataSet;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.LODDataSet;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.searching.IAddressDBPlaceFinder;
import org.openstreetmap.osm.data.searching.IPlaceFinder;
import org.openstreetmap.osm.data.searching.InetPlaceFinder;
import org.openstreetmap.osm.io.FileLoader;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.travelingsalesman.actions.LoadMapFileActionListener;
import org.openstreetmap.travelingsalesman.gui.MainFrame;
import org.openstreetmap.travelingsalesman.gui.widgets.ErrorDialog;
import org.openstreetmap.travelingsalesman.gui.wizard.ConfigWizard;
import org.openstreetmap.travelingsalesman.navigation.NavigationManager;
import org.openstreetmap.travelingsalesman.navigation.OsmNavigationConfigSection;
import org.openstreetmap.travelingsalesman.routing.IRouter;
import org.openstreetmap.travelingsalesman.routing.IVehicle;
import org.openstreetmap.travelingsalesman.routing.NameHelper;
import org.openstreetmap.travelingsalesman.routing.Route;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.routing.metrics.IRoutingMetric;
import org.openstreetmap.travelingsalesman.routing.metrics.StaticFastestRouteMetric;
import org.openstreetmap.travelingsalesman.routing.routers.TurnRestrictedAStar;
import org.openstreetmap.travelingsalesman.routing.selectors.Motorcar;


/**
 * Main-class of the traveling-salesman -application.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public final class Main {

    /**
     * is never instanciated.
     */
    private Main() {
    }

    /**
     * @param args arguments
     */
    public static void main(final String[] args) {

        //debug: download street
        try {

//          ------------------------------- configure logging
            configureLoggingConsole();
            configureLoggingPopup();
            configureLoggingLevelAll();

//          ------------------------------- configure settings

            ConfigurationSection rootConfigSection = Settings.getRootConfigurationSection();
            rootConfigSection.addSubSections(new OsmNavigationConfigSection());
            rootConfigSection.addSubSections(new TSNavigationConfigSection());

//          ------------------------------- load default-map from server or local file
            String defaultDir = Settings.getInstance().get("map.dir",
                    Settings.getInstance().get("tiledMapCache.dir",
                    System.getProperty("user.home")
                    + File.separator + ".openstreetmap" + File.separator + "map" + File.separator));
            File dir = new File(defaultDir);
            IDataSet osmData = null;
            if (!dir.exists()) {
                ConfigWizard newUserWizard = new ConfigWizard();
                newUserWizard.setModal(true);
                newUserWizard.pack();
                newUserWizard.setVisible(true);
                osmData = newUserWizard.getDataSet();
            } else {
                //IDataSet osmData = new FileTileDataSet(); // new LODDataSet();
                //IDataSet osmData = new OsmBinDataSet(new File("d:\\osmbin.data")); //DBDataSet(); // new LODDataSet();
                osmData = Settings.getInstance().getPlugin(IDataSet.class, LODDataSet.class.getName());
//                osmData = new LODDataSet();
            }


//          ------------------------------- start application
            if (args.length != 0) {
                /* forceDatabaseOpen without dialog */
                forceDatabaseOpen(osmData, false);
                // command-line version
                int argIndex = 0;
                // handle global options
                while (args.length > argIndex) {
                    if (args[argIndex].equalsIgnoreCase("--loglevel")
                            && args.length > argIndex + 2) {
                        String pkg = args[argIndex + 1];
                        String level = args[argIndex + 2];
                        System.out.println("setting log-level for \""
                                + pkg + "\" to \""
                                + level + "\"");
                        Logger.getLogger(pkg).setLevel(Level.parse(level));
                        argIndex += 3;
                        continue;
                    }
                    if (args[argIndex].equalsIgnoreCase("--override")
                            && args.length > argIndex + 2) {
                        String key = args[argIndex + 1];
                        String value = args[argIndex + 2];
                        System.out.println("overriding setting for \""
                                + key + "\" twith \""
                                + value + "\"");
                        Settings.getInstance().override(key, value);
                        argIndex += 3;
                        continue;
                    }
                    if (args[argIndex].equalsIgnoreCase("--set")
                            && args.length > argIndex + 2) {
                        String key = args[argIndex + 1];
                        String value = args[argIndex + 2];
                        System.out.println("setting setting for \""
                                + key + "\" twith \""
                                + value + "\"");
                        Settings.getInstance().override(key, value);
                        argIndex += 3;
                        continue;
                    }
                    if (args[argIndex].equalsIgnoreCase("route")) {
                        handleRouteCommand(args, argIndex, osmData);
                        return;
                    } else if (args[argIndex].equalsIgnoreCase("import")) {
                        handleImport(args, argIndex, osmData);
                        return;
                    } else if (args[argIndex].equalsIgnoreCase("gui")) {
                        System.out.println("starting GUI...");
                        MainFrame frame = new MainFrame();
                        frame.setOsmData(osmData);
                        frame.setVisible(true);
                        return;
                    } else {
                        System.err.println("unrecognized command or option: " + args[argIndex]);
                        displayCmdlnUsage();
                        return;
                    }
                }
            } else {
                /* forceDatabaseOpen with dialog */
                forceDatabaseOpen(osmData, true);
                MainFrame frame = new MainFrame();
                frame.setOsmData(osmData);
                frame.setVisible(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Handle the command-line command "import".
     * @param aArgs all command-line arguments
     * @param argIndex the index of the "route"-command in the argument-list
     * @param aOsmData the map to use
     */
    private static void handleImport(final String[] aArgs, final int argIndex, final IDataSet aOsmData) {
        System.out.println("importing " + aArgs[argIndex + 1]);
        try {
            if (aArgs[argIndex + 1].toLowerCase().startsWith("http") || aArgs[argIndex + 1].toLowerCase().startsWith("ftp")) {
                LoadMapFileActionListener.loadMapURL(null, new URL(aArgs[argIndex + 1]), null, null, aOsmData);
            } else {
                LoadMapFileActionListener.loadMapFile(null, new File(aArgs[argIndex + 1]), null, null, aOsmData);
            }
            aOsmData.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("importing " + aArgs[argIndex + 1] + " done");
    }

    /**
     * constant used for route-output in GPX-format.
     */
    public static final String GPX = "gpx";
    /**
     * constant used for route-output in GPX-format.
     */
    public static final String CSV = "csv";

    /**
     * Handle the command-line command "route".
     * @param aArgs all command-line arguments
     * @param argIndex the index of the "route"-command in the argument-list
     * @param aMap the map to use
     */
    private static void handleRouteCommand(final String[] aArgs, final int argIndex, final IDataSet aMap) {
        IDataSet map = aMap;
        IRouter router = new TurnRestrictedAStar();
        IVehicle vehicle = new Motorcar();
        IRoutingMetric metric = new StaticFastestRouteMetric();
        PrintStream out = System.out;
        String outFormat = CSV;

        int firstNodeArg = argIndex + 1;
        boolean parameterParsed = true;
        while (parameterParsed) {
            parameterParsed = false;
            if (aArgs[firstNodeArg].equals("-loadmap")) {
                FileLoader loader = new FileLoader(new File(aArgs[firstNodeArg + 1]));
                map = loader.parseOsm();
                firstNodeArg += 2;
                parameterParsed = true;
            }
            if (aArgs[firstNodeArg].equals("-router")) {
                try {
                    router = (IRouter) Class.forName(aArgs[firstNodeArg + 1]).newInstance();
                } catch (Exception e) {
                    System.err.println("Cannot used given router-class. Using default router.");
                    e.printStackTrace();
                }
                firstNodeArg += 2;
                parameterParsed = true;
            }
            if (aArgs[firstNodeArg].equals("-vehicle")) {
                try {
                    vehicle = (IVehicle) Class.forName(aArgs[firstNodeArg + 1]).newInstance();
                } catch (Exception e) {
                    System.err.println("Cannot used given vehicle-class. Using default router.");
                    e.printStackTrace();
                }
                firstNodeArg += 2;
                parameterParsed = true;
            }
            if (aArgs[firstNodeArg].equals("-metric")) {
                try {
                    metric = (IRoutingMetric) Class.forName(aArgs[firstNodeArg + 1]).newInstance();
                } catch (Exception e) {
                    System.err.println("Cannot used given metric-class. Using default router.");
                    e.printStackTrace();
                }
                firstNodeArg += 2;
                parameterParsed = true;
            }
            if (aArgs[firstNodeArg].equals("-" + GPX)) {
                FileOutputStream f;
                try {
                    f = new FileOutputStream(aArgs[firstNodeArg + 1]);
                    out = new PrintStream(f, true, "UTF8");
                } catch (Exception e) {
                    System.err.println("Error: Cannot open file: " + aArgs[firstNodeArg + 1]);
                }
                outFormat = GPX;
                firstNodeArg += 2;
                parameterParsed = true;
            }
            if (aArgs[firstNodeArg].equals("-" + CSV)) {
                FileOutputStream f;
                try {
                    f = new FileOutputStream(aArgs[firstNodeArg + 1]);
                    out = new PrintStream(f);
                } catch (Exception e) {
                    System.err.println("Error: Cannot open file: " + aArgs[firstNodeArg + 1]);
                }
                outFormat = CSV;
                firstNodeArg += 2;
                parameterParsed = true;
            }
        }

        Node[] nodes = new Node[aArgs.length - firstNodeArg];
        for (int i = 0; i < nodes.length; i++) {
            long nodeID = parseNodeCmdln(aMap, aArgs[i + firstNodeArg]);
            Node node = aMap.getNodeByID(nodeID);
            if (node == null) {
                System.err.println("Node" + nodeID + " not found in map");
                System.exit(-1);
            }
            nodes[i] = node;
        }
        if (outFormat.equalsIgnoreCase(CSV)) {
            out.println("from(nodeID), via(wayID), to(nodeID), length in meters");
        }
        if (outFormat.equalsIgnoreCase(GPX)) {
            // we need to make sure that we are writing UTF8 if
            // we specify it in the header
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            out.println("<gpx version=\"1.0\" creator=\"Traveling Salesman " + MainFrame.RESOURCE.getString("Main.Version") + "\" "
                    + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                    + "xmlns=\"http://www.topografix.com/GPX/1/0\" "
                    + "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">");
            out.println("<trk>");
            out.println("<trkseg>");
        }
        metric.setMap(map);
        router.setMetric(metric);
        for (int i = 0; i < nodes.length - 1; i++) {
            Route route = router.route(map, nodes[i + 1], nodes[i], vehicle);
            if (route == null) {
                System.err.println("No route found from " + nodes[i].getId()
                        + " to " + nodes[i + 1].getId());
                System.exit(-1);
            }
            List<RoutingStep> routingSteps = route.getRoutingSteps();
            if (outFormat == CSV) {
                for (RoutingStep routingStep : routingSteps) {
                    out.println(routingStep.getStartNode().getId()
                            + ", " + routingStep.getWay().getId()
                            + ", " + routingStep.getEndNode().getId()
                            + ", " + routingStep.distanceInMeters());
                }
            }
            if (outFormat == GPX) {

                for (RoutingStep step : routingSteps) {
                    Node endnode = step.getEndNode();
                    out.println("<trkpt lat=\"" + endnode.getLatitude() + "\" lon=\"" + endnode.getLongitude() + "\" >");
                    out.println("<name>" + NameHelper.getNameForNode(aMap, endnode)  + "</name>");
                    out.println("<cmt>osm-id " + endnode.getId() + "</cmt>");
                    out.println("<desc>osm-id " + endnode.getId() + "</desc>");
                    out.println("</trkpt>");
                }
            }
        }
        if (outFormat == GPX) {
            out.println("</trkseg></trk></gpx>");
        }
    }

    /**
     * Whenever a node is given on the command-line,
     * it can be given in various formats. Determine
     * the format and find the node.
     * @param aMap the map we are working on
     * @param aNodeDesc the node
     * @return the nodeID
     */
    private static long parseNodeCmdln(final IDataSet aMap, final String aNodeDesc) {
        if (aNodeDesc.startsWith("[") && aNodeDesc.endsWith("]")) {
            // node is given as [lat, lon]
            String[] latLon = aNodeDesc.substring(1, aNodeDesc.length() - 1).split(",");
            LatLon location = new LatLon(Double.parseDouble(latLon[0]), Double.parseDouble(latLon[1]));
            Node nearestNode = aMap.getNearestNode(location, new Motorcar());
            if (nearestNode == null) {
                System.err.println("There is no node near " + aNodeDesc + " in out map.");
                System.exit(-1);
            }
            return nearestNode.getId();
        }
        return Long.parseLong(aNodeDesc);
    }

    /**
     * Display how to use the command-line interface.
     */
    private static void displayCmdlnUsage() {
        System.err.println("Usage: ");
        System.err.println("java -jar travelingSalesman.jar : start graphical navigator");
        System.err.println("java -jar travelingSalesman.jar [options] route [-router <classname>] [-metric <classname>] [-vehicle <classname>] "
                + "[-loadmap file.osm] [-csv filename.csv|-gpx filename.gpx] startNodeID [viaNodeIDs]* endNodeID : "
                + "navigate from start to end and output as CSV or to a GPX or CSV file when specified");
        System.err.println("java -jar travelingSalesman.jar [options] route [-router <classname>] [-metric <classname>] [-vehicle <classname>] "
                + "[-loadmap file.osm] [latitude, longitude] [latitude, longitude] "
                + ": navigate from start to end location and output as CSV");
        System.err.println("java -jar travelingSalesman.jar  [options] import <filename>");
        System.err.println("java -jar travelingSalesman.jar  [options] import <file-URL>");
        System.err.println("java -jar travelingSalesman.jar  [options] gui");
        System.err.println("options: (you may specify any number of them including none");
        System.err.println("--loglevel <package> <level>");
        System.err.println("   level may be: SEVERE, WARNING, INFO, FINE, FINER or FINEST");
        System.err.println("--set <key> <value> (permanent config-setting)");
        System.err.println("--override <key> <value>  (temporary config-setting)");
    }

    /**
     * Force the database to be opened and to replay all logs
     * by requesting a single node.
     * @param osmData the database
     */
    private static void forceDatabaseOpen(final IDataSet osmData, final boolean dialog) {
        // force DBDataSet to open a connection
        JDialog pleaseWaitDialog = null;
        if (dialog) {
            pleaseWaitDialog = new JDialog();
            pleaseWaitDialog.setTitle("Please wait...");
            JTextArea pleaseWaitLabel = new JTextArea("Opening database, please wait.\n"
                    + "if the database has not been closed properly after the last map-import\n"
                    + "this may take a while to compute...");
            pleaseWaitLabel.setEditable(false);
            pleaseWaitLabel.setBorder(BorderFactory.createEtchedBorder());
            pleaseWaitDialog.getContentPane().add(pleaseWaitLabel);
            pleaseWaitDialog.pack();
            pleaseWaitDialog.setVisible(true);
        }
        try {
            System.out.println("opening Map-Database...");
            osmData.getNodeByID(0);
            if (osmData instanceof DBDataSet) {
                DBDataSet dbData = (DBDataSet) osmData;
                dbData.commit();
            }
            System.out.println("Map-Database opened");

            IPlaceFinder finder = Settings.getInstance().getPlugin(IPlaceFinder.class, InetPlaceFinder.class.getName());
            if (finder instanceof IAddressDBPlaceFinder) {
                ((IAddressDBPlaceFinder) finder).checkpointDB();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (dialog) {
            pleaseWaitDialog.setVisible(false);
            pleaseWaitDialog.dispose();
        }
    }

    /**
     * Configures logging to write all output to the console.
     */
    private static void configureLoggingConsole() {
        Logger rootLogger = Logger.getLogger("");

        // Remove any existing handlers.
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // add a debug-file
//        try {
//            Handler fileHandler = new FileHandler("e:\\libosm.osmbin.debug.log");
//            fileHandler.setLevel(Level.FINEST);
//            fileHandler.setFormatter(new SimpleFormatter());
//            Logger.getLogger("org.openstreetmap.osm.data.osmbin").addHandler(fileHandler);
//        } catch (Exception e) {
//            System.err.println("Could not create debug-log for tracing osmbin");
//            e.printStackTrace();
//        }

        // Add a new console handler.
        Handler consoleHandler = new StdoutConsoleHandler();
        consoleHandler.setLevel(Level.FINER);
        consoleHandler.setFilter(new Filter() {

            @Override
            public boolean isLoggable(final LogRecord aRecord) {
                Level level = aRecord.getLevel();
                return !level.equals(Level.WARNING) && !level.equals(Level.SEVERE);
            }
        });
        rootLogger.addHandler(consoleHandler);

        Handler consoleHandlerErr = new ConsoleHandler();
        consoleHandlerErr.setLevel(Level.WARNING);
        rootLogger.addHandler(consoleHandlerErr);
    }

    /**
     * Show a popup on all severe messages.
     */
    private static void configureLoggingPopup() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(new Handler() {

            @Override
            public void close() {
            }

            @Override
            public void flush() {
            }

            @Override
            public void publish(final LogRecord aRecord) {
                try {
                    if (aRecord.getLevel().intValue() >= Level.SEVERE.intValue()) {
                        Runnable asyncPopup = new Runnable() {
                            public void run() {
                                String message = "\n" + aRecord.getMessage()
                                        + "\n";
                                if (aRecord.getThrown() != null) {
                                    message += "\ndue to:\n a "
                                            + aRecord.getThrown().getClass()
                                                    .getSimpleName() + " \""
                                            + aRecord.getThrown().getMessage()
                                            + "\"";
                                }
                                if (aRecord.getSourceClassName() != null) {
                                    message += "\nin:\n "
                                            + aRecord.getSourceClassName()
                                                    .replaceAll(".*\\.", "");
                                    if (aRecord.getSourceMethodName() != null) {
                                        message += ":"
                                                + aRecord.getSourceMethodName()
                                                + "()";
                                    }
                                }
                                if (aRecord.getThrown() != null) {
                                    final int maxStrackTraceLength = 1000;
                                    StringWriter writer = new StringWriter();
                                    aRecord.getThrown().printStackTrace(
                                            new PrintWriter(writer));
                                    String stackTrace = writer.getBuffer()
                                            .toString();
                                    if (stackTrace.length() > maxStrackTraceLength) {
                                        stackTrace = stackTrace.substring(0,
                                                maxStrackTraceLength)
                                                + "...";
                                    }
                                    message += "\n\n" + stackTrace;
                                }
                                ErrorDialog.errorHappened(aRecord.getMessage(), message);
//                                JOptionPane.showMessageDialog(null, message,
//                                        "Error:" + aRecord.getMessage(),
//                                        JOptionPane.ERROR_MESSAGE);
                            }
                        };
                        // this must be asynchronous because errors may happen
                        // in the swingworker-thread
                        NavigationManager.getExecutorService().execute(asyncPopup);
//                        asyncPopup.run();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Configures the logging level.
     */
    private static void configureLoggingLevelAll() {
        Logger.getLogger("").setLevel(Level.ALL);
        Logger.getLogger("sun").setLevel(Level.WARNING);
        Logger.getLogger("com.sun").setLevel(Level.WARNING);
        Logger.getLogger("java").setLevel(Level.WARNING);
        Logger.getLogger("javax").setLevel(Level.WARNING);

        // general log-level
        Logger.getLogger("org.openstreetmap").setLevel(Level.INFO);
        // areas currently being watched closely
        Logger.getLogger("org.openstreetmap.travelingsalesman.routing.routers").setLevel(Level.WARNING);
        Logger.getLogger("org.openstreetmap.travelingsalesman.routing.describers").setLevel(Level.FINER);
        // areas currently in development/debugging
        Logger.getLogger("org.openstreetmap.travelingsalesman.trafficblocks").setLevel(Level.FINER);
//        Logger.getLogger("org.openstreetmap.osm.data.osmbin.GeoIndexFile").setLevel(Level.INFO);
//        Logger.getLogger("org.openstreetmap.osm.data.searching.AddressDBPlaceFinder").setLevel(Level.INFO);
//        Logger.getLogger("org.openstreetmap.osm.data.searching.advancedAddressDB.AdvancedAddressDBPlaceFinder").setLevel(Level.INFO);
//        Logger.getLogger("org.openstreetmap.osm.data.FileTileDataSet").setLevel(Level.FINEST);
        //Logger.getLogger("org.openstreetmap.osm.data.TileCalculator").setLevel(Level.FINEST);
        // do not log every GPS-location-fix
        Logger.getLogger("org.openstreetmap.travelingsalesman.gps.GpsDProvider").setLevel(Level.FINE);
        //Logger.getLogger("org.openstreetmap.travelingsalesman.gui.widgets.BasePaintVisitor").setLevel(Level.WARNING);

        // we are debugging the hsqldb-code
        //Logger.getLogger("org.openstreetmap.osm.data.hsqldb").setLevel(Level.FINER);
        Logger.getLogger("org.openstreetmap.travelingsalesman.painting").setLevel(Level.WARNING);
        //Logger.getLogger("org.openstreetmap.osm.data.DBDataSet").setLevel(Level.FINER);
//        Logger.getLogger("org.openstreetmap.osm.data.osmbin").setLevel(Level.FINEST);
    }

}
