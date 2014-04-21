package org.openstreetmap.osm.data.osmbin;

import java.io.IOException;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * IGeoIndexFile.java<br/>
 * created: 28.11.2008 10:03 <br/>
 *<br/><br/>
 * <b>This is the nodes.idx-file as described in
 * <a href="http://wiki.openstreetmap.org/index.php/User:MarcusWolschon%5Cosmbin_draft">here</a></b><br/>
 * It stores an index on the nodeID of nodes and maps them to record-numbers in nodes.obm.
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:OsmbinClasses.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 * @see IGeoIndexFile
 * @see Node
 */
public interface IIDIndexFile {

    /**
     * Add the given entity-ID to record-number -mapping to this index.
     * @param id the ID of the entity
     * @param recordNr the record-number where the entity with this ID is stored
     * @throws IOException if we cannot grow the file
     */
    void put(final long id, final long recordNr) throws IOException;

    /**
     * Remove the given entity from this index.
     * @param id the ID of the entity
     * @throws IOException if we cannot write or seek in the file.
     */
    void remove(final long id) throws IOException;

    /**
     * Look up a record-number in this index.
     * @param id the ID of the entity
     * @return the record-number where the entity with this ID is stored or -1
     * @throws IOException if we cannot read from the file
     */
    long get(final long id) throws IOException;

    /**
     * Release all ressources.
     * @throws IOException if we cannot write
     */
    void close() throws IOException;
}