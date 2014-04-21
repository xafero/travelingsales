/**
 * Speechd.java
 * created: 21.03.2009 07:01:37
 * (c) 2009 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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
package org.openstreetmap.travelingsalesman.routing.speech;

//automatically created logger for debug and error -output
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.ConfigurationSetting;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.travelingsalesman.navigation.OsmNavigationConfigSection;

import speechd.ssip.SSIPClient;
import speechd.ssip.SSIPException;
import speechd.ssip.SSIPPriority;


/**
 * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: osmnavigation<br/>
 * Speechd.java<br/>
 * created: 21.03.2009 07:01:37 <br/>
 *<br/><br/>
 * <b>a voice using a local speech-dispatcher daemon.</b>
 * See <a href="http://www.freebsoft.org/speechd">Speechd</a>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class Speechd implements IVoiceSynth {

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(Speechd.class.getName());

    /**
     * The client we are using to communicate with the SSIP-server.
     */
    private SSIPClient mySSIPClient = null;

    /**
     * @return Returns the sSIPClient.
     * @throws SSIPException if we cannot create the client
     * @see #mySSIPClient
     */
    public SSIPClient getSSIPClient() throws SSIPException {
        if (this.mySSIPClient == null) {
            final String name = "TravelingSalesman";
            final String component = null;
            final String user = null;
            Settings settings = Settings.getInstance();
            try {
                System.setProperty("speechd.host", settings.get("speechd.host", SSIPClient.DEFAULT_HOST));
            } catch (Exception e2) {
                LOG.log(Level.SEVERE, "[Exception] Problem in "
                           + getClass().getName(),
                             e2);
            }
            try {
                System.setProperty("speechd.port", "" + settings.getInteger("speechd.port", SSIPClient.DEFAULT_PORT));
            } catch (Exception e2) {
                LOG.log(Level.SEVERE, "[Exception] Problem in "
                           + getClass().getName(),
                             e2);
            }
            this.mySSIPClient = new SSIPClient(name, component, user);
            try {
                String module = settings.get("speechd.outputmodule", "");
                if (module != null && module.trim().length() > 0) {
                    this.mySSIPClient.setOutputModule(module);
                }
            } catch (Exception e1) {
                LOG.log(Level.SEVERE, "[Exception] Problem in "
                           + getClass().getName(),
                             e1);
            }
            try {
                String voice = settings.get("speechd.voice", "");
                if (voice != null && voice.trim().length() > 0) {
                    this.mySSIPClient.setVoice(voice);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "[Exception] Problem in "
                           + getClass().getName(),
                             e);
            }
        }

        return this.mySSIPClient;
    }

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "Speechd@" + hashCode();
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public void speak(final String aSentence) {
        try {
            SSIPClient client = getSSIPClient();
            client.say(SSIPPriority.MESSAGE, aSentence);
        } catch (SSIPException e) {
            LOG.log(Level.WARNING, "[SSIPException] Problem in "
                       + getClass().getName(),
                         e);
        }
    }
    /**
     * Test if the local speechd works.
     * @param args ignored
     */
    public static void main(final String[] args) {
        new Speechd().speak("test");
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public ConfigurationSection getSettings() {
        ConfigurationSection speechd = new ConfigurationSection("speechd");
      try {
        //================================= host
            Settings.getInstance().get("speechd.host", SSIPClient.DEFAULT_HOST);
            ConfigurationSetting hostSetting = new ConfigurationSetting("speechd.host",
                    OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.host.title"),
                    ConfigurationSetting.TYPES.STRING,
                    OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.host.category"),
                    OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.host.desc"));
            speechd.addSetting(hostSetting);
          //================================= port
            Settings.getInstance().getInteger("speechd.port", SSIPClient.DEFAULT_PORT);
            ConfigurationSetting portSetting = new ConfigurationSetting("speechd.port",
                    OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.port.title"),
                    ConfigurationSetting.TYPES.INTEGER,
                    OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.port.category"),
                    OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.port.desc"));
            speechd.addSetting(portSetting);
            //================================= outputmodule
            ConfigurationSetting outputmoduleSetting = new ConfigurationSetting("speechd.outputmodule",
                    OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.outputmodule.title"),
                    ConfigurationSetting.TYPES.STRING,
                    OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.outputmodule.category"),
                    OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.outputmodule.desc"));
            try {
                Map<String, String> modules = new HashMap<String, String>();
                modules.put(OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.outputmodule.default"), "");
                List<String> modules2 = getSSIPClient().getOutputModules();
                for (String module : modules2) {
                    modules.put(module, module);
                }
                outputmoduleSetting.setValues(modules);
            } catch (SSIPException e) {
                LOG.log(Level.WARNING, "[SSIPException] Problem in "
                           + getClass().getName(),
                             e);
            }
            speechd.addSetting(outputmoduleSetting);
            //================================= voice
            ConfigurationSetting voiceSetting = new ConfigurationSetting("speechd.voice",
                    OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.voice.title"),
                    ConfigurationSetting.TYPES.STRING,
                    OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.voice.category"),
                    OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.voice.desc"));
            try {
                Map<String, String> voices = new HashMap<String, String>();
                voices.put(OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.speechd.voice.default"), "");
                List<String> voices2 = getSSIPClient().getVoices();
                for (String voice : voices2) {
                    voices.put(voice, voice);
                }
                voiceSetting.setValues(voices);
            } catch (SSIPException e) {
                LOG.log(Level.WARNING, "[SSIPException] Problem in "
                           + getClass().getName(),
                             e);
            }
            speechd.addSetting(voiceSetting);
    } catch (Exception e) {
        LOG.log(Level.WARNING, "", e);
    }
        return speechd;
    }
}


