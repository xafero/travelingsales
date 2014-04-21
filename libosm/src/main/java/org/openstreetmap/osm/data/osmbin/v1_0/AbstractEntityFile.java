/**
 * AbstractEntityFile.java
 * created: 21.01.2009
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
package org.openstreetmap.osm.data.osmbin.v1_0;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.data.osmbin.AttrNames;
import org.openstreetmap.osm.data.osmbin.FixedRecordFile;
import org.openstreetmap.osm.data.osmbin.IDIndexFile;
import org.openstreetmap.osm.data.osmbin.IIDIndexFile;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * AbstractEntityFile.java<br/>
 * created: 21.01.2009 <br/>
 *<br/>
 * <b>This is the *.obm-file as described in
 * <a href="http://wiki.openstreetmap.org/index.php?title=User:MarcusWolschon/osmbin_draft#version_1.0">here</a></b><br/>
 * We assume that all record start with the element-id (long) stored as int
 * and that Integer.MIN_VALUE denoted an unused record.<br/>
 * We also allow reading and writing attributes of entities. For this we first
 * parse the taglist of the {@link Entity#getTagList()} and split
 * values for tags that are too long for a fixed size slot into
 * multiple slots. Then we store them in the format:<br/>
 * <ul>
 *  <li>(short) id of tag-name, or {@link Short#MIN_VALUE} for an empty slot or {@link Short#MIN_VALUE} + 1 for a continuation of the value in the last slot</li>
 *  <li>tag-characters in UTF16 padded to the length of the fixed-size-record with the Java-Char -value 0. This padding is removed uppon reading.</li>
 * </ul>
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:OsmbinClasses.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */

public abstract class AbstractEntityFile extends FixedRecordFile {

    /**
     * Default for {@link #myGrowExcessRecordsCount} unless
     * overridden in child-classes.
     */
    private static final int DEFAULTGROWEXCESSCOUNT = 255;

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(AbstractEntityFile.class
            .getName());

    /**
     * We pad attribute-values with this character
     * upon writing up to the size of the fixed-size
     * attribute-record. All occurrences of this character
     * are removed upon reading.
     */
    private static final char ILLEGALCHAR = 0;

    /**
     * The file containing an index by node-ID.
     */
    private IIDIndexFile myIndex;


    /**
     * The file attribute-names are stored in.
     */
    private AttrNames myAttrNamesFile;

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
     * If we need to grow the file,
     * create this may spare records.<br/>
     * Defaults to 255, may be changed in the constructor
     * of sub-classes.
     */
    protected int myGrowExcessRecordsCount = DEFAULTGROWEXCESSCOUNT;

    /**
     * @param aFileName the filename of the *.obm -file.
     * @param anAttrNamesFile The file attribute-names are stored in.
     * @param anIndex The file containing an index by node-ID.
     * @throws IOException if we cannot open the file
     */
    protected AbstractEntityFile(final File aFileName,
            final IIDIndexFile anIndex,
            final AttrNames anAttrNamesFile)
            throws IOException {
        super(aFileName);
        setAttrNamesFile(anAttrNamesFile);
        setIndex(anIndex);
        this.myLastRecordWritten = getRecordCount();
    }

