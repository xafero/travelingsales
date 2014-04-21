package org.openstreetmap.osm.data.visitors;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * Implementation of the visitor scheme.
 * Every OsmPrimitive can be visited by
 * several different visitors.
 */
public interface Visitor {

    /**
     * @param n the Node we visit
     */
    void visit(Node n);

    /**
     * @param w The Way we visit.
     */
    void visit(Way w);

    /**
     * @param r The Relation we visit.
     */
    void visit(Relation r);

}
