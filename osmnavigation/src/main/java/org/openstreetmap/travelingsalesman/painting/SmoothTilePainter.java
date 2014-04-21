/**
 * This file is part of OSMNavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  OSMNavigation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  OSMNavigation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OSMNavigation.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.painting;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOException;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.LODDataSet;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.EastNorth;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.projection.Projection;
import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.navigation.OsmNavigationConfigSection;

/**
 * A reimplementation of {@link TilePaintVisitor} that (down)loads
 * tiles asynchronously.
 * @author <a href="mailto:paulsoucy1@gmail.com">Paul Soucy</a>
 */
public class SmoothTilePainter extends SimplePaintVisitor {

    /**
     * Maximum zoom-level.
     */
    protected static final int MAXZOOM = 12;

    /**
     * If we cannot get a tile's width, we assume it's this wide.
     */
    protected static final int DEFAULTTILEWIDTH = 256;

    /**
     * If we cannot get a tile's height, we assume it's this high.
     */
    protected static final int DEFAULTTILEHEIGHT = 256;

    /**
     * 180° .
     */
    private static final double HALFCIRCLE = 180d;

    /**
     * my logger for debug and error-output.
     */
    protected static final Logger LOG = Logger.getLogger(SmoothTilePainter.class.getName());

    /**
     * The executor-service we use for parallel operation.
     * TODO: merge with the executor in
     * {@link org.openstreetmap.travelingsalesman.navigation.NavigationManager}.
     */
    private final ExecutorService myExecutorService;

    /**
     * The tiles we are downloading/rendering in parlalel.
     */
    //protected static final HashSet<downloadTileTask> downloadingTiles = new HashSet<downloadTileTask>();

    /**
     * The maximum size of {@link #downloadingTiles}.
     */
    //protected static final int MAX_DOWNLAOD_QUEUE_SIZE = 3;

    /**
     * Create a new TilePaintVisitor.
     */
    /*public TilePaintVisitor() {
    }*/

    /**
    * Create a new TilePaintVisitor and load all settings.
    */
   public SmoothTilePainter() {
      myExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
   }

   /**
    * Shutdown the {@link #myExecutorService}.
    * ${@inheritDoc}.
    */
   protected void finalize() {
       myExecutorService.shutdown();
   }

