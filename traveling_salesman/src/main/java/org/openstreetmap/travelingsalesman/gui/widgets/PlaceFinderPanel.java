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
package org.openstreetmap.travelingsalesman.gui.widgets;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.searching.IAddressDBPlaceFinder;
import org.openstreetmap.osm.data.searching.IExtendedPlaceFinder;
import org.openstreetmap.osm.data.searching.IPlaceFinder;
import org.openstreetmap.osm.data.searching.InetPlaceFinder;
import org.openstreetmap.osm.data.searching.Place;
import org.openstreetmap.osm.data.searching.SimplePlaceFinder;
import org.openstreetmap.osm.data.searching.advancedAddressDB.AdvancedAddressDBPlaceFinder;
import org.openstreetmap.travelingsalesman.gui.MainFrame;

import com.l2fprod.common.util.ResourceManager;

/**
 * This panel allowes to search for a given Place
 * and let the user select from the results.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class PlaceFinderPanel extends JPanel {

    /**
     * Out resource-manager for i18n.
     */
    protected static final ResourceManager RESOURCE = ResourceManager.get(MainFrame.class);

    /**
     * For serializing a PlaceFinderPanel.
     */
    private static final long serialVersionUID = 1L;

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(PlaceFinderPanel.class.getName());

    /**
     * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
     * Project: traveling_salesman<br/>
     * PlaceFinderPanel.java<br/>
     * created: 18.11.2007 12:13:53 <br/>
     *<br/><br/>
     * This actionListener takes the input from myCityZipInputTextField
     * and myStreetHouseOnputTextField and executes a search.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private final class SearchFieldInputListener implements ActionListener {

        /**
         * Thead for asynchronous searching.
         */
        private Thread mySearchThread = null;

        /**
         * ${@inheritDoc}.
         */
        public void actionPerformed(final ActionEvent arg0) {
            if (mySearchThread != null && mySearchThread.isAlive())
                mySearchThread.interrupt();

            LOG.info("asynchronous search for a place started...");

            mySearchThread = new Thread("PlaceFinder") {
                public void run() {
                    search(myCityZipInputTextField.getText().replace(
                            myCityZipInputDefaultText, ""),
                            myStreetInputTextField.getText().replace(
                                    myStreetInputDefaultText, ""),
                            myHouseInputTextField.getText().replace(
                                    myHouseInputDefaultText, ""));
                    LOG.info("asynchronous search for a place ended...");
                }
            };
            mySearchThread.start();
        }
    }

    /**
     * Interface that our listeners implement.
     */
    public interface PlaceFoundListener {

        /**
         * A place has been found by the {@link PlaceFinderPanel}.
         * @param aPlace the place found
         */
        void placeFound(final Place aPlace);

        /**
         * The user wants to preview a place.
         * @param aPlace the place found
         */
        void placePreview(final Place aPlace); // currently unused
    }

    /**
     * Our {@link PlaceFoundListener}s.
     */
    private Collection<PlaceFoundListener> myPlaceFoundListeners = new HashSet<PlaceFoundListener>();


    /**
     * Default-text to display in {@link #myCityZipInputTextField}.
     * It will be ignored in searches.
     */
    private String myCityZipInputDefaultText = RESOURCE.getString("Main.Search.defaultText.City");

    /**
     * Default-text to display in {@link #myStreetInputTextField}.
     * It will be ignored in searches.
     */
    private String myStreetInputDefaultText = RESOURCE.getString("Main.Search.defaultText.Street");
    /**
     * Default-text to display in {@link #myHouseInputTextField}.
     * It will be ignored in searches.
     */
    private String myHouseInputDefaultText = RESOURCE.getString("Main.Search.defaultText.House");

    /**
     * Where the user enters the query
     * for city-name + zip-coe.
     */
    private JTextField myCityZipInputTextField = new JTextField(myCityZipInputDefaultText);

    /**
     * Where the user enters the query
     * for Steet.
     */
    private JTextField myStreetInputTextField = new JTextField(myStreetInputDefaultText);
    /**
     * Where the user enters the query
     * for house-number.
     */
    private JTextField myHouseInputTextField = new JTextField(myHouseInputDefaultText);

    /**
     * The list of results.
     */
    private JList resultsList = new JList();

    /**
     * The map that we can search on
     * locally.
     */
    private IDataSet myMap;

    /**
     * a status-bar to give feedback while we are searching.
     */
    private JLabel myStatusBar;

    /**
     * Create a new {@link PlaceFinderPanel}
     * and initialize the GUI.
     * @param aMap The map that we can search on locally.
     * @param aListener the listener for the found places
     * @param aStatusBar a status-bar to give feedback while we are searching
     */
    public PlaceFinderPanel(final IDataSet aMap, final PlaceFoundListener aListener, final JLabel aStatusBar) {
        this.setLayout(new BorderLayout());
        this.myStatusBar = aStatusBar;

        JPanel inputs = new JPanel();
        inputs.setLayout(new GridLayout(1, 2));
        inputs.add(myCityZipInputTextField);

        JPanel temp = new JPanel();
        temp.setLayout(new BorderLayout());
        temp.add(myStreetInputTextField, BorderLayout.CENTER);

        final int minHouseNrWidth = 40;
        final int minHouseNrHeight = 10;
        myHouseInputTextField.setMinimumSize(new Dimension(minHouseNrWidth, minHouseNrHeight));
        temp.add(myHouseInputTextField, BorderLayout.EAST);

        inputs.add(temp);
        this.add(inputs, BorderLayout.NORTH);
        this.add(new JScrollPane(resultsList), BorderLayout.CENTER);

        this.myMap = aMap;
        this.myPlaceFoundListeners.add(aListener);
        this.myCityZipInputTextField.addActionListener(new SearchFieldInputListener());
        this.myCityZipInputTextField.addFocusListener(new FocusListener() {

            /**
             * If the  field contains the default-text,
             * select it uppon getting focus. So the user
             * can start typing right away..
             */
            @Override
            public void focusGained(final FocusEvent aE) {
                if (myCityZipInputTextField.getText().equals(myCityZipInputDefaultText)) {
                    myCityZipInputTextField.setSelectionStart(0);
                    myCityZipInputTextField.setSelectionEnd(myCityZipInputDefaultText.length());
                }
            }

            @Override
            public void focusLost(final FocusEvent aE) {
                // ignored
            }
        });
        this.myStreetInputTextField.addActionListener(new SearchFieldInputListener());
        this.myStreetInputTextField.addFocusListener(new FocusListener() {

            /**
             * If the  field contains the default-text,
             * select it uppon getting focus. So the user
             * can start typing right away..
             */
            @Override
            public void focusGained(final FocusEvent aE) {
                if (myStreetInputTextField.getText().equals(myStreetInputDefaultText)) {
                    myStreetInputTextField.setSelectionStart(0);
                    myStreetInputTextField.setSelectionEnd(myStreetInputDefaultText.length());
                }
            }

            @Override
            public void focusLost(final FocusEvent aE) {
                // ignored
            }
        });
        this.myHouseInputTextField.addActionListener(new SearchFieldInputListener());
        this.myHouseInputTextField.addFocusListener(new FocusListener() {

            /**
             * If the  field contains the default-text,
             * select it uppon getting focus. So the user
             * can start typing right away..
             */
            @Override
            public void focusGained(final FocusEvent aE) {
                if (myHouseInputTextField.getText().equals(myHouseInputDefaultText)) {
                    myHouseInputTextField.setSelectionStart(0);
                    myHouseInputTextField.setSelectionEnd(myHouseInputDefaultText.length());
                }
            }

            @Override
            public void focusLost(final FocusEvent aE) {
                // ignored
            }
        });
        this.resultsList.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(final ListSelectionEvent arg0) {
                Object selectedValue = resultsList.getSelectedValue();
                if (!(selectedValue instanceof Place)) {
                    return;
                    //may happen when the user clicks the
                    // "searching..." -entry in the list
                }
                Place place = (Place) selectedValue;
                if (place != null)
                    for (PlaceFoundListener listener : myPlaceFoundListeners) {
                        listener.placeFound(place);
                    }
            }
            }
        );
        myCityZipInputTextField.setEnabled(myMap != null);
        myStreetInputTextField.setEnabled(myMap != null);
        myHouseInputTextField.setEnabled(myMap != null);
        this.setBorder(new BevelBorder(1));
    }

    /**
     * Search for a place by its name.
     * @param keyWords the name to look for
     */
    public void search(final String keyWords) {
        search(keyWords, "", "");
    }
    /**
     * Search for a place by its name.
     * The arguments are not to be null. Use the empty string instead.
     * @param aZipAndCiy the city-name or zip-code + " " + city to look for
     * @param aStreetName the street-name to look for
     * @param aHouseNumber optional (may be null) house-number to look for
     */
    public void search(final String aZipAndCiy, final String aStreetName, final String aHouseNumber) {
        // give feedback to the user that something is happening
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        DefaultListModel busyListModel = new DefaultListModel();
        busyListModel.addElement(MainFrame.RESOURCE.getString("placeFinderPanel.status.amSearching"));
        this.resultsList.setModel(busyListModel);
        if (myStatusBar != null) {
            myStatusBar.setText(MainFrame.RESOURCE.getString("placeFinderPanel.status.amSearching"));
        }

        try {
            IPlaceFinder finder = Settings.getInstance().getPlugin(IPlaceFinder.class, InetPlaceFinder.class.getName());
            //IPlaceFinder finder = Settings.getInstance().getPlugin(IPlaceFinder.class, InetPlaceFinder.class.getName());
            finder.setMap(this.getMap());

            LOG.log(Level.FINE, "PlaceFinderPanel.search() - using '" + finder.getClass().getName() + "'");

            Iterator<Place> places = null;
            if (finder instanceof IExtendedPlaceFinder) {
                IExtendedPlaceFinder extendedFinder = (IExtendedPlaceFinder) finder;
                String houseNr = aHouseNumber;
                String street = aStreetName;
                String city = aZipAndCiy;
                String zipCode = null;
                String country = null;
                //TODO: extract zipCode
                places = extendedFinder.findAddress(houseNr, street, city, zipCode, country).iterator();
            } else {
                String keyWord = aZipAndCiy;
                if (aStreetName != null) {
                    keyWord += " " + aStreetName;
                    if (aHouseNumber != null) {
                        keyWord += " " + aHouseNumber;
                    }
                }
                places = finder.findPlaces(keyWord.trim()).iterator();
            }

//          fallback to the AdvancedAddressDBPlaceFinder first
            if ((!places.hasNext()) && !(finder instanceof IAddressDBPlaceFinder)) {
                LOG.log(Level.FINE, "PlaceFinderPanel.search() - '" + finder.getClass().getName() + "' found nothing "
                        + "using [" + finder.getClass().getName() + "], using AddressDBPlaceFinder");
                finder = new AdvancedAddressDBPlaceFinder();
                finder.setMap(this.getMap());
                places = ((AdvancedAddressDBPlaceFinder) finder).findAddress(aHouseNumber, aStreetName, aZipAndCiy, null, null).iterator();
            }

            //then fallback to the most simple of algorithms
            if (!places.hasNext() && !(finder instanceof SimplePlaceFinder)) {
                LOG.log(Level.FINE, "PlaceFinderPanel.search() - '" + finder.getClass().getName() + "' found nothing, using SimplePlaceFinder");
                finder = new SimplePlaceFinder();
                finder.setMap(this.getMap());
                String keyWord = aZipAndCiy;
                if (aStreetName != null) {
                    keyWord += " " + aStreetName;
                    if (aHouseNumber != null) {
                        keyWord += " " + aHouseNumber;
                    }
                }
                places = finder.findPlaces(keyWord.trim()).iterator();
            }
//TODO: we should ask for the cities first. Fill them in a tree.
// when expanded ask for suburbs. if yes=> show suburbs + a suburb "all"
// if not and if a suburb is expanded, ask for streets
// if a street is expanded, ask for house-numbers.
            // this is a trivial search and not very usefull
            DefaultListModel model = new DefaultListModel();
            while (places.hasNext()) {
                Place w = places.next();
                model.addElement(w);
            }
            this.resultsList.setModel(model);
        } finally {
            // end the feedback to the user that something is happening
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            if (myStatusBar != null) {
                myStatusBar.setText(MainFrame.RESOURCE.getString("placeFinderPanel.status.searchingDone"));
            }
            if (this.resultsList.getModel() == busyListModel) {
                this.resultsList.setModel(new DefaultListModel());
            }
        }
    }

    /**
     * @return the map we are using
     */
    public IDataSet getMap() {
        return myMap;
    }

    /**
     * @param aMap the map we are using
     */
    public void setMap(final IDataSet aMap) {
        myMap = aMap;
        myCityZipInputTextField.setEnabled(myMap != null);
        myStreetInputTextField.setEnabled(myMap != null);
        myHouseInputTextField.setEnabled(myMap != null);
    }

    /**
     * Inform our listeners about a new place found.
     * @param aPlace the place
     */
    public void firePlaceFound(final Place aPlace) {
        Collection<PlaceFoundListener> listeners = this.myPlaceFoundListeners;
        for (PlaceFoundListener placeFoundListener : listeners) {
            placeFoundListener.placeFound(aPlace);
        }
    }

}
