/**
 * This file is part of LibOSM by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  LibOSM is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LibOSM is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LibOSM.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.osm.data;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;

/**
 * Improved Osmosis-Tile-Calculator that can also do the inverse
 * calculation.
 */
public class TileCalculator extends org.openstreetmap.osmosis.core.util.TileCalculator {

    /**
     * The number of bits of a tile-number
     * in the osmosis-format.
     */
    private static final int TILEBITCOUNT = 15;

    /**
     * The maximum longitude in degrees.
     * (360°)
     */
    private static final double MAX_LONGITUDE = 360d;

    /**
     * The maximum latitude in degrees.
     * (180°)
     */
    private static final double MAX_LATITUDE = 180d;

    /**
     * 180°.
     */
    private static final double HALF_CIRCLE = 180d;

    /**
     * 90°.
     */
    private static final double QUARTER_CIRCLE = 90d;

    /**
     * The tiles split the planet into this many tiles
     * in each direction.
     */
    private static final int MAXTILE = 65536;

    /**
     * A tile is this wide in lat-direction.
     */
    private static final double LATTILESTEP = MAX_LATITUDE / MAXTILE;

    /**
     * A tile is this wide in lon-direction.
     */
    private static final double LONTILESTEP = MAX_LONGITUDE / MAXTILE;

    /**
     * We never return the data of more then
     * this number of tiles.
     */
    private static final int MAXTILECOUNTRETURNED = 32767;

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(TileCalculator.class.getName());


    /**
     * Calculates a tile index based upon the supplied coordinates.
     *
     * @param latLon
     *            The coordinate latitude and longitude.
     * @return The tile index value.
     */
    public long calculateTile(final LatLon latLon) {
        return calculateTile(latLon.lat(), latLon.lon());
    }

    /**
     * @param bounds some bounds
     * @return all tile-numbers that the bounds span
     */
    /*public Collection<Long> getTileIDsForBounds(final Bounds bounds) {
        List<Long> retval = new LinkedList<Long>();
        LatLon center = bounds.center();
        double centerLat = center.lat();
        double centerLon = center.lon();
        if (bounds.contains(centerLon, centerLon))
            retval.add(calculateTile(centerLon, centerLon));

        double minLon = bounds.getMin().lon();
        double minLat = bounds.getMin().lat();
        double maxLon = bounds.getMax().lon();
        double maxLat = bounds.getMax().lat();

        for (int dist = 1; true; dist++) {
            if (dist > (maxLon - minLon) / LONTILESTEP
             && dist > (maxLat - minLat) / LATTILESTEP)
                break;

            // upper
            double lat = centerLat - (dist * LATTILESTEP);
            for (double lon = centerLon - (dist * LONTILESTEP / 2); lon <= centerLon + (dist * LONTILESTEP / 2); lon += LONTILESTEP)  {
                if (lon < minLon)
                    continue;
                if (lon > maxLon)
                    break;
                if (!bounds.contains(lat, lon))
                    continue;
                retval.add(calculateTile(lat, lon));
                if (retval.size() > MAXTILECOUNTRETURNED) {
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.log(Level.FINEST, "We refuse to return the data of more than "
                            + MAXTILECOUNTRETURNED + " tiles. Called from:", new Exception("dummy-exception to trace call"));
                    } else {
                        LOG.log(Level.INFO, "We refuse to return the data of more than "
                                + MAXTILECOUNTRETURNED + " tiles");
                    }
                    return retval;
                }
            }
            // lower
            lat = centerLat + (dist * LATTILESTEP);
            for (double lon = centerLon - (dist * LONTILESTEP / 2); lon <= centerLon + (dist * LONTILESTEP / 2); lon += LONTILESTEP)  {
                if (lon < minLon)
                    continue;
                if (lon > maxLon)
                    break;
                if (!bounds.contains(lat, lon))
                    continue;
                retval.add(calculateTile(lat, lon));
                if (retval.size() > MAXTILECOUNTRETURNED) {
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.log(Level.FINEST, "We refuse to return the data of more than "
                            + MAXTILECOUNTRETURNED + " tiles. Called from:", new Exception("dummy-exception to trace call"));
                    } else {
                        LOG.log(Level.INFO, "We refuse to return the data of more than "
                                + MAXTILECOUNTRETURNED + " tiles");
                    }
                    return retval;
                }
            }
            // left
            double lon = centerLon - (dist * LONTILESTEP);
            for (lat = centerLat - (dist * LATTILESTEP / 2); lat <= centerLat + (dist * LATTILESTEP / 2); lat += LATTILESTEP)  {
                if (lat < minLat)
                    continue;
                if (lat > maxLat)
                    break;
                if (!bounds.contains(lat, lon))
                    continue;
                retval.add(calculateTile(lat, lon));
                if (retval.size() > MAXTILECOUNTRETURNED) {
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.log(Level.FINEST, "We refuse to return the data of more than "
                            + MAXTILECOUNTRETURNED + " tiles. Called from:", new Exception("dummy-exception to trace call"));
                    } else {
                        LOG.log(Level.INFO, "We refuse to return the data of more than "
                                + MAXTILECOUNTRETURNED + " tiles");
                    }
                    return retval;
                }
            }
            // right
            lon = centerLon + (dist * LONTILESTEP);
            for (lat = centerLat - (dist * LATTILESTEP / 2); lat <= centerLat + (dist * LATTILESTEP / 2); lat += LATTILESTEP)  {
                if (lat < minLat)
                    continue;
                if (lat > maxLat)
                    break;
                if (!bounds.contains(lat, lon))
                    continue;
                retval.add(calculateTile(lat, lon));
                if (retval.size() > MAXTILECOUNTRETURNED) {
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.log(Level.FINEST, "We refuse to return the data of more than "
                            + MAXTILECOUNTRETURNED + " tiles. Called from:", new Exception("dummy-exception to trace call"));
                    } else {
                        LOG.log(Level.INFO, "We refuse to return the data of more than "
                                + MAXTILECOUNTRETURNED + " tiles");
                    }
                    return retval;
                }
            }
        }*/