    /**
     * Visit all elements of the
     * {@link IDataSet} in the right order.
     * @param aData the {@link IDataSet} to visit
     * @param aGraphics where to draw to
     * @see org.openstreetmap.travelingsalesman.gui.widgets.SimplePaintVisitor#visitAll(org.openstreetmap.osm.data.MemoryDataSet, java.awt.Graphics)
     */
   @Override
    public void visitAll(final IDataSet aData, final Graphics2D aGraphics) {

       // take care about the correct level of details for the current
       // zoom-level
       IDataSet data = aData;
       final int lod1ZoomLevel = 7;
       final int lod2ZoomLevel = 5;
       final int lod3ZoomLevel = 5;
       if (getNavigatableComponent().getZoom() <= lod1ZoomLevel) {
           if (data instanceof LODDataSet) {
               if (getNavigatableComponent().getZoom() <= lod3ZoomLevel) {
                   data = ((LODDataSet) data).getLOD3DataSet();
               } else if (getNavigatableComponent().getZoom() <= lod2ZoomLevel) {
                   data = ((LODDataSet) data).getLOD2DataSet();
               } else {
                   data = ((LODDataSet) data).getLOD1DataSet();
               }
           } else {
               LOG.log(Level.WARNING, "SmoothTilePainter was ordered to draw a"
                       + " very low zoom-level of "
                       + getNavigatableComponent().getZoom() + " but the map"
                       + " is no LODDataSet! This may be very,very slow.");

           }
       }
       //--------------------------------------

        Graphics2D g2d = (Graphics2D) aGraphics;
        INavigatableComponent navigatable = super.getNavigatableComponent();
        int zoom = navigatable.getZoom() + 2;
        LatLon center = navigatable.getProjection().eastNorth2latlon(navigatable.getCenter());
        LOG.log(Level.FINEST, "TilePaintVisitor: zoom=" + zoom + " center=" + center);

        try {
            Tile currentTile = null;
            // check for screen boundaries
            LatLon lu = navigatable.getLatLon(0, 0);
            int firstX = 0;
            if (lu.lon() >= -HALFCIRCLE) {
                firstX = getXTileNumber(lu.lat(), lu.lon(), zoom);
            }
            int firstY = 0;
            if (lu.lat() <= MAXMERCATOR) {
                firstY = getYTileNumber(lu.lat(), lu.lon(), zoom);
            }
            LatLon rl = navigatable.getLatLon(navigatable.getWidth() - 1, navigatable.getHeight() - 1);
            int lastX = (1 << zoom) - 1;
            if (rl.lon() <= HALFCIRCLE) {
                lastX = getXTileNumber(rl.lat(), rl.lon(), zoom);
            }
            int lastY = (1 << zoom) - 1;
            if (rl.lat() >= -MAXMERCATOR) {
                lastY = getYTileNumber(rl.lat(), rl.lon(), zoom);
            }

            BufferedImage bmp = null;
            Point leftUpper = null;
            Point rightLower = null;
            // iterate over the width and height of the draw-area in tile-sizes
            for (int x = firstX; x <= lastX; x++) {
                for (int y = firstY; y <= lastY; y++) {
                    currentTile = getTile(x, y, zoom);
                    bmp = currentTile.getBitmap();
                    leftUpper  = navigatable.getPoint(navigatable.getProjection().latlon2eastNorth(currentTile.getLeftUpper()));
                    rightLower = navigatable.getPoint(navigatable.getProjection().latlon2eastNorth(currentTile.getRightLower()));
                    if (bmp == null) {
                        g2d.setColor(Color.RED);
                        int boxWidth = rightLower.x - leftUpper.x;
                        int boxHight = rightLower.y - leftUpper.y;
                        final int textheight = 40;
                        g2d.drawString("Loading Image...", leftUpper.x + boxWidth / 2 - textheight, leftUpper.y + boxHight / 2);
                        g2d.drawRect(leftUpper.x, leftUpper.y, boxWidth, boxHight);
                    } else {
                        // draw the current tile
                        g2d.drawImage(bmp, leftUpper.x, leftUpper.y, rightLower.x - leftUpper.x, rightLower.y - leftUpper.y ,  null);
                    }
                }
            }
//            Tile currentTile = getTile(navigatable.getLatLon(0, 0), zoom);
//            BufferedImage bmp = null;
//            Tile firstXTile = currentTile;
//            Point leftUpper  = navigatable.getPoint(navigatable.getProjection().latlon2eastNorth(currentTile.getLeftUpper()));
//            Point rightLower = navigatable.getPoint(navigatable.getProjection().latlon2eastNorth(currentTile.getRightLower()));
//            final int maxLoop = 200;
//            int detectInfiniteLoop = maxLoop;
//
//            
//            // iterate over the width and height of the draw-area in tile-sizes
//            while     (leftUpper.x < navigatable.getWidth())  {
//                if (detectInfiniteLoop == 0) {
//                    break;
//                }
//                while (leftUpper.y < navigatable.getHeight()) {
//                    detectInfiniteLoop--;
//                    if (detectInfiniteLoop == 0) {
//                        break;
//                    }
//                    //LOG.log(Level.FINE, "drawing tile rightLower=" + rightLower + " leftUpper=" + leftUpper
//                    //        + " window: width=" + navigatable.getWidth() + " height=" + navigatable.getHeight());
//
//                    bmp = currentTile.getBitmap();
//                    if (bmp == null) {
//                        // ignore tiles that could not be downloaded yet
//                        // they will render themself eventually and cause a repaint.
//                        //LOG.log(Level.WARNING, "no Bitmap yet for tile " + currentTile.myXTileNumber + "-" + currentTile.myYTileNumber + " at zoom=" + zoom);
//
//                        g2d.setColor(Color.RED);
//                        int boxWidth = rightLower.x - leftUpper.x;
//                        int boxHight = rightLower.y - leftUpper.y;
//                        final int textheight = 40;
//                        g2d.drawString("Loading Image...", leftUpper.x + boxWidth / 2 - textheight, leftUpper.y + boxHight / 2);
//                        g2d.drawRect(leftUpper.x, leftUpper.y, boxWidth, boxHight);
//
//                    } else {
//                        // draw the current tile
//                        g2d.drawImage(bmp, leftUpper.x, leftUpper.y, rightLower.x - leftUpper.x, rightLower.y - leftUpper.y ,  null);
//                    }
//                    currentTile = getTile(currentTile.getXtile(), currentTile.getYtile() + 1, zoom);
//                    leftUpper  = navigatable.getPoint(navigatable.getProjection().latlon2eastNorth(currentTile.getLeftUpper()));
//                    rightLower = navigatable.getPoint(navigatable.getProjection().latlon2eastNorth(currentTile.getRightLower()));
//                }
//                firstXTile = getTile(firstXTile.getXtile() + 1, firstXTile.getYtile(), zoom);
//                currentTile = firstXTile;
//                leftUpper  = navigatable.getPoint(navigatable.getProjection().latlon2eastNorth(currentTile.getLeftUpper()));
//                rightLower = navigatable.getPoint(navigatable.getProjection().latlon2eastNorth(currentTile.getRightLower()));
//            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while painting tiles!", e);
        }

        final int maxZoom = 18;
        if (zoom >= maxZoom) {
            super.visitAll(aData, aGraphics);
       }

    }

