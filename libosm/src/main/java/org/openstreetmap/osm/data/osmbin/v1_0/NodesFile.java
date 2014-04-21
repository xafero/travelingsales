/**
 * NodesFile.java
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
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.data.osmbin.AttrNames;
import org.openstreetmap.osm.data.osmbin.IDIndexFile;
import org.openstreetmap.osm.data.osmbin.IGeoIndexFile;
import org.openstreetmap.osm.data.osmbin.IIDIndexFile;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.util.FixedPrecisionCoordinateConvertor;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * NodesFile.java<br/>
 * created: 02.01.2009<br/>
 *<br/>
 * <b>This is the nodes.obm-file as described in
 * <a href="http://wiki.openstreetmap.org/index.php?title=User:MarcusWolschon/osmbin_draft#version_1.0">here</a></b>
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:OsmbinClasses.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 * The NodeID in the record allows cleaning up and defragmenting the file. The byte-index into the file is stored in the attrvalue-slot prepended by a marker
 */
public class NodesFile extends AbstractEntityFile {

    /**
     * Tag-values of at most this many characters aer
     * written inline into a record. Longer values
     * are stored externally.
     */
    private static final int NUMTAGVALCHARS = 16;

    /**
     * This many slots to store a single
     * tag-value exists in a single record.
     * If more are required, additional records
     * are used to store the node.
     */
    private static final int ATTRCOUNTPERRECORD = 1;

    /**
     * This many slots to store a single
     * way-reference in a single record.
     * If more are required, additional records
     * are used to store the node.
     */
    private static final int WAYREFCOUNTPERRECORD = 3;
    /**
     * This many slots to store a single
     * relation-reference in a single record.
     * If more are required, additional records
     * are used to store the node.
     */
    private static final int RELATIONREFCOUNTPERRECORD = 1;

    /**
     * The file containing an index by node-location.
     */
    private IGeoIndexFile my2DIndex;

    /**
     * The default initial capacity - MUST be a power of two.
     * @see #myNodeCache
     */
    static final int DEFAULT_CACHE_INITIAL_CAPACITY = 16;

    /**
     * The load factor used when none specified in constructor.
     * @see #myNodeCache
     */
    static final float DEFAULT_CACHE_LOAD_FACTOR = 0.75f;


    /**
     * Cache for nodes to avoid loading them repeatedly.
     */
    private Map<Long, ExtendedNode> myNodeCache = Collections.synchronizedMap(
            new LinkedHashMap<Long, ExtendedNode>(DEFAULT_CACHE_INITIAL_CAPACITY, DEFAULT_CACHE_LOAD_FACTOR, true) {

                /**
                 * generated.
                 */
                private static final long serialVersionUID = -2064475227892362436L;
                /**
                 * Maximum size of this cache
                 */
                static final int MAXCACHESIZE = 64;
                /**
                 * {@inheritDoc}
                 */
                @Override
                protected boolean removeEldestEntry(final Map.Entry<Long, ExtendedNode> anEldestEntry) {
                    return size() > MAXCACHESIZE;
                }
            });

    /**
     * @param aFileName the filename of the nodes.obm -file.
     * @param anAttrNamesFile The file attribute-names are stored in.
     * @param anIndex The file containing an index by node-ID.
     * @param a2DIndex The file containing an index by node-location (may be null).
     * @throws IOException if we cannot open or create the file.
     */
    public NodesFile(final File aFileName,
                     final AttrNames anAttrNamesFile,
                     final IIDIndexFile anIndex,
                     final IGeoIndexFile a2DIndex) throws IOException {
        super(aFileName, anIndex, anAttrNamesFile);
        set2DIndex(a2DIndex);
        super.myGrowExcessRecordsCount = GROWEXCESSRECORDS;
    }

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(NodesFile.class
            .getName());

