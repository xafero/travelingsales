package org.openstreetmap.osm.data.h2;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;

/**
 * This class has been taken from Osmosis-mysql and adapted to the H2 database.<br/>
 * This class manages the lifecycle of JDBC objects to minimise the risk of
 * connection leaks and to support a consistent approach to database access.
 *
 * @author Brett Henderson
 */
public class DatabaseContext {

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(DatabaseContext.class.getName());

    /**
     * did we load the driver already?
     */
    private static boolean           driverLoaded;

    /**
     * Our database-connection.
     */
    private Connection               connection;

    /**
     * This statement is used by streaming result sets. It is stored globally
     * here to allow it to remain open after a method return. It will be closed
     * during release or if a new streaming result set is created.
     */
    private Statement                streamingStatement;

    /**
     * Creates a new instance.
     */
    public DatabaseContext() {
        this.myDatabaseURL = getDefaultURL();
    }

    /**
     * Creates a new instance.
     * @param jdbcURL the URL we are using.
     */
    public DatabaseContext(final String jdbcURL) {
        this.myDatabaseURL = jdbcURL;
    }

    /**
     * Utility method for ensuring that the database driver is registered.
     */
    private static void loadDatabaseDriver() {
        if (!driverLoaded) {
            // Lock to ensure two threads don't try to load the driver at the same time.
            synchronized (DatabaseContext.class) {
                // Check again to ensure another thread hasn't loaded the driver
                // while we waited for the lock.
                try {
                    Class.forName("org.h2.Driver");

                } catch (ClassNotFoundException e) {
                    throw new OsmosisRuntimeException(
                            "Unable to find database driver.", e);
                }

                driverLoaded = true;
            }
        }
    }

    /**
     * If no database connection is open, a new connection is opened. The
     * database connection is then returned.
     *
     * @return The database connection.
     */
    private Connection getConnection() {
        if (connection == null) {

            loadDatabaseDriver();

            try {
                connection = DriverManager.getConnection(myDatabaseURL);

                connection.setAutoCommit(false);
                checkSchema(connection);

            } catch (SQLException e) {
                throw new OsmosisRuntimeException(
                        "Unable to establish a database connection to "
                        + myDatabaseURL +  ". because of: "
                        + e.getMessage(), e);
            }
        }

        return connection;
    }


    /**
     * the JDBC-URL of the  database.
     */
    private String myDatabaseURL;

    /**
     * @return the JDBC-URL of the default database.
     */
    public static String getDefaultURL() {
        return "jdbc:h2:file:"
                + System.getProperty("user.home").replace('\\', '/') + "/.openstreetmap/database;shutdown=true";
    }
    /**
     * The current schema-version this code was written for.
     */
    public static final int SCHEMAVERSION = 1;

    /**
     * Helper for {@link #checkSchema(Connection)} to
     * check only once.
     * (This variable is static because we only have one
     * database and cannot be configured to use any other one.)
     */
    private boolean schemaChecked = false;

    /**
     * Create or update the table-schema of
     * the database.
     * @throws SQLException in case something happens
     * @param con the database-connectiton opened
     */
    private void checkSchema(final Connection con) throws SQLException {

        if (schemaChecked)
            return;

        Statement stmt = con.createStatement();
        ResultSet results;
        try {
            results = stmt.executeQuery("SELECT version FROM schema_info");
        } catch (Exception e) {
            if (e instanceof SQLException && (
                    e.getMessage().contains("Table not found")
                    || e.getMessage().contains("Table \"SCHEMA_INFO\" not found")
                    )) {
                LOG.log(Level.INFO, "Creating database...");
            } else {
                LOG.log(Level.WARNING, "Cannot query H2-Database for schema-version. Assuming it does not exist yet.\n"
                        + "Creating database...", e);
            }

            stmt.close();
            createSchema(con);
            schemaChecked = true;
            return;
        }
        if (results.next()) {
            int version = results.getInt("version");
            if (version > getSchemaVersion()) {
                LOG.log(Level.WARNING, "H2-Database has schema-version " + version + "!\n"
                        + "This code was written for version " + getSchemaVersion());
                results.close();
                stmt.close();
                schemaChecked = true;
                return;
            }
            if (version < getSchemaVersion()) {
                LOG.log(Level.INFO, "H2-Database has old schema-version " + version + "!\n"
                        + "This code was written for version " + getSchemaVersion());
                results.close();
                if (version > getSchemaVersion()) {
                    LOG.log(Level.WARNING, "H2-Database has schema-version " + version + "!\n"
                            + "Recreating database... for version " + getSchemaVersion());
                    results.close();
                    stmt.close();
                    schemaChecked = true;
                    return;
                }
                stmt.close();
                schemaChecked = true;
                return;
            }
            LOG.log(Level.FINE, "H2-Database contains correct schema-version " + version + " (required is " + getSchemaVersion() + ")!");
            results.close();
            stmt.close();
            schemaChecked = true;
            return;
        } else {
            LOG.log(Level.WARNING, "H2-Database contains no schema-version!\n"
                    + "Re-creating database...");
            results.close();
            stmt.close();
            createSchema(con);
            schemaChecked = true;
            return;
        }
    }

