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
import org.openstreetmap.osm.io.HsqldbDatabaseLoader;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

/**
 * This is a reimplementation of {@link org.openstreetmap.osmosis.core.mysql.v0_5.impl.CurrentRelationReader} that
 * is supposed to return only a fraction of the database.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class HsqldbCurrentRelationReader extends HsqldbBaseReader<Relation> {

    /**
     * my logger for debug and error-output.
     */
    static final Logger LOG = Logger.getLogger(HsqldbCurrentRelationReader.class.getName());

        /**
         * The sql-statement to use if a bounding-box is given.<br/>
         * They are to be returned ordered by id, then version.
         */
        private static final String SELECT_SQL =
            "SELECT "
            + " r.id,"
            + " r.timestamp "
            + " FROM current_relations r {1}"
            + " WHERE true {0}";

//        /**
//         * The sql-statement to use if a bounding-box is given.<br/>
//         * They are to be returned ordered by id, then version.
//         */
//        private static final String SELECT_BOUNDINGBOX_SQL =
//            "SELECT "
//            + " distinct(r.id) as id,"
//            + " r.timestamp, "
//            + " u.display_name"
//            + " FROM current_relations r, current_way_nodes wn, current_nodes n, users u {5}"
//            + " WHERE w.user_id = u.id AND wn.id = w.id AND wn.node_id = n.id"
//            + " AND n.latitude < {0} AND n.latitude > {1}"
//            + " AND n.longitude < {2} AND n.longitude > {3}  {4}";

        /**
         * An additional WHERE-clause to add to the SQL.
         */
        private String additionalWhere = "";

        /**
         * An additional FROM-clause to add to the SQL.
         */
        private String additionalFrom = "";

//        /**
//         * Return only the nodes within this bounding-box.
//         * Stored in the fixed-point format used in the
//         * database..<br/>
//         * Defaults to  Integer.MIN_VALUE
//         * @see org.openstreetmap.osmosis.core.mysql.common.FixedPrecisionCoordinateConvertor
//         */
//        private int minLat = Integer.MIN_VALUE;
//
//        /**
//         * Return only the nodes within this bounding-box.
//         * Stored in the fixed-point format used in the
//         * database..<br/>
//         * Defaults to  Integer.MAX_VALUE
//         * @see org.openstreetmap.osmosis.core.mysql.common.FixedPrecisionCoordinateConvertor
//         */
//        private int maxLat = Integer.MAX_VALUE;
//
//        /**
//         * Return only the nodes within this bounding-box.
//         * Stored in the fixed-point format used in the
//         * database..<br/>
//         * Defaults to  Integer.MIN_VALUE
//         * @see org.openstreetmap.osmosis.core.mysql.common.FixedPrecisionCoordinateConvertor
//         */
//        private int minLon = Integer.MIN_VALUE;
//
//        /**
//         * Return only the nodes within this bounding-box.<br/>
//         * Stored in the fixed-point format used in the
//         * database..<br/>
//         * Defaults to  Integer.MAX_VALUE
//         * @see org.openstreetmap.osmosis.core.mysql.common.FixedPrecisionCoordinateConvertor
//         */
//        private int maxLon = Integer.MAX_VALUE;

        /**
         * A prepared-statement to get all {@link RelationMember}s
         * of a relation.
         */
        private PreparedStatement myGetMembersStmt;

        /**
         * A prepared-statement to get all {@link Tag}s
         * of a relation.
         */
        private PreparedStatement myGetTagsStmt;

        /**
         * We utilize 2 DatabaseContexts that represent
         * a logical connection to the database.
         * This is the first of them.
         */
        private DatabaseContext secondContext;

        /**
         * This constructor creates an instance that will read ALL relations of the database.
         */
        public HsqldbCurrentRelationReader() {
            super();
            secondContext = new DatabaseContext();
        }

