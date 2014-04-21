//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.travelingsalesman.gui.widgets;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.painting.ColorHelper;

/**
 * Provide a component where the user can
 * see an indication of the zoom-level of an {@link INavigatableComponent}.
 *
 * @author imi
 */
public class MapScaler extends JComponent {

    /**
     * Our MapScaler.java.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The default-height of this component.
     */
    private static final int SCALERHEIGHT = 30;
    /**
     * The default-width of this component.
     */
    private static final int SCALERWIDTH = 100;

    /**
     * image for painted scaler caching.
     */
    private Image image;

    /**
     * scale of painted scaler.
     */
    private double currentDist = 0;

    /**
     * The component we are providing
     * map-scaling for.
     */
    private final INavigatableComponent mv;

    /**
     *
     *@param nv The component we are providing map-scaling for.
     */
    public MapScaler(final INavigatableComponent nv) {
        this.mv = nv;
        setSize(SCALERWIDTH, SCALERHEIGHT);
        setOpaque(false);
    }

    /**
     * repaint scaler only if scale changed
     * ${@inheritDoc}.
     */
    @Override
    public void paint(final Graphics g) {
        // 1. check scale is changed ?
        if (scaleIsChanged()) {
            // 2. if changed prepare new Image with text and drawIt
            // note new Image should be with transparent pixels
            prepareImage((Graphics2D) g);
        }
        // 3. else just draw prepared (cached) Image
        g.drawImage(image, 0, 0, this);
    }

    /**
     * Prepare {@link #image} from the {@link GraphicsConfiguration}
     * of the given Graphics2D.
     * @param g the Graphics2D to get the {@link GraphicsConfiguration} from.
     */
    private void prepareImage(final Graphics2D g) {
        this.image =
        g.getDeviceConfiguration().createCompatibleImage(SCALERWIDTH,
                                                        SCALERHEIGHT,
                                                        Transparency.BITMASK);
        this.scalerUpdate(this.image.getGraphics());
    }

    /**
     * @return true if the scale of {@link #mv} has changed.
     */
    private boolean scaleIsChanged() {
        LatLon ll1 = mv.getLatLon(0, 0);
        LatLon ll2 = mv.getLatLon(SCALERWIDTH, 0);
        double dist = LatLon.distanceInMeters(ll1, ll2);
        if (dist != this.currentDist) {
            this.currentDist = dist;
            return true;
        }
        return false;
    }

    /**
     * redraw scaler for new scale.
     * @param g the graphics to draw to.
     */
    private void scalerUpdate(final Graphics g) {
        final int kilo = 1000;
        final int deca = 100;
        final float centi = 10f;

        // create the text to show
        LatLon ll1 = mv.getLatLon(mv.getWidth() / 2, mv.getHeight() / 2);
        LatLon ll2 = mv.getLatLon(mv.getWidth() / 2 + SCALERWIDTH,
                                  mv.getHeight() / 2);
        final double dist = LatLon.distanceInMeters(ll1, ll2);

        String text = null;
        if (dist > kilo) {
            text = (Math.round(dist / deca) / centi) + " km";
        } else {
            text = Math.round(dist) + " m";
        }
        Rectangle2D bound = g.getFontMetrics().getStringBounds(text, g);

        // define some constants
        final int heightDivider = 3;
        final int markerBarHeight = getHeight() / heightDivider;
        final int textWidth = 50;
        final int textHeight = 23;
        final int markerBarOffset = 3;
        final int secondMarkerOffset = 24;
        final int markerBar1 = 1;
        final int markerBar2 = 3;

        // draw the outlined markers on the ends of a horizontal slider
        g.setColor(ColorHelper.html2color("#000000"));
        g.drawLine(0, markerBarHeight / 2 + 1, (SCALERWIDTH - 1), markerBarHeight / 2 + 1);
        g.drawLine(0, markerBarHeight / 2 - 1, (SCALERWIDTH - 1), markerBarHeight / 2 - 1);
        g.drawLine(0, 0, 0, markerBarHeight);
        g.drawLine(2, 0, 2, markerBarHeight);
        g.drawLine((SCALERWIDTH - markerBar1), 0, (SCALERWIDTH - markerBar1), markerBarHeight);
        g.drawLine((SCALERWIDTH - markerBar2), 0, (SCALERWIDTH - markerBar2), markerBarHeight);

        // draw the marker on the ends of a horizontal slider
        g.setColor(ColorHelper.html2color(
                Settings.getPreferences().get("color.scale", "#ffffff")));
        g.drawLine(1, markerBarHeight / 2, (SCALERWIDTH - 2), markerBarHeight / 2);
        g.drawLine(1, 0, 1, markerBarHeight);
        g.drawLine((SCALERWIDTH - 2), 0, (SCALERWIDTH - 2), markerBarHeight);

        // draw middle bars
        g.drawLine(textWidth - 1, 0, textWidth - 1, markerBarHeight);
        g.drawLine(textHeight + 1, markerBarOffset, textHeight + 1, markerBarHeight - markerBarOffset);
        g.drawLine(textWidth + secondMarkerOffset,
                   markerBarOffset,
                   textWidth + secondMarkerOffset,
                   markerBarHeight - markerBarOffset);

        //draw outlined text
        int x = (int) (textWidth - bound.getWidth() / 2);
        int y = textHeight;
        drawOutlinedText(g, text, x, y);
        g.setColor(ColorHelper.html2color(
                Settings.getPreferences().get("color.scale", "#ffffff")));
        g.drawString(text, x, y);
    }

    /**
     * Draw the given text to the given graphics at the given location.
     * @param g where to draw to
     * @param text what to draw
     * @param x where to draw it
     * @param y where to draw it
     */
    private void drawOutlinedText(final Graphics g, final String text,
                                  final int x, final int y) {
        g.setColor(ColorHelper.html2color("#000000"));
        g.drawString(text, x    , y - 1);
        g.drawString(text, x    , y + 1);
        g.drawString(text, x - 1, y);
        g.drawString(text, x - 1, y - 1);
        g.drawString(text, x - 1, y + 1);
        g.drawString(text, x + 1, y);
        g.drawString(text, x + 1, y - 1);
        g.drawString(text, x + 1, y + 1);
    }

}