    /**
     * Get the start-recordnumber of all elements in the file.
     * @return al iterator over all first records of elements.
     */
    protected Iterator<Long> getAllElements() {
        return new Iterator<Long>() {
            /**
             * Where we currently are in the file.
             */
            private long myCurrentRecord = 0;
            /**
             * The ID of the last element reat.
             */
            private long myCurrentElementID = -1;

            @Override
            public boolean hasNext() {
                long current = myCurrentRecord;
                long id = myCurrentElementID;
                if (next() > 0) {
                    myCurrentRecord = current;
                    myCurrentElementID = id;
                    return true;
                }
                return false;
            }

            @Override
            public Long next() {
                long current = myCurrentRecord;
                while (current < getRecordCount()) {
                    ByteBuffer record = null;
                    try {
                        record = getRecordForReading(current);
                        int id = record.getInt();
                        if (id != myCurrentElementID && id != Integer.MIN_VALUE) {
                            myCurrentElementID = id;
                            myCurrentRecord = current;
                            return current;
                        }
                        current++;
                    } catch (IOException x) {
                        LOG.log(Level.SEVERE, "Cannot iterate over all elements", x);
                    } finally {
                        if (record != null) {
                            releaseRecord(record);
                        }
                    }
                }
                return (long) Integer.MIN_VALUE;
            }

            @Override
            public void remove() {
                // ignored
            }
        };
    }
    /**
     * Find as a number of consecutive,
     * free record and return the index of the
     * first one.
     * @param aRequired the minimum number of free records required
     * @return the index of the first one
     * @throws IOException if we cannot grow the file
     */
    protected long findFreeRecords(final int aRequired) throws IOException {
        int freeCount = 0;
        long recordCount = getRecordCount();
        int i = myUsedRecordIndex.nextClearBit(0);
        if (!myUsedRecordIndex.get(0)) {
            i = 0;
        }
        outherloop:
        for (; i < recordCount; i++) {
            // Record i is known to be in used.
            // Skip until the next free record following it.
            if (myUsedRecordIndex.get(i)) {
                i = myUsedRecordIndex.nextClearBit(i);
            }
            // skip checks after the last element that could
            // possibly be non-empty
            if (i > myLastRecordWritten) {
                myLastRecordWritten = i + aRequired;
                if (i + aRequired < getRecordCount()) {
                    return i;
                }
                break outherloop; // grow the file
            }
            // check myUsedRecordIndex if there is even a chance of
            // enough free records
            for (int count = 1; count < aRequired - freeCount; count++) {
                if (myUsedRecordIndex.get(count + i)) {
                    // there cannot be enough free records after i,
                    // find the next free block o records
                    i += count;
                    i = myUsedRecordIndex.nextClearBit(i);
                    continue outherloop;
                }
            }

            // check if this record is indeed free
            int currentID;
            try {
                ByteBuffer mem = getRecordForReading(i);
                currentID = mem.getInt();
                releaseRecord(mem);
            } catch (IllegalStateException e) {
                if (e.getCause() != null && e.getCause() instanceof EOFException) {
                    LOG.severe("Encountered unexpected EOF in the supposed middle of the file"
                            + " at record " + i + " setting EOF to this location and growing.");
                    recordCount = i;
                    continue;
                } else {
                    throw e;
                }
            }
            if (currentID == Integer.MIN_VALUE) {
             // cache the info where the first free record is
                myUsedRecordIndex.clear(i);

                freeCount++;
                if (freeCount >= aRequired) {
                    return i - aRequired + 1;
                }
            } else {
                freeCount = 0;
                myUsedRecordIndex.set(i);
                i = myUsedRecordIndex.nextClearBit(i);
            }
        }

        // increase the file-length, blank the additional
        // records and create a new memory-mapping with the
        // new size
        long start =  System.currentTimeMillis();
        LOG.info("Growing Entity-File...");
        long grown = growFile(aRequired  - freeCount + myGrowExcessRecordsCount, aRequired - freeCount);
        LOG.info("Growing Entity-File...initializing new records");
        for (long j = recordCount; j < recordCount + grown; j++) {
            invalidateRecord(j);
        }
        final double msPerSecond = 1000.0;
        LOG.info("Growing Relations-File...done " + ((System.currentTimeMillis() - start) / msPerSecond) + " seconds");
        return recordCount - freeCount;
    }

    /**
     * Declare the record with the given index free for reuse.
     * @param anIndex the index to overwrite.
     * @throws IOException if we cannot write
     */
    private void invalidateRecord(final long anIndex) throws IOException {
        ByteBuffer mem = getRecordForWriting(anIndex);
        int start = mem.position();
        mem.putInt(Integer.MIN_VALUE);
        mem.position(start);
        writeRecord(mem, anIndex, false);

        LOG.finest(getFileName() + " - invalidating record " + anIndex);
        myUsedRecordIndex.clear((int) anIndex);
    }
    /**
     * Declare the record with the given index free for reuse.
     * @param aRecordNumber the record to overwrite.
     * @param aNodeID the nodeID this record should have. Else it's a critical error.
     * @throws IOException if we cannot write
     */
    protected void invalidateRecord(final long aRecordNumber, final long aNodeID) throws IOException {
        boolean assertionsOn = false;
        assert (assertionsOn = true);
        if (assertionsOn) {
            //this is  a safe version of invalidateRecord that first checks to not invalidate unexpected records
            ByteBuffer mem = getRecordForReading(aRecordNumber);
            int start = mem.position();
            int safetyTest = mem.getInt();
            mem.position(start);
            releaseRecord(mem);
            mem = null;
            if (safetyTest != Integer.MIN_VALUE && safetyTest != aNodeID) {
                throw new IllegalArgumentException("Major internal error! "
                        + "NOT invalidating record "
                        + aRecordNumber + " that is still in use by element "
                        + safetyTest + " instead of element "
                        + aNodeID);
            }
        }
        invalidateRecord(aRecordNumber);
        this.myUsedRecordIndex.clear((int) aRecordNumber);
    }


