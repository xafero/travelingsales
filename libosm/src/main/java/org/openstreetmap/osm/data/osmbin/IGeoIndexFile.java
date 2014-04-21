package org.openstreetmap.osm.data.osmbin;

import java.io.IOException;
import java.util.Set;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * IGeoIndexFile.java<br/>
 * created: 28.11.2008 10:03 <br/>
 *<br/><br/>
 * <b>This is the nodes.id2-file as described in
 * <a href="http://wiki.openstreetmap.org/index.php/User:MarcusWolschon%5Cosmbin_draft">here</a></b><br/>
 * It stores a 2d-index on the latitude and longitude of nodes and maps them to record-numbers in nodes.obm.
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:OsmbinClasses.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 * @see IIDIndexFile
 * @see Node
 */
public interface IGeoIndexFile {

    /**
     * Add the given entity-ID to record-number -mapping to this index.
     * @param aNodeRecordIndex the record-number in nodex.obm of the node
     * @param aLatitude the latitude to index by encoded as a long integer
     * @param aLongitude the longitude to index by encoded as a long integer
     * @throws IOException if we cannot grow the file
     * @see FixedPrecisionCoordinateConvertor
     */
    void put(final long aNodeRecordIndex, final long aLatitude, final long aLongitude) throws IOException;

    /**
     * Remove the given entity from this index.
     * @param aNodeRecordIndex the record-number in nodex.obm of the node
     * @param aLatitude the latitude to index by encoded as a long integer
     * @param aLongitude the longitude to index by encoded as a long integer
     * @throws IOException if we cannot write or seek in the file.
     * @see FixedPrecisionCoordinateConvertor
     */
    void remove(final long aNodeRecordIndex, final long aLatitude, final long aLongitude) throws IOException;

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
    Set<Long> get(final long aMinLatitude, final long aMinLongitude, final long aMaxLatitude, final long aMaxLongitude) throws IOException;

    /**
     * Release all ressources.
     * @throws IOException if we cannot write
     */
    void close() throws IOException;
}
