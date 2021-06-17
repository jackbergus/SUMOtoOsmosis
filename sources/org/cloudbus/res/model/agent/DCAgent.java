package org.cloudbus.res.model.agent;

import org.cloudbus.osmosis.core.OsmesisDatacenter;

public class DCAgent extends AbstractAgent {
    private final OsmesisDatacenter osmesisDatacenter;
    private MELRoutingPolicy melRoutingPolicy;

    public DCAgent(OsmesisDatacenter osmesisDatacenter, MELRoutingPolicy melRoutingPolicy) {
        this.osmesisDatacenter = osmesisDatacenter;
        this.melRoutingPolicy = melRoutingPolicy;
    }

    @Override
    public void notifyUpdateState() {
        //check osmesisDC state and create a message if necessary
    }

    public void setMelRoutingPolicy(MELRoutingPolicy melRoutingPolicy) {
        this.melRoutingPolicy = melRoutingPolicy;
    }
}
