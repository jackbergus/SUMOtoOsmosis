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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RES_example3_AgentMessage extends AgentMessage {
    List<String> availableMELs = new ArrayList<>();

    public RES_example3_AgentMessage() {
        //This is necessary for dynamic agent instance creation.
    }

    public List<String> getAvailableMELs() {
        return availableMELs;
    }

    public void addMEL(String mel){
        availableMELs.add(mel);
    }

    public RES_example3_AgentMessage(Long id, String source, Set<String> destination) {
        super(id, source, destination);
    }
}
