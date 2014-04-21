/**
 * OsmNavigationConfigSection.java
 * (c) 2007 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of osmnavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  osmnavigation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  osmnavigation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with osmnavigation.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.navigation;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.ConfigurationSetting;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.ConfigurationSetting.TYPES;
import org.openstreetmap.travelingsalesman.gps.DummyGPSProvider;
import org.openstreetmap.travelingsalesman.gps.GPXFileProvider;
import org.openstreetmap.travelingsalesman.gps.GpsDProvider;
import org.openstreetmap.travelingsalesman.gps.IGPSProvider;
import org.openstreetmap.travelingsalesman.gps.JGPSProvider;
import org.openstreetmap.travelingsalesman.gps.NMEAFileProvider;
import org.openstreetmap.travelingsalesman.gps.gpsdemulation.MiniGPSD;
import org.openstreetmap.travelingsalesman.painting.ODRPaintVisitor;
import org.openstreetmap.travelingsalesman.painting.SmoothTilePainter;
import org.openstreetmap.travelingsalesman.painting.IPaintVisitor;
import org.openstreetmap.travelingsalesman.painting.SimplePaintVisitor;
import org.openstreetmap.travelingsalesman.painting.TilePaintVisitor;
import org.openstreetmap.travelingsalesman.painting.odr.GermanMapFeatures;
import org.openstreetmap.travelingsalesman.painting.odr.IMapFeaturesSet;
import org.openstreetmap.travelingsalesman.painting.odr.JosmMapFeatures;
import org.openstreetmap.travelingsalesman.routing.IRouter;
import org.openstreetmap.travelingsalesman.routing.IVehicle;
import org.openstreetmap.travelingsalesman.routing.describers.IRouteDescriber;
import org.openstreetmap.travelingsalesman.routing.describers.SimpleRouteDescriber;
import org.openstreetmap.travelingsalesman.routing.metrics.IRoutingMetric;
import org.openstreetmap.travelingsalesman.routing.metrics.ShortestRouteMetric;
import org.openstreetmap.travelingsalesman.routing.metrics.StaticFastestRouteMetric;
import org.openstreetmap.travelingsalesman.routing.routers.DepthFirstRouter;
import org.openstreetmap.travelingsalesman.routing.routers.DijkstraRouter;
//import org.openstreetmap.travelingsalesman.routing.routers.DijkstraRouter;
import org.openstreetmap.travelingsalesman.routing.routers.DirectedDepthFirstRouter;
import org.openstreetmap.travelingsalesman.routing.routers.MultiTargetDijkstraRouter;
import org.openstreetmap.travelingsalesman.routing.routers.TurnRestrictedAStar;
import org.openstreetmap.travelingsalesman.routing.selectors.Motorcar;
import org.openstreetmap.travelingsalesman.routing.speech.ExternalProgram;
import org.openstreetmap.travelingsalesman.routing.speech.IVoiceSynth;
import org.openstreetmap.travelingsalesman.routing.speech.NoVoice;
//import org.openstreetmap.travelingsalesman.routing.speech.Speechd;


import org.openstreetmap.travelingsalesman.routing.speech.Speechd;

import com.l2fprod.common.util.ResourceManager;

/**
 * This configuration-section groups all settings that parts of
 * osmnavigation offers.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class OsmNavigationConfigSection extends ConfigurationSection {

    /**
     * Out resource-manager for i18n.
     */
    public static final ResourceManager RESOURCE = ResourceManager.get(OsmNavigationConfigSection.class);

    /**
     * Get the version of OSMNavigation we are using.
     * @return the version-string.
     */
    public static final String getVersion() {
        return RESOURCE.getString("osmnavigation.version");
    }

    /**
     */
    public OsmNavigationConfigSection() {
        super(RESOURCE.getString("libosm.configsection.title"));

//        addSetting(new ConfigurationSetting("gpsd.address",
//                       RESOURCE.getString("libosm.configsection.gpsd.host.title"),
//                       TYPES.STRING, RESOURCE.getString("libosm.configsection.gpsd.category"),
//                       RESOURCE.getString("libosm.configsection.gpsd.host.desc")));
//        addSetting(new ConfigurationSetting("gpsd.port",
//                       RESOURCE.getString("libosm.configsection.gpsd.port.title"),
//                       TYPES.INTEGER, RESOURCE.getString("libosm.configsection.gpsd.category"),
//                       RESOURCE.getString("libosm.configsection.gpsd.port.desc")));

//        addSetting(new ConfigurationSetting("JGPSProvider.port",
//                RESOURCE.getString("libosm.configsection.JGPSProvider.port.title"),
//                TYPES.STRING, RESOURCE.getString("libosm.configsection.JGPSProvider.port.category"),
//                RESOURCE.getString("libosm.configsection.JGPSProvider.port.desc")));

        addSetting(new ConfigurationSetting(MiniGPSD.SETTINGS_GPSDEMULATION_PORT,
                RESOURCE.getString("libosm.configsection.emulatedgpsd.port.title"),
                TYPES.STRING, RESOURCE.getString("libosm.configsection.emulatedgpsd.port.category"),
                RESOURCE.getString("libosm.configsection.emulatedgpsd.port.desc")));

        addSetting(new ConfigurationSetting(IRoutingMetric.class,
                       RESOURCE.getString("libosm.configsection.IRoutingMetric.title"),
                       RESOURCE.getString("libosm.configsection.IRoutingMetric.category"),
                       RESOURCE.getString("libosm.configsection.IRoutingMetric.desc")));
        addSetting(new ConfigurationSetting(IRouter.class,
                       RESOURCE.getString("libosm.configsection.IRouter.title"),
                       RESOURCE.getString("libosm.configsection.IRouter.category"),
                       RESOURCE.getString("libosm.configsection.IRouter.desc")));
        addSetting(new ConfigurationSetting(IVehicle.class,
                RESOURCE.getString("libosm.configsection.IVehicle.title"),
                RESOURCE.getString("libosm.configsection.IVehicle.category"),
                RESOURCE.getString("libosm.configsection.IVehicle.desc")));
        addSetting(new ConfigurationSetting(IRouteDescriber.class,
                RESOURCE.getString("travelingsalesman.configsection.IRouteDescriber.title"),
                RESOURCE.getString("travelingsalesman.configsection.IRouteDescriber.category"),
                RESOURCE.getString("travelingsalesman.configsection.IRouteDescriber.desc")));

        addSetting(new ConfigurationSetting(IVoiceSynth.class,
                RESOURCE.getString("travelingsalesman.configsection.IVoiceSynth.title"),
                RESOURCE.getString("travelingsalesman.configsection.IVoiceSynth.category"),
                RESOURCE.getString("travelingsalesman.configsection.IVoiceSynth.desc")));
        addSetting(new ConfigurationSetting(IGPSProvider.class,
                      RESOURCE.getString("libosm.configsection.IGPSProvider.title"),
                      RESOURCE.getString("libosm.configsection.IGPSProvider.category"),
                      RESOURCE.getString("libosm.configsection.IGPSProvider.desc")));
        addSetting(new ConfigurationSetting("routing.reroute.treshold.distFromRouteInKm",
                      RESOURCE.getString("libosm.configsection.recalculateDistance.title"),
                      TYPES.DOUBLE, RESOURCE.getString("libosm.configsection.recalculateDistance.category"),
                      RESOURCE.getString("libosm.configsection.recalculateDistance.desc")));
        addSetting(new ConfigurationSetting("Painter.AntiAliasing",
                RESOURCE.getString("libosm.configsection.AntiAliasing.title"),
                TYPES.BOOLEAN, RESOURCE.getString("libosm.configsection.AntiAliasing.category"),
                RESOURCE.getString("libosm.configsection.AntiAliasing.desc")));
        addSetting(new ConfigurationSetting("Painter.OffScreenCachingStrategy",
                RESOURCE.getString("libosm.configsection.OffScreenCachingStrategy.title"),
                TYPES.BOOLEAN, RESOURCE.getString("libosm.configsection.OffScreenCachingStrategy.category"),
                RESOURCE.getString("libosm.configsection.OffScreenCachingStrategy.desc")));
        addSetting(new ConfigurationSetting("Painter.OffScreenCachingSize",
                RESOURCE.getString("libosm.configsection.OffScreenCachingSize.title"),
                TYPES.DOUBLE, RESOURCE.getString("libosm.configsection.OffScreenCachingSize.category"),
                RESOURCE.getString("libosm.configsection.OffScreenCachingSize.desc")));
//        addSetting(new ConfigurationSetting("NMEAFileProvider.filename",
//                RESOURCE.getString("libosm.configsection.NMEAFileProvider_filename.title"),
//                TYPES.STRING, RESOURCE.getString("libosm.configsection.NMEAFileProvider_filename.category"),
//                RESOURCE.getString("libosm.configsection.NMEAFileProvider_filename.desc")));
//        addSetting(new ConfigurationSetting("GPXFileProvider.filename",
//                RESOURCE.getString("libosm.configsection.GPXFileProvider_filename.title"),
//                TYPES.STRING, RESOURCE.getString("libosm.configsection.GPXFileProvider_filename.category"),
//                RESOURCE.getString("libosm.configsection.GPXFileProvider_filename.desc")));
    }

    static {
        Settings.registerPlugin(IPaintVisitor.class, SmoothTilePainter.class.getName());
        Settings.registerPlugin(IPaintVisitor.class, TilePaintVisitor.class.getName());
        Settings.registerPlugin(IPaintVisitor.class, SimplePaintVisitor.class.getName());
        Settings.registerPlugin(IPaintVisitor.class, ODRPaintVisitor.class.getName());

        Settings.registerPlugin(IRouter.class, DepthFirstRouter.class.getName());
        Settings.registerPlugin(IRouter.class, DirectedDepthFirstRouter.class.getName());
        Settings.registerPlugin(IRouter.class, DijkstraRouter.class.getName());
        Settings.registerPlugin(IRouter.class, MultiTargetDijkstraRouter.class.getName());
        Settings.registerPlugin(IRouter.class, TurnRestrictedAStar.class.getName());

        Settings.registerPlugin(IVehicle.class, Motorcar.class.getName());

        Settings.registerPlugin(IRoutingMetric.class, ShortestRouteMetric.class.getName());
        Settings.registerPlugin(IRoutingMetric.class, StaticFastestRouteMetric.class.getName());

        Settings.registerPlugin(IRouteDescriber.class, SimpleRouteDescriber.class.getName());

        Settings.registerPlugin(IVoiceSynth.class, NoVoice.class.getName());
        Settings.registerPlugin(IVoiceSynth.class, Speechd.class.getName());
        Settings.registerPlugin(IVoiceSynth.class, ExternalProgram.class.getName());

        Settings.registerPlugin(IGPSProvider.class, DummyGPSProvider.class.getName());
        Settings.registerPlugin(IGPSProvider.class, GPXFileProvider.class.getName());
        Settings.registerPlugin(IGPSProvider.class, NMEAFileProvider.class.getName());
        Settings.registerPlugin(IGPSProvider.class, GpsDProvider.class.getName());
        Settings.registerPlugin(IGPSProvider.class, JGPSProvider.class.getName());

        Settings.registerPlugin(IMapFeaturesSet.class, JosmMapFeatures.class.getName());
        Settings.registerPlugin(IMapFeaturesSet.class, GermanMapFeatures.class.getName());
    }
}
