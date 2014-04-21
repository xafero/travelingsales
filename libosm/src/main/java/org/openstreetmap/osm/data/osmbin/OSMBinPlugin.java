package org.openstreetmap.osm.data.osmbin;

import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osm.data.osmbin.v1_0.OsmBinV10DatasetReaderFactory;
import org.openstreetmap.osm.data.osmbin.v1_0.OsmBinV10ReaderFactory;
import org.openstreetmap.osm.data.osmbin.v1_0.OsmBinV10ReindexerFactory;
import org.openstreetmap.osm.data.osmbin.v1_0.OsmBinV10WriterFactory;


import org.openstreetmap.osmosis.core.pipeline.common.TaskManagerFactory;
import org.openstreetmap.osmosis.core.plugin.PluginLoader;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * WriteOSMBinPlugin.java<br/>
 * created: 20.01.2009<br/>
 *<br/><br/>
 * <b>This is the factory that creates all osmosis-tasks related to the OsmBin file-format
 * we have.</b><br/>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class OSMBinPlugin implements PluginLoader {

    @Override
    public Map<String, TaskManagerFactory> loadTaskFactories() {
        // TODO Auto-generated method stub
        HashMap<String, TaskManagerFactory> map = new HashMap<String, TaskManagerFactory>();
        map.put("write-osmbin-0.6", new OsmBinV10WriterFactory());
        map.put("write-osmbin", new OsmBinV10WriterFactory());
        map.put("writeosmbin-0.6", new OsmBinV10WriterFactory());
        map.put("writeosmbin", new OsmBinV10WriterFactory());
        map.put("read-osmbin-0.6", new OsmBinV10ReaderFactory());
        map.put("readosmbin-0.6", new OsmBinV10ReaderFactory());
        map.put("read-osmbin", new OsmBinV10ReaderFactory());
        map.put("readosmbin", new OsmBinV10ReaderFactory());
        map.put("reindex-osmbin-0.6", new OsmBinV10ReindexerFactory());
        map.put("reindex-osmbin", new OsmBinV10ReindexerFactory());
        map.put("reindexosmbin-0.6", new OsmBinV10ReindexerFactory());
        map.put("reindexosmbin", new OsmBinV10ReindexerFactory());
        map.put("dataset-osmbin-0.6", new OsmBinV10DatasetReaderFactory());
        map.put("dataset-osmbin", new OsmBinV10DatasetReaderFactory());
        map.put("datasetosmbin-0.6", new OsmBinV10DatasetReaderFactory());
        map.put("datasetosmbin", new OsmBinV10DatasetReaderFactory());
        return map;
    }
}
