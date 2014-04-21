package org.openstreetmap.travelingsalesman.gui.widgets;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JComponent;


/**
 * Abstract class that draws "on map" widgets, placed over map.
 * These widgets are always visible even fullscreen mode.
 *
 * @author Oleg Chubaryov
 */
public class MapOverBox extends JComponent {

    /**
     * Default serial id.
     */
    private static final long serialVersionUID = 4784325486683211393L;

    /**
     * Size of underlied black box.
     */
    private Dimension dimension = null;

    /**
     * Construtor with defined sizes.
     * @param aDimension the dimension
     */
    public MapOverBox(final Dimension aDimension) {
        this.dimension = aDimension;
        setPreferredSize(this.dimension);
        setOpaque(true);
    }

    /**
     * Paint black underlied rectamgle.
     * @param g the gpachics context
     */
    public void paintComponent(final Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        Color oldColor = g2d.getColor();
        Composite oldComposite = g2d.getComposite();
        try {
            oldColor = g2d.getColor();
            oldComposite = g2d.getComposite();
            g2d.setColor(Color.BLACK);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            g2d.fillRoundRect(0, 0, this.dimension.width, this.dimension.height, 10, 10);
        } finally {
            g2d.setComposite(oldComposite);
            g2d.setColor(oldColor);
        }
    }
}