    /**
     * If we need to grow the file,
     * create this may spare records.
     */
    private static final int GROWEXCESSRECORDS = 60000;

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "NodesFile@" + hashCode();
    }

    /**
     * Run this as a program to analyse a file
     * and provide statistics for development.
     * @param args Usage: FILENAME.obm attrnames.txt FILENAME.idx
     */
    public static void main(final String[] args) {
        final int argsCount = 3;
        /*if (args.length != argsCount) {
            System.out.println("This program provides some statistics on a generated file\n"
                    + "Usage: java NodesFile DIRECTORY - where DIRECTORY contains nodes.obm attrnames.txt nodes.idx");
            return;
        }*/
        try {
            File dir = new  File("e:\\osmbin.data");//DEBUG args[0]);
            File file = new File(dir, "nodes.obm");
            NodesFile subject = new NodesFile(file, new AttrNames(new File(dir, "attrnames.txt")), new IDIndexFile(new File(dir, "ways.idx")), null);
            System.out.println("Number of records in obm-file: " + subject.getRecordCount() + " = " + (subject.getRecordCount() * subject.getRecordLength()) + " bytes");
            System.out.println("Lengt of each record in obm-file: " + subject.getRecordLength());
            int unusedRecordCount = 0;
            int tmpConsecutiveEmptyRecords = 0;
            int tmpCurrentEntryLength = 0;
            int tmpCurrentTagCount = 0;
            int tmpCurrentWayCount = 0;
            int tmpLastNodeID = Integer.MIN_VALUE;
            int[] emptyBlocksHistogram = new int[4];
            int[] entryLengthHistogram = new int[18];
            int[] entryTagCountHistogram = new int[24];
            int[] entryTagValueLengthHistogram = new int[NUMTAGVALCHARS];
            int numTagValues = 0;
            int numExternalTagValues = 0;
            int[] entryWayCountHistogram = new int[8];
            Set<Integer> debug = new HashSet<Integer>();
            int[] emptyTagSlotHistogram = new int[ATTRCOUNTPERRECORD + 1];
            int[] emptyWaySlotHistogram = new int[WAYREFCOUNTPERRECORD + 1];
            ByteBuffer mem = subject.getMemoryMapping();
            for (int recNR = 0; recNR < subject.getRecordCount(); recNR++) {
                subject.getRecordForReading(recNR);
                int id = mem.getInt();
                int version = mem.getInt(); // skip version
                if (tmpLastNodeID == id) {
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
                    if (tmpCurrentWayCount >= entryWayCountHistogram.length) {
                        entryWayCountHistogram[entryWayCountHistogram.length - 1]++;
                    } else {
                        entryWayCountHistogram[tmpCurrentWayCount]++;
                    }
                    tmpCurrentEntryLength = 1;
                    tmpCurrentTagCount = 0;
                    tmpCurrentWayCount = 0;
                    debug.clear();
                }

                if (id == Integer.MIN_VALUE) {
                    // unused record
                    unusedRecordCount++;
                    tmpConsecutiveEmptyRecords++;
                    tmpLastNodeID = id;
                    continue;
                }
                tmpConsecutiveEmptyRecords = 0; // this record is not empty
                int lat = mem.getInt(); // skip lat
                int lon = mem.getInt(); // skip lon
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
//                            System.out.println("record " + recNR + " slot" + a + ": " + subject.getAttrNamesFile().getAttributeName(tagKeyID) + "=" + new String(tagValue));
                            entryTagValueLengthHistogram[(new String(tagValue)).trim().length()]++;
                        } else {
                            numExternalTagValues++;
                        }
                        tmpCurrentTagCount++;
                    } else {
                        // empty tag-slot
                        tmpCountEmptyTagSlots++;
//                        System.out.println("record " + recNR + " slot" + a + " (should be spaces): '" + new String(tagValue) + "'");
                    }
                }
                emptyTagSlotHistogram[tmpCountEmptyTagSlots]++;

                // skip way-IDs
                int tmpCountEmptyWaySlots = 0;
                for (int a = 0; a < WAYREFCOUNTPERRECORD; a++) {
                    int wayID = mem.getInt();
                    if (wayID != Integer.MIN_VALUE) {
                        // non-empty
                        tmpCurrentWayCount++;
                        if (debug.contains(wayID)) {
                            System.err.println("ERROR: way " + wayID
                                    + " contained in node " + id + " in slot " + a
                                    + " (using at least " + tmpCurrentEntryLength + " records) multiple times");
                        }
                        debug.add(wayID);
                    } else {
                        // empty way-slot
                        tmpCountEmptyWaySlots++;
                    }
                }
                emptyWaySlotHistogram[tmpCountEmptyWaySlots]++;
                for (int a = 0; a < RELATIONREFCOUNTPERRECORD; a++) {
                    int relID = mem.getInt();
                }

                // create a histogram of empty block-sizes in the file
                if (tmpConsecutiveEmptyRecords > 0) {
                    if (tmpConsecutiveEmptyRecords >= emptyBlocksHistogram.length) {
                        emptyBlocksHistogram[emptyBlocksHistogram.length - 1]++;
                    } else {
                        emptyBlocksHistogram[tmpConsecutiveEmptyRecords]++;
                    }
                }

                tmpLastNodeID = id;
            }

            ///////////////////////////
            // show statistics
            System.out.println("Slots for tags per record: " + ATTRCOUNTPERRECORD);
            System.out.println("Slots for ways per record: " + WAYREFCOUNTPERRECORD);
            System.out.println("Number of free records: " + unusedRecordCount + " =" + (unusedRecordCount * subject.getRecordLength()) + " byte");
            for (int i = 0; i < emptyBlocksHistogram.length; i++) {
                System.out.print("blocks of " + i);
                if (i == emptyBlocksHistogram.length - 1) {
                    System.out.print(" or more");
                }
                System.out.println(" empty records: " + emptyBlocksHistogram[i] + " (last count includes all larger blocks)");
            }
            for (int i = 0; i < entryLengthHistogram.length; i++) {
                System.out.print("nodes using " + i);
                if (i == entryLengthHistogram.length - 1) {
                    System.out.print(" or more");
                }
                System.out.println(" records: " + entryLengthHistogram[i]);
            }
            for (int i = 0; i < entryTagCountHistogram.length; i++) {
                System.out.print("nodes with " + i);
                if (i == entryTagCountHistogram.length - 1) {
                    System.out.print(" or more");
                }
                System.out.println(" tags: " + entryTagCountHistogram[i]);
            }
            for (int i = 0; i < entryWayCountHistogram.length; i++) {
                System.out.print("nodes with " + i);
                if (i == entryWayCountHistogram.length - 1) {
                    System.out.print(" or more");
                }
                System.out.println(" ways: " + entryWayCountHistogram[i]);
            }
            for (int i = 0; i < emptyTagSlotHistogram.length; i++) {
                System.out.println("records with " + i + " empty slots for tags: " + emptyTagSlotHistogram[i]);
            }
            for (int i = 0; i < emptyWaySlotHistogram.length; i++) {
                System.out.println("records with " + i + " empty slots for ways: " + emptyWaySlotHistogram[i]);
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
     * ${@inheritDoc}.
     */
    @Override
    public int getRecordLength() {
        return getNodeRecordLength();
    }

    /**
     * @return the length of a record in bytes.
     * @see FixedRecordFile
     */
    protected static int getNodeRecordLength() {
        final int bytesPerShort = 2;
        final int bytesPerInt = 4;
        final int bytesPerChar = 2;
        return bytesPerInt //  ID
        + bytesPerInt // version
        + bytesPerInt + bytesPerInt // lat + lon
        + ATTRCOUNTPERRECORD * (bytesPerShort + bytesPerChar * NUMTAGVALCHARS)
        + (WAYREFCOUNTPERRECORD * bytesPerInt)
        + (RELATIONREFCOUNTPERRECORD * bytesPerInt);
    }

    /**
     * Side-effect: position is increased by 4 bytes.
     * @return the ID of the node at the current position
     * @see #getRecordForReading(int)
     */
//    @Deprecated
//    public int getNodeID() {
//        return getMemoryMapped().getInt();
//    }

    /**
     * @param aNode the node to examine
     * @return the required number of consecutive records to store this node
     */
    public int getRequiredRecordCount(final ExtendedNode aNode) {
        Collection<Tag> tagList = aNode.getTags();
        double count = (1.0 * tagList.size()) / ATTRCOUNTPERRECORD;
        count = Math.max(count, (1.0 * aNode.getReferencedWayIDs().size()) / WAYREFCOUNTPERRECORD);
        count = Math.max(count, (1.0 * aNode.getReferencedRelationIDs().size()) / RELATIONREFCOUNTPERRECORD);

        // at least 1 record is needed
        if (count == 0) {
            return 1;
        }
        return (int) Math.ceil(count);
    }

    /**
     * Write the given node.
     * If it is already stored in the nodesFile,
     * the old records are overwritten. If we require
     * more or less records then before, allocation and
     * deallocation are done here too.
     * @param aNode the node to store
     * @param aRecordNr the first of the records this node is stored in
     * @throws IOException on problems growing the file
     */
    public void writeNode(final ExtendedNode aNode, final long aRecordNr) throws IOException {
        this.myNodeCache.put(aNode.getId(), aNode);
        long recordNr = aRecordNr;
        int required = getRequiredRecordCount(aNode);
        List<Object> preparedTagList = super.prepareTagList(aNode.getTags(), NUMTAGVALCHARS);
        required = Math.max(required, preparedTagList.size() / ATTRCOUNTPERRECORD);
        int used     = getUsedRecordCount(recordNr, aNode.getId());

        if (required > used) {
            // invalidate old records and find a new place
            for (long i = recordNr; i< recordNr + used; i++) {
                invalidateRecord(i, aNode.getId());
            }
            get2DIndex().remove(recordNr,
                    FixedPrecisionCoordinateConvertor.convertToFixed(aNode.getLatitude()),
                    FixedPrecisionCoordinateConvertor.convertToFixed(aNode.getLongitude()));
            recordNr = findFreeRecords(required);
            if (recordNr > Integer.MAX_VALUE) {
                throw new IllegalStateException("record-number does no longer fit into an integer.");
            }
            getIndex().put(aNode.getId(), recordNr);

            get2DIndex().put(recordNr,
                    FixedPrecisionCoordinateConvertor.convertToFixed(aNode.getLatitude()),
                    FixedPrecisionCoordinateConvertor.convertToFixed(aNode.getLongitude()));
            used = required;
        }
        if (required < used) {
            //invalidate excess records
            for (long i = recordNr + required; i < recordNr + used; i++) {
                invalidateRecord(i, aNode.getId());
            }
        }

        List<Long> wayIDsOrdered = new ArrayList<Long>(aNode.getReferencedWayIDs());
        List<Long> relIDsOrdered = new ArrayList<Long>(aNode.getReferencedRelationIDs());
        try {
            for (int i = 0; i < required; i++) {
                writeOneNodeRecord(aNode, wayIDsOrdered, relIDsOrdered, i, recordNr + i, preparedTagList);
                myUsedRecordIndex.set((int) (recordNr + i));
                if (aRecordNr + i > myLastRecordWritten) {
                    myLastRecordWritten = aRecordNr + i;
                }
            }
        } catch (RecordInUseException e) {
           LOG.log(Level.SEVERE, "The record is already in use!!!"
                   + "Writing this node in some free space instead.", e);
           myUsedRecordIndex.set((int) e.getRecordNumber());
           get2DIndex().remove(recordNr,
                   FixedPrecisionCoordinateConvertor.convertToFixed(aNode.getLatitude()),
                   FixedPrecisionCoordinateConvertor.convertToFixed(aNode.getLongitude()));
           recordNr = findFreeRecords(required);
           if (recordNr > Integer.MAX_VALUE) {
               throw new IllegalStateException("record-number does no longer fit into an integer.");
           }
           getIndex().put(aNode.getId(), recordNr);

           get2DIndex().put(recordNr,
                   FixedPrecisionCoordinateConvertor.convertToFixed(aNode.getLatitude()),
                   FixedPrecisionCoordinateConvertor.convertToFixed(aNode.getLongitude()));
           used = required;
           for (int i = 0; i < required; i++) {
               writeOneNodeRecord(aNode, wayIDsOrdered, relIDsOrdered, i, recordNr + i, preparedTagList);
               myUsedRecordIndex.set((int) (recordNr + i));
               if (aRecordNr + i > myLastRecordWritten) {
                   myLastRecordWritten = aRecordNr + i;
               }
           }
        }
    }


    /**
     * Return the first of the consecutive record
     * where this node is stored.
     * @param aNodeID the node to look for
     * @return the record-number of the node or -1
     * @throws IOException if we cannot read the record-index
     */
    public long findRecordForNode(final long aNodeID) throws IOException {
        return super.findRecordForElement(aNodeID);
    }


    /**
     * Mark all records of the given node as free.
     * @param aNode the node to remove from storage
     * @throws IOException if the node cannot be removed from the index
     */
    public void removeNode(final Node aNode) throws IOException {
        this.myNodeCache.remove(aNode.getId());
        long recordNr = removeElement(aNode.getId());
        IGeoIndexFile geoIndex = get2DIndex();
        if (geoIndex != null) {
            geoIndex.remove(recordNr,
                    FixedPrecisionCoordinateConvertor.convertToFixed(aNode.getLatitude()),
                    FixedPrecisionCoordinateConvertor.convertToFixed(aNode.getLongitude()));
        }
    }
    /**
     * Write the given node into the current and following records.
     * @param aNode the node to write
     * @param wayIDs the IDs of all ways containing this node
     * @param relIDs the IDs of all relations containing this node
     * @param counter this many records have been written already
     * @param aRecordNumber the location to write to
     * @param tagList tag-list as prepared for writing by {@link AbstractEntityFile}
     * @throws IOException  if an attribute-key cannot be created
     */
    private void writeOneNodeRecord(final Node aNode, final List<Long> wayIDs, final List<Long> relIDs,
            final int counter,
            final long aRecordNumber,
            final List<Object> tagList) throws IOException  {
        if (aNode.getId() > Integer.MAX_VALUE) {
            throw new IllegalStateException("nodeID too large to be represented as an integer");
        }
        //ByteBuffer mem = null;
        //assert (mem = getRecordForReading(aRecordNumber)) != null;
        ByteBuffer mem = getRecordForReading(aRecordNumber);
        if (mem != null) {
            int start = mem.position();
            // assertions are enabled, do safety-checks to not overwrite
            // a record already used by another node
            int safetyTest = mem.getInt();
            mem.position(start);
            if (safetyTest != Integer.MIN_VALUE && safetyTest != aNode.getId()) {
                releaseRecord(mem);
                mem = null;
                throw new RecordInUseException(aRecordNumber, "Major internal error! "
                        + "NOT overwriting record "
                        + aRecordNumber + " that is still in use by way "
                        + safetyTest + " with record " + counter + " of way "
                        + aNode.getId());
            }
        }
//        if (mem == null) {
//            mem = getRecordForWriting(aRecordNumber);
//        }

        int start = mem.position();
        //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        //LOG.finest(getFileName() + " - node " + aNode.getId() + " - part " + counter + " writing to record " + aRecordNumber + " (mem.positiion()=" + mem.position() + ")");
        assert (mem.position() % getRecordLength() == 0) : "We are not at a valid  start-location for a record";
        mem.putInt((int) aNode.getId());
        mem.putInt(aNode.getVersion());
        mem.putInt(FixedPrecisionCoordinateConvertor.convertToFixed(aNode.getLatitude()));
        mem.putInt(FixedPrecisionCoordinateConvertor.convertToFixed(aNode.getLongitude()));

        // write 4 Attributes
        for (int a = 0; a < ATTRCOUNTPERRECORD; a++) {
            Object tag = null;
            int indexIntoTaglist = a + (ATTRCOUNTPERRECORD * counter);
            if (tagList.size() > indexIntoTaglist) {
                tag = tagList.get(indexIntoTaglist);
            }
            writeAttribute(mem, NUMTAGVALCHARS, tag);
        }
        // write 4 ways
        for (int w = 0; w < WAYREFCOUNTPERRECORD; w++) {
            long wayID = Integer.MIN_VALUE;
            int indexIntoWaylist = w + (WAYREFCOUNTPERRECORD * counter);
            if (wayIDs.size() > indexIntoWaylist) {
                wayID = wayIDs.get(indexIntoWaylist);
            }
            if (wayID > Integer.MAX_VALUE) {
                throw new IllegalStateException("wayID too large to be represented as an integer");
            }
            mem.putInt((int) wayID);
        }
     // write 1 relation
        for (int r = 0; r < RELATIONREFCOUNTPERRECORD; r++) {
            long relID = Integer.MIN_VALUE;
            int indexIntoRellist = r + (RELATIONREFCOUNTPERRECORD * counter);
            if (relIDs.size() > indexIntoRellist) {
                relID = relIDs.get(indexIntoRellist);
            }
            if (relID > Integer.MAX_VALUE) {
                throw new IllegalStateException("relID too large to be represented as an integer");
            }
            mem.putInt((int) relID);
        }
        assert (mem.position() % getRecordLength() == 0) : "We are not at a valid  end-location for a record";
        mem.position(start);
        writeRecord(mem, aRecordNumber);
    }

    /**
     * @return all nodes in the file.
     */
    public Iterator<Node> getallNodes() {
        return new Iterator<Node>() {
                private Iterator<Long> myNodeIDs = getAllElements();
                private Node myLastNode;

                public boolean hasNext() {
                    return myNodeIDs.hasNext();
                }

                public Node next() {
                    try {
                        myLastNode = readNode(Integer.MIN_VALUE, myNodeIDs.next());
                        return myLastNode;
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException("UnsupportedEncodingException while fetchine next node", e);
                    } catch (IOException e) {
                        throw new IllegalStateException("IOException while fetchine next node", e);
                    }
                }

                public void remove() {
                    try {
                        removeNode(myLastNode);
                    } catch (IOException e) {
                        throw new IllegalStateException("cannot remove node", e);
                    }
                }

        };
    }

    /**
     * Read the node with the given id from storage.
     * @param aNodeID the node to read
     * @return null if it is not present.
     * @throws IOException if we cannot read an external attribute
     */
    public ExtendedNode readNode(final long aNodeID) throws IOException {
        ExtendedNode cached = this.myNodeCache.get(aNodeID);
        if (cached != null) {
            return cached;
        }
        long recordNr = findRecordForNode(aNodeID);
        //TODO: this is code for debugging
        if (recordNr == -1) {
            recordNr = findRecordForNode(aNodeID);
            if (recordNr != -1) {
                LOG.severe("The IDIndex returned -1 on the first call but " + recordNr
                        + " on a second call to findRecordForNode(aNodeID=" + aNodeID + ")");
            }
        }
        return readNode(aNodeID, recordNr);
    }

    /**
     * Read the node with the given id from storage.
     * @param aNodeID the node to read (ignored if Long.MIN_VALUE)
     * @param aRecordNr the first of the records this node is stored in
     * @return null if it is not present.
     * @throws IOException if we cannot read an external attribute
     * @throws UnsupportedEncodingException if UTF-8 is not supported
     */
    public ExtendedNode readNode(final long aNodeID, final long aRecordNr) throws UnsupportedEncodingException, IOException {
        ExtendedNode cached = this.myNodeCache.get(aNodeID);
        if (cached != null) {
            return cached;
        }
        long recordNr = aRecordNr;
        if (recordNr < 0) {
            return null;
        }
        ByteBuffer mem = getRecordForReading(recordNr);
        long id = mem.getInt();
        if (aNodeID != Long.MIN_VALUE && id != aNodeID) {
            // our index is broken, scan the file and index
            // the correct location.
            if (id == Integer.MIN_VALUE) {
                LOG.log(Level.SEVERE, "broken nodex.idx -file."
                        + " Record " + recordNr + " for node "
                        + aNodeID + " is marked as free.");
                myUsedRecordIndex.clear((int) recordNr);
            } else {
                LOG.log(Level.SEVERE, "broken nodex.idx -file."
                        + " Record " + recordNr + " for node "
                        + aNodeID + " stores node "
                        + id + " instead.");
            }
            releaseRecord(mem);
            if (aNodeID != Long.MIN_VALUE) {
                recordNr = findRecordForElementFallback(aNodeID);
                if (recordNr < 0) {
                    LOG.log(Level.SEVERE, "given node "
                            + aNodeID + " does not exist in this file.");
                    getIndex().remove(aNodeID);
                    return null;
                }
                mem = getRecordForReading(recordNr);
                id = mem.getInt();
                if (id != aNodeID) {
                    releaseRecord(mem);
                    throw new IllegalStateException("Internal error!"
                            + " findRecordForNodeFallback returned recorID "
                            + recordNr + " that does NOT contain the node "
                            + aNodeID + " that it scanned for.");
                }
                getIndex().put(aNodeID, recordNr);
            }
        }
        //LOG.finest(getFileName() + " - node " + aNodeID + " reading from record " + recordNr + " (mem.positiion()=" + mem.position() + ")");
        int version = mem.getInt();
        int latI = mem.getInt();
        double lat = FixedPrecisionCoordinateConvertor.convertToDouble(latI);
        int lonI = mem.getInt();
        double lon = FixedPrecisionCoordinateConvertor.convertToDouble(lonI);
        Collection<Integer> referencingWayIDs = new LinkedList<Integer>();
        Collection<Integer> referencingRelIDs = new LinkedList<Integer>();

        //  read attributes
        int recordCount = 0;
        LinkedList<Tag> tagList = new LinkedList<Tag>();
        while (true) {
            // read attributes of current record
            for (int a = 0; a < ATTRCOUNTPERRECORD; a++) {
                Tag tag = null;
                tag = readAttribute(mem, NUMTAGVALCHARS, tagList);
                if (tag != null) {
                    tagList.add(tag);
                }
            }

            // read way-IDs
            for (int a = 0; a < WAYREFCOUNTPERRECORD; a++) {
                int wayID = mem.getInt();
                if (wayID != Integer.MIN_VALUE) {
                    referencingWayIDs.add(wayID);
                }
            }

            // read relation-IDs
            for (int a = 0; a < RELATIONREFCOUNTPERRECORD; a++) {
                int relID = mem.getInt();
                if (relID != Integer.MIN_VALUE) {
                    referencingRelIDs.add(relID);
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
            if (temp != id) {
                break;
            }
            temp = mem.getInt(); // skip version
            if (temp != version) {
                break;
            }
            //LOG.finest(getFileName() + " - node " + aNodeID + " - part " + recordCount + " reading from record " + recordNr + " (mem.positiion()=" + mem.position() + ")");
            mem.getInt(); // skip lat
            mem.getInt(); // skip lon
        }

        ExtendedNode retval = new ExtendedNode(id, version, 0, lat, lon, tagList);
        for (Integer referencingWayID : referencingWayIDs) {
            retval.addReferencedWay(referencingWayID);
        }
        for (Integer referencingRelID : referencingRelIDs) {
            retval.addReferencedRelation(referencingRelID);
        }
        return retval;
    }

    /**
     * @return my Index by location (may be null)
     */
    public IGeoIndexFile get2DIndex() {
        return my2DIndex;
    }

    /**
     * @param aMy2DIndex the index on location to use to set (may be null)
     */
    public void set2DIndex(final IGeoIndexFile aMy2DIndex) {
        my2DIndex = aMy2DIndex;
    }
}


