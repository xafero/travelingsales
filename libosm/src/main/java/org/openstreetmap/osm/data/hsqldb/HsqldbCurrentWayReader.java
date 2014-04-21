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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.data.mysql.ConstrainedCurrentNodeReader.COMPARESTYLE;
import org.openstreetmap.osm.data.mysql.ConstrainedCurrentNodeReader.CONDITION;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.util.FixedPrecisionCoordinateConvertor;

/**
 * This is a reimplementation of {@link org.openstreetmap.osmosis.core.mysql.v0_5.impl.CurrentWayReader} that
 * is supposed to return only a fraction of the database.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class HsqldbCurrentWayReader extends HsqldbBaseReader<Way> {

    /**
     * my logger for debug and error-output.
     */
    static final Logger LOG = Logger.getLogger(HsqldbCurrentWayReader.class.getName());

        /**
         * The sql-statement to use if a bounding-box is given.<br/>
         * They are to be returned ordered by id, then version.
         */
        private static final String SELECT_SQL =
            "SELECT "
            + " w.id as way_id,"
            + " w.timestamp as way_timestamp "
            + " FROM current_ways w {1}"
            + " WHERE true {0}";

        /**
         * The sql-statement to use if a bounding-box is given.<br/>
         * They are to be returned ordered by id, then version.
         */
        private static final String SELECT_BOUNDINGBOX_SQL =
            "SELECT "
            + " distinct(w.id) as way_id,"
            + " w.timestamp as way_timestamp "
            + " FROM current_nodes n, current_way_nodes wn, current_ways w {5}"
            + " WHERE "
            + "     n.latitude < {0} AND n.latitude > {1}"
            + " AND n.longitude < {2} AND n.longitude > {3} "
            + " AND wn.id = w.id AND wn.node_id = n.id  {4}";

        /**
         * An additional WHERE-clause to add to the SQL.
         */
        private String additionalWhere = "";

        /**
         * An additional FROM-clause to add to the SQL.
         */
        private String additionalFrom = "";

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
         * A prepared-statement to get all {@link Node}s
         * of a way.
         */
        private PreparedStatement myGetNodesStmt;

        /**
         * A prepared-statement to get all {@link Tag}s
         * of a way.
         */
        private PreparedStatement myGetTagsStmt;

        /**
         * We utilize 2 DatabaseContexts that represent
         * a logical connection to the database.
         * This is the first of them.
         */
        private DatabaseContext secondContext;

        /**
         * This constructor creates an instance that will read ALL ways of the database.
         */
        public HsqldbCurrentWayReader() {
            super();
            secondContext = new DatabaseContext();
        }

        /**
         * This constructor creates an instance that will read ALL ways of the database
         * that are within a bounding-box.
         * @param aminLat only the elements within/intersecting this bounding-box will be returned
         * @param amaxLat only the elements within/intersecting this bounding-box will be returned
         * @param aminLon only the elements within/intersecting this bounding-box will be returned
         * @param amaxLon only the elements within/intersecting this bounding-box will be returned
         */
        public HsqldbCurrentWayReader(
                final double aminLat, final double aminLon, final double amaxLat, final double amaxLon) {
            this();

            reInitialize(aminLat, aminLon, amaxLat, amaxLon);
        }

        /**
         * Re-initialize this class to read ALL ways of the database that are within a bounding-box
         * using ths still open database-connections.
         * @param aminLat only the elements within/intersecting this bounding-box will be returned
         * @param amaxLat only the elements within/intersecting this bounding-box will be returned
         * @param aminLon only the elements within/intersecting this bounding-box will be returned
         * @param amaxLon only the elements within/intersecting this bounding-box will be returned
         */
        public void reInitialize(final double aminLat, final double aminLon, final double amaxLat, final double amaxLon) {
            additionalFrom = "";
            additionalWhere = "";
            this.minLat = FixedPrecisionCoordinateConvertor.convertToFixed(Math.min(aminLat, amaxLat));
            this.maxLat = FixedPrecisionCoordinateConvertor.convertToFixed(Math.max(aminLat, amaxLat));
            this.minLon = FixedPrecisionCoordinateConvertor.convertToFixed(Math.min(aminLon, amaxLon));
            this.maxLon = FixedPrecisionCoordinateConvertor.convertToFixed(Math.max(aminLon, amaxLon));
        }

        /**
         * @param aNodeID the id of the single node we want fetch from the ways for the db.
         */
        public HsqldbCurrentWayReader(final long aNodeID) {
            this();
            reInitialize(aNodeID);
        }

        /**
         * Re-initialize this class to read one way from the database that
         * contains the given node using ths still open database-connections.
         * @param aNodeID the id of the single node we want fetch from the ways for the db.
         */
        public void reInitializeForNode(final long aNodeID) {
            additionalFrom = " JOIN current_way_nodes wn2 ON wn2.node_id = " + aNodeID + " AND wn2.id = w.id ";
            additionalWhere = "";
            this.minLat = Integer.MIN_VALUE;
            this.minLon = Integer.MIN_VALUE;
            this.maxLat = Integer.MAX_VALUE;
            this.maxLon = Integer.MAX_VALUE;
        }

        /**
         * Re-initialize this class to read one way from the database
         * using ths still open database-connections.
         * @param aWayID the id of the single way we want fetch from the db.
         */
        public void reInitialize(final long aWayID) {
            additionalFrom = "";
            additionalWhere = " AND w.id = " + aWayID + " ";
            this.minLat = Integer.MIN_VALUE;
            this.minLon = Integer.MIN_VALUE;
            this.maxLat = Integer.MAX_VALUE;
            this.maxLon = Integer.MAX_VALUE;
        }

        /**
         * Creates a new instance that will read all ways that have tags matching the given condition.
         * @param aTags name-value -pairs
         * @param aOr combine the different values in aTags via AND or via OR.
         * @param aLike compare the names and values via SQL-"like" or literaly
         * @param aminLat only the elements within/intersecting this bounding-box will be returned
         * @param amaxLat only the elements within/intersecting this bounding-box will be returned
         * @param aminLon only the elements within/intersecting this bounding-box will be returned
         * @param amaxLon only the elements within/intersecting this bounding-box will be returned
         */
        public HsqldbCurrentWayReader(
                final Map<String, String> aTags, final CONDITION aOr, final COMPARESTYLE aLike,
                final double aminLat, final double aminLon, final double amaxLat, final double amaxLon) {
            this(aTags, aOr, aLike);
            this.minLat = FixedPrecisionCoordinateConvertor.convertToFixed(Math.min(aminLat, amaxLat));
            this.maxLat = FixedPrecisionCoordinateConvertor.convertToFixed(Math.max(aminLat, amaxLat));
            this.minLon = FixedPrecisionCoordinateConvertor.convertToFixed(Math.min(aminLon, amaxLon));
            this.maxLon = FixedPrecisionCoordinateConvertor.convertToFixed(Math.max(aminLon, amaxLon));
        }

        /**
         * Creates a new instance that will read all ways that have tags matching the given condition.
         * @param aTags name-value -pairs
         * @param aOr combine the different values in aTags via AND or via OR.
         * @param aLike compare the names and values via SQL-"like" or literaly
         */
        public HsqldbCurrentWayReader(
                final Map<String, String> aTags, final CONDITION aOr, final COMPARESTYLE aLike) {
            this();
            this.secondContext = new DatabaseContext();

            if (aTags.size() > 0) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, String> tag : aTags.entrySet()) {
                    if (sb.length() > 0) {
                        switch (aOr) {
                        case AND : sb.append(" AND "); break;
                        default : sb.append(" OR "); break;
                        }
                    }

                    // compare the key
                    switch(aLike) {
                    case LIKE:  sb.append("(k like '").append(tag.getKey()).append("' ");
                                break;
                    default:    sb.append("(k = '").append(tag.getKey()).append("' ");
                                break;
                    }

                    // compare the value
                    if (tag.getValue() != null) {
                        switch(aLike) {
                        case LIKE:  sb.append(" AND v like '").append(tag.getValue()).append("') ");
                                    break;
                        default:    sb.append(" AND v = '").append(tag.getValue()).append("') ");
                                    break;
                        }
                    }
                }
                this.additionalWhere = " AND w.id in ( SELECT id from current_way_tags where " + sb.toString() + ")";
            }
        }


        /**
         * {@inheritDoc}
         */
        protected ResultSet createResultSet() {

            String sql;
            if (minLat != Integer.MIN_VALUE || maxLat != Integer.MAX_VALUE
             || minLon != Integer.MIN_VALUE || maxLon != Integer.MAX_VALUE) {
                // if a bounding-box is given
                sql = MessageFormat.format(SELECT_BOUNDINGBOX_SQL, new Object[]
                                          {Double.toString(maxLat),
                                           Double.toString(minLat),
                                           Double.toString(maxLon),
                                           Double.toString(minLon),
                                           additionalWhere,
                                           additionalFrom});
            } else {
                sql = MessageFormat.format(SELECT_SQL, new Object[] {additionalWhere, additionalFrom});
            }
            //LOG.log(Level.FINE, "SQL=" + sql);
            //System.out.println("SQL=" + sql);

            if (myGetNodesStmt == null)
                myGetNodesStmt = this.secondContext.prepareStatement("SELECT node_id from current_way_nodes where id = ? ORDER BY sequence_id");
            if (myGetTagsStmt == null)
                myGetTagsStmt =  this.secondContext.prepareStatement("SELECT k, v from current_way_tags where id = ?");
//TODO: queries like "SELECT  w.id as way_id, w.timestamp as way_timestamp  FROM current_ways w , current_way_nodes wn2 WHERE  wn2.node_id = 105791403 AND wn2.id = w.id" take a LOONG time
            return getResultSet(sql.replace("WHERE true  AND", "WHERE ").replace(" FROM current_ways w , current_way_nodes wn2", " FROM current_way_nodes wn2, current_ways w"));
        }

        /**
         * {@inheritDoc}.
         * @see java.util.Iterator#next()
         */
        public Way getNext() {
            long wayId;
            int version;
            Way way;
            Date timestamp;

            ResultSet resultSet = getResultSet();

            //----------- read the way
            try {
                wayId = resultSet.getLong("way_id");
                version = resultSet.getInt("version");
                timestamp = new Date(resultSet.getTimestamp("way_timestamp").getTime());
            } catch (SQLException e) {
                throw new OsmosisRuntimeException("Unable to read way.", e);
            }

            //----------- read the way-nodes
            List<WayNode> wayNodes = new LinkedList<WayNode>();
            try {
                myGetNodesStmt.setLong(1, wayId);
                ResultSet result = myGetNodesStmt.executeQuery();
                int count = 0;
                while (result.next()) {
                    long nodeID = result.getLong("node_id");
                    wayNodes.add(new WayNode(nodeID));
                    count++;
                }
                if (count == 0)
                    LOG.log(Level.FINE, "Way " + wayId + " has no waynodes!");
                result.close();
            } catch (SQLException e) {
                throw new OsmosisRuntimeException("Unable to read way nodes for way " + wayId + ".", e);
            }

            //----------- read the way-tags
            Collection<Tag> tags = new LinkedList<Tag>();
            try {
                myGetTagsStmt.setLong(1, wayId);
                ResultSet result = myGetTagsStmt.executeQuery();
                while (result.next()) {
                    String key = result.getString(1);
                    String value = result.getString(2);
                    tags.add(new Tag(key, value));
                }
                result.close();

            } catch (SQLException e) {
                throw new OsmosisRuntimeException("Unable to read way tags.", e);
            }

            way = new Way(wayId, version, timestamp, null, 0);
            way.getTags().addAll(tags);
            way.getWayNodes().addAll(wayNodes);
            return way;
        }
    };
