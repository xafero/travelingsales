//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.osm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.openstreetmap.osm.data.coordinates.Bounds;


/**
 * This class holds all preferences for JOSM.
 *
 * Other classes can register their beloved properties here. All properties will be
 * saved upon set-access.
 *
 * @author imi
 */
public class JosmPreferences {

    /**
     * A listener to be informed about updates to preferences.
     * @author imi
     */
    public static interface PreferenceChangedListener {
        /**
         * @param key the key of the values changed
         * @param newValue the new value
         */
        void preferenceChanged(final String key, final String newValue);
    }

    /**
     * Class holding one bookmarkentry.
     * @author imi
     */
    public static class Bookmark {

        /**
         * The name of a bookmark.
         */
        private String name;

        /**
         * The bounds of a bookmark.
         */
        private Bounds bounds = new Bounds();

        /**
         * ${@inheritDoc}.
         */
        @Override
        public String toString() {
            return name;
        }

        /**
         * @return Returns the name.
         * @see #name
         */
        public String getName() {
            return this.name;
        }

        /**
         * @param pName The name to set.
         * @see #name
         */
        public void setName(final String pName) {
            this.name = pName;
        }

        /**
         * @return Returns the bounds.
         * @see #bounds
         */
        public Bounds getBounds() {
            return this.bounds;
        }

        /**
         * @param pBounds The bounds to set.
         * @see #bounds
         */
        public void setBounds(final Bounds pBounds) {
            if (pBounds == null) {
                throw new IllegalArgumentException("null 'pBounds' given!");
            }
            this.bounds = pBounds;
        }

        /**
         * @param pName the name of the bookmarked place.
         * @param pBounds the bounds of the bookmarked place.
         */
        public Bookmark(final String pName, final Bounds pBounds) {
            super();
            this.name = pName;
            this.bounds = pBounds;
        }
    }

    /**
     * Our {@link PreferenceChangedListener}s.
     */
    //private final ArrayList<PreferenceChangedListener> listener = new ArrayList<PreferenceChangedListener>();

    /**
     * Map the property name to the property object.
     */
    private final SortedMap<String, String> properties = new TreeMap<String, String>();

    /**
     * Return the location of the user defined preferences file.
     * @return the directory where the josm-proferences are stored.
     */
    public String getPreferencesDir() {
        if (System.getenv("APPDATA") != null) {
            return System.getenv("APPDATA") + "/JOSM/";
        }
        return System.getProperty("user.home") + "/.josm/";
    }

    /**
     * @return A list of all existing directories, where ressources could be stored.
     */
    public Collection<String> getAllPossiblePreferenceDirs() {
        LinkedList<String> locations = new LinkedList<String>();
        locations.add(getPreferencesDir());
        String s = System.getenv("JOSM_RESOURCES");
        if (s != null) {
            if (!s.endsWith("/") && !s.endsWith("\\")) {
                s = s + "/";
            }
            locations.add(s);
        }
        s = System.getProperty("josm.resources");
        if (s != null) {
            if (!s.endsWith("/") && !s.endsWith("\\")) {
                s = s + "/";
            }
            locations.add(s);
        }
        String appdata = System.getenv("APPDATA");
        if (System.getenv("ALLUSERSPROFILE") != null && appdata != null && appdata.lastIndexOf("\\") != -1) {
            appdata = appdata.substring(appdata.lastIndexOf("\\"));
            locations.add(System.getenv("ALLUSERSPROFILE") + appdata + "/JOSM/");
        }
        locations.add("/usr/local/share/josm/");
        locations.add("/usr/local/lib/josm/");
        locations.add("/usr/share/josm/");
        locations.add("/usr/lib/josm/");
        return locations;
    }


    /**
     * @param key the key to test
     * @return true if we have a setting with that key.
     */
    public synchronized boolean hasKey(final String key) {
        return properties.containsKey(key);
    }

    /**
     * @param key the key to test
     * @return the setting with that key or the empty string
     */
    public synchronized String get(final String key) {
        if (!properties.containsKey(key))
            return "";
        return properties.get(key);
    }
    /**
     * @param key the key to test
     * @param def the default if we do not have a setting
     * @return the setting with that key or the given default-string
     */
    public synchronized String get(final String key, final String def) {
        final String prop = properties.get(key);
        if (prop == null || prop.equals(""))
            return def;
        return prop;
    }

