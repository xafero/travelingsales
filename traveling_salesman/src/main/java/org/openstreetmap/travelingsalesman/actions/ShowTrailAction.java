/**
 * 
 */
package org.openstreetmap.travelingsalesman.actions;

import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.AbstractAction;
import org.openstreetmap.travelingsalesman.gps.IGPSProvider;
import org.openstreetmap.travelingsalesman.gps.data.GpsTrack;
import org.openstreetmap.travelingsalesman.gps.data.GpsTracksStorage;
import org.openstreetmap.travelingsalesman.gui.MainFrame;

/**
 * Action to show the current GPS-Track we are collecting
 * while driving in a TracksStorage.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class ShowTrailAction extends AbstractAction {

    /**
     * generated.
     */
    private static final long serialVersionUID = 948384333730575587L;

    /**
     * The current GPS-Track we are collecting
     * while driving.
     */
    private GpsTrack myTrail = null;

    /**
     * GpsTracksStorage where we store our track.
     */
    private GpsTracksStorage myTracksStorage;

    /**
     * Where we collect a track from.
     */
    private IGPSProvider myGPSProvider;

    /**
     * @param aTracksStorage GpsTracksStorage where we store our track.
     * @param aProvider where we collect a track from
     */
    public ShowTrailAction(final GpsTracksStorage aTracksStorage,
                           final IGPSProvider aProvider) {
        super(MainFrame.RESOURCE.getString("Main.Buttons.ShowTrail"));
        //putValue(Action.SHORT_DESCRIPTION, MainFrame.RESOURCE.getString("Actions.Buttons.ShowTrail"));
        this.myTracksStorage = aTracksStorage;
        this.myGPSProvider = aProvider;
    }


    /**
     * {@inheritDoc}
     */
    public void actionPerformed(final ActionEvent aE) {
        if (myTrail == null) {
            myTrail = new GpsTrack();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm");
            myTrail.setName(sdf.format(new Date()));
            myTracksStorage.add(myTrail);
            myGPSProvider.addGPSListener(myTrail);
        } else {
            myGPSProvider.removeGPSListener(myTrail);
            myTracksStorage.remove(myTrail);
            myTrail = null;
        }
    }

}
