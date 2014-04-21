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

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.IIOException;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.navigation.OsmNavigationConfigSection;

/**
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class TilePaintVisitor extends SimplePaintVisitor {

    /**
     * Our TilePaintVisitor.java.
     */
    private static final int MAXZOOM = 12;

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(TilePaintVisitor.class.getName());

    /**
     * Create a new TilePaintVisitor.
     */
    /*public TilePaintVisitor() {
    }*/

    /**
    * Create a new TilePaintVisitor and load all settings.
    */
   public TilePaintVisitor() {
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

        Graphics2D g2d = (Graphics2D) aGraphics;
        INavigatableComponent navigatable = super.getNavigatableComponent();
        int zoom = Math.min(navigatable.getZoom() + 2, INavigatableComponent.MAXZOOM);
        LatLon center = navigatable.getProjection().eastNorth2latlon(navigatable.getCenter());
        LOG.log(Level.FINEST, "TilePaintVisitor: zoom=" + zoom + " center=" + center);

        //Tile-Source 1 (uses our resolution)
/*        BufferedImage bmp = getBitmap(zoom, center);
        if (bmp != null)
            g2d.drawImage(bmp, 0, 0, null);*/

        //Tile-Source 2 (uses tiled images). Faster but more difficult to use

        // if not all tiles are there, default to the
        // SimplePaintVisitor as a fallback
        boolean notAllTilesPresent = false;

        try {
            Tile currentTile = null;
            // check for screen boundaries
            LatLon lu = navigatable.getLatLon(0, 0);
            int firstX = 0;
            if (lu.lon() >= -Tile.HALFCIRCLE) {
                firstX = Tile.getXTileNumber(lu.lat(), lu.lon(), zoom);
            }
            int firstY = 0;
            if (lu.lat() <= Tile.MAXMERCATOR) {
                firstY = Tile.getYTileNumber(lu.lat(), lu.lon(), zoom);
            }
            LatLon rl = navigatable.getLatLon(navigatable.getWidth() - 1, navigatable.getHeight() - 1);
            int lastX = (1 << zoom) - 1;
            if (rl.lon() <= Tile.HALFCIRCLE) {
                lastX = Tile.getXTileNumber(rl.lat(), rl.lon(), zoom);
            }
            int lastY = (1 << zoom) - 1;
            if (rl.lat() >= -Tile.MAXMERCATOR) {
                lastY = Tile.getYTileNumber(rl.lat(), rl.lon(), zoom);
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
                        // ignore tiles that could not be downloaded
                        LOG.log(Level.WARNING, "no Bitmap for tile " + currentTile.myXTileNumber + "-" + currentTile.myYTileNumber + " at zoom=" + zoom);
                        notAllTilesPresent = true;
                    } else {
                        // draw the current tile
                        g2d.drawImage(bmp, leftUpper.x, leftUpper.y, rightLower.x - leftUpper.x, rightLower.y - leftUpper.y ,  null);
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while painting tiles!", e);
            notAllTilesPresent = true;
        }

       if (notAllTilesPresent) {
            super.visitAll(aData, aGraphics);
       }
    }

//    /**
//     * @param bmp the bitmap
//     * @param defaultHeight the value to return if the bitmap is null
//     * @return the height of the bitmap or the default-value.
//     */
//    private int getBitmapHeight(final BufferedImage bmp, final int defaultHeight) {
//        if (bmp == null)
//            return defaultHeight;
//        return bmp.getHeight();
//    }
//
//    /**
//     * @param bmp the bitmap
//     * @param defaultWidth the value to return if the bitmap is null
//     * @return the width of the bitmap or the default-value.
//     */
//    private int getBitmapWidth(final BufferedImage bmp, final int defaultWidth) {
//        if (bmp == null)
//            return defaultWidth;
//        return bmp.getWidth();
//    }

    /**
     * Load or create a new tile of the given location and zoom-level.
     * @param latLon the location of the upper-left corner
     * @param zoom the zoom-level
     * @throws IOException if javax.imageio has a problem
     * @return the tile, never null
     */
    protected Tile getTile(final LatLon latLon, final int zoom) throws IOException {
        return getTile(
                Tile.getXTileNumber(latLon.lat(), latLon.lon(), zoom),
                Tile.getYTileNumber(latLon.lat(), latLon.lon(), zoom),
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

            Tile retval = null;
            Reference<Tile> reference = yMap.get(yTileNr);
            if (reference != null)
                retval = reference.get();

            if (retval == null) {
                retval = new Tile(xTileNr, yTileNr, zoom);
                yMap.put(yTileNr, new java.lang.ref.SoftReference/* WeakReference*/<Tile>(retval));
            }

            if (retval != null)
                return retval;

            return yMap.get(yTileNr).get();

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[RuntimeException] Problem in getTile()",
                         e);
            return null; //new Tile(xTileNr, yTileNr, zoom);
        }
    }

    /**
     * The cache for {@link #getTile(int, int, int)}}.
     * It uses WeakReferences that are garbage-collected when memory
     * gets tight.
     */
    private ArrayList<HashMap<Integer, HashMap<Integer, Reference<Tile>>>>
       myTileCache
       = new ArrayList<HashMap<Integer,
                               HashMap<Integer,
                               Reference<Tile>>>>(MAXZOOM);

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
     * This class is a wrapper for a SlippyMap-Tile.
     * It contains the image and coordinates of a
     * pre-rendered map-part.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private static class Tile {

        /**
         * 180° in mercator.
         */
        private static final double MAXMERCATOR = 85.0511d;

        /**
         * 180° .
         */
        private static final double HALFCIRCLE = 180d;

        /**
         * my logger for debug and error-output.
         */
        private static final Logger LOG = Logger.getLogger(Tile.class.getName());

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
            double width = Math.abs(projected[PROJECTED_EAST] - projected[PROJECTED_WEST]);
            LOG.log(Level.FINEST, "Tile.getWidth() = " + width);
            return width;
        }

        /**
         * @return the difference between the north and the south longitude
         */
        public double getHeight() {
            double height = Math.abs(projected[PROJECTED_SOUTH] - projected[PROJECTED_NORTH]);
            return height;
        }

        /**
         * @return the geographical coordinates of the left-upper corner of this tile.
         */
        public LatLon getLeftUpper() {
           return new LatLon(projected[PROJECTED_NORTH], projected[PROJECTED_WEST]);
        }

        /**
         * @return the geographical coordinates of the right-lower corner of this tile.
         */
        public LatLon getRightLower() {
            return new LatLon(projected[PROJECTED_SOUTH], projected[PROJECTED_EAST]);
        }

        /**
         * @return The geographical coordinates of this tile in lat+lon.
         */
        public Bounds getBounds() {
            return new Bounds(projected[PROJECTED_SOUTH], projected[PROJECTED_WEST],
                    projected[PROJECTED_NORTH], projected[PROJECTED_EAST]);
        }

        /**
         * @return the directory where we store all tiles on disk.
         */
        private String getTileDirName() {
            return Settings.getInstance().get("renderedTileCache.dir", System.getProperty("user.home")
                    + File.separator + ".openstreetmap" + File.separator + "tiles" + File.separator);
        }

        /**
         * Create a new tile of the given location and zoom-level.
         * @param latLon the location
         * @param zoom the zoom-level
         * @throws IOException if javax.imageio has a problem
         */
        public Tile(final LatLon latLon, final int zoom) throws IOException {
            this(latLon.lat(), latLon.lon(), zoom);
        }

        /**
         * Create a new tile of the given location and zoom-level.
         * @param lat the location
         * @param lon the location
         * @param zoom the zoom-level
         * @throws IOException if javax.imageio has a problem
         */
        public Tile(final double lat, final double lon, final int zoom) throws IOException {
            this(getXTileNumber(lat, lon, zoom), getYTileNumber(lat, lon, zoom), zoom);
        }

        /**
         * Create a new tile of the given location and zoom-level.
         * @param xTile the horizontal tile-number
         * @param yTile the horizontal tile-number
         * @param zoom the zoom-level
         * @throws IOException if javax.imageio has a problem
         */
        public Tile(final int xTile, final int yTile, final int zoom) throws IOException {
            File cacheFile = new File(getTileDirName() + zoom + File.separator + xTile + File.separator + yTile + ".png");

            // try to load from cache first
            if (cacheFile.exists() && cacheFile.length() > 0) {
                try {
                    this.myBitmap = javax.imageio.ImageIO.read(cacheFile);
                } catch (IIOException e) {
                    LOG.log(Level.WARNING, "cannot load tile from cached file " + cacheFile.getCanonicalPath(), e);
                }
            }

            // download tile from server
            if (this.myBitmap == null || this.myBitmap.getWidth() < 1 || this.myBitmap.getHeight() < 1) {
                if (yTile < 0 || xTile < 0
                        || yTile >= 1 << zoom || xTile >= 1 << zoom)
                        throw new IOException("Tile out of range! zoom="
                                + zoom + " x=" + xTile + " y="
                                + yTile);
                URL url = new URL("http://tile.openstreetmap.org/" + zoom + "/" + xTile + "/" + yTile + ".png");
                try {
                    URLConnection connection = url.openConnection();
                    connection.setRequestProperty("User-Agent", "osmnavigation TilePaintVisitor/" + OsmNavigationConfigSection.getVersion());
                    myBitmap = javax.imageio.ImageIO.read(connection.getInputStream());
                } catch (IIOException e) {
                    if (e.getCause() != null && e.getCause() instanceof UnknownHostException) {
                        LOG.log(Level.WARNING, "UnknownHostException, cannot download tile " + url.toExternalForm());
                    } else {
                        LOG.log(Level.WARNING, "cannot download tile " + url.toExternalForm(), e);
                    }
                }
                LOG.log(Level.FINE, "new Tile downloaded");
                //(xtile=" + myXTileNumber + ", ytile=" + myYTileNumber + " zoom=" + zoom
                //        + ") => x=" + myXTileNumber + " y=" +  myYTileNumber + " projected=" + Arrays.toString(projected));

                // try to save to cache
                if (this.myBitmap != null)
                try {
                    cacheFile.getParentFile().mkdirs();
                    javax.imageio.ImageIO.write(this.myBitmap, "png", cacheFile);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "cannot save tile to cached file " + cacheFile.getCanonicalPath(), e);
                }
            }

            this.myXTileNumber = xTile;
            this.myYTileNumber = yTile;
            this.myZoomLevel = zoom;
            projected = projectTileNumber(myXTileNumber, myYTileNumber, zoom);
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
          * used by {@link #projectTileNumber(int, int, int)}.
          * @param mercY the mercator-coordinate
          * @return the latitude
          */
         private double projectMercToLat(final double mercY) {
             return HALFCIRCLE / Math.PI * Math.atan(Math.sinh(mercY));
         }

         /**
          * used by {@link #projectTileNumber(int, int, int)}.
          * @param lat the latitude
          * @return the mercator-coordinate for it.
          */
         private double projectF(final double lat) {
             double lat2 = deg2rad(lat);
             double y = Math.log(Math.tan(lat2) + (1 / Math.cos(lat2)));
             return y;
         }

         /**
          * used by {@link #projectTileNumber(int, int, int)}.
          * @param deg degreed
          * @return radiants
          */
         private double deg2rad(final double deg) {
             return (deg * Math.PI / HALFCIRCLE);
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
