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
package org.openstreetmap.osm.data.hsqldb;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
//import java.util.logging.Logger;

import org.openstreetmap.osm.data.mysql.ConstrainedCurrentNodeReader.COMPARESTYLE;
import org.openstreetmap.osm.data.mysql.ConstrainedCurrentNodeReader.CONDITION;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.util.FixedPrecisionCoordinateConvertor;
/*import org.openstreetmap.osmosis.core.mysql.v0_5.impl.EmbeddedTagProcessor;*/
// TODO: Check MySQL tags!


/**
 * Reads current nodes from a database ordered by their identifier.
 * @author Brett Henderson
 */
public class HsqldbCurrentNodeReader  extends HsqldbBaseReader<Node> {

    /**
     * my logger for debug and error-output.
     */
    //private static final Logger LOG = Logger.getLogger(HsqldbCurrentNodeReader.class.getName());

    /**
     * The sql-statement to use if no bounding-box is given.
     */
    private static final String SELECT_SQL =
        "SELECT n.id, n.timestamp, n.latitude, n.longitude, n.tags"
        + " FROM current_nodes n"
        + " WHERE true {0}"
        + " ORDER BY n.id";

    /**
     * The sql-statement to use if a bounding-box is given.
     */
    private static final String SELECT_BOUNDINBOX_SQL =
        "SELECT n.id, n.timestamp, n.latitude, n.longitude, n.tags"
        + " FROM current_nodes n"
        + " WHERE n.latitude < {0} AND n.latitude > {1}"
        + " AND   n.longitude < {2} AND n.longitude > {3}"
        + "  {4}"
        + " ORDER BY n.id";


    /**
     * An additional where-clause for the SQL-expression.
     */
    private String additionalWhere = "";

    /**
     * Helper-Class to parse the tags-column
     * of the "current_nodes"-table.
     */
    private /*EmbeddedTagProcessor*/ Object tagParser; // TODO: Check tag processing!

    /**
     * Return only the nodes within this bounding-box.
     * Stored in the fixed-point format used in the
     * database..<br/>
     * Defaults to  Integer.MIN_VALUE
     * @see org.openstreetmap.osmosis.core.mysql.common.FixedPrecisionCoordinateConvertor
     */
    private int minLat = Integer.MIN_VALUE;

    /**
     * Return only the nodes within this bounding-box.
     * Stored in the fixed-point format used in the
     * database..<br/>
     * Defaults to  Integer.MAX_VALUE
     * @see org.openstreetmap.osmosis.core.mysql.common.FixedPrecisionCoordinateConvertor
     */
    private int maxLat = Integer.MAX_VALUE;

    /**
     * Return only the nodes within this bounding-box.
     * Stored in the fixed-point format used in the
     * database..<br/>
     * Defaults to  Integer.MIN_VALUE
     * @see org.openstreetmap.osmosis.core.mysql.common.FixedPrecisionCoordinateConvertor
     */
    private int minLon = Integer.MIN_VALUE;

    /**
     * Return only the nodes within this bounding-box.<br/>
     * Stored in the fixed-point format used in the
     * database..<br/>
     * Defaults to  Integer.MAX_VALUE
     * @see org.openstreetmap.osmosis.core.mysql.common.FixedPrecisionCoordinateConvertor
     */
    private int maxLon = Integer.MAX_VALUE;

    /**
     * Creates a new instance that will read all nodes.
     *
     */
    public HsqldbCurrentNodeReader() {
       super();
       tagParser = /*new EmbeddedTagProcessor()*/ null; // TODO: Check tag processing!
    }

    /**
     * Creates a new instance that will read only one node with a given id.
     *
     * @param aNodeID the nodeID.
     */
    public HsqldbCurrentNodeReader(final long aNodeID) {
        this();

       reInitialize(aNodeID);
    }

    /**
     * Re-initialize this class to read one node from the database
     * using ths still open database-connections.
     * @param aNodeID the id of the single node we want fetch from the db.
     */
    public void reInitialize(final long aNodeID) {
        additionalWhere = " AND n.id = " + aNodeID;
        this.minLat = Integer.MIN_VALUE;
        this.minLon = Integer.MIN_VALUE;
        this.maxLat = Integer.MAX_VALUE;
        this.maxLon = Integer.MAX_VALUE;
    }

    /**
     * Creates a new instance that will read all nodes within a bounding-box..
     *
     * @param aminLat only the elements within/intersecting this bounding-box will be returned
     * @param amaxLat only the elements within/intersecting this bounding-box will be returned
     * @param aminLon only the elements within/intersecting this bounding-box will be returned
     * @param amaxLon only the elements within/intersecting this bounding-box will be returned
     */
    public HsqldbCurrentNodeReader(
                             final double aminLat, final double aminLon, final double amaxLat, final double amaxLon) {
        this();
        reInitialize(aminLat, aminLon, amaxLat, amaxLon);
    }

