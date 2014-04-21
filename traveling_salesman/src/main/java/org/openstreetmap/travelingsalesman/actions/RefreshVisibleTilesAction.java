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

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.gui.MainFrame;

/**
 * This action for remove visible tiles from disk cache. If we have an internet
 * connection deleted tiles will download again.
 *
 * TODO: clear from memory cache and reload at realtime.
 * TODO: add parameter - zoom depth (how many levels of zoom will be cleared.)
 *
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
 */
public class RefreshVisibleTilesAction extends AbstractAction {

    /**
     * How many zoom levels will be cleared additionally to current level.
     */
    private final int clearDepth = 0;

    /**
     * Our OpenInJosmAction.java.
     */
    private static final long serialVersionUID = 1L;

//    /**
//     * my logger for debug and error-output.
//     */
//    private static final Logger LOG = Logger.getLogger(RefreshVisibleTilesAction.class.getName());

    /**
     * We need this to get the map and the visible area.
     */
    private INavigatableComponent myNavigatableComponent;

    /**
     * @param aNavigatableComponent
     *            We need this to get the map and the visible area.
     */
    public RefreshVisibleTilesAction(final INavigatableComponent aNavigatableComponent) {
        super(MainFrame.RESOURCE.getString("Actions.RefreshTiles.Label"));
        this.myNavigatableComponent = aNavigatableComponent;
        putValue(Action.SHORT_DESCRIPTION, MainFrame.RESOURCE.getString("Actions.RefreshTiles.Description"));
        /*
         * action.putValue(Action.SMALL_ICON, new ImageIcon(TaskPaneMain.class
         * .getResource(iconPath)));
         */
    }

//    /**
//     * 180° in mercator.
//     */
//    private static final double MAXMERCATOR = 85.0511d;

    /**
     * 180° .
     */
    private static final double HALFCIRCLE = 180d;

    /**
     * Calculate the tile-number for a given location and zoom-number for
     * OpenStreetMap's SlippyMap.
     *
     * @param lat
     *            the location
     * @param lon
     *            the location
     * @param zoom
     *            the zoom-level
     * @return the tile-number in X-direction
     */
    private static int getXTileNumber(final double lat, final double lon, final int zoom) {
        int zoomFactor = (1 << zoom);
        int xtile = (int) Math
                .floor((lon + (int) HALFCIRCLE) / (2 * (int) HALFCIRCLE) * zoomFactor);
        return xtile;
    }

    /**
     * Calculate the tile-number for a given location and zoom-number for
     * OpenStreetMap's SlippyMap.
     *
     * @param lat
     *            the location
     * @param lon
     *            the location
     * @param zoom
     *            the zoom-level
     * @return the tile-number in Y-direction
     */
    private static int getYTileNumber(final double lat, final double lon, final int zoom) {
        int zoomFactor = (1 << zoom);
        int ytile = (int) Math.floor((1 - Math.log(Math.tan(lat * Math.PI / HALFCIRCLE) + 1
                / Math.cos(lat * Math.PI / HALFCIRCLE))
                / Math.PI)
                / 2 * zoomFactor);
        return ytile;
    }

    /**
     * @return the directory where we store all tiles on disk.
     */
    private String getTileDirName() {
        return Settings.getInstance().get(
                "renderedTileCache.dir",
                System.getProperty("user.home") + File.separator + ".openstreetmap"
                        + File.separator + "tiles" + File.separator);
    }

    /**
     * ${@inheritDoc}.
     *
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(final ActionEvent arg0) {
        Bounds visibleArea = myNavigatableComponent.getMapBounds();
        int visibleZoom = myNavigatableComponent.getZoom();
//        System.out.println("visible bounds = " + visibleArea.toString() + ", zoom = " + visibleZoom);
        LatLon max = visibleArea.getMax();
        LatLon min = visibleArea.getMin();
        //int zoom = visibleZoom + 2;
        for (int zoom = visibleZoom + 2; zoom <= visibleZoom + 2 + clearDepth; zoom++) {
            int xMaxTile = getXTileNumber(max.lat(), max.lon(), zoom);
            int xMinTile = getXTileNumber(min.lat(), min.lon(), zoom);
            int yMaxTile = getYTileNumber(min.lat(), min.lon(), zoom);
            int yMinTile = getYTileNumber(max.lat(), max.lon(), zoom);
            for (int xTile = xMinTile; xTile <= xMaxTile; xTile++) {
                for (int yTile = yMinTile; yTile <= yMaxTile; yTile++) {
                    File cacheFile = new File(getTileDirName() + zoom + File.separator + xTile + File.separator + yTile + ".png");
//                  System.out.println(cacheFile.getAbsolutePath());
                    cacheFile.delete();
                }
            }
        }
        // clear LRU cache for optimized painters.
//        if (myNavigatableComponent instanceof SmoothTilePainter) {
//            //SmoothTilePainter painter = (SmoothTilePainter) myNavigatableComponent.
//            //painter.clearTileLRUCache();
//        }

        //if (myNavigatableComponent instanceof SmoothTilePainter) {
//            SmoothTilePainter painter = (SmoothTilePainter) myNavigatableComponent;
//            painter.clearTileLRUCache();
       // }
    }
}