    /**
     * Mark all records of the given node as free.
     * @param anElementID the id of the element to remove from storage
     * @throws IOException if the way cannot be removed from the index
     * @return the first record-number formerly occupied
     */
    protected long removeElement(final long anElementID) throws IOException {
        long recordNr = findRecordForElement(anElementID);
        LOG.finest(getFileName() + " - removing element " + anElementID + " record " + recordNr);
        int used     = getUsedRecordCount(recordNr, anElementID);

        // remove from the index first
        getIndex().remove(anElementID);
        // invalidate records and find a new place
        for (long i = recordNr; i< recordNr + used; i++) {
            LOG.finest(getFileName() + " - removing element " + anElementID + " - part " + (i - recordNr) + " record " + i);
            invalidateRecord(i, anElementID);
        }

        // in case another thread accessed the way
        // while we where deleting it, make sure it is
        // removed from the index.
        getIndex().remove(anElementID);
        assert getIndex().get(anElementID) < 0;

        return recordNr;
    }

    /**
     * Read one slot reserved for storing an attribute.
     * @param mem the buffer to read from, position at the first byte of the attribute-entry.
     * @param aCharCount one entry has space for this many characters
     * @param aTagList the tags reat so far. List must be modifiable to support long attribute-values
     *        spread across multiple attribute-records.
     * @return a tag or null
     */
    protected Tag readAttribute(final ByteBuffer mem,
            final int aCharCount,
            final List<Tag> aTagList) {
        char[] tagValue = new char[aCharCount];
        short tagKeyID = mem.getShort();
        for (int i = 0; i < aCharCount; i++) {
            tagValue[i] = mem.getChar();
        }

        String value = new String(tagValue).replaceAll("" + ILLEGALCHAR, "");
        if (tagKeyID == (Short.MIN_VALUE + 1)) {
            // this is the continuation of the last tag
            if (aTagList.size() == 0) {
                throw new IllegalStateException("First tag of an entity cannot be a continuation!");
            }
            Tag oldTag = aTagList.remove(aTagList.size() - 1);
            return new Tag(oldTag.getKey(), oldTag.getValue() + value.replaceAll("" + ILLEGALCHAR, ""));
        }
        if (tagKeyID != Short.MIN_VALUE) {
            String tagKey = getAttrNamesFile().getAttributeName(tagKeyID);
            if (tagKey != null && value != null) {
                return new Tag(tagKey, value);
            }
        }
        return null;
    }

    /**
     * Write a pararedTag into the given ByteBuffer.
     * @param mem where to write to
     * @param aCharCount length of the value
     * @param aPreparedTag what to write
     * @throws IOException if we cannot write
     */
    protected void writeAttribute(final ByteBuffer mem,
            final int aCharCount,
            final Object aPreparedTag) throws IOException {

        Tag aTag = (Tag) aPreparedTag;
        // store key
        if (aTag == null) {
            mem.putShort((short) Short.MIN_VALUE);
        } else if (aTag.getKey() == null) {
            // this is the continuation of a value
            // that was too long
            mem.putShort((short) (Short.MIN_VALUE + 1));
        } else {
            short attrNameKey = getAttrNamesFile().getOrCreateKey(aTag.getKey());
            //newlines,... may change assert getAttrNamesFile().getAttributeName(attrNameKey).equalsIgnoreCase(aTag.getKey());
            mem.putShort(attrNameKey);
        }

        // store value
        if (aTag == null) {
            for (int c = 0; c < aCharCount; c++) {
                mem.putChar(' ');
            }
        } else if (aTag.getValue().length() > aCharCount) {
            // this can no longer happen
            LOG.log(Level.SEVERE, "This should not happen. A tag-value was longer then charcount!");
        } else {
            String value = aTag.getValue();
            for (int c = 0; c < value.length(); c++) {
                mem.putChar(value.charAt(c));
            }
            for (int c = value.length(); c < aCharCount; c++) {
                mem.putChar(ILLEGALCHAR);
            }
        }
    }

    /**
     * @param firstRecordNr the first of the records that this element is stored in
     * @param anElementID if of the expected element (for safety-tests)
     * @return the number of consecutive records this node uses
     * @throws IOException if we cannot read the nodes
     */
    public int getUsedRecordCount(final long firstRecordNr, final long anElementID) throws IOException {
        if (firstRecordNr < 0) {
            return 0; // illegal record
        }
        int elementID = (int) anElementID;
        ByteBuffer mem = null;
        for (long i = firstRecordNr; i < getRecordCount(); i++) {
            int nextElementID;
            try {
                mem = getRecordForReading(i);
                nextElementID = mem.getInt();
            } finally {
                releaseRecord(mem);
            }
            if (nextElementID != elementID) {
                return (int) (i - firstRecordNr);
            }
        }
        return 1;
    }

