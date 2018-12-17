/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 3, 2008
 */
package org.jsoar.kernel.memory;

import android.test.AndroidTestCase;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.memory.WmeSupportInfo.Support;
import org.jsoar.kernel.symbols.Identifier;

import java.util.List;

/**
 * @author ray
 */
public class WmeSupportInfoTest extends AndroidTestCase
{
    private Agent agent;
    
    @Override
    public void setUp()
    {
        agent = new Agent(getContext());
    }
    
    @Override
    public void tearDown()
    {
    }
    
    /**
     * Test method for {@link org.jsoar.kernel.memory.WmeSupportInfo#get(org.jsoar.kernel.Agent, org.jsoar.kernel.memory.Wme)}.
     */
    public void testGet() throws Exception
    {
        ProductionManager pm = agent.getProductions();
        
        // Some productions (courtesy of Bob M.) that create two preferences 
        // for the same WME, one with i-support and one with o-support.
        pm.loadProduction("i-support\n" +
            "(state <s> ^superstate nil ^io <io>)\n" +
            "-->\n" +
            "(<s> ^foo bar)");

        pm.loadProduction("propose\n" +
            "(state <s> ^superstate nil)\n" +
            "-->\n" +
            "(<s> ^operator <o> +)");

        pm.loadProduction("o-support\n" +
            "(state <s> ^operator <o> ^io <io>)\n" +
            "-->\n" +
            "(<s> ^foo bar)");

        agent.runFor(2, RunType.DECISIONS);
        
        final Identifier s1 = agent.getSymbols().findIdentifier('S', 1);
        assertNotNull(s1);

        final Wme foo = Wmes.matcher(agent).attr("foo").value("bar").find(s1);
        assertNotNull(foo);
        
        WmeSupportInfo info = WmeSupportInfo.get(agent, foo);
        assertNotNull(info);
        assertSame(foo, info.getWme());
        List<Support> supports = info.getSupports();
        assertNotNull(supports);
        assertEquals(2, supports.size());
        Support a = supports.get(0);
        assertEquals("o-support", a.getSource().getName().toString());
        assertTrue(a.isOSupported());
        Support b = supports.get(1);
        assertEquals("i-support", b.getSource().getName().toString());
        assertFalse(b.isOSupported());
    }

}