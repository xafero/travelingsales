/**
 * FixedRecordFile.java
 * created: 09.11.2008 12:27:55
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

//other imports
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;

//automatically created logger for debug and error -output
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

//automatically created propertyChangeListener-Support
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * FixedRecordFile.java<br/>
 * created: 09.11.2008 12:27:55 <br/>
 *<br/><br/>
 * <b>This class represents a file with fixed-size records mapped into memory!</b>
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:OsmbinClasses.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public abstract class FixedRecordFile {

    /**
     * We map at most this many bytes into memory.
     */
    private static final int MAXSIZE = Integer.MAX_VALUE;

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(FixedRecordFile.class
            .getName());

    /**
     * The memory-mapped content of our file.
     * @see #memoryMapped
     */
    private MappedByteBuffer memoryMapped;

    /**
     * {@link #memoryMapped} need not map the
     * whole file. So this value tells us
     * what position in the file the first
     * byte of the mapping has.
     */
    private long memoryMappedFirstByte = 0;

    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_CACHE_INITIAL_CAPACITY = 16;

    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_CACHE_LOAD_FACTOR = 0.75f;


    /**
     * All records thrown out of {@link #myRecordBufferCache}
     * are inserted here to be re-used for a different record-number.
     */
    private ConcurrentLinkedQueue<ByteBuffer> myEmptyBuffers = new ConcurrentLinkedQueue<ByteBuffer>();

    /**
     * Whenever memory-mapping fails, we use
     * conventional IO. For that we often need
     * temporary buffers the size of a single
     * record. This collection caches such buffers
     * for reuse.
     * It is not guaranteed that the contained buffers are
     * rewound.
     * @see #getRecordLength()
     * @see #getRecordForWriting(long)
     * @see #releaseRecord(ByteBuffer)
     */
    private Map<Long, ByteBuffer> myRecordBufferCache = Collections.synchronizedMap(
            new LinkedHashMap<Long, ByteBuffer>(DEFAULT_CACHE_INITIAL_CAPACITY, DEFAULT_CACHE_LOAD_FACTOR, true) {

                /**
                 * generated.
                 */
                private static final long serialVersionUID = -2064475227892362436L;
                /**
                 * Maximum size of this cache
                 */
                static final int MAXCACHESIZE = 4096;//1024;
                /**
                 * {@inheritDoc}
                 */
                @Override
                protected boolean removeEldestEntry(final Map.Entry<Long, ByteBuffer> anEldestEntry) {
                    if (size() > MAXCACHESIZE) {
                        this.remove(anEldestEntry.getKey());
                        myEmptyBuffers.add(anEldestEntry.getValue());
                    }
                    return false;
                }
            });