    /**
     * @param bmp the bitmap
     * @param defaultHeight the value to return if the bitmap is null
     * @return the height of the bitmap or the default-value.
     */
    protected int getBitmapHeight(final BufferedImage bmp, final int defaultHeight) {
        if (bmp == null)
            return defaultHeight;
        return bmp.getHeight();
    }

    /**
     * @param bmp the bitmap
     * @param defaultWidth the value to return if the bitmap is null
     * @return the width of the bitmap or the default-value.
     */
    protected int getBitmapWidth(final BufferedImage bmp, final int defaultWidth) {
        if (bmp == null)
            return defaultWidth;
        return bmp.getWidth();
    }

    /**
     * Load or create a new tile of the given location and zoom-level.
     * @param latLon the location of the upper-left corner
     * @param zoom the zoom-level
     * @throws IOException if javax.imageio has a problem
     * @return the tile, never null
     */
    protected Tile getTile(final LatLon latLon, final int zoom) throws IOException {
        return getTile(
                SmoothTilePainter.getXTileNumber(latLon.lat(), latLon.lon(), zoom),
                SmoothTilePainter.getYTileNumber(latLon.lat(), latLon.lon(), zoom),
                zoom);
    }
    /**
     * Load or create a new tile of the given location and zoom-level.
     * @param xTileNr the horizontal tile-number
     * @param yTileNr the horizontal tile-number
     * @param zoom the zoom-level
     * @throws IOException if javax.imageio has a problem
     * @return the tile, never null
     */
    protected Tile getTile(final int xTileNr, final int yTileNr, final int zoom) throws IOException {
        try {
            Tile retval = null;
            //-------------------
            // try lease recently used tiles first
            // (it is not memory-sensitive and thus will not
            // get garbage-collected)
            String lruKey = zoom + "x" + xTileNr + "x" + yTileNr;
            retval = myTileCacheLRU.get(lruKey);
            if (retval != null)
                return retval;

            //-------------------
            // lots of lazy-creation -code for the different levels

            while (myTileCache.size() <= zoom) {
                myTileCache.add(new HashMap<Integer,
                                    HashMap<Integer, Reference<Tile>>>());
            }
            HashMap<Integer, HashMap<Integer, Reference<Tile>>> xMap = myTileCache.get(zoom);
            if (xMap == null) {
                xMap = new HashMap<Integer, HashMap<Integer, Reference<Tile>>>();
                myTileCache.set(zoom, xMap);
            }
            HashMap<Integer, Reference<Tile>> yMap = xMap.get(xTileNr);
            if (yMap == null) {
                yMap = new HashMap<Integer, Reference<Tile>>();
                xMap.put(xTileNr, yMap);
            }

            //-------------------
            // here it gets interesting

            Reference<Tile> reference = yMap.get(yTileNr);
            if (reference != null)
                retval = reference.get();

            if (retval == null) {
                if (zoom >= 0 && xTileNr >= 0 && yTileNr >= 0 && xTileNr < (1 << zoom) && yTileNr < (1 << zoom)) {
                    retval = new Tile(xTileNr, yTileNr, zoom, true);
                    yMap.put(yTileNr, new java.lang.ref.SoftReference/* WeakReference*/<Tile>(retval));
                    if (myTileCacheLRU.size() >= TILECACHELRUSIZE)
                        myTileCacheLRU.remove(myTileCacheLRU.keySet().iterator().next());
                    myTileCacheLRU.put(lruKey, retval);
                }
            }

            if (retval != null)
                return retval;

//            return yMap.get(yTileNr).get();
            return null;
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[RuntimeException] Problem in getTile()",
                         e);
            return null; //new Tile(xTileNr, yTileNr, zoom);
        }
    }

    /**
     * Trigger a repaint on our {@link INavigatableComponent}.
     * @param t the tile that needs repainting
     */
    private void repaintTile(final Tile t) {
        INavigatableComponent nc2 = super.getNavigatableComponent();
        if (nc2 instanceof java.awt.Component)
            ((java.awt.Component) nc2).repaint();
    }

    /**
     * The cache for ${@link #getTile(int, int, int)}}.
     * It uses WeakReferences that are garbage-collected when memory
     * gets tight.
     */
    private ArrayList<HashMap<Integer, HashMap<Integer, Reference<Tile>>>>
       myTileCache
       = new ArrayList<HashMap<Integer,
                               HashMap<Integer,
                               Reference<Tile>>>>(MAXZOOM);

    /**
     * Maximum length of {@link #myTileCacheLRU}.
     */
    private static final int TILECACHELRUSIZE = 16;
    /**
     * Lease recently used tiles.
     */
    private Map<String, Tile> myTileCacheLRU = new HashMap<String, Tile>();

    /**
     * 180� in mercator.
     */
    private static final double MAXMERCATOR = 85.0511d;

