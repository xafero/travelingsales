/**
 * HouseNumberFinderTest.java
 * created: 19.04.2008 13:59:40
 * (c) 2008 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of libosm by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  libosm is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  libosm is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with libosm.  If not, see <http://www.gnu.org/licenses/>.
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

//other imports


//automatically created logger for debug and error -output
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

//automatically created propertyChangeListener-Support
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.NodeHelper;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.io.FileLoader;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * HouseNumberFinderTest.java<br/>
 * created: 19.04.2008 13:59:40 <br/>
 *<br/><br/>
 * <b>Command-Line-Program to test calculating the geolocation of house-numbers.</b>
 * This code is no longer used but is left as an example of how to generate output
 * for OpenLayers.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class HouseNumberFinderTest {

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(
             HouseNumberFinderTest.class.getName());

    /**
     * About 10-20m .
     */
    private static final double BOUNDS_SEARCH_OFFSET = 0.02d;

    /**
     * Output a location as an OpenLayours-point.
     * @param location the location to output to stdout
     */
    public static void outputOpenLayers(final LatLon location) {
        System.out.println("Content-type: application/json\n\n");
        System.out.println(
                "{\"type\":\"Feature\", "
                + "\"id\":\"OpenLayers.Feature.Vector_109\", "
                + "\"properties\":{}, \"geometry\":{"
                   + "\"type\":\"Point\", "
                   + "\"coordinates\":[" + location.lon() + ", " + location.lat() + "]"
                + "}, "
                + "\"crs\":{\"type\":\"OGC\", "
                + "\"properties\":{\"urn\":\"urn:ogc:def:crs:OGC:1.3:CRS84\"}}}");
    }

    /**
     * Output a location as an OpenLayours-point.
     * @param area the location to output to stdout
     * @param map the map we are working with
     */
    public static void outputOpenLayers(final Way area, final MemoryDataSet map) {
        System.out.println("Content-type: application/json\n\n");
        System.out.println(
                "{\"type\":\"Feature\", "
                + "\"id\":\"OpenLayers.Feature.Vector_109\", "
                + "\"properties\":{}, \"geometry\":{"
                   + "\"type\":\"Polygon\", "
                   + "\"coordinates\":[[");

        List<Node> nodes = map.getWayHelper().getNodes(area);
        boolean hasLast = false;
        for (Node node : nodes) {
            if (hasLast) {
                System.out.print(",");
            }
            System.out.println("[" + node.getLongitude() + ", " + node.getLatitude() + "]");
            hasLast = true;
        }

        System.out.println(
                   "]]"
                + "}, "
                + "\"crs\":{\"type\":\"OGC\", "
                + "\"properties\":{\"urn\":\"urn:ogc:def:crs:OGC:1.3:CRS84\"}}}");
    }

    /**
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        //debug
        //String[] args = new String[] {"test", "GET", "?street=Baumeisterstraße&houseno=38"};
        //String[] args = new String[] {"/home/fox/hno.osm", "Baumeisterstraße", "11"};
        //String[] args = new String[] {"/home/fox/hno.osm", "Baumeisterstraße", "40"};
        if (args.length != (2 + 1)) {
            System.out.println("Usage: HouseNumberFinderTest "
                    + "<mapfile>.osm <streetname> <housenumber>");
            return;
        }

        // support non-standalone operation as a CGI-script.
        if (args[1].equalsIgnoreCase("GET")) {
            try {
                    //decode CGI
                    args[0] = "/tmp/hno.osm";
                    String queryString = args[2];
                    StringTokenizer st = new StringTokenizer(queryString, "&?");
                    while (st.hasMoreTokens()) {
                        String nameValue = st.nextToken();
                        int index = nameValue.indexOf("=");
                        if (index > 0) {
                            String name = nameValue.substring(0, index);
                            String value = nameValue.substring(index + 1);
                            if (name.equalsIgnoreCase("street")) {
                                args[1] = URLDecoder.decode(value, "UTF-8");
                            }
                            if (name.equalsIgnoreCase("houseno")) {
                                args[2] = URLDecoder.decode(value, "UTF-8");
                            }
                        }
                    }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                LOG.log(Level.SEVERE, "[UnsupportedEncodingException]",
                             e);
            }
        }

        File mapfile = new File(args[0]);
        if (!mapfile.exists()) {
            System.err.println("File " + mapfile.getAbsolutePath()
                    + " does not exist");
            return;
        }
        try {
            FileLoader loader = new FileLoader(mapfile);
            MemoryDataSet map = loader.parseOsm();

            prepareMap(map);

            // find all matching ways (name must be exact match)
            Iterator<Way> ways = map.getWaysByName(args[1], null);

            while (ways.hasNext()) {
                Way street = ways.next();
                LOG.log(Level.FINE, "Examining way " + street);

                Bounds streetBounds = map.getWayHelper().getWayBounds(street);
                String streetName = WayHelper.getTag(street, Tags.TAG_NAME);
                if (streetName == null) {
                    streetName = "";
                }
                streetName = NameHelper.normalizeName(streetName);

                List<Node> houseNumberNodes = getHouseNumberNodes(map, streetBounds, streetName);
                /*LOG.log(Level.FINE, "Examining way " + street
                        + " - found " + houseNumberNodes.size()
                        +  "houseNumber-nodes");*/

                // find house-number as single node

                for (Node node : houseNumberNodes) {
                    String houseNR = NodeHelper.getTag(node, "addr:housenumber");
                    if (houseNR.equalsIgnoreCase(args[2])) {
                        /*System.err.println("found \"" + args[1] + "\" number \"" + args[2] + "\" in node "
                                + node.getId() + " at " + node.getLatitude() + "/" + node.getLongitude());*/
                        outputOpenLayers(new LatLon(node.getLatitude(), node.getLongitude()));
                        //(was a todo): output nearest location on way. instead
                        return;
                    }
                }

                List<Way> houseNumberAreas = getHouseNumberAreas(map, streetBounds, streetName);
                /*LOG.log(Level.FINE, "Examining way " + street
                        + " - found " + houseNumberNodes.size()
                        +  "houseNumber-nodes");*/

                // find house-number as single node

                for (Way area : houseNumberAreas) {
                    String houseNR = WayHelper.getTag(area, Tags.TAG_ADDR_HOUSENUMBER);
                    if (houseNR.equalsIgnoreCase(args[2])) {
                        /*System.err.println("found \"" + args[1] + "\" number \"" + args[2] + "\" in node "
                                + node.getId() + " at " + node.getLatitude() + "/" + node.getLongitude());*/
                        outputOpenLayers(area, map);
                        //(was a todo): output nearest location on way. instead
                        return;
                    }
                    houseNR = WayHelper.getTag(area, Tags.TAG_ADDR_HOUSENAME);
                    if (houseNR.equalsIgnoreCase(args[2])) {
                        /*System.err.println("found \"" + args[1] + "\" number \"" + args[2] + "\" in node "
                                + node.getId() + " at " + node.getLatitude() + "/" + node.getLongitude());*/
                        outputOpenLayers(area, map);
                        //(was a todo): output nearest location on way. instead
                        return;
                    }
                }

                // find house-number in interpolated ones
                try {
                    int givenHouseNr = Integer.parseInt(args[2]);

                    // find the house-number before and after ours
                    for (Node node : houseNumberNodes) {
                        String houseNR = NodeHelper.getTag(node, "addr:housenumber");
                        try {
                            int startHouseNr = Integer.parseInt(houseNR);
                            if (startHouseNr > givenHouseNr) {
                                // start-number is past ours
                                continue;
                            }
                            Iterator<Way> interpolationWays =  map.getWaysForNode(node.getId());
                            while (interpolationWays.hasNext()) {
                                Way interpolationWay = interpolationWays.next();
                                if (findHouseNrInInterpolation(args, map, givenHouseNr, node, startHouseNr, interpolationWay) != null) {
                                    return;
                                }
                            }
                        } catch (NumberFormatException x) {
                            //ignore non-numerical house-numbers
                            x.printStackTrace();
                        }
                    }
                } catch (NumberFormatException x) {
                    //ignore non-numerical house-numbers
                    x.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            LOG.log(Level.SEVERE, "[RuntimeException] Problem in "
                       + "new File(args[0])", e);
        }
    }

    /**
     * Given a start-node, find the next next along a given
     * way that also has a numerical house-number and
     * interpolate a location.
     * @param args used for debug-output only
     * @param map the map we are working on
     * @param givenHouseNr search for this housenumber
     * @param startNode the node for startHouseNr2
     * @param startHouseNr2 the house-number of the start-node
     * @param interpolationWay the way we are looking for.
     *        This is not a street (no highway-tag) gut a way connecting nodes
     *        with housenumbers.
     * @return null if nthe given givenHouseNr is not betwee startHouseNr2
     *         and the next such node on the given way.
     */
    private static LatLon findHouseNrInInterpolation(final String[] args,
            final MemoryDataSet map,
            final int givenHouseNr,
            final Node startNode,
            final int startHouseNr2,
            final Way interpolationWay) {

        String interpolationStrategy = WayHelper.getTag(interpolationWay, Tags.TAG_ADDR_INTERPOLATON);
        if (interpolationStrategy == null) {
            return null;
        }
        int startHouseNr = startHouseNr2;
//        if (interpolationStrategy.equalsIgnoreCase("all")) {
//            step = 1;
//        }
        if (interpolationStrategy.equalsIgnoreCase("even")) {
            // if startHouseNr is not even, increment by 1
            if (startHouseNr % 2 == 1) {
                startHouseNr++;
            }
            // is the number we are looking for even?
            if (givenHouseNr % 2 == 1) {
                return null;
            }
        }
        if (interpolationStrategy.equalsIgnoreCase("odd")) {
            //: if startHouseNr is not odd, increment by 1
            if (startHouseNr % 2 == 0) {
                startHouseNr++;
            }
//          is the number we are looking for odd?
            if (givenHouseNr % 2 == 0) {
                return null;
            }
        }


        List<Node> nextNodes = map.getWayHelper().getNodes(interpolationWay);
        boolean found = false;
        for (Node nextNode : nextNodes) {
            // search for our node first
            if (nextNode.getId() == startNode.getId()) {
                found = true;
                continue;
            }
            // this node is after ours
            if (found) {
                try {
                    String endHouseNrStr = NodeHelper.getTag(nextNode, "addr:housenumber");
                    if (endHouseNrStr != null) {
                        int endHouseNr = Integer.parseInt(endHouseNrStr);
                        if (endHouseNr < givenHouseNr) {
                            // next node is past our ones
                            break;
                        }
                        if (interpolationStrategy.equalsIgnoreCase("even")) {
                            // if endHouseNr is not even, decrement by 1
                            if (endHouseNr % 2 == 1) {
                                endHouseNr--;
                            }
                        }
                        if (interpolationStrategy.equalsIgnoreCase("odd")) {
                            //: if endHouseNr is not odd, decrement by 1
                            if (endHouseNr % 2 == 0) {
                                endHouseNr--;
                            }
                        }
                        //note: end may be < start if the ndes are not in ascending order of housenumbers

                        //(was a todo): see if there is a relation of type "roadAccess" and use it

                        float interpolation = (givenHouseNr - startHouseNr) / (((float) (endHouseNr - startHouseNr)));
                        LatLon interpolatedHouseLocation =
                               new LatLon(startNode.getLatitude() + interpolation * (nextNode.getLatitude() - startNode.getLatitude()),
                                          startNode.getLongitude() + interpolation * (nextNode.getLongitude() - startNode.getLongitude())
                                         );
                        //(was a todo) return projection of interpolated LatLon to street
                        /*System.err.println("found \"" + args[1] + "\" number \"" + args[2] + "\" in way between node "
                                + startNode.getId() + " (housenumber=" + startHouseNr + ") at " + startNode.getLatitude() + "/" + startNode.getLongitude() + " and "
                                + nextNode.getId() + " (housenumber=" + endHouseNr + ") at " + nextNode.getLatitude() + "/" + nextNode.getLongitude()
                                + " interpolated location of house is: " + interpolatedHouseLocation);*/
                        outputOpenLayers(interpolatedHouseLocation);
                        return interpolatedHouseLocation;
                    }
                } catch (NumberFormatException x) {
                    //ignore non-numerical house-numbers
                    x.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Get all nodes with housenumbers relating to the given way.
     * @param map the map we are working on
     * @param streetBounds the bounds around the way to look in
     * @param streetName the name of the street in question
     * @return all nodes with housenumbers relating to the given way
     */
    private static List<Node> getHouseNumberNodes(final MemoryDataSet map,
                                                  final Bounds streetBounds,
                                                  final String streetName) {
        List<Node> houseNumberNodes = new LinkedList<Node>();
        // find all house-number-nodes for our streets
        Iterator<Node> nodes =
        map.getNodes(new Bounds(
                streetBounds.getMin().lat() - BOUNDS_SEARCH_OFFSET,
                streetBounds.getMin().lon() - BOUNDS_SEARCH_OFFSET,
                streetBounds.getMax().lat() + BOUNDS_SEARCH_OFFSET,
                streetBounds.getMax().lon() + BOUNDS_SEARCH_OFFSET
                ));
        while (nodes.hasNext()) {
            Node node = nodes.next();

//                  only use house-number -nodes.
            String houseNR = NodeHelper.getTag(node, "addr:housenumber");
            if (houseNR == null) {
                continue;
            }

            // is the nearest to this way is ours?
            String tag = NodeHelper.getTag(node, "addr:street");
            if (tag == null) {
                //(was a todo): street-name is not given, try relation and fall back to nearest street (street = way with higway-tag)
                continue;
            } else {
                // the node contains a street-name as a hint, see if it matches
               if (!NameHelper.normalizeName(tag).equals(streetName)) {
                   continue;
               }
            }
            houseNumberNodes.add(node);
        }
        return houseNumberNodes;
    }

    /**
     * Get all ways with housenumbers relating to the given way.
     * @param map the map we are working on
     * @param streetBounds the bounds around the way to look in
     * @param streetName the name of the street in question
     * @return all nodes with housenumbers relating to the given way
     */
    private static List<Way> getHouseNumberAreas(final MemoryDataSet map,
                                                  final Bounds streetBounds,
                                                  final String streetName) {
        List<Way> houseNumberNodes = new LinkedList<Way>();
        // find all house-number-nodes for our streets
        Iterator<Way> areas =
        map.getWays(new Bounds(
                streetBounds.getMin().lat() - 2 * BOUNDS_SEARCH_OFFSET,
                streetBounds.getMin().lon() - 2 * BOUNDS_SEARCH_OFFSET,
                streetBounds.getMax().lat() + 2 * BOUNDS_SEARCH_OFFSET,
                streetBounds.getMax().lon() + 2 * BOUNDS_SEARCH_OFFSET
                ));
        while (areas.hasNext()) {
            Way area = areas.next();

//                  only use house-number -nodes.
            String houseNR = WayHelper.getTag(area, Tags.TAG_ADDR_HOUSENUMBER);
            if (houseNR == null) {
                houseNR = WayHelper.getTag(area, Tags.TAG_ADDR_HOUSENAME);
            }
            if (houseNR == null) {
                continue;
            }

            // is the nearest to this way is ours?
            String tag = WayHelper.getTag(area, Tags.TAG_ADDR_STREET);
            if (tag == null) {
                //(was a todo): street-name is not given, try relation and fall back to nearest street (street = way with higway-tag)
                continue;
            } else {
                // the node contains a street-name as a hint, see if it matches
               if (!NameHelper.normalizeName(tag).equals(streetName)) {
                   continue;
               }
            }
            houseNumberNodes.add(area);
        }
        return houseNumberNodes;
    }

    //------------------------ support for propertyChangeListeners ------------------

    /**
     * Add relations for all house-numbers to their ways.
     * @param aMap our map
     */
    private static void prepareMap(final MemoryDataSet aMap) {
        // (was todo) for all housenumber-nodes that are not in a "associatedStreet"-relation, add such a relation
    }

    /**
     * support for firing PropertyChangeEvents.
     * (gets initialized only if we really have listeners)
     */
    private volatile PropertyChangeSupport myPropertyChange = null;

    /**
     * Returned value may be null if we never had listeners.
     * @return Our support for firing PropertyChangeEvents
     */
    protected PropertyChangeSupport getPropertyChangeSupport() {
        return myPropertyChange;
    }

    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     *
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(
            final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(listener);
    }

    /**
     * Add a PropertyChangeListener for a specific property.  The listener
     * will be invoked only when a call on firePropertyChange names that
     * specific property.
     *
     * @param propertyName  The name of the property to listen on.
     * @param listener  The PropertyChangeListener to be added
     */
    public final void addPropertyChangeListener(final String propertyName,
            final PropertyChangeListener listener) {
        if (myPropertyChange == null) {
            myPropertyChange = new PropertyChangeSupport(this);
        }
        myPropertyChange.addPropertyChangeListener(propertyName, listener);
    }

    /**
     * Remove a PropertyChangeListener for a specific property.
     *
     * @param propertyName  The name of the property that was listened on.
     * @param listener  The PropertyChangeListener to be removed
     */
    public final void removePropertyChangeListener(final String propertyName,
            final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(propertyName,
                    listener);
        }
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a PropertyChangeListener that was registered
     * for all properties.
     *
     * @param listener  The PropertyChangeListener to be removed
     */
    public synchronized void removePropertyChangeListener(
            final PropertyChangeListener listener) {
        if (myPropertyChange != null) {
            myPropertyChange.removePropertyChangeListener(listener);
        }
    }

    //-------------------------------------------------------

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "HouseNumberFinderTest@" + hashCode();
    }


}
