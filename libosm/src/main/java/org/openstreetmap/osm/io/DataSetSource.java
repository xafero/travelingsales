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
package org.openstreetmap.osm.io;

import java.util.Iterator;

import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.coordinates.Bounds;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.Source;

/**
 * This is an <a href="http://wiki.openstreetmap.org/index.php/Osmosis">Osmosis</a> {@link Sink}
 * that reads the map-data it gets from an {@link IDataSet}.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
class DataSetSource implements Source {

    /**
     * The DataSet we are reading from.
     */
    private IDataSet myDataSet;

    /**
     * @param aDataSet The DataSet we are reading from.
     */
    protected DataSetSource(final IDataSet aDataSet) {
        super();
        myDataSet = aDataSet;
    }


    /**
     * @return the dataSet we added everything we got via {@link #process(EntityContainer)} to.
     */
    public IDataSet getDataSet() {
        return myDataSet;
    }

    /**
     * @param aDataSet the dataSet to add everything we get via {@link #process(EntityContainer)} to.
     */
    public void setDataSet(final IDataSet aDataSet) {
        myDataSet = aDataSet;
    }

    /*@Override */ // TODO: Check override!
    public void setSink(final Sink aSink) {
        Iterator<Node> nodes = getDataSet().getNodes(Bounds.WORLD);
        while (nodes.hasNext()) {
            aSink.process(new NodeContainer(nodes.next()));
        }
        Iterator<Way> ways = getDataSet().getWays(Bounds.WORLD);
        while (ways.hasNext()) {
            aSink.process(new WayContainer(ways.next()));
        }
        Iterator<Relation> relations = getDataSet().getRelations(Bounds.WORLD);
        while (relations.hasNext()) {
            aSink.process(new RelationContainer(relations.next()));
        }
        aSink.complete();
    }
}
