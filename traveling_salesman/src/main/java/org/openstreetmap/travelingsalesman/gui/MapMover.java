/**
 * License: GPL. Copyright 2007 by Immanuel Scholz and others
 */
package org.openstreetmap.travelingsalesman.gui;


import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.NodeHelper;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.EastNorth;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedNode;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.routing.selectors.UsedTags;


/**
 * Enables moving of the map by holding down the right mouse button and drag
 * the mouse. Also, enables zooming by the mouse wheel.
 *
 * @author imi,
 * @author <a href="mailto:combbs@users.sourceforge.net">Oleg Chubaryov</a>
 */
public class MapMover extends MouseAdapter implements MouseMotionListener, MouseWheelListener {

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(MapMover.class.getName());
    /**
     * Divide the value of {@link MouseWheelEvent#getWheelRotation()} by this
     * value to get the zoom-level-change.
     */
    private static final double WEELTOZOOM = 1.2;
    /**
     * We allow no zoom-level below this one.
     */
    private static final double MINZOOM = 0.1;
    /**
     * We allow scale below this one.
     */
    public static final double MAXSCALE = 1E-3;
    /**
     * We allow scale above this one.
     */
    public static final double MINSCALE = 1E-8;

    /**
     * This is an action to add to a panel for using the mouse-weel to zoom.
     */
    private final class ZoomerAction extends AbstractAction {

        /**
         * For serializing a MapMover.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Either "." or "," to zoom in or out.
         */
        private final String myAction;

        /**
         * @param actionName Either "." or "," to zoom in or out.
         */
        public ZoomerAction(final String actionName) {
            this.myAction = actionName;
        }

        /**
         * ${@inheritDoc}.
         */
        public void actionPerformed(final ActionEvent e) {
            if (myAction.equals(".") || myAction.equals(",")) {
                Point mouse = ((JComponent) nc).getMousePosition();
                if (mouse == null) {
                    JComponent component = (JComponent) nc;
                    mouse = new Point((int) component.getBounds().getCenterX(),
                                      (int) component.getBounds().getCenterY());
                }
                int weelRotation = 1;
                if (myAction.equals(","))
                    weelRotation = -1;
                MouseWheelEvent we = new MouseWheelEvent((JComponent) nc,
                                                         e.getID(),
                                                         e.getWhen(),
                                                         e.getModifiers(),
                                                         mouse.x,
                                                         mouse.y,
                                                         0,
                                                         false,
                                                         MouseWheelEvent.WHEEL_UNIT_SCROLL,
                                                         1,
                                                         weelRotation);
                mouseWheelMoved(we);
            } else {
                EastNorth center = nc.getCenter();
                final int unknownConstant = 5;
                EastNorth newcenter = nc.getEastNorth(
                        nc.getWidth() / 2 + nc.getWidth() / unknownConstant,
                        nc.getHeight() / 2 + nc.getHeight() / unknownConstant);
                if (myAction.equals("left"))
                    nc.zoomTo(new EastNorth(2 * center.east() - newcenter.east(), center.north()), nc.getScale());
                else if (myAction.equals("right"))
                    nc.zoomTo(new EastNorth(newcenter.east(), center.north()), nc.getScale());
                else if (myAction.equals("up"))
                    nc.zoomTo(new EastNorth(center.east(), 2 * center.north() - newcenter.north()), nc.getScale());
                else if (myAction.equals("down"))
                    nc.zoomTo(new EastNorth(center.east(), newcenter.north()), nc.getScale());
            }
        }
    }

    /**
     * The point in the map that was the under the mouse point
     * when moving around started.
     */
    private EastNorth mousePosMove;
    /**
     * The map to move around.
     */
    private final INavigatableComponent nc;
    /**
     * The old cursor when we changed it to movement cursor.
     */
    private Cursor oldCursor;

    /**
     * We are currently moving.
     */
    private boolean movementInPlace = false;

