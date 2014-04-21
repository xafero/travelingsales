package org.openstreetmap.osm.data.osmbin;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osm.data.osmbin.v1_0.OsmBinV10DatasetReaderFactory;
import org.openstreetmap.osm.data.osmbin.v1_0.OsmBinV10ReindexerFactory;
import org.openstreetmap.osm.data.osmbin.v1_0.OsmBinV10WriterFactory;


import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;

public class WriteOSMBinPlugin implements PluginLoader {

    @Override
    public Map<String, TaskManagerFactory> loadTaskFactories() {
        HashMap<String, TaskManagerFactory> map = new HashMap<String, TaskManagerFactory>();
        map.put("write-osmbin-0.6", new OsmBinV10WriterFactory());
        map.put("dataset-osmbin-0.6", new OsmBinV10DatasetReaderFactory());
        map.put("reindex-osmbin-0.6", new OsmBinV10ReindexerFactory());
        return map;
    }}
