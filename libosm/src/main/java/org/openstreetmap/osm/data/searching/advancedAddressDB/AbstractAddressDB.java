/**
 * AbstractAddressDB.java
 * created: 31.01.2009
 * (c) 2007 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of libosm by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
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
package org.openstreetmap.osm.data.searching.advancedAddressDB;

//other imports
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

//automatically created logger for debug and error -output
import java.util.logging.Level;
import java.util.logging.Logger;

//automatically created propertyChangeListener-Support
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.h2.DatabaseContext;
import org.openstreetmap.osm.data.searching.IAddressDBPlaceFinder;

/**
 * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * AbstractAddressDB.java<br/>
 * created: 31.01.2009 <br/>
 *<br/><br/>
 * This class contains all the database-code for the AdvancedAddressDBPlaceFinder.
 * <img src="http://apps.sourceforge.net/mediawiki/travelingsales/index.php?title=Image:AdvancedAddressDBPlaceFinder.png"/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
abstract class AbstractAddressDB implements IAddressDBPlaceFinder {

    /**
     * Helper-class to commit changes asynchronously
     * to not block the core that does an import
     * with addresses.
     */
    private final class AsyncCommitThread extends Thread {
        /**
         * {@inheritDoc}.
         */
        public void run() {
            try {
                Thread.sleep(PRECOMMITSLEEPMILLIS);
            } catch (InterruptedException e1) {
                LOG.log(Level.FINEST, "Commit-thread has been interrupted.");
            }
            try {
                getDatabase().commit();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Cannot commit AddressDBPlaceFinder-Database.");
            }
        }
    }

    /**
     * @param aDirName the directory where we store all tiles.
     */
    protected AbstractAddressDB(final File aDirName) {
        super();
        myDBDirName = aDirName.getAbsolutePath();
    }

    /**
     * Use the configured default-directory.
     */
    protected AbstractAddressDB() {
        super();
    }

    /**
     * Milliseconds for the thread started by {@link #doCommitAsync()}
     * before actually commiting ({@value #PRECOMMITSLEEPMILLIS} ms).
     */
    private static final int PRECOMMITSLEEPMILLIS = 60 * 1000;
    /**
     * Do not return more thene this many results
     * to a query. (e.g. looking for a large city
     * without specifying the street.)
     */
    protected static final int MAXRESULTS = 20;
    /**
     * The map we search on.
     */
    private IDataSet myMap;

    /**
     * Automatically created logger for debug and error-output.
     */
    protected static final Logger LOG = Logger
                .getLogger(AdvancedAddressDBPlaceFinder.class.getName());
    /**
     * The database we store cities and
     * roads in.
     */
    private static DatabaseContext myDatabase = null;
    /**
     *  Used by {@link #doCommitAsync()}.
     */
    private Thread commitThread = null;

    /**
     * support for firing PropertyChangeEvents.
     * (gets initialized only if we really have listeners)
     */
    private volatile PropertyChangeSupport myPropertyChange = null;

    /**
     * The directory where we store all tiles.
     */
    private String myDBDirName;

    /**
     * @return see {@link #myPropertyChange}.
     */
    protected PropertyChangeSupport getPropertyChange() {
        return myPropertyChange;
    }

    /**
     * @return the directory where we store all tiles.
     */
    private String getDBDirName() {
        // we use the directory of the tiles from the FileTileDataSet.
        if (this.myDBDirName == null) {
            this.myDBDirName = Settings.getInstance().get("map.dir",
                    Settings.getInstance().get("tiledMapCache.dir", System.getProperty("user.home")
                            + File.separator + ".openstreetmap"
                            + File.separator + "map" + File.separator));
        }
        return myDBDirName;
    }

    /**
     * Commit our database asynchronously.<br/>
     * Do nothing if we are already commiting.
     */
    protected void doCommitAsync() {
        if (commitThread == null || !commitThread.isAlive()) {
            commitThread = new AsyncCommitThread();
            commitThread.start();
        }
    }

    /**
     * If we have a database-connection,
     * commit and properly close it.
     */
    public static void shutdown() {
        if (myDatabase != null) {
            myDatabase.commit();
            myDatabase.executeStatement("SHUTDOWN COMPACT");
            myDatabase.release();
            myDatabase = null;
        }
    }

    /**
     * @return This is the database where we store index.
     */
    protected DatabaseContext getDatabase() {
        if (myDatabase == null) {
            myDatabase =     new DatabaseContext("jdbc:h2:file:" + getDBDirName() + "streets") {

                /**
                 * the schema-version required.
                 */
                private static final int ADDRESSDBSCHEMAVERSION = 4;

                /**
                 * @return the schema-version required.
                 */
                @Override
                public int getSchemaVersion() {
                    LOG.log(Level.FINE, "returning required schema-version of " + ADDRESSDBSCHEMAVERSION);
                    return ADDRESSDBSCHEMAVERSION;
                }

                /**
                 * Create or update the table-schema of
                 * the database.
                 * @throws SQLException in case something happens
                 * @param con the database-connection opened
                 */
                @Override
                protected void createSchema(final Connection con) throws SQLException {

                        Statement stmt = con.createStatement();

                        LOG.log(Level.INFO, "Creating new address-database. in " + getDBDirName());

                        stmt.executeUpdate(
                                "DROP TABLE schema_info IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE schema_info ("
                              + "         version int default NULL"
                              + "     )");

                        stmt.execute(
                                "INSERT INTO schema_info VALUES (" + ADDRESSDBSCHEMAVERSION + ")");

                        stmt.execute(
                                "DROP TABLE city IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE city (\n"
                              + "  id        BIGINT    NOT NULL,\n"
                              + "  idType      INT     NOT NULL,\n" //0=way 1=node, 2=relation
                              + "  defaultName varchar NOT NULL,\n"
                              + "  centerLat BIGINT    NOT NULL,\n" // center of the city
                              + "  centerLon BIGINT    NOT NULL,\n"
                              + "  radius    BIGINT    NOT NULL,\n" // size of bounding-box
                              + "  PRIMARY KEY  (id, idType));\n"
                              + "CREATE INDEX city_name_idx ON city (defaultName);\n");

                        stmt.execute(
                                "DROP TABLE suburb IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE suburb (\n"
                              + "  id        BIGINT    NOT NULL,\n"
                              + "  idType      INT     NOT NULL,\n" //0=way 1=node, 2=relation
                              + "  defaultName varchar NOT NULL,\n"
                              + "  centerLat BIGINT    NOT NULL,\n" // center of the city
                              + "  centerLon BIGINT    NOT NULL,\n"
                              + "  radius    BIGINT    NOT NULL,\n" // size of bounding-box
                              + "  PRIMARY KEY  (id, idType));\n"
                              + "CREATE INDEX suburb_name_idx ON suburb (defaultName);\n");

                        stmt.execute(
                                "DROP TABLE street IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE street (\n"
                              + "  id        BIGINT    NOT NULL,\n"
                              + "  defaultName varchar NOT NULL,\n"
                              + "  centerLat BIGINT    NOT NULL,\n" // center of the city
                              + "  centerLon BIGINT    NOT NULL,\n"
                              + "  radius    BIGINT    NOT NULL,\n" // size of bounding-box
                              + "  PRIMARY KEY  (id));\n"
                              + "CREATE INDEX street_name_idx ON street (defaultName);\n");
                }

            };
        }

        return myDatabase;
    }

    /**
     * Returned value may be null if we never had listeners.
     * @return Our support for firing PropertyChangeEvents
     */
    protected PropertyChangeSupport getPropertyChangeSupport() {
        return myPropertyChange;
    }

    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     *
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property.  The listener
     * will be invoked only when a call on firePropertyChange names that
     * specific property.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The PropertyChangeListener to be removed
     */
    public final void removePropertyChangeListener(final String propertyName, final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(propertyName,
                    listener);
        }
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     *
     * @param listener  The PropertyChangeListener to be removed
     */
    public synchronized void removePropertyChangeListener(final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(listener);
        }
    }

    /**
     * @return Returns the map.
     * @see #myMap
     */
    public IDataSet getMap() {
        return myMap;
    }

    /**
     * ${@inheritDoc}.
     */
    public void setMap(final IDataSet pMap) {
        if (pMap == null)
            throw new IllegalArgumentException("null map given");

        this.myMap = pMap;
        //indexDatabase(myMap);
    }

    /**
     * Checkpoint the database.
     */
    public void checkpointDB() {
         getDatabase().executeStatement("CHECKPOINT");
    }
}
