package com.codeslap.persistence;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author cristian
 */
public class NodeTest {
    @Test
    public void testNode() {
        Node root = new Node(String.class);
        assertNull(root.getParent());

        Node child = new Node(Integer.class);
        assertTrue(root.addChild(child));
        assertTrue(root.addChild(new Node(Long.class)));
        assertNotNull(root.hashCode());

        List<Node> allButMeChild = child.allButMe();
        assertEquals(2, allButMeChild.size());
        assertTrue(allButMeChild.add(child));
        assertFalse(allButMeChild.add(child));

        List<Node> allButMeRoot = root.allButMe();
        assertEquals(2, allButMeRoot.size());

        Node child2 = new Node(Integer.class);
        assertFalse(root.addChild(child2));
    }
}