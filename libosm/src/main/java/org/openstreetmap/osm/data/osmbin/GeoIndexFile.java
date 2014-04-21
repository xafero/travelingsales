/**
 * GeoIndexFile.java
 * created: 28.11.2008 12:29
 * (c) 2008 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of libosm by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  libosm is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  libosm is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with libosm.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.osm.data.osmbin;


import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * GeoIndexFile.java<br/>
 * created: 28.11.2008 12:29 <br/>
 *<br/><br/>
 * <b>This is the *.id2-file as described in
 * <a href="http://wiki.openstreetmap.org/index.php/User:MarcusWolschon%5Cosmbin_draft">here</a></b>
 * http://en.wikipedia.org/wiki/Kd-tree
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:OsmbinClasses.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class GeoIndexFile extends FixedRecordFile implements IGeoIndexFile {

    /**
     * Minimum number of records to allocat4e on growing.
     */
    private static final int MINIMUMALLOCATION = 4096;

    /**
     * @param aFileName the filename of the *.idx -file
     * @throws IOException if we cannot open or create the file.
     */
    public GeoIndexFile(final File aFileName) throws IOException {
        super(aFileName);
    }

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(GeoIndexFile.class
            .getName());
    /**
     * If we need to grow the file,
     * create this may spare records.
     */
    private static final int GROWEXCESSRECORDS = 512000;

    /**
     * Cache the index to the first record that may
     * be free.
     */
    private long myFirstFreeRecordIndex = 0;

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "IDIndexFile" + hashCode();
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public int getRecordLength() {
        final int bytesPerInt = 4;
        final int bytesPerLong = 8;
        return 0
            + 2 * bytesPerLong // lat+lon
            + bytesPerInt      // nodeRecordNumber
            + 2 * bytesPerInt; // left+richt child, record-NR
    }

    /**
     * @param record the recordNumber to check
     * @return true if the record is free for reuse.
     * @throws IOException if the record cannot be reat
     */
    public boolean isRecordUnused(final long record) throws IOException {
        ByteBuffer mem = getRecordForReading(record);
        try {
            long lat = mem.getLong();
            long lon = mem.getLong();
            return lat == Long.MIN_VALUE && lon == Long.MIN_VALUE;
        } finally {
            releaseRecord(mem);
        }
    }



    /**
     * Find a
     * free record and return the index of the
     * first one.
     * @param ignoreThisOne do not return the record with this number
     * @return the index of the first one
     * @throws IOException if we cannot grow the file
     */
    private long findFreeRecord(final long ignoreThisOne) throws IOException {
        long recordCount = getRecordCount();
        for (long i = myFirstFreeRecordIndex; i < recordCount; i++) {
            if (i != ignoreThisOne && isRecordUnused(i)) {
                myFirstFreeRecordIndex = i;
                return i;
            }
        }

        // increase the file-length, blank the additional
        // record and create a new memory-mapping with the
        // new size
        long start =  System.currentTimeMillis();
        LOG.info("Growing a 2D-Index-File... from " + recordCount + " to " + (recordCount + GROWEXCESSRECORDS) + " records");
        /*long grown = */growFile(GROWEXCESSRECORDS, MINIMUMALLOCATION);
        myFirstFreeRecordIndex = recordCount;
        LOG.info("Growing an Index-File...initializing new records");
        long newRecordCount = getRecordCount();
        assert newRecordCount > recordCount;
        for (long i = recordCount; i < newRecordCount; i++) {
            invalidateRecord(i);
        }
        final double msPerSecond = 1000.0;
        LOG.info("Growing an 2D-index-File...done " + ((System.currentTimeMillis() - start) / msPerSecond) + " seconds");
        return recordCount;
    }



    /**
     * Declare the record with the given index free for reuse.
     * @param anIndex the index to overwrite.
     * @throws IOException if we cannot write the record
     */
    private void invalidateRecord(final long anIndex) throws IOException {
        ByteBuffer mem = getRecordForWriting(anIndex);
        int start = mem.position();

        mem.putLong(Long.MIN_VALUE);
        mem.putLong(Long.MIN_VALUE);
        mem.putInt(Integer.MIN_VALUE);
        mem.position(start);
        writeRecord(mem, anIndex, false);

        if (anIndex < myFirstFreeRecordIndex) {
            myFirstFreeRecordIndex = anIndex;
        }
    }

    /**
     * Add the given entity-ID to record-number -mapping to this index.
     * @param aNodeRecordIndex the record-number in nodex.obm of the node
     * @param aLatitude the latitude to index by encoded as a long integer
     * @param aLongitude the longitude to index by encoded as a long integer
     * @throws IOException if we cannot grow the file
     * @see FixedPrecisionCoordinateConvertor
     */
    public void put(final long aNodeRecordIndex, final long aLatitude, final long aLongitude) throws IOException {

        if (aNodeRecordIndex > Integer.MAX_VALUE) {
            throw new IllegalStateException("we are unable to index a record-index "
                    + "above " + Integer.MAX_VALUE + " (given: " + aNodeRecordIndex + ")");
        }
        if (getRecordCount() == 0) {
            // create root-record
            growFile(1, 1);
            invalidateRecord(0);
        }
        put(true, 0, aNodeRecordIndex, aLatitude, aLongitude, new HashSet<Long>());
    }

    /**
     * @param aValue the record-number in nodex.obm of the node
     * @param aLatitude the latitude to index by encoded as a long integer
     * @param aLongitude the longitude to index by encoded as a long integer
     * @param currentRecordNumber the node  we are at
     * @param evenDepth true if we are at the root-node (depth 0) or another even depth
     * @param loopDetect store the encountered record-numbers to detect loops
     * @throws IOException in case we cannot read or write
     */
    private void put(final boolean evenDepth,
                        final long currentRecordNumber,
                        final long aValue,
                        final long aLatitude,
                        final long aLongitude,
                        final Set<Long> loopDetect) throws IOException {
        if (!loopDetect.add(currentRecordNumber)) {
            throw new IllegalStateException("uncorrectable loop detected"
                    + " in tree-node " + currentRecordNumber);
        }
        if (currentRecordNumber > getRecordCount()) {
            throw new IllegalArgumentException("currentRecordNumber beyond end of file");
        }

        ByteBuffer mem = getRecordForReading(currentRecordNumber);
        int start = mem.position();
        try {
            long currentLat = mem.getLong();
            long currentLon = mem.getLong();
            // is this an empty record?
            if (currentLat == Long.MIN_VALUE && currentLon == Long.MIN_VALUE) {
                // put the node here
                mem.position(start);

                // 2x4 byte lat+lon
                mem.putLong(aLatitude);
                mem.putLong(aLongitude);
                mem.putInt((int) aValue);

                // empty children
                mem.putInt(Integer.MIN_VALUE);
                mem.putInt(Integer.MIN_VALUE);
                mem.position(start);
                writeRecord(mem, currentRecordNumber);
                mem = null;
                return;
            }

            // recurse
            int currentValue = mem.getInt();
            int leftChild = mem.getInt();
            int rightChild = mem.getInt();
            if (evenDepth) {
                if (aLatitude > currentLat) {
                    if (loopDetect.contains(leftChild)) {
                        LOG.severe("Loop detected! Tree-node "
                                + currentRecordNumber + " loops back to "
                                + "tree node " + leftChild
                                + " - repairing tree.");
                        leftChild = Integer.MIN_VALUE;
                    }
                    if (leftChild == Integer.MIN_VALUE) {
                        // no left child -> create new left child
                        leftChild = (int) findFreeRecord(currentRecordNumber);
                        assert leftChild != currentRecordNumber;
                        assert rightChild != currentRecordNumber;
                        mem.position(start);
                        mem.putLong(currentLat);
                        mem.putLong(currentLon);
                        mem.putInt(currentValue);
                        mem.putInt(leftChild);
                        mem.putInt(rightChild);
                        mem.position(start);
                        writeRecord(mem, currentRecordNumber);
                        mem = null;
                    }
                    put(!evenDepth, leftChild,
                            aValue,
                            aLatitude, aLongitude,
                            loopDetect);
                } else {
                    if (loopDetect.contains(rightChild)) {
                        LOG.severe("Loop detected! Tree-node "
                                + currentRecordNumber + " loops back to "
                                + "tree node " + rightChild
                                + " - repairing tree.");
                        rightChild = Integer.MIN_VALUE;
                    }
                    if (rightChild == Integer.MIN_VALUE) {
                        // no left child -> create new right child
                        rightChild = (int) findFreeRecord(currentRecordNumber);
                        assert leftChild != currentRecordNumber;
                        assert rightChild != currentRecordNumber;
                        mem.position(start);
                        mem.putLong(currentLat);
                        mem.putLong(currentLon);
                        mem.putInt(currentValue);
                        mem.putInt(leftChild);
                        mem.putInt(rightChild);
                        mem.position(start);
                        writeRecord(mem, currentRecordNumber);
                        mem = null;
                    }
                    put(!evenDepth, rightChild,
                            aValue,
                            aLatitude, aLongitude, loopDetect);
                }
            } else {
                if (aLongitude > currentLon) {
                    if (loopDetect.contains(leftChild)) {
                        LOG.severe("Loop detected! Tree-node "
                                + currentRecordNumber + " loops back to "
                                + "tree node " + leftChild
                                + " - repairing tree.");
                        leftChild = Integer.MIN_VALUE;
                    }
                    if (leftChild == Integer.MIN_VALUE) {
                        // no left child -> create new left child
                        leftChild = (int) findFreeRecord(currentRecordNumber);
                        assert leftChild != currentRecordNumber;
                        assert rightChild != currentRecordNumber;
                        mem.position(start);
                        mem.putLong(currentLat);
                        mem.putLong(currentLon);
                        mem.putInt(currentValue);
                        mem.putInt(leftChild);
                        mem.putInt(rightChild);
                        mem.position(start);
                        writeRecord(mem, currentRecordNumber);
                        mem = null;
                    }
                    put(!evenDepth, leftChild,
                            aValue,
                            aLatitude, aLongitude, loopDetect);
                } else {
                    if (loopDetect.contains(rightChild)) {
                        LOG.severe("Loop detected! Tree-node "
                                + currentRecordNumber + " loops back to "
                                + "tree node " + rightChild
                                + " - repairing tree.");
                        rightChild = Integer.MIN_VALUE;
                    }
                    if (rightChild == Integer.MIN_VALUE) {
                        // no left child -> create new right child
                        rightChild = (int) findFreeRecord(currentRecordNumber);
                        assert leftChild != currentRecordNumber;
                        assert rightChild != currentRecordNumber;
                        mem.position(start);
                        mem.putLong(currentLat);
                        mem.putLong(currentLon);
                        mem.putInt(currentValue);
                        mem.putInt(leftChild);
                        mem.putInt(rightChild);
                        mem.position(start);
                        writeRecord(mem, currentRecordNumber);
                        mem = null;
                    }
                    put(!evenDepth, rightChild,
                            aValue,
                            aLatitude, aLongitude, loopDetect);
                }
            }

        } finally {
            if (mem != null) {
                releaseRecord(mem);
            }
        }
    }

    /**
     * Remove the given entity from this index.
     * @param aNodeRecordIndex the record-number in nodex.obm of the node
     * @param aLatitude the latitude to index by encoded as a long integer
     * @param aLongitude the longitude to index by encoded as a long integer
     * @throws IOException if we cannot write or seek in the file.
     * @see FixedPrecisionCoordinateConvertor
     */
    public void remove(final long aNodeRecordIndex, final long aLatitude, final long aLongitude) throws IOException {

        if (aNodeRecordIndex > Integer.MAX_VALUE) {
            throw new IllegalStateException("we are unable to index a record-index "
                    + "above " + Integer.MAX_VALUE + " (given: " + aNodeRecordIndex + ")");
        }
        if (getRecordCount() == 0) {
            // create root-record
            growFile(1, 1);
            invalidateRecord(0);
        }
        remove(true, 0, aNodeRecordIndex, aLatitude, aLongitude);
    }
    /**
     * Remove the given entity from this index.
     * @param aNodeRecordIndex the record-number in nodex.obm of the node
     * @param aLatitude the latitude to index by encoded as a long integer
     * @param aLongitude the longitude to index by encoded as a long integer
     * @param currentRecordNumber the node  we are at
     * @param evenDepth true if we are at the root-node (depth 0) or another even depth
     * @throws IOException if we cannot write or seek in the file.
     * @see FixedPrecisionCoordinateConvertor
     */
    private void remove(final boolean evenDepth,
            final long currentRecordNumber,
            final long aNodeRecordIndex,
            final long aLatitude,
            final long aLongitude) throws IOException {

        if (currentRecordNumber < 0 || isRecordUnused(currentRecordNumber)) {
            return;
        }
        ByteBuffer mem = getRecordForReading(currentRecordNumber);
        int start = mem.position();
        try {
            // recurse
            long currentLat = mem.getLong();
            long currentLon = mem.getLong();
            int value = mem.getInt();
            int leftChild = mem.getInt();
            if (leftChild == currentRecordNumber) {
                LOG.severe("Record " + currentRecordNumber + " contains itself as a left cild!");
                leftChild = Integer.MIN_VALUE;
            }
            int rightChild = mem.getInt();
            if (rightChild == currentRecordNumber) {
                LOG.severe("Record " + currentRecordNumber + " contains itself as a right cild!");
                rightChild = Integer.MIN_VALUE;
            }
            if (currentLat == Long.MIN_VALUE && currentLon == Long.MIN_VALUE) {
                // empty record
                releaseRecord(mem);
                mem = null;
                return;
            }
            if (value == aNodeRecordIndex) {
                // TODO: implement actual deletion
                mem.position(start);
                currentLat = mem.getLong();
                currentLon = mem.getLong();
                mem.putInt(Integer.MIN_VALUE); //this is a trick to return -1
                mem.position(start);
                writeRecord(mem, currentRecordNumber);
                mem = null;
                return;
            }
            if (evenDepth) {
                if (aLatitude > currentLat) {
                    remove(!evenDepth, leftChild,
                            aNodeRecordIndex,
                            aLatitude, aLongitude);
                } else {
                    remove(!evenDepth, rightChild,
                            aNodeRecordIndex,
                            aLatitude, aLongitude);
                }
            } else {
                if (aLongitude > currentLon) {
                    remove(!evenDepth, leftChild,
                            aNodeRecordIndex,
                            aLatitude, aLongitude);
                } else {
                    remove(!evenDepth, rightChild,
                            aNodeRecordIndex,
                            aLatitude, aLongitude);
                }
            }
        } finally {
            if (mem != null) {
                releaseRecord(mem);
            }
        }

    }

    /**
     * Look up a record-number in this index.
     * @param aMinLatitude the minimum (inclusive) latitude to index by encoded as a long integer
     * @param aMinLongitude the minimum (inclusive) longitude to index by encoded as a long integer
     * @param aMaxLatitude the maximum (inclusive) latitude to index by encoded as a long integer
     * @param aMaxLongitude the maximum (inclusive) longitude to index by encoded as a long integer
     * @return the record-numbers in nodes.obm where the entities with these coordinates are stored or an empty set
     * @throws IOException if we cannot read from the file
     * @see FixedPrecisionCoordinateConvertor
     * @see NodesFile
     */
    public Set<Long> get(final long aMinLatitude, final long aMinLongitude, final long aMaxLatitude, final long aMaxLongitude) throws IOException {
        Set<Long> retval = get(true, new HashSet<Long>(), 0, aMinLatitude, aMinLongitude, aMaxLatitude, aMaxLongitude);
        LOG.log(Level.FINER, "GeoIndex returning " + retval.size() + " elements for bounding-box");
        return retval;
    }

    /**
     * Look up a record-number in this index.
     * @param aMinLatitude the minimum (inclusive) latitude to index by encoded as a long integer
     * @param aMinLongitude the minimum (inclusive) longitude to index by encoded as a long integer
     * @param aMaxLatitude the maximum (inclusive) latitude to index by encoded as a long integer
     * @param aMaxLongitude the maximum (inclusive) longitude to index by encoded as a long integer
     * @param retval nodes collected to far in recursion
     * @param currentRecordNumber the node  to analyse
     * @param evenDepth true if we are at the root-node (depth 0) or another even depth
     * @return the record-numbers in nodes.obm where the entities with these coordinates are stored or an empty set
     * @throws IOException if we cannot read from the file
     * @see FixedPrecisionCoordinateConvertor
     * @see NodesFile
     */
    protected Set<Long> get(final boolean evenDepth,
                            final Set<Long> retval,
                            final long currentRecordNumber,
                            final long aMinLatitude,
                            final long aMinLongitude,
                            final long aMaxLatitude,
                            final long aMaxLongitude) throws IOException {

        if (currentRecordNumber  >= getRecordCount()
            || currentRecordNumber == Integer.MIN_VALUE) {
            return retval;
        }
        ByteBuffer mem = getRecordForReading(currentRecordNumber);
        //int start = mem.position();
        try {
            long currentLat = mem.getLong();
            long currentLon = mem.getLong();
            long value = mem.getInt();
            int leftChild = mem.getInt();
            int rightChild = mem.getInt();
            releaseRecord(mem);
            mem = null;

            if (rightChild == currentRecordNumber) {
                LOG.log(Level.SEVERE, "Broken tree-node "
                        + currentRecordNumber + " has itself "
                        + "as a right child. Ignoring right child.");
                rightChild = Integer.MIN_VALUE;
            }
            if (leftChild == currentRecordNumber) {
                LOG.log(Level.SEVERE, "Broken tree-node "
                        + currentRecordNumber + " has itself "
                        + "as a left child. Deleting right child.");
                leftChild = Integer.MIN_VALUE;
            }

            if (currentLat == Long.MIN_VALUE && currentLon == Long.MIN_VALUE) {
                // empty record
                return retval;
            }

            // add the current id?
            if (currentLat <= aMaxLatitude && currentLat >= aMinLatitude) {
                if (currentLon <= aMaxLongitude && currentLon >= aMinLongitude) {
                    if (value != Integer.MIN_VALUE) {
                        retval.add(value);
                    }
                }
            }
            // recurse left and right?
            if (evenDepth) {
                // recurse left ?
                if (aMaxLatitude > currentLat) {
                    get(!evenDepth, retval, leftChild, aMinLatitude, aMinLongitude, aMaxLatitude, aMaxLongitude);
                }
                // recurse right ?
                if (aMinLatitude < currentLat) {
                    get(!evenDepth, retval, rightChild, aMinLatitude, aMinLongitude, aMaxLatitude, aMaxLongitude);
                }
            } else {
             // recurse left ?
                if (aMaxLongitude > currentLon) {
                    get(!evenDepth, retval, leftChild, aMinLatitude, aMinLongitude, aMaxLatitude, aMaxLongitude);
                }
                // recurse right ?
                if (aMinLongitude < currentLon) {
                    get(!evenDepth, retval, rightChild, aMinLatitude, aMinLongitude, aMaxLatitude, aMaxLongitude);
                }
            }
            // recurse right?
        } finally {
            if (mem != null) {
                releaseRecord(mem);
            }
        }
        return retval;
    }
   
}