    /**
     * @param aBounds The bounds who's tile-ids we enumerate.
     * @param aCombineTiles the returned tileIDs have this many digits removed
     * @return This iterator gives all tile-ids that are contained in a given bounds.
     */
    public Iterator<Long> getTileIDsForBounds(final Bounds aBounds,
            final int aCombineTiles) {
        return new TileIDIterator(aBounds, aCombineTiles);
    }

    /**
     * @param aBoundingBox The bounds who's tile-ids we enumerate.
     * @param aAllTileIDs Return only members of this set.
     * @param aCombineTiles the given tileIDs have this many digits removed
     * @return This iterator gives all tile-ids that are contained in a given bounds.
     */
    public Collection<Long> getTileIDsForBounds(final Bounds aBoundingBox,
                                        final Collection<Long> aAllTileIDs,
                                        final int aCombineTiles) {
        LinkedList<Long> retval = new LinkedList<Long>();

        LatLon min = aBoundingBox.getMin();
        LatLon max = aBoundingBox.getMax();

        for (Long tileNr : aAllTileIDs) {

            XyPair[] xy = calculateXyPairFromTileNumber(tileNr, aCombineTiles);
            double minLat = calculateLatitudeFromTileY(xy[0].getYNumber());
            double minLon = calculateLongitudeFromTileX(xy[0].getXNumber());
            double maxLat = calculateLatitudeFromTileY(xy[1].getYNumber() + 1);
            double maxLon = calculateLongitudeFromTileX(xy[1].getXNumber() + 1);
            if (minLat > max.getXCoord())
                continue;
            if (minLon > max.getYCoord())
                continue;
            if (maxLat < min.getXCoord())
                continue;
            if (maxLon < min.getYCoord())
                continue;
            retval.add(tileNr);

        }
        return retval;
    }

    /**
     * @param tileNR the tile-number to calculate for
     * @param aCombineTiles the given tileIDs have this many digits removed
     * @return 2 XyPairs. 0=min, 1=max
     * @see #calculateXyPairFromTileNumber(long)
     */
    private XyPair[] calculateXyPairFromTileNumber(final long tileNR,
                                                 final int aCombineTiles) {
        if (aCombineTiles == 0) {
            XyPair ret = calculateXyPairFromTileNumber(tileNR);
            return new XyPair[] {ret, ret};
        }

        XyPair[] ret = new XyPair[] {
                new XyPair(Integer.MAX_VALUE, Integer.MAX_VALUE),
                new XyPair(Integer.MIN_VALUE, Integer.MIN_VALUE)
                };
        final int deca = 10;
        for (int i = 0; i<deca; i++) {

            XyPair[] current = calculateXyPairFromTileNumber(
                    deca * tileNR + i, aCombineTiles - 1);
            ret[0].setXNumber(Math.min(ret[0].getXNumber(),
                    current[0].getXNumber()));
            ret[0].setYNumber(Math.min(ret[0].getYNumber(),
                    current[0].getYNumber()));

            ret[1].setXNumber(Math.max(ret[1].getXNumber(),
                    current[1].getXNumber()));
            ret[1].setYNumber(Math.max(ret[1].getYNumber(),
                    current[1].getYNumber()));
        }
        return ret;
    }

