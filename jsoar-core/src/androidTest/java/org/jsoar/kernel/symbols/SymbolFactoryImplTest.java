/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 30, 2008
 */
package org.jsoar.kernel.symbols;


import android.test.AndroidTestCase;

import junit.framework.Assert;

import org.jsoar.util.ByRef;

import java.io.File;
import java.util.List;

/**
 * @author ray
 */
public class SymbolFactoryImplTest extends AndroidTestCase
{
    private SymbolFactoryImpl syms;
    
    /**
     * @throws java.lang.Exception
     */
    @Override
    public void setUp() throws Exception
    {
        syms = new SymbolFactoryImpl();
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    public void tearDown() throws Exception
    {
        syms = null;
    }
    
    public void testMakeNewIdentifier()
    {
        IdentifierImpl s = syms.make_new_identifier('s', (short) 1);
        assertNotNull(s);
        assertEquals('S', s.getNameLetter());
        assertEquals(1, s.getNameNumber());
        assertEquals(1, s.level);
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.findIdentifier(s.getNameLetter(), s.getNameNumber()));
        
        // Make another id and make sure the id increments
        s = syms.make_new_identifier('s', (short) 4);
        assertNotNull(s);
        assertEquals('S', s.getNameLetter());
        assertEquals(2, s.getNameNumber());
        assertEquals(4, s.level);
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.findIdentifier(s.getNameLetter(), s.getNameNumber()));
    }

    public void testMakeFloatConstant()
    {
        DoubleSymbolImpl s = syms.createDouble(3.14);
        assertNotNull(s);
        assertEquals(3.14, s.getValue(), 0.0001);
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.findDouble(s.getValue()));
        assertSame(s, syms.createDouble(s.getValue()));
    }

    public void testMakeIntConstant()
    {
        IntegerSymbolImpl s = syms.createInteger(99);
        assertNotNull(s);
        assertEquals(99, s.getValue());
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.findInteger(s.getValue()));
        assertSame(s, syms.createInteger(s.getValue()));
    }
    
    public void testMakeLargeIntConstant()
    {
        IntegerSymbolImpl s = syms.createInteger(999999999999L);
        assertNotNull(s);
        assertEquals(999999999999L, s.getValue());
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.findInteger(s.getValue()));
        assertSame(s, syms.createInteger(s.getValue()));
    }
    
    
    public void testMakeNewSymConstant()
    {
        StringSymbolImpl s = syms.createString("A sym constant");
        assertNotNull(s);
        assertEquals("A sym constant", s.getValue());
        assertFalse(s.hash_id == 0);
        assertSame(s, syms.findString(s.getValue()));
        assertSame(s, syms.createString(s.getValue()));
    }
    
    public void testGenerateNewSymConstant()
    {
        StringSymbolImpl a0 = syms.createString("A0");
        StringSymbolImpl a1 = syms.createString("A1");
        
        ByRef<Integer> number = ByRef.create(0);
        StringSymbol a2 = syms.generateUniqueString("A", number);
        assertNotNull(a2);
        assertNotSame(a0, a2);
        assertNotSame(a1, a2);
        assertEquals("A2", a2.getValue());
        assertEquals(3, number.value.intValue());
    }
    
    public void testCreateJavaSymbol()
    {
        File f = new File(System.getProperty("user.dir"));
        JavaSymbol js = syms.findJavaSymbol(f);
        assertNull(js);
        js = syms.createJavaSymbol(f);
        assertNotNull(js);
        assertEquals(f, js.getValue());
    }
    
    public void testNullJavaSymbol()
    {
        JavaSymbol js = syms.findJavaSymbol(null);
        assertNotNull(js);
        assertNull(js.getValue());
    }
    
    public void testGarbageCollectedSymbolsAreRemovedFromCache()
    {
        //This needs to be larger than twice the base capacity of the map -ACNickels
        final int iterations = 1000;
        for(int i = 0; i < iterations; ++i)
        {
            assertNotNull(syms.createInteger(i));
            assertNotNull(syms.createString(Integer.toString(i)));
        }
        // Why do I believe this test works? Because it fails if I remove the
        // call to the garbage collector here :) -Unknown

        // Well, I managed to make this work again, and did indeed confirm that
        // symbols will be garbage collected, but now the test is even uglier.
        //There are three cycles here.  The first writes,  the the second
        //does several reads on the orphaned soft references, and the third
        //confirms that at least some were cleaned up.  -ACNickels
        System.gc();
        for(int i = 0; i < iterations; ++i)
        {
            //The more iterations the better.  Each read has a chance to free an
            //orphaned soft reference
            for(int j = 0; j < 1000; j++) {
                syms.findInteger(i);
                syms.findString(Integer.toString(i));
            }
        }
        System.gc();
        //Check that at least some symbols are collected
        Assert.assertTrue(syms.getAllSymbols().size() < iterations);
    }
    
    public void testGetStringSymbols()
    {
        final StringSymbolImpl a = syms.createString("a");
        final StringSymbolImpl b = syms.createString("b");
        
        final List<StringSymbol> strings = syms.getSymbols(StringSymbol.class);
        assertNotNull(strings);
        assertEquals(2, strings.size());
        assertTrue(strings.contains(a));
        assertTrue(strings.contains(b));
    }
    
    public void testGetIntegerSymbols()
    {
        final IntegerSymbolImpl a = syms.createInteger(2);
        final IntegerSymbolImpl b = syms.createInteger(3);
        
        final List<IntegerSymbol> values = syms.getSymbols(IntegerSymbol.class);
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(a));
        assertTrue(values.contains(b));
    }
    public void testGetDoubleSymbols()
    {
        final DoubleSymbolImpl a = syms.createDouble(2.2);
        final DoubleSymbolImpl b = syms.createDouble(3.3);
        
        final List<DoubleSymbol> values = syms.getSymbols(DoubleSymbol.class);
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(a));
        assertTrue(values.contains(b));
    }
    public void testGetVariableSymbols()
    {
        final Variable a = syms.make_variable("a");
        final Variable b = syms.make_variable("b");
        
        final List<Variable> values = syms.getSymbols(Variable.class);
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(a));
        assertTrue(values.contains(b));
    }
    public void testGetJavaSymbols()
    {
        final JavaSymbolImpl a = syms.createJavaSymbol(new File("hi"));
        final JavaSymbolImpl b = syms.createJavaSymbol(new File("bye"));
        final JavaSymbolImpl n = syms.createJavaSymbol(null);
        
        final List<JavaSymbol> values = syms.getSymbols(JavaSymbol.class);
        assertNotNull(values);
        assertEquals(3, values.size());
        assertTrue(values.contains(a));
        assertTrue(values.contains(b));
        assertTrue(values.contains(n));
    }
    public void testGetIdentifierSymbols()
    {
        final IdentifierImpl a = syms.createIdentifier('s');
        final IdentifierImpl b = syms.createIdentifier('i');
        
        final List<Identifier> values = syms.getSymbols(Identifier.class);
        assertNotNull(values);
        assertEquals(2, values.size());
        assertTrue(values.contains(a));
        assertTrue(values.contains(b));
    }
    
    public void testImportReturnsInputUnchangedIfItsAlreadyOwnedByFactory()
    {
        final IntegerSymbol s = syms.createInteger(99);
        assertSame(s, syms.importSymbol(s));
    }
    
    public void testImportThrowsAnExceptionForIdentifiers()
    {
        final Identifier id = syms.createIdentifier('T');
        try {
            syms.importSymbol(id);
            Assert.fail("Should have thrown");
        }catch(IllegalArgumentException e){
            Assert.assertEquals("Tried to import identifier T1 into symbol factory.", e.getMessage());
        }
    }
    
    public void testImportThrowsAnExceptionForVariables()
    {
        final Variable id = syms.make_variable("foo");
        try {
            syms.importSymbol(id);
            Assert.fail("Should have thrown");
        }catch(IllegalArgumentException e){
            Assert.assertEquals("Tried to import variable foo into symbol factory.", e.getMessage());
        }
    }
    
    public void testCanImportStringsAcrossFactories()
    {
        final SymbolFactory other = new SymbolFactoryImpl();
        final StringSymbol i = syms.createString("test");
        final Symbol s = other.importSymbol(i);
        assertNotSame(i, s);
        assertEquals(i.getValue(), s.asString().getValue());
    }
    
    public void testCanImportIntegersAcrossFactories()
    {
        final SymbolFactory other = new SymbolFactoryImpl();
        final IntegerSymbol i = syms.createInteger(12345);
        final Symbol s = other.importSymbol(i);
        assertNotSame(i, s);
        assertEquals(i.getValue(), s.asInteger().getValue());
    }
    
    public void testCanImportDoublesAcrossFactories()
    {
        final SymbolFactory other = new SymbolFactoryImpl();
        final DoubleSymbol i = syms.createDouble(12345.9);
        final Symbol s = other.importSymbol(i);
        assertNotSame(i, s);
        assertEquals(i.getValue(), s.asDouble().getValue(), 0.000001);
    }
    
    public void testCanImportJavaSymbolsAcrossFactories()
    {
        final SymbolFactory other = new SymbolFactoryImpl();
        final JavaSymbol i = syms.createJavaSymbol(new File("."));
        final Symbol s = other.importSymbol(i);
        assertNotSame(i, s);
        assertSame(i.getValue(), s.asJava().getValue());
    }
}