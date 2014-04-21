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
package org.openstreetmap.osm.data.mysql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
//import java.util.logging.Logger;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.database.DatabaseLoginCredentials;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.util.FixedPrecisionCoordinateConvertor;
/*import org.openstreetmap.osmosis.core.mysql.v0_5.impl.EmbeddedTagProcessor;*/ // TODO: Check tag processing!


/**
 * Reads current nodes from a database ordered by their identifier.
 * @author Brett Henderson
 */
public class ConstrainedCurrentNodeReader  extends ConstrainedBaseReader<Node> {

    /**
     * my logger for debug and error-output.
     */
    //private static final Logger LOG = Logger.getLogger(ConstrainedCurrentNodeReader.class.getName());

    /**
     * The sql-statement to use if no bounding-box is given.
     */
    private static final String SELECT_SQL =
        "SELECT n.id, n.timestamp, u.data_public, u.display_name, n.latitude, n.longitude, n.tags"
        + " FROM current_nodes n, users u"
        + " WHERE n.user_id = u.id {0}"
        + " ORDER BY n.id";

    /**
     * The sql-statement to use if a bounding-box is given.
     */
    private static final String SELECT_BOUNDINBOX_SQL =
        "SELECT n.id, n.timestamp, u.data_public, u.display_name, n.latitude, n.longitude, n.tags, n.visible"
        + " FROM current_nodes n"
        + " LEFT OUTER JOIN users u ON n.user_id = u.id"
        + " WHERE n.latitude < {0} AND n.latitude > {1}"
        + " AND   n.longitude < {2} AND n.longitude > {3}"
        + "  {4}"
        + " ORDER BY n.id";

    /**
     * When searching for nodes by name,
     * compare the search-expression via SQL-"LIKE"
     * or for string-equality?
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    public static enum COMPARESTYLE { LITERAL, LIKE };

    /**
     * When searching for nodes by tag-values,
     * do all tag-values of the search-expressions
     * have to match or only one?
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    public static enum CONDITION { AND, OR };

    /**
     * An additional where-clause for the SQL-expression.
     */
    private String additionalWhere = "";

    /**
     * Helper-Class to parse the tags-column
     * of the "current_nodes"-table.
     */
    private /*EmbeddedTagProcessor*/ Object tagParser;

    /**
     * Return only the nodes within this bounding-box.
     * Stored in the fixed-point format used in the
     * database..<br/>
     * Defaults to  Integer.MIN_VALUE
     * @see FixedPrecisionCoordinateConvertor
     */
    private int minLat = Integer.MIN_VALUE;

    /**
     * Return only the nodes within this bounding-box.
     * Stored in the fixed-point format used in the
     * database..<br/>
     * Defaults to  Integer.MAX_VALUE
     * @see FixedPrecisionCoordinateConvertor
     */
    private int maxLat = Integer.MAX_VALUE;

    /**
     * Return only the nodes within this bounding-box.
     * Stored in the fixed-point format used in the
     * database..<br/>
     * Defaults to  Integer.MIN_VALUE
     * @see FixedPrecisionCoordinateConvertor
     */
    private int minLon = Integer.MIN_VALUE;

    /**
     * Return only the nodes within this bounding-box.<br/>
     * Stored in the fixed-point format used in the
     * database..<br/>
     * Defaults to  Integer.MAX_VALUE
     * @see FixedPrecisionCoordinateConvertor
     */
    private int maxLon = Integer.MAX_VALUE;

    /**
     * Creates a new instance that will read all nodes.
     *
     * @param aLoginCredentials
     *            Contains all information required to connect to the database.
     */
    public ConstrainedCurrentNodeReader(final DatabaseLoginCredentials aLoginCredentials) {
       super(aLoginCredentials);
       tagParser = null; //new EmbeddedTagProcessor();
    }

