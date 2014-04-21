/**
 * This file is part of OSMNavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  OSMNavigation is free software: you can redistribute it and/or modify
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
 *  along with OSMNavigation.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.routing.selectors;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.ConfigurationSetting;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.ConfigurationSetting.TYPES;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.NodeHelper;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osm.data.osmbin.v1_0.ExtendedNode;
import org.openstreetmap.travelingsalesman.navigation.OsmNavigationConfigSection;
import org.openstreetmap.travelingsalesman.routing.IVehicle;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * A selector that forbids footways and cycleways.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public final class Motorcar implements IVehicle {

    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(Motorcar.class.getName());

    /**
     * Setting the user can use to specify a minimum width of the street.
     */
    private static final String SETTING_WIDTH = "vehicles.motorcar.width";

    /**
     * Setting the user can use to specify a minimum height of the tunnel/....
     */
    private static final String SETTING_HEIGHT = "vehicles.motorcar.height";

    /**
     * Setting the user can use to specify a minimum weight for the street
     * to carry.
     */
    private static final String SETTING_WEIGHT = "vehicles.motorcar.weight";

    /**
     */
    public Motorcar() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAllowed(final IDataSet aMap, final Relation aRelation) {
        return true;
    }

    /**
     * @return true if this node can be traveled by car.
     * @param aNode the node to test
     * @param aMap the map we operate on.
     */
    public boolean isAllowed(final IDataSet aMap, final Node aNode) {
    	// LOG.info("isAllowed: "+aMap+", "+aNode);

        String carAccess = NodeHelper.getTag(aNode, UsedTags.TAG_ACCESS_CAR);
        if (carAccess != null && (carAccess.equalsIgnoreCase("no") || carAccess.equalsIgnoreCase("false"))) {
            return false;
        }
        String barrier = NodeHelper.getTag(aNode, UsedTags.TAG_BARRIER);
        if (barrier != null) {
            return false;
        }

        String width = WayHelper.getTag(aNode, UsedTags.TAG_WIDTH);
        if (width != null) {
            double configured = Settings.getInstance().getDouble(SETTING_WIDTH, 0);
            if (configured > 0) {
                try {
                    if (Double.parseDouble(width) < configured) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    LOG.log(Level.WARNING, "Node " + aNode.getId() + " with illegal "
                            + UsedTags.TAG_WIDTH + "-tag found");
                }
            }
        }
        String weight = WayHelper.getTag(aNode, UsedTags.TAG_WEIGHT);
        if (width != null) {
            double configured = Settings.getInstance().getDouble(SETTING_WEIGHT, 0);
            if (configured > 0) {
                try {
                    if (Double.parseDouble(weight) < configured) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    LOG.log(Level.WARNING, "Node " + aNode.getId() + " with illegal "
                            + UsedTags.TAG_WEIGHT + "-tag found");
                }
            }
        }
        String height = WayHelper.getTag(aNode, UsedTags.TAG_HEIGHT);
        if (height != null) {
            double configured = Settings.getInstance().getDouble(SETTING_HEIGHT, 0);
            if (configured > 0) {
                try {
                    if (Double.parseDouble(height) < configured) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    LOG.log(Level.WARNING, "Node " + aNode.getId() + " with illegal "
                            + UsedTags.TAG_HEIGHT + "-tag found");
                }
            }
        }

        Iterator<Way> ways = null;
        if (aNode instanceof ExtendedNode) {
            ExtendedNode xnode = (ExtendedNode) aNode;
            Set<Long> referencedWayIDs = xnode.getReferencedWayIDs();
            Set<Way> ways2 = new HashSet<Way>();
            for (Long wayId : referencedWayIDs) {
                Way way = aMap.getWaysByID(wayId);
                if (way != null) {
                    ways2.add(way);
                }
            }
            ways = ways2.iterator();
        }
        if (ways == null) {
            ways = aMap.getWaysForNode(aNode.getId());
        }
        while (ways.hasNext()) {
            Way way = ways.next();
            if (way != null && isAllowed(aMap, way)) {
                return true;
            }
        }
        return false;
    }
    /**
     * @param way the way to test
     * @param aMap the map we operate on.
     * @return true if this is a oneway-street in the opposide direction
     */
    public boolean isReverseOneway(final IDataSet aMap, final Way way) {

        // primary
        String oneway = WayHelper.getTag(way, Tags.TAG_ONEWAY);
        if (oneway != null) {
            oneway = oneway.toLowerCase();
            return oneway.equals("-1") || oneway.equals("reverse");
        }
        return  false;
    }

    /**
     * @param way the way to test
     * @param aMap the map we operate on.
     * @return true if this is a oneway-street-
     */
    public boolean isOneway(final IDataSet aMap, final Way way) {

        // primary
        String oneway = WayHelper.getTag(way, Tags.TAG_ONEWAY);
        if (oneway != null) {
            oneway = oneway.toLowerCase();
            return oneway.equals("yes") || oneway.equals("true") || oneway.equals("1");
        }

        // secondary
        String junction = WayHelper.getTag(way, Tags.TAG_JUNCTION);
        if (junction != null) {
            junction = junction.toLowerCase();
            return junction.equals("roundabout");
        }

        return  false;
        // motorway does not imply oneway=yes in all countries!
        // This may lead to very bad routes!!!
    }

    /**
     * See http://wiki.openstreetmap.org/index.php/Map_Features#Highway
     * for details on the tags we test.
     * @return true if this node can be traveled by car.
     * @param aWay the way to test
     * @param aMap the map we operate on.
     */
    public boolean isAllowed(final IDataSet aMap, final Way aWay) {
        if (aWay == null) {
            throw new IllegalArgumentException("null way given");
        }

        String carAccess = WayHelper.getTag(aWay, UsedTags.TAG_ACCESS_CAR);
        // explicitely denied
        if (carAccess != null && (carAccess.equalsIgnoreCase("no") || carAccess.equalsIgnoreCase("false"))) {
            return false;
        }
        // explicitely allowed
        if (carAccess != null && (carAccess.equalsIgnoreCase("yes") || carAccess.equalsIgnoreCase("true"))) {
            return true;
        }

        String junction = WayHelper.getTag(aWay, UsedTags.TAG_JUNCTION);
        if (junction != null && !ALLOWEDJUNCTIONTYPES.contains(junction)) {
            return false;
        }

        // it must be a highway and not a
        // fence or powerline or area-marker,..
        String highway = WayHelper.getTag(aWay, Tags.TAG_HIGHWAY);
        if (highway == null || !ALLOWEDHIGHWAYTYPES.contains(highway)) {
            return false;
        }
        String width = WayHelper.getTag(aWay, UsedTags.TAG_WIDTH);
        if (width != null) {
            double configured = Settings.getInstance().getDouble(SETTING_WIDTH, 0);
            if (configured > 0) {
                try {
                    if (Double.parseDouble(width) < configured) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    LOG.log(Level.WARNING, "Way " + aWay.getId() + " with illegal "
                            + UsedTags.TAG_WIDTH + "-tag found");
                }
            }
        }
        String weight = WayHelper.getTag(aWay, UsedTags.TAG_WEIGHT);
        if (width != null) {
            double configured = Settings.getInstance().getDouble(SETTING_WEIGHT, 0);
            if (configured > 0) {
                try {
                    if (Double.parseDouble(weight) < configured) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    LOG.log(Level.WARNING, "Way " + aWay.getId() + " with illegal "
                            + UsedTags.TAG_WEIGHT + "-tag found");
                }
            }
        }
        String height = WayHelper.getTag(aWay, UsedTags.TAG_HEIGHT);
        if (height != null) {
            double configured = Settings.getInstance().getDouble(SETTING_HEIGHT, 0);
            if (configured > 0) {
                try {
                    if (Double.parseDouble(height) < configured) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    LOG.log(Level.WARNING, "Way " + aWay.getId() + " with illegal "
                            + UsedTags.TAG_HEIGHT + "-tag found");
                }
            }
        }

        return true;
    }

    /**
     * Values for the highway-tag that are not allowed.
     */
    private static final Set<String> ALLOWEDHIGHWAYTYPES = new HashSet<String>();

    /**
     * Values for the junction-tag that are not allowed.
     */
    private static final Set<String> ALLOWEDJUNCTIONTYPES = new HashSet<String>();

    static {
        ALLOWEDHIGHWAYTYPES.add("motorway");
        ALLOWEDHIGHWAYTYPES.add("motorway_link");
        ALLOWEDHIGHWAYTYPES.add("motorway_junction");
        ALLOWEDHIGHWAYTYPES.add("trunk");
        ALLOWEDHIGHWAYTYPES.add("trunk_link");
        ALLOWEDHIGHWAYTYPES.add("primary");
        ALLOWEDHIGHWAYTYPES.add("primary_link");
        ALLOWEDHIGHWAYTYPES.add("secondary");
        ALLOWEDHIGHWAYTYPES.add("tertiary");
        ALLOWEDHIGHWAYTYPES.add("unclassified");
        ALLOWEDHIGHWAYTYPES.add("track");
        ALLOWEDHIGHWAYTYPES.add("residential");

//      these are to be used on nodes but by mistake may have been added to a way
        ALLOWEDHIGHWAYTYPES.add("mini_roundabout");
        ALLOWEDHIGHWAYTYPES.add("stop");
        ALLOWEDHIGHWAYTYPES.add("traffic_signals");
        ALLOWEDHIGHWAYTYPES.add("crossing");
        ALLOWEDHIGHWAYTYPES.add("toll_booth");
        ALLOWEDHIGHWAYTYPES.add("incline");
        ALLOWEDHIGHWAYTYPES.add("incline_steep");
        ALLOWEDHIGHWAYTYPES.add("ford");
        ALLOWEDHIGHWAYTYPES.add("bus_stop"); // must be carefull here

//      junctions
        ALLOWEDJUNCTIONTYPES.add("roundabout");
        ALLOWEDJUNCTIONTYPES.add("User_Defined");
        ALLOWEDJUNCTIONTYPES.add("user_deined");

//      must be carefull with these but they
//      may be part of the start or end of a route
        ALLOWEDHIGHWAYTYPES.add("service");
        ALLOWEDHIGHWAYTYPES.add("gate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationSection getSettings() {
        ConfigurationSection settings = new ConfigurationSection("Motorcar");
        settings.addSetting(new ConfigurationSetting(SETTING_WEIGHT,
                OsmNavigationConfigSection.RESOURCE.getString("vehicle.motorcar.weight.title"),
                TYPES.DOUBLE,
                OsmNavigationConfigSection.RESOURCE.getString("vehicle.motorcar.weight.category"),
                OsmNavigationConfigSection.RESOURCE.getString("vehicle.motorcar.weight.desc")));
        settings.addSetting(new ConfigurationSetting(SETTING_WIDTH,
                OsmNavigationConfigSection.RESOURCE.getString("vehicle.motorcar.width.title"),
                TYPES.DOUBLE,
                OsmNavigationConfigSection.RESOURCE.getString("vehicle.motorcar.width.category"),
                OsmNavigationConfigSection.RESOURCE.getString("vehicle.motorcar.width.desc")));
        settings.addSetting(new ConfigurationSetting(SETTING_HEIGHT,
                OsmNavigationConfigSection.RESOURCE.getString("vehicle.motorcar.height.title"),
                TYPES.DOUBLE,
                OsmNavigationConfigSection.RESOURCE.getString("vehicle.motorcar.height.category"),
                OsmNavigationConfigSection.RESOURCE.getString("vehicle.motorcar.height.desc")));
        return settings;
    }
}
