/**
 * License: GPL. Copyright 2007 by Immanuel Scholz and others.
 */
package org.openstreetmap.osm.data.visitors;

import org.openstreetmap.osm.data.IDataSet;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * Visitor, that adds the visited object to the dataset given at constructor.
 *
 * Is not capable of adding keys.

 */
public class AddVisitor implements Visitor {

    /**
     * The DataSet to add everything we see to.
     */
    private final IDataSet myDataSet;

    /**
     * @param ds The DataSet to add everything we see to.
     */
    public AddVisitor(final IDataSet ds) {
        this.myDataSet = ds;
    }

    /**
     * @param n visit a node
     */
    public void visit(final Node n) {
        myDataSet.addNode(n);
    }

    /**
     * @param w visit a way
     */
    public void visit(final Way w) {
        myDataSet.addWay(w);
    }

    /**
     * @param r The Relation we visit.
     */
    public void visit(final Relation r) {
        myDataSet.addRelation(r);
    }

    /**
     * @return The DataSet to add everything we see to.
     */
    public IDataSet getDataSet() {
        return myDataSet;
    }
}
