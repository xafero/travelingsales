package org.openstreetmap.travelingsalesman.gui.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.openstreetmap.travelingsalesman.actions.LoadTrackFileAction;
import org.openstreetmap.travelingsalesman.gps.data.GpsTrack;
import org.openstreetmap.travelingsalesman.gps.data.GpsTracksStorage;
import org.openstreetmap.travelingsalesman.gps.data.ITracksChangeListener;
import org.openstreetmap.travelingsalesman.gps.data.TrackEvent;
import org.openstreetmap.travelingsalesman.gui.MainFrame;

import com.l2fprod.common.util.ResourceManager;

/**
 * Panel showing loaded tracks with buttons: add, remove (edit), etc..
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
 */
public class TracksPanel extends JPanel implements ITracksChangeListener, ListDataListener, ListCellRenderer {

    /**
     * Show this many rows of tracks in the UI.
     */
    private static final int VISIBLETRACKCOUNT = 5;

    /**
     * Default serial UID.
     */
    private static final long serialVersionUID = 1L;

    /**
    * The display list.
    */
    private JList displaylist = null;

//    /**
//     * Playback speed label text.
//     */
//    private JLabel playbackSpeedText = new JLabel("Playback speed: ");
//
//    /**
//     * Label to show the current multiplier in playback.
//     */
//    private JLabel playbackMultiplierText = new JLabel("");

    /**
     * Storage contains gps tracks.
     */
    private GpsTracksStorage myStorage = null;

//    /**
//     * Current multiplier (for playback-speed) index from mvals.
//     */
//    private int multiplyer = 2;

//    /**
//     * Multiplier possible multiplier values  (for playback-speed).
//     */
//    private static final String[] MVALS = {"0.1", "0.5", "1", "5", "10", "50", "100", "200", "500", "1000", "5000"};
//
//    /**
//     * my logger for debug and error-output.
//     */
//    private static final Logger LOG = Logger.getLogger(TracksPanel.class.getName());

    /**
     * Simple constructor.
     */
    public TracksPanel() {
//        this.setLayout(new GridLayout(0, 4));
//        this.add(playbackSpeedText);
//        this.add(playbackMultiplierText);
//        playbackMultiplierText.setText(multiplierAsString());
        setLayout(new BorderLayout());
        displaylist = new JList(getTracksStorage());
        displaylist.setCellRenderer(this);
        displaylist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(displaylist);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
        displaylist.setVisibleRowCount(VISIBLETRACKCOUNT);
        JPanel buttonPanel = new JPanel(new GridLayout(0, 1));
        buttonPanel.setBackground(UIManager.getColor("TaskPaneGroup.background"));

        // Load track button
        JButton openButton = new JButton(new LoadTrackFileAction(getTracksStorage()));
        buttonPanel.add(openButton);

        // Remove track button
        JButton removeButton = new JButton(ResourceManager.get(MainFrame.class).getString("Main.Buttons.Remove"));
        removeButton .addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent arg0) {
                    myStorage.remove((GpsTrack) displaylist.getSelectedValue());
            }
        });
        buttonPanel.add(removeButton);

        // spacer
        buttonPanel.add(new JPanel());

        displaylist.setFixedCellWidth(this.getPreferredSize().width - openButton.getPreferredSize().width);
        add(buttonPanel, BorderLayout.EAST);


        getTracksStorage().addListDataListener(this);
    }

//    /**
//     * @return current speed-multiplier  (for playback-speed)
//     */
//    private String multiplierAsString() {
//        return MVALS[multiplyer] + "x";
//    }


    /**
     * Lazy initializer.
     * @return my GPS tracks storage.
     */
    public GpsTracksStorage getTracksStorage() {
        if (myStorage == null) {
            myStorage = new GpsTracksStorage();
        }
        return myStorage;
    }

    /**
     * {@inheritDoc}
     */
    public void updateTrack(final TrackEvent action, final GpsTrack track) {
//        if (action == TrackEvent.ADD) {
//            //list.addElement(track);
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void contentsChanged(final ListDataEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void intervalAdded(final ListDataEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void intervalRemoved(final ListDataEvent e) {
    }

    /**
     * {@inheritDoc}
     */
    public Component getListCellRendererComponent(final JList list, final Object value,
            final int index, final boolean isSelected, final boolean cellHasFocus) {
        DefaultListCellRenderer dlcr = new DefaultListCellRenderer();
        Component label = dlcr.getListCellRendererComponent(list, ((GpsTrack) value).getName(), index, isSelected, cellHasFocus);
        label.setForeground((Color) getTracksStorage().getProperty((GpsTrack) value, "color"));
        return label;
    }
}
