/**
 * ExternalProgram.java
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
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.ConfigurationSetting;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.travelingsalesman.navigation.OsmNavigationConfigSection;



/**
 * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: osmnavigation<br/>
 * ExternalProgram.java<br/>
 * created: 21.03.2009 07:01:37 <br/>
 *<br/><br/>
 * <b>using an external program as a voice</b>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class ExternalProgram implements IVoiceSynth {

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(ExternalProgram.class.getName());

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "ExternalProgram@" + hashCode();
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public void speak(final String aSentence) {
        try {
            String cmdln = Settings.getInstance().get("libosm.speech.externalprogram.cmdln", "/usr/bin/spd-say \"{0}\"");
            String runme = MessageFormat.format(cmdln, aSentence);
            //System.err.println(runme);
            Runtime.getRuntime().exec(runme);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[Exception] Problem in "
                       + getClass().getName(),
                         e);
        }
    }
    /**
     * Test if the local speechd works.
     * @param args ignored
     */
    public static void main(final String[] args) {
        new ExternalProgram().speak("this is a test");
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public ConfigurationSection getSettings() {
        ConfigurationSection speechd = new ConfigurationSection("external voice");
      //================================= host
        Settings.getInstance().get("libosm.speech.externalprogram.cmdln", "/usr/bin/spd-say \"{0}\"");
        ConfigurationSetting hostSetting = new ConfigurationSetting("libosm.speech.externalprogram.cmdln",
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.externalprogram.cmdln.title"),
                ConfigurationSetting.TYPES.STRING,
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.externalprogram.cmdln.category"),
                OsmNavigationConfigSection.RESOURCE.getString("libosm.configsection.externalprogram.cmdln.desc"));
        speechd.addSetting(hostSetting);
        return speechd;
    }
}