    /**
     * Creates a new instance that will read only one node with a given id.
     *
     * @param aNodeID the nodeID.
     * @param loginCredentials
     *            Contains all information required to connect to the database.
     */
    public ConstrainedCurrentNodeReader(final DatabaseLoginCredentials loginCredentials,
            final long aNodeID) {
        this(loginCredentials);

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
     * @param loginCredentials
     *            Contains all information required to connect to the database.
     * @param aminLat only the elements within/intersecting this bounding-box will be returned
     * @param amaxLat only the elements within/intersecting this bounding-box will be returned
     * @param aminLon only the elements within/intersecting this bounding-box will be returned
     * @param amaxLon only the elements within/intersecting this bounding-box will be returned
     */
    public ConstrainedCurrentNodeReader(final DatabaseLoginCredentials loginCredentials,
                             final double aminLat, final double aminLon, final double amaxLat, final double amaxLon) {
        this(loginCredentials);
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
     * @param aCredentials
     *            Contains all information required to connect to the database.
     * @param aTags name-value -pairs
     * @param aOr combine the different values in aTags via AND or via OR.
     * @param aLike compare the names and values via SQL-"like" or literaly
     */
    public ConstrainedCurrentNodeReader(final DatabaseLoginCredentials aCredentials,
            final Map<String, String> aTags, final CONDITION aOr, final COMPARESTYLE aLike) {
        this(aCredentials);
        reInitialize(aTags, aOr, aLike);
    }

    /**
     * Re-initialize this class to read one node from the database
     * using ths still open database-connections.
     * @param aCredentials
     *            Contains all information required to connect to the database.
     * @param aTags name-value -pairs
     * @param aOr combine the different values in aTags via AND or via OR.
     * @param aLike compare the names and values via SQL-"like" or literaly
     * @param aminLat only the elements within/intersecting this bounding-box will be returned
     * @param amaxLat only the elements within/intersecting this bounding-box will be returned
     * @param aminLon only the elements within/intersecting this bounding-box will be returned
     * @param amaxLon only the elements within/intersecting this bounding-box will be returned
     */
    public ConstrainedCurrentNodeReader(final DatabaseLoginCredentials aCredentials,
            final Map<String, String> aTags, final CONDITION aOr, final COMPARESTYLE aLike,
            final double aminLat, final double aminLon, final double amaxLat, final double amaxLon) {
        this(aCredentials, aTags, aOr, aLike);
        this.minLat = FixedPrecisionCoordinateConvertor.convertToFixed(Math.min(aminLat, amaxLat));
        this.maxLat = FixedPrecisionCoordinateConvertor.convertToFixed(Math.max(aminLat, amaxLat));
        this.minLon = FixedPrecisionCoordinateConvertor.convertToFixed(Math.min(aminLon, amaxLon));
        this.maxLon = FixedPrecisionCoordinateConvertor.convertToFixed(Math.max(aminLon, amaxLon));
    }

    /**
     * Re-initialize this class to read one node from the database
     * using ths still open database-connections.
     * @param aTags name-value -pairs
     * @param aOr combine the different values in aTags via AND or via OR.
     * @param aLike compare the names and values via SQL-"like" or literaly
     * @param aminLat only the elements within/intersecting this bounding-box will be returned
     * @param amaxLat only the elements within/intersecting this bounding-box will be returned
     * @param aminLon only the elements within/intersecting this bounding-box will be returned
     * @param amaxLon only the elements within/intersecting this bounding-box will be returned
     */
    public void reInitialize(final Map<String, String> aTags, final CONDITION aOr, final COMPARESTYLE aLike,
            final double aminLat, final double aminLon, final double amaxLat, final double amaxLon) {
        reInitialize(aTags, aOr, aLike);
        this.minLat = FixedPrecisionCoordinateConvertor.convertToFixed(Math.min(aminLat, amaxLat));
        this.maxLat = FixedPrecisionCoordinateConvertor.convertToFixed(Math.max(aminLat, amaxLat));
        this.minLon = FixedPrecisionCoordinateConvertor.convertToFixed(Math.min(aminLon, amaxLon));
        this.maxLon = FixedPrecisionCoordinateConvertor.convertToFixed(Math.max(aminLon, amaxLon));
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

                // it maybe: the only tag, the first tag, the last tag or a tag in the middle of others
                if (tag.getValue() != null) {
                    sb.append("(n.tags like '%;").append(tag.getKey()).append("=").append(tag.getValue()).append(";%') ");
                } else {
                    sb.append("(n.tags like '%;").append(tag.getKey()).append("=%') ");
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
        int version;
        Date timestamp;
        //String userName;
        double latitude;
        double longitude;
        String tags;
        Node node;

        try {
            id = resultSet.getLong("id");

            version = resultSet.getInt("version");

            timestamp = new Date(resultSet.getTimestamp("timestamp").getTime());

            //userName = resultSet.getString("display_name");

            latitude = FixedPrecisionCoordinateConvertor.convertToDouble(resultSet
                    .getInt("latitude"));

            longitude = FixedPrecisionCoordinateConvertor.convertToDouble(resultSet
                    .getInt("longitude"));

            tags = resultSet.getString("tags");

        } catch (SQLException e) {
            throw new OsmosisRuntimeException("Unable to read node fields.", e);
        }

        List<org.openstreetmap.osmosis.core.domain.v0_6.Tag> oldTags = null; //tagParser.parseTags(tags); // TODO: Check tags!
        Collection<Tag> newTags = new LinkedList<Tag>();
        for (org.openstreetmap.osmosis.core.domain.v0_6.Tag oldtag : oldTags) {
            newTags.add(new Tag(oldtag.getKey(), oldtag.getValue()));
        }
        node = new Node(id, version, timestamp, null, 0, latitude, longitude);
        node.getTags().addAll(newTags);

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
