package org.cloudbus.agent;

import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class AbstractAgent implements Agent{
    private List<AgentMessage> inQueue = new ArrayList<>();

    private String name;

    private long mID=0;

     public AgentMessage newAgentMessage(){
        AgentMessage message = AgentBroker.getInstance().createEmptyMessage();
        message.setID(mID);
        message.setTIMESTAMP(CloudSim.clock());
        message.setSOURCE(name);
        mID++;
        return message;
    }

    public void publishMessage(AgentMessage message){
        //Publish message is managed by the Osmotic Broker.
        AgentBroker.getInstance().distributeMessage(message);
    }

    public List<AgentMessage> getReceivedMessages(){
        List<AgentMessage> messages = new ArrayList<>();
        inQueue.forEach(msg -> messages.add(msg));
        inQueue.clear();
        return messages;
    }

    public void receiveMessage(AgentMessage message){
        inQueue.add(message);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