    /**
     * This iterator gives all tile-ids that are contained in a given bounds.
     */
    public class TileIDIterator implements Iterator<Long> {
        /**
         * The bounds who's tile-ids
         * we enumerate.
         */
        private Bounds myBounds;

        /**
         * the returned tileIDs have this many digits removed.
         */
        private int myCombineTiles;

        /**
         * Our current position in the iteration.
         */
        private double myCurrentLat;

        /**
         * Our current position in the iteration.
         */
        private double myCurrentLon;

        /**
         * The next value we return.
         */
        private Long myNextValue = null;

        /**
         * @param aBounds The bounds who's tile-ids
         * we enumerate.
         * @param aCombineTiles the returned tileIDs have this many digits removed
         */
        public TileIDIterator(final Bounds aBounds, final int aCombineTiles) {
            super();
            myBounds = aBounds;
            myCombineTiles = aCombineTiles;
            myCurrentLat = myBounds.getMin().lat();
            myCurrentLon = myBounds.getMin().lon();

            if (getBounds().contains(myCurrentLat, myCurrentLon)) {
                myNextValue = calculateTile(myCurrentLat, myCurrentLon);
                LOG.log(Level.FINEST, "TileIterator returning " + myCurrentLat + " x " + myCurrentLon + "  = " + myNextValue);
            } else {
                myNextValue = next();
            }
        }

        /**
         * @return Returns the bounds.
         * @see #myBounds
         */
        public Bounds getBounds() {
            return myBounds;
        }

        /**
         * ${@inheritDoc}.
         */
        public boolean hasNext() {
            return myNextValue != null;
        }

        /**
         * ${@inheritDoc}.
         */
        public Long next() {
            Long myOldNextValue = myNextValue;

            while (true) {
                LatLon innerNext = innerNext();
                if (innerNext == null) {
                    myNextValue = null;
                    break;
                }
                if (getBounds().contains(innerNext.lat(), innerNext.lon())) {
                    myNextValue = calculateTile(innerNext.lat(), innerNext.lon());
                    final int deca = 10;
                    for (int i = 0; i<myCombineTiles; i++)
                        myNextValue = myNextValue / deca;
                    if (myNextValue != myOldNextValue) {
                        LOG.log(Level.FINEST, "TileIterator returning " + innerNext + "  = " + myNextValue);
                        break;
                    }
                }
            }
            return myOldNextValue;
        }

        /**
         * @return the next tile-location.
         */
        private LatLon innerNext() {

//          step inner loop
            if (myCurrentLat <= getBounds().getMax().lat()) {
                myNextValue = calculateTile(myCurrentLat, myCurrentLon);
                myCurrentLat += LATTILESTEP;
                return new LatLon(myCurrentLat, myCurrentLon);
            }

            // step outher loop
            if (myCurrentLat > getBounds().getMax().lat()) {
                myCurrentLat = myBounds.getMin().lat();
                myCurrentLon += LONTILESTEP;
            }

            // check to end outher loop
            if (myCurrentLon > getBounds().getMax().lon()) {
                return null;
            }

            return new LatLon(myCurrentLat, myCurrentLon);
        }

        /**
         * ${@inheritDoc}.
         */
        public void remove() {
            throw new IllegalArgumentException("remove makes no sense here");
        }
    };


/*        for (double lon = bounds.getMin().lon(); lon <= bounds.getMax().lon(); lon += LONTILESTEP)
            for (double lat = bounds.getMin().lat(); lat <= bounds.getMax().lat(); lat += LATTILESTEP) {
                if (!bounds.contains(lat, lon))
                    continue;
                retval.add(calculateTile(lat, lon));
                if (retval.size() > MAXTILECOUNTRETURNED) {
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.log(Level.FINEST, "We refuse to return the data of more than "
                            + MAXTILECOUNTRETURNED + " tiles. Called from:", new Exception("dummy-exception to trace call"));
                    } else {
                        LOG.log(Level.INFO, "We refuse to return the data of more than "
                                + MAXTILECOUNTRETURNED + " tiles");
                    }
                    return retval;
                }
            }
        return retval;
    }*/

