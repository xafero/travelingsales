package org.openstreetmap.osm;

import org.openstreetmap.osm.data.searching.AddressDBPlaceFinder;
import org.openstreetmap.osm.data.searching.advancedAddressDB.AdvancedAddressDBPlaceFinder;

/**
 * List of Tags that are explicitly used in LibOSM.
 */
public class Tags {

	/**
	 * Does nothing yet.
	 */
	protected Tags() {
		super();
	}

	/**
	 * "name".
	 */
	public static final String TAG_NAME = "name";

	/**
	 * "highway" denotes general type or road. Valid values on ways:<br/>
	 * <ul>
	 * <li>"motorway"</li>
	 * <li>"motorway_link"</li>
	 * <li>"motorway_junction"</li>
	 * <li>"trunk"</li>
	 * <li>"trunk_link"</li>
	 * <li>"primary"</li>
	 * <li>"primary_link"</li>
	 * <li>"secondary"</li>
	 * <li>"tertiary"</li>
	 * <li>"unclassified"</li>
	 * <li>"track"</li>
	 * <li>"residential"</li>
	 * <li>"footway"</li>
	 * <li>"cycleway"</li>
	 * </ul>
	 * Valid values on nodes:<br/>
	 * <ul>
	 * <li>"mini_roundabout"</li>
	 * <li>"stop"</li>
	 * <li>"traffic_signals"</li>
	 * <li>"crossing"</li>
	 * <li>"toll_booth"</li>
	 * <li>"incline"</li>
	 * <li>"incline_steep"</li>
	 * <li>"ford"</li>
	 * <li>"bus_stop"</li>
	 * </ul>
	 */
	public static final String TAG_HIGHWAY = "highway";
	/**
	 * "place".
	 */
	public static final String TAG_PLACE = "place";
	/**
	 * "addr:street".
	 */
	public static final String TAG_ADDR_STREET = "addr:street";
	/**
	 * "addr:full".
	 */
	public static final String TAG_ADDR_FULL = "addr:full";
	/**
	 * "ref".
	 */
	public static final String TAG_REF = "ref";
	/**
	 * "int_ref".
	 */
	public static final String TAG_INT_REF = "int_ref";
	/**
	 * "nat_ref".
	 */
	public static final String TAG_NAT_REF = "nat_ref";
	/**
	 * "loc_ref".
	 */
	public static final String TAG_LOC_REF = "loc_ref";
	/**
	 * "is_in".
	 * 
	 * @see #TAG_IS_IN_COUNTRY
	 * @see #TAG_IS_IN_CITY
	 */
	public static final String TAG_IS_IN = "is_in";
	/**
	 * "is_in:city".
	 * 
	 * @see #TAG_IS_IN
	 */
	public static final String TAG_IS_IN_CITY = "is_in:city";

	/**
	 * "type". Commonly used on relations.
	 */
	public static final String TAG_TYPE = "type";

	/**
	 * "boundary".
	 */
	public static final String TAG_BOUNDARY = "boundary";
	/**
	 * "admin_level".
	 */
	public static final String TAG_ADMIN_LEVEL = "admin_level";

	/**
	 * "waterway".
	 */
	public static final String TAG_WATERWAY = "waterway";

	/**
	 * "waterway".
	 */
	public static final String TAG_RIVERBANK = "riverbank";

	/**
	 * "natural".
	 */
	public static final String TAG_NATURAL = "natural";

	/**
	 * "landuse".
	 */
	public static final String TAG_LANDUSE = "landuse";

	/**
	 * "oneway". Valid values: "no", "false", "true", "yes".
	 */
	public static final String TAG_ONEWAY = "oneway";

	/**
	 * Tag describing the type of a junction.<br/>
	 * Valid values: "roundabout", "User_Defined"<br/>
	 * Additional values to consider: "user_deined"
	 */
	public static final String TAG_JUNCTION = "junction";

	/**
	 * "addr:postal_code". Used by {@link AdvancedAddressDBPlaceFinder} and
	 * {@link AddressDBPlaceFinder}.
	 */
	public static final String TAG_ADDR_POSTALCODE = "addr:postcode";
	/**
	 * "post_code". Used by {@link AdvancedAddressDBPlaceFinder} and
	 * {@link AddressDBPlaceFinder}.
	 */
	public static final String TAG_ADDR_POSTALCODE2 = "post_code";

	/**
	 * "addr:interpolation". Valid values: "even", "odd", "all", "both"
	 */
	public static final String TAG_ADDR_INTERPOLATON = "addr:interpolation";

	/**
	 * "addr:housenumber".
	 */
	public static final String TAG_ADDR_HOUSENUMBER = "addr:housenumber";

	/**
	 * "addr:housename".
	 */
	public static final String TAG_ADDR_HOUSENAME = "addr:housename";

	/**
	 * "bridge".
	 */
	public static final String TAG_BRIDGE = "bridge";

}