    /**
     * @return the schema-version required.
     */
    public int getSchemaVersion() {
        return SCHEMAVERSION;
    }

    /**
     * Create or update the table-schema of
     * the database.
     * @throws SQLException in case something happens
     * @param con the database-connectiton opened
     */
    protected void createSchema(final Connection con) throws SQLException {

            Statement stmt = con.createStatement();

            stmt.executeUpdate(
                    "DROP TABLE schema_info IF EXISTS CASCADE;"
                  + "CREATE CACHED TABLE schema_info ("
                  + "         version int default NULL"
                  + "     )");

            stmt.execute(
                    "INSERT INTO schema_info VALUES (" + getSchemaVersion() + ")");

            stmt.execute(
                      "DROP TABLE current_nodes IF EXISTS CASCADE;"
                    + "CREATE CACHED TABLE current_nodes (\n"
                    + "  id        BIGINT    NOT NULL,\n"
                    + "  latitude  int       NOT NULL,\n"
                    + "  longitude int       NOT NULL,\n"
                    + "  tags      VARCHAR   NOT NULL,\n"
                    + "  timestamp datetime  NOT NULL,\n"
                    + "  tile      BIGINT    NOT NULL,\n"
                    + "  PRIMARY KEY  (id));\n"
                    + "CREATE INDEX current_nodes_lonlat_idx ON current_nodes (latitude, longitude);\n"
                    + "CREATE INDEX current_nodes_tile_idx ON current_nodes (tile);\n");

            stmt.execute(
                      "DROP TABLE current_relation_members IF EXISTS CASCADE;"
                    + "CREATE CACHED TABLE current_relation_members (\n"
                    + "  id          BIGINT               NOT NULL,\n"
                    + "  member_type TINYINT default 0    NOT NULL,\n"// 0=node 1=way 2=relation
                    + "  member_id   bigint               NOT NULL,\n"
                    + "  member_role varchar default \'\' NOT NULL,\n"
                    + "  PRIMARY KEY  (id,member_type,member_id,member_role));\n"
                    + "CREATE INDEX current_relation_members_member_idx ON current_relation_members (member_type, member_id);\n");

            stmt.execute(
                      "DROP TABLE current_relation_tags IF EXISTS CASCADE;"
                    + "CREATE CACHED TABLE current_relation_tags (\n"
                    + "  id bigint              NOT NULL,\n"
                    + "  k varchar default \'\' NOT NULL,\n"
                    + "  v varchar default \'\' NOT NULL);\n"
                    + "CREATE INDEX current_relation_tags_id_idx ON current_relation_tags (id);\n"
                    + "CREATE INDEX current_relation_tags_k_idx ON current_relation_tags (k);\n");

            stmt.execute(
                      "DROP TABLE current_relations IF EXISTS CASCADE;"
                    + "CREATE CACHED TABLE current_relations (\n"
                    + "  id        BIGINT   NOT NULL,\n"
                    + "  timestamp datetime NOT NULL,\n"
                    + "  PRIMARY KEY  (id)\n"
                    + ");");

            stmt.execute(
                      "DROP TABLE current_way_nodes IF EXISTS CASCADE;"
                    + "CREATE CACHED TABLE current_way_nodes (\n"
                    + "  id          bigint NOT NULL,\n"
                    + "  node_id     bigint NOT NULL,\n"
                    + "  sequence_id int NOT NULL,\n"
                    + "  PRIMARY KEY  (id,sequence_id));"
                    + "CREATE INDEX current_way_nodes_node_idx ON current_way_nodes (node_id);\n");

            stmt.execute(
                      "DROP TABLE current_way_tags IF EXISTS CASCADE;"
                    + "CREATE CACHED TABLE current_way_tags (\n"
                    + "  id bigint              NOT NULL,\n"
                    + "  k varchar default \'\' NOT NULL,\n"
                    + "  v varchar default \'\' NOT NULL);\n"
                    + "CREATE INDEX current_way_tags_od_idx ON current_way_tags (id);\n"
                    + "CREATE INDEX current_way_tags_k_idx ON current_way_tags (k);\n");

            stmt.execute(
                      "DROP TABLE current_ways IF EXISTS CASCADE;"
                    + "CREATE CACHED TABLE current_ways (\n"
                    + "  id        BIGINT   NOT NULL,\n"
                    + "  timestamp datetime NOT NULL,\n"
                    + "  PRIMARY KEY  (id)\n"
                    + ");");


    }

    /**
     * Executes a sql statement against the database.
     *
     * @param sql
     *            The sql statement to be invoked.
     */
    public void executeStatement(final String sql) {
        try {
            Statement statement;

            statement = getConnection().createStatement();

            statement.execute(sql);

        } catch (SQLException e) {
            throw new OsmosisRuntimeException("Unable to execute statement.\nSQL=" + sql, e);
        }
    }

