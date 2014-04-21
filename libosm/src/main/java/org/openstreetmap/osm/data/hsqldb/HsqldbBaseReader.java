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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;

/**
 * This is a base-class to do the house-keeping of database-connections
 * for {@link HsqldbCurrentNodeReader} and {@link org.openstreetmap.osm.data.mysql.ConstrainedCurrentWayReader}.
 * @param <T> te type we are a {@link ReleasableIterator} for.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public abstract class HsqldbBaseReader<T> implements ReleasableIterator<T> {

    /**
     * my logger for debug and error-output.
     */
    static final Logger LOG = Logger.getLogger(HsqldbBaseReader.class.getName());

    /**
     * We utilize 2 DatabaseContexts that represent
     * a logical connection to the database.
     * This is the first of them.
     */
    private DatabaseContext firstContext;

    /**
     * The ResultSet of the SQL-question asking for the WayNodes.
     */
    private ResultSet myResultSet;


    /**
     * The JDBC-Statement of the first
     * database-connection. Saved here for
     * reuse.
     */
    //private Statement firstStatement;


    /**
     * @return the result-set for the query this instance was initialized for.
     */
    protected abstract ResultSet createResultSet();

    /**
     * Returns a cached ResultSet if we already created one.
     * @return the result-set for the query this instance was initialized for.
     */
    protected ResultSet getResultSet() {
        if (myResultSet != null)
            return myResultSet;
        myResultSet = createResultSet();
        return myResultSet;
    }

    /**
     * This constructor creates an instance that will read ALL ways of the database.
     */
    public HsqldbBaseReader() {
        firstContext = new DatabaseContext();
    }

    /**
     * Helper to log a known bug with isClosed only once.
     */
    //private boolean isClosedLogged = false;

    /**
     * @param sql the query to ask
     * @return the result-set for tha query
     */
    protected ResultSet getResultSet(final String sql) {
        if (sql == null)
            throw new IllegalArgumentException("null sql given");
        LOG.log(Level.FINER, "SQL=" + sql);

//        if (firstStatement != null) {
//            try {
//                try {
//                    if (!firstStatement.isClosed()) {
//                        myResultSet =  firstStatement.executeQuery(sql);
//                        return myResultSet;
//                    } else {
//                        LOG.log(Level.WARNING, "existing statement is already closed, cannot create a new resultset from it");
//                    }
//                } catch (AbstractMethodError e) {
//                    if (!isClosedLogged) {
//                        LOG.log(Level.WARNING, "AbstractMethodError when trying to call isClosed(). This is a bug in the db-driver! Working around it...");
//                        isClosedLogged = true;
//                    }
//                    myResultSet =  firstStatement.executeQuery(sql);
//                } catch (IllegalAccessError e) {
//                    if (!isClosedLogged) {
//                        LOG.log(Level.WARNING, "IllegalAccessError when trying to call isClosed(). This is a bug in the db-driver! Working around it...");
//                        isClosedLogged = true;
//                    }
//                    myResultSet =  firstStatement.executeQuery(sql);
//                }
//            } catch (SQLException e) {
//                LOG.log(Level.WARNING, "Could not create new resultSet from existing statement, creating new statement\nsql=" + sql, e);
//                firstStatement = null;
//                myResultSet =  firstContext.executeStreamingQuery(sql);
//            }
//        }

        if (myResultSet == null)
        try {
            myResultSet =  firstContext.executeStreamingQuery(sql);
        } catch (RuntimeException e1) {
            throw new IllegalStateException("cannot create result-set for query: \n" + sql, e1);
        }
//        try {
//            firstStatement = myResultSet.getStatement();
//        } catch (SQLException e) {
//            LOG.log(Level.WARNING, "Could not get statement from resultSet for later reuse", e);
//        }
        return myResultSet;
    }

    /**
     * Has {@link #hasNext()} been called already since the last call to
     * {@link #next()}?
     */
    private boolean isNextTested = false;
    /**
     * If {@link #hasNext()} has been called since the last call to
     * {@link #next()}, did it return true?
     */
    private boolean hasNext = false;

    /**
     * {@inheritDoc}.
     * @see java.util.Iterator#next()
     */
    public T next() {
        isNextTested = false;
        return getNext();
    }

    /**
     * This method is the actual implementation
     * of {@link java.util.Iterator#next()}. It is called
     * by {@link #next()} after that function did
     * some housekeeping on our state.
     * @see java.util.Iterator#next()
     * @return the next element of the iterator or null.
     */
    public abstract T getNext();

    /**
     * ${@inheritDoc}.
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {

        if (isNextTested)
            return hasNext;

        ResultSet resultSet = getResultSet();
        isNextTested = true;
        hasNext = false;
        try {
            hasNext =  resultSet.next();
        } catch (SQLException e) {
            throw new OsmosisRuntimeException("Unable to check if where is a next way.", e);
        }
        return hasNext;
    }

    /**
     * {@inheritDoc}.
     * @see org.openstreetmap.osmosis.core.mysql.common.BaseTableReader#release()
     */
    public void release() {
        isNextTested = false;
        try {
            if (this.myResultSet != null/* && !this.myResultSet.isClosed()*/) {
                this.myResultSet.close();
                myResultSet.getStatement().close();
                this.myResultSet = null;
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Exception while closing resultset", e);
        }
    }

    /**
     * @return true if this iterator is ready to be reinitialized and used again.
     */
    public boolean isReleased() {
        return (this.myResultSet == null);
    }

    /**
     * {@inheritDoc}.
     */
    public void remove() {
        throw new IllegalArgumentException("remove is not supported!");
    }

}
