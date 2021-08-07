/*
 * Title:        IoTSim-Osmosis-RES 1.0
 * Description:  IoTSim-Osmosis-RES enables the testing and validation of osmotic computing applications
 * 			     over heterogeneous edge-cloud SDN-aware environments powered by the Renewable Energy Sources.
 *
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2021, Newcastle University (UK) and Saudi Electronic University (Saudi Arabia) and
 *                     AGH University of Science and Technology (Poland)
 *
 */

package org.cloudbus.cloudsim.osmesis.examples;

import org.cloudbus.agent.AgentMessage;
import org.cloudbus.agent.DeviceAgent;

import java.util.List;

public class RES_example3_DeviceAgent extends DeviceAgent {

    List<AgentMessage> messages;

    public RES_example3_DeviceAgent() {
        //This is necessary for dynamic agent instance creation.
    }

    @Override
    public void monitor() {
        super.monitor();

        //Do nothing.
    }

    @Override
    public void analyze() {
        super.analyze();

        //Do nothing.
    }

    @Override
    public void plan() {
        super.plan();

        //Get already received messages and analyze the data.
        //It clears the incoming buffer.
        messages = getReceivedMessages();

    }

    @Override
    public void execute() {
        super.execute();

        //Enforce the final adaptability actions e.g. add routing rules using
        //getIoTDevice().getRoutingTable().addRule(...);

    }
}
