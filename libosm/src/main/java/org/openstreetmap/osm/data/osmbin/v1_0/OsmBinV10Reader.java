package org.openstreetmap.osm.data.osmbin.v1_0;

import java.io.File;
import java.util.Iterator;

import org.openstreetmap.osm.data.OsmBinDataSet;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.DatasetSink;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 * Osmosis-task to read data in OSMBin-v1.0 -format.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class OsmBinV10Reader  implements RunnableSource {

    /**
     * The osmbin-database to write to.
     */
    protected final OsmBinDataSet myDataSet;

    /**
     * Where we write to.
     */
    private Sink myCurrentSink;

    /**
     * @param aDir the directory with the osmbin-database
     */
    public OsmBinV10Reader(final File aDir) {
        if (!aDir.exists()) {
            if (!aDir.mkdirs()) {
                throw new IllegalArgumentException("Cannot create directory "
                        + aDir.getAbsolutePath());
            }
        }

        if (!aDir.isDirectory()) {
            throw new IllegalArgumentException("Not a directory "
                    + aDir.getAbsolutePath());
        }
        myDataSet = new OsmBinDataSet(aDir);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        Iterator<Node> nodes = this.myDataSet.getNodes(Bounds.WORLD);
        while (nodes.hasNext()) {
            NodeContainer node = new NodeContainer(nodes.next());
            this.myCurrentSink.process(node);
        }
        Iterator<Way> ways = this.myDataSet.getWays(Bounds.WORLD);
        while (ways.hasNext()) {
            WayContainer way = new WayContainer(ways.next());
            this.myCurrentSink.process(way);
        }
        Iterator<Relation> relations = this.myDataSet.getRelations(Bounds.WORLD);
        while (relations.hasNext()) {
            RelationContainer relation = new RelationContainer(relations.next());
            this.myCurrentSink.process(relation);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSink(final Sink aArg0) {
        this.myCurrentSink = aArg0;
    }
}
