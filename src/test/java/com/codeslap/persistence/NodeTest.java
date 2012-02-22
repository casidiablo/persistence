/*
 * Copyright 2012 CodeSlap
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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