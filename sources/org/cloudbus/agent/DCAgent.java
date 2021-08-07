package org.cloudbus.agent;

import org.cloudbus.osmosis.core.OsmesisDatacenter;

public class DCAgent extends AbstractAgent {
    protected OsmesisDatacenter osmesisDatacenter;

    public DCAgent(OsmesisDatacenter osmesisDatacenter) {
        this.osmesisDatacenter = osmesisDatacenter;
    }

    public DCAgent() {
        //This is necessary for dynamic agent instance creation.
    }

    public void setOsmesisDatacenter(OsmesisDatacenter osmesisDatacenter) {
        this.osmesisDatacenter = osmesisDatacenter;
    }

    @Override
    public void monitor() {

    }

    @Override
    public void analyze() {

    }

    @Override
    public void plan() {

    }

    @Override
    public void execute() {

    }
}