    /**
     * @param prefix the prefix to look for
     * @return all settings beginning with that prefix
     */
    public synchronized Map<String, String> getAllPrefix(final String prefix) {
        final Map<String, String> all = new TreeMap<String, String>();

        for (final Entry<String, String> e : properties.entrySet())
            if (e.getKey().startsWith(prefix))
                all.put(e.getKey(), e.getValue());
        return all;
    }

    /**
     * @param key the key to test
     * @return the setting with that key or false
     */
    public synchronized boolean getBoolean(final String key) {
        return getBoolean(key, false);
    }

    /**
     * @param key the key to test
     * @param def the default if we do not have a setting
     * @return the setting with that key or the given default-value
     */
    public synchronized boolean getBoolean(final String key, final boolean def) {
        if (!properties.containsKey(key))
            return def;
        return Boolean.parseBoolean(properties.get(key));
    }

//    /**
//     * Inform our {@link PreferenceChangedListener}s.
//     * @param key the key that changed
//     * @param value the new value
//     */
//    private void firePreferenceChanged(final String key, final String value) {
//        for (final PreferenceChangedListener l : listener)
//            l.preferenceChanged(key, value);
//    }


    /**
     * Load all preferences from disk.
     * @throws IOException in case something goes wrong.
     */
    public void load() throws IOException {
        properties.clear();
        final BufferedReader in = new BufferedReader(new FileReader(getPreferencesDir() + "preferences"));
        try {
            int lineNumber = 0;
            for (String line = in.readLine(); line != null; line = in.readLine(), lineNumber++) {
                final int i = line.indexOf('=');
                if (i == -1 || i == 0)
                    throw new IOException("Malformed config file at line " + lineNumber);
                properties.put(line.substring(0, i), line.substring(i + 1));
            }
        } finally {
            in.close();
        }

    }

    /**
     * Set the default-preferences.
     */
    public final void resetToDefault() {
        properties.clear();
        properties.put("laf", "javax.swing.plaf.metal.MetalLookAndFeel");
        properties.put("projection", "org.openstreetmap.josm.data.projection.Epsg4326");
        properties.put("propertiesdialog.visible", "true");
        properties.put("osm-server.url", "http://www.openstreetmap.org/api");
    }

    /**
     * Load the bookmarks from disk.
     * @return the bookmarks
     * @throws IOException in case anything happens.
     */
    public Collection<Bookmark> loadBookmarks() throws IOException {
        File bookmarkFile = new File(getPreferencesDir() + "bookmarks");
        if (!bookmarkFile.exists())
            bookmarkFile.createNewFile();
        BufferedReader in = new BufferedReader(new FileReader(bookmarkFile));

        Collection<Bookmark> bookmarks = new LinkedList<Bookmark>();
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            StringTokenizer st = new StringTokenizer(line, ",");

            try {
            Bookmark b = new Bookmark(st.nextToken(),
                    new Bounds(
                            Double.parseDouble(st.nextToken()),
                            Double.parseDouble(st.nextToken()),
                            Double.parseDouble(st.nextToken()),
                            Double.parseDouble(st.nextToken())));
            bookmarks.add(b);
            } catch (NumberFormatException x) {
                // line not parsed
                x.printStackTrace();
            }

        }
        in.close();
        return bookmarks;
    }

    /**
     * Save the given bookmarks to a file.
     * @param bookmarks the bookmarks to save.
     * @throws IOException in case anything happens.
     */
    public void saveBookmarks(final Collection<Bookmark> bookmarks) throws IOException {
        File bookmarkFile = new File(getPreferencesDir() + "bookmarks");
        if (!bookmarkFile.exists())
            bookmarkFile.createNewFile();

        PrintWriter out = new PrintWriter(new FileWriter(bookmarkFile));
        for (Bookmark b : bookmarks) {
            String name = b.getName().replace(',', '_');
            out.print(name + ",");
            Bounds bounds = b.getBounds();
            out.print(bounds.getMin().getXCoord() +  ":");
            out.print(bounds.getMin().getYCoord() +  ":");
            out.print(bounds.getMax().getXCoord() +  ":");
            out.print(bounds.getMax().getYCoord());
            out.println();
        }
        out.close();
    }
}
