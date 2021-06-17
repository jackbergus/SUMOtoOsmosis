package org.cloudbus.res.model.agent;

public interface Agent {
    void notifyUpdateState();
    void notifySend();
    void notifyAccept(AgentMessage message);
}
