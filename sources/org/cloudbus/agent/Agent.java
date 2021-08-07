package org.cloudbus.agent;

import java.util.List;

public interface Agent {

    String getName();

    void receiveMessage(AgentMessage message);

    void monitor();
    void analyze();
    void plan();
    void execute();
}
