/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2009
 */
package org.jsoar.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoar.kernel.Agent;
import org.jsoar.util.events.SoarEventManager;

import com.google.common.collect.MapMaker;

/**
 * Helper class that deals with managing which threaded agents are attached to which
 * {@link Agent} instances.
 * 
 * @author ray
 */
enum ThreadedAgentManager
{
    INSTANCE;
    
    private static final Log logger = LogFactory.getLog(ThreadedAgentManager.class);
    
    private final Map<Agent, ThreadedAgent> agents = new MapMaker().weakKeys().makeMap();
    private final SoarEventManager events = new SoarEventManager();
    
    public ThreadedAgent create()
    {
        synchronized(agents)
        {
            final ThreadedAgent agent = attach(new Agent()).initialize(new CompletionHandler<Void>() {

                @Override
                public void finish(Void result)
                {
                    synchronized(agents)
                    {
                        agents.notify();
                    }
                }});
            try
            {
                agents.wait();
            }
            catch (InterruptedException e)
            {
                logger.error("Interrupted waiting for new ThreadedAgent to initialize.", e);
                Thread.currentThread().interrupt(); // reset interrupt
            }
            
            return agent;
        }
    }
    
    public ThreadedAgent find(Agent agent)
    {
        synchronized (agents)
        {
            return agents.get(agent);
        }
    }
    
    public List<ThreadedAgent> getAll()
    {
        synchronized (agents)
        {
            return new ArrayList<ThreadedAgent>(agents.values());
        }
    }
    
    public ThreadedAgent attach(Agent agent) 
    {
        synchronized(agents)
        {
            ThreadedAgent ta = agents.get(agent);
            if(ta == null)
            {
                ta = new ThreadedAgent(agent);
                agents.put(agent, ta);
                events.fireEvent(new ThreadedAgentAttachedEvent(ta));
            }
            return ta;
        }
    }
    
    public void detach(ThreadedAgent agent)
    {
        synchronized(agents)
        {
            agents.remove(agent);
            events.fireEvent(new ThreadedAgentDetachedEvent(agent));
        }
    }

    /**
     * @return the events
     */
    public SoarEventManager getEventManager()
    {
        return events;
    }
}