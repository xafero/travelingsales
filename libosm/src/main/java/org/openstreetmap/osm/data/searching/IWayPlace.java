package org.openstreetmap.osm.data.searching;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

public interface IWayPlace {

    /**
     * @return The node to return.
     */
    public abstract Node getResult();

    /**
     * @return Returns the way.
     * @see #myWay
     */
    public abstract Way getWay();

}
