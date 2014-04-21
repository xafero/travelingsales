package org.openstreetmap.osm.data.h2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public interface IConnection {

    /**
     * Close the underlying connection.
     * This method is ONLY to by used by {@link H2DataSet#returnConnection(MyConnection)}.
     */
    void close();

    /**
     * @see Connection#isClosed()
     * @return true if the underlying connection is closed.
     * @throws SQLException if something bad happens
     */
    boolean isClosed() throws SQLException;

    /**
     * @return the addWayNodeStmt the prepared statement used by {@link H2DataSet#addNode(Node)}
     * @throws SQLException if the prepared statement cannot be created.
     */
    PreparedStatement getAddNodeStmt() throws SQLException;

    /**
     * @return the addWayNodeStmt the prepared statement used by {@link H2DataSet#addWay(Way)}
     * @throws SQLException if the prepared statement cannot be created.
     */
    PreparedStatement getAddWayNodeStmt() throws SQLException;

    /**
     * @return the addWayStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    PreparedStatement getAddWayStmt() throws SQLException;

    /**
     * @return the deleteWaysNodesStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    PreparedStatement getDeleteWaysNodesStmt()
            throws SQLException;

    /**
     * @return The prepared statement used by {@link H2DataSet#getNodeByID(long)}.
     * @throws SQLException if the prepared statement cannot be created.
     */
    PreparedStatement getGetNodeByIDStmt() throws SQLException;

    /**
     * @return the getWaysForNodeStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getGetWaysForNodeStmt()
            throws SQLException;

    /**
     * @return the getWayByIDStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    PreparedStatement getGetWayByIDStmt() throws SQLException;

    /**
     * @return the getNodesForWayStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    PreparedStatement getGetNodesForWayStmt()
            throws SQLException;

    /**
     * @return the getNodeByAreaStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    PreparedStatement getGetNodeByAreaStmt() throws SQLException;

    /**
     * @return the deleteNodeStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getDeleteNodeStmt() throws SQLException;

    /**
     * @return the deleteWayStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getDeleteWayStmt() throws SQLException;

    /**
     * @return the deleteRelationStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getDeleteRelationStmt()
            throws SQLException;

    /**
     * @return the addRelationStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getAddRelationStmt() throws SQLException;

    /**
     * @return the addRelationMemberStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getAddRelationMemberStmt()
            throws SQLException;

    /**
     * @return the deleteRelationMemberStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getDeleteRelationMemberStmt()
            throws SQLException;

    /**
     * @return the getMembersForRelation
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getGetRelationByID() throws SQLException;

    /**
     * @return the getMembersForRelation
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getGetMembersForRelation()
            throws SQLException;

    /**
     * @return the getRelationsForNodeStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getGetRelationsForNodeStmt()
            throws SQLException;

    /**
     * @return the getRelationsForWayStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getGetRelationsForWayStmt()
            throws SQLException;

    /**
     * @return the getGetWayByTagStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getGetWayByTagStmt() throws SQLException;

    /**
     * @return the getGetWayByTagStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getGetNodesByTagStmt()
            throws SQLException;

    /**
     * @return the getGetWayByTagStmt
     * @throws SQLException if the prepared statement cannot be created.
     */
    public abstract PreparedStatement getGetRelByTagStmt() throws SQLException;

    public abstract java.sql.Connection getConnection()  throws SQLException;

}