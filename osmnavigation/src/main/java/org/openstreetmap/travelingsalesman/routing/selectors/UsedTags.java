/**
 * 
 */
package org.openstreetmap.travelingsalesman.routing.selectors;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.openstreetmap.osm.Tags;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.Selector;
import org.openstreetmap.osm.data.WayHelper;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * Selector that lets only elements pass that
 * are used in routing or rendering.
 */
public class UsedTags extends Tags implements Selector/*, org.openstreetmap.travelingsalesman.routing.Vehile*/ {
    /**
     * my logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(UsedTags.class.getName());

    /**
     * Keep these tags regardless of their value when filtering tags on entities.
     */
    private static final Set<String> TAGS = new HashSet<String>();
    /**
     * Keep these tags/value -combinations when filtering tags on entities.
     */
    private static final Map<String, Set<String>> TAGSVALUES = new HashMap<String, Set<String>>();

    /**
     * "aeroway".
     */
    public static final String TAG_AEROWAY = "aeroway";

    /**
     * "amenity".
     */
    public static final String TAG_AMENITY = "amenity";

    /**
     * "shop".
     */
    public static final String TAG_SHOP = "shop";

    /**
     * "tourism".
     */
    public static final String TAG_TOURISM = "tourism";

    /**
     * Tag to describe if cars have access to a road.<br/>
     * Valid values: "no", "false".
     */
    public static final String TAG_ACCESS_CAR = "car";

    /**
     * "barrier".
     */
    public static final String TAG_BARRIER = "barrier";

    /**
     * Tag to describe if hgvs have access to a road.<br/>
     * Valid values: "no", "false".
     */
    public static final String TAG_ACCESS_HEAVYGOODSVEHICLE = "hgv";

    /**
     * "railway".
     */
    public static final String TAG_RAILWAY = "railway";
    /**
     * "maxspeed".
     * Valid values: [0-9]+ " "* + ("kmh"|"kph"|"mph")?
     */
    public static final String TAG_MAXSPEED = "maxspeed";

    /**
     * "is_in:country".
     * @see #TAG_IS_IN
     */
    public static final String TAG_IS_IN_COUNTRY = "is_in:country";

    /**
     * "building".
     */
    public static final String TAG_BUILDING = "building";

    /**
     * "sport".
     */
    public static final String TAG_SPORT = "sport";

    /**
     * "historic".
     */
    public static final String TAG_HISTORIC = "historic";

    /**
     * Width of the street.
     * "width"
     */
    public static final String TAG_WIDTH = "width";
    /**
     * Maximum weight the street can carry.
     * "maxweight"
     */
    public static final String TAG_WEIGHT = "maxweight";
    /**
     * Maximum height of a vehicle the tunnel can carry.
     * "maxheight"
     */
    public static final String TAG_HEIGHT = "maxheight";

    static {
        TAGS.add(TAG_NAME);
        TAGS.add(TAG_REF);
        TAGS.add(TAG_INT_REF);
        TAGS.add(TAG_LOC_REF);
        TAGS.add(TAG_NAT_REF);
        TAGS.add(TAG_HIGHWAY);
        TAGS.add(TAG_HEIGHT);
        TAGS.add(TAG_WIDTH);
        TAGS.add(TAG_WEIGHT);
        TAGS.add(TAG_BRIDGE);
        TAGS.add(TAG_JUNCTION);
        TAGS.add(TAG_AEROWAY);
        TAGS.add(TAG_MAXSPEED);
        TAGS.add(TAG_JUNCTION);
        TAGS.add(TAG_BARRIER);
        TAGS.add(TAG_AMENITY);
        TAGS.add(TAG_TOURISM);
        TAGS.add(TAG_SHOP);
        TAGS.add(TAG_PLACE);
        TAGS.add(TAG_NATURAL);
        TAGS.add("historic");
        TAGS.add(TAG_WATERWAY);
        TAGS.add(TAG_RIVERBANK);
        TAGS.add(TAG_RAILWAY);
        TAGS.add(TAG_LANDUSE);
        TAGS.add("leisure");
        TAGS.add(TAG_ADDR_HOUSENUMBER);
        TAGS.add(TAG_ADDR_HOUSENAME);
        TAGS.add(TAG_ADDR_STREET);
        TAGS.add(TAG_ADDR_POSTALCODE);
        TAGS.add(TAG_ADDR_POSTALCODE2);
        TAGS.add(TAG_ADDR_FULL);
        TAGS.add("addr:interpolation");
        TAGS.add(TAG_BUILDING);
        TAGS.add(TAG_SPORT);
        TAGS.add(TAG_HISTORIC);
    }
    static {
        Set<String> yesNo = new HashSet<String>();
        yesNo.add("yes");
        yesNo.add("no");
        yesNo.add("true");
        yesNo.add("false");
        yesNo.add("0");
        yesNo.add("1");
        yesNo.add("-1");
        TAGSVALUES.put(Tags.TAG_ONEWAY, yesNo);
        TAGSVALUES.put("bridge", yesNo);
        TAGSVALUES.put("tunnel", yesNo);
        TAGSVALUES.put(TAG_ACCESS_CAR, yesNo);
        TAGSVALUES.put(TAG_ACCESS_HEAVYGOODSVEHICLE, yesNo);
    }

