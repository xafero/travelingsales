/**
 * 
 */
package org.openstreetmap.travelingsalesman.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.travelingsalesman.gps.data.GpsTracksStorage;
import org.openstreetmap.travelingsalesman.gui.MainFrame;

/**
 * Action to show a file-open dialoag and load new
 * tracks into a {@link GpsTracksStorage}.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class LoadTrackFileAction extends AbstractAction {

    /**
     * generated.
     */
    private static final long serialVersionUID = -7676902074525228578L;
    /**
     * Where we add the track to.
     */
    private GpsTracksStorage myTracksStorage;

    /**
     * @param aTracksStorage Where we add the track to
     */
    public LoadTrackFileAction(final GpsTracksStorage aTracksStorage) {
        super(MainFrame.RESOURCE.getString("Main.Menu.Map.OpenTrackFile"));
        putValue(Action.SHORT_DESCRIPTION, MainFrame.RESOURCE.getString("Main.Menu.Map.OpenTrackFile.ToolTip"));
        putValue(Action.NAME, "load track file");
        this.myTracksStorage = aTracksStorage;
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final ActionEvent aE) {
        JFileChooser fileChooser = new JFileChooser(new File(Settings.getInstance().get("traveling-salesman.loadFile.lastPath", ".")));
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.addChoosableFileFilter(new FileFilter() {
            public boolean accept(final File file) {
                if (file.isDirectory())
                    return true;
                String fileName = file.getName().toLowerCase();
                return fileName.endsWith(".gpx")
                || fileName.endsWith(".nmea");
            }
            public String getDescription() {
                return "GPX/NMEA track files (.gpx/.nmea)";
            }
        });
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            final File[] selectedFiles = fileChooser.getSelectedFiles();
            for (int i = 0; i < selectedFiles.length; i++) {
                Settings.getInstance().put("traveling-salesman.loadFile.lastPath", selectedFiles[i].getParent());
                myTracksStorage.addTrackFile(selectedFiles[i]);
            }
        }
    }

}