//    /**
//     * Records we have not yet written to disk.
//     * @see #getRecordLength()
//     * @see #getRecordForWriting(long)
//     * @see #releaseRecord(ByteBuffer)
//     */
//    private Map<Long, ByteBuffer> myDirtyRecordCache =
//            new LinkedHashMap<Long, ByteBuffer>(DEFAULT_CACHE_INITIAL_CAPACITY, DEFAULT_CACHE_LOAD_FACTOR, true);

    /**
     * Number of records in file.
     * We must know this because the buffer is not initialized
     * beyond the end of the file.
     */
    private long myRecordCount;

    /**
     * The file we use.
     */
    private File myFileName;

    /**
     * @param aFileName the file to use
     * @throws IOException if we cannot memory-map the file
     */
    public FixedRecordFile(final File aFileName) throws IOException {
        if (!aFileName.exists()) {
            if (!aFileName.createNewFile()) {
                throw new IOException("file '" + aFileName.getAbsolutePath() + "' could not be created.");
            }
        }
        setFileName(aFileName);
        long recordCnt = aFileName.length() / getRecordLength();
        if (recordCnt > Integer.MAX_VALUE) {
            throw new IllegalStateException("recordcount too large to be represented as an integer");
        }
        setRecordCount((int) recordCnt);
        //myFileChannel = new RandomAccessFile(aFileName, "rws").getChannel();
        myFileChannel = new RandomAccessFile(aFileName, "rw").getChannel();
        try {
            this.memoryMapped = myFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, aFileName.length());
        } catch (Exception e) {
            LOG.log(Level.INFO, "Memory-maping the file failed in "
                       + getClass().getName()
                       + " - using conventional io instead");
        }
    }

    /**
     * @return the length of a record. (Must be a fixed number)
     */
    public abstract int getRecordLength();
    /**
     * @return true if the currently selected record is free for reuse.
     */
    //public abstract boolean isRecordUnused();

    /**
     * Position us at the record with the given number.
     * The returned record may be memory-mapped or
     * simply a ByteBuffer containing the selected Record.
     * You need to call either this method or {@link #releaseRecord(ByteBuffer)}
     * for the returned record later.
     * @param aRecordNumber the number of the record to select
     * @return a ByteBuffer positioned at that record.
     * @throws IOException if we cannot read
     */
    public ByteBuffer getRecordForReading(final long aRecordNumber) throws IOException {

        ByteBuffer cached = this.myRecordBufferCache.get(aRecordNumber);
//        if (cached  != null) {
//            LOG.info("getRecordForReading() retrieved record " + aRecordNumber + " from cache");
//        }
//        if (cached == null) {
////            synchronized (this.myDirtyRecordCache) {
//                cached = this.myDirtyRecordCache.get(aRecordNumber);
////            }
////            if (cached  != null) {
////                LOG.warning("getRecordForReading() retrieved record " + aRecordNumber + " from being written");
////            }
//        }
        if (cached != null) {
            cached = cached.duplicate();
            cached.rewind();
            return cached;
        }
        ByteBuffer record = getRecordForWriting(aRecordNumber);
        if (record instanceof MappedByteBuffer) {
            // memory-mapped
            return record;
        }
        // read the record using conventional IO
        record.rewind();
        if (this.myFileChannel == null || !this.myFileChannel.isOpen()) {
            this.myFileChannel = new RandomAccessFile(this.myFileName, "rw").getChannel();
        }
        try {
            synchronized (this.myFileChannel) {
                long newPosition = aRecordNumber * getRecordLength();
                if (this.myFileChannel.position() != newPosition) {
                    this.myFileChannel.position(newPosition);
                }
                int reat = 0;
                while (reat < getRecordLength()) {
                    int readNow = this.myFileChannel.read(record);
                    if (readNow < 0) {
                        this.myRecordCount = aRecordNumber;
                        throw new EOFException("record " + record + " seems to be beyong the end of the file");
                    }
                    reat += readNow;
                }
            }
            this.myRecordBufferCache.put(aRecordNumber, record);
        } catch (ClosedChannelException e) {
            LOG.log(Level.SEVERE, "Cannot read record "
                    + aRecordNumber + " at "
                    + (aRecordNumber * getRecordLength()), e);
            this.myFileChannel = null;
            return getRecordForReading(aRecordNumber);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read record "
                    + aRecordNumber + " at "
                    + (aRecordNumber * getRecordLength()), e);
        }
        record.rewind();
        return record;
    }
    /**
     * Position us at the record with the given number.
     * The returned record may be memory-mapped or
     * simply a ByteBuffer the site of the selected Record.
     * You need to call either this method or {@link #releaseRecord(ByteBuffer)}
     * for the returned record later.
     * @param aRecordNumber the number of the record to select
     * @return a ByteBuffer positioned at that record.
     */
    public ByteBuffer getRecordForWriting(final long aRecordNumber) {
        if (aRecordNumber < 0) {
            throw new IllegalArgumentException("aRecordNumber < 0");
        }
        if (aRecordNumber > getRecordCount()) {
            throw new IllegalArgumentException("aRecordNumber > getRecordCount()");
        }
        long pos = aRecordNumber * (long) getRecordLength();

        if (this.memoryMapped == null
                || pos > Integer.MAX_VALUE
                || pos < memoryMappedFirstByte
                || pos + getRecordLength() > this.memoryMapped.limit()) {
            // memory-mapping failed, allocate a conventional buffer
            // (the queue is thread-safe)
            ByteBuffer cached = this.myRecordBufferCache.get(aRecordNumber);
//            if (cached  != null) {
//                LOG.warning("getRecordForWriting() retrieved record " + aRecordNumber + " from cache");
//            }
//            if (cached == null) {
//                cached = this.myDirtyRecordCache.get(aRecordNumber);
//                if (cached != null) {
//                    synchronized (this.myDirtyRecordCache) {
//                        this.myDirtyRecordCache.remove(aRecordNumber);
//                    }
//                }
////                if (cached  != null) {
////                    LOG.warning("getRecordForWriting() unscheduled record " + aRecordNumber + " from being written");
////                }
//            }
            if (cached == null) {
                cached = myEmptyBuffers.poll();
                if (cached == null) {
                    cached = ByteBuffer.allocate(getRecordLength());
                } else {
                    cached.rewind();
                }
            } else {
                cached = cached.duplicate();
                cached.rewind();
            }
            return cached;
        }
     // we duplicate, so that position in the returned records are independend
        ByteBuffer retval = this.memoryMapped.duplicate();
        retval.position((int) (pos - memoryMappedFirstByte));
        return retval;
    }
    /**
     * Write back a record fetched via {@link #getRecordForReading(int)}
     * or via {@link #getRecordForWriting(int)}.
     * You need to call either this method or {@link #releaseRecord(ByteBuffer)}
     * for any record optained from these 2 functions.
     * @param aRecordBuffer the record. Rewound.
     * @param recordNr the record-number.
     * @throws IOException if we cannot write the record
     */
    public void writeRecord(final ByteBuffer aRecordBuffer, final long recordNr) throws IOException {
        writeRecord(aRecordBuffer, recordNr, true);
    }
        /**
         * Write back a record fetched via {@link #getRecordForReading(int)}
         * or via {@link #getRecordForWriting(int)}.
         * You need to call either this method or {@link #releaseRecord(ByteBuffer)}
         * for any record optained from these 2 functions.
         * @param aRecordBuffer the record. Rewound.
         * @param recordNr the record-number.
         * @param sync sync after writing
         * @throws IOException if we cannot write the record
         */
        public void writeRecord(final ByteBuffer aRecordBuffer, final long recordNr, final boolean sync) throws IOException {
            if (aRecordBuffer instanceof MappedByteBuffer) {
                //we need not do anything due to memory-mapping
                return;
            }
            if (aRecordBuffer.remaining() < getRecordLength()) {
                throw new IllegalArgumentException("given record is not rewound!");
            }
          writeRecordInternal(aRecordBuffer, recordNr, sync);


////            if (sync) {
////                writeRecordInternal(aRecordBuffer, recordNr, sync);
////            } else {
////                this.myRecordBufferCache.put(recordNr, aRecordBuffer);
//                synchronized (this.myDirtyRecordCache) {
////                    LOG.warning("writeRecord() scheduling record " + recordNr + " to be written");
//                    this.myDirtyRecordCache.put(recordNr, aRecordBuffer);
//                    // if the write-cache gets too full, don`t return until it has shrunk
//                    while (this.myDirtyRecordCache.size() > 16385) {
//                        LOG.warning("writeRecord() schedule for records to be written is full, waiting for it to shrink");
//                        try {
//                            wakeupWriteThread();
//                            this.myDirtyRecordCache.wait();
////                            Thread.currentThread().sleep(50);
//                        } catch (InterruptedException e) {
//                            // ignored
//                        }
//                    }
//                }
//                wakeupWriteThread();
////            }
        }

