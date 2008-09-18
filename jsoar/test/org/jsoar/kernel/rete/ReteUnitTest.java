/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2008
 */
package org.jsoar.kernel.rete;


import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.Trace;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.parser.Parser;
import org.jsoar.kernel.symbols.Identifier;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class ReteUnitTest extends JSoarTest
{
    private Rete rete;
    private Listener listener;
    
    private class Listener implements ReteListener
    {
        Set<Production> matching = new HashSet<Production>();
        
        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#finishRefraction(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.Production, org.jsoar.kernel.rete.Instantiation, org.jsoar.kernel.rete.ReteNode)
         */
        @Override
        public boolean finishRefraction(Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node)
        {
            return false;
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#p_node_left_addition(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode, org.jsoar.kernel.rete.Token, org.jsoar.kernel.Wme)
         */
        @Override
        public void p_node_left_addition(Rete rete, ReteNode node, Token tok, Wme w)
        {
            matching.add(node.b_p.prod);
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#p_node_left_removal(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode, org.jsoar.kernel.rete.Token, org.jsoar.kernel.Wme)
         */
        @Override
        public void p_node_left_removal(Rete rete, ReteNode node, Token tok, Wme w)
        {
            matching.remove(node.b_p.prod);
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#startRefraction(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.Production, org.jsoar.kernel.rete.Instantiation, org.jsoar.kernel.rete.ReteNode)
         */
        @Override
        public void startRefraction(Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node)
        {
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rete.ReteListener#removingProductionNode(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode)
         */
        @Override
        public void removingProductionNode(Rete rete, ReteNode p_node)
        {
        }
        
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.JSoarTest#setUp()
     */
    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        
        this.listener = new Listener();
        this.rete = new Rete(Trace.createStdOutTrace().enableAll(), varGen);
        this.rete.setReteListener(listener);
    }

    @Test
    public void testInitDummyTopNode() throws Exception
    {
        assertNotNull(rete.dummy_top_node);
        assertEquals(ReteNodeType.DUMMY_TOP_BNODE, rete.dummy_top_node.node_type);
        RightToken dummyTopToken = (RightToken) rete.dummy_top_node.a_np.tokens.first.get();
        assertNotNull(dummyTopToken);
        assertNull(dummyTopToken.parent);
        assertSame(rete.dummy_top_node, dummyTopToken.node);
    }

    @Test
    public void testAddProductionToRete() throws Exception
    {
        Parser parser = createParser(
           "testAddProductionToRete \n" +
           "(<root> ^integer 1 \n" +
           "        ^float 3.14 \n" +
           "        ^string |S| \n" +
           "        ^id <id>) \n" +
           "--> \n" +
           "(write <root>)");
        
        Production p = parser.parse_production();
        assertNotNull(p);
        
        ProductionAddResult result = rete.add_production_to_rete(p);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);

        // TODO: Test structure of built rete
    }
    
    @Test
    public void testSimpleAddWmeToRete() throws Exception
    {
        Parser parser = createParser(
           "testAddProductionToRete \n" +
           "(<root> ^integer 1 ^float 3.14 ^string |S| ^id <id>)" +
           "--> \n" +
           "(write <root>)");
        
        Production p = parser.parse_production();
        assertNotNull(p);
        
        ProductionAddResult result = rete.add_production_to_rete(p);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
        
        // Add WMEs one at a time and make sure there isn't a match until the last WME is added
        Identifier root = syms.make_new_identifier('R', (short) 0);
        Wme intWme = new Wme(root, syms.make_sym_constant("integer"), syms.make_int_constant(1), false, 0);
        rete.add_wme_to_rete(intWme);
        assertFalse(listener.matching.contains(p));
        
        Wme floatWme = new Wme(root, syms.make_sym_constant("float"), syms.make_float_constant(3.14), false, 0);
        rete.add_wme_to_rete(floatWme);
        assertFalse(listener.matching.contains(p));
        
        Wme stringWme = new Wme(root, syms.make_sym_constant("string"), syms.make_sym_constant("S"), false, 0);
        rete.add_wme_to_rete(stringWme);
        assertFalse(listener.matching.contains(p));
       
        Wme idWme = new Wme(root, syms.make_sym_constant("id"), syms.make_new_identifier('i', (short) 0), false, 0);
        rete.add_wme_to_rete(idWme);
        assertTrue(listener.matching.contains(p));
        
        // Remove int WME to verify the production unmatches
        rete.remove_wme_from_rete(intWme);
        assertFalse(listener.matching.contains(p));
       
        // Re-add it to verify it re-matches
        rete.add_wme_to_rete(intWme);
        assertTrue(listener.matching.contains(p));
        
        // Remove rest of WMEs to verify the production doesn't match again.
        rete.remove_wme_from_rete(floatWme);
        assertFalse(listener.matching.contains(p));
        rete.remove_wme_from_rete(idWme);
        assertFalse(listener.matching.contains(p));
        rete.remove_wme_from_rete(stringWme);
        assertFalse(listener.matching.contains(p));
        
        rete.remove_wme_from_rete(intWme);
        assertFalse(listener.matching.contains(p));
    }
    
    @Test
    public void testReteWithNegatedConjunctiveCondition() throws Exception
    {
        Parser parser = createParser(
           "testReteWithNegatedConjunctiveCondition \n" +
           "(<root> ^integer 1 ^float 3.14)\n" +
           "-{ (<root> ^string |S|) " +
           "   (<root> ^id <id>)}\n" +
           "--> \n" +
           "(write <root>)");
        
        Production p = parser.parse_production();
        assertNotNull(p);
        
        ProductionAddResult result = rete.add_production_to_rete(p);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
        
        Identifier root = syms.make_new_identifier('R', (short) 0);
        Wme intWme = new Wme(root, syms.make_sym_constant("integer"), syms.make_int_constant(1), false, 0);
        rete.add_wme_to_rete(intWme);
        assertFalse(listener.matching.contains(p));
        
        Wme floatWme = new Wme(root, syms.make_sym_constant("float"), syms.make_float_constant(3.14), false, 0);
        rete.add_wme_to_rete(floatWme);
        
        // At this point, the production should match because the negated condition is false
        assertTrue(listener.matching.contains(p));
        
        Wme stringWme = new Wme(root, syms.make_sym_constant("string"), syms.make_sym_constant("S"), false, 0);
        rete.add_wme_to_rete(stringWme);
        
        // NCC stays false when the string is added
        assertTrue(listener.matching.contains(p));
       
        Wme idWme = new Wme(root, syms.make_sym_constant("id"), syms.make_new_identifier('i', (short) 0), false, 0);
        rete.add_wme_to_rete(idWme);
        
        // Addint this WME makes the NCC true, so the production unmatches
        assertFalse(listener.matching.contains(p));
        
        // Remove int WME to verify the production re-matches
        rete.remove_wme_from_rete(stringWme);
        assertTrue(listener.matching.contains(p));
       
        // Re-add it to verify it unmatches
        rete.add_wme_to_rete(stringWme);
        assertFalse(listener.matching.contains(p));
    }
    
    @Test
    public void testSimpleReteTests() throws Exception
    {
        Parser parser = createParser(
           "testAddProductionToRete \n" +
           "(<root> ^integer  < 2 ^float >= 3.14 ^string << T UV S >> ^id <id>)" +
           "--> \n" +
           "(write <root>)");
        
        Production p = parser.parse_production();
        assertNotNull(p);
        
        ProductionAddResult result = rete.add_production_to_rete(p);
        assertNotNull(result);
        assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
        
        // Add WMEs one at a time and make sure there isn't a match until the last WME is added
        Identifier root = syms.make_new_identifier('R', (short) 0);
        Wme intWme = new Wme(root, syms.make_sym_constant("integer"), syms.make_int_constant(1), false, 0);
        rete.add_wme_to_rete(intWme);
        assertFalse(listener.matching.contains(p));
        
        Wme floatWme = new Wme(root, syms.make_sym_constant("float"), syms.make_float_constant(3.14), false, 0);
        rete.add_wme_to_rete(floatWme);
        assertFalse(listener.matching.contains(p));
        
        Wme stringWme = new Wme(root, syms.make_sym_constant("string"), syms.make_sym_constant("S"), false, 0);
        rete.add_wme_to_rete(stringWme);
        assertFalse(listener.matching.contains(p));
       
        Wme idWme = new Wme(root, syms.make_sym_constant("id"), syms.make_new_identifier('i', (short) 0), false, 0);
        rete.add_wme_to_rete(idWme);
        assertTrue(listener.matching.contains(p));
        
        // Remove int WME to verify the production unmatches
        rete.remove_wme_from_rete(intWme);
        assertFalse(listener.matching.contains(p));
       
        // Re-add it to verify it re-matches
        rete.add_wme_to_rete(intWme);
        assertTrue(listener.matching.contains(p));
        
        // Remove rest of WMEs to verify the production doesn't match again.
        rete.remove_wme_from_rete(floatWme);
        assertFalse(listener.matching.contains(p));
        rete.remove_wme_from_rete(idWme);
        assertFalse(listener.matching.contains(p));
        rete.remove_wme_from_rete(stringWme);
        assertFalse(listener.matching.contains(p));
        
        rete.remove_wme_from_rete(intWme);
        assertFalse(listener.matching.contains(p));
    }

    @Test
    public void testAddProductionSimpleMakeAction() throws Exception
    {
        Parser parser = createParser(
                "testAddProductionSimpleMakeAction \n" +
                "(state <s> ^superstate nil)" +
                "--> \n" +
                "(<s> ^value 1)");
             
         Production p = parser.parse_production();
         assertNotNull(p);
         
         ProductionAddResult result = rete.add_production_to_rete(p);
         assertNotNull(result);
         assertEquals(ProductionAddResult.NO_REFRACTED_INST, result);
    }
}
