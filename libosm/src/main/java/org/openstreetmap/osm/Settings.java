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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.java.plugin.ObjectFactory;
import org.java.plugin.Plugin;
import org.java.plugin.PluginLifecycleException;
import org.java.plugin.PluginManager;
import org.java.plugin.PluginManager.PluginLocation;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.ExtensionPoint;
import org.java.plugin.registry.ManifestProcessingException;
import org.java.plugin.registry.PluginDescriptor;
import org.java.plugin.registry.Extension.Parameter;
import org.java.plugin.standard.StandardPluginLocation;
import org.openstreetmap.osm.ConfigurationSetting.TYPES;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.projection.Epsg4326;
import org.openstreetmap.osm.data.projection.Mercator;
import org.openstreetmap.osm.data.projection.Projection;
import org.openstreetmap.osm.data.searching.IPlaceFinder;
import org.openstreetmap.osm.data.searching.InetPlaceFinder;
import org.openstreetmap.osm.data.searching.SimplePlaceFinder;
import org.openstreetmap.osm.data.searching.advancedAddressDB.AdvancedAddressDBPlaceFinder;

import com.l2fprod.common.util.ResourceManager;

/**
 * This class contains basic settings
 * that are required to work with OSM-data.
 * (Like the projection-method to use)
 *
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public final class Settings {

    /**
     * The name of the JPF-core-plugin we offer.
     */
    private static final String COREPLUGIN = "org.openstreetmap.osm.Plugins.Core";

    /**
     * Name of the extension-point we use in JPF.
     */
    private static final String COREEXTENSIONPOINT = "GenericPlugin";

    /**
     * Helper for implementing support for {@link PropertyChangeListener}s.
     */
    private PropertyChangeSupport myPropertyChangeSupport = new PropertyChangeSupport(this);

    /**
     * @param aListener a new PropertyChangeListener to add.
     */
    public void addPropertyChangeListener(final PropertyChangeListener aListener) {
        myPropertyChangeSupport.addPropertyChangeListener(aListener);
    }

    /**
     * @param aListener a new PropertyChangeListener to add.
     * @param aPropertyName the name of the property we are interested in
     */
    public void addPropertyChangeListener(final String aPropertyName, final PropertyChangeListener aListener) {
        myPropertyChangeSupport.addPropertyChangeListener(aPropertyName, aListener);
    }

    /**
     * @param aListener a new PropertyChangeListener to add.
     * @param aPluginInterface the interface of the plugin-type we are interested in
     */
    public void addPropertyChangeListener(final Class<? extends IPlugin> aPluginInterface, final PropertyChangeListener aListener) {
        myPropertyChangeSupport.addPropertyChangeListener(getPropertyNameForPlugin(aPluginInterface), aListener);
    }

    /**
     * Create a new PluginProxy that will always route your
     * method-calls to the latest configured plugin.
     * @param <T> the plugin-interface
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    private static final class PluginProxyHandler<T extends IPlugin> implements InvocationHandler, PropertyChangeListener {

        /**
         * The proxy-interface we work for to pass to
         * {@link Settings#getPlugin(Class, String)}.
         */
        private Class<T> myIFace;

        /**
         * The default-class-name to pass to
         * {@link Settings#getPlugin(Class, String)}.
         */
        private String myDefaultClassName;

        /**
         * The current instance of the plugin we
         * are proxying for.
         */
        private T myInstance = null;

        /**
         * Arguments to add*Listener-methods are saved here.
         */
        private Map<Object[], Method> myListeners = new HashMap<Object[], Method>();

        /**
         * Create a new PluginProxy that will always route your
         * method-calls to the latest configured plugin.
         * @param aIface the plugin-interface
         * @param aDefaultClassName the default-plugin
         */
        public PluginProxyHandler(final Class<T> aIface, final String aDefaultClassName) {
            this.myIFace = aIface;
            this.myDefaultClassName = aDefaultClassName;
            Settings.getInstance().addPropertyChangeListener(aIface, this);
        }

        /**
         * {@inheritDoc}
         */
        public Object invoke(final Object proxy, final Method m, final Object[] args) throws Throwable {
            if (myInstance == null) {
                myInstance = Settings.getInstance().getPlugin(myIFace, myDefaultClassName);
            }
            if (myInstance == null) {
                LOG.severe("No plugin configured for \n"
                        + myIFace.getName()
                        + " and the default-plugin\n"
                        + myDefaultClassName
                        + " was not loaded");
                // TODO: Remove this hack later, when the configuration is ... clear!
                Class<?> type = Class.forName(myDefaultClassName);
                Object instance = type.newInstance();
                myInstance = (T) instance;
                /* return null; */
            }
            if (m.getName().matches("add.*Listener")) {
                this.myListeners.put(args, m);
            }
            if (m.getName().matches("remove.*Listener")) {
                // this may not work but it does not really matter
                this.myListeners.remove(args);
            }
            if (args == null) {
//this is okay LOG.warning("null arguments-list given to method " + myInstance.getClass().getName() + ":" + m.getName() + "(..), trying empty argument list. This may fail.");
                return m.invoke(myInstance, new Object[]{});
            } else {
                return m.invoke(myInstance, args);
            }
        }

        /**
         * We are only registred to the property of the plugin
         * we are proxying for. Thus we remove our instance
         * so it may be re-created for the next use uppon
         * a property-change.
         * @param arg0 ignored
         */
        public void propertyChange(final PropertyChangeEvent arg0) {
            // remove listeners
            removeListeners();
            myInstance = null;
            // re-add listeners
            readdListeners();
        }

        /**
         * The plugin to be used has changed.
         * Try our best to re-attach the listeners
         * the old instance had.
         */
        private void readdListeners() {
            if (myListeners.size() > 0) {
                myInstance = Settings.getInstance().getPlugin(myIFace, myDefaultClassName);
                Set<Entry<Object[], Method>> listeners = myListeners.entrySet();
                for (Entry<Object[], Method> entry : listeners) {
                    try {
                        entry.getValue().invoke(myInstance, entry.getKey());
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Cannot re-add listeners", e);
                    }
                }
            }
        }

        /**
         * The plugin to use has changed.
         * Try our best to remove all known listeners from
         * the old instance.
         */
        private void removeListeners() {
            if (myListeners.size() > 0 && myInstance != null) {
                Set<Entry<Object[], Method>> listeners = myListeners.entrySet();
                Method[] methods = myInstance.getClass().getMethods();
                for (Entry<Object[], Method> entry : listeners) {
                    try {
                        String methodName = entry.getValue().getName().replace("add", "remove");
                        for (int i = 0; i < methods.length; i++) {
                            if (methods[i].getName().equals(methodName)
                                    && methods[i].getParameterTypes().length == entry.getKey().length) {
                                try {
                                    methods[i].invoke(myInstance, entry.getKey());
                                } catch (Exception e) {
                                    LOG.log(Level.SEVERE, "Cannot remove listener", e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOG.log(Level.SEVERE, "Cannot enumerate methods to remove listeners", e);
                    }
                }
            }
        }
    }

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(Settings.class.getName());

    /**
     * PluginManager to allow working with JPF-plugins.
     */
    private static PluginManager myPluginManager;

    /**
     * utility-classes have no public constructor.
     */
    private Settings() {
        myProperties = new Properties();        
        myOverrideProperties = new Properties();
        try {
            File preferencesFile = getPreferencesFile();
            if (preferencesFile.exists())
                myProperties.loadFromXML(new FileInputStream(preferencesFile));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not load the (existing) preferences-file", e);
            e.printStackTrace();
        }
    }

    /**
     * The singleton-instance.
     */
    private static Settings instance;

    /**
     * @return The singleton-instance.
     */
    public static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }

    /**
     * indexed by the interface they implement, get all class-names of implementations
     * of this interface.
     */
    private static HashMap<Class<? extends IPlugin>, Set<String>> myPluginValuesByInterface = new HashMap<Class<? extends IPlugin>, Set<String>>();

    /**
     * Global application preferences of JOSM.
     * We use them to load the osm-userlogin
     * of JOSM's preferences-file
     * Known preferences are:
     * "csv.importstring"
     * "osm-server.url" (mandatory)
     * "osm-server.version" default="0.4"
     * "osm-server.username"
     * "osm-server.password"
     */
    private static JosmPreferences pref = new JosmPreferences();

    /**
     * Our own preferences.
     * @see #myOverrideProperties
     */
    private Properties myProperties = null;

    /**
     * Our own preferences.<br/>
     * These settings have priority over the ones in {@link #myProperties}
     * but are never stored on disk.
     */
    private Properties myOverrideProperties = null;

    /**
     * Out resource-manager for i18n.
     */
    public static final ResourceManager RESOURCE = ResourceManager.get(Settings.class);

    /**
     * This is where we store all the configuration-sections
     * in order to allow applications to build a generic
     * (and dynamic) preferences-dialog.
     */
    private static ConfigurationSection myRootConfigurationSection = new ConfigurationSection("configuration");

    static {
        ConfigurationSection serverSection = new ConfigurationSection("OpenStreetMap");
        myRootConfigurationSection.addSubSections(serverSection);
        serverSection.addSetting(new ConfigurationSetting("osm.ServerBaseURL.v0.6",
                RESOURCE.getString("libosm.settings.mapServerURL.title"), TYPES.STRING,
                RESOURCE.getString("libosm.settings.mapServerURL.category"),
                RESOURCE.getString("libosm.settings.mapServerURL.desc")));
        serverSection.addSetting(new ConfigurationSetting("map.dir",
                RESOURCE.getString("libosm.settings.mapdir.title"), TYPES.STRING,
                RESOURCE.getString("libosm.settings.mapdir.category"),
                RESOURCE.getString("libosm.settings.mapdir.desc"), true));
        serverSection.addSetting(new ConfigurationSetting(Projection.class,
                RESOURCE.getString("libosm.settings.projection.title"),
                RESOURCE.getString("libosm.settings.projection.category"),
                RESOURCE.getString("libosm.settings.projection.desc")));
        serverSection.addSetting(new ConfigurationSetting(IPlaceFinder.class,
                RESOURCE.getString("libosm.settings.IPlaceFinder.title"),
                RESOURCE.getString("libosm.settings.IPlaceFinder.category"),
                RESOURCE.getString("libosm.settings.IPlaceFinder.desc")));


        registerPlugin(Projection.class, Epsg4326.class.getName());
        registerPlugin(Projection.class, Mercator.class.getName());

        registerPlugin(IPlaceFinder.class, SimplePlaceFinder.class.getName());
        registerPlugin(IPlaceFinder.class, InetPlaceFinder.class.getName());
        //replaced by improved implementaion registerPlugin(IPlaceFinder.class, AddressDBPlaceFinder.class.getName());
        registerPlugin(IPlaceFinder.class, AdvancedAddressDBPlaceFinder.class.getName());
    }

    /**
     * Global application preferences of JOSM.
     * We use them to load the osm-userlogin
     * of JOSM's preferences-file.
     * @return Global osm preferences
     */
    public static JosmPreferences getPreferences() {
        if (pref == null) {
            pref = new JosmPreferences();
        }

        return pref;
    }

    /**
     * The global dataset.
     */
    //private static DataSet myDataSet = new DataSet();

    /**
     * The {@link Projection}} method used.
     */
    private static Projection myProj;

    /**
     * @return The {@link Projection}} method used.
     */
    public static Projection getProjection() {
        if (myProj == null) {
            try {
                String defaultProjectionName = Settings.pref.get("projection", org.openstreetmap.osm.data.projection.Epsg4326.class.getName());
                String className = Settings.getInstance().get("plugin.useImpl.Projection", defaultProjectionName);
                Settings.myProj = (Projection) Class.forName(className).newInstance();
            } catch (final Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "The projection could not be read from preferences. Using EPSG:4326.");
                Settings.myProj = new Epsg4326();
            }
        }
        return myProj;
    };

    /**
     * @return the location of the user defined preferences file.
     */
    private File getPreferencesFile() {
        if (System.getenv("APPDATA") != null) {
            return new File(System.getenv("APPDATA") + "/libosm/preferences.xml");
        }
        return new File(System.getProperty("user.home") + "/.libosm/preferences.xml");
    }

    /**
     * Get a property from our own preferences.
     * @param key the property-name
     * @param defaultValue the default-value
     * @return the property
     */
    public synchronized String get(final String key, final String defaultValue) {
        if (myOverrideProperties.containsKey(key)) {
            return myOverrideProperties.getProperty(key);
        }
        
        if (!myProperties.containsKey(key)) {
            // special handling for properties that may be stored in josms preferences
            // try to fetch them from there.
            if (key.equals("osm-server.url") || key.equals("osm-server.use-compression")) {
                return getPreferences().get(key, defaultValue);
            }
            //org.openstreetmap.osm.Settings.getPreferences().put("osm-server.url", "http://www.openstreetmap.org/api");
            //org.openstreetmap.osm.Settings.getPreferences().put("osm-server.use-compression", "true");

            if (defaultValue != null)
                this.put(key, defaultValue);
            return defaultValue;
        }
        String propValue = myProperties.getProperty(key, defaultValue);
        return (propValue == null) || (propValue.trim().isEmpty()) ? defaultValue : propValue;
    }

    /**
     * Get a property from our own preferences.
     * @param key the property-name
     * @param defaultValue the default-value
     * @return the property
     */
    public synchronized boolean getBoolean(final String key, final boolean defaultValue) {
        boolean val;
        try {
            val = Boolean.parseBoolean(get(key, Boolean.toString(defaultValue)));
        } catch (NumberFormatException e) {
            LOG.log(Level.SEVERE, "getBoolean(key=" + key + ")=" + get(key, Boolean.toString(defaultValue)), e);
            val = defaultValue;
            put(key, Boolean.toString(defaultValue));
        }
        return val;
    }

    /**
     * Get a property from our own preferences.
     * @param key the property-name
     * @param defaultValue the default-value
     * @return the property
     */
    public int getInteger(final String key, final int defaultValue) {
        int val;
        try {
            val = Integer.parseInt(get(key, Integer.toString(defaultValue)));
        } catch (NumberFormatException e) {
            LOG.log(Level.SEVERE, "getInteger(key=" + key + ")=" + get(key, Integer.toString(defaultValue)), e);
            val = defaultValue;
            put(key, Integer.toString(defaultValue));
        }
        return val;
    }

    /**
     * Get a property from our own preferences.
     * @param key the property-name
     * @param defaultValue the default-value
     * @return the property
     */
    public double getDouble(final String key, final double defaultValue) {
        double val;
        try {
            val = Double.parseDouble(get(key, Double.toString(defaultValue)));
        } catch (NumberFormatException e) {
            LOG.log(Level.SEVERE, "getDouble(key=" + key + ")=" + get(key, Double.toString(defaultValue)), e);
            val = defaultValue;
            put(key, Double.toString(defaultValue));
        }
        return val;
    }

    /**
     * Store a property.
     * Errors on writing the file are reported but raise no exception.
     * @param key the property-name
     * @param value the new value
     */
    public synchronized void put(final String key, final String value) {

        LOG.log(Level.FINE, "Settings.put(key='" + key + "', value='" + value + "')");

        String old = myProperties.getProperty(key);

        // remove override
        if (myOverrideProperties.containsKey(key)) {
            myOverrideProperties.remove(key);
        }

        if (value == null) {
            if (myProperties.remove(key) == null) {
                return; // nothing changed
            }
        } else {
            if (myProperties.put(key, value) == value) {
                return; // nothing changed
            }
        }
        try {
            File prefFile = getPreferencesFile();
            if (!prefFile.getParentFile().exists())
                if (!prefFile.getParentFile().mkdirs())
                    LOG.log(Level.SEVERE, "Cannot create directors " + prefFile.getParentFile().getAbsolutePath() + " to store preferences-file in!");
            myProperties.storeToXML(new FileOutputStream(prefFile), "UTF-8");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not save the preferences-file", e);
            e.printStackTrace();
        }
        myPropertyChangeSupport.firePropertyChange(key, old, value);
    }

    /**
     * Store a property.
     * Errors on writing the file are reported but raise no exception.
     * @param key the property-name
     * @param value the new value
     */
    public synchronized void override(final String key, final String value) {

        LOG.log(Level.FINE, "override(key='" + key + "', value='" + value + "')");

        String old = get(key, null);

        if (value == null) {
            if (myOverrideProperties.remove(key) == null) {
                return; // nothing changed
            }
        } else {
            if (myOverrideProperties.put(key, value) == value) {
                return; // nothing changed
            }
        }
        myPropertyChangeSupport.firePropertyChange(key, old, value);
    }

    /**
     * Where {@link #getPlugin(Class, String)} returns the plugin
     * currently configured. This method returns a dynamic proxy
     * that changes at runtime when the configured plugin changes.
     * @param <T> the interface the result must implement.
     * @param iface the interface the result must implement.
     * @param defaultClassName the default-value
     * @return the a dynamic proxy to the plugin-instance or an exception.
     */
    @SuppressWarnings("unchecked")
    public <T extends IPlugin> T getPluginProxy(final Class<T> iface, final String defaultClassName) {
        return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{iface}, new PluginProxyHandler<T>(iface, defaultClassName));
    }

    /**
     * @param iface the interface the plugin must implement.
     * @return the name of the property storing the class-name to instanciate.
     */
    private String getPropertyNameForPlugin(final Class<? extends IPlugin> iface) {
        String name = iface.getName();
        name  = name.substring(name.lastIndexOf('.') + 1);

        return "plugin.useImpl." + name;
    }

    /**
     * Load the plugin that is to be used to provide
     * the contract given by the interface.
     * @param <T> the interface the result must implement.
     * @param iface the interface the result must implement.
     * @param defaultClassName the default-value
     * @return the plugin-instance or an exception.
     */
    @SuppressWarnings("unchecked")
    private <T extends IPlugin> T getPlugin_internal(final Class<T> iface, final String className, final Object[] aArguments) {
        Object o = null;
        if (className == null) {
            return null;
        }
        if (className.startsWith("JPF:")) {
            // use Java Plugin Framework
            try {
                String[] splits = className.split(":");
                PluginManager pluginManager = getPluginManager();
                PluginDescriptor core = pluginManager.getRegistry().getPluginDescriptor(COREPLUGIN);
                ExtensionPoint point = pluginManager.getRegistry().getExtensionPoint(core.getId(), COREEXTENSIONPOINT);
                for (Iterator<Extension> it = point.getConnectedExtensions().iterator(); it.hasNext();) {
                    Extension ext = it.next();
                    PluginDescriptor descr = ext.getDeclaringPluginDescriptor();
                    if (!ext.getId().equals(splits[1])) {
                        continue;
                    }
                    pluginManager.enablePlugin(descr, true);
                    pluginManager.activatePlugin(descr.getId());
                    ClassLoader pluginClassLoader = pluginManager.getPluginClassLoader(descr);
                    Class<?> loadClass = pluginClassLoader.loadClass(ext.getParameter("class").valueAsString());
                    o = instanciatePlugin(loadClass, aArguments);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING , "Settings.getPlugin() - cannot instanciate configured JPF-plugin '" + className + "' - trying default class-name", e);
            }
        } else {
            // use Class.forName
            try {
                Class<?> pluginClass = Class.forName(className);
                o = instanciatePlugin(pluginClass, aArguments);
            } catch (Exception e) {
                LOG.log(Level.WARNING , "Settings.getPlugin() - cannot instanciate configured '" + className + "' - trying default class-name", e);
            }
        }

        if (o != null && !iface.isAssignableFrom(o.getClass())) {
            String name2 = "null";
            if (o != null)
                name2 = o.getClass().getName();
            LOG.log(Level.SEVERE, "cannot use " + name2
                                             + " as an " + iface.getName());
            o = null;
        }

        if (o != null && !IPlugin.class.isAssignableFrom(o.getClass())) {
            String name2 = "null";
            if (o != null)
                name2 = o.getClass().getName();
            LOG.log(Level.SEVERE, "cannot use " + name2
                                             + " as a plugin! It does not implement the IPlugin-interface!");
            o = null;
        }

        return (T) o;
    }

    /**
     * @param loadClass
     * @param aArguments 
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private Object instanciatePlugin(Class<?> loadClass, Object[] aArguments)
            throws InstantiationException, IllegalAccessException {
        if (aArguments == null || aArguments.length == 0) {
            return loadClass.newInstance();
        }

        Constructor<?>[] constructors = loadClass.getConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterTypes().length == aArguments.length) {
                try {
                    return constructor.newInstance(aArguments);
                } catch (final Exception e) {
                    LOG.log(Level.WARNING, "chosen plugin-constructor not working", e);
                }
            }
        }
        LOG.log(Level.WARNING, "No constructor in plugin that matches provided arguments");
        return null;
    }
    /**
     * Load the plugin that is to be used to provide
     * the contract given by the interface.
     * @param <T> the interface the result must implement.
     * @param iface the interface the result must implement.
     * @param defaultClassName the default-value
     * @return the plugin-instance or an exception.
     */
    @SuppressWarnings("unchecked")
    public <T extends IPlugin> T getPlugin(final Class<T> iface, final String defaultClassName) {

        String className = get(getPropertyNameForPlugin(iface), defaultClassName);
        if (className == null || className.trim().length() == 0) {
            LOG.warning("No plugin configured for " + iface.getName());
            return null;
        }
        T o = getPlugin_internal(iface, className, null);
        if (o == null) {
            getPlugin_internal(iface, defaultClassName, null);
        }
        if (o == null) {
            LOG.severe("Unable to load the configured or the default-plugin for " + iface.getName());
            return null;
        }

        return o;
    }



    /**
     * Get all plugins that are registred for an interface.
     * @param iFace the interface the plugins provide
     * @return a map from a displayable name to the value you have to set the property to to choose this plugin
     * @see #registerPlugin(Class, String)
     * @see #getRegistredPlugins(Class)
     */
    public static Map<String, String> getRegistredPlugins(final Class<? extends IPlugin> iFace) {

        if (iFace == null)
            throw new IllegalArgumentException("null plugin-interface given");
        Map<String, String> retval = new LinkedHashMap<String, String>();

        // add old-style plugins
        Set<String> list = myPluginValuesByInterface.get(iFace);
        if (list == null)
            list = new HashSet<String>();
        for (String pluginClass : list) {
            int idx = pluginClass.lastIndexOf('.');
            String visualValue = pluginClass;
            if (idx > 0) {
                visualValue =  pluginClass.substring(idx + 1);
            }
            retval.put(visualValue, pluginClass);
        }

        // add JPF-style plugins
        try {
            PluginManager pluginManager = getPluginManager();
            Plugin corePlugin = pluginManager.getPlugin(COREPLUGIN);
            ExtensionPoint extensionPoint = corePlugin.getDescriptor().getExtensionPoint(COREEXTENSIONPOINT);
            Collection<Extension> availableExtensions = extensionPoint.getAvailableExtensions();
            for (Extension extension : availableExtensions) {
                Parameter interfaceProvided = extension.getParameter("interfaceProvided");
                if (iFace.getName().equals(interfaceProvided.valueAsString())) {
                    retval.put(extension.getParameter("name").valueAsString(),
                            "JPF:"
                            + extension.getId()
                            );
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot list JPF-plugins for " + iFace.getName() , e);
        }
        return Collections.unmodifiableMap(retval);
    }

    /**
     * Register a new plugin.
     * @param iFace the interface it provides
     * @param className the fully qualified class-name
     * @see #getRegistredPlugins(Class)
     * @see #getRegistredPlugins(Class)
     */
    public static void registerPlugin(final Class<? extends IPlugin> iFace, final String className) {

        if (iFace == null)
            throw new IllegalArgumentException("null plugin-interface given");
        if (className == null)
            throw new IllegalArgumentException("null class-name given");

        Set<String> list = myPluginValuesByInterface.get(iFace);
        if (list == null) {
            list = new HashSet<String>();
            myPluginValuesByInterface.put(iFace, list);
        }
        list.add(className);
    }

    /**
     * The DataFormat used in OSM for modification-timestamps.
     */
    public static final DateFormat TIMESTAMPFORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * @return the rootConfigurationSection
     */
    public static ConfigurationSection getRootConfigurationSection() {
        return myRootConfigurationSection;
    }

    /**
     * @return the myPluginManager
     */
    private static PluginManager getPluginManager() {
        if (myPluginManager == null) {
         try {
            // Create a new JPF plugin manager.
                myPluginManager = ObjectFactory.newInstance().createManager();

                // Search known locations for plugin files.
                LOG.fine("Searching for JPF plugins.");
                List<PluginLocation> locations = gatherJpfPlugins();

                // Register the core plugin.
                LOG.fine("Registering the core plugin.");
                registerCorePlugin(myPluginManager);

                // Register all located plugins.
                LOG.fine("Registering the extension plugins.");
                if (locations.size() != 0) {
                    registerJpfPlugins(myPluginManager, locations);
                }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Cannot create PluginManager and initialize JPF-plugins", e);
        } catch (java.lang.NoClassDefFoundError e) {
            LOG.log(Level.SEVERE, "Cannot create PluginManager and initialize JPF-plugins", e);
        }
        }
        return myPluginManager;
    }


    /**
     * @return a list of all JPF-plugins found.
     */
    private static List<PluginLocation> gatherJpfPlugins() {
        File[] pluginsDirs = new File[] {
                new File("plugins"),
                new File(System.getProperty("user.home") + "/.openstreetmap" + File.separator + "libosm"
                        + File.separator + "plugins"),
                new File(System.getProperty("user.home") + "/.openstreetmap" + File.separator + "osmosis"
                        + File.separator + "plugins"),
                new File(System.getenv("APPDATA") + File.separator + "openstreetmap" + File.separator + "osmosis"
                        + File.separator + "plugins"),
                new File(System.getenv("APPDATA") + File.separator + "openstreetmap" + File.separator + "libosm"
                        + File.separator + "plugins"),
                new File(".." + File.separator + "SpeechPack" + File.separator + "dist"),
                new File(".." + File.separator + "FreeTTS-SpeechPack" + File.separator + "dist")
        };

        FilenameFilter pluginFileNameFilter = new FilenameFilter() {

            /**
             * @param dir
             *            the directory of the file
             * @param name
             *            the unqualified name of the file
             * @return true if this may be a plugin-file
             */
            public boolean accept(final File dir, final String name) {
                return name.toLowerCase().endsWith(".zip") || name.toLowerCase().endsWith(".jar");
            }
        };
        List<PluginLocation> locations = new LinkedList<PluginLocation>();
        for (File pluginDir : pluginsDirs) {
            LOG.info("Loading plugins in " + pluginDir.getAbsolutePath());
            if (!pluginDir.exists()) {
                continue;
            }
            File[] plugins = pluginDir.listFiles(pluginFileNameFilter);
            try {
                if (plugins != null) {
                    for (int i = 0; i < plugins.length; i++) {
                        LOG.finest("Found plugin " + plugins[i].getAbsolutePath());
                        PluginLocation created = StandardPluginLocation.create(plugins[i]);
                        if (created != null) {
                            locations.add(created);
                        }
                    }
                }
            } catch (MalformedURLException e) {
                LOG.log(Level.SEVERE, "Cannot create plugin location " + pluginDir.getAbsolutePath(), e);
            }
        }
        return locations;
    }

    /**
     * Register the core plugin from which other plugins will extend.
     *
     * @param pluginManager
     *            The plugin manager to register the plugin with.
     */
    private static void registerCorePlugin(final PluginManager pluginManager) {
        try {
            URL core;
            PluginDescriptor coreDescriptor;

            // Get the plugin configuration file.
            core = getInstance().getClass().getResource("/org/openstreetmap/osm/Plugins/plugin.xml");
            LOG.finest("Plugin URL: " + core);

            // Register the core plugin in the plugin registry.
            pluginManager.getRegistry().register(new URL[] {core});

            // Get the plugin descriptor from the registry.
            coreDescriptor = pluginManager.getRegistry().getPluginDescriptor(
                    COREPLUGIN);

            // Enable the plugin.
            pluginManager.enablePlugin(coreDescriptor, true);
            pluginManager.activatePlugin(COREPLUGIN);

        } catch (ManifestProcessingException e) {
            LOG.log(Level.SEVERE, "Unable to register core plugin.", e);
        } catch (PluginLifecycleException e) {
            LOG.log(Level.SEVERE, "Unable to enable core plugin.", e);
        }
    }

    /**
     * Register the given JPF-plugins with the {@link PluginManager}.
     * @param locations
     *            the plugins found
     * @param pluginManager the pluginManager to register them with.
     */
    private static void registerJpfPlugins(final PluginManager pluginManager, final List<PluginLocation> locations) {
        if (locations == null) {
            throw new IllegalArgumentException("null plugin-list given");
        }

        try {
            pluginManager.publishPlugins(locations.toArray(new PluginLocation[locations.size()]));
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Unable to publish plugins.", e);
        }
    }

    /**
     * Load the plugin that is to be used to provide
     * the contract given by the interface.
     * @param <T> the interface the result must implement.
     * @param aPluginInterface the interface the result must implement.
     * @param aDefaultClassName the default-value
     * @param aExtensionPointName (optional) string that is attached to the settings-key
     * @param aArguments (optional)arguments to be passed to the contructor of the plugin
     * @return the plugin-instance or an exception.
     */
    @SuppressWarnings("unchecked")
    public <T extends IPlugin> T getPlugin(final Class<T> aPluginInterface,
            final String aDefaultClassName,
            final String aExtensionPointName,
            final Object[] aArguments) {

        String property = getPropertyNameForPlugin(aPluginInterface);
        if (aExtensionPointName != null && aExtensionPointName.trim().length() > 0) {
            property += "." + aExtensionPointName;
        }
        String className = get(property, aDefaultClassName);
        if (className == null || className.trim().length() == 0) {
            LOG.warning("No plugin configured for " + aPluginInterface.getName() + "." + aExtensionPointName);
            return null;
        }
        T o = getPlugin_internal(aPluginInterface, className, aArguments);
        if (o == null) {
            getPlugin_internal(aPluginInterface, className, aArguments);
        }
        if (o == null) {
            LOG.severe("Unable to load the configured or the default-plugin for " + aPluginInterface.getName());
            return null;
        }

        return o;
    }


}
