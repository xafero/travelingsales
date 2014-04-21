/**
 * This file is part of Traveling-Salesman by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  Traveling-Salesman is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Traveling-Salesman is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Traveling-Salesman.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.ConfigurationSetting;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.ConfigurationSetting.TYPES;
import org.openstreetmap.osm.data.FileTileDataSet;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.LODDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.OsmBinDataSet;
import org.openstreetmap.osm.data.h2.H2DataSet;
import org.openstreetmap.travelingsalesman.painting.IPaintVisitor;

import com.l2fprod.common.util.ResourceManager;

/**
 * This configuration-section groups all settings that parts of
 * traveling-salesman offers.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class TSNavigationConfigSection extends ConfigurationSection {

    /**
     * Out resource-manager for i18n.
     */
    public static final ResourceManager RESOURCE = ResourceManager.get(TSNavigationConfigSection.class);

    /**
     */
    public TSNavigationConfigSection() {
        super("Traveling-Salesman");
        addSetting(new ConfigurationSetting("filtermap",
                RESOURCE.getString("travelingsalesman.configsection.filtermap.title"), TYPES.BOOLEAN,
                RESOURCE.getString("travelingsalesman.configsection.filtermap.category"),
                RESOURCE.getString("travelingsalesman.configsection.filtermap.desc")));
        addSetting(new ConfigurationSetting("traveling_salesman.DefaultZoomLevel",
                RESOURCE.getString("travelingsalesman.configsection.defaultzoom.title"), TYPES.DOUBLE,
                RESOURCE.getString("travelingsalesman.configsection.defaultzoom.category"),
                RESOURCE.getString("travelingsalesman.configsection.defaultzoom.desc")));

        addSetting(new ConfigurationSetting(IPaintVisitor.class,
                RESOURCE.getString("travelingsalesman.configsection.IPaintVisitor.title"),
                RESOURCE.getString("travelingsalesman.configsection.IPaintVisitor.category"),
                RESOURCE.getString("travelingsalesman.configsection.IPaintVisitor.desc")));

        addSetting(new ConfigurationSetting(IDataSet.class,
                RESOURCE.getString("travelingsalesman.configsection.mapplugin.title"),
                RESOURCE.getString("travelingsalesman.configsection.mapplugin.category"),
                RESOURCE.getString("travelingsalesman.configsection.mapplugin.desc")));
        Settings.registerPlugin(IDataSet.class, LODDataSet.class.getName());
        Settings.registerPlugin(IDataSet.class, OsmBinDataSet.class.getName());
        Settings.registerPlugin(IDataSet.class, H2DataSet.class.getName());
        Settings.registerPlugin(IDataSet.class, MemoryDataSet.class.getName());
        Settings.registerPlugin(IDataSet.class, FileTileDataSet.class.getName());
        addSetting(new ConfigurationSetting("map.dir",
                RESOURCE.getString("travelingsalesman.configsection.mapdir.title"), TYPES.STRING,
                RESOURCE.getString("travelingsalesman.configsection.mapdir.category"),
                RESOURCE.getString("travelingsalesman.configsection.mapdir.desc"), true));

        addSetting(new ConfigurationSetting("verifyMapImports",
                RESOURCE.getString("travelingsalesman.configsection.verifyMapImports.title"), TYPES.BOOLEAN,
                RESOURCE.getString("travelingsalesman.configsection.verifyMapImports.category"),
                RESOURCE.getString("travelingsalesman.configsection.verifyMapImports.desc"), false));

        addSetting(new ConfigurationSetting("renderedTileCache.dir",
                RESOURCE.getString("travelingsalesman.configsection.renderedTileCacheDir.title"), TYPES.STRING,
                RESOURCE.getString("travelingsalesman.configsection.renderedTileCacheDir.category"),
                RESOURCE.getString("travelingsalesman.configsection.renderedTileCacheDir.desc")));

        addSetting(new ConfigurationSetting("traveling-salesman.stripDownload.width",
                RESOURCE.getString("travelingsalesman.configsection.stripDownload.title"), TYPES.DOUBLE,
                RESOURCE.getString("travelingsalesman.configsection.stripDownload.category"),
                RESOURCE.getString("travelingsalesman.configsection.stripDownload.desc")));

        addSetting(new ConfigurationSetting("traveling-salesman.stripDownload.automatic",
                RESOURCE.getString("travelingsalesman.configsection.doDownload.title"), TYPES.BOOLEAN,
                RESOURCE.getString("travelingsalesman.configsection.doDownload.category"),
                RESOURCE.getString("travelingsalesman.configsection.doDownload.desc")));

        addSetting(new ConfigurationSetting("traveling-salesman.maxStripLength",
                RESOURCE.getString("travelingsalesman.configsection.maxStripLength.title"), TYPES.DOUBLE,
                RESOURCE.getString("travelingsalesman.configsection.maxStripLength.category"),
                RESOURCE.getString("travelingsalesman.configsection.maxStripLength.desc")));
        
        addSetting(new ConfigurationSetting("traveling-salesman.loadFile.lastPath",
                RESOURCE.getString("travelingsalesman.configsection.lastPath.title"), TYPES.STRING,
                RESOURCE.getString("travelingsalesman.configsection.lastPath.category"),
                RESOURCE.getString("travelingsalesman.configsection.lastPath.desc")));

        addSetting(new ConfigurationSetting("traveling-salesman.restoreLastState",
                RESOURCE.getString("travelingsalesman.configsection.restoreLastState.title"), TYPES.BOOLEAN,
                RESOURCE.getString("travelingsalesman.configsection.restoreLastState.category"),
                RESOURCE.getString("travelingsalesman.configsection.restoreLastState.desc")));

        addSetting(new ConfigurationSetting("traveling-salesman.nmeaLogger",
                RESOURCE.getString("travelingsalesman.configsection.nmeaLogger.title"), TYPES.BOOLEAN,
                RESOURCE.getString("travelingsalesman.configsection.nmeaLogger.category"),
                RESOURCE.getString("travelingsalesman.configsection.nmeaLogger.desc")));

        addSetting(new ConfigurationSetting("showAllTags",
                RESOURCE.getString("travelingsalesman.configsection.showAllTags.title"), TYPES.BOOLEAN,
                RESOURCE.getString("travelingsalesman.configsection.showAllTags.category"),
                RESOURCE.getString("travelingsalesman.configsection.showAllTags.desc")));
    }

}