    /**
     * Return the first of the consecutive record
     * where this node is stored.
     * @param anElementID the id of the element to look for
     * @return the record-number of the node or -1
     * @throws IOException if we cannot read the record-index
     */
    protected long findRecordForElement(final long anElementID) throws IOException {
        try {
            long recordNr = getIndex().get(anElementID);
            if (recordNr > getRecordCount()) {
                LOG.severe("Index contains a record-number beyond the end of the file for"
                        + " elementID=" + anElementID + " recordNumber in index="
                        + recordNr + " record count=" + getRecordCount());
                getIndex().remove(anElementID);
                LOG.finest(getFileName() + " - findRecordForElement " + anElementID + " - not found");
                return -1;
            }
            //LOG.finest(getFileName() + " - findRecordForElement " + anElementID + " record " + recordNr);
            return recordNr;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot access index.", e);
            //LOG.log(Level.SEVERE, "Cannot access index. Scanning all records.", e);
        }
        //return findRecordForElementFallback(aNodeID);
    }

    /**
     * Return the first of the consecutive record
     * where this node is stored.
     * This method SCANS the complete file.
     * It is only to be used if the index is broken
     * and needs to be repaired.
     * @param anElementID the node to look for
     * @return the record-number of the node or -1
     * @throws IOException if we cannot read the record-index
     */
    protected long findRecordForElementFallback(final long anElementID)
    throws IOException {
        for (long i = 0; i < getRecordCount(); i++) {
            ByteBuffer mem = getRecordForReading(i);
            long currentNodeID = mem.getInt();
            releaseRecord(mem);
            if (currentNodeID == anElementID) {
                LOG.log(Level.SEVERE, "Repairing broken index by updating existing element "
                        + anElementID + " to recordNumber " + i + " in *.obm");
                getIndex().put(anElementID, i);
                return i;
            }
        }
        LOG.log(Level.SEVERE, "Repairing broken index by removing non-existing element "
                + anElementID + " from index.");
        getIndex().remove(anElementID);
        return -1; // not found
    }

    /**
     * @param  record a buffer containing the record's content
     * @return true if the currently selected record is free for reuse.
     */
    public boolean isRecordUnused(final ByteBuffer record) {
        return record.getInt() == Integer.MIN_VALUE;
    }

    /**
     * @param aIndex The file containing an index by node-ID.
     */
    protected void setIndex(final IDIndexFile aIndex) {
        myIndex = aIndex;
    }

    /**
     * @param aIndex the index to set
     */
    public void setIndex(final IIDIndexFile aIndex) {
        myIndex = aIndex;
    }

    /**
     * @return the attrNamesFile
     */
    protected AttrNames getAttrNamesFile() {
        return myAttrNamesFile;
    }

    /**
     * @param aAttrNamesFile the attrNamesFile to set
     */
    protected void setAttrNamesFile(final AttrNames aAttrNamesFile) {
        myAttrNamesFile = aAttrNamesFile;
    }

    /**
     * @return the index
     */
    protected IIDIndexFile getIndex() {
        return myIndex;
    }

    /**
     * Prepare the list of tags of an object for writing
     * by splitting it into a list of objects that can be written
     * into slots fit for at most aNumvalchars characters.
     * @param aTagList the taglist to work on
     * @param aNumtagvalchars the maximum number of chars to store in one attribute-entry
     * @return objects fit for {@link #writeAttribute(ByteBuffer, int, Object)}
     */
    public List<Object> prepareTagList(final Collection<Tag> aTagList, final int aNumtagvalchars) {
        final List<Object> retval = new ArrayList<Object>(aTagList.size());
        for (Tag input : aTagList) {
            retval.addAll(prepareTag(input, aNumtagvalchars));
        }
        return retval;
    }

    /**
     * Recursively work on a single tag for {@link #prepareTagList(List, int)}.
     * @param aInput the tag to work on
     * @param aNumtagvalchars the maximum number of chars to store in one attribute-entry
     * @return the objects it is split into
     */
    private Collection<Object> prepareTag(final Tag aInput, final int aNumtagvalchars) {
        String value = aInput.getValue();
        if (value.length() <= aNumtagvalchars) {
            LinkedList<Object> retval = new LinkedList<Object>();
            retval.add(aInput);
            return retval;
        }
        String left = value.substring(0, value.length() - aNumtagvalchars);
        Collection<Object> retval = prepareTag(new Tag(aInput.getKey(), left), aNumtagvalchars);
        String right = value.substring(value.length() - aNumtagvalchars, value.length());
        retval.add(new Tag(null, right));
        return retval;
    }
}
