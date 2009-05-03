/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 14, 2008
 */
package org.jsoar.kernel.memory;


import static org.junit.Assert.*;

import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.io.CycleCountInput;
import org.jsoar.kernel.memory.Wmes.MatcherBuilder;
import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.tracing.Trace.Category;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class RecognitionMemoryTest
{
    private Agent agent;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        this.agent.initialize();
    }
    
    @Test
    public void testRhsFunctionThatCreatesStructure() throws Exception
    {
        new CycleCountInput(agent.getInputOutput(), agent.getEventManager());
        
        // Add a RHS function that puts a structure in working memory
        RhsFunctionHandler func = new AbstractRhsFunctionHandler("rhs-structure", 0, 0) {

            @Override
            public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
            {
                Identifier r1 = context.getSymbols().createIdentifier('R');
                
                final SymbolFactory syms = context.getSymbols();
                context.addWme(r1, syms.createString("x"), syms.createInteger(1));
                context.addWme(r1, syms.createString("y"), syms.createInteger(2));
                context.addWme(r1, syms.createString("z"), syms.createInteger(3));
                context.addWme(r1, syms.createString("name"), syms.createString("hello"));
                
                return r1;
            }};
            
        agent.getRhsFunctions().registerHandler(func);
        agent.getTrace().setEnabled(Category.WM_CHANGES, true);
        agent.getProperties().set(SoarProperties.WAITSNC, true);
        agent.getProductions().loadProduction("" +
        		"testRhsFunctionThatCreatesStructure\n" +
        		"(state <s> ^superstate nil ^io.input-link.cycle-count 1)\n" +
        		"-->" +
        		"(<s> ^result (rhs-structure))");
        
        agent.runFor(1, RunType.DECISIONS);
        
        // Verify that the x, y, z, name structure is there
        final Identifier s1 = agent.getSymbols().findIdentifier('S', 1);
        final MatcherBuilder m = Wmes.matcher(agent);
        Wme result = m.attr("result").find(s1);
        assertNotNull(result);
        final Identifier r1 = result.getValue().asIdentifier();
        
        Wme x, y, z, name;
        assertNotNull(x = m.reset().attr("x").value(1).find(r1));
        assertNotNull(y = m.reset().attr("y").value(2).find(r1));
        assertNotNull(z = m.reset().attr("z").value(3).find(r1));
        assertNotNull(name = m.reset().attr("name").value("hello").find(r1));
        
        // Step again and verify that all the WMEs are retracted because of the test on 
        // cycle-count
        agent.runFor(1, RunType.DECISIONS);
        
        assertNull(m.reset().attr("result").find(s1));
        assertFalse(agent.getAllWmesInRete().contains(x));
        assertFalse(agent.getAllWmesInRete().contains(y));
        assertFalse(agent.getAllWmesInRete().contains(z));
        assertFalse(agent.getAllWmesInRete().contains(name));
    }

}