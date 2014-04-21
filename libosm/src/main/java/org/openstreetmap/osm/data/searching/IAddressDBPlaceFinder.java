/**
 * IAddressDBPlaceFinder.java
 * created: 31.01.2009 
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

import java.awt.geom.Rectangle2D;
import java.util.Collection;

import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;




/**
 * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * IAddressDBPlaceFinder.java<br/>
 * created: 31.01.2009 <br/>
 *<br/><br/>
 * Interface for {@link IPlaceFinder}s that use a database.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public interface IAddressDBPlaceFinder extends IExtendedPlaceFinder {

    /**
     * Get the map we use.
     * @return the map
     */
    IDataSet getMap();

    /**
     * @param aStreet the way to look for
     * @return the name of one city this way is in or null
     */
    String getCityNameForWay(final Way aStreet);

    /**
     * @param aStreet the way to look for
     * @return the name of one suburb this way is in or null
     */
    String getSuburbNameForWay(final Way aStreet);

    /**
     * @param aStreet the way to look for
     * @return the name of one zip-code this way is in or null
     */
    String getZipCodeNameForWay(final Way aStreet);

    /**
     * Add the given way into the index if
     * it is of a type we index (city, street, ...).
     * @param aEntity the entity to index
     */
    void indexWay(final Way aEntity);

    /**
     * Add the given node into the index if
     * it is of a type we index (city, street, ...).
     * @param aEntity the entity to index
     */
    void indexNode(final Node aEntity);

    /**
     * Index all entities in the given database.
     * @param aMap the database to index
     */
    void indexDatabase(final IDataSet aMap);

    /**
     * Commit everything.
     */
    void checkpointDB();

    /**
     * Find all cities in the given rectangle.
     * @param aSearchArea the area to search
     * @return all cities, may be empty but not null.
     */
    Collection<CityPlace> findPlaces(Rectangle2D aSearchArea);

}


