package org.openstreetmap.osm.data.osmbin.v1_0;

import java.io.File;
import java.util.Map;

import org.openstreetmap.osm.data.OsmBinDataSet;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 * Osmosis-task to write data in OSMBin-v1.0 -format.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class OsmBinV10Writer  implements Sink {

    /**
     * The osmbin-database to write to.
     */
    protected final OsmBinDataSet myDataSet;

    /**
     * @param aDir the directory with the osmbin-database
     */
    public OsmBinV10Writer(final File aDir) {
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

    @Override
    public void process(final EntityContainer aContainer) {
        Entity entity = aContainer.getEntity();
        if (entity instanceof Node) {
            myDataSet.addNode((Node) entity);
        } else if (entity instanceof Way) {
            myDataSet.addWay((Way) entity);
        } else if (entity instanceof Relation) {
            myDataSet.addRelation((Relation) entity);
        }
    }

    @Override
    public void complete() {
    }

    @Override
    public void release() {
        myDataSet.shutdown();
    }

	@Override
	public void initialize(Map<String, Object> map) {
		throw new RuntimeException("implement it!");
	}

}
