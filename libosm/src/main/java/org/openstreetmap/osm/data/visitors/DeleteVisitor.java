//License: GPL. Copyright 2007 by Immanuel Scholz and others
/**
 */
package org.openstreetmap.osm.data.visitors;

import org.openstreetmap.osm.data.IDataSet;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * Visitor, that removes the visited objects
 * from the dataset given to the constructor.
 *
 */
public class DeleteVisitor implements Visitor {

    /**
     * The dataset we empty.
     */
    private final IDataSet myMap;

    /**
     * @param pDS the dataset we empty
     */
    public DeleteVisitor(final IDataSet pDS) {
        if (pDS == null)
            throw new IllegalArgumentException("null map given!");
        this.myMap = pDS;
    }

    /**
     * @param n the Node we delete
     */
    public void visit(final Node n) {
        myMap.removeNode(n);
    }

    /**
     * @param w the Way we delete
     */
    public void visit(final Way w) {
        myMap.removeWay(w);
    }

    /**
     * @param r the Relation we delete
     */
    public void visit(final Relation r) {
        myMap.removeRelation(r);
    }

    /**
     * @return the map we operate on
     */
    public IDataSet getMap() {
        return myMap;
    }
}
