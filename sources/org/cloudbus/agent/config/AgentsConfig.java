package org.cloudbus.agent.config;

import java.util.Collection;

public class AgentsConfig {
    public String DCAgentClassName;
    public String DeviceAgentClassName;
    public String CentralAgentClassName;
    public String AgentMessageClassName;
    public double MAPEInterval;
    public Collection<TopologyLink> TopologyLinks;
}
