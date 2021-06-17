package org.cloudbus.res.model.agent;

import java.util.ArrayList;
import java.util.Collection;

public abstract class AbstractAgent implements Agent{
    public static CentralAgent centralAgent= CentralAgent.getInstance();
    protected Collection<AgentMessage> generatedMessages= new ArrayList<>();

    @Override
    public void notifySend() {
        generatedMessages.forEach(message -> centralAgent.acceptMessage(message));
        generatedMessages.clear();
    }

    @Override
    public void notifyAccept(AgentMessage message) {
        generatedMessages.add(message);
    }
}
