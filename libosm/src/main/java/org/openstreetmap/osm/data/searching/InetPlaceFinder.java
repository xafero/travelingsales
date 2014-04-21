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
package org.openstreetmap.osm.data.searching;

import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.osm.data.IDataSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * A placefinder using: <a hef="http://www.frankieandshadow.com/osm/search.xml?find=&lt;urlencodedstring&gt;">frankieandshadow.com/osm/search.xml</a>
 * described in: <a href="http://wiki.openstreetmap.org/index.php/Name_finder#Abbreviations.2C_Accented_characters_etc">wiki.openstreetmap.org/Name_finder</a>.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class InetPlaceFinder implements IExtendedPlaceFinder {

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(InetPlaceFinder.class.getName());

    /**
     * The map we are searching on.
     */
    private IDataSet myMap;

    /**
     * This plugin has no  settings, thus this method returns null
     * as described in {@link IPlugin#getSettings()}.
     * @return null
     */
    public ConfigurationSection getSettings() {
        return null;
    }

    /**
     * @param aSearchExpression a name or search-expression for the OSM-name-finder.
     * @return all places with that name
     * @see org.openstreetmap.osm.data.searching.IPlaceFinder#findPlaces(java.lang.String)
     */
    public Collection<Place> findPlaces(final String aSearchExpression) {


        LinkedList<Place> retval = new LinkedList<Place>();

        try {
            URL url = new URL("http://www.frankieandshadow.com/osm/search.xml?find=" + URLEncoder.encode(aSearchExpression, "UTF-8"));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(url.openStream()));

            NodeList wayElements = document.getDocumentElement().getElementsByTagName("named");
            for (int i = 0; i < wayElements.getLength(); i++) {
                try {
                    Element wayElement = (Element) wayElements.item(i);
                    String type = wayElement.getAttribute("type");
                    if (type == null || !type.equalsIgnoreCase("way"))
                        continue;

                    long id = Long.parseLong(wayElement.getAttribute("id"));
                    String name = wayElement.getAttribute(Tags.TAG_NAME);
                    double lat = Double.parseDouble(wayElement.getAttribute("lat"));
                    double lon = Double.parseDouble(wayElement.getAttribute("lon"));
                    //TODO: return the attribute:  zoom='16'  <!-- the suggested zoom level (for top-level names) when constructing a map url -->
                    retval.add(new WayReferencePlace(myMap, name, id, lat, lon));
                } catch (RuntimeException e) {
                    LOG.log(Level.SEVERE, "Exception while searching with the OpenStreetMap-Name_finder for '" + aSearchExpression + "' with result #" + i, e);
                }
            }
            return retval;

        } catch (UnknownHostException e) {
            LOG.log(Level.INFO, "We seem to have no internet while searching with the OpenStreetMap-Name_finder for '" + aSearchExpression + "'");
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Exception while searching with the OpenStreetMap-Name_finder for '" + aSearchExpression + "'", e);
        }

        return retval;
    }

    /**
     * @param aMap The map we are searching on.
     * @see org.openstreetmap.osm.data.searching.IPlaceFinder#setMap(org.openstreetmap.osm.data.IDataSet)
     */
    public void setMap(final IDataSet aMap) {
        if (aMap == null)
            throw new IllegalArgumentException("null map given!");
        this.myMap = aMap;
    }

    /**
     * We care only for streets and cities here.
     * ${@inheritDoc}.
     */
    public Collection<Place> findAddress(final String pHouseNr,
                                         final String pStreet, final String pCity,
                                         final String pZipCode, final String pCountry) {
        if (pStreet == null || pStreet.trim().length() == 0)
            return findPlaces(pCity);

        if (pCity == null || pCity.trim().length() == 0)
            return findPlaces(pStreet);

        return findPlaces(pStreet + " , " + pCity);
    }

}
