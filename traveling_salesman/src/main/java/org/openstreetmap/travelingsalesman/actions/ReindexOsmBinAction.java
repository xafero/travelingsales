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
package org.openstreetmap.travelingsalesman.actions;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.LODDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.OsmBinDataSet;
import org.openstreetmap.osm.data.osmbin.v1_0.OsmBinV10Reindexer;
import org.openstreetmap.travelingsalesman.gui.MainFrame;


/**
 * This action allows recreating the index if the current DataSet is an
 * OsmBin-Dataset or LODDataSet with OsmBinDatasets..
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class ReindexOsmBinAction extends AbstractAction {

    /**
     * Our OpenInJosmAction.java.
     */
    private static final long serialVersionUID = 1L;

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(ReindexOsmBinAction.class.getName());

    /**
     * We need this to get the map and the visible area.
     */
    private MainFrame myNavigatableComponent;

    /**
     * @param aNavigatableComponent We need this to get the map and the visible area.
     */
    public ReindexOsmBinAction(final MainFrame aNavigatableComponent) {
        super("Reindex OsmBin");
        this.myNavigatableComponent = aNavigatableComponent;
        putValue(Action.SHORT_DESCRIPTION, "Recreate the .idx -files for your .obm");
        /*
         * action.putValue(Action.SMALL_ICON, new ImageIcon(TaskPaneMain.class
                  .getResource(iconPath)));
         */
    }

    /**
     * ${@inheritDoc}.
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(final ActionEvent arg0) {
        if (myNavigatableComponent != null) {
            myNavigatableComponent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }

//        File tempFile = null;
        try {
            IDataSet dataSet = myNavigatableComponent.getOsmData();
            if (dataSet instanceof OsmBinDataSet) {
                OsmBinDataSet baseDataSet = (OsmBinDataSet) dataSet;
                File dir = baseDataSet.getDataDirectory();
                myNavigatableComponent.setOsmData(new MemoryDataSet());
                baseDataSet.shutdown();
                reindex(dir);
                myNavigatableComponent.setOsmData(new OsmBinDataSet(dir));
            } else if (dataSet instanceof LODDataSet) {
                LODDataSet lodDataSet = (LODDataSet) dataSet;
                if (lodDataSet.getBaseDataSet()  instanceof OsmBinDataSet) {
                    OsmBinDataSet baseDataSet = (OsmBinDataSet) lodDataSet.getBaseDataSet();
                    File dir = baseDataSet.getDataDirectory();
                    lodDataSet.setBaseDataSet(new MemoryDataSet());
                    baseDataSet.shutdown();
                    reindex(dir);
                    lodDataSet.setBaseDataSet(new OsmBinDataSet(dir));
                }
                if (lodDataSet.getLOD1DataSet()  instanceof OsmBinDataSet) {
                    OsmBinDataSet baseDataSet = (OsmBinDataSet) lodDataSet.getLOD1DataSet();
                    File dir = baseDataSet.getDataDirectory();
                    lodDataSet.setLOD1DataSet(new MemoryDataSet());
                    baseDataSet.shutdown();
                    reindex(dir);
                    lodDataSet.setLOD1DataSet(new OsmBinDataSet(dir));
                }
                if (lodDataSet.getLOD2DataSet()  instanceof OsmBinDataSet) {
                    OsmBinDataSet baseDataSet = (OsmBinDataSet) lodDataSet.getLOD2DataSet();
                    File dir = baseDataSet.getDataDirectory();
                    lodDataSet.setLOD2DataSet(new MemoryDataSet());
                    baseDataSet.shutdown();
                    reindex(dir);
                    lodDataSet.setLOD2DataSet(new OsmBinDataSet(dir));
                }
                if (lodDataSet.getLOD3DataSet()  instanceof OsmBinDataSet) {
                    OsmBinDataSet baseDataSet = (OsmBinDataSet) lodDataSet.getLOD3DataSet();
                    File dir = baseDataSet.getDataDirectory();
                    lodDataSet.setLOD3DataSet(new MemoryDataSet());
                    baseDataSet.shutdown();
                    reindex(dir);
                    lodDataSet.setLOD3DataSet(new OsmBinDataSet(dir));
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot reindex", e);
            return;
        } finally {
            if (myNavigatableComponent != null) {
                myNavigatableComponent.setCursor(Cursor.getDefaultCursor());
            }
        }
    }

    /**
     * Reindex the given directory with OsmBin-Data.
     * @param aDir the directory
     */
    private void reindex(final File aDir) {
        OsmBinV10Reindexer reindexer = new OsmBinV10Reindexer(aDir);
        reindexer.run();
    }

}
