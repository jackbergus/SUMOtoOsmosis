package org.cloudbus.agent;

import org.cloudbus.cloudsim.edge.iot.IoTDevice;
import org.cloudbus.osmosis.core.OsmesisDatacenter;

import java.util.*;

public class AgentBroker {
    //Singleton pattern
    private static AgentBroker singleton = new AgentBroker();

    public static AgentBroker getInstance() {
        return singleton;
    }

    //Communication topology for Osmotic Agents
    private Map<String, Collection<String>> linksTopology = new HashMap<>();

    //Agents Names2Agents
    //Osmotic Agents are uniquely recognized by name
    private Map<String,DCAgent> agentsDC = new HashMap<>();
    private Map<String,DeviceAgent> agentsDevices = new HashMap<>();

    //Agents classes
    private Class dcAgentClass;
    private Class deviceAgentClass;
    private Class agentMessageClass;

    public void setDcAgentClass(Class dcAgentClass) {
        this.dcAgentClass = dcAgentClass;
    }

    public void setDeviceAgentClass(Class deviceAgentClass) {
        this.deviceAgentClass = deviceAgentClass;
    }

    public void setAgentMessageClass(Class agentMessageClass) {
        this.agentMessageClass = agentMessageClass;
    }

    //Agents creation
    public void createDCAgent(String dcName, OsmesisDatacenter dc){
        if (dcAgentClass==null) return;
        try {
            DCAgent dcAgent = (DCAgent) dcAgentClass.newInstance();
            dcAgent.setOsmesisDatacenter(dc);
            dcAgent.setName(dcName);
            agentsDC.put(dcName,dcAgent);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void createDeviceAgent(String deviceName, IoTDevice device){
        if (deviceAgentClass==null) return;
        try {
            DeviceAgent deviceAgent = (DeviceAgent) deviceAgentClass.newInstance();
            deviceAgent.setIoTDevice(device);
            deviceAgent.setName(deviceName);
            agentsDevices.put(deviceName, deviceAgent);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public AgentMessage createEmptyMessage(){
        AgentMessage message = null;
        try {
            message = (AgentMessage) agentMessageClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return message;
    }

    //Osmotic Agents link topology
    private void addLink2Topology(String source, String destination){
        if (!linksTopology.containsKey(source)){
            linksTopology.put(source, new HashSet<String>());
        }

        if (!linksTopology.get(source).contains(destination)){
            linksTopology.get(source).add(destination);
        }
    }

    public void addAgentLink(String source, String destination){
        //links are bidirectional
        addLink2Topology(source,destination);
        addLink2Topology(destination,source);
    }

    public AbstractAgent getAgentByName(String name){
        if (agentsDC.containsKey(name)){
            return agentsDC.get(name);
        } else if (agentsDevices.containsKey(name)){
            return agentsDevices.get(name);
        } else return null;
    }

    public void distributeMessage(AgentMessage message){
        Collection<String> dst;
        if (message.getDESTINATION() == null){
            dst = linksTopology.get(message.SOURCE);
        } else {
            dst = message.getDESTINATION();
        }

        if (dst!=null){
            dst.forEach(target->getAgentByName(target).receiveMessage(message));
        }
    }

    private double timeIntervalMAPE;

    //MAPE loop interval
    public void setMAPEInterval(double time_interval){
        this.timeIntervalMAPE = time_interval;
    }

    private double lastMAPEloop=-1;

    public void executeMAPE(double clock){
        if (lastMAPEloop < 0){
            executeMAPE();
            lastMAPEloop = clock;
        } else if (lastMAPEloop + timeIntervalMAPE < clock){
            executeMAPE();
            lastMAPEloop = clock;
        }
    }

    public void executeMAPE(){
        //Monitor & Analyze
        for(Agent agent: agentsDC.values()){
            agent.monitor();
            agent.analyze();
        }
        for(Agent agent: agentsDevices.values()){
            agent.monitor();
            agent.analyze();
        }

        //Message passing between agents

        //Plan & Execute
        for(Agent agent: agentsDC.values()){
            agent.plan();
            agent.execute();
        }
        for(Agent agent: agentsDevices.values()){
            agent.plan();
            agent.execute();
        }
    }
}
