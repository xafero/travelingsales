/**
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
 *
 *  Created: 10.11.2007
 */
package org.openstreetmap.osm.data;

import java.io.File;
import java.lang.ref.SoftReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.h2.DatabaseContext;
import org.openstreetmap.osm.data.searching.NameHelper;
import org.openstreetmap.osm.io.FileLoader;

import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlWriter;

/**
 * This dataset used files that store all nodes of one
 * tile and all their ways.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class FileTileDataSet implements IDataSet {

    /**
     * name of this dataSet. May not be null.
     * Defaults to the empty name.
     * This is used by ${@link LODDataSet}
     * to have different names and this
     * sub-directories for lower-resolution
     * maps.
     */
    private String myAdditionalPath = "";

    /**
     * If 0 we save one file for
     * each tile-number.
     * If 1 the last digit is ignored
     * and we save 1 tile for 10
     * tile-numners.
     * If 2 the last digit is ignored
     * and we save 1 tile for 100
     * tile-nmners.
     */
    private int myCombineTiles = 0;

    /**
     * Create a FileTileDataSet,
     * that saved in the default-path.
     */
    public FileTileDataSet() {
        this(null, 2);
    }

    /**
     * Create a FileTileDataSet,
     * that saved in the a subdirectory of
     * the default-path.
     * This allowes to have more then one
     * FileTileDataSet with different data.
     * @param additionalPath name of this dataSet. May be null for default.
     * @param aCombineTiles remove this many digits from the tile-number to
     *        create the filename
     */
    public FileTileDataSet(final String additionalPath,
                           final int aCombineTiles) {
        this.myAdditionalPath = additionalPath;
        this.myCombineTiles = aCombineTiles;
    }

    /**
     * This Thread will look in {@link FileTileDataSet#mySaveQueue} and
     * save all tiles in there.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private final class SavingThread extends Thread {
        /**
         * Sleep this many milliseconds if there is nothing
         * to be saved.
         */
        private static final int SLEEPMILLISECONDS = 100;

        /**
         * Do our work.
         * @see java.lang.Thread#run()
         */
        public void run() {
            try {
                final int maxCountIdle = 10; // stop the thread when reaching these many sleep-cycles without work to do
                int countIdle = 0;
                while (true) {
                    MemoryDataSet tile = null;
                    long aTileNumber = -1;
                    synchronized (mySaveQueue) {
                        if (!mySaveQueue.isEmpty()) {
                            aTileNumber = mySaveQueue.keySet().iterator().next();
                            tile = mySaveQueue.remove(aTileNumber);
                        }
                    }
                    if (tile == null) {
                        countIdle++;
                        if (countIdle == maxCountIdle)
                            return;
                        if (countIdle == maxCountIdle - 1)
                            LOG.log(Level.FINE, "runtimes=" + getStatistics());
                        sleep(SLEEPMILLISECONDS);
//                        if (mySaveQueue.size() > 0)
//                            LOG.log(Level.FINE, mySaveQueue.size() + " tiles to save.");
                        continue;
                    }
                    countIdle = 0;
                    //LOG.log(Level.FINE, "Saving tile " + aTileNumber + " to disk! " + mySaveQueue.size() + " other tiles to save.");
                    try {
                        XmlWriter writer = new XmlWriter(getTileFileName(aTileNumber), CompressionMethod.None);

                        for (Iterator<Node> nodes = tile.getNodes(Bounds.WORLD); nodes.hasNext();) {
                            writer.process(new NodeContainer(nodes.next()));
                        }
                        for (Iterator<Way> ways = tile.getWays(Bounds.WORLD); ways.hasNext();) {
                            writer.process(new WayContainer(ways.next()));
                        }
                        for (Iterator<Relation> relations = tile.getRelations(Bounds.WORLD); relations.hasNext();) {
                            writer.process(new RelationContainer(relations.next()));
                        }
                        writer.complete();
                    } catch (ConcurrentModificationException e) {
//                        LOG.log(Level.INFO, "ConcurrentModification in Saving-Thread in FileTileDataSet for tile "
//                                + aTileNumber + ". Re-queuing that tile to be saved later.");
                        synchronized (mySaveQueue) {
                            // requeue only if a newer version of it
                            // is not already queued
                            mySaveQueue.putIfAbsent(aTileNumber, tile);
                        }
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Saving-Thread in FileTileDataSet has a problem saving tile "
                                + aTileNumber + " to disk!", e);
                    }
                }
            } catch (InterruptedException e) {
                LOG.log(Level.INFO, "Saving-Thread in FileTileDataSet has been interrupted and will stop.");
            }
        }
    }

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(FileTileDataSet.class.getName());



    /**
     * This is the memory-sensitive cache of tiles.
     */
    private Map<Long, SoftReference<MemoryDataSet>> myTileCache = new java.util.concurrent.ConcurrentHashMap<Long, SoftReference<MemoryDataSet>>();

    /**
     * The given tile-number is already shortened acording
     * to ${@link #myCombineTiles}.
     * @param aTileNumber a tile-number
     * @return the file where that tile should be saved on disk
     */
    private File getTileFileName(final long aTileNumber) {
        String dir = getTileDirName();
        File dirF = new File(dir);
        if (!dirF.exists())
            if (!dirF.mkdirs())
                LOG.log(Level.SEVERE, "cannot create directory " + dirF.getAbsolutePath() + " to store map-tiles in");
        File f = new File(dir + aTileNumber + ".osm");
        return f;
    }

    /**
     * @return the directory where we store all tiles.
     */
    private String getTileDirName() {
        String defaultDir = Settings.getInstance().get("map.dir",
                Settings.getInstance().get("tiledMapCache.dir", System.getProperty("user.home")
                + File.separator + ".openstreetmap" + File.separator + "map" + File.separator));
        if (this.myAdditionalPath == null || this.myAdditionalPath.length() == 0)
            return defaultDir;
        return defaultDir + this.myAdditionalPath + File.separator;
    }

    /**
     * Cache for myAllTileIDs().
     */
    private Set<Long> myAllTileIDs = null;

    /**
     * The returned tile-numbers are already shortened acording
     * to ${@link #myCombineTiles}.
     * @return the ids of all tiles on disk
     */
    private Collection<Long> getAllTileIDs() {
        if (myAllTileIDs != null)
            return myAllTileIDs;

        String dir = getTileDirName();
        File dirF = new File(dir);
        File[] files = dirF.listFiles();
        Set<Long> retval = new HashSet<Long>();
        if (files != null)
        for (File name : files) {
            String name2 = name.getName().toLowerCase();
            if (name2.endsWith(".osm"))
                retval.add(Long.parseLong(name2.replace(".osm", "")));
        }
        myAllTileIDs = retval;
        return retval;
    }

    /**
     * Load a tile from disk.
     * @param aTileNumber the number of the tile
     * @return all data of the tile or null.
     */
    protected MemoryDataSet getTile(final long aTileNumber) {

        // look in the cache
        SoftReference<MemoryDataSet> tileRef = myTileCache.get(aTileNumber);
        if (tileRef != null) {
            MemoryDataSet tile = tileRef.get();
            if (tile != null)
                return tile;
        } else {
           myTileCache.remove(aTileNumber);
        }
//      look in the saving-cache
        synchronized (mySaveQueue) {
            MemoryDataSet tile = mySaveQueue.get(aTileNumber);
            if (tile != null) {
//              MemoryDataSet is save against ConcurrentModificationException
                return tile;
            }
        }

        //load tile from disk
        long start = System.currentTimeMillis();
        File tileFile = getTileFileName(aTileNumber);
        if (!tileFile.exists()) {
            return null;
        }
        FileLoader loader = new FileLoader(tileFile);
        MemoryDataSet tile = loader.parseOsm();
        myTileCache.put(aTileNumber, new SoftReference<MemoryDataSet>(tile));
        addStatisticalTime(start, "getTile(from disk)");
        return tile;
    }

    /**
     * Save a new or updated tile.
     * The given tile-number is already shortened acording
     * to ${@link #myCombineTiles}.
     * @param aTileNumber the number of the tile
     * @param tile all data of the tile
     */
    protected void saveTile(final long aTileNumber, final MemoryDataSet tile) {
        if (myAllTileIDs != null)
            myAllTileIDs.add(aTileNumber);

        //LOG.log(Level.FINE, "queuing tile " + aTileNumber + " for saving to disk!");
        synchronized (mySaveQueue) {
            // we throw out old versions of the tile
            // that are not saved yet here too
            mySaveQueue.put(aTileNumber, tile);
        }

        if (mySavingThread == null || !mySavingThread.isAlive()) {
            LOG.log(Level.FINE, "Starting new SavingThread after queuing tile " + aTileNumber + " for saving to disk!");
            mySavingThread = new SavingThread();
            mySavingThread.setDaemon(true);
            mySavingThread.setPriority(Thread.MIN_PRIORITY);
            mySavingThread.start();
        }
        myTileCache.put(aTileNumber, new SoftReference<MemoryDataSet>(tile));
        //LOG.log(Level.FINER, "Tile " + aTileNumber + " queued for saving to disk!");
    }
    /**
     * This map contains all tiles that are currently being saved.
     * Access the queue only whils synchronized to it!
     */
    private ConcurrentMap<Long, MemoryDataSet> mySaveQueue = new ConcurrentHashMap<Long, MemoryDataSet>();

    /**
     * This thread will look in {@link #mySaveQueue} and
     * save all tiles there to disk.
     */
    private Thread mySavingThread = null;

    /**
     * Helper to calculate the tile-number for a node.
     */
    private TileCalculator myTileCalculator = new TileCalculator();

    /**
     * The returned tile-number is already shortened acording
     * to ${@link #myCombineTiles}.
     * @param node the node to examine
     * @return the tile-number for the given node
     */
    protected long getTileNumber(final Node node) {
        if (node == null)
            throw new IllegalArgumentException("null node given");

        long origTile =  myTileCalculator.calculateTile(node.getLatitude(), node.getLongitude());
        final int decadic = 10; // numbers are to the base of 10 in later file-names.
        for (int i = 0; i < this.myCombineTiles; i++)
            origTile = origTile / decadic;
        return origTile;
    }

    /**
     * @param node the node to get the tile for
     * @return the MemoryDataSet for that tile.
     */
    private MemoryDataSet getOrCreateTile(final Node node) {
        if (node == null) {
            throw new IllegalArgumentException("null node given");
        }

        long tileNr = getTileNumber(node);
        MemoryDataSet tile = getTile(tileNr);
        if (tile == null) {
            tile = new MemoryDataSet();
        }
        return tile;
    }

    /**
     * @param way the way who's nodes to get the tiles for
     * @return the MemoryDataSet for that tiles.
     */
    private Map<Long, MemoryDataSet> getOrCreateTiles(final Way way) {
        if (way == null)
            throw new IllegalArgumentException("null way given");

        Map<Long, MemoryDataSet> tiles = new HashMap<Long, MemoryDataSet>();
        for (WayNode nodes : way.getWayNodes()) {
            Node n = getNodeByID(nodes.getNodeId());
            if (n != null) {
                long tileNr = getTileNumber(n);
                if (!tiles.containsKey(tileNr))
                    tiles.put(tileNr, getOrCreateTile(n));
            }
        }
        return tiles;
    }

    /**
     * @param rel the relation who's nodes to get the tiles for
     * @return the MemoryDataSet for that tiles.
     */
    private Map<Long, MemoryDataSet> getOrCreateTiles(final Relation rel) {
        if (rel == null)
            throw new IllegalArgumentException("null relation given");

        Map<Long, MemoryDataSet> tiles = new HashMap<Long, MemoryDataSet>();
        for (RelationMember member : rel.getMembers()) {
            long id = member.getMemberId();
            switch (member.getMemberType()) {
            case Node :
                Node n = getNodeByID(id);
                if (n != null) {
                    long tileNr = getTileNumber(n);
                    if (!tiles.containsKey(tileNr))
                        tiles.put(tileNr, getOrCreateTile(n));
                }
                break;
            case Relation:
                Relation subRel = getRelationByID(id);
                if (subRel != null)
                    tiles.putAll(getOrCreateTiles(subRel));
                break;
            case Way:
                Way way = getWaysByID(id);
                if (way != null) {
                    tiles.putAll(getOrCreateTiles(way));
                }
                break;
            default: throw new IllegalArgumentException("Relation contains an unknown entity-type '"
                    + member.getMemberType() + "' in role '" + member.getMemberRole() + "'");
            }
        }
        return tiles;
    }


    /**
     * This is the database where we store indice.
     */
    private DatabaseContext myIndexDB;

    /**
     * @return This is the database where we store indice.
     */
    protected DatabaseContext getIndexDatabase() {
        if (myIndexDB == null) {
            myIndexDB =     new DatabaseContext("jdbc:h2:file:" + getTileDirName() + "index") {

                /**
                 * @return the schema-version required.
                 */
                @Override
                public int getSchemaVersion() {
                    LOG.log(Level.FINE, "returning required schema-version of " + DatabaseContext.SCHEMAVERSION);
                    return DatabaseContext.SCHEMAVERSION;
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

                        stmt.executeUpdate(
                                "DROP TABLE schema_info IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE schema_info ("
                              + "         version int default NULL"
                              + "     )");

                        stmt.execute(
                                "INSERT INTO schema_info VALUES (" + DatabaseContext.SCHEMAVERSION + ")");

                        stmt.execute(
                                "DROP TABLE nodes_index IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE nodes_index (\n"
                              + "  id        BIGINT    NOT NULL,\n"
                              + "  tile      BIGINT    NOT NULL,\n"
                              + "  PRIMARY KEY  (id));\n"
                              + "CREATE INDEX nodes_tile_idx ON nodes_index (tile);\n");

                        stmt.execute(
                                "DROP TABLE ways_index IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE ways_index (\n"
                              + "  id        BIGINT    NOT NULL,\n"
                              + "  tile      BIGINT    NOT NULL,\n"
                              + "  PRIMARY KEY  (id, tile));\n"
                              + "CREATE INDEX ways_tile_idx ON ways_index (tile);\n");

                        stmt.execute(
                                "DROP TABLE relations_index IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE relations_index (\n"
                              + "  id        BIGINT    NOT NULL,\n"
                              + "  tile      BIGINT    NOT NULL,\n"
                              + "  PRIMARY KEY  (id, tile));\n"
                              + "CREATE INDEX relations_tile_idx ON relations_index (tile);\n");

                        // node that the "name"-tag is saved in normalized form
                        // see NameHelper.normalizeName(String) for details
                        // note that the index-database stores the names
                        // normalized but the tile-files do not!
                        // thus the unchanged name-tag is returned
                        stmt.execute(
                                "DROP TABLE node_tags IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE node_tags (\n"
                              + "  id bigint              NOT NULL,\n"
                              + "  k varchar default \'\' NOT NULL,\n"
                              + "  v varchar default \'\' NOT NULL,\n"
                              + "  PRIMARY KEY  (id, k));\n"
                              + "CREATE INDEX node_tags_k_idx ON node_tags (k);\n");

                        // node that the "name"-tag is saved in normalized form
                        // see NameHelper.normalizeName(String) for details
                        // note that the index-database stores the names
                        // normalized but the tile-files do not!
                        // thus the unchanged name-tag is returned
                        stmt.execute(
                                "DROP TABLE way_tags IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE way_tags (\n"
                              + "  id bigint              NOT NULL,\n"
                              + "  k varchar default \'\' NOT NULL,\n"
                              + "  v varchar default \'\' NOT NULL,\n"
                              + "  PRIMARY KEY  (id, k));\n"
                              + "CREATE INDEX way_tags_k_idx ON way_tags (k);\n");

                        stmt.execute(
                                "DROP TABLE relation_tags IF EXISTS CASCADE;"
                              + "CREATE CACHED TABLE relation_tags (\n"
                              + "  id bigint              NOT NULL,\n"
                              + "  k varchar default \'\' NOT NULL,\n"
                              + "  v varchar default \'\' NOT NULL,\n"
                              + "  PRIMARY KEY  (id, k));\n"
                              + "CREATE INDEX relation_tags_k_idx ON relation_tags (k);\n");
                }

            };
        }

        return myIndexDB;
    }

    /**
     * Used by {@link #indexEntity(Entity, EntityType, Map)}.
     */
    private Map<EntityType, PreparedStatement> myIndexEntityStmts = new HashMap<EntityType, PreparedStatement>();

    /**
     * We store the summed times we where in certain functions
     * here to give statistics.
     */
    private Map<String, Long> mystatistics = new HashMap<String, Long>();

    /**
     * Add runtimes to {@link #mystatistics}.
     * @param startTime the time we entered the function
     * @param functionName the name of the function
     */
    private void addStatisticalTime(final long startTime, final String functionName) {
        Long sum = mystatistics.get(functionName);
        if (sum == null) {
            sum = (System.currentTimeMillis() - startTime);
        } else {
            sum += (System.currentTimeMillis() - startTime);
        }
        mystatistics.put(functionName, sum);
    }

    /**
     * @return a string with the summed runtimes of certain interesting functions.
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        for (String key : mystatistics.keySet()) {
            sb.append('[').append(key).append('=').append(mystatistics.get(key).toString()).append("ms]");
        }
        return sb.toString();
    }

    /**
     * Cache for {@link #indexEntity(Entity, EntityType, Map)}.
     */
    private Map<EntityType, PreparedStatement> myIsIndexEntityStatements = new HashMap<EntityType, PreparedStatement>();

    /**
     * Cache for {@link #indexEntity(Entity, EntityType, Map)}.
     */
    private Map<EntityType, PreparedStatement> myDoIndexEntityStatements = new HashMap<EntityType, PreparedStatement>();

    /**
     * Add the given entity to the index-database.
     * @param entity the entity to index
     * @param type the type of entity
     * @param tiles the tiles it belongs to (already shortened acording to
     * ${@link #myCombineTiles}).
     */
    protected void indexEntity(final Entity entity, final EntityType type, final Map<Long, MemoryDataSet> tiles) {
        long start = System.currentTimeMillis();
        DatabaseContext indexDatabase = getIndexDatabase();
        PreparedStatement isIndexedStmt = myIsIndexEntityStatements.get(type);
        if (isIndexedStmt == null) {
            isIndexedStmt = indexDatabase.prepareStatement(
                    "SELECT id FROM " + type.name() + "s_index WHERE id=? AND tile=?");
            myIsIndexEntityStatements.put(type, isIndexedStmt);
        }
        PreparedStatement doIndexStmt = myDoIndexEntityStatements.get(type);
        if (doIndexStmt == null) {
            doIndexStmt = indexDatabase.prepareStatement(
                    "INSERT INTO " + type.name()
                        + "s_index (id, tile) VALUES (?, ?)");
            myDoIndexEntityStatements.put(type, doIndexStmt);
        }

        for (Long tileNr : tiles.keySet()) {

            // is this entity already in the index?
            boolean alreadyIndexed = false;
            ResultSet result = null;
            try {
                isIndexedStmt.setLong(1, entity.getId());
                isIndexedStmt.setLong(2, tileNr);
                result = isIndexedStmt.executeQuery();
                alreadyIndexed = (result != null && result.next());
            } catch (Exception x) {
                LOG.log(Level.SEVERE, "Cannot check if entity is already indexed", x);
            } finally {
                if (result != null)
                try {
                    result.close();
                } catch (SQLException x2) {
                    LOG.log(Level.SEVERE, "Cannot close result-set. ignoring this.", x2);
                }
            }
            if (!alreadyIndexed) {
                try {
                    doIndexStmt.setLong(1, entity.getId());
                    doIndexStmt.setLong(2, tileNr);
                    doIndexStmt.executeUpdate();
                } catch (SQLException e) {
                    LOG.log(Level.SEVERE, "Cannot index the tile of entity " + entity.getId(),
                                 e);
                }
            }
        }

        PreparedStatement prep2 = myIndexEntityStmts.get(type);
        if (prep2 == null) {
            prep2 = indexDatabase.prepareStatement("INSERT INTO " + type.name()
                    + "_tags (id, k, v) VALUES (?, ?, ?)");
            myIndexEntityStmts.put(type, prep2);
        }

        for (Tag tag : entity.getTags()) {
            try {
                // we do not store "created_by" in this DataSet at all.
                if (tag.getKey().equals("created_by"))
                    continue;
                int i = 1;
                prep2.setLong(i++, entity.getId());
                // names are normalized because they are searched in the
                // getXByName-functons
                // name, int_name, nat_name, reg_name, loc_name, old_name, name
                // and the internationalized forms name:de name:fr name:en,...
                if (tag.getKey().equalsIgnoreCase(Tags.TAG_NAME)
                        || tag.getKey().contains("_" + Tags.TAG_NAME)
                        || tag.getKey().startsWith(Tags.TAG_NAME + ":")) {
                    prep2.setString(i++, tag.getKey());
                    prep2.setString(i++, NameHelper.normalizeName(tag.getValue()));
                } else {
                    prep2.setString(i++, tag.getKey());
                    prep2.setString(i++, tag.getValue());
                }
                prep2.execute();
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "Cannot index tag '" + tag.getKey() + "' of " + type.name() + "-entity '" + entity.getId() + "'", e);
            }
        }
        try {
            prep2.getConnection().commit();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot commit indexing entity '" + entity.getId() + "'", e);
        }
        addStatisticalTime(start, "indexEntity");
    }

    /**
     * Remove the given entity from the index-database.
     * @param entity the entity to index
     * @param type the type of entity
     */
    protected void unindexEntity(final Entity entity, final EntityType type) {
        DatabaseContext indexDatabase = getIndexDatabase();
        indexDatabase.executeStatement("DELETE FROM " + type.name()
                + "s_index WHERE id=" + entity.getId() + "");
        indexDatabase.executeStatement("DELETE FROM " + type.name()
                + "_tags WHERE id=" + entity.getId() + "");
        indexDatabase.commit();
    }

    //=======================================

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#addNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public void addNode(final Node newNode) {
        long start = System.currentTimeMillis();
        long tileNr = getTileNumber(newNode);
        MemoryDataSet tile = getOrCreateTile(newNode);
        // check if we have the node in this or a newer version
        Node old = tile.getNodeByID(newNode.getId());
        if (old != null) {
            if (newNode.getTimestamp() == null || old.getTimestamp() == null || newNode.getTimestamp().after(old.getTimestamp())) {
                removeNode(old);
            } else {
                return;
            }
        }

        tile.addNode(newNode);
        saveTile(tileNr, tile);

        Map<Long, MemoryDataSet> tiles = new HashMap<Long, MemoryDataSet>();
        tiles.put(tileNr, tile);
        indexEntity(newNode, EntityType.Node, tiles);
        addStatisticalTime(start, "addNode");
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#addRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public void addRelation(final Relation newRelation) {
        long start = System.currentTimeMillis();
