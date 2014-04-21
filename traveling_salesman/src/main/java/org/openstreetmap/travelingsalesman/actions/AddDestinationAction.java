/**
 * 
 */
package org.openstreetmap.travelingsalesman.actions;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
//import java.util.logging.Logger;

import javax.swing.Action;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.searching.NodePlace;
import org.openstreetmap.osm.data.searching.Place;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.gui.MainFrame;
import org.openstreetmap.travelingsalesman.gui.widgets.PlaceFinderPanel;
import org.openstreetmap.travelingsalesman.routing.IVehicle;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: Traveling Salesman<br/>
 * AddDestinationAction<br/>
 * created: 19.03.2009 <br/>
 *<br/><br/>
 * <b>Open the system-web-browser to file a bug-report.</b>
 * @author  <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class AddDestinationAction extends MouseAdapter implements Action {

    /**
     * Our logger for debug- and error-output.
     */
//    private static final Logger LOG = Logger.getLogger(AddDestinationAction.class.getName());

    /**
     * The INavigatableComponent to translate screen-coordinated to a geolocation.
     */
    private INavigatableComponent myMapPanel;
    /**
     * The map we operate on.
     */
    private IDataSet myMap;
    /**
     * What nodes under the cursor are valid destinations.
     */
    private IVehicle mySelector;
    /**
     * Where to report our destination to.
     */
    private PlaceFinderPanel myPlaceFinderPanel;

    /**
     * The new destination found.
     */
    private Place myNewDestination = null;
    /**
     * @param aMapPanel The INavigatableComponent to translate screen-coordinated to a geolocation.
     * @param aSelector What nodes under the cursor are valid destinations.
     * @param aPlaceFinderPanel Where to report our destination to.
     */
    public AddDestinationAction(final INavigatableComponent aMapPanel,
                                final IVehicle aSelector,
                                final PlaceFinderPanel aPlaceFinderPanel) {
        // TODO Auto-generated constructor stub
        this.myMapPanel = aMapPanel;
        this.myMap = aMapPanel.getDataSet();
        this.mySelector = aSelector;
        this.myPlaceFinderPanel = aPlaceFinderPanel;
        this.putValue(Action.NAME, MainFrame.RESOURCE.getString("Main.ContextMenu.AddDestination"));
    }



    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(final ActionEvent aE) {
        if (myNewDestination != null) {
            myPlaceFinderPanel.firePlaceFound(myNewDestination);
        }
    }


    ////////////////////////////
    /**
     * Used for {@link PropertyChangeListener}s.
     */
    private PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

    /**
     * Used for {@link #putValue(String, Object)}.
     */
    private Map<String, Object> myValues = new HashMap<String, Object>();

    /**
     * ${@inheritDoc}.
     */
    public void addPropertyChangeListener(final PropertyChangeListener aListener) {
        this.myPropertyChangeSupport.addPropertyChangeListener(aListener);
    }


    /**
     * ${@inheritDoc}.
     */
    public Object getValue(final String aKey) {
        return myValues.get(aKey);
    }


    /**
     * ${@inheritDoc}.
     */
    public boolean isEnabled() {
        return true;
    }


    /**
     * ${@inheritDoc}.
     */
    public void putValue(final String aKey, final Object aValue) {
        Object old = myValues.put(aKey, aValue);
        myPropertyChangeSupport.firePropertyChange(aKey, old, aValue);
    }


    /**
     * ${@inheritDoc}.
     */
    public void removePropertyChangeListener(final PropertyChangeListener aListener) {
        this.myPropertyChangeSupport.removePropertyChangeListener(aListener);
    }


     /**
      * ${@inheritDoc}.
      */
     public void setEnabled(final boolean aB) {
        // ignored
    }



    /**
     * {@inheritDoc}
     * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseReleased(final MouseEvent aE) {

        LatLon latLon = myMapPanel.getLatLon(aE.getX(), aE.getY());
        Node nearestNode = myMap.getNearestNode(latLon, mySelector);
        if (nearestNode != null) {
            this.myNewDestination = new NodePlace(nearestNode, myMap);
        } else {
            this.myNewDestination = null;
        }
    }




}