    /**
     * Create a new MapMover.
     * @param navComp the component we are working on
     * @param contentPane add the {@link ZoomerAction} to this panel.
     */
    public MapMover(final INavigatableComponent navComp, final JPanel contentPane) {
        this.nc = navComp;
        ((JComponent) nc).addMouseListener(this);
        ((JComponent) nc).addMouseMotionListener(this);
        ((JComponent) nc).addMouseWheelListener(this);

        String[] n = {",", ".", "up", "right", "down", "left"};
        int[] k = {KeyEvent.VK_COMMA, KeyEvent.VK_PERIOD, KeyEvent.VK_UP, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT};
        int[] k2 = {KeyEvent.VK_PAGE_UP, KeyEvent.VK_PAGE_DOWN, KeyEvent.VK_UP, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT};

        if (contentPane != null) {
            for (int i = 0; i < n.length; ++i) {
                contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                      .put(KeyStroke.getKeyStroke(k[i], KeyEvent.CTRL_DOWN_MASK),
                                                  "MapMover.Zoomer." + n[i]);
                contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                        KeyStroke.getKeyStroke(k2[i], 0),
                        "MapMover.Zoomer." + n[i]);
                contentPane.getActionMap().put("MapMover.Zoomer." + n[i],
                                               new ZoomerAction(n[i]));
            }
        }
    }

    /**
     * If the left or right mouse button is pressed, move the map.
     * @param e the event.
     */
    public void mouseDragged(final MouseEvent e) {
        int offMask = MouseEvent.BUTTON2_DOWN_MASK;
        int onMask1 = MouseEvent.BUTTON1_DOWN_MASK;
        int onMask2 = MouseEvent.BUTTON3_DOWN_MASK;
        if (((e.getModifiersEx() & (onMask1 | offMask)) == onMask1) || ((e.getModifiersEx() & (onMask2 | offMask)) == onMask2)) {
            if (mousePosMove == null)
                startMovement(e);
            EastNorth center = nc.getCenter();
            EastNorth mouseCenter = nc.getEastNorth(e.getX(), e.getY());
            EastNorth p = new EastNorth(
                    mousePosMove.east() + center.east() - mouseCenter.east(),
                    mousePosMove.north() + center.north() - mouseCenter.north());
            nc.zoomTo(p, nc.getScale());
        } else
            endMovement();
    }

    /**
     * Show popup menu.
     * @param e the event.
     */
    @Override
    public void mouseClicked(final MouseEvent e) {
        // ignored
    }

    /**
     * Start the movement, if it was the 1st or 3rd button (left or right button).
     * @param e the event.
     */
    @Override public void mousePressed(final MouseEvent e) {
        int offMask = MouseEvent.BUTTON2_DOWN_MASK;
        if ((e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3) && (e.getModifiersEx() & offMask) == 0)
            startMovement(e);
    }

    /**
     * Change the cursor back to it's pre-move cursor.
     * @param e the event.
     */
    @Override public void mouseReleased(final MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3)
            endMovement();
    }

    /**
     * Start movement by setting a new cursor and remember the current mouse
     * position.
     * @param e The mouse event that leat to the movement from.
     */
    private void startMovement(final MouseEvent e) {
        if (movementInPlace)
            return;

        movementInPlace = true;
        mousePosMove = nc.getEastNorth(e.getX(), e.getY());
        oldCursor = ((JComponent) nc).getCursor();
        ((JComponent) nc).setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    /**
     * End the movement. Setting back the cursor and clear the movement variables
     */
    private void endMovement() {
        if (!movementInPlace)
            return;
        movementInPlace = false;
        if (oldCursor != null)
            ((JComponent) nc).setCursor(oldCursor);
        else
            ((JComponent) nc).setCursor(Cursor.getDefaultCursor());
        mousePosMove = null;
        oldCursor = null;
    }

    /**
     * Zoom the map by 1/WEELZOOM of current zoom per wheel-delta.
     * @param e The wheel event.
     */
    public void mouseWheelMoved(final MouseWheelEvent e) {
        int w = nc.getWidth();
        int h = nc.getHeight();

        double zoom = MINZOOM;
        if (e.getWheelRotation() < 0) {
            zoom = 1 / WEELTOZOOM;
        } else if (e.getWheelRotation() > 0) {
            zoom = WEELTOZOOM;
        }
        double zoomfactor = (zoom - 1) / 2 + 1;
        if (((nc.getScale() * Settings.getProjection().scaleFactor() < MAXSCALE) || (zoomfactor < 1))
         && ((nc.getScale() * Settings.getProjection().scaleFactor() > MINSCALE) || (zoomfactor > 1))) {
            double newHalfWidth  = w * zoomfactor - w / 2;
            double newHalfHeight = h * zoomfactor - h / 2;
            double centerx = e.getX() - (e.getX() - w / 2) * newHalfWidth  * 2 / w;
            double centery = e.getY() - (e.getY() - h / 2) * newHalfHeight * 2 / h;
            EastNorth newCenter = nc.getEastNorth((int) centerx, (int) centery);
            nc.zoomTo(newCenter, nc.getScale() * zoom);
        }
    }

    private Thread myAsyncShowTooltipThread;
    /**
     * {@inheritDoc}
     */
    public void mouseMoved(final MouseEvent e) {
        if (myAsyncShowTooltipThread != null && myAsyncShowTooltipThread.isAlive()) {
            return;
        }
        final LatLon point = nc.getLatLon(e.getX(), e.getY());
        myAsyncShowTooltipThread = new Thread("MapMover.ShowToolTip async") {
            public void run() {
                try {
                    MapMover.this.showToolTipForNearestNode(point);
                } catch (Exception e) {
                    LOG.log(Level.SEVERE,"[Exception] Problem in "
                               + getClass().getName(),
                                 e);
                }
            }
        };
        myAsyncShowTooltipThread.setDaemon(true);
        myAsyncShowTooltipThread.setPriority(Thread.MIN_PRIORITY);
        myAsyncShowTooltipThread.start();
    }

    /**
     * Show tooltip with objects name and address.
     * @param point for search nearest node
     */
    private void showToolTipForNearestNode(final LatLon point) {
        final Node node = (ExtendedNode) nc.getDataSet().getNearestNode(point, new UsedTags());
        if (node != null) {
            nc.setSelectedNodePosition(new LatLon(node.getLatitude(), node.getLongitude()));
            ((JComponent) nc).repaint();
            if (!Settings.getInstance().getBoolean("showAllTags", false)) {
                StringBuffer text = new StringBuffer("<html>");
                String name = NodeHelper.getTag(node, Tags.TAG_NAME);
                if (name != null) {
                    text.append(name);
                }
                String fulladdress = NodeHelper.getTag(node, Tags.TAG_ADDR_FULL);
                if (fulladdress != null) {
                    text.append("<br>" + fulladdress);
                } else {
                    String street = NodeHelper.getTag(node, Tags.TAG_ADDR_STREET);
                    if (street != null) {
                        text.append("<br>" + street);
                    }
                    String housenumber = NodeHelper.getTag(node, UsedTags.TAG_ADDR_HOUSENUMBER);
                    if (housenumber != null) {
                        text.append(", " + housenumber);
                    }
                    String housename = NodeHelper.getTag(node, UsedTags.TAG_ADDR_HOUSENAME);
                    if (housename != null) {
                        text.append(", " + housename);
                    }
                }
                final Iterator<Way> ways = nc.getDataSet().getWaysForNode(node.getId());
                while (ways.hasNext()) {
                    Way way = ways.next();
                    name = WayHelper.getTag(way, Tags.TAG_NAME);
                    final int minLengthForComma = 6;
                    if (name != null) {
                        if (text.length() > minLengthForComma) {
                            text.append(", ");
                        }
                        text.append(name);
                    }
                    fulladdress = WayHelper.getTag(way, Tags.TAG_ADDR_FULL);
                    if (fulladdress != null) {
                        text.append("<br>" + fulladdress);
                    } else {
                        String street = WayHelper.getTag(way, Tags.TAG_ADDR_STREET);
                        if (street != null) {
                            if (text.length() > minLengthForComma)
                                text.append("<br>");
                            text.append(street);
                        }
                        String housenumber = WayHelper.getTag(way, UsedTags.TAG_ADDR_HOUSENUMBER);
                        if (housenumber != null) {
                            text.append(", " + housenumber);
                        }
                        String housename = WayHelper.getTag(way, UsedTags.TAG_ADDR_HOUSENAME);
                        if (housename != null) {
                            text.append(", " + housename);
                        }
                    }
                }
                text.append("</html>");
                final int minLength = 13;
                if (text.length() > minLength) {
                    String str = text.toString();
                    ((JComponent) nc).setToolTipText(str);
                } else {
                    ((JComponent) nc).setToolTipText(null);
                }
            } else {
                Collection<Tag> tags = node.getTags();
                StringBuffer text = new StringBuffer();
                text.append("<html><i>node id : " + node.getId() + "</i><br><hr>");
                for (Tag tag : tags) {
                    text.append(tag.getKey() + " = " + tag.getValue() + "<br>");
                }
                final Iterator<Way> ways = nc.getDataSet().getWaysForNode(node.getId());
                if (ways != null) {
                    while (ways.hasNext()) {
                        text.append("<hr>");
                        Way way = ways.next();
                        text.append("<i>way id : " + way.getId() + "</i><hr>");
                        for (Tag tag : way.getTags()) {
                            text.append(tag.getKey() + " = " + tag.getValue() + "<br>");
                        }
                    }
                }
                text.append("</html>");
                if (text.length() > 0) {
                    String str = text.toString();
                    ((JComponent) nc).setToolTipText(str);
                } else {
                    ((JComponent) nc).setToolTipText(null);
                }
            }
        }
    }
}
