package org.cloudbus.agent.config;

import java.util.Collection;

public class AgentConfigProvider {
    private final AgentsConfig agentsConfig;

    public AgentConfigProvider(AgentsConfig agentsConfig) {
        this.agentsConfig = agentsConfig;
    }

    public Class<?> getDCAgentClass() throws ClassNotFoundException {
        return Class.forName(agentsConfig.DCAgentClassName);
    }

    public Class<?> getDeviceAgentClass() throws ClassNotFoundException {
        return Class.forName(agentsConfig.DeviceAgentClassName);
    }

    public Class<?> getCentralAgentClass() throws ClassNotFoundException {
        return Class.forName(agentsConfig.CentralAgentClassName);
    }

    public Class<?> getAgentMessageClass() throws ClassNotFoundException {
        return Class.forName(agentsConfig.AgentMessageClassName);
    }

    public Collection<TopologyLink> getTopologyLinks(){
        return agentsConfig.TopologyLinks;
    }
}