//      check if the way is newer first
        Relation old = getRelationByID(newRelation.getId());
        if (old != null) {
            if (newRelation.getTimestamp() == null || old.getTimestamp() == null || newRelation.getTimestamp().after(old.getTimestamp())) {
                removeRelation(old);
            } else {
                return;
            }
        }

        Map<Long, MemoryDataSet> tiles = getOrCreateTiles(newRelation);
        for (Map.Entry<Long, MemoryDataSet> entry : tiles.entrySet()) {
            entry.getValue().removeRelation(newRelation);
            entry.getValue().addRelation(newRelation);
            saveTile(entry.getKey(), entry.getValue());
        }
        indexEntity(newRelation, EntityType.Relation, tiles);
        addStatisticalTime(start, "addRelation");
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#addWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public void addWay(final Way newWay) {
        long start = System.currentTimeMillis();

        // check if the way is newer first
        Way oldWay = getWaysByID(newWay.getId());
        addStatisticalTime(start, "addWay-remove(getWaysByID)");
        if (oldWay != null) {
            if (newWay.getTimestamp() == null || oldWay.getTimestamp() == null || newWay.getTimestamp().after(oldWay.getTimestamp())) {
                removeWay(oldWay);
            } else {
                return;
            }
        }
        addStatisticalTime(start, "addWay-remove");
        start = System.currentTimeMillis();

        Map<Long, MemoryDataSet> tiles = getOrCreateTiles(newWay);
        for (Map.Entry<Long, MemoryDataSet> entry : tiles.entrySet()) {
            entry.getValue().addWay(newWay);
            saveTile(entry.getKey(), entry.getValue());
        }
        addStatisticalTime(start, "addWay-add");
        start = System.currentTimeMillis();

        indexEntity(newWay, EntityType.Way, tiles);
        addStatisticalTime(start, "addWay-index");
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#containsNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public boolean containsNode(final Node aW) {
        return getOrCreateTile(aW).containsNode(aW);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#containsRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public boolean containsRelation(final Relation aR) {
        Map<Long, MemoryDataSet> tiles = getOrCreateTiles(aR);
        // we may not have all tiles here so we need to test all of them
        for (Map.Entry<Long, MemoryDataSet> entry : tiles.entrySet()) {
            if (entry.getValue().containsRelation(aR))
                return true;
        }
        return false;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#containsWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public boolean containsWay(final Way aW) {
        Map<Long, MemoryDataSet> tiles = getOrCreateTiles(aW);
        // we may not have all tiles here so we need to test all of them
        for (Map.Entry<Long, MemoryDataSet> entry : tiles.entrySet()) {
            if (entry.getValue().containsWay(aW))
                return true;
        }
        return false;
    }

    /**
     * The radius (in unprojected lat/lon) of the radius to search in
     * {@link #getNearestNode(LatLon, Selector)}.
     * Defaults to the width of one tile = 360/65K.
     */
    private static final double GETNEARESTNODERADIUS = 0.0054931640625d;
    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getNearestNode(org.openstreetmap.osm.data.coordinates.LatLon, org.openstreetmap.osm.data.Selector)
     */
    public Node getNearestNode(final LatLon aLastGPSPos, final Selector aSelector) {
        // check all tiles within a given radius
        Iterator<Long> tileIDs = myTileCalculator.getTileIDsForBounds(new Bounds(aLastGPSPos, GETNEARESTNODERADIUS), myCombineTiles);
        Node nearest = null;
        double minDist = Double.MAX_VALUE;

        while (tileIDs.hasNext()) {
            Long tileID = tileIDs.next();
            MemoryDataSet tile = getTile(tileID);
            if (tile == null)
                continue;
            Node node2 = tile.getNearestNode(aLastGPSPos, aSelector);
            if (node2 == null)
                continue;

            LatLon pos = new LatLon(node2.getLatitude(), node2.getLongitude());
            double dist = pos.distance(aLastGPSPos);
            if (dist < minDist) {
                nearest = node2;
                minDist = dist;
            }
        }

        return nearest;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getNodeByID(long)
     */
    public Node getNodeByID(final long aNodeID) {
        long start = System.currentTimeMillis();

        // check all loaded tiles
        try {
//          if (myTileCache.size() > 30000) {
//              //LOG.warning("FileTileDataSet->myTileCache is growing out of proportion! size now " + myTileCache.size());
//            }
//          ArrayList<Long> emptyCacheKeys = new ArrayList<Long>();
            for (Entry<Long, SoftReference<MemoryDataSet>> cache : myTileCache.entrySet()) {
                MemoryDataSet tile = cache.getValue().get();

                if (tile != null) {
                    Node node = tile.getNodeByID(aNodeID);
                    if (node != null) {
                        addStatisticalTime(start, "getNodeByID-memory");

//                     // clean up memory
//                        for (Long emptyKey : emptyCacheKeys) {
//                          myTileCache.remove(emptyKey);
//                      }
                        return node;
                    }
                } else {
//                  emptyCacheKeys.add(cache.getKey());
                    myTileCache.remove(cache.getKey());
                }
            }

         // clean up memory
//            for (Long emptyKey : emptyCacheKeys) {
//              myTileCache.remove(emptyKey);
//            }
        } catch (RuntimeException e1) {
            LOG.log(Level.FINE, "[RuntimeException] Problem in "
                       + getClass().getName() + " in getNodeByID() "
                       + "while checking all tiles in memory",
                         e1);
        }
        addStatisticalTime(start, "getNodeByID-memory");

        // try the index
        try {
            DatabaseContext db = getIndexDatabase();
            ResultSet result = db.executeStreamingQuery(
                    "SELECT tile FROM nodes_index WHERE id=" + aNodeID);
            while (result.next()) {
                long tileNr = result.getLong("tile");
                MemoryDataSet tile = getTile(tileNr);
                if (tile != null) {
                    Node node = tile.getNodeByID(aNodeID);
                    if (node != null) {
                        result.close();
                        addStatisticalTime(start, "getNodeByID-memory+db");
                        return node;
                    }
                }
            }
            result.close();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot use index-database for getNodeByID()! Falling back to loading all tiles. This will be VERY SLOW.", e);
        }

        addStatisticalTime(start, "getNodeByID-memory+db");
        // fall back to loading all tiles
/*
//      check all tiles
        for (Long tileID : getAllTileIDs()) {
            MemoryDataSet tile = getTile(tileID);
            if (tile != null) {
                Node node = tile.getNodeByID(aNodeID);
                if (node != null) {
                    addStatisticalTime(start, "getNodeByID-memory+db+alltiles");
                    return node;
                }
            }
        }
        addStatisticalTime(start, "getNodeByID-memory+db+alltiles");*/
        return null;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getNodes(org.openstreetmap.osm.data.coordinates.Bounds)
     */
    public Iterator<Node> getNodes(final Bounds aBoundingBox) {
        Set<Node> retval = new HashSet<Node>();

        Iterator<Long> tileIDs = null;
        final double maxBoundsForIteration = 10.0d;
        if (aBoundingBox.getSize() > maxBoundsForIteration) {
            // the bounding-box is too large. Do not
            // iterate over all tiles in the box but
            // check the tiles we have for overlapping
            // the box.
            Collection<Long> tiles = myTileCalculator.getTileIDsForBounds(aBoundingBox, getAllTileIDs(), myCombineTiles);
            final int reportMinTileCount = 10;
            if (tiles.size() > reportMinTileCount)
                LOG.info("FileTileDataSet[" + this.myAdditionalPath + "]"
                        + ".getNodes has to check " + tiles.size()
                        + " existing tiles for nodes. This may take a while.");
            tileIDs = tiles.iterator();
        } else {
            tileIDs = myTileCalculator.getTileIDsForBounds(aBoundingBox, myCombineTiles);
        }

        while (tileIDs.hasNext()) {
            Long tileID = tileIDs.next();

            MemoryDataSet tile = getTile(tileID);
            if (tile != null)
                for (Iterator<Node> nodes = tile.getNodes(aBoundingBox); nodes.hasNext();)
                    retval.add(nodes.next());
        }
        return retval.iterator();
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getNodesByName(java.lang.String)
     */
    public Iterator<Node> getNodesByName(final String aLookFor) {
        // try the index
        Set<Node> retval = new HashSet<Node>();
        try {
            DatabaseContext db = getIndexDatabase();
            PreparedStatement prep = db.prepareStatement(
                    "SELECT distinct n.id, n.tile FROM node_tags t, nodes_index n WHERE t.id = n.id "
                    + "AND (t.k = 'name' OR t.k = 'int_name' OR t.k = 'nat_name' OR t.k = 'reg_name' OR t.k = 'loc_name' OR t.k = 'old_name' OR t.k  like 'name:' "
                    + "OR   t.k = 'ref'  OR t.k = 'int_ref'  OR t.k = 'nat_ref'  OR t.k = 'reg_ref'  OR t.k = 'loc_ref'  OR t.k = 'old_ref'  OR t.k  like 'ref:'"
                    + ") AND t.v like ? ORDER by n.tile");
            prep.setString(1, NameHelper.buildNameSearchSQLMatch(aLookFor));
            ResultSet result = prep.executeQuery();
            while (result.next()) {
                long tileNr = result.getLong("tile");
                long id = result.getLong("id");
                MemoryDataSet tile = getTile(tileNr);
                if (tile != null) {
                    Node node = tile.getNodeByID(id);
                    if (node != null)
                        retval.add(node);
                }
            }
            result.close();
            return retval.iterator();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot use index-database for getNodesByName()! Falling back to loading all tiles. This will be VERY SLOW.", e);
        }

        // fall back to loading all tiles
        for (Long tileID : getAllTileIDs()) {
            MemoryDataSet tile = getTile(tileID);
            if (tile != null) {
                for (Iterator<Node> nodes = tile.getNodesByName(aLookFor); nodes.hasNext();)
                    retval.add(nodes.next());
            }
        }
        return retval.iterator();
    }

    /**
     * ${@inheritDoc}.
     */
    public Iterator<Node> getNodesByTag(final String aKey, final String aValue) {
        // try the index
        Set<Node> retval = new HashSet<Node>();
        try {
            DatabaseContext db = getIndexDatabase();
            PreparedStatement prep = null;
            if (aValue == null) {

                prep = db.prepareStatement(
                        "SELECT distinct n.id, n.tile FROM node_tags t, nodes_index n WHERE t.id = n.id "
                        + "AND t.k = ?  ORDER by n.tile");
                prep.setString(1, aKey);
            } else {
                prep = db.prepareStatement(
                        "SELECT distinct n.id, n.tile FROM node_tags t, nodes_index n WHERE t.id = n.id "
                        + "AND t.k = ? AND t.v like ? ORDER by n.tile");
                prep.setString(1, aKey);
                prep.setString(2, aValue);
            }

            ResultSet result = prep.executeQuery();
            while (result.next()) {
                long tileNr = result.getLong("tile");
                long id = result.getLong("id");
                MemoryDataSet tile = getTile(tileNr);
                if (tile != null) {
                    Node node = tile.getNodeByID(id);
                    if (node != null)
                        retval.add(node);
                }
            }
            result.close();
            return retval.iterator();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot use index-database for getNodesByName()! Falling back to loading all tiles. This will be VERY SLOW.", e);
        }

        // fall back to loading all tiles
        for (Long tileID : getAllTileIDs()) {
            MemoryDataSet tile = getTile(tileID);
            if (tile != null) {
                for (Iterator<Node> nodes = tile.getNodesByTag(aKey, aValue); nodes.hasNext();)
                    retval.add(nodes.next());
            }
        }
        return retval.iterator();
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getRelationByID(long)
     */
    public Relation getRelationByID(final long aRelationID) {
        // try the index
        try {
            DatabaseContext db = getIndexDatabase();
            ResultSet result = db.executeStreamingQuery(
                    "SELECT tile FROM relations_index WHERE id=" + aRelationID);
            while (result.next()) {
                long tileNr = result.getLong("tile");
                MemoryDataSet tile = getTile(tileNr);
                if (tile != null) {
                    Relation relation = tile.getRelationByID(aRelationID);
                    if (relation != null) {
                        result.close();
                        return relation;
                    }
                }
            }
            result.close();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot use index-database for getRelationByID()! Falling back to loading all tiles. This will be VERY SLOW.", e);
        }

        // fall back to loading all tiles
//      check all loaded tiles
        for (SoftReference<MemoryDataSet> cache : myTileCache.values()) {
            MemoryDataSet tile = cache.get();
            if (tile != null) {
                Relation node = tile.getRelationByID(aRelationID);
                if (node != null)
                    return node;
            }
        }
        return null;
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getRelations(org.openstreetmap.osm.data.coordinates.Bounds)
     */
    public Iterator<Relation> getRelations(final Bounds aBoundingBox) {
        Set<Relation> retval = new HashSet<Relation>();

        Iterator<Long> tileIDs = null;
        final double maxBoundsForIteration = 10.0d;
        if (aBoundingBox.getSize() > maxBoundsForIteration) {
            // the bounding-box is too large. Do not
            // iterate over all tiles in the box but
            // check the tiles we have for overlapping
            // the box.
            Collection<Long> tiles = myTileCalculator.getTileIDsForBounds(aBoundingBox, getAllTileIDs(), myCombineTiles);
            final int reportMinTileCount = 10;
            if (tiles.size() > reportMinTileCount)
                LOG.info("FileTileDataSet[" + this.myAdditionalPath + "]"
                        + ".getRelations has to check " + tiles.size()
                        + " existing tiles for relations. This may take a while.");
            tileIDs = tiles.iterator();
        } else {
            tileIDs = myTileCalculator.getTileIDsForBounds(aBoundingBox, myCombineTiles);
        }

        while (tileIDs.hasNext()) {
            Long tileID = tileIDs.next();
            MemoryDataSet tile = getTile(tileID);
            if (tile != null)
                for (Iterator<Relation> nodes = tile.getRelations(aBoundingBox); nodes.hasNext();)
                    retval.add(nodes.next());
        }
        return retval.iterator();
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWayHelper()
     */
    public WayHelper getWayHelper() {
        return new WayHelper(this);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWays(org.openstreetmap.osm.data.coordinates.Bounds)
     */
    public Iterator<Way> getWays(final Bounds aBoundingBox) {
        Set<Way> retval = new HashSet<Way>();

        Iterator<Long> tileIDs = null;
        final double maxBoundsForIteration = 1.0d;
        if (aBoundingBox.getSize() > maxBoundsForIteration) {
            // the bounding-box is too large. Do not
            // iterate over all tiles in the box but
            // check the tiles we have for overlapping
            // the box.
            Collection<Long> tiles = myTileCalculator.getTileIDsForBounds(aBoundingBox, getAllTileIDs(), myCombineTiles);
            final int reportMinTileCount = 10;
            if (tiles.size() > reportMinTileCount)
                LOG.info("FileTileDataSet[" + this.myAdditionalPath + "]"
                        + ".getWays has to check " + tiles.size()
                        + " existing tiles for ways. This may take a while.");
            tileIDs = tiles.iterator();
        } else {
            tileIDs = myTileCalculator.getTileIDsForBounds(aBoundingBox, myCombineTiles);
        }
        while (tileIDs.hasNext()) {
            Long tileID = tileIDs.next();
            MemoryDataSet tile = getTile(tileID);
            if (tile != null)
                for (Iterator<Way> nodes = tile.getWays(aBoundingBox); nodes.hasNext();)
                    retval.add(nodes.next());
        }
        return retval.iterator();
    }

    /**
     * Prepared statement used by {@link #getWaysByID(long)}.
     */
    private PreparedStatement getTileForWayStmt = null;

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWaysByID(long)
     */
    public Way getWaysByID(final long aWayID) {
        long start = System.currentTimeMillis();

//      check all loaded tiles in memory
        try {
//           if (myTileCache.size() > 5000) {
//               LOG.warning("FileTileDataSet->myTileCache is growing out of proportion! size now " + myTileCache.size());
//            }
            //ArrayList<Long> emptyCacheKeys = new ArrayList<Long>();
            for (Entry<Long, SoftReference<MemoryDataSet>> cache : myTileCache.entrySet()) {
                MemoryDataSet tile = cache.getValue().get();
                if (tile != null) {
                    Way foundWay = tile.getWaysByID(aWayID);
                    if (foundWay != null) {
                        addStatisticalTime(start, "getWaysByID-memory");

//                        // clean up memory
//                        for (Long emptyKey : emptyCacheKeys) {
//                          myTileCache.remove(emptyKey);
//                      }
                        return foundWay;
                    }
                } else {
                    myTileCache.remove(cache.getKey());
                    //emptyCacheKeys.add(cache.getKey());
                }
            }

            // clean up memory
//            for (Long emptyKey : emptyCacheKeys) {
//               myTileCache.remove(emptyKey);
//            }
        } catch (RuntimeException e1) {
            LOG.log(Level.SEVERE, "[RuntimeException] Problem in "
                       + getClass().getName(),
                         e1);
        }
        addStatisticalTime(start, "getWaysByID-memory");

        // try the index
        try {
            DatabaseContext db = getIndexDatabase();
            if (getTileForWayStmt == null)
                getTileForWayStmt = db.prepareStatementForStreaming("SELECT tile FROM ways_index WHERE id=?");
            ResultSet result = null;
            try {
                getTileForWayStmt.setLong(1, aWayID);
                result = getTileForWayStmt.executeQuery();
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "[Exception] Problem in "
                           + getClass().getName() + ":getWaysByID(" + aWayID + ")",
                             e);
                getTileForWayStmt = null;
                result = db.executeStreamingQuery(
                        "SELECT tile FROM ways_index WHERE id=" + aWayID);
            }

            while (result.next()) {
                long tileNr = result.getLong("tile");
                MemoryDataSet tile = getTile(tileNr);
                if (tile != null) {
                    Way way = tile.getWaysByID(aWayID);
                    if (way != null) {
                        result.close();
                        addStatisticalTime(start, "getWaysByID-memory+db");
                        return way;
                    }
                }
            }
            result.close();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot use index-database for getWaysByID()! Falling back to loading all tiles. This will be VERY SLOW.", e);
        }
        addStatisticalTime(start, "getWaysByID-memory+db");

        // fall back to loading all tiles
/*
//      check all tiles
        for (Long tileID : getAllTileIDs()) {
            MemoryDataSet tile = getTile(tileID);
            if (tile != null) {
                Way way = tile.getWaysByID(aWayID);
                if (way != null) {
                    addStatisticalTime(start, "getNodeByID-memory+db+alltiles");
                    return way;
                }
            }
        }
        addStatisticalTime(start, "getWaysByID-memory+db+alltiles");*/
        return null;
    }

    /**
     * ${@inheritDoc}.
     */
    public Iterator<Way> getWaysByTag(final String aKey, final String aValue) {
        // try the index
        Set<Way> retval = new HashSet<Way>();
        try {
            DatabaseContext db = getIndexDatabase();
            PreparedStatement prep = null;
            if (aValue == null) {
                // find all ways containing this tag
                prep = db.prepareStatement(
                        "SELECT n.id, n.tile FROM way_tags t, ways_index n WHERE t.id = n.id "
                        + "AND (t.k = ?) ORDER by n.tile");
                prep.setString(1, aKey);
            } else {
             // find all ways containing this tag with this value
                prep = db.prepareStatement(
                        "SELECT n.id, n.tile FROM way_tags t, ways_index n WHERE t.id = n.id "
                        + "AND (t.k = ?) AND t.v=? ORDER by n.tile");
                prep.setString(1, aKey);
                prep.setString(2, aValue);
            }

            ResultSet result = prep.executeQuery();
            while (result.next()) {
                long tileNr = result.getLong("tile");
                long id = result.getLong("id");
                MemoryDataSet tile = getTile(tileNr);
                if (tile != null) {
                    Way way = tile.getWaysByID(id);
                    if (way != null)
                        retval.add(way);
                }
            }
            return retval.iterator();
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot use index-database for getWaysByName()! Falling back to loading all tiles. This will be VERY SLOW.", e);
        }

        // fall back to loading all tiles
        for (Long tileID : getAllTileIDs()) {
            MemoryDataSet tile = getTile(tileID);
            if (tile != null) {
                for (Iterator<Way> nodes = tile.getWaysByTag(aKey, aValue); nodes.hasNext();)
                    retval.add(nodes.next());
            }
        }
        return retval.iterator();
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWaysByName(java.lang.String)
     */
    public Iterator<Way> getWaysByName(final String aLookFor, final Bounds boundingbox) {
        // try the index
        Set<Way> retval = new HashSet<Way>();
        try {
            DatabaseContext db = getIndexDatabase();
            PreparedStatement prep = db.prepareStatement(
                    "SELECT n.id, n.tile FROM way_tags t, ways_index n WHERE t.id = n.id "
                    + "AND (t.k = 'name' OR t.k = 'int_name' OR t.k = 'nat_name' OR t.k = 'reg_name' OR t.k = 'loc_name' OR t.k = 'old_name' OR t.k  like 'name:' "
                    + "OR   t.k = 'ref'  OR t.k = 'int_ref'  OR t.k = 'nat_ref'  OR t.k = 'reg_ref'  OR t.k = 'loc_ref'  OR t.k = 'old_ref'  OR t.k  like 'ref:'"
                    + ") AND t.v like ? ORDER by n.tile");
            // note that the index-database stores the names
            // normalized but the tile-files do not!
            // thus the unchanged name-tag is returned
            prep.setString(1, NameHelper.buildNameSearchSQLMatch(aLookFor));
            ResultSet result = prep.executeQuery();
            while (result.next()) {
                long tileNr = result.getLong("tile");
                long id = result.getLong("id");
                MemoryDataSet tile = getTile(tileNr);
                if (tile != null) {
                    Way way = tile.getWaysByID(id);
                    if (way != null)
                        retval.add(way);
                }
            }

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Cannot use index-database for getWaysByName()! Falling back to loading all tiles. This will be VERY SLOW.", e);

//          fall back to loading all tiles
            for (Long tileID : getAllTileIDs()) {
                MemoryDataSet tile = getTile(tileID);
                if (tile != null) {
                    for (Iterator<Way> nodes = tile.getWaysByName(aLookFor, boundingbox); nodes.hasNext();)
                        retval.add(nodes.next());
                }
            }
        }


//      filter by bounding-box
        if (boundingbox != null) {
            Set<Way> retval2 = new HashSet<Way>();
            for (Way way : retval) {
                for (WayNode wayNode : way.getWayNodes()) {
                    Node node = getNodeByID(wayNode.getNodeId());
                    if (node != null && boundingbox.contains(node.getLatitude(), node.getLongitude())) {
                        retval2.add(way);
                        break;
                    }
                }
            }
            return retval2.iterator();
        }
        return retval.iterator();
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#getWaysForNode(long)
     */
    public Iterator<Way> getWaysForNode(final long aNodeID) {
        Node node = getNodeByID(aNodeID);
        if (node == null)
            return new LinkedList<Way>().iterator();
        MemoryDataSet tile = getOrCreateTile(node);
        return tile.getWaysForNode(aNodeID);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#removeNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)
     */
    public void removeNode(final Node aW) {
        long tileNr = getTileNumber(aW);
        MemoryDataSet tile = getOrCreateTile(aW);
        tile.removeNode(aW);
        saveTile(tileNr, tile);
        unindexEntity(aW, EntityType.Node);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#removeRelation(org.openstreetmap.osmosis.core.domain.v0_5.Relation)
     */
    public void removeRelation(final Relation aR) {
        Map<Long, MemoryDataSet> tiles = getOrCreateTiles(aR);
        for (Map.Entry<Long, MemoryDataSet> entry : tiles.entrySet()) {
            entry.getValue().removeRelation(aR);
            saveTile(entry.getKey(), entry.getValue());
        }
        unindexEntity(aR, EntityType.Relation);
    }

    /**
     * ${@inheritDoc}.
     * @see org.openstreetmap.osm.data.IDataSet#removeWay(org.openstreetmap.osmosis.core.domain.v0_5.Way)
     */
    public void removeWay(final Way aW) {
        Map<Long, MemoryDataSet> tiles = getOrCreateTiles(aW);
        for (Map.Entry<Long, MemoryDataSet> entry : tiles.entrySet()) {
            entry.getValue().removeWay(aW);
            saveTile(entry.getKey(), entry.getValue());
        }
        unindexEntity(aW, EntityType.Way);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (mySavingThread != null)
        synchronized (mySaveQueue) {
            if (!mySaveQueue.isEmpty()) {
                try {
                    final int sleepms = 100;
                    Thread.sleep(sleepms);
                } catch (InterruptedException e) {
                    LOG.finer("sleep interrupted");
                }
            }
        }
        getIndexDatabase().commit();
        getIndexDatabase().executeStatement("SHUTDOWN");
        getIndexDatabase().release();
        myIndexDB = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationSection getSettings() {
        return null;
    }

}
