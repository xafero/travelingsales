/**
 * IDIndexFile.java
 * created: 16.11.2008 14:25:24
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


//automatically created logger for debug and error -output
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//automatically created propertyChangeListener-Support
//import java.beans.PropertyChangeListener;
//import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * IDIndexFile.java<br/>
 * created: 16.11.2008 14:25 <br/>
 *<br/><br/>
 * <b>This is the *.idx-file as described in
 * <a href="http://wiki.openstreetmap.org/index.php/User:MarcusWolschon%5Cosmbin_draft">here</a></b>
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:OsmbinClasses.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class IDIndexFile extends FixedRecordFile implements IIDIndexFile {

    /**
     * 1000.
     */
    private static final double MILLI = 1000.0;

    /**
     * Minimum number of records to allocat4e on growing.
     */
    private static final int MINIMUMALLOCATION = 16384;

    /**
     * How many children does an inner node have?
     */
    private static final int TREEORDER = 4;
    /**
     * How many bits of the ID an inner node
     * represent??
     */
    private static final int TREEORDERBITS = 2;
    /**
     * How many levels does this  tree have?
     */
    private static final int TREEDEPTH = 1 + 64 / TREEORDERBITS;

    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_CACHE_INITIAL_CAPACITY = 16;

    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_CACHE_LOAD_FACTOR = 0.75f;

    /**
     * A cache to not look up the same ID twice.
     */
    private Map<Long, Long> myCache = Collections.synchronizedMap(
            new LinkedHashMap<Long, Long>(DEFAULT_CACHE_INITIAL_CAPACITY, DEFAULT_CACHE_LOAD_FACTOR, true) {

                /**
                 * generated.
                 */
                private static final long serialVersionUID = -2064475227892362436L;
                /**
                 * Maximum size of this cache
                 */
                static final int MAXCACHESIZE = 256;
                /**
                 * {@inheritDoc}
                 */
                @Override
                protected boolean removeEldestEntry(final Map.Entry<Long, Long> anEldestEntry) {
                    return (size() > MAXCACHESIZE);
                }
            });


    /**
     * The last record that can possibly be occupied.
     */
    protected long myLastRecordWritten = Long.MAX_VALUE;

    /**
     * Cache the index to the records that may
     * be free to avoid scanning for free records.
     */
    protected BitSet myUsedRecordIndex = new BitSet();

    /**
     * @param aFileName the filename of the *.idx -file
     * @throws IOException if we cannot open or create the file.
     */
    public IDIndexFile(final File aFileName) throws IOException {
        super(aFileName);
        this.myLastRecordWritten = getRecordCount();
    }

    /**
     * Calculate some statistics about the tree.
     * @param args 1 argument = .idx file-name
     */
    public static void main(final String[] args) {
        try {
            FileInputStream in = new FileInputStream(new File(args[0]));
            //FileInputStream in = new FileInputStream(new File("D:\\.osm\\nodes.idx"));
            final int bytesPerInt = 4;
            final int recordSize = bytesPerInt * TREEORDER;
            byte[] record = new byte[recordSize];
            ByteBuffer recordBuffer = ByteBuffer.wrap(record);
            long[] history = new long[TREEORDER + 1];
            for (int i = 0; i < history.length; i++) {
                history[i] = 0;
            }
            long recordNr = 0;
            final int kilo = 1024;
            long emptySlots = 0;
            while (in.read(record) == recordSize) {
                if (recordNr % kilo == 0) {
                    System.out.println("reat record " + recordNr + " = " + ((recordNr * recordSize) / (kilo * kilo)) + " MB");
                }
                recordNr++;
                recordBuffer.rewind();
                int order = TREEORDER;
                for (int i = 0; i < TREEORDER; i++) {
                    int entry = recordBuffer.getInt();
                    if (isEmptyMarker(entry)) {
                        order--;
                    }
                }
                history[order]++;
                emptySlots += (TREEORDER - order);
            }
            in.close();
            for (int i = 0; i < history.length; i++) {
                System.out.println("We have " + history[i] + " Tree-Nodes with " + i + " children.");
            }
            System.out.println("Empty records consume " + (history[0] * recordSize / (kilo * kilo)) + "MB of disk-space");
            System.out.println("Empty slots   consume " + (emptySlots * bytesPerInt / (kilo * kilo)) + "MB of disk-space");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(IDIndexFile.class
            .getName());
    /**
     * If we need to grow the file,
     * create this may spare records.
     */
    private static final int GROWEXCESSRECORDS = 5120000;

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
        return bytesPerInt * TREEORDER;
    }


    /**
     * @param record the recordNumber to check
     * @return true if the record is free for reuse.
     * @throws IOException if the record cannot be reat
     */
    public boolean isRecordUnused(final long record) throws IOException {
        ByteBuffer mem = getRecordForReading(record);
        for (int i = 0; i < TREEORDER; i++) {
            // the empty record consists of only Integer.MIN_VALUE
            if (!isEmptyMarker(mem.getInt())) {
                releaseRecord(mem);
                return false;
            }
        }
        releaseRecord(mem);
        return true;
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
        for (long i = myFirstFreeRecordIndex; i < recordCount; i = myUsedRecordIndex.nextClearBit((int) i + 1)) {
            if (i == ignoreThisOne) {
                continue;
            }
            if (i > this.myLastRecordWritten) {
                this.myLastRecordWritten++;
                myFirstFreeRecordIndex = i + 1;
                myUsedRecordIndex.set((int) i);
                return i;
            }
            if (isRecordUnused(i)) {
                myFirstFreeRecordIndex = i + 1;
                myUsedRecordIndex.set((int) i);
                return i;
            }
        }

        // increase the file-length, blank the additional
        // record and create a new memory-mapping with the
        // new size
        long start =  System.currentTimeMillis();
        LOG.info("Growing an Index-File... from " + recordCount + " to " + (recordCount + GROWEXCESSRECORDS) + " records");
        try {
            long grown = growFile(GROWEXCESSRECORDS, MINIMUMALLOCATION);
            myFirstFreeRecordIndex = recordCount;
            LOG.info("Growing an Index-File...initializing new records");
            for (long i = recordCount; i < recordCount + grown; i++) {
                invalidateRecord(i);
            }
        } catch (IOException e) {
            if (e.getCause() != null && e.getCause() instanceof OutOfMemoryError) {
                LOG.log(Level.SEVERE, "OutOfMemoryError while memory-mapping index-file. Trying smaller size-increase.", e);
                System.gc();
                System.runFinalization();
                long grown = growFile(GROWEXCESSRECORDS / 4, MINIMUMALLOCATION);
                LOG.info("Growing an Index-File (second atempt)...initializing new records");
                for (long i = recordCount; i < recordCount + grown; i++) {
                    invalidateRecord(i);
                }
            } else {

                LOG.log(Level.SEVERE, "Error while memory-mapping index-file.", e);
                throw e;
            }
        }
        LOG.info("Growing an Index-File...done " + ((System.currentTimeMillis() - start) / MILLI) + " seconds");
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
        for (int i = 0; i < TREEORDER; i++) {
            mem.putInt(Integer.MIN_VALUE);
        }
        mem.position(start);
        writeRecord(mem, anIndex);
        this.myUsedRecordIndex.clear((int) anIndex);
        if (anIndex < myFirstFreeRecordIndex) {
            myFirstFreeRecordIndex = anIndex;
        }
        if (anIndex == myLastRecordWritten) {
            myLastRecordWritten--;
        }
    }

    /**
     * internally used for error-messages on assertions.
     */
    private static final ThreadLocal<StringBuilder> DEBUGLOGS = new ThreadLocal<StringBuilder>();

    /**
     * Add the given entity-ID to record-number -mapping to this index.
     * @param id the ID of the entity
     * @param value the record-number where the entity with this ID is stored
     * @throws IOException if we cannot grow the file
     */
    public void put(final long id, final long value) throws IOException {
        DEBUGLOGS.set(new StringBuilder());
        if (value >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("we cannot store a value thig big.");
        }
        if (id >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("we cannot store a key thig big.");
        }
        this.myCache.put(id, value);
        final int bytesPerInt = 4;
        long recordNumber = 0; //ROOT
        if (getRecordCount() == 0) {
            // create root-record
            growFile(1, 1);
            invalidateRecord(0);
            myFirstFreeRecordIndex = 1;
        }
        long remainingID = id;
        for (int depth = 0; depth < TREEDEPTH; depth++) {
            int significantBits = (int) (remainingID & (TREEORDER - 1));
            remainingID = remainingID >> TREEORDERBITS;

            // leaf
            if (depth == TREEDEPTH - 1) {
                // it is imortant to use getRecordForReading here because we set only 4 bytes and leave the rest of the record alone
                ByteBuffer mem = getRecordForReading(recordNumber);
                int start1 = mem.position();
                mem.position(start1 + significantBits * bytesPerInt);
                if (value > Integer.MAX_VALUE) {
                    throw new IllegalStateException("we cannot index node-records beyond "
                            + Integer.MAX_VALUE + ". Given was recordNR " + value);
                }
                mem.putInt((int) value);
                mem.position(start1);
                this.myUsedRecordIndex.set((int) recordNumber);
                writeRecord(mem, recordNumber);
                if (recordNumber > this.myLastRecordWritten) {
                    this.myLastRecordWritten = recordNumber;
                }
                mem = null;
//                long temp = 0;
//                assert (temp = get(id)) == value : "get(id=" + id + ") after put(id=" + id + ") "
//                                       + "does not return the stored value " + value
//                                       + " but " + temp
//                                       + "\ndebuglog: " + debuglog;
                //LOG.finest(getFileName() + ":put(id=" + id + ", value=" + value + ") - leaf - " + debuglog.get());
                DEBUGLOGS.set(null);
                return;
            }
            ByteBuffer mem = getRecordForReading(recordNumber);

            // get the record-number of the child in slot {significantBits}
            mem.position(mem.position() + significantBits * bytesPerInt);
            int nextRecordNumber = mem.getInt();
            releaseRecord(mem);
            mem = null;
            // check sanity
            assert nextRecordNumber != recordNumber : "Tree-node " + recordNumber + " loops back to itself"
                                                    + " in slot " + significantBits;
            // is there such a child or do we need to create one?
            if (isEmptyMarker(nextRecordNumber)) {
                // create a new record
               mem = null;
               long newRecord = findFreeRecord(recordNumber);
//               if (LOG.isLoggable(Level.FINEST)) {
//                   DEBUGLOGS.get().append("put(id=" + id + ", value=" + value + ") depth " + depth + "/"
//                           + TREEDEPTH + " - storing tree-node in new record "
//                           + newRecord + " and storing reference to it in slot "
//                           + significantBits + " of current record " + recordNumber + "\n");
//               }
               if (newRecord > Integer.MAX_VALUE) {
                   throw new IllegalStateException("index of new record does no longer fit into an integer");
               }
               mem = getRecordForReading(recordNumber);
               int start = mem.position();
               mem.position(start + significantBits * bytesPerInt);
               // write reference to new tree-child in {newRecord}
               // into slot {significantBits}
               // of the current tree-node in recordNumber
               mem.putInt((int) newRecord);
               mem.position(start);
               this.myUsedRecordIndex.set((int) recordNumber);
               writeRecord(mem, recordNumber);
               if (recordNumber > this.myLastRecordWritten) {
                   this.myLastRecordWritten = recordNumber;
               }
               mem = null;
               recordNumber = newRecord;
            } else {
//                if (LOG.isLoggable(Level.FINEST)) {
//                    DEBUGLOGS.get().append("put(id=" + id + ", value=" + value + ") depth " + depth + "/"
//                            + TREEDEPTH + " - traversing down to record "
//                            + nextRecordNumber + " and referenced in slot "
//                            + significantBits + " of current record " + recordNumber + "\n");
//                }
                recordNumber = nextRecordNumber;
            }
        }
        //LOG.finest(getFileName() + ":put(id=" + id + ", value=" + value + ") - " + debuglog.get());
        DEBUGLOGS.set(null);
    }
    /**
     * @param nextRecordNumber the record-number to test
     * @return true if this is the marker for an empty record
     */
    private static boolean isEmptyMarker(final int nextRecordNumber) {
        //final int secondary = -2147483648;
        return nextRecordNumber < 0; //== Integer.MIN_VALUE || nextRecordNumber == secondary;
    }
    /**
     * Remove the given entity from this index.
     * @param id the ID of the entity
     * @throws IOException if we cannot read or write
     */
    public void remove(final long id) throws IOException {
        DEBUGLOGS.set(new StringBuilder());
        final int bytesPerInt = 4;
        int recordNumber = 0; //ROOT
        long remainingID = id;
        this.myCache.remove(id);
        for (int depth = 0; depth < TREEDEPTH; depth++) {
            if (recordNumber >= getRecordCount()) {
                //debuglog.get().append(" id=" + id + " depth=" + depth + " record=" + recordNumber + " hitting record-nr beyond end of file\n");
                //LOG.finest(getFileName() + " - " + debuglog.get());
                DEBUGLOGS.set(null);
                return;
            }
            ByteBuffer mem = getRecordForReading(recordNumber);
            int significantBits = (int) (remainingID & (TREEORDER - 1));
            remainingID = remainingID >> TREEORDERBITS;
            int start = mem.position();
            mem.position(start + significantBits * bytesPerInt);

            // leaf
            if (depth == TREEDEPTH - 1) {
                mem.putInt(Integer.MIN_VALUE);
                mem.position(start);
                writeRecord(mem, recordNumber);
                //debuglog.get().append(" id=" + id + " depth=" + depth + " record=" + recordNumber + " hitting leaf\n");
                //LOG.finest(getFileName() + ":remove() - " + debuglog.get());
                DEBUGLOGS.set(null);
                this.myCache.remove(id);
                return;
            }
            recordNumber = mem.getInt();
            DEBUGLOGS.get().append(" id=" + id + " depth=" + depth + " next record=" + recordNumber + "\n");
            releaseRecord(mem);
            if (isEmptyMarker(recordNumber)) {
                //debuglog.get().append(" id=" + id + " depth=" + depth + " record=" + recordNumber + " empty record\n");
                //LOG.finest(getFileName() + " - " + debuglog.get());
                DEBUGLOGS.set(null);
                this.myCache.remove(id);
                return;
            }
        }

        //LOG.finest(getFileName() + " - " + debuglog.get());
        DEBUGLOGS.set(null);
        //TODO: shrink the tree
        this.myCache.remove(id);
    }
    /**
     * Look up a record-number in this index.
     * @param id the ID of the entity
     * @return the record-number where the entity with this ID is stored or -1
     * @throws IOException if we cannot read the record
     */
    public long get(final long id) throws IOException {
        DEBUGLOGS.set(new StringBuilder());
        final int bytesPerInt = 4;
        long recordNumber = 0; //ROOT
        long remainingID = id;
        Long cached = this.myCache.get(id);
        if (cached != null) {
            return cached.longValue();
        }
        for (int depth = 0; depth < TREEDEPTH; depth++) {
            if (recordNumber >= getRecordCount()) {
                if (getRecordCount() > 0) {
                    LOG.log(Level.SEVERE, "current record-number "
                            + recordNumber + " is after the end of file "
                            + getRecordCount());
                }
                return -1;
            }
            int significantBits = (int) (remainingID & (TREEORDER - 1));
            remainingID = remainingID >> TREEORDERBITS;
            ByteBuffer mem = null;
            try {
                mem = getRecordForReading(recordNumber);
                mem.position(mem.position() + significantBits * bytesPerInt);
                recordNumber = mem.getInt();
                DEBUGLOGS.get().append("get(id=" + id + ") depth " + depth + "/"
                            + TREEDEPTH + " - traversing down to record "
                            + recordNumber + " referenced in slot "
                            + significantBits + " of current record\n");
            } finally {
                releaseRecord(mem);
                mem = null;
            }
            if (recordNumber <= Integer.MAX_VALUE && isEmptyMarker((int) recordNumber)) {
                LOG.log(Level.FINEST, "The sub-tree we need to walk"
                        + " down on depth " + depth + "/" + TREEDEPTH
                        + " does not exist. We seem not to contain "
                        + " the id " + id);
                return -1;
            }
            if (recordNumber < 0) {
                LOG.log(Level.FINEST, "The sub-tree we need to walk"
                        + " down on depth " + depth + "/" + TREEDEPTH
                        + " does not exist. We seem not to contain "
                        + " the id " + id);
                return -1;
            }
        }
        DEBUGLOGS.set(null);
        return recordNumber; // in the space, where the next recordNR would be, the leaf has the stored index
    }
}


