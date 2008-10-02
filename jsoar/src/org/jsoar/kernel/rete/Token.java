/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.util.ListHead;

/**
 * 
 * rete.cpp:590
 * 
 * @author ray
 */
public class Token
{
    /* --- Note: "parent" is NIL on negative node negrm (local join result) 
    tokens, non-NIL on all other tokens including CN and CN_P stuff.
    I put "parent" at offset 0 in the structure, so that upward scans
    are fast (saves doing an extra integer addition in the inner loop) --- */
    public final Token parent;
    ReteNode node;
    public final Wme w;
    
    Token first_child;// head of dll of childen
    Token next_sibling;
    private Token previous_sibling;
    
    Token next_from_wme;
    private Token previous_from_wme; // Part of dll from wme, tree-based remove
    Token next_of_node;
    private Token previous_of_node;// Part of dll of tokens at node
    
    final ListHead<RightToken> negrm_tokens = ListHead.newInstance(); /* join results: for Neg, CN nodes only */
    
    public Token(ReteNode current_node, Token parent_tok, Wme parent_wme)
    {
        assert current_node != null;
        
        this.node = current_node;
        this.parent = parent_tok;
        this.w = parent_wme;
        
        addToNode(node.a_np, this);
        addToParent(parent, this);
        addToWme(w, this);
    }
    
    public Token getNextSiblingOrParent()
    {
        return next_sibling != null ? next_sibling : parent;
    }
    
    public void removeFromParent()
    {
        if(next_sibling != null)
        {
            next_sibling.previous_sibling = previous_sibling;
        }
        if(previous_sibling != null)
        {
            previous_sibling.next_sibling = next_sibling;
        }
        else
        {
            parent.first_child = next_sibling;
        }
        next_sibling = null;
        previous_sibling = null;
    }
    
    private static void addToParent(Token parent, Token child)
    {
        if(parent != null)
        {
            child.next_sibling = parent.first_child;
            child.previous_sibling = null;
            if(parent.first_child != null)
            {
                parent.first_child.previous_sibling = child;
            }
            parent.first_child = child;
        }
    }

    public void removeFromWme()
    {
        if(w != null)
        {
            if(next_from_wme != null)
            {
                next_from_wme.previous_from_wme = previous_from_wme;
            }
            if(previous_from_wme != null)
            {
                previous_from_wme.next_from_wme = next_from_wme;
            }
            else
            {
                w.tokens = next_from_wme;
            }
            next_from_wme = null;
            previous_from_wme = null;
        }
    }
    
    public void removeFromNode()
    {
        final NonPosNodeData a_np = node.a_np;
        
        if(next_of_node != null)
        {
            next_of_node.previous_of_node = previous_of_node;
        }
        if(previous_of_node != null)
        {
            previous_of_node.next_of_node = next_of_node;
        }
        else
        {
            a_np.tokens = next_of_node;
        }
        next_of_node = null;
        previous_of_node = null;
    }
    
    private static void addToNode(NonPosNodeData a_np, Token tok)
    {
        tok.next_of_node = a_np.tokens;
        tok.previous_of_node = null;
        if(a_np.tokens != null)
        {
            a_np.tokens.previous_of_node = tok;
        }
        a_np.tokens = tok;
    }
    
    
    private static void addToWme(Wme wme, Token tok)
    {
        if (wme != null)
        {
            tok.next_from_wme = wme.tokens;
            tok.previous_from_wme = null;
            if(wme.tokens != null)
            {
                wme.tokens.previous_from_wme = tok;
            }
            wme.tokens = tok;
        }
        
    }
}
