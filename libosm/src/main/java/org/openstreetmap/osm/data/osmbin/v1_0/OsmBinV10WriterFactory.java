/**
 *
 */
package org.openstreetmap.osm.data.osmbin.v1_0;

import java.io.File;

import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.pipeline.v0_6.SinkManager;

public class OsmBinV10WriterFactory extends TaskManagerFactory  {



    private static final String ARG_DIR_NAME = "dir";
    private static final String DEFAULT_DIR_NAME = "osmbin";

    /**
     * {@inheritDoc}
     */
    protected TaskManager createTaskManagerImpl(final TaskConfiguration taskConfig) {
        String fileName;
        File dir;
        OsmBinV10Writer task;

        // Get the task arguments.
        fileName = getStringArgument(
            taskConfig,
            ARG_DIR_NAME,
            getDefaultStringArgument(taskConfig, DEFAULT_DIR_NAME)
        );

        // Create a file object from the file name provided.
        dir = new File(fileName);

        // Build the task object.
        task = new OsmBinV10Writer(dir);

        return new SinkManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
    }
}