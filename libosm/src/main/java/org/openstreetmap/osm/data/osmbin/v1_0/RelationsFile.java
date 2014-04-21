/**
 * RelationsFile.java
 * created: 02.01.2009
 * (c) 2008 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und
Beratung</a>
 * This file is part of libosm by Marcus Wolschon <a
href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by
contacting him directly.
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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
//import java.util.logging.Level;
//import java.util.logging.Logger;

//automatically created propertyChangeListener-Support
//import java.beans.PropertyChangeListener;
//import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.openstreetmap.osm.data.osmbin.AttrNames;
import org.openstreetmap.osm.data.osmbin.IDIndexFile;

import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und
Beratung</a>.<br/>
 * Project: libosm<br/>
 * RelationsFile.java<br/>
 * created: 02.01.2009 <br/>
 *<br/><br/>s
 * <b>This is the relations.obm-file as described in
 * <a href="http://wiki.openstreetmap.org/index.php?title=User:MarcusWolschon/osmbin_draft#version_1.0">here</a></b>
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:OsmbinClasses.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class RelationsFile extends AbstractEntityFile {

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
     * are used to store the way.
     */
    private static final int ATTRCOUNTPERRECORD = 1;

    /**
     * This many slots to store a single
     * reference including type-code and role-string
     * in a single record.
     * If more are required, additional records
     * are used to store the way.
     */
    private static final int REFCOUNTPERRECORD = 4;

    /**
     * If we need to grow the file,
     * create this may spare records.
     */
    private static final int GROWEXCESSRECORDS = 2000;


    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(RelationsFile.class
            .getName());

    /**
     * @param aFileName the filename of the ways.obm -file.
     * @param anAttrNamesFile The file attribute-names are stored in.
     * @param anIndex The file containing an index by node-ID.
     * @throws IOException if we cannot open or create the file.
     */
    public RelationsFile(final File aFileName,
            final AttrNames anAttrNamesFile,
            final IDIndexFile anIndex) throws IOException {
        super(aFileName, anIndex, anAttrNamesFile);
        super.myGrowExcessRecordsCount = GROWEXCESSRECORDS;
    }



