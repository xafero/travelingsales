//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.travelingsalesman.gui.widgets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.osm.data.coordinates.EastNorth;
import org.openstreetmap.osm.data.projection.Projection;
import org.openstreetmap.travelingsalesman.INavigatableComponent;

/**
 * A JSlider to change the current zoom-level of an {@link INavigatableComponent}.
 */
public class ZoomSlider extends JSlider implements PropertyChangeListener, ChangeListener {

    /**
     * For serializing ZoomSlider..
     */
    private static final long serialVersionUID = 1L;


    /**
     * Default-Width of this control.
     */
    private static final int DEFAULTWIDTH = 20;


    /**
     * The component we provide a zoom-slider for.
     */
    private final INavigatableComponent mv;

    /**
     * Does the user hold the mouse-button down?
     */
    private boolean clicked = false;

    /**
     * @param nv The component we provide a zoom-slider for.
     */
    public ZoomSlider(final INavigatableComponent nv) {
        super(0, DEFAULTWIDTH);
        setOpaque(false);
        this.mv = nv;
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(final MouseEvent e) {
                clicked = true;
            }
            @Override public void mouseReleased(final MouseEvent e) {
                clicked = false;
            }
        });
        setValue(this.mv.getZoom());
        mv.addPropertyChangeListener("scale", this);
        addChangeListener(this);
    }

    /**
     * ${@inheritDoc}.
     */
    public void propertyChange(final PropertyChangeEvent evt) {
        if (!getModel().getValueIsAdjusting()) {
            setValue(this.mv.getZoom());
        }
    }

    /**
     * ${@inheritDoc}.
     */
    public void stateChanged(final ChangeEvent e) {
        if (!clicked)
            return;

        // The border in px. to allow for the INavigatableComponent.
        final int border = 20;

        EastNorth pos = this.mv.getProjection().latlon2eastNorth(Projection.MAX_LAT, Projection.MAX_LON);

        for (int zoom = 0; zoom < getValue(); ++zoom) {
            pos = new EastNorth(pos.east() / 2, pos.north() / 2);
        }

        double zoomLevel;
        if (this.mv.getWidth() < this.mv.getHeight()) {
            zoomLevel =  Math.abs(pos.east() / (this.mv.getWidth() - border));
        } else {
            zoomLevel =  Math.abs(pos.north() / (this.mv.getHeight() - border));
        }
        this.mv.zoomTo(this.mv.getCenter(), zoomLevel);
    }
}
