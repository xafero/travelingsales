
package org.openstreetmap.travelingsalesman.actions;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.IHintableDataSet;
import org.openstreetmap.osm.data.LODDataSet;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.EastNorth;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedNode;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedWay;
import org.openstreetmap.osm.data.searching.IAddressDBPlaceFinder;
import org.openstreetmap.osm.data.searching.advancedAddressDB.AdvancedAddressDBPlaceFinder;
import org.openstreetmap.osm.data.visitors.BoundingXYVisitor;
import org.openstreetmap.osm.io.FileLoader;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.common.ChangeAction;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.gui.MainFrame;
import org.openstreetmap.travelingsalesman.routing.selectors.UsedTags;
import org.openstreetmap.travelingsalesman.trafficblocks.TMCLocationIndexer;

/**
 * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: traveling_salesman<br/>
 * LoadMapFileActionListener.java<br/>
 *<br/><br/>
 * <b>ActionListener for loading a map from a URL or file.</b>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */

public final class LoadMapFileActionListener implements ActionListener {

    /**
     * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
     * Project: traveling_salesman<br/>
     * LoadMapFileActionListener.java<br/>
     * created: 05.04.2009 07:57:16 <br/>
     *<br/><br/>
     * <b>This Sink does only count how many Nodes, Ways and Relations exist in a file.</b>
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    public static class CountItemsSink implements Sink, ChangeSink {

        /**
         * system-time when we started.
         */
        private long myStartTime = System.currentTimeMillis();

        /**
         * Number of {@link Node}s counted so far.
         */
        private long myNodesCount = 0;

        /**
         * @return Returns the nodesCount.
         * @see #myNodesCount
         */
        public long getNodesCount() {
            return myNodesCount;
        }

        /**
         * @return Returns the waysCount.
         * @see #myWaysCount
         */
        public long getWaysCount() {
            return myWaysCount;
        }

        /**
         * @return Returns the relationsCount.
         * @see #myRelationsCount
         */
        public long getRelationsCount() {
            return myRelationsCount;
        }

        /**
         * Number of {@link Way}s counted so far.
         */
        private long myWaysCount = 0;

