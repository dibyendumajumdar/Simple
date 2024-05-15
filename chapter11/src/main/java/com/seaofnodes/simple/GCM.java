package com.seaofnodes.simple;

import com.seaofnodes.simple.node.Node;

import java.util.*;

public class GCM {

    static class LoopNest {
        Node _loopHead;
        Set<Node> _nodes;

        public LoopNest(Node loopHead) {
            _loopHead = loopHead;
            _nodes = new HashSet<>();
            _nodes.add(loopHead);
        }

        public void insert(Node m, Stack<Node> stack) {
            if (!_nodes.contains(m)) {
                _nodes.add(m);
                stack.push(m);
            }
        }
    }

    // Based on Compilers: Principles, Techniques and Tools
    // p 604
    // 1986 ed
    public LoopNest getNaturalLoop(Node head, Node backedge) {
        Stack<Node> stack = new Stack<>();
        LoopNest loop = new LoopNest(head);
        loop.insert(backedge, stack);
        // trace back up from backedge to head
        while (!stack.isEmpty()) {
            Node m = stack.pop();
            for (Node pred : m._inputs) {
                // Assume we only need CFG nodes
                if (pred == null || !pred.isCFG()) continue;
                loop.insert(pred, stack);
            }
        }
        return loop;
    }

    public List<LoopNest> findLoops(List<Node> nodes) {
        List<LoopNest> list = new ArrayList<>();
        for (Node n : nodes) {
            assert n.isCFG();
            for (Node input : n._inputs) {
                if (input == null || !input.isCFG()) continue;
                if (n.dominates(input)) {
                    System.out.println("Found loop head at " + n);
                    list.add(getNaturalLoop(n, input));
                }
            }
        }
        return list;
    }

    public void schedule(Node root) {
        DominatorTree tree = new DominatorTree(root);
        List<LoopNest> naturalLoops = findLoops(tree._cfgNodes);
    }

}