//        /**
//         * Wakeup or create a worker-thread that writes dirty records to
//         * disk.
//         */
//        private void wakeupWriteThread() {
//            if (myAsyncWriteThread == null) {
//                myAsyncWriteThread = new Thread() {
//
//                    /* (non-Javadoc)
//                     * @see java.lang.Thread#run()
//                     */
//                    @Override
//                    public void run() {
//                        long lastKey = -1;
//                        while (!isInterrupted()) {
//                            try {
//                                long key = -1;
//                                ByteBuffer byteBuffer = null;
//                                synchronized (myDirtyRecordCache) {
//                                    // try to use write in order to avoid repositioning in #writeInternal
//                                    key = lastKey + 1;
//                                    byteBuffer = myDirtyRecordCache.remove(key);
//
//                                    if (byteBuffer == null) {
//                                        // if the next record is no in the list, use any record
//
//                                        Map.Entry<Long, ByteBuffer> entry = null;
//                                        Iterator<Map.Entry<Long, ByteBuffer>> iterator = myDirtyRecordCache.entrySet().iterator();
//                                        if (!iterator.hasNext()) {
//                                            // if there is no record to be written at all, wait for one
////                                        LOG.warning("myAsyncWriteThread: nothing to write");
//                                            myDirtyRecordCache.notifyAll();
//                                            myDirtyRecordCache.wait();
//                                            continue;
//                                        }
//                                        entry = iterator.next();
//                                        key = entry.getKey();
//                                        byteBuffer = entry.getValue();
//                                        iterator.remove();
//                                    }
////                                    LOG.warning("myAsyncWriteThread: writing record " + entry.getKey());
//                                    int queueSize = myDirtyRecordCache.size();
//                                    if (queueSize < 16385 && queueSize % 1000 == 0) {
//                                        myDirtyRecordCache.notifyAll();
//                                    }
//                                }
//                                writeRecordInternal(byteBuffer, key, false);
//                                lastKey = key;
//                            } catch (InterruptedException e) {
//                                if (myDirtyRecordCache.isEmpty()) {
//                                    break;
//                                }
//                            } catch (Exception e) {
//                                LOG.log(Level.SEVERE, "cannot write record in AsyncWriteThread", e);
//                            }
//                        }
//                        LOG.warning("myAsyncWriteThread: as interrupted - shutting down");
//                    }
//                };
//                myAsyncWriteThread.start();
//            }
//            synchronized (this.myDirtyRecordCache) {
//                this.myDirtyRecordCache.notifyAll();
//            }
//        }
//
//        /**
//         * @see #wakeupWriteThread();
//         */
//        private Thread myAsyncWriteThread = null;

        /**
         * Write back a record fetched via {@link #getRecordForReading(int)}
         * or via {@link #getRecordForWriting(int)}.
         * You need to call either this method or {@link #releaseRecord(ByteBuffer)}
         * for any record optained from these 2 functions.
         * @param aRecordBuffer the record. Rewound.
         * @param recordNr the record-number.
         * @param sync sync after writing
         * @throws IOException if we cannot write the record
         */
        private void writeRecordInternal(final ByteBuffer aRecordBuffer, final long recordNr, final boolean sync) throws IOException {

            this.myRecordBufferCache.put(recordNr, aRecordBuffer);

            try {
                synchronized (this.myFileChannel) {
                    aRecordBuffer.rewind();
                    //write back this buffer
                    long newPosition = recordNr * getRecordLength();
                    if (this.myFileChannel == null || !this.myFileChannel.isOpen()) {
                        this.myFileChannel = new RandomAccessFile(getFileName(), "rw").getChannel();
                    }
                    if (this.myFileChannel.position() != newPosition) {
                        this.myFileChannel.position(newPosition);
                    }
                    int written = this.myFileChannel.write(aRecordBuffer);
                    while (written != getRecordLength()) {
                        LOG.log(Level.INFO, "incomplete write! only " + written
                                + " bytes of " + getRecordLength()
                                + " bytes. writing remaining bytes...");
                        written += this.myFileChannel.write(aRecordBuffer);
                    }
//                if (sync) {
                        //this.myFileChannel.force(false);
//                }
                }
//            this.myRecordBufferCache.put(recordNr, aRecordBuffer);
                releaseRecord(aRecordBuffer);
            } catch (ClosedChannelException e) {
                LOG.log(Level.WARNING, "Channel closed while writing", e);
                this.myFileChannel = null;
                this.myFileChannel = new RandomAccessFile(getFileName(), "rw").getChannel();
                writeRecordInternal(aRecordBuffer, recordNr, sync);
            }
        }

        /**
         * Release a record fetched via {@link #getRecordForReading(int)}
         * or via {@link #getRecordForWriting(int)}.
         * You need to call either this method or {@link #releaseRecord(ByteBuffer)}
         * for any record optained from these 2 functions.
         * @param aRecordBuffer the record. Not rewound. (may be null)
     */
    public void releaseRecord(final ByteBuffer aRecordBuffer) {
        if (aRecordBuffer == null) {
            return;
        }
        if (aRecordBuffer instanceof MappedByteBuffer) {
            return;
        }
    }
    //------------------------ support for propertyChangeListeners ------------------

    /**
     * support for firing PropertyChangeEvents.
     * (gets initialized only if we really have listeners)
     */
    private volatile PropertyChangeSupport myPropertyChange = null;

    private FileChannel myFileChannel;

    /**
     * Returned value may be null if we never had listeners.
     * @return Our support for firing PropertyChangeEvents
     */
    protected PropertyChangeSupport getPropertyChangeSupport() {
        return myPropertyChange;
    }

    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     *
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property.  The listener
     * will be invoked only when a call on firePropertyChange names that
     * specific property.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(final String propertyName,
                                                final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The PropertyChangeListener to be removed
     */
    public final void removePropertyChangeListener(final String propertyName,
                                                   final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(propertyName,
                    listener);
        }
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     *
     * @param listener  The PropertyChangeListener to be removed
     */
    public synchronized void removePropertyChangeListener(final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(listener);
        }
    }

    //-------------------------------------------------------

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "FixedRecordFile@" + hashCode();
    }

    /**
     * Use {@link #getRecordForReading(int)} and {@link #getRecordForWriting(int)} instead.
     * @return Returns the memoryMapped.
     * @see #memoryMapped
     */
    @Deprecated
    protected ByteBuffer getMemoryMapping() {
        return this.memoryMapped;
    }

    /**
     * clean up any io-ressources.
     * @throws IOException if we cannot close our file
     */
    public void close() throws IOException {
//        if (myAsyncWriteThread != null) {
//            try {
//                myAsyncWriteThread.interrupt();
//            } catch (Exception e) {
//                LOG.severe("cannot interrupt AsyncWriteThread");
//            }
//        }
        if (this.memoryMapped != null) {
            this.memoryMapped = null;
        }
        if (this.myFileChannel != null) {
            this.myFileChannel.close();
            this.myFileChannel = null;
        }
    }

    /**
     * @return the recordCount
     */
    public long getRecordCount() {
        return myRecordCount;
    }

    /**
     * Increase the file-length to accomodate aCount additional
     * records. Does NOT blank the new records.
     * This code may not do anything at all if the file grows automatically
     * by writing to it. Do not atempt to read records you have not
     * written yet.
     * @param aPreferedCount the number of new records to allocate
     * @param aMinimumCount the number of new records to allocate if aPreferedCount is to much
     * @throws IOException if we cannot grow the file
     * @return the number of records we grew
     */
    public long growFile(final long aPreferedCount, final long aMinimumCount) throws IOException {
        if (this.myFileChannel == null) {
            this.myFileChannel = new RandomAccessFile(getFileName(), "rw").getChannel();
        }
        // this works much better with -XX:MaxDirectMemorySize=512M
        WeakReference<MappedByteBuffer> isGarbageCollected = new WeakReference<MappedByteBuffer>(this.memoryMapped);
        this.memoryMapped = null;
        long start = System.currentTimeMillis();
        System.gc();
        System.runFinalization();
        int i = 2 * 2;
        LOG.info("Growing File - forced Garbage collection done after " + (System.currentTimeMillis() - start) + "ms...");
        while (isGarbageCollected.get() != null) {
            LOG.info("Growing File - memory-mapping not yet garbage-collected");
            System.gc();
            System.runFinalization();
            Thread.yield();
            try {
                final int milliseconds = 1000;
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                LOG.info("sleep interrupted while waiting for finalization");
            }
            i--;
            if (i == 0) {
                LOG.info("Growing File - memory-mapping not yet garbage-collected - GOING ON ANYWAY");
                break;
            }
        }
        isGarbageCollected = null;
        LOG.info("Growing File - old memory collected, mapping new memory...");
        try {
            // if memory-mapping the complete file failed the last time,
            // do not try again
            if (this.memoryMapped != null || this.memoryMappedFirstByte != 0) {
                try {
                    this.memoryMapped = this.myFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, (getRecordCount() + aPreferedCount) * getRecordLength());
                    this.memoryMappedFirstByte = 0;
                    setRecordCount(getRecordCount() + (long) aPreferedCount);
                    return aPreferedCount;
                } catch (IOException e) {
                    LOG.log(Level.INFO, "could not map "
                            + (((long) getRecordCount() + (long) aPreferedCount)
                                    * getRecordLength())
                                    + " bytes into memory - "
                                    + "trying to map only a part of the file");
                } catch (IllegalArgumentException e) {
                    LOG.log(Level.INFO, "could not map "
                            + (((long) getRecordCount() + (long) aPreferedCount)
                                    * getRecordLength())
                                    + " bytes into memory - "
                                    + "trying to map only a part of the file");
                }
            }
            this.memoryMapped = null;
            this.memoryMappedFirstByte = getRecordCount() * getRecordLength();
            this.memoryMapped = this.myFileChannel.map(FileChannel.MapMode.READ_WRITE, this.memoryMappedFirstByte, aPreferedCount * getRecordLength());
            setRecordCount(getRecordCount() + (long) aPreferedCount);
            return aPreferedCount;
        } catch (Exception x) {
            //if Memory-Mapping fails, make this.memoryMap=null and return. convention-IO -code will do the growing
            this.memoryMapped = null;
            LOG.log(Level.WARNING, "could not map "
                    + (((long) getRecordCount() + (long) aPreferedCount)
                            * getRecordLength())
                            + " bytes into memory - continuing with conventional IO");
        }

        //////////////////////////////////////////////////////////
        // memory mapping failed. The conventional IO-code
        // grows the file by writing 1 byte
        this.myFileChannel.position((getRecordCount() + aMinimumCount) * (long) getRecordLength() - 1);
        this.myFileChannel.write(ByteBuffer.allocate(1));
        setRecordCount(getRecordCount() + aMinimumCount);
        return aMinimumCount;
    }
    /**
     * @param aRecordCount the recordCount to set
     */
    private void setRecordCount(final long aRecordCount) {
        this.myRecordCount = aRecordCount;
    }

    /**
     * @return the fileName
     */
    public File getFileName() {
        return myFileName;
    }

    /**
     * @param aFileName the fileName to set
     */
    private void setFileName(final File aFileName) {
        myFileName = aFileName;
    }
}


