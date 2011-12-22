package com.egoclean.persistence;

import java.util.ArrayList;
import java.util.List;

/**
 * Nodes used to create a non-cyclic hierarchy of classes.
 */
class Node {
    private final Class<?> mContent;
    private List<Node> children;
    private Node mParent;

    Node(Class<?> content) {
        mContent = content;
    }

    boolean addChild(final Node child) {
        if (child.mContent == mContent) {
            return false;
        }
        List<Node> allButMe = allButMe();
        if (allButMe.contains(child)) {
            return false;
        }
        if (children == null) {
            children = new ArrayList<Node>();
        }
        children.add(child);
        child.setParent(this);
        return true;
    }

    List<Node> allButMe() {
        Node root = this;
        do {
            Node parent = root.getParent();
            if (parent == null) {
                break;
            }
            root = parent;
        } while (true);
        List<Node> allButMe = new ArrayList<Node>(){
            @Override
            public boolean add(Node node) {
                if (contains(node)) {
                    return false;
                }
                return super.add(node);
            }
        };
        if (root.mContent != mContent) {
            allButMe.add(root);
        }
        allButMe(root, allButMe, this);
        return allButMe;
    }

    private void allButMe(Node node, List<Node> list, Node me) {
        if (node.children == null || node.children.size() == 0) {
            return;
        }
        for (Node child : node.children) {
            if (child.mContent != me.mContent) {
                list.add(child);
            }
            allButMe(child, list, me);
        }
    }

    public Node getParent() {
        return mParent;
    }

    public void setParent(Node parent) {
        mParent = parent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;

        Node node = (Node) o;

        if (!mContent.equals(node.mContent)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return mContent.hashCode();
    }

    @Override
    public String toString() {
        return "Node{" +
                "content=" + mContent +
                ", mParent=" + (mParent == null ? "I'm the root" : mParent.mContent) +
                ", children=" + children +
                '}';
    }

    void removeChild(Node child) {
        children.remove(child);
    }
}
