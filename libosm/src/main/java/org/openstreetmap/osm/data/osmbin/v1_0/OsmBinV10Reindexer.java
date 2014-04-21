package org.openstreetmap.osm.data.osmbin.v1_0;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.openstreetmap.osm.data.osmbin.GeoIndexFile;
import org.openstreetmap.osm.data.osmbin.IDIndexFile;
import org.openstreetmap.osmosis.core.task.common.RunnableTask;
import org.openstreetmap.osmosis.core.util.FixedPrecisionCoordinateConvertor;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * OsmBinV10Reindexer.java<br/>
 * created: 02.02.2009<br/>
 *<br/><br/>
 * <b>This is an Osmosis-task to rebuild the 1D-indice of a map in OsmBin-format.</b><br/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class OsmBinV10Reindexer  implements RunnableTask  {

    /**
     * The directory containing the OsmBin-data.
     */
    private final File myDirectory;

    /**
     * @param aDir the directory with the osmbin-database
     */
    public OsmBinV10Reindexer(final File aDir) {
        if (!aDir.exists()) {
            throw new IllegalArgumentException("Directory "
                    + aDir.getAbsolutePath() + " does not exist. ");
        }

        if (!aDir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory "
                    + aDir.getAbsolutePath());
        }
        this.myDirectory = aDir;
    }

    /**
     *
     * @param args only 1 argument allowed. The directory with the osmbin-data
     */
    public static void main(final String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: OsmBinV10Reindexer <directory with osmbin-data>");
            return;
        }
        OsmBinV10Reindexer worker = new OsmBinV10Reindexer(new File(args[0]));
        worker.run();
    }

    /**
     * Read nodes.obm, ways.obm and relations.obm in {@link #myDirectory}
     * and rebuild nodes.idx, ways.idx and relations.idx .
     */
    @Override
    public void run() {
        try {
            File tempFile0 = new File(myDirectory, "nodes.tempreindexing.idx");
            if (tempFile0.exists()) {
                tempFile0.delete();
            }
            File tempFile0d = new File(myDirectory, "nodes.tempreindexing.id2");
            if (tempFile0d.exists()) {
                tempFile0d.delete();
            }
            File tempFile1 = new File(myDirectory, "ways.tempreindexing.idx");
            if (tempFile1.exists()) {
                tempFile1.delete();
            }
            File tempFile2 = new File(myDirectory, "relations.tempreindexing.idx");
            if (tempFile2.exists()) {
                tempFile2.delete();
            }
            reindexNodes(new File(myDirectory, "nodes.obm"),
                    NodesFile.getNodeRecordLength(),
                    new IDIndexFile(tempFile0),
                    new GeoIndexFile(tempFile0d),
                    new File(myDirectory, "nodes.idx"),
                    new File(myDirectory, "nodes.id2"));
            reindex(new File(myDirectory, "ways.obm"),
                    WaysFile.getWayRecordLength(),
                    new IDIndexFile(tempFile1),
                    new File(myDirectory, "ways.idx"));
            reindex(new File(myDirectory, "relations.obm"),
                    RelationsFile.getRelationRecordLength(),
                    new IDIndexFile(tempFile2),
                    new File(myDirectory, "relations.idx"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reindex a single .obm -file. (Only the default 1D-index. Not the 2D-index of nodes.)
     * @param anOBmFile the .obm -file to scan
     * @param aRecordSize the size of the record
     * @param anOutputFile the temporary file to build the index in
     * @param aFinalOutputFile the index-file after re-creation
     * @throws IOException if anything fails
     * @see {@link NodesFile#getNodeRecordLength()}
     * @see {@link WaysFile#getWayRecordLength()}
     * @see {@link RelationsFile#getRelationRecordLength()}
     */
    protected void reindex(final File anOBmFile, final int aRecordSize, final IDIndexFile anOutputFile, final File aFinalOutputFile) throws IOException {
        InputStream read = new BufferedInputStream(new FileInputStream(anOBmFile));
        byte[] record = new byte[aRecordSize];
        ByteBuffer recordBuffer = ByteBuffer.wrap(record);
        int lastID = Integer.MIN_VALUE;

        // scan the file. All records starts with the id of the element.
        // the id Integer.MIN_VALUE denotes an empty record.
        for (long recordNr = 0; true; recordNr++) {
            int reat = read.read(record);
            if (reat < aRecordSize) {
                break;
            }
            recordBuffer.rewind();
            int id = recordBuffer.getInt();
            if (id != lastID) {
                lastID = id;
                if (id != Integer.MIN_VALUE) {
                    anOutputFile.put(id, recordNr);
                }
            }
        }

        // rename the temporary file into the final file
        anOutputFile.close();
        aFinalOutputFile.delete();
        anOutputFile.getFileName().renameTo(aFinalOutputFile);
    }
    /**
     * Reindex a single .obm -file. (Only the default 1D-index. Not the 2D-index of nodes.)
     * @param anOBmFile the .obm -file to scan
     * @param aRecordSize the size of the record
     * @param anOutputFile the temporary file to build the index in
     * @param aGeoIndexFile 
     * @param aFinalOutputFile the index-file after re-creation
     * @param aFile 
     * @throws IOException if anything fails
     * @see {@link NodesFile#getNodeRecordLength()}
     * @see {@link WaysFile#getWayRecordLength()}
     * @see {@link RelationsFile#getRelationRecordLength()}
     */
    protected void reindexNodes(final File anOBmFile,
            final int aRecordSize,
            final IDIndexFile anOutputFile,
            final GeoIndexFile aGeoIndexFile,
            final File aFinalOutputFile,
            final File aFinalGeoOutputFile) throws IOException {
        InputStream read = new BufferedInputStream(new FileInputStream(anOBmFile));
        byte[] record = new byte[aRecordSize];
        ByteBuffer recordBuffer = ByteBuffer.wrap(record);
        int lastID = Integer.MIN_VALUE;

        // scan the file. All records starts with the id of the element.
        // the id Integer.MIN_VALUE denotes an empty record.
        for (long recordNr = 0; true; recordNr++) {
            int reat = read.read(record);
            if (reat < aRecordSize) {
                break;
            }
            recordBuffer.rewind();
            int id = recordBuffer.getInt();
            if (id != lastID) {
                lastID = id;
                if (id != Integer.MIN_VALUE) {
                    int version = recordBuffer.getInt();
                    int latI = recordBuffer.getInt();
                    double lat = FixedPrecisionCoordinateConvertor.convertToDouble(latI);
                    int lonI = recordBuffer.getInt();
                    double lon = FixedPrecisionCoordinateConvertor.convertToDouble(lonI);

                    anOutputFile.put(id, recordNr);
                    aGeoIndexFile.put(recordNr, latI, lonI);
                }
            }
        }

        // rename the temporary file into the final file
        anOutputFile.close();
        aFinalOutputFile.delete();
        anOutputFile.getFileName().renameTo(aFinalOutputFile);
        aGeoIndexFile.close();
        aFinalGeoOutputFile.delete();
        aGeoIndexFile.getFileName().renameTo(aFinalGeoOutputFile);
    }

}