    /**
     * Creates a new database prepared statement.
     *
     * @param sql
     *            The statement to be created.
     * @return The newly created statement.
     */
    public PreparedStatement prepareStatement(final String sql) {
        try {
            PreparedStatement preparedStatement;

            preparedStatement = getConnection().prepareStatement(sql);

            return preparedStatement;

        } catch (SQLException e) {
            throw new OsmosisRuntimeException(
                    "Unable to create database prepared statement.\n"
                    + "SQL=" + sql + "\n"
                    + "DB-URL=" + this.myDatabaseURL + "\n"
                    + "Schema-Version=" + getSchemaVersion(), e);
        }
    }

    /**
     * Creates a new database statement that is configured so that any result
     * sets created using it will stream data from the database instead of
     * returning all records at once and storing in memory.
     * <p>
     * If no input parameters need to be set on the statement, use the
     * executeStreamingQuery method instead.
     *
     * @param sql
     *            The statement to be created. This must be a select statement.
     * @return The newly created statement.
     */
    public PreparedStatement prepareStatementForStreaming(final String sql) {
        try {
            PreparedStatement statement;

            // Create a statement for returning streaming results.
            statement = getConnection().prepareStatement(sql,
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            statement.setFetchSize(Integer.MIN_VALUE);

            return statement;

        } catch (SQLException e) {
            throw new OsmosisRuntimeException(
                    "Unable to create streaming resultset statement.\nSQL=" + sql, e);
        }
    }

    /**
     * Creates a result set that is configured to stream results from the
     * database.
     *
     * @param sql
     *            The query to invoke.
     * @return The result set.
     */
    public ResultSet executeStreamingQuery(final String sql) {
        if (sql == null) {
            throw new IllegalArgumentException("null sql given");
        }
        try {
            ResultSet resultSet;

            // Close any existing streaming statement.
//            if (streamingStatement != null) {
//                try {
//                    streamingStatement.close();
//
//                } catch (SQLException e) {
//                    // Do nothing.
//                    LOG.log(Level.FINEST, "Ignoring exception while closing jdbc-statement", e);
//                }
//
//                streamingStatement = null;
//            }

            // Create a statement for returning streaming results.
            streamingStatement = getConnection().createStatement(
                    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            if (streamingStatement == null) {
                throw new NullPointerException("getConnection().createStatement() returned no statement");
            }
            if (streamingStatement.getConnection() == null) {
                throw new NullPointerException("getConnection().createStatement() returned a statement with no connection");
            }

            //streamingStatement.setFetchSize(Integer.MIN_VALUE);

            try {
                resultSet = streamingStatement.executeQuery(sql);
            } catch (NullPointerException e) {
                // work around a condition in H2 where the connection
                // inside the statement is set to null during executeQuery()
                if (streamingStatement.isClosed() || streamingStatement.getConnection() == null) {
                    LOG.log(Level.SEVERE,
                            "streamingStatement lost it's db-connection. Retrying once.");
                    streamingStatement = getConnection().createStatement(
                            ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                    resultSet = streamingStatement.executeQuery(sql);
                } else {
                    throw new OsmosisRuntimeException(
                            "Unable to create streaming resultset because of a RuntimeException.\nSQL=" + sql, e);
                }
            }

            return resultSet;

        } catch (SQLException e) {
            throw new OsmosisRuntimeException(
                    "Unable to create streaming resultset.\nSQL=" + sql, e);
        } catch (RuntimeException e) {
            throw new OsmosisRuntimeException(
                    "Unable to create streaming resultset because of a RuntimeException.\nSQL=" + sql, e);
        }
    }

    /**
     * Commits any outstanding transaction.
     */
    public void commit() {
        if (connection != null) {
            try {
                connection.commit();
            } catch (SQLException e) {
                throw new OsmosisRuntimeException("Unable to commit changes.",
                        e);
            }
        }
    }

    /**
     * Releases all database resources. This method is guaranteed not to throw
     * transactions and should always be called in a finally block whenever this
     * class is used.
     */
    public void release() {
        if (streamingStatement != null) {
            try {
                streamingStatement.close();

            } catch (SQLException e) {
                // Do nothing.
                LOG.log(Level.FINEST, "Ignoring exception while closing jdbc-statement", e);
            }

            streamingStatement = null;
        }
        if (connection != null) {
            try {
                connection.close();

            } catch (SQLException e) {
                // Do nothing.
                LOG.log(Level.FINEST, "Ignoring exception while closing jdbc-connection", e);
            }

            connection = null;
        }
    }

    /**
     * Enforces cleanup of any remaining resources during garbage collection.
     * This is a safeguard and should not be required if release is called
     * appropriately.
     * @throws Throwable anything may be thrown
     */
    @Override
    protected void finalize() throws Throwable {
        release();

        super.finalize();
    }
}
