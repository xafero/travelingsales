/**
 *
 */
package org.openstreetmap.osm.data.osmbin.v1_0;

import java.io.File;

import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkDatasetSourceManager;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * OsmBinV10DatasetReaderFactory.java<br/>
 * created: 02.02.2009<br/>
 *<br/><br/>
 * <b>This is the factory for {@link OsmBinV10DatasetReader}.
 * An Osmosis-task to read and provide random access to a map in OsmBin-format.</b><br/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class OsmBinV10DatasetReaderFactory extends TaskManagerFactory  {



    private static final String ARG_DIR_NAME = "dir";
    private static final String DEFAULT_DIR_NAME = "osmbin";

    /**
     * {@inheritDoc}
     */
    protected TaskManager createTaskManagerImpl(final TaskConfiguration taskConfig) {
        String fileName;
        File dir;
        OsmBinV10DatasetReader task;

        // Get the task arguments.
        fileName = getStringArgument(
            taskConfig,
            ARG_DIR_NAME,
            getDefaultStringArgument(taskConfig, DEFAULT_DIR_NAME)
        );

        // Create a file object from the file name provided.
        dir = new File(fileName);

        // Build the task object.
        task = new OsmBinV10DatasetReader(dir);

        return new SinkDatasetSourceManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
    }
}