package org.cloudbus.agent;

public interface Agent {
    void notifyUpdateState();
    void notifySend();
    void notifyAccept(AgentMessage message);
}