    /**
     * Calculates a tile index based upon the supplied coordinates.
     *
     * @param latitude
     *            The coordinate latitude.
     * @param longitude
     *            The coordinate longitude.
     * @return The tile index value.
     */
    @Override
    public long calculateTile(final double latitude, final double longitude) {
        int x;
        int y;
        long tile;

        x = (int) Math.round((longitude + HALF_CIRCLE) * MAXTILE / MAX_LONGITUDE);
        y = (int) Math.round((latitude + QUARTER_CIRCLE) * MAXTILE / MAX_LATITUDE);

        tile = 0;

        for (int i = TILEBITCOUNT; i >= 0; i--) {
            tile = (tile << 1) | ((x >> i) & 1);
            tile = (tile << 1) | ((y >> i) & 1);
        }

        return tile;
    }

    /**
     * @param tileX an x-tile-number
     * @return the minimum longitude this corresponds to
     */
    public double calculateLongitudeFromTileX(final long tileX) {
        double x = (double)tileX;
        return (x * MAX_LONGITUDE / MAXTILE) - HALF_CIRCLE;
    }

    /**
     * @param tileX an y-tile-number
     * @return the minimum latitude this corresponds to
     */
    public double calculateLatitudeFromTileY(final long tileY) {
        double y = (double)tileY;
        return (y * MAX_LATITUDE / MAXTILE) - QUARTER_CIRCLE;
    }

    /**
     * Given a lat + lon calculate the tile-number for it.
     * @param latitude the lat
     * @param longitude the lon
     * @return the tile-number
     */
    protected long calculateTileInternal(final double latitude, final double longitude) {
        int x;
        int y;
        long tile;


        x = (int) Math.round((longitude + MAX_LONGITUDE / 2) * MAXTILE / MAX_LONGITUDE);
        y = (int) Math.round((latitude + MAX_LATITUDE / 2) * MAXTILE / MAX_LATITUDE);

        tile = 0;

        for (int i = TILEBITCOUNT; i >= 0; i--) {
            tile = (tile << 1) | ((x >> i) & 1);
            tile = (tile << 1) | ((y >> i) & 1);
        }

        return tile;
    }

    /**
     * Take a tile-number and convert it into a pair of x and y tile-coordinates.
     * @param tile the tile to parse
     * @return the pair of coordinates
     */
    public XyPair calculateXyPairFromTileNumber(final long tile) {
        XyPair pair = new XyPair();
        long x = 0;
        long y = 0;

        for (int i = TILEBITCOUNT; i >= 0; i--) {

            y = (y << 1) | ((tile >> (i * 2)) & 1);

            x = (x << 1) | ((tile >> ((i * 2) + 1)) & 1);

        }
        pair.myXNumber = (int) x;
        pair.myYNumber = (int) y;

        return pair;
    }

    /**
     * A pair of X and Y -tile-coordinated.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    public static class XyPair {

        /**
         * Construct the pair (0, 0).
         */
        public XyPair() {
            this.myXNumber = 0;
            this.myYNumber = 0;
        }

        /**
         * Construct the pair (x, y).
         * @param x the x-number of the tile.
         * @param y the y-number of the tile.
         */
        public XyPair(final int x, final int y) {
            this.myXNumber = x;
            this.myYNumber = y;
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public String toString() {
            return "[" + myXNumber + "," + myYNumber + "]";
        }

        /**
         * The x-number of the tile.
         */
        private int myXNumber;

        /**
         * The y-number of the tile.
         */
        private int myYNumber;

        /**
         * @return the tile-number for this pair
         */
        public long getTile() {
            long tile = 0;

            for (int i = TILEBITCOUNT; i >= 0; i--) {
                tile = (tile << 1) | ((getXNumber() >> i) & 1);
                tile = (tile << 1) | ((getYNumber() >> i) & 1);
            }

            return tile;
        }

        /**
         * @return The x-number of the tile.
         * @see #myXNumber
         */
        public int getXNumber() {
            return this.myXNumber;
        }

        /**
         * @return The y-number of the tile.
         * @see #myYNumber
         */
        public int getYNumber() {
            return this.myYNumber;
        }

        /**
         * @param pNumber The x-number of the tile. to set.
         * @see #myXNumber
         */
        public void setXNumber(final int pNumber) {
            this.myXNumber = pNumber;
        }

        /**
         * @param pNumber The y-number of the tile. to set.
         * @see #myYNumber
         */
        public void setYNumber(final int pNumber) {
            this.myYNumber = pNumber;
        }

    }


}
