/**
 * OsmBinV10ReindexerFactory.java
 * created: 02.02.2009
 * (c) 2008 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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
package org.openstreetmap.osm.data.osmbin.v1_0;

import java.io.File;

import org.openstreetmap.osmosis.core.pipeline.common.RunnableTaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskConfiguration;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManager;
import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * OsmBinV10ReindexerFactory.java<br/>
 * created: 02.02.2009<br/>
 *<br/><br/>
 * <b>This is the factory for OsmBinV10Reindexer. An Osmosis-task to rebuild the 1D-indice of a map in OsmBin-format.</b><br/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class OsmBinV10ReindexerFactory extends TaskManagerFactory  {



    private static final String ARG_DIR_NAME = "dir";
    private static final String DEFAULT_DIR_NAME = "osmbin";

    /**
     * {@inheritDoc}
     */
    protected TaskManager createTaskManagerImpl(final TaskConfiguration taskConfig) {
        String fileName;
        File dir;
        OsmBinV10Reindexer task;

        // Get the task arguments.
        fileName = getStringArgument(
            taskConfig,
            ARG_DIR_NAME,
            getDefaultStringArgument(taskConfig, DEFAULT_DIR_NAME)
        );

        // Create a file object from the file name provided.
        dir = new File(fileName);

        // Build the task object.
        task = new OsmBinV10Reindexer(dir);

        return new RunnableTaskManager(taskConfig.getId(), task, taskConfig.getPipeArgs());
    }
}