/*Tile-Source 1 (uses our resolution)
    private BufferedImage lastBitmap = null;
    private double lastLat = 0;
    private double lastLon = 0;
    private int lastZoom = 0;
    private double bitmapLeftUpperLat = 0;
    private double bitmapLeftUpperLon = 0;


        **
         * @param zoom
         * @param leftUpper
         *
        private BufferedImage getBitmap(final int zoom, final LatLon leftUpper) {
            if (zoom < 4)
                return null;

            if (lastBitmap == null || zoom != lastZoom || lastLat != leftUpper.getXCoord() || lastLon != leftUpper.getYCoord())
            try {

                URL url = new URL("http://tah.openstreetmap.org/MapOf/?lat=" + leftUpper.lat()
                        + "&long=" + leftUpper.lon() + "&z=" + (zoom + 2)
                        + "&w=" + nc.getWidth() + "&h=" + nc.getHeight() + "&format=jpeg");
                LOG.log(Level.FINEST, "TilePaintVisitor: downloading " + url);
                BufferedImage bitmap = javax.imageio.ImageIO.read(url);
                this.lastBitmap = bitmap;
                this.lastLat = leftUpper.getXCoord();
                this.lastLon = leftUpper.getYCoord();
                this.lastZoom = zoom;
                return bitmap;
            } catch (MalformedURLException e) {
                //  Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                //  Auto-generated catch block
                e.printStackTrace();
            }
            return lastBitmap;
        }
*/


    /**
     * A single task for the {@link SmoothTilePainter#myExecutorService}.
     * @author <a href="mailto:paulsoucy1@gmail.com">Paul Soucy</a>
     */
    public class DownloadTileTask implements Callable<BufferedImage> {

        /**
         * The URL of the tile-image to download.
         */
        private URL url;

        /**
         * @param downloadUrl The URL of the tile-image to download.
         */
        public DownloadTileTask(final URL downloadUrl) {
            if (downloadUrl == null)
                throw new IllegalArgumentException("null url given");
            this.url = downloadUrl;
        }

        /**
         * ${@inheritDoc}.
         */
        //@Override
        public BufferedImage call() throws Exception {
            return javax.imageio.ImageIO.read(url);
        }

        /**
         * ${@inheritDoc}.
         */
        public boolean equals(final Object o) {
            if (o instanceof DownloadTileTask) {
                if (url.equals(((DownloadTileTask) o).url)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * ${@inheritDoc}.
         */
        public int hashCode() {
            return url.hashCode();
        }

    }

    /**
     * Calculate the tile-number for a given location and zoom-number
     * for OpenStreetMap's SlippyMap.
     * @param lat the location
     * @param lon the location
     * @param zoom the zoom-level
     * @return the tile-number in X-direction
     */
    public static int getXTileNumber(final double lat, final double lon, final int zoom) {
        int zoomFactor = (1 << zoom);
        int xtile = (int) Math.floor((lon + (int) HALFCIRCLE) / (2 * (int) HALFCIRCLE) * zoomFactor);
        return xtile;
    }

    /**
     * Calculate the tile-number for a given location and zoom-number
     * for OpenStreetMap's SlippyMap.
     * @param lat the location
     * @param lon the location
     * @param zoom the zoom-level
     * @return the tile-number in Y-direction
     */
    public static int getYTileNumber(final double lat, final double lon, final int zoom) {
        int zoomFactor = (1 << zoom);
        int ytile = (int) Math.floor(
                (1 - Math.log(Math.tan(lat * Math.PI / HALFCIRCLE)
                              + 1 / Math.cos(lat * Math.PI / HALFCIRCLE)
                             ) / Math.PI)
                / 2 * zoomFactor);
        return ytile;
    }

    /**
     * This class is a wrapper for a SlippyMap-Tile.
     * It contains the image and coordinates of a
     * pre-rendered map-part.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    protected class Tile {

        /**
         * This class is a Runnable to download a SlippyMap-Tile.
         * asynchronously.
         */
        private final class AsyncTimeLoader implements Runnable {

            /**
             * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
             * Project: osmnavigation<br/>
             * SmoothTilePainter.java<br/>
             * created: 09.12.2007 09:10:16 <br/>
             *<br/><br/>
             * This class implements an INavigatableComponent that coveres
             * a single tile.
             * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
             */
            private final class BufferdImageNavComponent implements INavigatableComponent {

                /**
                 * Initialize and calculate center and scale from the tile-bounds.
                 */
                public BufferdImageNavComponent() {
                    EastNorth min = getProjection().latlon2eastNorth(getBounds().getMin());
                    EastNorth max = getProjection().latlon2eastNorth(getBounds().getMax());
                    center = new EastNorth(min.east() + (max.east() - min.east()) / 2,
                            min.north() + (max.north() - min.north()) / 2);
                    scaleX = (max.east()  - min.east())  / getWidth();
                    scaleY = (max.north() - min.north()) / getHeight();
//                    scale = Math.max(scaleX, scaleY); // minimum scale to see all of the screen
                }
                /**
                 * @see #getCenter().
                 */
                private EastNorth center;

                /**
                 * @see #getScale().
                 */
                private double scaleX;
                /**
                 * @see #getScale().
                 */
                private double scaleY;

                /**
                 * ${@inheritDoc}.
                 */
                public void addPropertyChangeListener(final String aProperty, final PropertyChangeListener aListener) {
                    // ignored
                }

                /**
                 * ${@inheritDoc}.
                 */
                public EastNorth getCenter() {
                    return center;
                }

                /**
                 * ${@inheritDoc}.
                 */
                public LatLon getCurrentPosition() {
                    return getNavigatableComponent().getCurrentPosition();
                }

                /**
                 * ${@inheritDoc}.
                 */
                public IDataSet getDataSet() {
                    return getNavigatableComponent().getDataSet();
                }

                /**
                 * ${@inheritDoc}.
                 */
                public EastNorth getEastNorth(final int x, final int y) {
                    return new EastNorth(
                            center.east() + (x - getWidth() / 2.0) * scaleX,
                            center.north() - (y - getHeight() / 2.0) * scaleY);
                }

                /**
                 * ${@inheritDoc}.
                 */
                public int getHeight() {
                    return myBitmap.getHeight();
                }

                /**
                 * ${@inheritDoc}.
                 */
                public LatLon getLatLon(final int x, final int y) {
                    EastNorth eastNorth = new EastNorth(
                            center.east()  + (x - getWidth() / 2.0) * scaleX,
                            center.north() - (y - getHeight() / 2.0) * scaleY);
                    return getProjection().eastNorth2latlon(eastNorth);
                }

                /**
                 * ${@inheritDoc}.
                 */
                public LatLon getLeftUpper() {
                    // not required
                    return null;
                }

                /**
                 * ${@inheritDoc}.
                 */
                public Bounds getMapBounds() {
                    return Tile.this.getBounds();
                }

                /**
                 * ${@inheritDoc}.
                 */
                public LatLon getNextManeuverPosition() {
                    return getNavigatableComponent().getNextManeuverPosition();
                }

                /**
                 * ${@inheritDoc}.
                 */
                public Point getPoint(final EastNorth aPoint) {
                    double x = (aPoint.east()  - center.east()) / scaleX + getWidth() / 2.0d;
                    double y = (center.north() - aPoint.north()) / scaleY + getHeight() / 2.0d;
                    return new Point((int) x, (int) y);
                }

                /**
                 * ${@inheritDoc}.
                 */
                public Projection getProjection() {
                    return getNavigatableComponent().getProjection();
                }

                /**
                 * ${@inheritDoc}.
                 */
                public double getScale() {
                    return getNavigatableComponent().getScale();
                }

                /**
                 * ${@inheritDoc}.
                 */
                public int getWidth() {
                    return myBitmap.getWidth();
                }

                /**
                 * ${@inheritDoc}.
                 */
                public int getZoom() {
                    return Tile.this.getZoomLevel();
                }

                /**
                 * ${@inheritDoc}.
                 */
                public void setCurrentPosition(final LatLon aCurrentPosition) {
                    getNavigatableComponent().setCurrentPosition(aCurrentPosition);
                }

                /**
                 * ${@inheritDoc}.
                 */
                public void setNextManeuverPosition(final LatLon aNextManeuverPosition) {
                    getNavigatableComponent().setNextManeuverPosition(aNextManeuverPosition);
                }

                /**
                 * ${@inheritDoc}.
                 */
                public void zoomTo(final EastNorth aCenter, final double aZoomLevel) {
                    getNavigatableComponent().zoomTo(aCenter, aZoomLevel);
                }

                /**
                 * ${@inheritDoc}.
                 */
                public void setSelectedNodePosition(final LatLon selectedNodePosition) {
                    getNavigatableComponent().setSelectedNodePosition(selectedNodePosition);
                }
            }

            /**
             * Our tile-number. (X-part)
             */
            private final int myTile;

            /**
             * Our zoom-level.
             */
            private final int myZoom;

            /**
             * Our tile-number. (Y-part)
             */
            private final int myTile2;

            /**
             * @param aTile Our tile-number. (X-part)
             * @param aZoom Our zoom-level.
             * @param aTile2 Our tile-number. (Y-part)
             * @param allowIllegalCoords if true no exception will be
             * raised. However illegal tiles cannot be rendered without an
             * exception.
             */
            private AsyncTimeLoader(final int aTile, final int aZoom,
                                    final int aTile2,
                                    final boolean allowIllegalCoords) {
                myTile = aTile;
                myZoom = aZoom;
                myTile2 = aTile2;
                if (!allowIllegalCoords) {
                    if (myZoom < 1
                        || myTile2 < 0 || myTile < 0
                        || myTile2 >= 1 << myZoom || myTile >= 1 << myZoom)
                        throw new IllegalArgumentException("Tile out of range! "
                                + "zoom=" + myZoom
                                + " x=" + myTile
                                + " y=" + myTile2);
                }
            }

            /**
             * Download a tile if there is an error in loading a cached tile
             * (e.g. written while disk full/...).
             * ${@inheritDoc}.
             */
            public void run() {
//                if (myZoom <= 18 && myZoom >=1 ) {
                final File cacheFile = new File(getTileDirName() + myZoom + File.separator + myTile2 + File.separator + myTile + ".png");
                /////////////////////////////////////////////
                // try to load from cache first
                if (cacheFile.exists() && cacheFile.length() > 0)
                    try {
                        myBitmap = javax.imageio.ImageIO.read(cacheFile);
//                      repaint only if we managed to get a bitmap
                        if (myBitmap != null && myBitmap.getWidth() > 0) {
                            repaintMe();
                            return;
                        }
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "cannot load tile from cached file " + cacheFile.getAbsolutePath(), e);
                    }

                    /////////////////////////////////////////////
                    // we could not load a cached tile, try to download one
                    try {
                        URL url = new URL("http://tile.openstreetmap.org/" + myZoom + "/" + myTile2 + "/" + myTile + ".png");
                        if (myTile2 < 0 || myTile < 0
                            || myTile2 >= 1 << myZoom || myTile >= 1 << myZoom)
//                            throw new IOException("Tile out of range! zoom="
//                                    + myZoom + " x=" + myTile + " y="
//                                    + myTile2);
                            return;
                        try {
                            File dir = new File(getTileDirName());
                            if (!dir.exists()) {
                                dir.mkdirs();
                            }
                            javax.imageio.ImageIO.setCacheDirectory(dir);
                        } catch (Exception e) {
                            LOG.log(Level.WARNING, "cannot set ImageIO cache-directory to " + getTileDirName(), e);
                        }
                        try {
                            myBitmap = null;
                            URLConnection connection = url.openConnection();
                            connection.setRequestProperty("User-Agent", "osmnavigation SmoothTilePainter/" + OsmNavigationConfigSection.getVersion());
                            myBitmap = javax.imageio.ImageIO.read(connection.getInputStream());
                        } catch (UnknownHostException e) {
                            LOG.log(Level.INFO, "UnknownHostException, cannot download tile " + url.toExternalForm());
                        } catch (IIOException e) {
                            if (e.getCause() != null && e.getCause() instanceof UnknownHostException) {
                                LOG.log(Level.INFO, "UnknownHostException, cannot download tile " + url.toExternalForm());
                            } else {
                                LOG.log(Level.WARNING, "cannot download tile " + url.toExternalForm(), e);
                            }
                        }

                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "cannot download pre-rendered tile [" + e.getClass().getSimpleName() + "]", e);
                    }

                    /////////////////////////////////////////////
                    // call another PaintVisitor to render a tile
                    // if failed to download a pre-made tile.
                    if (myBitmap == null)
                    try {
                        myBitmap = new BufferedImage(DEFAULTTILEWIDTH, DEFAULTTILEHEIGHT, BufferedImage.TYPE_INT_ARGB);
                        //IPaintVisitor painter = new SimplePaintVisitor();
                        ODRPaintVisitor painter = new ODRPaintVisitor();
                        painter.setNoDataPainter(new SimplePaintVisitor());
                        painter.setFallbackPainter(new SimplePaintVisitor());
                        painter.setNavigatableComponent(new BufferdImageNavComponent());
                        painter.visitAll(getNavigatableComponent().getDataSet(), (Graphics2D) myBitmap.getGraphics());

                        //DEBUG: do not cache rendered tiles (map-data may change)
                        repaintMe();
                        return;
                    } catch (Exception e) {
                        myBitmap = null; // do not cache broken tiles
                        LOG.log(Level.WARNING, "cannot render tile", e);
                    }

                     /////////////////////////////////////////////
                    // try to save the tile to cache
                    if (myBitmap != null) {
                        try {
                            cacheFile.getParentFile().mkdirs();
                            javax.imageio.ImageIO.write(myBitmap, "png", cacheFile);
                        } catch (Exception e) {
                            LOG.log(Level.WARNING, "cannot save tile to cached file " + cacheFile.getAbsolutePath(), e);
                        }
                        // repaint only if we managed to get a bitmap
                        repaintMe();
                    }
//                }
            }
        }

        /**
         * the bitmap of the tile.
         */
        private BufferedImage myBitmap;

        /**
         * Our zoom-level.
         */
        private int myZoomLevel;

        /**
         * The x-number of this tile. (Changes with zoom-level)
         */
        private int myXTileNumber;

        /**
         * The x-number of this tile. (Changes with zoom-level)
         */
        private int myYTileNumber;

        /**
         * Our geographical location in lat+lon.
         * {Lat2, Long1, Lat1, Long1 + Unit}).
         *  S,W,N,E
         */
        private double[] projected;

        /**
         * Index into the array {@link #projected}.
         */
        private static final int PROJECTED_SOUTH = 0;

        /**
         * Index into the array {@link #projected}.
         */
        private static final int PROJECTED_WEST = 1;

        /**
         * Index into the array {@link #projected}.
         */
        private static final int PROJECTED_NORTH = 2;

        /**
         * Index into the array {@link #projected}.
         */
        private static final int PROJECTED_EAST = 3;

        /**
         * The returned value may be null if it cannot be downloaded!
         * @return the bitmap of the tile
         */
        public BufferedImage getBitmap() {
            return myBitmap;
        }

        /**
         * @return the difference between the west and the east latitude
         */
        public double getWidth() {
            if (projected == null)
                projected = projectTileNumber(getXtile(), getYtile(), getZoomLevel());
            double width = Math.abs(projected[PROJECTED_EAST] - projected[PROJECTED_WEST]);
            LOG.log(Level.FINEST, "Tile.getWidth() = " + width);
            return width;
        }

        /**
         * @return the difference between the north and the south longitude
         */
        public double getHeight() {
            if (projected == null)
                projected = projectTileNumber(getXtile(), getYtile(), getZoomLevel());
            double height = Math.abs(projected[PROJECTED_SOUTH] - projected[PROJECTED_NORTH]);
            return height;
        }

        /**
         * @return the geographical coordinates of the left-upper corner of this tile.
         */
        public LatLon getLeftUpper() {
            if (projected == null)
                projected = projectTileNumber(getXtile(), getYtile(), getZoomLevel());
           return new LatLon(projected[PROJECTED_NORTH], projected[PROJECTED_WEST]);
        }

        /**
         * @return the geographical coordinates of the right-lower corner of this tile.
         */
        public LatLon getRightLower() {
            if (projected == null)
                projected = projectTileNumber(getXtile(), getYtile(), getZoomLevel());
            return new LatLon(projected[PROJECTED_SOUTH], projected[PROJECTED_EAST]);
        }

        /**
         * @return The geographical coordinates of this tile in lat+lon.
         */
        public Bounds getBounds() {
            if (projected == null)
                projected = projectTileNumber(getXtile(), getYtile(), getZoomLevel());
            return new Bounds(projected[PROJECTED_SOUTH], projected[PROJECTED_WEST],
                    projected[PROJECTED_NORTH], projected[PROJECTED_EAST]);
        }

        /**
         * @return the directory where we store all tiles on disk.
         */
        private String getTileDirName() {
            return Settings.getInstance().get("renderedTileCache.dir",
                      Settings.getInstance().get("map.dir",
                                                 System.getProperty("user.home") + File.separator + ".openstreetmap")
                    + File.separator + "tiles" + File.separator);
        }

        /**
         * Create a new tile of the given location and zoom-level.
         * @param latLon the location
         * @param zoom the zoom-level
         * @throws IOException if javax.imageio has a problem
         */
        public Tile(final LatLon latLon, final int zoom) throws IOException {
            this(latLon.lat(), latLon.lon(), zoom, false);
        }

        /**
         * Create a new tile of the given location and zoom-level.
         * @param lat the location
         * @param lon the location
         * @param zoom the zoom-level
         * @param allowIllegalCoords if true no exception will be
         * raised. However illegal tiles cannot be rendered without an
         * exception.
         * @throws IOException if javax.imageio has a problem
         */
        public Tile(final double lat, final double lon, final int zoom,
                final boolean allowIllegalCoords) throws IOException {
            this(getXTileNumber(lat, lon, zoom),
                 getYTileNumber(lat, lon, zoom), zoom, allowIllegalCoords);
        }

        /**
         * Create a new tile of the given location and zoom-level.
         * @param xTile the horizontal tile-number
         * @param yTile the horizontal tile-number
         * @param zoom the zoom-level
         * @param allowIllegalCoords if true no exception will be
         * raised. However illegal tiles cannot be rendered without an
         * exception.
         * @throws IOException if javax.imageio has a problem
         */
        public Tile(final int xTile, final int yTile, final int zoom,
                final boolean allowIllegalCoords) throws IOException {

            // load this tile from disk or server asynchronously
            SmoothTilePainter.this.myExecutorService.execute(
                    new AsyncTimeLoader(yTile, zoom, xTile, allowIllegalCoords));
            this.myXTileNumber = xTile;
            this.myYTileNumber = yTile;
            this.myZoomLevel = zoom;
            projected = projectTileNumber(myXTileNumber, myYTileNumber, zoom);

            LOG.log(Level.FINE, "new Tile(xtile=" + myXTileNumber + ", ytile=" + myYTileNumber + " zoom=" + zoom
                    + ") => x=" + myXTileNumber + " y=" +  myYTileNumber + " projected=" + Arrays.toString(projected));
        }

         /**
          * return the lat+lon of all 4 corners of the given tile.
          * @param x the horizontal tile-number
          * @param y the horizontal tile-number
          * @param zoom the zoom-level
          * @return the lat+lon of the 4 corners
          */
         protected double[] projectTileNumber(final int x, final int y, final int zoom) {
             double unit = 1.0d / (1 << zoom);
             double relY1 = unit * y;
             double relY2 = relY1 + unit;
             double limitY = projectF(MAXMERCATOR);
             double rangeY = 2.0d * limitY;
             relY1 = limitY - rangeY * relY1;
             relY2 = limitY - rangeY * relY2;
             double lat1 = projectMercToLat(relY1);
             double lat2 = projectMercToLat(relY2);
             unit = (2.0 * HALFCIRCLE) / (1 << zoom);
             double long1 = -1 * HALFCIRCLE + x * unit;
             return (new double[] {lat2, long1, lat1, long1 + unit}); // S,W,N,E
         }

         /**
          * used by ${@link #projectTileNumber(int, int, int)}.
          * @param mercY the mercator-coordinate
          * @return the latitude
          */
         private double projectMercToLat(final double mercY) {
             return HALFCIRCLE / Math.PI * Math.atan(Math.sinh(mercY));
         }

         /**
          * used by ${@link #projectTileNumber(int, int, int)}.
          * @param lat the latitude
          * @return the mercator-coordinate for it.
          */
         private double projectF(final double lat) {
             double lat2 = deg2rad(lat);
             double y = Math.log(Math.tan(lat2) + (1 / Math.cos(lat2)));
             return y;
         }

         /**
          * used by ${@link #projectTileNumber(int, int, int)}.
          * @param deg degreed
          * @return radiants
          */
         private double deg2rad(final double deg) {
             return (deg * Math.PI / HALFCIRCLE);
         }

         /**
          * @see SmoothTilePainter#repaintTile(org.openstreetmap.travelingsalesman.painting.SmoothTilePainter.Tile)
          */
         private void repaintMe() {
             SmoothTilePainter.this.repaintTile(this);
         }

        /**
         * @return the xtile
         */
        public int getXtile() {
            return myXTileNumber;
        }

        /**
         * @return the ytile
         */
        public int getYtile() {
            return myYTileNumber;
        }

        /**
         * @return Our zoom-level.
         */
        public int getZoomLevel() {
            return this.myZoomLevel;
        }

    }


}
