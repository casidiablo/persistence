package com.egoclean.persistence;

import org.junit.Test;

import java.util.List;

public class NodeTest {
    @Test
    public void testNode() {
        Node root = new Node(String.class);
        System.out.println(root);

        Node child = new Node(Integer.class);
        root.addChild(child);
        System.out.println(root);

        List<Node> allButMeChild = child.allButMe();
        System.out.println("All but me ("+allButMeChild.size()+") child:");
        for (Node node : allButMeChild) {
            System.out.println(":: "+node);
        }

        List<Node> allButMeRoot = root.allButMe();
        System.out.println("All but me ("+allButMeRoot.size()+") root:");
        for (Node node : allButMeRoot) {
            System.out.println(":: "+node);
        }

        Node child2 = new Node(Integer.class);
        if (root.addChild(child2)) {
            System.out.println(root);

            List<Node> allButMeChild2 = child2.allButMe();
            System.out.println("All but me ("+allButMeChild2.size()+") child2:");
            for (Node node : allButMeChild2) {
                System.out.println(":: "+node);
            }
        }
    }
}
