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

import java.util.Iterator;

import org.openstreetmap.osm.data.coordinates.Bounds;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * This class can take a DataSet and filter it acording to
 * a {@link Selector}.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class DataSetFilter {

    /**
     * The Selector we are filtering with.
     */
    private Selector mySelector;

    /**
     * @param aSelector the selector used to filter
     */
    public DataSetFilter(final Selector aSelector) {
        setSelector(aSelector);
    }

    /**
     * @return the selector
     */
    public Selector getSelector() {
        return mySelector;
    }

    /**
     * @param aSelector the selector to set
     */
    public void setSelector(final Selector aSelector) {
        mySelector = aSelector;
    }

    /**
     * Filter the input to only include what the selector
     * permits and what is referenced by the permitted
     * elements..
     * @param input the dataset to filter
     * @return null if the input is null and input if the selector is null.
     */
    public MemoryDataSet filterDataSet(final MemoryDataSet input) {
        if (input == null)
            return null;
        if (getSelector() == null)
            return input;
        MemoryDataSet output = new MemoryDataSet();

        Iterator<Way> ways = input.getWays(Bounds.WORLD);
        while (ways.hasNext()) {
            Way w = ways.next();
            if (!getSelector().isAllowed(input, w))
                continue;
            output.addWay(w);
            for (WayNode wn : w.getWayNodes()) {
                Node n = input.getNodeByID(wn.getNodeId());
                if (n != null)
                    output.addNode(n);
            }
        }

        Iterator<Node> nodes = input.getNodes(Bounds.WORLD);
        while (nodes.hasNext()) {
            Node w = nodes.next();
            if (!getSelector().isAllowed(input, w))
                continue;
            output.addNode(w);
        }

        Iterator<Relation> relations = input.getRelations(Bounds.WORLD);
        while (relations.hasNext()) {
            Relation r = relations.next();
            if (!getSelector().isAllowed(input, r))
                continue;
            output.addRelation(r);
        }

        return output;
    }


}
