package org.cloudbus.res.model.agent;

import java.util.Set;

public class AgentMessage {
    public final Long ID;
    public final Agent SOURCE;
    public final Set<Agent> DESTINATION;

    public AgentMessage(Long id, Agent source, Set<Agent> destination) {
        ID = id;
        SOURCE = source;
        DESTINATION = destination;
    }
}
