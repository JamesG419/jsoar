/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.antlr.stringtemplate.StringTemplate;
import org.jsoar.legilimens.LegilimensApplication;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.StringTools;
import org.restlet.Application;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;

/**
 * @author ray
 */
public class BaseAgentResource extends BaseResource
{
    protected String agentName;
    protected ThreadedAgent agent;
    
    /* (non-Javadoc)
     * @see org.restlet.resource.UniformResource#doInit()
     */
    @Override
    protected void doInit() throws ResourceException
    {
        super.doInit();
        
        agentName = getRequest().getAttributes().get("agentName").toString();
        agent = getLegilimens().getAgent(agentName);

        setExisting(agent != null);
    }

    /* (non-Javadoc)
     * @see org.jsoar.legilimens.BaseResource#setTemplateAttributes(java.util.Map)
     */
    @Override
    public void setTemplateAttributes(Map<String, Object> attrs)
    {
        super.setTemplateAttributes(attrs);
        attrs.put("agent", agent);
    }

    /* (non-Javadoc)
     * @see org.jsoar.legilimens.resources.BaseResource#html(java.lang.String)
     */
    @Override
    public Representation html(final String templateName)
    {
        // Override html to do rendering in agent thread
        final LegilimensApplication app = getLegilimens();
        final Callable<Representation> callable = new Callable<Representation>()
        {

            @Override
            public Representation call() throws Exception
            {
                Application.setCurrent(app);
                final StringTemplate t = template(templateName);
                return new StringRepresentation(t.toString(), MediaType.TEXT_HTML);
            }
        };
        
        try
        {
            return agent.executeAndWait(callable, 10, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            return new StringRepresentation(StringTools.getStackTrace(e), MediaType.TEXT_PLAIN);
        }
        catch (ExecutionException e)
        {
            e.printStackTrace();
            return new StringRepresentation(StringTools.getStackTrace(e), MediaType.TEXT_PLAIN);
        }
        catch (TimeoutException e)
        {
            return new StringRepresentation(StringTools.getStackTrace(e), MediaType.TEXT_PLAIN);
        }
    }    
    
    
    
}