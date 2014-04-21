/**
 * This file is part of LibOSM by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  LibOSM is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LibOSM is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LibOSM.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.osm;

//import java.util.logging.Logger;

import java.io.File;
import java.util.Map;

import org.openstreetmap.osm.Plugins.IPlugin;


/**
 * A ConfigurationSetting informs about a single key that
 * can be added to the Settings. Inclusding it's name and data-type.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class ConfigurationSetting {

    /**
     * my logger for debug and error-output.
     */
    //private static final Logger LOG = Logger.getLogger(ConfigurationSetting.class.getName());

    /**
     * an optional short description
     * that describes this setting in
     * more detail.
     */
    private String myShortDescription;

    /**
     * the key to store this setting under.
     * @see Settings#get(String, String)
     */
    private String myKey;

    /**
     * @param aKey the key to set
     */
    public void setKey(String aKey) {
        myKey = aKey;
    }

    /**
     * The human-readable name to display
     * if no translated version is avaliable.
     */
    private String myDefaultDisplayName;

    /**
     * Within a {@link ConfigurationSection}
     * settings are groupes by equal categories.
     */
    private String myCategory = null;


    /**
     * the possible values to choose from (may be null).<br/>
     * The mapping maps display-name to settings-value.
     */
    private Map<String, String> myValues;

    /**
     * The interface the plugin must implement.
     */
    private Class<? extends IPlugin> myPluginInterface = IPlugin.class;

    /**
     * Does this setting require a restart in order to take effekt?
     */
    private boolean myRequiredRestart = false;

    /**
     * @return The interface the plugin must implement.
     */
    public Class<? extends IPlugin> getPluginInterface() {
        return myPluginInterface;
    }
    /**
     * @param aPluginInterface The interface the plugin must implement.
     */
    public void setPluginInterface(final Class<? extends IPlugin> aPluginInterface) {
        if (aPluginInterface == null)
            throw new IllegalArgumentException("null PluginInterface given");
        this.myPluginInterface = aPluginInterface;
    }


    /**
     * Enumeration of all types
     * a ConfigurationSetting can have.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    public static enum TYPES {
        /**
         * The setting can be any string.
         */
        STRING {
            /**
             * @return the java.lang.Class that represents this type
             */
            public Class<? extends Object> toClass() {
                return String.class;
            };
        },
        /**
         * The setting can only be an integer.
         */
        INTEGER {
            /**
             * @return the java.lang.Class that represents this type
             */
            public Class<? extends Object> toClass() {
                return Integer.class;
            };
        },
        /**
         * The setting can only be a double.
         */
        DOUBLE {
            /**
             * @return the java.lang.Class that represents this type
             */
            public Class<? extends Object> toClass() {
                return Double.class;
            };
        },
        /**
         * The setting is the class-name to instanciate
         * for a plugin. The class muss implement the
         * interface given.
         */
        PLUGIN {
            /**
             * @return the java.lang.Class that represents this type
             */
            public Class<? extends Object> toClass() {
                return String.class;
            };
        },
        /**
         * The setting can only be a boolean.
         */
        BOOLEAN {
            /**
             * @return the java.lang.Class that represents this type
             */
            public Class<? extends Object> toClass() {
                return Boolean.class;
            };
        },
        /**
         * The setting can only be a filename.
         */
        FILE {
            /**
             * @return the java.lang.Class that represents this type
             */
            public Class<? extends Object> toClass() {
                return File.class;
            };
        };

        /**
         * @return the java.lang.Class that represents this type
         */
        public abstract Class<? extends Object> toClass();
    };

    /**
     * The type of this plugin.
     */
    private ConfigurationSetting.TYPES myType = TYPES.STRING;

    /**
     * @return The human-readable name to display (may be translated to the current locale).
     */
    public String getDisplayName() {
        return myDefaultDisplayName;
    }

    /**
     * @return the key to store this setting under.
     */
    public String getKey() {
        return myKey;
    }

    /**
     * @return The type of this plugin.
     */
    public ConfigurationSetting.TYPES getType() {
        return myType;
    }

    /**
     * @param aKey the key to store this setting under.
     * @param aDefaultDisplayName The human-readable name to display.
     * @param aType The type of this plugin.
     * @param aCategory a category within this {@link ConfigurationSection} to group settings.
     * @param aDescription a short description for the user to understand this property.
     */
    public ConfigurationSetting(final String aKey, final String aDefaultDisplayName, final ConfigurationSetting.TYPES aType,
            final String aCategory, final String aDescription) {
        this(aKey, aDefaultDisplayName, aType);
        setCategory(aCategory);
        setShortDescription(aDescription);
    }
    /**
     * @param aKey the key to store this setting under.
     * @param aDefaultDisplayName The human-readable name to display.
     * @param aType The type of this plugin.
     * @param aCategory a category within this {@link ConfigurationSection} to group settings.
     * @param aDescription a short description for the user to understand this property.
     * @param aRequiringRestart does this setting require a restart before taking effect?
     */
    public ConfigurationSetting(final String aKey, final String aDefaultDisplayName, final ConfigurationSetting.TYPES aType,
            final String aCategory, final String aDescription, final boolean aRequiringRestart) {
        this(aKey, aDefaultDisplayName, aType, aCategory, aDescription);
        this.setRequiredRestart(aRequiringRestart);
    }
    /**
     * @param aKey the key to store this setting under.
     * @param aDefaultDisplayName The human-readable name to display.
     * @param aType The type of this plugin.
     */
    public ConfigurationSetting(final String aKey, final String aDefaultDisplayName, final ConfigurationSetting.TYPES aType) {
        if (aKey == null)
            throw new IllegalArgumentException("null key given");
        if (aDefaultDisplayName == null)
            throw new IllegalArgumentException("null defaultDisplayName given");
        if (aType == TYPES.PLUGIN)
            throw new IllegalArgumentException("please use the other constructor for plugin-type settings");
        myKey = aKey;
        myDefaultDisplayName = aDefaultDisplayName;
        myType = aType;
        myValues = null;
    }

    /**
     * @param aPluginInterface the interface the plugin must implement.
     * @param aDefaultDisplayName The human-readable name to display.
     */
    public ConfigurationSetting(final Class<? extends IPlugin> aPluginInterface, final String aDefaultDisplayName) {
        if (aPluginInterface == null)
            throw new IllegalArgumentException("null aPluginInterface given");
        if (aDefaultDisplayName == null)
            throw new IllegalArgumentException("null defaultDisplayName given");

        String name = aPluginInterface.getName();
        name  = name.substring(name.lastIndexOf('.') + 1);
        myKey = "plugin.useImpl." + name;
        myDefaultDisplayName = aDefaultDisplayName;
        myType = TYPES.PLUGIN;
        myPluginInterface = aPluginInterface;
        myValues = null;
    }

    /**
     * @param aPluginInterface the interface the plugin must implement.
     * @param aDefaultDisplayName The human-readable name to display.
     * @param aCategory a category within this {@link ConfigurationSection} to group settings.
     * @param aDescription a short description for the user to understand this property.
     */
    public ConfigurationSetting(final Class<? extends IPlugin> aPluginInterface, final String aDefaultDisplayName,
            final String aCategory, final String aDescription) {
        this(aPluginInterface, aDefaultDisplayName);
        setCategory(aCategory);
        setShortDescription(aDescription);
    }
    /**
     * @param aPluginInterface the interface the plugin must implement.
     * @param aDefaultDisplayName The human-readable name to display.
     * @param aCategory a category within this {@link ConfigurationSection} to group settings.
     * @param aDescription a short description for the user to understand this property.
     * @param aRequiredRestart  does this setting require a restart before taking effect?
     */
    public ConfigurationSetting(final Class<? extends IPlugin> aPluginInterface, final String aDefaultDisplayName,
            final String aCategory, final String aDescription, final boolean aRequiredRestart) {
        this(aPluginInterface, aDefaultDisplayName, aCategory, aDescription);
        this.setRequiredRestart(aRequiredRestart);
    }
    /**
     * an optional short description
     * that describes this setting in
     * more detail.
     * @return the shortDescription
     */
    public String getShortDescription() {
        return myShortDescription;
    }

    /**
     * an optional short description
     * that describes this setting in
     * more detail.
     * @param aShortDescription the shortDescription to set
     */
    public void setShortDescription(final String aShortDescription) {
        myShortDescription = aShortDescription;
    }

    /**
     * Within a {@link ConfigurationSection}
     * settings are groupes by equal categories.
     * @return the category (may be null)
     */
    public String getCategory() {
        return myCategory;
    }

    /**
     * Within a {@link ConfigurationSection}
     * settings are groupes by equal categories.
     * @param aCategory the category to set (null is allowed)
     */
    public void setCategory(final String aCategory) {
        myCategory = aCategory;
    }
    /**
     * @return Does this setting require a restart in order to take effekt?
     */
    public boolean isRequiredRestart() {
        return myRequiredRestart;
    }
    /**
     * @param aRequiredRestart Does this setting require a restart in order to take effekt?
     */
    public void setRequiredRestart(final boolean aRequiredRestart) {
        myRequiredRestart = aRequiredRestart;
    }
    /**
     * The mapping maps display-name to settings-value.
     * @param aValues the possible values to choose from (may be null)
     */
    public void setValues(final Map<String, String> aValues) {
        this.myValues = aValues;
    }

    /**
     * The returned mapping maps display-name to settings-value.
     * @return the possible values to choose from (may be null).
     * @see #myValues
     */
    public Map<String, String> getValues() {
        return myValues;
    }
}
