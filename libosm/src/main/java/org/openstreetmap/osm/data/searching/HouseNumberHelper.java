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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

//automatically created propertyChangeListener-Support
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.NodeHelper;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedNode;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedWay;

import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * HouseNumberFinderTest.java<br/>
 * created: 19.04.2008 13:59:40 <br/>
 *<br/><br/>
 * <b>Command-Line-Program to test calculating the geolocation of house-numbers.</b>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class HouseNumberHelper {

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(
             HouseNumberHelper.class.getName());

    /**
     * About 10-20m .
     */
    private static final double BOUNDS_SEARCH_OFFSET = 0.002d;

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

    /**
     * @param node the node to get the interpolation-ways for
     * @param aMap the map we operate on
     * @param aStreetName the name of the street to look for
     * @param aStreet may be null. WayIDs of the ways that make up the street corresponding to aStreetName
     * @return all ways that are interpolation-ways for this node and this street
     */
    public static Iterator<Way> getInterpolationWays(final IDataSet aMap,
                      final Node node,
                      final String aStreetName,
                      final Collection<Long> aStreet) {
        String streetName = NameHelper.normalizeName(aStreetName);
        Iterator<Way> ways = aMap.getWaysForNode(node.getId());
        Collection<Way> retval = new LinkedList<Way>();
        while (ways.hasNext()) {
            Way next = ways.next();

            // is this an interpolation-way at all?
            String interpolationStrategy = WayHelper.getTag(next, Tags.TAG_ADDR_INTERPOLATON);
            if (interpolationStrategy == null) {
                continue;
            }

         // is the nearest to this way is ours?
            String tag = WayHelper.getTag(next, Tags.TAG_ADDR_STREET);
            if (tag == null) {
                // street-name is not given, first try relation and
                if (next instanceof ExtendedWay) {
                    ExtendedWay n = (ExtendedWay) next;
                    Set<Long> referencedRelationIDs = n.getReferencedRelationIDs();
                    tag = checkAssociatedStreet(aMap, aStreetName, aStreet,
                            referencedRelationIDs);
                }
            }
            if (tag == null && next.getWayNodes().size() > 0) {
                // then fall back to nearest street (street = way with higway-tag)

                Node nodeByID = aMap.getNodeByID(next.getWayNodes().get(0).getNodeId());
                if (nodeByID != null) {
                    Way w = getNearestStreet(nodeByID, aMap);
                    if (w != null) {
                        tag = WayHelper.getTag(w, Tags.TAG_NAME);
                    }
                }
            }

            if (tag != null) {
                // the node contains a street-name as a hint, see if it matches
               if (!NameHelper.normalizeName(tag).equals(streetName)) {
                   if (LOG.isLoggable(Level.FINE)) {
                       LOG.log(Level.FINE, "Ignoring interpolation-way belonging to street '" + tag + "'='" + NameHelper.normalizeName(tag)
                               + "' instead of '" + aStreetName + "'='" + aStreetName + "'");
                   }
                   continue;
               }
            }

            retval.add(next);
        }
        return retval.iterator();
    }

    /**
     * Given a start-node, find the next next along a given
     * way that also has a numerical house-number and
     * interpolate a location.
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
    public static LatLon findHouseNrInInterpolation(
            final IDataSet map,
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

                        //TODO: see if there is a relation of type "roadAccess" and use it

                        float interpolation = (givenHouseNr - startHouseNr) / (((float) (endHouseNr - startHouseNr)));
                        LatLon interpolatedHouseLocation =
                               new LatLon(startNode.getLatitude() + interpolation * (nextNode.getLatitude() - startNode.getLatitude()),
                                          startNode.getLongitude() + interpolation * (nextNode.getLongitude() - startNode.getLongitude())
                                         );
                        /*System.err.println("found \"" + args[1] + "\" number \"" + args[2] + "\" in way between node "
                                + startNode.getId() + " (housenumber=" + startHouseNr + ") at " + startNode.getLatitude() + "/" + startNode.getLongitude() + " and "
                                + nextNode.getId() + " (housenumber=" + endHouseNr + ") at " + nextNode.getLatitude() + "/" + nextNode.getLongitude()
                                + " interpolated location of house is: " + interpolatedHouseLocation);*/
                        outputOpenLayers(interpolatedHouseLocation);
                        return interpolatedHouseLocation;
                    }
                } catch (NumberFormatException x) {
                    //ignore non-numerical house-numbers
                    LOG.finest("ignoring non-numeric housenumber");
                }
            }
        }
        return null;
    }

    /**
     * Get all nodes with housenumbers relating to the given way.
     * @param map the map we are working on
     * @param streetBounds the bounds around the way to look in
     * @param aStreetName the name of the street in question
     * @param aStreet may be null. WayIDs of the ways that make up the street corresponding to aStreetName
     * @return all nodes with housenumbers relating to the given way
     */
    public static List<Node> getHouseNumberedNodes(final IDataSet map,
                                                  final Bounds streetBounds,
                                                  final String aStreetName,
                                                  final Collection<Long> aStreet) {
        String streetName = NameHelper.normalizeName(aStreetName);
        List<Node> houseNumberNodes = new LinkedList<Node>();
        // find all house-number-nodes for our streets
        Bounds boundingBox = new Bounds(
                streetBounds.getMin().lat() - BOUNDS_SEARCH_OFFSET,
                streetBounds.getMin().lon() - BOUNDS_SEARCH_OFFSET,
                streetBounds.getMax().lat() + BOUNDS_SEARCH_OFFSET,
                streetBounds.getMax().lon() + BOUNDS_SEARCH_OFFSET
                );
        LOG.log(Level.INFO, "Searching bounding.box for nodes with addr:housenumber: " + boundingBox);
        Iterator<Node> nodes =
        map.getNodes(boundingBox);
        int examined = 0;
        while (nodes.hasNext()) {
            Node node = nodes.next();
            examined++;
//                  only use house-number -nodes.
            String houseNR = NodeHelper.getTag(node, "addr:housenumber");
            if (houseNR == null) {
                houseNR = NodeHelper.getTag(node, "addr:housename");
                if (houseNR == null) {
                    continue;
                }
            }

            // is the nearest to this way is ours?
            String tag = NodeHelper.getTag(node, "addr:street");
            if (tag == null) {
                // street-name is not given, first try relation and
                if (node instanceof ExtendedNode) {
                    ExtendedNode n = (ExtendedNode) node;
                    Set<Long> referencedRelationIDs = n.getReferencedRelationIDs();
                    tag = checkAssociatedStreet(map, aStreetName, aStreet,
                            referencedRelationIDs);
                }
            }
            if (tag == null) {
                // then fall back to nearest street (street = way with highway-tag)

                Way w = getNearestStreet(node, map);
                if (w != null) {
                    tag = WayHelper.getTag(w, Tags.TAG_NAME);
                }
            }

            if (tag != null) {
                // the node contains a street-name as a hint, see if it matches
               if (!NameHelper.normalizeName(tag).equals(streetName)) {
                   if (LOG.isLoggable(Level.FINE)) {
                       LOG.log(Level.FINE, "Ignoring node belonging to street '" + tag + "'='" + NameHelper.normalizeName(tag)
                               + "' instead of '" + aStreetName + "'='" + streetName + "'");
                   }
                   continue;
               }
            }
            houseNumberNodes.add(node);
        }
        LOG.log(Level.INFO, "Searching bounding.box for nodes with addr:housenumber: " + boundingBox
                + " examined " + examined + " nodes, matching=" + houseNumberNodes.size());
        return houseNumberNodes;
    }

    /**
     * Check if an entity is in an type=associatedStreet -relation
     * with the street we are looking for.
     * That street can be given by name or by name and wayIDs.
     * The second method is prefered.
     * @param map the map we operate on
     * @param aStreetName the name of the street to look for
     * @param aStreet may be null. WayIDs of the ways that make up the street corresponding to aStreetName
     * @param referencedRelationIDs the relations the house is a member of
     * @return the name of the associated street. or null.
     */
    private static String checkAssociatedStreet(final IDataSet map,
            final String aStreetName, final Collection<Long> aStreet,
            final Set<Long> referencedRelationIDs) {
        try {
            for (Long relID : referencedRelationIDs) {
                Relation rel = map.getRelationByID(relID);
                if (rel == null) {
                    continue;
                }
                String type = WayHelper.getTag(rel, Tags.TAG_TYPE);
                if (type == null || !type.equalsIgnoreCase("associatedStreet")) {
                    continue;
                }
                if (aStreet != null) {
                    //we have an explicit list of wayIDs, check if any
                    //of them is a member of the correct role
                    List<RelationMember> members = rel.getMembers();
                    for (RelationMember relationMember : members) {
                        if (!relationMember.getMemberType().equals(EntityType.Way)) {
                            continue;
                        }
                        if (relationMember.getMemberRole() != null && !relationMember.getMemberRole().equalsIgnoreCase("street")) {
                            continue;
                        }
                        if (aStreet.contains(new Long(relationMember.getMemberId()))) {
                            return aStreetName;
                        }
                    }
                } else {
                    // no explicit list of wayIDs is given,
                    // we have to improvise with the names
                    String name = WayHelper.getTag(rel, Tags.TAG_NAME);
                    if (name != null) {
                        return name;
                    } else {
                        List<RelationMember> members = rel.getMembers();
                        for (RelationMember relationMember : members) {
                            if (!relationMember.getMemberType().equals(EntityType.Way)) {
                                continue;
                            }
                            if (!relationMember.getMemberRole().equalsIgnoreCase("street")) {
                                continue;
                            }
                            Way way = map.getWaysByID(relationMember.getMemberId());
                            if (way == null) {
                                continue;
                            }
                            String wayName = WayHelper.getTag(way, Tags.TAG_NAME);
                            if (wayName == null) {
                                continue;
                            }
                            return wayName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "[Exception] Problem in checkAssociatedStreet()",
                         e);
        }
        return null;
    }

    /**
     * Find the street, nearest to a node.
     * @param aNode ther node
     * @param aMap the map to work on
     * @return the nearest street or null
     */
    private static Way getNearestStreet(final Node aNode, final IDataSet aMap) {
        Node nearestNode = aMap.getNearestNode(new LatLon(aNode.getLatitude(), aNode.getLongitude()), new NearestStreetSelector());
        Iterator<Way> ways = aMap.getWaysForNode(nearestNode.getId());
        while (ways.hasNext()) {
            Way way = ways.next();
            if (WayHelper.getTag(way, Tags.TAG_HIGHWAY) != null
                && WayHelper.getTag(way, Tags.TAG_NAME) != null) {
                return way;
            }
        }
        return null;
    }

    /**
     * Get all ways with housenumbers relating to the given way.
     * @param map the map we are working on
     * @param streetBounds the bounds around the way to look in
     * @param aStreetName the name of the street in question
     * @param aStreet may be null. WayIDs of the ways that make up the street corresponding to aStreetName
     * @return all nodes with housenumbers relating to the given way
     */
    public static List<Way> getHouseNumberedPolygons(final IDataSet map,
                                                  final Bounds streetBounds,
                                                  final String aStreetName,
                                                  final Collection<Long> aStreet) {
        String streetName = NameHelper.normalizeName(aStreetName);
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
                if (houseNR == null) {
                    continue;
                }
            }

            // is the nearest to this way is ours?
            String tag = WayHelper.getTag(area, Tags.TAG_ADDR_STREET);
            if (tag == null) {
                // street-name is not given, first try relation and
                if (area instanceof ExtendedWay) {
                    ExtendedWay n = (ExtendedWay) area;
                    Set<Long> referencedRelationIDs = n.getReferencedRelationIDs();
                    tag = checkAssociatedStreet(map, aStreetName, aStreet,
                            referencedRelationIDs);
                }
            }
            if (tag == null && area.getWayNodes().size() > 0) {
                // then fall back to nearest street (street = way with higway-tag)

                Node nodeByID = map.getNodeByID(area.getWayNodes().get(0).getNodeId());
                if (nodeByID != null) {
                    Way w = getNearestStreet(nodeByID, map);
                    if (w != null) {
                        tag = WayHelper.getTag(w, Tags.TAG_NAME);
                    }
                }
            }

            if (tag != null) {
                // the node contains a street-name as a hint, see if it matches
               if (!NameHelper.normalizeName(tag).equals(streetName)) {
                   if (LOG.isLoggable(Level.FINE)) {
                       LOG.log(Level.FINE, "Ignoring polygon belonging to street '" + tag + "'='" + NameHelper.normalizeName(tag)
                               + "' instead of '" + aStreetName + "'='" + streetName + "'");
                   }
                   continue;
               }
            }
            houseNumberNodes.add(area);
        }
        return houseNumberNodes;
    }

    //------------------------ support for propertyChangeListeners ------------------

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


