/**
 * OsmBinV10NodeEntityManager.java
 * created: 28.03.2009 07:21:07
 * (c) 2009 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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
package org.openstreetmap.osm.data.osmbin.v1_0;



import java.util.Iterator;
//import java.util.logging.Logger;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.OsmBinDataSet;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osmosis.core.container.v0_6.EntityManager;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;


/**
 * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * OsmBinV10NodeEntityManager.java<br/>
 * created: 28.03.2009 07:21:07 <br/>
 *<br/><br/>
 * <b>EntityManager&lt;Node&gt; to allow reading and writing nodes to an {@link OsmBinDataSetV10} from Osmosis.</b>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class DatasetNodeEntityManager implements EntityManager<Node> {

//    /**
//     * Automatically created logger for debug and error-output.
//     */
//    private static final Logger LOG = Logger.getLogger(OsmBinV10NodeEntityManager.class
//                                                                            .getName());

    /**
     * The dataset we operate on.
     */
    private IDataSet myDataSet;

    /**
     * @param aDataSet The dataset we operate on.
     */
    public DatasetNodeEntityManager(
            final OsmBinDataSet aDataSet) {
        this.myDataSet = aDataSet;
    }

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "OsmBinV10NodeEntityManager@" + hashCode();
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public void addEntity(final Node aEntity) {
        this.myDataSet.addNode(aEntity);
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public boolean exists(final long aId) {
        return this.myDataSet.getNodeByID(aId) != null;
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public Node getEntity(final long aId) {
        return this.myDataSet.getNodeByID(aId);
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public ReleasableIterator<Node> iterate() {
        final Iterator<Node> iterator = this.myDataSet.getNodes(Bounds.WORLD);
        if (iterator instanceof ReleasableIterator) {
            return (ReleasableIterator<Node>) iterator;
        }
        return new ReleasableIterator<Node>() {
            private Iterator<Node> myIterator = iterator;

            @Override
            public boolean hasNext() {
                return myIterator.hasNext();
            }

            @Override
            public Node next() {
                return myIterator.next();
            }

            @Override
            public void remove() {
                myIterator.remove();
            }

            @Override
            public void release() {
                // do nothing
            }
        };
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public void modifyEntity(final Node aEntity) {
        this.myDataSet.addNode(aEntity);

    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public void removeEntity(final long aEntityId) {
        Node entity = getEntity(aEntityId);
        if (entity != null) {
            this.myDataSet.removeNode(entity);
        }

    }
}


