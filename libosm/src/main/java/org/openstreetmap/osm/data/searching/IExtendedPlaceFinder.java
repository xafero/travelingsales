/**
 * IExtendedPlaceFinder.java
 * created: 18.11.2007 11:59:56
 * (c) 2007 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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

import java.util.Collection;


/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * IExtendedPlaceFinder.java<br/>
 * created: 18.11.2007 11:59:56 <br/>
 *<br/><br/>
 * An ExtendedPlaceFinder allows the search for more structured
 * places then just the simple keyword-search an IPlaceFinder is
 * capable of.<br/>
 * It knows about streets, houses and cities.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public interface IExtendedPlaceFinder extends IPlaceFinder {

    /**
     * Most simple search-method. Returns everything that matches this name.
     * @param houseNr (may be null) the house-number.
     * @param street (may NOT be null) the house-number.
     * @param city (may NOT be null) the city.
     * @param zipCode (may be null) the zip-code of/in the city.
     * @param country (may be null) the country.
     * @return all matching places
     */
    Collection<Place> findAddress(final String houseNr, final String street, final String city, final String zipCode, final String country);
}
