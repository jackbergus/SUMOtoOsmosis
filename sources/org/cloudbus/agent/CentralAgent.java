package org.cloudbus.agent;

import java.util.Queue;

public class CentralAgent {
    private static CentralAgent instance;
    protected Queue<AgentMessage> messages;
    void processMessages()
    {
        while(!messages.isEmpty())
        {
            AgentMessage agentMessage=messages.poll();
            agentMessage.DESTINATION.forEach(agent-> agent.notifyAccept(agentMessage));
        }
    }

    void acceptMessage(AgentMessage message)
    {
        messages.add(message);
    }

    private CentralAgent()
    {

    }

    public static CentralAgent getInstance()
    {
        if(instance==null)
            instance=new CentralAgent();
        return instance;
    }
}
