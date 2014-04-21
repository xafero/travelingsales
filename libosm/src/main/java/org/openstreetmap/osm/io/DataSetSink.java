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

import java.util.Map;

import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osmosis.core.container.v0_6.ChangeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.common.ChangeAction;
import org.openstreetmap.osmosis.core.task.v0_6.ChangeSink;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;


/**
 * This is an <a href="http://wiki.openstreetmap.org/index.php/Osmosis">Osmosis</a> {@link Sink}
 * that writes the map-data it gets into an {@link IDataSet}.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class DataSetSink implements Sink, ChangeSink {

    /**
     * The DataSet we are writing to.
     */
    private IDataSet myDataSet = new MemoryDataSet();


    /**
     * @param aWriteTo the map to write to
     */
    public DataSetSink(final IDataSet aWriteTo) {
        this.myDataSet = aWriteTo;
    }

    /**
     */
    public DataSetSink() {
        this.myDataSet = new MemoryDataSet();
    }

    /**
     * ignored.
     */
    public void complete() {
    }

    /**
     * add the entity inside the given container to
     * {@link #myDataSet}}.
     * {@inheritDoc}
     */
    public void process(final EntityContainer aEntityContainer) {

        if (aEntityContainer instanceof NodeContainer) {
            NodeContainer nc = (NodeContainer) aEntityContainer;
            myDataSet.addNode((Node) nc.getEntity());
        }
        if (aEntityContainer instanceof WayContainer) {
            WayContainer wc = (WayContainer) aEntityContainer;
            myDataSet.addWay((Way) wc.getEntity());
        }
        if (aEntityContainer instanceof RelationContainer) {
            RelationContainer rc = (RelationContainer) aEntityContainer;
            myDataSet.addRelation((Relation) rc.getEntity());
        }
    }

    /**
     * ignored.
     */
    public void release() {
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

    /**
     * {@inheritDoc}
     */
    /* @Override */ // TODO: Check override!
    public void process(final ChangeContainer aChange) {
        ChangeAction action = aChange.getAction();
        EntityContainer entityContainer = aChange.getEntityContainer();

        switch (action) {
        default:
        case Modify:
        case Create:
            process(entityContainer);
            break;
        case Delete:
            if (entityContainer instanceof NodeContainer) {
                NodeContainer nc = (NodeContainer) entityContainer;
                myDataSet.removeNode((Node) nc.getEntity());
            } else
            if (entityContainer instanceof WayContainer) {
                WayContainer wc = (WayContainer) entityContainer;
                myDataSet.removeWay((Way) wc.getEntity());
            } else
            if (entityContainer instanceof RelationContainer) {
                RelationContainer rc = (RelationContainer) entityContainer;
                myDataSet.removeRelation((Relation) rc.getEntity());
            }
            break;
        }
    }

	public void initialize(Map<String, Object> map) {
		// TODO: Check if it works!
		throw new RuntimeException("initialize! implement it!");
	}
}