//        /**
//         * This constructor creates an instance that will read ALL relations of the database
//         * that are within a bounding-box.
//         * @param aLoginCredentials the database and -login we use.
//         * @param aminLat only the elements within/intersecting this bounding-box will be returned
//         * @param amaxLat only the elements within/intersecting this bounding-box will be returned
//         * @param aminLon only the elements within/intersecting this bounding-box will be returned
//         * @param amaxLon only the elements within/intersecting this bounding-box will be returned
//         */
//        public HsqldbCurrentRelationReader(final DatabaseLoginCredentials aLoginCredentials,
//                final double aminLat, final double aminLon, final double amaxLat, final double amaxLon) {
//            this(aLoginCredentials);
//
//            reInitialize(aminLat, aminLon, amaxLat, amaxLon);
//        }
//
//        /**
//         * Re-initialize this class to read ALL ways of the database that are within a bounding-box
//         * using ths still open database-connections.
//         * @param aminLat only the elements within/intersecting this bounding-box will be returned
//         * @param amaxLat only the elements within/intersecting this bounding-box will be returned
//         * @param aminLon only the elements within/intersecting this bounding-box will be returned
//         * @param amaxLon only the elements within/intersecting this bounding-box will be returned
//         */
//        public void reInitialize(final double aminLat, final double aminLon, final double amaxLat, final double amaxLon) {
//            additionalFrom = "";
//            additionalWhere = "";
//            this.minLat = FixedPrecisionCoordinateConvertor.convertToFixed(Math.min(aminLat, amaxLat));
//            this.maxLat = FixedPrecisionCoordinateConvertor.convertToFixed(Math.max(aminLat, amaxLat));
//            this.minLon = FixedPrecisionCoordinateConvertor.convertToFixed(Math.min(aminLon, amaxLon));
//            this.maxLon = FixedPrecisionCoordinateConvertor.convertToFixed(Math.max(aminLon, amaxLon));
//        }

        /**
         * @param aRelationID the id of the single relation we want fetch from the db.
         */
        public HsqldbCurrentRelationReader(final long aRelationID) {
            this();
            reInitialize(aRelationID);
        }

        /**
         * Re-initialize this class to read one way from the database that
         * contains the given node using ths still open database-connections.
         * @param aRelationID the id of the single relation we want fetch from the ways for the db.
         */
        public void reInitialize(final long aRelationID) {
            additionalFrom = "";
            additionalWhere = " AND r.id = " + aRelationID + " ";
//            this.minLat = Integer.MIN_VALUE;
//            this.minLon = Integer.MIN_VALUE;
//            this.maxLat = Integer.MAX_VALUE;
//            this.maxLon = Integer.MAX_VALUE;
        }


        /**
         * Creates a new instance that will read all relations that have tags matching the given condition.
         * @param aTags name-value -pairs
         * @param aOr combine the different values in aTags via AND or via OR.
         * @param aLike compare the names and values via SQL-"like" or literaly
         */
        public HsqldbCurrentRelationReader(
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
                    switch(aLike) {
                      case LIKE:  sb.append("(k like '").append(tag.getKey()).append("' AND v like '").append(tag.getValue()).append("') ");
                                  break;
                      default:    sb.append("(k = '").append(tag.getKey()).append("' AND v = '").append(tag.getValue()).append("') ");
                                  break;
                    }
                }
                this.additionalWhere = " AND r.id in ( SELECT id from current_relation_tags where " + sb.toString() + ")";
            }
        }


        /**
         * {@inheritDoc}
         */
        protected ResultSet createResultSet() {

            String sql;
//            if (minLat != Integer.MIN_VALUE || maxLat != Integer.MAX_VALUE
//             || minLon != Integer.MIN_VALUE || maxLon != Integer.MAX_VALUE) {
//                // if a bounding-box is given
//                sql = MessageFormat.format(SELECT_BOUNDINGBOX_SQL, new Object[]
//                                          {Double.toString(maxLat),
//                                           Double.toString(minLat),
//                                           Double.toString(maxLon),
//                                           Double.toString(minLon),
//                                           additionalWhere,
//                                           additionalFrom});
//            } else {
                sql = MessageFormat.format(SELECT_SQL, new Object[] {additionalWhere, additionalFrom});
//            }
            //LOG.log(Level.FINE, "SQL=" + sql);
            //System.out.println("SQL=" + sql);

            if (myGetMembersStmt == null)
                myGetMembersStmt = this.secondContext.prepareStatement(
                        "SELECT member_id, member_type, member_role from current_relation_members where id = ?");
            if (myGetTagsStmt == null)
                myGetTagsStmt =  this.secondContext.prepareStatement("SELECT k, v from current_way_tags where id = ?");

            return getResultSet(sql);
        }

        /**
         * {@inheritDoc}.
         * @see java.util.Iterator#next()
         */
        public Relation getNext() {
            long relationId;
            int version;
            Date timestamp;
            Relation relation;
            ResultSet resultSet = getResultSet();

            //----------- read the relation
            try {
                relationId = resultSet.getLong("id");
                version = resultSet.getInt("version");
                timestamp = new Date(resultSet.getTimestamp("timestamp").getTime());
            } catch (SQLException e) {
                throw new OsmosisRuntimeException("Unable to read the next relation.", e);
            }

            List<RelationMember> members = new LinkedList<RelationMember>();
            //----------- read the way-nodes
            try {
                myGetMembersStmt.setLong(1, relationId);
                ResultSet result = myGetMembersStmt.executeQuery();
                int count = 0;
                while (result.next()) {
                    long memberID = result.getLong("member_id");
//                  0=node 1=way 2=relation
                    int memberTypeStr = result.getInt("member_type");
                    String memberRole = result.getString("member_role");

                    EntityType memberType;
                    if (memberTypeStr == HsqldbDatabaseLoader.WAYTYPEID) {
                        memberType = EntityType.Way;
                    } else if (memberTypeStr == HsqldbDatabaseLoader.NODETYPEID) {
                        memberType = EntityType.Node;
                    } else if (memberTypeStr == HsqldbDatabaseLoader.RELATIONTYPEID) {
                        memberType = EntityType.Relation;
                    } else {
                        LOG.log(Level.SEVERE, "Relation " + relationId + " has a member "
                                + "of the unknown type '" + memberTypeStr + "'. Ignoring the member.");
                        continue;
                    }
                    members .add(new RelationMember(memberID, memberType, memberRole));
                    count++;
                }
                if (count == 0)
                    LOG.log(Level.FINE, "Relation " + relationId + " has no members!");
                result.close();
            } catch (SQLException e) {
                throw new OsmosisRuntimeException("Unable to read members for relation " + relationId + ".", e);
            }

            Collection<Tag> tags = new LinkedList<Tag>();
            //----------- read the way-tags
            try {
                myGetTagsStmt.setLong(1, relationId);
                ResultSet result = myGetTagsStmt.executeQuery();
                while (result.next()) {
                    String key = result.getString(1);
                    String value = result.getString(2);
                    tags.add(new Tag(key, value));
                }
                result.close();

            } catch (SQLException e) {
                throw new OsmosisRuntimeException("Unable to read relation tags.", e);
            }

            relation = new Relation(relationId, version, timestamp, null, 0);
            relation.getTags().addAll(tags);
            relation.getMembers().addAll(members);
            return relation;
        }
    };