    /**
     * Re-initialize this class to read one node from the database
     * using ths still open database-connections.
     * @param aminLat only the elements within/intersecting this bounding-box will be returned
     * @param amaxLat only the elements within/intersecting this bounding-box will be returned
     * @param aminLon only the elements within/intersecting this bounding-box will be returned
     * @param amaxLon only the elements within/intersecting this bounding-box will be returned
     */
    public void reInitialize(final double aminLat, final double aminLon, final double amaxLat, final double amaxLon) {
        this.additionalWhere = "";
        this.minLat = FixedPrecisionCoordinateConvertor.convertToFixed(Math.min(aminLat, amaxLat));
        this.maxLat = FixedPrecisionCoordinateConvertor.convertToFixed(Math.max(aminLat, amaxLat));
        this.minLon = FixedPrecisionCoordinateConvertor.convertToFixed(Math.min(aminLon, amaxLon));
        this.maxLon = FixedPrecisionCoordinateConvertor.convertToFixed(Math.max(aminLon, amaxLon));
    }

    /**
     * Creates a new instance that will read all nodes that have tags matching the given condition.
     * @param aTags name-value -pairs
     * @param aOr combine the different values in aTags via AND or via OR.
     * @param aLike compare the names and values via SQL-"like" or literaly
     */
    public HsqldbCurrentNodeReader(
            final Map<String, String> aTags, final CONDITION aOr, final COMPARESTYLE aLike) {
        this();
        reInitialize(aTags, aOr, aLike);
    }

    /**
     * Re-initialize this class to read one node from the database
     * using ths still open database-connections.
     * @param aTags name-value -pairs
     * @param aOr combine the different values in aTags via AND or via OR.
     * @param aLike compare the names and values via SQL-"like" or literaly
     */
    public void reInitialize(final Map<String, String> aTags, final CONDITION aOr, final COMPARESTYLE aLike) {
        if (aTags.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> tag : aTags.entrySet()) {
                if (sb.length() > 0) {
                    switch (aOr) {
                    case AND : sb.append(" AND "); break;
                    default : sb.append(" OR "); break;
                    }
                }
                //ignored switch(aLike) {

                if (tag.getValue() != null) {
                    // it maybe: the only tag, the first tag, the last tag or a tag in the middle of others
                    sb.append("(n.tags like '%;").append(tag.getKey()).append("=").append(tag.getValue()).append(";%') ");

                } else {
                    // it maybe: the only tag, the first tag, the last tag or a tag in the middle of others
                    sb.append("(n.tags like '%").append(tag.getKey()).append("=%') ");

                }
            }
            this.additionalWhere = " AND (" + sb.toString() + ")";
        } else {
            this.additionalWhere = "";
        }
        this.minLat = Integer.MIN_VALUE;
        this.minLon = Integer.MIN_VALUE;
        this.maxLat = Integer.MAX_VALUE;
        this.maxLon = Integer.MAX_VALUE;
    }
    /**
     * {@inheritDoc}
     */
    protected ResultSet createResultSet() {

        String sql;
        if (minLat != Integer.MIN_VALUE || maxLat != Integer.MAX_VALUE
         || minLon != Integer.MIN_VALUE || maxLon != Integer.MAX_VALUE) {
            // if a bounding-box is given
            sql = MessageFormat.format(SELECT_BOUNDINBOX_SQL, new Object[]
                                      {Double.toString(maxLat),
                                       Double.toString(minLat),
                                       Double.toString(maxLon),
                                       Double.toString(minLon),
                                       this.additionalWhere});
        } else {
            sql = MessageFormat.format(SELECT_SQL, new Object[] {this.additionalWhere});
        }
        return getResultSet(sql);
    }


    /**
     * {@inheritDoc}.
     * @see java.util.Iterator#next()
     */
    public Node getNext() {

        ResultSet resultSet = getResultSet();

        long id;
        Date timestamp;
        double latitude;
        double longitude;
        String tags;
        Node node;
        int version;

        try {
            id = resultSet.getLong("id");

            version = resultSet.getInt("version");
            
            timestamp = new Date(resultSet.getTimestamp("timestamp").getTime());

            latitude = FixedPrecisionCoordinateConvertor.convertToDouble(resultSet
                    .getInt("latitude"));

            longitude = FixedPrecisionCoordinateConvertor.convertToDouble(resultSet
                    .getInt("longitude"));

            tags = resultSet.getString("tags");

        } catch (SQLException e) {
            throw new OsmosisRuntimeException("Unable to read node fields.", e);
        }

        List<org.openstreetmap.osmosis.core.domain.v0_6.Tag> v5tags = /*tagParser.parseTags(tags);*/ null; // TODO: Check tag parsing!
        Collection<Tag> v6tags = new ArrayList<Tag>(v5tags.size());
        for (org.openstreetmap.osmosis.core.domain.v0_6.Tag tag : v5tags) {
            v6tags.add(new Tag(tag.getKey(), tag.getValue()));
        }
        node = new Node(id, version, timestamp, (OsmUser) null, 0, latitude, longitude);
        node.getTags().addAll(v6tags);

        return (node);
    }

    /**
     * {@inheritDoc}.
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new IllegalArgumentException("remove is not supported!");
    }
}