        /**
         * Number of {@link Relation}s counted so far.
         */
        private long myRelationsCount = 0;
        /**
         * Just an overridden ToString to return this classe's name
         * and hashCode.
         * @return className and hashCode
         */
        public String toString() {
            return "CountItemsSink@" + hashCode();
        }
        /**
         * {@inheritDoc}
         */
        @Override
        public void process(final ChangeContainer aChange) {
            ChangeAction action = aChange.getAction();
            EntityContainer entityContainer = aChange.getEntityContainer();

            switch (action) {
            default:
            case Modify:
            case Create:
                process(entityContainer);
                break;
            case Delete:
                break;
            }
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void process(final EntityContainer aEntityContainer) {
            if (aEntityContainer instanceof NodeContainer) {
                myNodesCount++;
            } else if (aEntityContainer instanceof WayContainer) {
                myWaysCount++;
            } else if (aEntityContainer instanceof RelationContainer) {
                myRelationsCount++;
            }

        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void complete() {
            LOG.info("counting the nodes, ways and relations to import took us : " + (System.currentTimeMillis() - myStartTime) + "ms");
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void release() {
            // ignored

        }

		@Override
		public void initialize(Map<String, Object> map) {
			// TODO: Check that!
			System.out.println("implement me!");
		}
    }

    /**
     * Sink that adds the supplied entities to {@link #myCurrentData}.
     */
    private static final class AddToMapSink implements Sink, ChangeSink {
        /**
         * A very small number of milliseconds to make sure we never divide by 0.
         */
        private static final float SMALLTIME = 0.01f;
        /**
         * Assumed time-different between importing a node and a way.
         * Used for predicting the time left.
         */
        private static final int NODETORELATIONTIMEFACTOR = 34;
        /**
         * Assumed time-different between importing a node and a relation.
         * Used for predicting the time left.
         */
        private static final int NODETOWAYTIMEFACTOR = 17;

        /**
         * Bounds of the imported area.
         */
        private BoundingXYVisitor myBounds;

        /**
         * The map we import into.
         */
        private final IDataSet              myCurrentData;
        /**
         * Have this AddressDB index the streets.
         */
        private final IAddressDBPlaceFinder myAddresses;
        /**
         * Display out progress on this label.
         */
        private final JLabel                myStatusLabel;
        /**
         * How many nodes have we import4ed so far?
         */
        private long countNode = 0;
        /**
         * How many ways have we import4ed so far?
         */
        private long countWay = 0;
        /**
         * How many relations have we import4ed so far?
         */
        private long countRelation = 0;
        /**
         * We filter the map with this selector to not load
         * what we don`t need.
         */
        private UsedTags myselector;
        /**
         * Optional: a known count of how many items get imported.
         */
        private CountItemsSink myItemCount;

        /**
         * System-time when we started.
         */
        private long myStartTime;
        /**
         * System-time when we received the last node.
         */
        private long myLastNodeTime = System.currentTimeMillis();
        /**
         * System-time when we received the last way.
         */
        private long myLastWayTime = System.currentTimeMillis();

        /**
         * Progressbar for the {@link #myStatusLabel}.
         */
        private JProgressBar myProgressBar;

        /**
         *  index all TMC locationCodes found.
         */
        private TMCLocationIndexer myTMCIndexer = new TMCLocationIndexer();

        /**
         * DateFormat to show the time left.
         */
        private SimpleDateFormat myDateFormat = new SimpleDateFormat("HH:mm:ss");

        /**
         * @param aCurrentData The map we import into.
         * @param aAddresses have this addressDB index the streets
         * @param aStatusLabel Display out progress on this label.
         * @param aProgressBar progressbar
         * @param aItemCount a known count of how many items get imported.
         */
        private AddToMapSink(final IDataSet aCurrentData,
                final IAddressDBPlaceFinder aAddresses,
                final JLabel aStatusLabel,
                final JProgressBar aProgressBar, final CountItemsSink aItemCount) {
            this(aCurrentData, aAddresses, aStatusLabel);
            this.myItemCount = aItemCount;
            this.myProgressBar = aProgressBar;
            if (myProgressBar != null) {
                myProgressBar.setValue(0);
                myProgressBar.setMaximum((int) (myItemCount.getNodesCount() + myItemCount.getWaysCount() + myItemCount.getRelationsCount()));
                myProgressBar.setIndeterminate(false);
                myProgressBar.setString(null);
                myProgressBar.setStringPainted(true);
            }
        }
        /**
         * @param aCurrentData The map we import into.
         * @param aAddresses have this addressDB index the streets
         * @param aStatusLabel Display out progress on this label.
         */
        private AddToMapSink(final IDataSet aCurrentData,
                final IAddressDBPlaceFinder aAddresses,
                final JLabel aStatusLabel) {
            this.myCurrentData = aCurrentData;
            this.myBounds = new BoundingXYVisitor(aCurrentData);
            this.myAddresses = aAddresses;
            this.myStatusLabel = aStatusLabel;
            if (Settings.getInstance().getBoolean("filtermap", false)) {
                this.myselector = new UsedTags();
            }
            this.myItemCount = null;
            myStartTime = System.currentTimeMillis();
        }

        /**
         * {@inheritDoc}
         */
        public void complete() {
            long endBounds = System.currentTimeMillis();
            LOG.log(Level.INFO, "Imported new map-data in:\n"
                    + "\t" + countNode + " nodes in " + (myLastNodeTime - myStartTime) + "ms = " + ((1.0 * countNode) / (myLastNodeTime - myStartTime)) + " nodes/ms\n"
                    + "\t" + countWay + " ways in " + (myLastWayTime - myLastNodeTime) + "ms = " + ((1.0 * countWay) / (myLastWayTime - myLastNodeTime)) + " ways/ms\n"
                    + "\t" + countRelation + " relations in " + (endBounds - myLastWayTime) + "ms = " + ((1.0 * countRelation) / (endBounds - myLastWayTime)) + " relations/ms\n"
                    + "\tsum " + (endBounds - myStartTime) + "ms\n");
            if (myselector != null) {
                LOG.log(Level.INFO, "We ignored " + myselector.getIgnoredTagsCount() + " tags");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void process(final ChangeContainer aChange) {
            ChangeAction action = aChange.getAction();
            EntityContainer entityContainer = aChange.getEntityContainer();

            switch (action) {
            default:
            case Modify:
            case Create:
                process(entityContainer);
                break;
            case Delete:
                if (entityContainer instanceof NodeContainer) {
                    NodeContainer nc = (NodeContainer) entityContainer;
                    myCurrentData.removeNode((Node) nc.getEntity());
                } else
                if (entityContainer instanceof WayContainer) {
                    WayContainer wc = (WayContainer) entityContainer;
                    myCurrentData.removeWay((Way) wc.getEntity());
                } else
                if (entityContainer instanceof RelationContainer) {
                    RelationContainer rc = (RelationContainer) entityContainer;
                    myCurrentData.removeRelation((Relation) rc.getEntity());
                }
                break;
            }
        }

        /**
         * {@inheritDoc}
         */
        public void process(final EntityContainer aEntityContainer) {
            final int nodeReportThreshold = 125;
            final int wayReportThreshold = 50;
            final int relReportThreshold = 5;

            if (aEntityContainer instanceof NodeContainer) {
                if (countNode % nodeReportThreshold == 1) {
                    String countStr = null;
                    if (myItemCount == null) {
                        countStr = Long.toString(countNode);
                    } else {
                        countStr = Long.toString(countNode) + "/" + myItemCount.getNodesCount();
                        if (myProgressBar != null) {
                            myProgressBar.setValue((int) countNode);
                         // predict the time left
                            float timePerNode = ((System.currentTimeMillis() - myStartTime + SMALLTIME) / countNode);
                            long itemsLeft = NODETORELATIONTIMEFACTOR * myItemCount.getRelationsCount()
                                           + NODETOWAYTIMEFACTOR * myItemCount.getWaysCount()
                                           + (myItemCount.getNodesCount() - countNode);
                            Date timeLeft = new Date((long) (timePerNode * itemsLeft) - myDateFormat.getTimeZone().getRawOffset());
                            myProgressBar.setString(myDateFormat.format(timeLeft));
                        }
                    }
                    if (myStatusLabel != null) {
                        myStatusLabel.setText(MessageFormat.format(
                                MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.loadingNodes"),
                                new Object[]{countStr}));
                    } else {
                        // used  on command-line
                        System.out.println(MessageFormat.format(
                                MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.loadingNodes"),
                                new Object[]{countStr}));
                    }
                }
                NodeContainer nc = (NodeContainer) aEntityContainer;
                Node node = (Node) nc.getEntity();
                if (myselector != null) {
                    if (!myselector.isAllowed(myCurrentData, nc.getEntity())) {
                        return;
                    }
                    node = myselector.filterNode(node);
                }
                countNode++;
                myCurrentData.addNode(node);
                myAddresses.indexNode(node);
                myTMCIndexer.visit(node);
                myBounds.visit(node);
                this.myLastNodeTime = System.currentTimeMillis();
            }

            if (aEntityContainer instanceof WayContainer) {
                if (countWay % wayReportThreshold == 1) {
                    String countStr = null;
                    if (myItemCount == null) {
                        countStr = Long.toString(countWay);
                    } else {
                        countStr = Long.toString(countWay) + "/" + myItemCount.getWaysCount();
                        if (myProgressBar != null) {
                            myProgressBar.setValue((int) (countNode + countWay));
                         // predict the time left
                            float timePerWay = ((System.currentTimeMillis() - myLastNodeTime + SMALLTIME) / countWay);
                            long itemsLeft = (NODETORELATIONTIMEFACTOR * myItemCount.getRelationsCount() / NODETOWAYTIMEFACTOR)
                                          + (myItemCount.getWaysCount() - countWay);
                            Date timeLeft = new Date((long) (timePerWay * itemsLeft) - myDateFormat.getTimeZone().getRawOffset());
                            myProgressBar.setString(myDateFormat.format(timeLeft));
                        }
                    }
                    if (myStatusLabel != null) {
                        myStatusLabel.setText(MessageFormat.format(
                            MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.loadingWays"),
                            new Object[]{countStr}));
                    } else {
                        // command-line
                        System.out.println(MessageFormat.format(
                                MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.loadingWays"),
                                new Object[]{countStr}));
                    }
                }
                WayContainer wc = (WayContainer) aEntityContainer;
                Way way = wc.getEntity();
                if (myselector != null) {
                    if (!myselector.isAllowed(myCurrentData, wc.getEntity())) {
                        return;
                    }
                    way = myselector.filterWay(way);
                }
                countWay++;
                myCurrentData.addWay(way);
                myAddresses.indexWay(way);
                myTMCIndexer.visit(way);
                this.myLastWayTime = System.currentTimeMillis();
            }

            if (aEntityContainer instanceof RelationContainer) {
                if (countRelation % relReportThreshold == 1) {
                    String countStr = null;
                    if (myItemCount == null) {
                        countStr = Long.toString(countRelation);
                    } else {
                        countStr = Long.toString(countRelation) + "/" + myItemCount.getRelationsCount();
                        if (myProgressBar != null) {
                            myProgressBar.setValue((int) (countNode + countWay + countRelation));
                         // predict the time left
                            float timePerRelation = ((System.currentTimeMillis() - myLastWayTime + SMALLTIME) / countRelation);
                            long itemsLeft = (myItemCount.getRelationsCount() - countRelation);
                            Date timeLeft = new Date((long) (timePerRelation * itemsLeft) - myDateFormat.getTimeZone().getRawOffset());
                            myProgressBar.setString(myDateFormat.format(timeLeft));
                        }
                    }
                    if (myStatusLabel != null) {
                        myStatusLabel.setText(MessageFormat.format(
                            MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.loadingRelations"),
                            new Object[]{countStr}));
                    } else {
                        // used  on command-line
                        System.out.println(MessageFormat.format(
                                MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.loadingRelations"),
                                new Object[]{countStr}));
                    }
                }
                RelationContainer rc = (RelationContainer) aEntityContainer;
                if (myselector != null) {
                    if (!myselector.isAllowed(myCurrentData, rc.getEntity())) {
                        return;
                    }
                }
                countRelation++;
                myCurrentData.addRelation(rc.getEntity());
                myTMCIndexer.visit(rc.getEntity());
                //addresses.indexRelation((Relation) rc.getEntity());
            }
        }

        /**
         * {@inheritDoc}
         */
        public void release() {
            if (myProgressBar != null) {
                myProgressBar.setString(null);
            }
        }
        /**
         * @return the bounds of all imported nodes
         */
        public Bounds getBounds() {
            return myBounds.getBounds();
        }
		@Override
		public void initialize(Map<String, Object> map) {
			// TODO: Check that implementation!
			System.out.println("implement me!");
		}
    }


    /**
     * Sink that verifies that the supplied entities have been added to {@link #myCurrentData}.
     */
    private static final class VerifyToMapSink implements Sink, ChangeSink {

        /**
         * my logger for debug and error-output.
         */
        private static final Logger LOG = Logger.getLogger(VerifyToMapSink.class.getName());

        /**
         * A very small number of milliseconds to make sure we never divide by 0.
         */
        private static final float SMALLTIME = 0.01f;
        /**
         * Assumed time-different between importing a node and a way.
         * Used for predicting the time left.
         */
        private static final int NODETORELATIONTIMEFACTOR = 34;
        /**
         * Assumed time-different between importing a node and a relation.
         * Used for predicting the time left.
         */
        private static final int NODETOWAYTIMEFACTOR = 17;

        /**
         * The map we import into.
         */
        private final IDataSet              myCurrentData;
        /**
         * Display out progress on this label.
         */
        private final JLabel                myStatusLabel;
        /**
         * How many nodes have we import4ed so far?
         */
        private long countNode = 0;
        /**
         * How many ways have we import4ed so far?
         */
        private long countWay = 0;
        /**
         * How many relations have we import4ed so far?
         */
        private long countRelation = 0;
        /**
         * We filter the map with this selector to not load
         * what we don`t need.
         */
        private UsedTags myselector;
        /**
         * Optional: a known count of how many items get imported.
         */
        private CountItemsSink myItemCount;

        /**
         * System-time when we started.
         */
        private long myStartTime;
        /**
         * System-time when we received the last node.
         */
        private long myLastNodeTime = System.currentTimeMillis();
        /**
         * System-time when we received the last way.
         */
        private long myLastWayTime = System.currentTimeMillis();

        /**
         * Progressbar for the {@link #myStatusLabel}.
         */
        private JProgressBar myProgressBar;

        /**
         * DateFormat to show the time left.
         */
        private SimpleDateFormat myDateFormat = new SimpleDateFormat("HH:mm:ss");

        /**
         * @param aCurrentData The map we import into.
         * @param aStatusLabel Display out progress on this label.
         * @param aProgressBar progressbar
         * @param aItemCount a known count of how many items get imported.
         */
        private VerifyToMapSink(final IDataSet aCurrentData,
                final JLabel aStatusLabel,
                final JProgressBar aProgressBar,
                final CountItemsSink aItemCount) {
            this(aCurrentData, aStatusLabel);
            this.myItemCount = aItemCount;
            this.myProgressBar = aProgressBar;
            if (myProgressBar != null) {
                myProgressBar.setValue(0);
                myProgressBar.setMaximum((int) (myItemCount.getNodesCount() + myItemCount.getWaysCount() + myItemCount.getRelationsCount()));
                myProgressBar.setIndeterminate(false);
                myProgressBar.setString(null);
                myProgressBar.setStringPainted(true);
            }
        }
        /**
         * @param aCurrentData The map we import into.
         * @param aStatusLabel Display out progress on this label.
         */
        private VerifyToMapSink(final IDataSet aCurrentData,
                final JLabel aStatusLabel) {
            IDataSet temp = aCurrentData;
            while (temp instanceof LODDataSet) {
                LODDataSet lod = (LODDataSet) temp;
                temp = lod.getBaseDataSet();
            }
            this.myCurrentData = temp;
            this.myStatusLabel = aStatusLabel;
            if (Settings.getInstance().getBoolean("filtermap", false)) {
                this.myselector = new UsedTags();
            }
            this.myItemCount = null;
            myStartTime = System.currentTimeMillis();
        }

        /**
         * {@inheritDoc}
         */
        public void complete() {
            long endBounds = System.currentTimeMillis();
            LOG.log(Level.INFO, "Imported new map-data in:\n"
                    + "\t" + countNode + " nodes in " + (myLastNodeTime - myStartTime) + "ms = " + ((1.0 * countNode) / (myLastNodeTime - myStartTime)) + " nodes/ms\n"
                    + "\t" + countWay + " ways in " + (myLastWayTime - myLastNodeTime) + "ms = " + ((1.0 * countWay) / (myLastWayTime - myLastNodeTime)) + " ways/ms\n"
                    + "\t" + countRelation + " relations in " + (endBounds - myLastWayTime) + "ms = " + ((1.0 * countRelation) / (endBounds - myLastWayTime)) + " relations/ms\n"
                    + "\tsum " + (endBounds - myStartTime) + "ms\n");
            if (myselector != null) {
                LOG.log(Level.INFO, "We ignored " + myselector.getIgnoredTagsCount() + " tags");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void process(final ChangeContainer aChange) {
            ChangeAction action = aChange.getAction();
            EntityContainer entityContainer = aChange.getEntityContainer();

            switch (action) {
            default:
            case Modify:
            case Create:
                process(entityContainer);
                break;
            case Delete:
//                if (entityContainer instanceof NodeContainer) {
//                    NodeContainer nc = (NodeContainer) entityContainer;
//                    if (myCurrentData.con)
//                    myCurrentData.removeNode((Node) nc.getEntity());
//                } else
//                if (entityContainer instanceof WayContainer) {
//                    WayContainer wc = (WayContainer) entityContainer;
//                    myCurrentData.removeWay((Way) wc.getEntity());
//                } else
//                if (entityContainer instanceof RelationContainer) {
//                    RelationContainer rc = (RelationContainer) entityContainer;
//                    myCurrentData.removeRelation((Relation) rc.getEntity());
//                }
                break;
            }
        }

        /**
         * {@inheritDoc}
         */
        public void process(final EntityContainer aEntityContainer) {
            final int nodeReportThreshold = 125;
            final int wayReportThreshold = 50;
            final int relReportThreshold = 5;

            if (aEntityContainer instanceof NodeContainer) {
                handleNodeContainer((NodeContainer) aEntityContainer, nodeReportThreshold);
            } else

            if (aEntityContainer instanceof WayContainer) {
                handleWay((WayContainer) aEntityContainer, wayReportThreshold);
            } else

            if (aEntityContainer instanceof RelationContainer) {
                handleRelationContainer((RelationContainer) aEntityContainer, relReportThreshold);
            }
        }

        /**
         * Do tha job of {@link #process(EntityContainer)} for relations.
         * @param aEntityContainer the container to import
         * @param relReportThreshold how often to report progress
         */
        private void handleRelationContainer(
                final RelationContainer aEntityContainer,
                final int relReportThreshold) {
            if (countRelation % relReportThreshold == 1) {
                String countStr = null;
                if (myItemCount == null) {
                    countStr = Long.toString(countRelation);
                } else {
                    countStr = Long.toString(countRelation) + "/" + myItemCount.getRelationsCount();
                    if (myProgressBar != null) {
                        myProgressBar.setValue((int) (countNode + countWay + countRelation));
                     // predict the time left
                        float timePerRelation = ((System.currentTimeMillis() - myLastWayTime + SMALLTIME) / countRelation);
                        long itemsLeft = (myItemCount.getRelationsCount() - countRelation);
                        Date timeLeft = new Date((long) (timePerRelation * itemsLeft) - myDateFormat.getTimeZone().getRawOffset());
                        myProgressBar.setString(myDateFormat.format(timeLeft));
                    }
                }
                if (myStatusLabel != null) {
                    myStatusLabel.setText(MessageFormat.format(
                        MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.verifiingRelations"),
                        new Object[]{countStr}));
                } else {
                    // used  on command-line
                    System.out.println(MessageFormat.format(
                            MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.verifiingRelations"),
                            new Object[]{countStr}));
                }
            }
            RelationContainer rc = (RelationContainer) aEntityContainer;
            Relation relation = rc.getEntity();
            if (myselector != null) {
                if (!myselector.isAllowed(myCurrentData, relation)) {
                    return;
                }
            }
            countRelation++;
            if (!myCurrentData.containsRelation(relation)) {
                LOG.severe("Relation " + relation.getId() + " was not imported properly!");
            } else {
                Relation importedRelation = myCurrentData.getRelationByID(relation.getId());
                if (importedRelation.getVersion() == relation.getVersion()) {
                    List<RelationMember> members = relation.getMembers();
                    List<RelationMember> importedMembers = importedRelation.getMembers();
                    if (members.size() != importedMembers.size()) {
                        LOG.severe("Relation " + relation.getId() + " has a different number of members and is not imported properly!");
                    }
                    for (int i = 0; i < members.size(); i++) {
                        if (members.get(i).getMemberType() != importedMembers.get(i).getMemberType()) {
                            LOG.severe("Relation " + relation.getId() + " has a different type for member at index " + i + " and is not imported properly!"
                                    + "\nshould have: " + members.get(i).getMemberType() + " but has: " + members.get(i).getMemberType());
                        } else if (!members.get(i).getMemberRole().equals(importedMembers.get(i).getMemberRole())) {
                            LOG.severe("Relation " + relation.getId() + " has a different role for member at index " + i + " and is not imported properly!"
                                    + "\nshould have: \"" + members.get(i).getMemberRole() + "\" but has: \"" + members.get(i).getMemberRole()+ "\"");
                        } else if (members.get(i).getMemberId() != importedMembers.get(i).getMemberId()) {
                            LOG.severe("Relation " + relation.getId() + " has a different member at index " + i + " and is not imported properly!"
                                    + "\nshould have: " + members.get(i).getMemberId() + " but has: " + members.get(i).getMemberId());
                        } else {

                            RelationMember importedMember = importedMembers.get(i);
                            if (importedMember.getMemberType() == EntityType.Node) {
                                Node n = myCurrentData.getNodeByID(importedMember.getMemberId());
                                if (n instanceof ExtendedNode) {
                                    ExtendedNode nx = (ExtendedNode) n;
                                    if (!nx.getReferencedRelationIDs().contains(relation.getId())) {
                                        LOG.severe("Relation " + relation.getId() + " has a correct member at index " + i
                                                + " BUT that node has no back-reference to the way "
                                                + "and is thus not imported properly!");
                                    }
                                }
                            }
                            if (importedMember.getMemberType() == EntityType.Way) {
                                Way n = myCurrentData.getWaysByID(importedMember.getMemberId());
                                if (n instanceof ExtendedWay) {
                                    ExtendedWay nx = (ExtendedWay) n;
                                    if (!nx.getReferencedRelationIDs().contains(relation.getId())) {
                                        LOG.severe("Relation " + relation.getId() + " has a correct member at index " + i
                                                + " BUT that way has no back-reference to the way "
                                                + "and is thus not imported properly!");
                                    }
                                }
                            }
//                                if (importedMember.getMemberType() == EntityType.Relation) {
//                                    Relation n = myCurrentData.getRelationByID(importedMember.getMemberId());
//                                    if (n instanceof ExtendedRelation) {
//                                        ExtendedRelation nx = (ExtendedRelation) n;
//                                        if (!nx.getReferencedRelationIDs().contains(relation.getId())) {
//                                            LOG.severe("Relation " + relation.getId() + " has a correct member at index " + i
//                                                    + " BUT that relation has no back-reference to the way "
//                                                    + "and is thus not imported properly!");
//                                        }
//                                    }
//                                }
                        }
                    }
                }
            }
        }
        /**
         * Do tha job of {@link #process(EntityContainer)} for nodes.
         * @param aEntityContainer the container to import
         * @param nodeReportThreshold how often to report progress
         */
        private void handleNodeContainer(
                final NodeContainer aEntityContainer,
                final int nodeReportThreshold) {
            if (countNode % nodeReportThreshold == 1) {
                String countStr = null;
                if (myItemCount == null) {
                    countStr = Long.toString(countNode);
                } else {
                    countStr = Long.toString(countNode) + "/" + myItemCount.getNodesCount();
                    if (myProgressBar != null) {
                        myProgressBar.setValue((int) countNode);
                     // predict the time left
                        float timePerNode = ((System.currentTimeMillis() - myStartTime + SMALLTIME) / countNode);
                        long itemsLeft = NODETORELATIONTIMEFACTOR * myItemCount.getRelationsCount()
                                       + NODETOWAYTIMEFACTOR * myItemCount.getWaysCount()
                                       + (myItemCount.getNodesCount() - countNode);
                        Date timeLeft = new Date((long) (timePerNode * itemsLeft) - myDateFormat.getTimeZone().getRawOffset());
                        myProgressBar.setString(myDateFormat.format(timeLeft));
                    }
                }
                if (myStatusLabel != null) {
                    myStatusLabel.setText(MessageFormat.format(
                            MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.verifiingNodes"),
                            new Object[]{countStr}));
                } else {
                    // used  on command-line
                    System.out.println(MessageFormat.format(
                            MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.verifiingNodes"),
                            new Object[]{countStr}));
                }
            }
            NodeContainer nc = (NodeContainer) aEntityContainer;
            Node node = (Node) nc.getEntity();
            if (myselector != null) {
                if (!myselector.isAllowed(myCurrentData, nc.getEntity())) {
                    return;
                }
                node = myselector.filterNode(node);
            }
            countNode++;
            if (!myCurrentData.containsNode(node)) {
               LOG.severe("Node " + node.getId() + " was not imported properly!");
            }
            this.myLastNodeTime = System.currentTimeMillis();
        }
        /**
         * Do tha job of {@link #process(EntityContainer)} for ways.
         * @param aEntityContainer the container to import
         * @param wayReportThreshold how often to report progress
         */
        private void handleWay(final WayContainer aEntityContainer,
                final int wayReportThreshold) {
            if (countWay % wayReportThreshold == 1) {
                String countStr = null;
                if (myItemCount == null) {
                    countStr = Long.toString(countWay);
                } else {
                    countStr = Long.toString(countWay) + "/" + myItemCount.getWaysCount();
                    if (myProgressBar != null) {
                        myProgressBar.setValue((int) (countNode + countWay));
                     // predict the time left
                        float timePerWay = ((System.currentTimeMillis() - myLastNodeTime + SMALLTIME) / countWay);
                        long itemsLeft = (NODETORELATIONTIMEFACTOR * myItemCount.getRelationsCount() / NODETOWAYTIMEFACTOR)
                                      + (myItemCount.getWaysCount() - countWay);
                        Date timeLeft = new Date((long) (timePerWay * itemsLeft) - myDateFormat.getTimeZone().getRawOffset());
                        myProgressBar.setString(myDateFormat.format(timeLeft));
                    }
                }
                if (myStatusLabel != null) {
                    myStatusLabel.setText(MessageFormat.format(
                        MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.verifiingWays"),
                        new Object[]{countStr}));
                } else {
                    // command-line
                    System.out.println(MessageFormat.format(
                            MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.verifiingWays"),
                            new Object[]{countStr}));
                }
            }
            WayContainer wc = (WayContainer) aEntityContainer;
            Way way = wc.getEntity();
            if (myselector != null) {
                if (!myselector.isAllowed(myCurrentData, wc.getEntity())) {
                    return;
                }
                way = myselector.filterWay(way);
            }
            countWay++;
            if (!myCurrentData.containsWay(way)) {
                LOG.severe("Way " + way.getId() + " was not imported properly!");
            } else {
                Way importedWay = myCurrentData.getWaysByID(way.getId());
                if (importedWay.getVersion() == way.getVersion()) {
                    List<WayNode> wayNodes = way.getWayNodes();
                    List<WayNode> importedWayNodes = importedWay.getWayNodes();
                    if (wayNodes.size() != importedWayNodes.size()) {
                        LOG.severe("Way " + way.getId() + " has a different number of wayNodes and is not imported properly!");
                    } else
                    for (int i = 0; i < wayNodes.size(); i++) {
                        if (wayNodes.get(i).getNodeId() != importedWayNodes.get(i).getNodeId()) {
                            LOG.severe("Way " + way.getId() + " has a different wayNode at index " + i + " and is not imported properly!");
                        } else {
                            Node n = myCurrentData.getNodeByID(importedWayNodes.get(i).getNodeId());
                            if (n instanceof ExtendedNode) {
                                ExtendedNode nx = (ExtendedNode) n;
                                if (!nx.getReferencedWayIDs().contains(way.getId())) {
                                    LOG.severe("Way " + way.getId() + " has a correct wayNode at index " + i
                                            + " BUT that node has no back-reference to the way "
                                            + "and is thus not imported properly!"
                                            + "\nHas references to ways: " + Arrays.toString(nx.getReferencedWayIDs().toArray()));
                                }
                            }
                        }
                    }
                }
            }
            this.myLastWayTime = System.currentTimeMillis();
        }

        /**
         * {@inheritDoc}
         */
        public void release() {
            if (myProgressBar != null) {
                myProgressBar.setString(null);
            }
        }
		@Override
		public void initialize(Map<String, Object> map) {
			throw new RuntimeException("implement me!");
		}
    }

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(LoadMapFileActionListener.class.getName());

    /**
     * Display our progress here.
     */
    private JLabel myStatusBarLabel;
    /**
     * Display our progress here.
     */
    private JProgressBar myProgressBar;
    /**
     * Import into this map.
     */
    private IDataSet myCurrentData;
    /**
     * May be null. Used to set the cursor.
     */
    private Frame myParentComponent;
    /**
     * Import this file.
     */
    private File myFileToLoad;
    /**
     * @param aParentComponent may be null. Used to set the cursor.
     * @param aStatusBarLabel may NOT be null
     * @param aJProgressBar may NOT be null
     * @param currentData the map to load into
     */
    public LoadMapFileActionListener(final Frame aParentComponent, final JLabel aStatusBarLabel, final JProgressBar aJProgressBar, final IDataSet currentData) {
        if (currentData == null) {
            throw new IllegalArgumentException("null map to import into given!");
        }
        this.myStatusBarLabel = aStatusBarLabel;
        this.myProgressBar = aJProgressBar;
        this.myCurrentData = currentData;
        this.myParentComponent = aParentComponent;
        this.myFileToLoad = null;
    }
    /**
     * @param aParentComponent may be null. Used to set the cursor.
     * @param aStatusBarLabel may NOT be null
     * @param aJProgressBar may NOT be null
     * @param currentData the map to load into
     * @param aFileToLoad the file to load
     */
    public LoadMapFileActionListener(final Frame aParentComponent,
                                     final JLabel aStatusBarLabel,
                                     final JProgressBar aJProgressBar,
                                     final IDataSet currentData,
                                     final File aFileToLoad) {
        if (currentData == null) {
            throw new IllegalArgumentException("null map to import into given!");
        }
        this.myStatusBarLabel = aStatusBarLabel;
        this.myProgressBar = aJProgressBar;
        this.myCurrentData = currentData;
        this.myParentComponent = aParentComponent;
        this.myFileToLoad = aFileToLoad;
    }

    /**
     * {@inheritDoc}.
     */
    public void actionPerformed(final ActionEvent arg0) {
        try {
            if (this.myFileToLoad != null && this.myFileToLoad.exists() && this.myFileToLoad.isFile()) {
                final File selectedFile = this.myFileToLoad;
                Settings.getInstance().put("traveling-salesman.loadFile.lastPath", selectedFile.getParent());
                (new Thread("load map-file") {

                    public void run() {
                        loadMapFile(myParentComponent, selectedFile, myStatusBarLabel, myProgressBar, myCurrentData);
                    }
                }).start();
                return;
            }

            JFileChooser fileChooser = new JFileChooser(new File(Settings.getInstance().get("traveling-salesman.loadFile.lastPath", ".")));
            fileChooser.setAcceptAllFileFilterUsed(true);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.addChoosableFileFilter(new FileFilter() {

                @Override
                public boolean accept(final File file) {
                    if (file.isDirectory())
                        return true;
                    String fileName = file.getName().toLowerCase();
                    return fileName.endsWith(".osc")
                          || fileName.endsWith(".osc.gz")
                          || fileName.endsWith(".osc.bz2")
                          || fileName.endsWith(".xml")
                          || fileName.endsWith(".xml.gz")
                          || fileName.endsWith(".xml.bz2");
                }

                @Override
                public String getDescription() {
                    return "OSM-Change-File (.osc/.diff.xml)";
                }
            });
            fileChooser.addChoosableFileFilter(new FileFilter() {

                @Override
                public boolean accept(final File file) {
                    if (file.isDirectory())
                        return true;
                    String fileName = file.getName().toLowerCase();
                    return fileName.endsWith(".osm")
                          || fileName.endsWith(".osm.gz")
                          || fileName.endsWith(".osm.bz2")
                          || fileName.endsWith(".xml")
                          || fileName.endsWith(".xml.gz")
                          || fileName.endsWith(".xml.bz2");
                }

                @Override
                public String getDescription() {
                    return "OSM-File (.osm/.xml)";
                }
            });

            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                final File selectedFile = fileChooser.getSelectedFile();
                Settings.getInstance().put("traveling-salesman.loadFile.lastPath", selectedFile.getParent());
                (new Thread("load map file") {

                    public void run() {
                        Bounds bounds = loadMapFile(myParentComponent, selectedFile, myStatusBarLabel, myProgressBar, myCurrentData);
                        if (bounds != null && myParentComponent instanceof MainFrame) {
                            try {
                                MainFrame mf = (MainFrame) myParentComponent;
                                Component[] components = mf.getContentPane().getComponents();
                                for (Component component : components) {
                                    if (component instanceof INavigatableComponent) {
                                        INavigatableComponent nc = (INavigatableComponent) component;
                                        nc.zoomTo(nc.getProjection().latlon2eastNorth(bounds.getCenter()), nc.getScale());
                                    }
                                    if (component instanceof Container) {
                                        Component[] components2 = ((Container) component).getComponents();
                                        for (Component component2 : components2) {
                                            if (component2 instanceof INavigatableComponent) {
                                                INavigatableComponent nc = (INavigatableComponent) component2;
                                                nc.zoomTo(nc.getProjection().latlon2eastNorth(bounds.getCenter()), nc.getScale());
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                LOG.log(Level.WARNING, "Cannot zoom to imported area", e);
                            }
                        } else {
                            // put bounds center and scale to Settings
                            if (bounds != null) {
                                final LatLon center = bounds.getCenter();
//                                Settings.getInstance().put("state.MapPanel.north", Double.toString(center.north()));
//                                Settings.getInstance().put("state.MapPanel.east", Double.toString(center.east()));
                                Settings.getInstance().put("state.MapPanel.latitude", Double.toString(center.lat()));
                                Settings.getInstance().put("state.MapPanel.longitude", Double.toString(center.lon()));
                                final int height = Settings.getInstance().getInteger("state.MainFrame.height", MainFrame.DEFAULTMAINFRAMEHEIGHT);
                                final int width  = Settings.getInstance().getInteger("state.MainFrame.width",  MainFrame.DEFAULTMAINFRAMEWIDTH);
                                EastNorth min = Settings.getProjection().latlon2eastNorth(bounds.getMin());
                                EastNorth max = Settings.getProjection().latlon2eastNorth(bounds.getMax());
                                double deltaNorth = max.north() - min.north();
                                double deltaEast  = max.east()  - min.east();
                                if (deltaNorth > 0 && deltaEast > 0 && height > 0 && width > 0) {
                                    double scale = Math.max(deltaNorth / height, deltaEast / width);
                                    Settings.getInstance().put("state.MapPanel.scale", String.valueOf(scale * Settings.getProjection().scaleFactor()));
                                }
                            }
                        }
                    }
                }).start();
            }
        } catch (Exception e) {
           e.printStackTrace();
        }
    }

    /**
     * Size of the buffer when reading from a URL.
     */
    private static final int READBUFFERSIZE = 255;

    /**
     * Load the given URL as the new map.
     * @param aParentComponent may be null. Used to set the cursor.
     * @param statusLabel may NOT be null
     * @param progressBar may NOT be null
     * @param currentData the map to load into
     * @param aSelectedFile the file
     * @return the bounds of the imported area
     * @throws IOException of we cannot load the file
     */
    public static Bounds loadMapURL(final Frame aParentComponent,
                                    final URL aSelectedFile,
                                    final JLabel statusLabel,
                                    final JProgressBar progressBar,
                                    final IDataSet currentData)  throws IOException {
        try {
            if (statusLabel != null) {
                statusLabel.setText(MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.downloadingFile"));
            }
            if (progressBar != null) {
                progressBar.setMaximum(1);
                progressBar.setValue(0);
                progressBar.setIndeterminate(true);
            }

            // download into temp-file
            String extension = aSelectedFile.getPath();
            extension = extension.substring(extension.indexOf("."));
            File file = File.createTempFile("downloaded", "." + extension);

            FileOutputStream fout = new FileOutputStream(file);

            InputStream stream = aSelectedFile.openStream();
            byte[] buffer = new  byte[READBUFFERSIZE];
            int len = -1;
            long sum = 0;
            long lastsum = 0;
            final int kilo = 1024;
            while ((len = stream.read(buffer)) > 0) {
                fout.write(buffer, 0, len);
                sum += len;
                if (lastsum != (sum / kilo) && statusLabel != null) {
                    statusLabel.setText(MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.downloadingFile") + " " + (sum / kilo) + "Kb");
                    lastsum = sum / kilo;
                }
            }
            fout.close();
            stream.close();
            return LoadMapFileActionListener.loadMapFile(aParentComponent, file, statusLabel, progressBar, currentData);
        } catch (Throwable x) {
            LOG.log(Level.SEVERE, "Exception while loading URL:", x);
            if (statusLabel != null) {
                statusLabel.setText(MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.Errorhappened"));
            }

            JOptionPane.showMessageDialog(null,
                    MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.Errorhappened")
                    + "\n" + x.getLocalizedMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            throw new IOException(x.getMessage(), x);
        } finally {
            if (aParentComponent != null) {
                aParentComponent.setCursor(Cursor.getDefaultCursor());
            }
            if (progressBar != null) {
                progressBar.setValue(2);
                progressBar.setIndeterminate(false);
            }
        }

    }
    /**
     * Load the given file as the new map.
     * @param aParentComponent may be null. Used to set the cursor.
     * @param statusLabel may NOT be null
     * @param progressBar may NOT be null
     * @param currentData the map to load into
     * @param aSelectedFile the file
     * @return the bounds of the imported area
     */
    public static Bounds loadMapFile(final Frame aParentComponent, final File aSelectedFile, final JLabel statusLabel, final JProgressBar progressBar, final IDataSet currentData) {
        Bounds retval = null;
        if (aParentComponent != null) {
            aParentComponent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

        try {
            if (statusLabel != null) {
                statusLabel.setText(MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.loadingFile"));
            }
            if (progressBar != null) {
                progressBar.setMaximum(2);
                progressBar.setValue(0);
                progressBar.setIndeterminate(true);
            }
            FileLoader loader = new FileLoader(aSelectedFile);

            if (progressBar != null) {
                progressBar.setValue(1);
            }
            // index all postal addresses found
            final IAddressDBPlaceFinder addresses = new AdvancedAddressDBPlaceFinder();
            addresses.setMap(currentData);

            // find out how many entities there are for the progress-bar
            if (statusLabel == null) {
                // used on the command-line
                System.out.println("counting items to import....");
            }
            CountItemsSink countSink = new CountItemsSink();
            loader.parseOsm(countSink);
            AddToMapSink sink = new AddToMapSink(currentData, addresses, statusLabel, progressBar, countSink);

            if (currentData instanceof IHintableDataSet) {
                ((IHintableDataSet) currentData).hintImportStarting();
            }
            try {
                loader.parseOsm(sink);
            } finally {
                if (currentData instanceof IHintableDataSet) {
                    ((IHintableDataSet) currentData).hintImportEnded();
                }
            }

            if (Settings.getInstance().getBoolean("verifyMapImports", false)) {
                VerifyToMapSink vSink = new VerifyToMapSink(currentData, statusLabel, progressBar, countSink);
                loader.parseOsm(vSink);
            }

            retval = sink.getBounds();
            if (progressBar != null) {
                progressBar.setValue(2);
                progressBar.setIndeterminate(false);
            }
            if (statusLabel != null) {
                statusLabel.setText("");
            }
        } catch (Throwable x) {
            LOG.log(Level.SEVERE, "Exception while loading file:", x);
            if (statusLabel != null) {
                statusLabel.setText(MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.Errorhappened"));
            }

            JOptionPane.showMessageDialog(null,
                    MainFrame.RESOURCE.getString("Main.Menu.Map.OpenFile.Status.Errorhappened")
                    + "\n" + x.getLocalizedMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);

        } finally {
            if (aParentComponent != null) {
                aParentComponent.setCursor(Cursor.getDefaultCursor());
            }
            if (progressBar != null) {
                progressBar.setValue(2);
                progressBar.setIndeterminate(false);
            }
        }

        if (progressBar != null) {
            progressBar.setIndeterminate(false);
            progressBar.setValue(progressBar.getMaximum());
        }
        return retval;
    }

}
