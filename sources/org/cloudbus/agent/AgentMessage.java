package org.cloudbus.agent;

import org.cloudbus.cloudsim.core.CloudSim;

import java.util.Collection;
import java.util.Set;

public class AgentMessage {
    public Long ID;
    public String SOURCE;
    public Collection<String> DESTINATION;
    public double TIMESTAMP;

    public AgentMessage() {
        //This is necessary for dynamic agent instance creation.
    }

    public AgentMessage(Long id, String source, Set<String> destination) {
        ID = id;
        SOURCE = source;
        DESTINATION = destination;
        TIMESTAMP = CloudSim.clock();
    }

    public void setID(Long ID) {
        this.ID = ID;
    }

    public void setSOURCE(String SOURCE) {
        this.SOURCE = SOURCE;
    }

    public void setDESTINATION(Collection<String> DESTINATION) {
        this.DESTINATION = DESTINATION;
    }

    public Collection<String> getDESTINATION() {
        return DESTINATION;
    }

    public void setTIMESTAMP(double TIMESTAMP) {
        this.TIMESTAMP = TIMESTAMP;
    }

}
