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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RES_example6_DeviceAgent extends DeviceAgent {

    List<AgentMessage> messages;

    Map<String, String> tempRoutingTable = new HashMap<>();
    Map<String, Double> tempBestRatio = new HashMap<>();

    public RES_example6_DeviceAgent() {
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

    private boolean isInstance(String name){
        boolean result = name.matches("^\\S*.[0-9]+$");
        return result;
    }

    private String getAbstract(String name){
        return name.replaceAll(".[0-9]+$", ".*");
    }

    private void updateTempRoutingTable(String abs_mel, String inst_mel, double res){
        if (!tempRoutingTable.containsKey(abs_mel)){
            tempRoutingTable.put(abs_mel,inst_mel);
            tempBestRatio.put(abs_mel,res);
        }

        if (tempBestRatio.get(abs_mel)<res){
            tempBestRatio.put(abs_mel,res);
            tempRoutingTable.put(abs_mel,inst_mel);
        }
    }

    @Override
    public void plan() {
        super.plan();

        //Get already received messages and analyze the data.
        //It clears the incoming buffer.
        messages = getReceivedMessages();

        tempRoutingTable.clear();
        tempBestRatio.clear();

        //choose the MEL instance from Edge with highest RES value
        for(AgentMessage message:messages){
            RES_example6_AgentMessage ex4_message = (RES_example6_AgentMessage) message;

            double res = ex4_message.getGreenEnergyRatio();
            for(String mel_name:ex4_message.getAvailableMELs()){
                if (isInstance(mel_name)){
                    updateTempRoutingTable(getAbstract(mel_name), mel_name, res);
                }
            }
        }
    }

    @Override
    public void execute() {
        super.execute();

        //Modify the routing to use the selected MEL instances
        for (String mel:tempRoutingTable.keySet()){
            getIoTDevice().getRoutingTable().addRule(mel,tempRoutingTable.get(mel));
        }
    }
}
