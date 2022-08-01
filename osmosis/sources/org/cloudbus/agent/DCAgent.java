package org.cloudbus.agent;

import org.cloudbus.osmosis.core.OsmesisDatacenter;
import org.cloudbus.res.EnergyController;

public class DCAgent extends AbstractAgent {
    protected OsmesisDatacenter osmesisDatacenter;
    protected EnergyController energyController;

    public DCAgent(OsmesisDatacenter osmesisDatacenter) {
        this.osmesisDatacenter = osmesisDatacenter;
    }

    public DCAgent() {
        //This is necessary for dynamic agent instance creation.
    }

    public void setOsmesisDatacenter(OsmesisDatacenter osmesisDatacenter) {
        this.osmesisDatacenter = osmesisDatacenter;
    }

    public OsmesisDatacenter getOsmesisDatacenter() {
        return osmesisDatacenter;
    }

    public void setEnergyController(EnergyController energyController) {
        this.energyController = energyController;
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