    /**
     * The number of tags we filtered out.
     */
    private long myIgnoredTagsCount;

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.Selector#isAllowed(org.openstreetmap.osm.data.IDataSet, org.openstreetmap.osmosis.core.domain.v0_6.Node)
     */
    @Override
    public boolean isAllowed(final IDataSet aMap, final Node aNode) {
    	// LOG.info("isAllowed: "+aMap+", "+aNode);
    	
        return true;
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.Selector#isAllowed(org.openstreetmap.osm.data.IDataSet, org.openstreetmap.osmosis.core.domain.v0_6.Way)
     */
    @Override
    public boolean isAllowed(final IDataSet aMap, final Way aWay) {
        if (WayHelper.getTag(aWay, TAG_AMENITY) != null) {
            return true;
        }
        if (WayHelper.getTag(aWay, TAG_HIGHWAY) != null) {
            return true;
        }
        if (WayHelper.getTag(aWay, TAG_HISTORIC) != null) {
            return true;
        }
        if (WayHelper.getTag(aWay, TAG_PLACE) != null) {
            return true;
        }
        if (WayHelper.getTag(aWay, TAG_NATURAL) != null) {
            return true;
        }
        if (WayHelper.getTag(aWay, TAG_BUILDING) != null) {
            return true;
        }
        if (WayHelper.getTag(aWay, TAG_LANDUSE) != null) {
            return true;
        }
        if (WayHelper.getTag(aWay, TAG_WATERWAY) != null) {
            return true;
        }
        if (WayHelper.getTag(aWay, "leisure") != null) {
            return true;
        }
        if (WayHelper.getTag(aWay, TAG_ADDR_INTERPOLATON) != null) {
            return true;
        }
        if (WayHelper.getTag(aWay, TAG_ADDR_HOUSENUMBER) != null) {
            return true;
        }
        if (WayHelper.getTag(aWay, TAG_ADDR_HOUSENAME) != null) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * @see org.openstreetmap.osm.data.Selector#isAllowed(org.openstreetmap.osm.data.IDataSet, org.openstreetmap.osmosis.core.domain.v0_6.Relation)
     */
    @Override
    public boolean isAllowed(final IDataSet aMap, final Relation aRelation) {
        String type = WayHelper.getTag(aRelation, Tags.TAG_TYPE);
        if (type != null) {
            if (type.equalsIgnoreCase("street")) {
                return true;
            }
            if (type.equalsIgnoreCase("associatedStreet")) {
                return true;
            }
            if (type.equalsIgnoreCase("roadAccess")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove all attributed from the way
     * that we do not use.
     * @param in the way to filter
     * @return the same way or a writable copy of it
     */
    public Way filterWay(final Way in) {
        Way wb = in.getWriteableInstance();
        //WayBuilder wb = new WayBuilder(in);
        Collection<Tag> newTags = new HashSet<Tag>();
        Collection<Tag> tags2 = wb.getTags();
        for (Tag tag : tags2) {
            String key = tag.getKey();
            if (TAGS.contains(key)) {
                newTags.add(tag);
                continue;
            }
            Set<String> values = TAGSVALUES.get(key);
            if (values != null && values.contains(tag.getValue())) {
                newTags.add(tag);
                continue;
            }
            this.myIgnoredTagsCount++;
//            if (key.equals("created_by") || key.equals("source")) {
//                continue; // dont log this one
//            }
//            LOG.severe("filtering out tag: "
//                    + key + "=" + tag.getValue());
        }
        wb.getTags().clear();
        wb.getTags().addAll(newTags);
        return wb;
//        wb.setTags(newTags);
//        return wb.buildEntity();
    }
    /**
     * Remove all tags from the node that are not used
     * by Traveling Salesman.
     * @param in the node to filter
     * @return the filtered node
     */
    public Node filterNode(final Node in) {
        Node wb = in.getWriteableInstance();
        //NodeBuilder wb = new NodeBuilder(in);
        Collection<Tag> newTags = new HashSet<Tag>();
        Collection<Tag> tags2 = wb.getTags();
        for (Tag tag : tags2) {
            String key = tag.getKey();
            if (TAGS.contains(key)) {
//                newTags.add(tag);
                continue;
            }
            Set<String> values = TAGSVALUES.get(key);
            if (values != null && values.contains(tag.getValue())) {
                newTags.add(tag);
                continue;
            }
            this.myIgnoredTagsCount++;
//            if (key.equals("created_by") || key.equals("source")) {
//                continue; // dont log this one
//            }
//            LOG.severe("filtering out tag: "
//                    + key + "=" + tag.getValue());
        }
        wb.getTags().clear();
        wb.getTags().addAll(newTags);
        //wb.setTags(newTags);
        return wb; //wb.buildEntity();
    }

    /**
     * @return the number of tags we filtered out.
     */
    public long getIgnoredTagsCount() {
        return myIgnoredTagsCount;
    }

//    /**
//     * ${@inheritDoc}.
//     */
//    @Override
//    public boolean isReverseOneway(final IDataSet aMap, final Way aWay) {
//        // we dont care.
//        return false;
//    }

//    /**
//     * ${@inheritDoc}.
//     */
//    @Override
//    public boolean isOneway(final IDataSet aMap, final Way aWay) {
//        // we dont care.
//        return false;
//    }
}
