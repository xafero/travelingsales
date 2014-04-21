/**
 * WaysFile.java
 * created: 02.01.2009
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


//automatically created logger for debug and error -output
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.openstreetmap.osm.data.osmbin.AttrNames;
import org.openstreetmap.osm.data.osmbin.IDIndexFile;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.util.FixedPrecisionCoordinateConvertor;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * WaysFile.java<br/>
 * created: 02.01.2009 <br/>
 *<br/>
 * <b>This is the ways.obm-file as described in
 * <a href="http://wiki.openstreetmap.org/index.php?title=User:MarcusWolschon/osmbin_draft#version_1.0">here</a></b>
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:OsmbinClasses.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class WaysFile extends AbstractEntityFile {

    /**
     * Tag-values of at most this many characters are
     * written directly into a record. Longer values
     * are stored externally.
     */
    private static final int NUMTAGVALCHARS = 16;

    /**
     * This many slots to store a single
     * tag-value exists in a single record.
     * If more are required, additional records
     * are used to store the way.
     */
    private static final int ATTRCOUNTPERRECORD = 1;

    /**
     * This many slots to store a single
     * node-reference in a single record.
     * If more are required, additional records
     * are used to store the way.
     */
    private static final int NODEREFCOUNTPERRECORD = 14;
    /**
     * This many slots to store a single
     * relation-reference in a single record.
     * If more are required, additional records
     * are used to store the way.
     */
    private static final int RELATIONREFCOUNTPERRECORD = 1;

    /**
     * If we need to grow the file,
     * create this may spare records.
     */
    private static final int GROWEXCESSRECORDS = 20000;

    /**
     * @param aFileName the filename of the ways.obm -file.
     * @param anAttrNamesFile The file attribute-names are stored in.
     * @param anIndex The file containing an index by node-ID.
     * @throws IOException if we cannot open or create the file.
     */
    public WaysFile(final File aFileName,
            final AttrNames anAttrNamesFile,
            final IDIndexFile anIndex) throws IOException {
        super(aFileName, anIndex, anAttrNamesFile);
        super.myGrowExcessRecordsCount = GROWEXCESSRECORDS;
    }

    /**
     * Run this as a program to analyse a file
     * and provide statistics for development.
     * @param args Usage: FILENAME.obm attrnames.txt FILENAME.idx
     */
    public static void main(final String[] args) {
        /*final int argsCount = 3;
        if (args.length != argsCount) {
            System.out.println("This program provides some statistics on a generated file\n"
                    + "Usage: java NodesFile DIRECTORY - where DIRECTORY contains nodes.obm attrnames.txt nodes.idx");
            return;
        }*/
        try {
            File dir = new  File("e:\\osmbin.data"); //DEBUG args[0]);
            File file = new File(dir, "ways.obm");
            WaysFile subject = new WaysFile(file, new AttrNames(new File(dir, "attrnames.txt")), new IDIndexFile(new File(dir, "ways.idx")));
            System.out.println("Number of records in obm-file: " + subject.getRecordCount() + " = " + (subject.getRecordCount() * subject.getRecordLength()) + " bytes");
            System.out.println("Lengt of each record in obm-file: " + subject.getRecordLength());
            int unusedRecordCount = 0;
            int tmpConsecutiveEmptyRecords = 0;
            int tmpCurrentEntryLength = 0;
            int tmpCurrentTagCount = 0;
            int tmpCurrentNodeCount = 0;
            int tmpLastWayID = Integer.MIN_VALUE;
            final int emptyBlocksHistogramSize = 4;
            int[] emptyBlocksHistogram = new int[emptyBlocksHistogramSize];
            final int entryLengthHistogramSize = 18;
            int[] entryLengthHistogram = new int[entryLengthHistogramSize];
            final int entryTagCountHistogramSize = 24;
            int[] entryTagCountHistogram = new int[entryTagCountHistogramSize];
            int[] entryTagValueLengthHistogram = new int[NUMTAGVALCHARS + 1];
            int numTagValues = 0;
            int numExternalTagValues = 0;
            final int entryNodeCountHistogramSize = 128;
            int[] entryNodeCountHistogram = new int[entryNodeCountHistogramSize];
            Set<Integer> debug = new HashSet<Integer>();
            int[] emptyTagSlotHistogram = new int[ATTRCOUNTPERRECORD + 1];
            int[] emptyNodeSlotHistogram = new int[NODEREFCOUNTPERRECORD + 1];
            ByteBuffer mem = subject.getMemoryMapping();

            for (int recNR = 0; recNR < subject.getRecordCount(); recNR++) {
                subject.getRecordForReading(recNR);
                int id = mem.getInt();
                if (tmpLastWayID == id) {
                    tmpCurrentEntryLength++;
                } else {
                 // create a histogram of empty block-sizes in the file
                    if (tmpCurrentEntryLength > 0) {
                        if (tmpCurrentEntryLength >= entryLengthHistogram.length) {
                            entryLengthHistogram[entryLengthHistogram.length - 1]++;
                        } else {
                            entryLengthHistogram[tmpCurrentEntryLength]++;
                        }
                    }
                    // histogram of tags/node
                    if (tmpCurrentTagCount >= entryTagCountHistogram.length) {
                        entryTagCountHistogram[entryTagCountHistogram.length - 1]++;
                    } else {
                        entryTagCountHistogram[tmpCurrentTagCount]++;
                    }
                    // histogram of ways/node
                    if (tmpCurrentNodeCount >= entryNodeCountHistogram.length) {
                        entryNodeCountHistogram[entryNodeCountHistogram.length - 1]++;
                    } else {
                        entryNodeCountHistogram[tmpCurrentNodeCount]++;
                    }
                    tmpCurrentEntryLength = 1;
                    tmpCurrentTagCount = 0;
                    tmpCurrentNodeCount = 0;
                    debug.clear();
                }

                if (id == Integer.MIN_VALUE) {
                    // unused record
                    unusedRecordCount++;
                    tmpConsecutiveEmptyRecords++;
                    tmpLastWayID = id;
                    continue;
                }
                tmpConsecutiveEmptyRecords = 0; // this record is not empty
                //int version =
                    mem.getInt(); // skip version
                //int lat =
                    mem.getInt(); // skip lat
                //int lon =
                    mem.getInt(); // skip lon
                //int lat2 =
                    mem.getInt(); // skip lat
                //int lon2 =
                    mem.getInt(); // skip lon
//                System.out.println("Record " + recNR + " id=" + id
//                        + " lat=" + FixedPrecisionCoordinateConvertor.convertToDouble(lat) + " lon=" + FixedPrecisionCoordinateConvertor.convertToDouble(lon));


                int tmpCountEmptyTagSlots = 0;
                for (int a = 0; a < ATTRCOUNTPERRECORD; a++) {
                    short tagKeyID = mem.getShort();
                    char[] tagValue = new char[NUMTAGVALCHARS];
                    for (int c = 0; c < NUMTAGVALCHARS; c++) {
                        tagValue[c] = mem.getChar();
                    }

                    if (tagKeyID != Short.MIN_VALUE) {
                        numTagValues++;
                        // non-empty
                        if (tagValue[0] != '\n') {
  //                          System.out.println("record " + recNR + " slot" + a + ": " + subject.getAttrNamesFile().getAttributeName(tagKeyID) + "=" + new String(tagValue));
                            entryTagValueLengthHistogram[(new String(tagValue)).trim().length()]++;
                        } else {
                            numExternalTagValues++;
                        }
                        tmpCurrentTagCount++;
                    } else {
                        // empty tag-slot
                        tmpCountEmptyTagSlots++;
                //        System.out.println("record " + recNR + " slot" + a + " (should be spaces): '" + new String(tagValue) + "'");
                    }
                }

                emptyTagSlotHistogram[tmpCountEmptyTagSlots]++;

                // skip way-IDs
                int tmpCountEmptyNodeSlots = 0;
                for (int a = 0; a < NODEREFCOUNTPERRECORD; a++) {
                    int nodeID = mem.getInt();
                    if (nodeID != Integer.MIN_VALUE) {
                        // non-empty
                        tmpCurrentNodeCount++;
//                        if (debug.contains(nodeID)) {
//                            System.err.println("ERROR: node " + nodeID
//                                    + " contained in way " + id + " in slot " + a
//                                    + " (using at least " + tmpCurrentEntryLength + " records) multiple times");
//                        } else {
 //                           System.out.println("node " + nodeID
  //                                  + " contained in way " + id + " in slot " + a);
//                        }
                        debug.add(nodeID);
                    } else {
                        // empty way-slot
                        tmpCountEmptyNodeSlots++;
                    }
                }
                emptyNodeSlotHistogram[tmpCountEmptyNodeSlots]++;
                for (int a = 0; a < RELATIONREFCOUNTPERRECORD; a++) {
                    //int relID =
                        mem.getInt();
                }

                // create a histogram of empty block-sizes in the file
                if (tmpConsecutiveEmptyRecords > 0) {
                    if (tmpConsecutiveEmptyRecords >= emptyBlocksHistogram.length) {
                        emptyBlocksHistogram[emptyBlocksHistogram.length - 1]++;
                    } else {
                        emptyBlocksHistogram[tmpConsecutiveEmptyRecords]++;
                    }
                }

                tmpLastWayID = id;
            }

            ///////////////////////////
            // show statistics
            System.out.println("Slots for tags per record: " + ATTRCOUNTPERRECORD);
            System.out.println("Slots for nodes per record: " + NODEREFCOUNTPERRECORD);
            System.out.println("Number of free records: " + unusedRecordCount + " =" + (unusedRecordCount * subject.getRecordLength()) + " byte");
            for (int i = 0; i < emptyBlocksHistogram.length; i++) {
                System.out.print("blocks of " + i);
                if (i == emptyBlocksHistogram.length - 1) {
                    System.out.print(" or more");
                }
                System.out.println(" empty records: " + emptyBlocksHistogram[i] + " (last count includes all larger blocks)");
            }
            for (int i = 0; i < entryLengthHistogram.length; i++) {
                System.out.print("ways using " + i);
                if (i == entryLengthHistogram.length - 1) {
                    System.out.print(" or more");
                }
                System.out.println(" records: " + entryLengthHistogram[i]);
            }
            for (int i = 0; i < entryTagCountHistogram.length; i++) {
                System.out.print("ways with " + i);
                if (i == entryTagCountHistogram.length - 1) {
                    System.out.print(" or more");
                }
                System.out.println(" tags: " + entryTagCountHistogram[i]);
            }
            for (int i = 0; i < entryNodeCountHistogram.length; i++) {
                System.out.print("ways with " + i);
                if (i == entryNodeCountHistogram.length - 1) {
                    System.out.print(" or more");
                }
                System.out.println(" nodes: " + entryNodeCountHistogram[i]);
            }
            for (int i = 0; i < emptyTagSlotHistogram.length; i++) {
                System.out.println("records with " + i + " empty slots for tags: " + emptyTagSlotHistogram[i]);
            }
            for (int i = 0; i < emptyNodeSlotHistogram.length; i++) {
                System.out.println("records with " + i + " empty slots for nodes: " + emptyNodeSlotHistogram[i]);
            }
            for (int i = 0; i < entryTagValueLengthHistogram.length; i++) {
                System.out.println("tag-values of " + i + " characters: " + entryTagValueLengthHistogram[i] + " of " + numTagValues + " tag-values total");
            }
            System.out.println("tag-values of more then " + NUMTAGVALCHARS + " characters stored externaly: " + numExternalTagValues);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(WaysFile.class
            .getName());

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "WaysFile@" + hashCode();
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public int getRecordLength() {
        return getWayRecordLength();
    }

    /**
     * @return the length of a record in bytes.
     * @see FixedRecordFile
     */
    protected static int getWayRecordLength() {
        final int bytesPerShort = 2;
        final int bytesPerInt = 4;
        final int bytesPerChar = 2;
        return bytesPerInt //  ID
        + bytesPerInt // version
        + bytesPerInt + bytesPerInt // lat min/max
        + bytesPerInt + bytesPerInt // lon min/max
        + ATTRCOUNTPERRECORD * (bytesPerShort + bytesPerChar * NUMTAGVALCHARS)
        + (NODEREFCOUNTPERRECORD * bytesPerInt)
        + (RELATIONREFCOUNTPERRECORD * bytesPerInt);
    }


    /**
     * @param  record a buffer containing the record's content
     * @return true if the currently selected record is free for reuse.
     */
    public boolean isRecordUnused(final ByteBuffer record) {
        return record.getInt() == Integer.MIN_VALUE;
    }

//    public int getWayID() {
//        return getMemoryMapped().getInt();
//    }

    /**
     * @param aWay the way to examine
     * @return the required number of consecutive records to store this node
     */
    public int getRequiredRecordCount(final ExtendedWay aWay) {
        Collection<Tag> tagList = aWay.getTags();
        double count = (1.0 * tagList.size()) / ATTRCOUNTPERRECORD;
        count = Math.max(count, (1.0 * aWay.getWayNodes().size()) / NODEREFCOUNTPERRECORD);
        count = Math.max(count, (1.0 * aWay.getReferencedRelationIDs().size()) / RELATIONREFCOUNTPERRECORD);

        // at least 1 record is needed
        if (count == 0) {
            return 1;
        }
        return (int) Math.ceil(count);
    }

    /**
     * @return all ways in the file.
     */
    public Iterator<Way> getallWays() {
        return new Iterator<Way>() {
                private Iterator<Long> myWayIDs = getAllElements();
                private Way myLastWay;

                public boolean hasNext() {
                    return myWayIDs.hasNext();
                }

                public Way next() {
                    try {
                        myLastWay = readWay(Integer.MIN_VALUE, myWayIDs.next());
                        return myLastWay;
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException("UnsupportedEncodingException while fetchine next way", e);
                    } catch (IOException e) {
                        throw new IllegalStateException("IOException while fetchine next way", e);
                    }
                }

                public void remove() {
                    try {
                        removeWay(myLastWay);
                    } catch (IOException e) {
                        throw new IllegalStateException("cannot remove way", e);
                    }
                }

        };
    }


    /**
     * Write the given way.
     * If it is already stored in the ways-File,
     * the old records are overwritten. If we require
     * more or less records then before, allocation and
     * deallocation are done here too.
     * @param aWay the node to store
     * @throws IOException on problems growing the file
     */
    public void writeWay(final ExtendedWay aWay) throws IOException {
        long recordNr = findRecordForWay(aWay.getId());
        writeWay(aWay, recordNr);
    }

    /**
     * Write the given way.
     * If it is already stored in the ways-File,
     * the old records are overwritten. If we require
     * more or less records then before, allocation and
     * deallocation are done here too.
     * @param aWay the node to store
     * @param aRecordNr the record -number of the first record that stored this way
     * @throws IOException on problems growing the file
     */
    public void writeWay(final ExtendedWay aWay, final long aRecordNr) throws IOException {
        long recordNr = aRecordNr;
        int required = getRequiredRecordCount(aWay);
        int used     = getUsedRecordCount(recordNr, aWay.getId());
        List<Object> preparedTagList = super.prepareTagList(aWay.getTags(), NUMTAGVALCHARS);
        required = Math.max(required, preparedTagList.size() / ATTRCOUNTPERRECORD);


        if (required > used) {
            // invalidate old records and find a new place
            for (long i = recordNr; i< recordNr + used; i++) {
                invalidateRecord(i, aWay.getId());
            }
            recordNr = findFreeRecords(required);
            getIndex().put(aWay.getId(), recordNr);
            used = required;
        }
        if (required < used) {
            //invalidate excess records
            for (long i = recordNr + required; i < recordNr + used; i++) {
                invalidateRecord(i, aWay.getId());
            }
        }
        List<Long> relations = new ArrayList<Long>(aWay.getReferencedRelationIDs());
        try {
            for (int i = 0; i < required; i++) {
                writeOndWayRecord(aWay, i, recordNr + i, relations, preparedTagList);
                myUsedRecordIndex.set((int) (recordNr + i));
                if (recordNr + i > myLastRecordWritten) {
                    myLastRecordWritten = recordNr + i;
                }
            }
        } catch (RecordInUseException e) {
            LOG.log(Level.SEVERE, "The record is already in use!!!"
                    + "Writing this way in some free space instead.", e);
            myUsedRecordIndex.set((int) e.getRecordNumber());

            recordNr = findFreeRecords(required);
            getIndex().put(aWay.getId(), recordNr);
            used = required;

            for (int i = 0; i < required; i++) {
                writeOndWayRecord(aWay, i, recordNr + i, relations, preparedTagList);
                myUsedRecordIndex.set((int) (recordNr + i));
                if (recordNr + i > myLastRecordWritten) {
                    myLastRecordWritten = recordNr + i;
                }
            }
        }
    }

    /**
     * Return the first of the consecutive record
     * where this node is stored.
     * @param aWayID the node to look for
     * @return the record-number of the node or -1
     * @throws IOException if we cannot read the records
     */
    public long findRecordForWay(final long aWayID) throws IOException {

        return super.findRecordForElement(aWayID);
    }


    /**
     * Mark all records of the given node as free.
     * @param aWay the node to remove from storage
     * @throws IOException if the way cannot be removed from the index
     */
    public void removeWay(final Way aWay) throws IOException {
        removeElement(aWay.getId());
    }

    /**
     * Write the given way into the current and following records.
     * @param aWay the node to write
     * @param aRecordNumber the number of the record to write to
     * @param counter this many records have been written already
     * @param relations the back-references to the relations containing this way
     * @param aPreparedTagList the key=value -pairs to attach
     * @throws IOException  if an attribute-key cannot be created
     */
    private void writeOndWayRecord(final ExtendedWay aWay,
            final int counter,
            final long aRecordNumber,
            final List<Long> relations,
            final List<Object> aPreparedTagList) throws IOException  {
        if (aWay.getId() > Integer.MAX_VALUE) {
            throw new IllegalStateException("wayID too large to be represented as an integer");
        }
        ByteBuffer mem = getRecordForReading(aRecordNumber);
        if (mem != null) {
            int start = mem.position();
            // assertions are enabled, do safety-checks to not overwrite
            // a record already used by another node
            int safetyTest = mem.getInt();
            mem.position(start);
            if (safetyTest != Integer.MIN_VALUE && safetyTest != aWay.getId()) {
                releaseRecord(mem);
                mem = null;
                throw new RecordInUseException(aRecordNumber, "Major internal error! "
                        + "NOT overwriting record "
                        + aRecordNumber + " that is still in use by way "
                        + safetyTest + " with record " + counter + " of way "
                        + aWay.getId());
            }
        }
        int start = mem.position();
        if (mem.limit() - start < getRecordLength()) {
            throw new IllegalStateException("Internal error!"
                    + " mem.limit()=" + mem.limit()
                    + " mem.position()=" + mem.position()
                    + " but recordSize=" + getRecordLength()
                    + " mem = " + mem.getClass().getName());
        }
        //LOG.finest(getFileName() + " - way " + aWay.getId() + " - part " + counter + " writing into record " + aRecordNumber + " (mem.positiion()=" + mem.position() + ")");
        mem.putInt((int) aWay.getId());
        mem.putInt(aWay.getVersion());
        mem.putInt(FixedPrecisionCoordinateConvertor.convertToFixed(aWay.getMinLatitude()));
        mem.putInt(FixedPrecisionCoordinateConvertor.convertToFixed(aWay.getMaxLatitude()));
        mem.putInt(FixedPrecisionCoordinateConvertor.convertToFixed(aWay.getMinLongitude()));
        mem.putInt(FixedPrecisionCoordinateConvertor.convertToFixed(aWay.getMaxLongitude()));

        // write 4 Attributes
        for (int a = 0; a < ATTRCOUNTPERRECORD; a++) {
            Object tag = null;
            int indexIntoTaglist = a + (ATTRCOUNTPERRECORD * counter);
            if (aPreparedTagList.size() > indexIntoTaglist) {
                tag = aPreparedTagList.get(indexIntoTaglist);
            }
            writeAttribute(mem, NUMTAGVALCHARS, tag);
        }
        // write 4 ways
        for (int w = 0; w < NODEREFCOUNTPERRECORD; w++) {
            int nodeID = Integer.MIN_VALUE;
            int indexIntoWaylist = w + (NODEREFCOUNTPERRECORD * counter);
            if (aWay.getWayNodes().size() > indexIntoWaylist) {
                long nodeIdLong = aWay.getWayNodes().get(indexIntoWaylist).getNodeId();
                if (nodeIdLong > Integer.MAX_VALUE) {
                    throw new IllegalStateException("nodeID too large to be represented as an integer");
                }
                nodeID = (int) nodeIdLong;
            }
            mem.putInt(nodeID);
        }
     // write 1 relations
        for (int r = 0; r < RELATIONREFCOUNTPERRECORD; r++) {
            int relID = Integer.MIN_VALUE;
            int indexIntoRellist = r + (RELATIONREFCOUNTPERRECORD * counter);
            if (relations.size() > indexIntoRellist) {
                long relIdLong = relations.get(indexIntoRellist);
                if (relIdLong > Integer.MAX_VALUE) {
                    throw new IllegalStateException("relationID too large to be represented as an integer");
                }
                relID = (int) relIdLong;
            }
            mem.putInt(relID);
        }
        mem.position(start);
        writeRecord(mem, aRecordNumber);
    }

    /**
     * Read the way with the given id from storage.
     * @param aWayID the way to read
     * @return null if it is not present.
     * @throws IOException if we cannot read an external attribute
     */
    public ExtendedWay readWay(final long aWayID) throws IOException {
        long recordNr = findRecordForWay(aWayID);
        return readWay(aWayID, recordNr);
    }

    /**
     * Read the way with the given id from storage.
     * @param aRecordNr the record -number of the first record storing this way
     * @param aWayID the way to read
     * @return null if it is not present.
     * @throws IOException if we cannot read an external attribute
     */
    public ExtendedWay readWay(final long aWayID, final long aRecordNr) throws IOException {
        long recordNr = aRecordNr;
        if (recordNr < 0) {
            return null;
        }
        ByteBuffer mem = getRecordForReading(recordNr);
        int id = mem.getInt();
        if (id != aWayID) {
            releaseRecord(mem);
            long origRecord = recordNr;
            recordNr = findRecordForElementFallback(aWayID);
            if (recordNr < 0) {
                getIndex().remove(aWayID);
                LOG.severe("record "
                        +  origRecord + " for way "
                        + aWayID + " stores way "
                        + id + " instead. Scanning all the file we noticed that"
                        + " we do not have that way at all!");
                return null;
            }
            mem = getRecordForReading(recordNr);
            id = mem.getInt();
            if (id != aWayID) {
                throw new IllegalStateException("record "
                        +  recordNr + " for way "
                        + aWayID + " stores way "
                        + id + " instead. Originally given record "
                        + origRecord + " was also wrong");
            } else {
                LOG.severe("record "
                        +  origRecord + " for way "
                        + aWayID + " stores way "
                        + id + " instead. It should have been record "
                        + recordNr + " instead");
                getIndex().put(aWayID, recordNr);
            }
        }
        //LOG.finest(getFileName() + " - way " + aWayID + " - reading from record " + recordNr + " (mem.positiion()=" + mem.position() + ")");
        int version = mem.getInt();
        double minLat = FixedPrecisionCoordinateConvertor.convertToDouble(mem.getInt());
        double maxLat = FixedPrecisionCoordinateConvertor.convertToDouble(mem.getInt());
        double minLon = FixedPrecisionCoordinateConvertor.convertToDouble(mem.getInt());
        double maxLon = FixedPrecisionCoordinateConvertor.convertToDouble(mem.getInt());
        // read attributes
        int recordCount = 0;
        List<Integer> referencedRelations = new LinkedList<Integer>();
        List<Tag> tagList = new LinkedList<Tag>();
        List<WayNode> wayNodes = new LinkedList<WayNode>();
        while (true) {
            // read attributes of current record
            for (int a = 0; a < ATTRCOUNTPERRECORD; a++) {
                Tag tag = null;
                tag = readAttribute(mem, NUMTAGVALCHARS, tagList);
                if (tag != null) {
                    tagList.add(tag);
                }
            }

            // read node-IDs
            for (int a = 0; a < NODEREFCOUNTPERRECORD; a++) {
                int nodeID = mem.getInt();
                if (nodeID != Integer.MIN_VALUE) {
                    wayNodes.add(new WayNode(nodeID));
                }
            }
         // read relation-IDs
            for (int a = 0; a < RELATIONREFCOUNTPERRECORD; a++) {
                int relID = mem.getInt();
                if (relID != Integer.MIN_VALUE) {
                    referencedRelations.add(relID);
                }
            }

            // check if the next record is for the same node
            recordNr++;
            if (recordNr >= getRecordCount()) {
                break;
            }
            releaseRecord(mem);
            recordCount++;
            mem = getRecordForReading(recordNr);
            int temp = mem.getInt();
            if (temp != aWayID) {
                break;
            }
            temp = mem.getInt(); // skip version
            if (temp != version) {
                break;
            }
            //LOG.finest(getFileName() + " - way " + aWayID + " - part " + recordCount + " reading from record " + recordNr + " (mem.positiion()=" + mem.position() + ")");
            temp = mem.getInt(); // skip lat(min)
            temp = mem.getInt(); // skip lat(max)
            temp = mem.getInt(); // skip lon(min)
            temp = mem.getInt(); // skip lon(max)
        }
        ExtendedWay retval = new ExtendedWay(id, version, tagList, wayNodes);
        retval.setMinLatitude(minLat);
        retval.setMaxLatitude(maxLat);
        retval.setMinLongitude(minLon);
        retval.setMaxLongitude(maxLon);
        for (Integer relID : referencedRelations) {
            retval.addReferencedRelation(relID);
        }
        releaseRecord(mem);
        assert retval.getWayNodes() != null;
        return retval;
    }

}


