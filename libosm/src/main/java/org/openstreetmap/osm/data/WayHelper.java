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
package org.openstreetmap.osm.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.Coordinate;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedWay;
import org.openstreetmap.osm.data.searching.CityBounds;
import org.openstreetmap.osm.data.visitors.BoundingXYVisitor;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
//import org.openstreetmap.osmosis.core.domain.v0_6.WayBuilder;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * This class stores all the functionality that we want for ways
 * but that is not included in the osmosis-way-class.
 * The classes org.openstreetmap.osm.data.Node/Segment/Way will
 * disapear with the upcoming OSM-0.5 -API. (That will also no
 * longer have segments at all.)
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class WayHelper {

    /**
     * The default-value for the "simplify-way.max-error"
     * -setting. Unit is meters.
     */
    private static final int DEFAULTSIMPLIFYTHRESHOLD = 50;
    /**
     * The default for the minimum distance between 2 nodes
     * in a way.
     */
    private static final double DEFAULTSIMPLIFYNODEDISTANCE = 0.001;

    /**
     * my logger for debug and error-output.
     */
    static final Logger LOG = Logger.getLogger(WayHelper.class.getName());

    /**
     * The map we are working with to
     * resolve ids, ....
     */
    private IDataSet myMap;

    /**
     * @param aMap The map we are working with to resolve ids
     */
    public WayHelper(final IDataSet aMap) {
        super();
        myMap = aMap;
    }

    /**
     * @return the map
     */
    public IDataSet getMap() {
        return myMap;
    }

//----------------------------------------------

//    /**
//     * @param aWay the way to get the nodes for
//     * @return all nodes for the way. (for OSM-0.5 ordered, for OSM-0.4 unordered)
//     */
//    public static Iterator<Node> getNodes(final Way aWay) {
//
//        HashSet<Node> nodesAlreadyReturned = new HashSet<Node>();
//
//        for (Iterator<Segment> segments = aWay.getSegments(); segments.hasNext();) {
//            Segment seg = segments.next();
//            Node n = seg.getFromNode();
//            if (n != null)
//                nodesAlreadyReturned.add(n);
//                 n = seg.getToNode();
//            if (n != null)
//                nodesAlreadyReturned.add(n);
//        }
//        return nodesAlreadyReturned.iterator();
//    }
//

    /**
     * @return the tag-value for the given key
     * @param way the way to get the tag from
     * @param key the key of the tag
     */
    public static String getTag(final Collection<Tag> way, final String key) {
        for (Tag tag : way) {
            try {
                if (tag.getKey().equals(key))
                    return tag.getValue();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Could not read tag. Skipping it.", e);
            }
        }
        return null;
    }

    /**
     * @return the tag-value for the given key
     * @param way the way to get the tag from
     * @param key the key of the tag
     */
    public static String getTag(final Entity way, final String key) {
        for (Tag tag : way.getTags()) {
            try {
                if (tag.getKey().equals(key))
                    return tag.getValue();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Could not read tag. Skipping it.", e);
            }
        }
        return null;
    }

    //----------------------------------- get Segments and Nodes


    /**
     * @param aWay the way to get the nodes for
     * @return all nodes for the way. (for OSM-0.5 ordered, for OSM-0.4 unordered)
     */
    public List<Node> getNodes(final Way aWay) {
        return getNodes(aWay.getWayNodes());
    }

    /**
     * @param aWay the way to get the nodes for
     * @return all nodes for the way. (for OSM-0.5 ordered, for OSM-0.4 unordered)
     */
    public List<Node> getNodes(final List<WayNode> aWay) {

        ArrayList<Node> retval = new ArrayList<Node>(aWay.size());
        for (WayNode nodeID : aWay) {
            Node nodeByID = getMap().getNodeByID(nodeID.getNodeId());
            if (nodeByID != null) {
                retval.add(nodeByID);
            } else {
                LOG.log(Level.WARNING, "Cannot load node " + nodeID.getNodeId()
                        + "! Ignoring this node.");
            }
        }

        return retval;
    }

//    /**
//     * @param aWay the way to get the nodes for
//     * @return all nodes for the way. (for OSM-0.5 ordered, for OSM-0.4 unordered)
//     */
//    public List<Node> getNodes(final WayBuilder aWay) {
//
//        List<WayNode> nodeIDs = aWay.getWayNodes();
//        ArrayList<Node> retval = new ArrayList<Node>(nodeIDs.size());
//        for (WayNode nodeID : nodeIDs) {
//            Node nodeByID = getMap().getNodeByID(nodeID.getNodeId());
//            if (nodeByID != null) {
//                retval.add(nodeByID);
//            } else {
//                LOG.log(Level.WARNING, "Cannot load node " + nodeID.getNodeId()
//                        + " for way " + aWay.getId() + "! Ignoring this node.");
//            }
//        }
//
//        return retval;
//    }

    /**
     * Get the bounding-box of all nodes of the given way.
     * @param way a way
     * @return the bounds of all nodes of the way
     */
    public Bounds getWayBounds(final Way way) {
        // get the bounds of the street
        BoundingXYVisitor streetBounds = new BoundingXYVisitor(getMap());
        List<Node> nodes = this.getNodes(way);
        for (Node node : nodes) {
            streetBounds.visit(node);
        }
        return streetBounds.getBounds();
    }
    /**
     * Get the bounding-box of all nodes of the given ways.
     * @param ways a ways
     * @return the bounds of all nodes of the way
     */
    public Bounds getWayBounds(final Collection<Way> ways) {
        // get the bounds of the street
        BoundingXYVisitor streetBounds = new BoundingXYVisitor(getMap());
        for (Way way : ways) {
            List<Node> nodes = this.getNodes(way);
            for (Node node : nodes) {
                streetBounds.visit(node);
            }
        }
        return streetBounds.getBounds();
    }

    //---------------------------------------- higher functions

    /**
     * Simplify the given way.
     * Do not touch nodes that are part of more then one way.
     * @param w the way to look at. It is not modified
     * @return the new list of way-nodes for the simplified way.
     */
    public List<WayNode> simplifyWay(final Way w) {
        double threshold =
            Settings.getInstance().getDouble("simplify-way.max-error", DEFAULTSIMPLIFYTHRESHOLD);
        double maxNodeDist =
            Settings.getInstance().getDouble("simplify-way.min-dist", DEFAULTSIMPLIFYNODEDISTANCE);
        return simplifyWay(w, threshold, maxNodeDist, false, true);
    }
    /**
     * Simplify the given way.
     * Do not touch nodes that are part of more then one way.
     * @param w the way to look at. It is not modified
     * @param threshold the threshold for xtemax in meters.
     * @param maxNodeDistance maximum distance between 2 nodes
     * @param keepTaggedNodes if true, nodes with tags are not removed
     * @param keepIntersectionNodes keep nodes belonging to more then one way
     * @return the new list of way-nodes for the simplified way.
     */
    public List<WayNode> simplifyWay(final Way w, final double threshold,
            final double maxNodeDistance,
            final boolean keepTaggedNodes,
            final boolean keepIntersectionNodes) {

        List<WayNode> wnew = new ArrayList<WayNode>(w.getWayNodes());
        return simplifyWay(wnew, threshold, maxNodeDistance, keepTaggedNodes, keepIntersectionNodes);
    }
    /**
     * Simplify the given way.
     * Do not touch nodes that are part of more then one way.
     * @param wnew the way to look at. It is not modified
     * @param threshold the threshold for xtemax in meters in meters.
     * @param minNodeDistance maximum distance between 2 nodes
     * @param keepTaggedNodes if true, nodes with tags are not removed
     * @param keepIntersectionNodes keep nodes belonging to more then one way
     * @return the new list of way-nodes for the simplified way.
     */
    public List<WayNode> simplifyWay(final List<WayNode> wnew, final double threshold,
            final double minNodeDistance,
            final boolean keepTaggedNodes,
            final boolean keepIntersectionNodes) {

        List<WayNode> wold = new ArrayList<WayNode>(wnew);
        int toI = wnew.size() - 1;
        for (int i = wnew.size() - 1; i >= 0; i--) {
            boolean used = false;
            if (keepIntersectionNodes) {
                Iterator<Way> backRefsV = myMap.getWaysForNode(wnew.get(i).getNodeId());
                if (backRefsV != null && backRefsV.hasNext()) {
                    backRefsV.next();
                    used = backRefsV.hasNext();
                }
            }
            if (keepTaggedNodes && !used) {
                Node node = myMap.getNodeByID(wnew.get(i).getNodeId());
                if (node != null) {
                    used = node.getTags().size() > 0;
                }
            }


            if (used) {
                simplifyWayRange(wnew, i, toI, threshold, minNodeDistance);
                toI = i;
            }
        }
        simplifyWayRange(wnew, 0, toI, threshold, minNodeDistance);
        if (LOG.isLoggable(Level.FINEST))
            LOG.log(Level.FINEST, "WayHelper.simplifyWay(threshold=" + threshold
                    + ") simplified from " + wold.size() + " to "
                    + wnew.size() + " nodes");
        return wnew;
    }

    /**
     * Simplify the part of the given way between from and to.
     * We assume that any node except to and from can be removed
     * without breaking anything.
     * @param thr the threshold for xtemax in meters.
     * @param aWay the way who's WayNodes to look at.
     * @param from the first way-node to look at.
     * @param to the last way-node to look at.
     * @param minDist minimum distance between 2 nodes
     */
    public void simplifyWayRange(final List<WayNode> aWay, final int from,
                                 final int to,
                                 final double thr,
                                 final double minDist) {
        if (to - from >= 2) {
            // replace the nodes between to and from with
            // the simplified list of nodes "ns".
            List<WayNode> ns = new ArrayList<WayNode>();
            simplifyWayRange(aWay, from, to, ns, thr, minDist);
            for (int j = to - 1; j > from; j--) {
                aWay.remove(j);
            }
            aWay.addAll(from + 1, ns);
        }
    }
    /**
     * the earch-radius im meters.
     */
    private static final double EARTHRAD = 6378137.0;

    /**
     * Takes an interval [from,to] and add all the WayNodes from (from,to) to ns
     * that satisfy (xte > xtemax) as long as xtemax >= thr.
     * (from and to are indices of wnew.nodes.)
     * @param thr the threshold for xtemax in meters.
     * @param aWay the way who's WayNodes to look at.
     * @param from the first way-node to look at.
     * @param to the last way-node to look at.
     * @param aSimpleWay the returned list of WayNodes that satisfy (xte > xtemax)
     * @param minDist minimum distance between 2 nodes
     */
    private void simplifyWayRange(final List<WayNode> aWay,
            final int from, final int to,
            final List<WayNode> aSimpleWay,
            final double thr,
            final double minDist) {
        WayNode fromWN = aWay.get(from);
        Node     fromN = myMap.getNodeByID(fromWN.getNodeId());
        WayNode   toWN = aWay.get(to);
        Node      toN  = myMap.getNodeByID(toWN.getNodeId());
        int imax = -1; // the node with the maximum xte-value
        double xtemax = 0; // the maximum xte-value

        if (fromN == null || toN == null) {
            for (int i = from + 1; i < to; i++) {
                WayNode wn = aWay.get(i);
                aSimpleWay.add(wn);
            }
        } else {
            for (int i = from + 1; i < to; i++) {
                WayNode wn = aWay.get(i);
                Node n = myMap.getNodeByID(wn.getNodeId());
                if (n != null) {
                    //TODO: use distance(node, nodeStart, nodeEnd) here instead
                    double xtd = LatLon.xtd(
                            fromN.getLatitude(), fromN.getLongitude(),
                            toN.getLatitude()  , toN.getLongitude(),
                            n.getLatitude()    , n.getLongitude());
                    double dist = Math.max(
                            Math.abs(fromN.getLatitude() - n.getLatitude()),
                            Math.abs(fromN.getLongitude() - n.getLongitude()));
                    double xte = Math.abs(EARTHRAD * xtd);
                        if (xte > xtemax && dist > minDist) {
                            xtemax = xte;
                            imax = i;
                        }
                }
            }
        }

        if (imax != -1 && xtemax >= thr) {
            simplifyWayRange(aWay, from, imax, aSimpleWay, thr, minDist);
            aSimpleWay.add(aWay.get(imax));
            simplifyWayRange(aWay, imax, to, aSimpleWay, thr, minDist);
        }
    }

    /**
     * @param street a way
     * @param aMap a map to get the nodes
     * @return bounds that contain all points of that way.
     */
    public static Bounds getBoundsForWay(final Way street, final IDataSet aMap) {
        CityBounds bounds = new CityBounds(WayHelper.getTag(street, Tags.TAG_NAME));
        int nodes = 0;
        for (WayNode wayNode : street.getWayNodes()) {
            Node node = aMap.getNodeByID(wayNode.getNodeId());
            if (node != null) {
                bounds.addPoint(node.getLatitude(), node.getLongitude());
                nodes++;
            }
        }
        if (nodes < 2) {
            return null;
        }
        return bounds;
    }

    /**
     * Return true if way is a closed one.
     * (Does not look for MultiPolygon-relations)
     * @param aWay the way
     * @return true if way is a polygon
     */
    public static boolean isPolygon(final Way aWay) {
        List<WayNode> wayNodes = aWay.getWayNodes();
        if (wayNodes.size() < 2) {
            return false;
        }
        WayNode firstNode = wayNodes.get(0);
        WayNode lastNode = wayNodes.get(wayNodes.size() - 1);
        return lastNode.getNodeId() == firstNode.getNodeId();
    }
    /**
     * Return true if way is oneway.
     * @param way the way
     * @return true if way is oneway
     */
    public static boolean isOneway(final Way way) {
        // primary
        String oneway = WayHelper.getTag(way, Tags.TAG_ONEWAY);
        if (oneway != null) {
            oneway = oneway.toLowerCase();
            return oneway.equals("yes") || oneway.equals("true");
        }
        // secondary
        String junction = WayHelper.getTag(way, Tags.TAG_JUNCTION);
        if (junction != null) {
            junction = junction.toLowerCase();
            return junction.equals("roundabout");
        }
        return  false;
    }

    /**
     * Return true if way is reverse oneway.
     * @param way the way to test
     * @return true if this is a oneway-street in the opposide direction
     */
    public static boolean isReverseOneway(final Way way) {

        // primary
        String oneway = WayHelper.getTag(way, Tags.TAG_ONEWAY);
        if (oneway != null) {
            oneway = oneway.toLowerCase();
            return oneway.equals("-1") || oneway.equals("reverse");
        }
        return  false;
    }

    /**
     * Return true if way is bridge.
     * @param way the way
     * @return true if way is bridge
     */
    public static boolean isBridge(final Way way) {
        // primary
        String bridge = WayHelper.getTag(way, Tags.TAG_BRIDGE);
        if (bridge != null) {
            bridge = bridge.toLowerCase();
            return bridge.equals("yes") || bridge.equals("true");
        }
        return  false;
    }

    /**
     * Function to calculate the area of polygon bounding box.
     * @param way the way of polygon
     * @return area of the polygon bounding box in square meters.
     */
    public static long areaBoundingBox(final ExtendedWay way) {
        double height = LatLon.distanceInMeters(
                new LatLon(way.getMinLatitude(), way.getMinLongitude()),
                new LatLon(way.getMaxLatitude(), way.getMinLongitude()));
        double width = LatLon.distanceInMeters(
                new LatLon(way.getMinLatitude(), way.getMinLongitude()),
                new LatLon(way.getMinLatitude(), way.getMaxLongitude()));
        return (int) (height * width);
    }

    /**
     * Function to calculate the area of a polygon, according to the algorithm
     * defined at http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/
     * Please use an area-true projection for serious use.
     * @param polyPoints
     *            array of points in the polygon
     * @return area of the polygon defined by pgPoints
     */
    public static double area(final Coordinate[] polyPoints) {
        int i, j, n = polyPoints.length;
        double area = 0;

        for (i = 0; i < n; i++) {
            j = (i + 1) % n;
            area += polyPoints[i].getXCoord() * polyPoints[j].getYCoord();
            area -= polyPoints[j].getXCoord() * polyPoints[i].getYCoord();
        }
        area /= 2.0;
        return (area);
    }

    /**
     * Function to calculate the center of mass for a given polygon, according
     * ot the algorithm defined at
     * http://local.wasp.uwa.edu.au/~pbourke/geometry/polyarea/ .
     *
     * @param polyPoints
     *            array of points in the polygon
     * @return point that is the center of mass (x, y)
     */
    public static double[] centerOfMass(final Coordinate[] polyPoints) {
        double cx = 0, cy = 0;
        double area = area(polyPoints);
        // could change this to Point2D.Float if you want to use less memory
        int i, j, n = polyPoints.length;

        double factor = 0;
        for (i = 0; i < n; i++) {
            j = (i + 1) % n;
            factor = (polyPoints[i].getXCoord() * polyPoints[j].getYCoord()
                    - polyPoints[j].getXCoord() * polyPoints[i].getYCoord());
            cx += (polyPoints[i].getXCoord() + polyPoints[j].getXCoord()) * factor;
            cy += (polyPoints[i].getYCoord() + polyPoints[j].getYCoord()) * factor;
        }
        final float f = 6.0f;
        area *= f;
        factor = 1 / area;
        cx *= factor;
        cy *= factor;
        return new double[]{cx, cy};
    }

    /**
     * @param aToWay the way to check.
     * @param aNodeID the ID to look for
     * @return true if the way contains the node with this ID.
     */
    public static boolean containsNode(final Way aToWay, final long aNodeID) {
        List<WayNode> wayNodes = aToWay.getWayNodes();
        for (WayNode wayNode : wayNodes) {
            if (wayNode.getNodeId() == aNodeID) {
                return true;
            }
        }
        return false;
    }


    /**
     * Determine if all of aFirstWay is parallel to a part of aSecondWay or the
     * other way around.
     * To optimize, aFirstWay should be the longer one.
     * @param aFirstWay way 1 - warning: this this is being modified
     * @param aSecondWay way 2 - warning: this this is being modified
     * @return null if not or a list of the smaller way merged into the longer way
     */
    public static List<Node> isParallel(final List<Node> aFirstWay, final List<Node> aSecondWay) {
        ArrayList<Node> aFirstWayCopy = new ArrayList<Node>(aFirstWay);
        ArrayList<Node> aSecondWayCopy = new ArrayList<Node>(aSecondWay);

        List<Node> retval = isParallelInternal(aFirstWayCopy, aSecondWayCopy);
        if (retval != null) {
            return retval;
        }
        aFirstWayCopy = new ArrayList<Node>(aFirstWay);
        aSecondWayCopy = new ArrayList<Node>(aSecondWay);
        Collections.reverse(aSecondWayCopy);
        retval = isParallelInternal(aSecondWayCopy, aFirstWayCopy);
        if (retval != null) {
            return retval;
        }
        return null;
    }

    /**
     * Determine if all of aFirstWay is parallel to a part of aSecondWay or the
     * other way around.
     * To optimize, aFirstWay should be the longer one.
     * @param aFirstWay way 1 - warning: this this is being modified
     * @param aSecondWay way 2 - warning: this this is being modified
     * @return null if not or a list of the smaller way merged into the longer way
     */
    private static List<Node> isParallelInternal(final List<Node> aFirstWay, final List<Node> aSecondWay) {

      ArrayList<Node> aFirstWayCopy = new ArrayList<Node>(aFirstWay);
        final double maxDist = 20; //20 meters apart is okay for 2 lanes even in extreme circumstances
        for (Node node : aSecondWay) {
            double dist = getDistanceAndInsert(node, aFirstWayCopy);
            if (dist > maxDist) {
                return null;
            }
        }

      return aFirstWayCopy;


//        ArrayList<Node> aFirstWayCopy = new ArrayList<Node>(aFirstWay);
//        ArrayList<Node> aSecondWayCopy = new ArrayList<Node>(aSecondWay);
//        int lastParallel = -1;
//        for (Node node : aSecondWayCopy) {
//            double dist = getDistanceAndInsert(node, aFirstWayCopy);
//            if (dist > maxDist) {
//                if (lastParallel >= 0) {
//                    // add the remaining nodes to the end
//                    //aFirstWay.add(node);
//                } else {
//                    break;
//                }
//            } else {
//                lastParallel++;
//            }
//        }
//        if (lastParallel >= 0) {
//            return aFirstWayCopy;
//        }
//
//        aFirstWayCopy = new ArrayList<Node>(aFirstWay);
//        aSecondWayCopy = new ArrayList<Node>(aSecondWay);
//        lastParallel = -1;
//        for (Node node : aFirstWayCopy) {
//            double dist = getDistanceAndInsert(node, aSecondWayCopy);
//            if (dist > maxDist) {
//                if (lastParallel >= 0) {
//                    // add the remaining nodes to the end
//                    //aSecondWay.add(node);
//                } else {
//                    break;
//                }
//            } else {
//                lastParallel++;
//            }
//        }
//        if (lastParallel >= 0) {
//            return aSecondWayCopy;
//        }
//        return null;
    }

    /**
     * Side-effect: aNode is inserted into aFirstWay in the place of least distance to the way.
     * @param aNode a node to determine the distance of
     * @param aWay a way to determine the distance to and insert the node into
     * @return the shortest distance between a point and a polyline in meters.
     */
    public static double getDistanceAndInsert(final Node aNode, final List<Node> aWay) {
        double minDist = Double.MAX_VALUE;
        int minIndex = 0;
        Node lastNode = null;
        int index = 1;
        for (Node node : aWay) {
            if (aNode.getId() == node.getId()) {
                return 0.0;
            }
            if (lastNode == null) {
                lastNode = node;
                continue;
            }
            double dist = getDistance(aNode, lastNode, node);
            lastNode = node;
            if (dist < minDist) {
                minDist = dist;
                minIndex = index;
                if (minDist == 0 && aNode.getId() == lastNode.getId()) {
                    return 0;
                }
            }
            index++;
        }
        if (minIndex == 1 && minDist == LatLon.distanceInMeters(aNode, aWay.get(0))) {
            aWay.add(0, aNode);
        } else if (minIndex == aWay.size() - 1 && minDist == LatLon.distanceInMeters(aNode, aWay.get(aWay.size() - 1))) {
            aWay.add(aNode);
        } else {
            aWay.add(minIndex, aNode);
        }
        return minDist;
    }

    /**
     * @param aNode  the node to test
     * @param aStart start of the line
     * @param aEnd   end of the line
     * @return the shortest distance between a point and a line in meters.
     */
    public static double getDistance(final Node aNode, final Node aStart, final Node aEnd) {
        // the line is defined as P(aStart + u*(aEnd-aStart))

        if (aStart.getLatitude() == aEnd.getLatitude()
                && aStart.getLongitude() == aEnd.getLongitude()) {
            return LatLon.distanceInMeters(aNode, aStart);
        }
        double length = LatLon.distance(aEnd, aStart);

        // warning: length is the squared distance
        // determine u:
        // u= (
        //      (aNode.lat - aStart.lat)*(aEnd.lat - aStart.lat)
        //     + (aNode.lon - aStart.lon)*(aEnd.lon - aStart.lon)
        //    ) / length
        double u = (
                      (aNode.getLatitude() - aStart.getLatitude())
                      * (aEnd.getLatitude() - aStart.getLatitude())
                    + (aNode.getLongitude() - aStart.getLongitude())
                      * (aEnd.getLongitude() - aStart.getLongitude())
                    ) / length;
        if (u < 0.0 || u > 1.0) {
            return Math.min(LatLon.distanceInMeters(aNode, aStart), LatLon.distanceInMeters(aNode, aEnd));
        }
        double projectedLat = aStart.getLatitude()  + u * (aEnd.getLatitude() - aStart.getLatitude());
        double projectedLon = aStart.getLongitude() + u * (aEnd.getLongitude() - aStart.getLongitude());
        return LatLon.distanceInMeters(new LatLon(aNode.getLatitude(), aNode.getLongitude()), new LatLon(projectedLat, projectedLon));
    }

}