//    /**
//     * Automatically created logger for debug and error-output.
//     */
//    private static final Logger LOG = Logger.getLogger(WaysFile.class
//            .getName());

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "RelationsFile@" + hashCode();
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public int getRecordLength() {
        return getRelationRecordLength();
    }

    /**
     * @return the length of a record in bytes.
     * @see FixedRecordFile
     */
    protected static int getRelationRecordLength() {
        final int bytesPerShort = 2;
        final int bytesPerInt = 4;
        final int bytesPerChar = 2;
        return bytesPerInt //  ID
        + bytesPerInt // verson
        + ATTRCOUNTPERRECORD * (bytesPerShort + bytesPerChar * NUMTAGVALCHARS)
        + (REFCOUNTPERRECORD * bytesPerInt * (1 + 1 + 1));
    }


    /**
     * @param  record a buffer containing the record's content
     * @return true if the currently selected record is free for reuse.
     */
    public boolean isRecordUnused(final ByteBuffer record) {
        return record.getInt() == Integer.MIN_VALUE;
    }

    /**
     * @param aRelation the relation to examine
     * @return the required number of consecutive records to store this
node
     */
    public int getRequiredRecordCount(final Relation aRelation) {
        Collection<Tag> tagList = aRelation.getTags();
        double count = (1.0 * tagList.size()) / ATTRCOUNTPERRECORD;
        count = Math.max(count, (1.0 * aRelation.getMembers().size())
                / REFCOUNTPERRECORD);

        // at least 1 record is needed
        if (count == 0) {
            return 1;
        }
        return (int) Math.ceil(count);
    }

    /**
     * @return all relations in the file.
     */
    public Iterator<Relation> getallRelations() {
        return new Iterator<Relation>() {
                private Iterator<Long> myRelationIDs = getAllElements();
                private Relation myLastRelation;

                public boolean hasNext() {
                    return myRelationIDs.hasNext();
                }

                public Relation next() {
                    try {
                        myLastRelation = readRelation(myRelationIDs.next());
                        return myLastRelation;
                    } catch (UnsupportedEncodingException e) {
                        throw new IllegalStateException("UnsupportedEncodingException while fetchine next relation", e);
                    } catch (IOException e) {
                        throw new IllegalStateException("IOException while fetchine next relation", e);
                    }
                }

                public void remove() {
                    try {
                        removeRelation(myLastRelation);
                    } catch (IOException e) {
                        throw new IllegalStateException("cannot remove relation", e);
                    }
                }

        };
    }

    /**
     * Write the given relation.
     * If it is already stored in the ways-File,
     * the old records are overwritten. If we require
     * more or less records then before, allocation and
     * deallocation are done here too.
     * @param aRelation the relation to store
     * @throws IOException on problems growing the file
     */
    public void writeRelation(final Relation aRelation) throws IOException {
        long recordNr = findRecordForRelation(aRelation.getId());
        int required = getRequiredRecordCount(aRelation);
        int used     = getUsedRecordCount(recordNr, aRelation.getId());
        List<Object> preparedTagList = super.prepareTagList(aRelation.getTags(), NUMTAGVALCHARS);
        required = Math.max(required, preparedTagList.size() / ATTRCOUNTPERRECORD);

        if (required > used) {
            // invalidate old records and find a new place
            for (long i = recordNr; i< recordNr + used; i++) {
                invalidateRecord(i, aRelation.getId());
            }
            recordNr = findFreeRecords(required);
            getIndex().put(aRelation.getId(), recordNr);
            used = required;
        }
        if (required < used) {
            //invalidate excess records
            for (long i = recordNr + required; i < recordNr + used; i++) {
                invalidateRecord(i, aRelation.getId());
            }
        }
        try {
            for (int i = 0; i < required; i++) {
                writeOneRelationRecord(aRelation, i, recordNr + i, preparedTagList);
                myUsedRecordIndex.set((int) (recordNr + i));
                if (recordNr + i > myLastRecordWritten) {
                    myLastRecordWritten = recordNr + i;
                }
            }
        } catch (RecordInUseException e) {
            LOG.log(Level.SEVERE, "The record is already in use!!!"
                    + "Writing this relation in some free space instead.", e);
            myUsedRecordIndex.set((int) e.getRecordNumber());

            recordNr = findFreeRecords(required);
            getIndex().put(aRelation.getId(), recordNr);
            used = required;

            for (int i = 0; i < required; i++) {
                writeOneRelationRecord(aRelation, i, recordNr + i, preparedTagList);
                myUsedRecordIndex.set((int) (recordNr + i));
                if (recordNr + i > myLastRecordWritten) {
                    myLastRecordWritten = recordNr + i;
                }
            }
        }
    }


    /**
     * Return the first of the consecutive record
     * where this relation is stored.
     * @param aRelationID the relation to look for
     * @return the record-number of the node or -1
     * @throws IOException if we cannot read the records
     */
    public long findRecordForRelation(final long aRelationID) throws IOException {
        return super.findRecordForElement(aRelationID);
    }


    /**
     * Mark all records of the given relation as free.
     * @param aRelation the relation to remove from storage
     * @throws IOException if the way cannot be removed from the index
     */
    public void removeRelation(final Relation aRelation) throws IOException {
        removeElement(aRelation.getId());
    }

    /**
     * Write the given relation into the current and following records.
     * @param aRelation the relation to write
     * @param counter this many records have been written already
     * @param aPreparedTagList the tags of the relation
     * @throws IOException  if an attribute-key cannot be created
     * @param recordNumber the record in this file where to write to
     */
    private void writeOneRelationRecord(final Relation aRelation, final int counter, final long recordNumber,
            final List<Object> aPreparedTagList) throws IOException  {
//        long firstRecordNumber = recordNumber;
//        if (recordNumber == 2000) {
//            LOG.log(Level.SEVERE, "Breakpoint - please analyse buffer-position for potential OverflowExceptin");
//        }
        if (aRelation.getId() > Integer.MAX_VALUE) {
            throw new IllegalStateException("relationID too large to be represented as an integer");
        }
        ByteBuffer mem = getRecordForReading(recordNumber);
        int start = mem.position();
        int safetyTest = mem.getInt();
        mem.position(start);
        if (safetyTest != Integer.MIN_VALUE && safetyTest != aRelation.getId()) {
            releaseRecord(mem);
            mem = null;
            throw new RecordInUseException(recordNumber, "Major internal error! "
                    + "NOT overwriting record "
                    + recordNumber + " that is still in use by relation "
                    + safetyTest + " with record " + counter + " of relation "
                    + aRelation.getId());
        }
        mem.putInt((int) aRelation.getId());
        mem.putInt(aRelation.getVersion());

        // write ATTRCOUNTPERRECORD Attributes
        for (int a = 0; a < ATTRCOUNTPERRECORD; a++) {
            Object tag = null;
            int indexIntoTaglist = a + (ATTRCOUNTPERRECORD * counter);
            if (aPreparedTagList.size() > indexIntoTaglist) {
                tag = aPreparedTagList.get(indexIntoTaglist);
            }
            writeAttribute(mem, NUMTAGVALCHARS, tag);
        }
        // write NODEREFCOUNTPERRECORD nodes
        for (int w = 0; w < REFCOUNTPERRECORD; w++) {
            int objectID = Integer.MIN_VALUE;
            int objectType = 0;
            int roleID = Integer.MIN_VALUE;
            int indexIntoObjectlist = w + (REFCOUNTPERRECORD * counter);
            if (aRelation.getMembers().size() > indexIntoObjectlist) {
                RelationMember object = aRelation.getMembers().get(indexIntoObjectlist);
                long objectIdLong = object.getMemberId();
                if (objectIdLong > Integer.MAX_VALUE) {
                    throw new IllegalStateException("nodeID too large to be represented as an integer");
                }
                objectID = (int) objectIdLong;
                objectType = object.getMemberType().ordinal();
                roleID = getAttrNamesFile().getOrCreateKey(object.getMemberRole());
                //newlines,.. may change assert getAttrNamesFile().getAttributeName((short) roleID).equalsIgnoreCase(object.getMemberRole());
            }
            mem.putInt(objectID);
            mem.putInt(objectType);
            mem.putInt(roleID);
        }
        mem.position(start);
        writeRecord(mem, recordNumber);
    }

    /**
     * Read the relation with the given id from storage.
     * @param aRelationID the way to read
     * @return null if it is not present.
     * @throws IOException if we cannot read an external attribute
     */
    public Relation readRelation(final long aRelationID) throws IOException {
        long recordNr = findRecordForRelation(aRelationID);
        return readRelation(recordNr, aRelationID);
    }
    /**
     * Read the relation with the given id from storage.
     * @param aRelationID the way to read
     * @param aRecordNr the record to read
     * @return null if it is not present.
     * @throws IOException if we cannot read an external attribute
     */
    public Relation readRelation(final long aRecordNr, final long aRelationID) throws IOException {
        long recordNr = aRecordNr;
        if (recordNr < 0) {
            return null;
        }
        ByteBuffer mem = getRecordForReading(recordNr);
        long id = mem.getInt();
        if (id != aRelationID && aRelationID != Integer.MIN_VALUE) {
            if (id == Integer.MIN_VALUE) {
                LOG.log(Level.SEVERE, "broken relations.idx -file."
                        + " Record " + recordNr + " for relation "
                        + aRelationID + " is marked as free.");
                myUsedRecordIndex.clear((int) recordNr);
            } else {
                LOG.log(Level.SEVERE, "record for relation "
                    + aRelationID + " stores relation "
                    + id + " instead");
            }
            releaseRecord(mem);
            if (aRelationID != Long.MIN_VALUE) {
                recordNr = findRecordForElementFallback(aRelationID);
                if (recordNr < 0) {
                    LOG.log(Level.SEVERE, "given relation "
                            + aRelationID + " does not exist in this file.");
                    getIndex().remove(aRelationID);
                    return null;
                }
                mem = getRecordForReading(recordNr);
                id = mem.getInt();
                if (id != aRelationID) {
                    releaseRecord(mem);
                    throw new IllegalStateException("Internal error!"
                            + " findRecordForRelationFallback returned recorID "
                            + recordNr + " that does NOT contain the relation "
                            + aRelationID + " that it scanned for.");
                }
                getIndex().put(aRelationID, recordNr);
            }
        }
        int version = mem.getInt();
        int recordCount = 0;
        List<Tag> tagList = new LinkedList<Tag>();
        List<RelationMember> members = new LinkedList<RelationMember>();
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
            for (int a = 0; a < REFCOUNTPERRECORD; a++) {
                int objectID   = mem.getInt();
                int objectType = mem.getInt();
                int roleID     = mem.getInt();
                if (objectID != Integer.MIN_VALUE) {
                    members.add(new RelationMember(objectID,  EntityType.values()[objectType], getAttrNamesFile().getAttributeName((short) roleID)));
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
            if (temp != aRelationID) {
                break;
            }
            temp = mem.getInt(); // skip version
            if (temp != version) {
                break;
            }
        }
        Relation retval = new Relation(id, version, new Date(), OsmUser.NONE, 0);
        retval.getTags().addAll(tagList);
        retval.getMembers().addAll(members);
        releaseRecord(mem);
        return retval;
    }

}



