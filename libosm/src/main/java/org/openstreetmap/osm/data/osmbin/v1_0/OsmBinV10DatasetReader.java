package org.openstreetmap.osm.data.osmbin.v1_0;

import java.io.File;
import java.util.Iterator;

import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osmosis.core.container.v0_6.Dataset;
import org.openstreetmap.osmosis.core.container.v0_6.DatasetContext;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityManager;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.task.v0_6.DatasetSink;
import org.openstreetmap.osmosis.core.task.v0_6.DatasetSource;
import org.openstreetmap.osmosis.core.task.v0_6.SinkDatasetSource;

/**
 * Osmosis-task to read data in OSMBin-v1.0 -format.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class OsmBinV10DatasetReader extends OsmBinV10Writer implements DatasetContext, Dataset, DatasetSource, SinkDatasetSource {


    /**
     * @param aDir the directory with the osmbin-database
     */
    public OsmBinV10DatasetReader(final File aDir) {
        super(aDir);
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public Node getNode(final long aNodeID) {
        return myDataSet.getNodeByID(aNodeID);
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public Relation getRelation(final long aRelationID) {
        return myDataSet.getRelationByID(aRelationID);
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public Way getWay(final long aWayID) {
        return myDataSet.getWaysByID(aWayID);
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public ReleasableIterator<EntityContainer> iterate() {
        return new ReleasableIterator<EntityContainer>() {

            /**
             * Iterator over all nodes. First to be tried.
             */
            private  ReleasableIterator<Node> myNodeIterator = getNodeManager().iterate();
            /**
             * Iterator over all ways. Second to be tried.
             */
            private  ReleasableIterator<Way> myWayIterator = getWayManager().iterate();
            /**
             * Iterator over all relations. First to be tried.
             */
            private  ReleasableIterator<Relation> myRelationIterator = getRelationManager().iterate();
            /**
             * The iterator that returned the last entity.
             */
            @SuppressWarnings("unchecked")
            private ReleasableIterator myLastIterator = myNodeIterator;

            @Override
            public boolean hasNext() {
                return myNodeIterator.hasNext() || this.myWayIterator.hasNext() || this.myRelationIterator.hasNext();
            }

            @Override
            public EntityContainer next() {
                if (this.myNodeIterator.hasNext()) {
                    this.myLastIterator = this.myNodeIterator;
                    return new NodeContainer(this.myNodeIterator.next());
                }
                if (this.myWayIterator.hasNext()) {
                    this.myLastIterator = this.myWayIterator;
                    return new WayContainer(this.myWayIterator.next());
                }
                if (this.myRelationIterator.hasNext()) {
                    this.myLastIterator = this.myRelationIterator;
                    return new RelationContainer(this.myRelationIterator.next());
                }
                return null;
            }

            @Override
            public void remove() {
                this.myLastIterator.remove();
            }

            @Override
            public void release() {
                this.myNodeIterator.release();
                this.myWayIterator.release();
                this.myRelationIterator.release();
                this.myNodeIterator = null;
                this.myWayIterator = null;
                this.myRelationIterator = null;
                this.myLastIterator = null;
            }
        };
    }

    /**
     * Allows all data within a bounding box to be iterated across.
     *
     * @param left
     *            The longitude marking the left edge of the bounding box.
     * @param right
     *            The longitude marking the right edge of the bounding box.
     * @param top
     *            The latitude marking the top edge of the bounding box.
     * @param bottom
     *            The latitude marking the bottom edge of the bounding box.
     * @param completeWays
     *            If true, all nodes within the ways will be returned even if
     *            they lie outside the box.
     * @return An iterator pointing to the start of the result data.
     */
    @Override
    public ReleasableIterator<EntityContainer> iterateBoundingBox(
            final double left, final double right, final double top, final double bottom, final boolean completeWays) {
        final Bounds bounds = new Bounds(left, right, top, bottom); //ignore completeWays
        return new ReleasableIterator<EntityContainer>() {

            /**
             * Iterator over all nodes. First to be tried.
             */
            private  Iterator<Node> myNodeIterator = myDataSet.getNodes(bounds);
            /**
             * Iterator over all ways. Second to be tried.
             */
            private  Iterator<Way> myWayIterator = myDataSet.getWays(bounds);
            /**
             * Iterator over all relations. First to be tried.
             */
            private  Iterator<Relation> myRelationIterator = myDataSet.getRelations(bounds);
            /**
             * The iterator that returned the last entity.
             */
            @SuppressWarnings("unchecked")
            private Iterator myLastIterator = myNodeIterator;

            @Override
            public boolean hasNext() {
                return myNodeIterator.hasNext() || this.myWayIterator.hasNext() || this.myRelationIterator.hasNext();
            }

            @Override
            public EntityContainer next() {
                if (this.myNodeIterator.hasNext()) {
                    this.myLastIterator = this.myNodeIterator;
                    return new NodeContainer(this.myNodeIterator.next());
                }
                if (this.myWayIterator.hasNext()) {
                    this.myLastIterator = this.myWayIterator;
                    return new WayContainer(this.myWayIterator.next());
                }
                if (this.myRelationIterator.hasNext()) {
                    this.myLastIterator = this.myRelationIterator;
                    return new RelationContainer(this.myRelationIterator.next());
                }
                return null;
            }

            @Override
            public void remove() {
                this.myLastIterator.remove();
            }

            @Override
            public void release() {
                this.myNodeIterator = null;
                this.myWayIterator = null;
                this.myRelationIterator = null;
                this.myLastIterator = null;
            }
        };
    }


    /**
     * ${@inheritDoc}.
     */
    @Override
    public void release() {
        this.myDataSet.shutdown();
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public void setDatasetSink(final DatasetSink aSink) {
        aSink.process(this);
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public EntityManager<Node> getNodeManager() {
        return new DatasetNodeEntityManager(this.myDataSet);
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public EntityManager<Relation> getRelationManager() {
        return new DatasetRelationEntityManager(this.myDataSet);
    }

    /**
     * ${@inheritDoc}.
     */
    @Override
    public EntityManager<Way> getWayManager() {
        return new DatasetWayEntityManager(this.myDataSet);
    }

    @Override
    public DatasetContext createReader() {
        return this;
    }

}
