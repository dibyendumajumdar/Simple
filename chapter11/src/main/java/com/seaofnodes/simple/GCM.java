package com.seaofnodes.simple;

import com.seaofnodes.simple.node.Node;

import java.util.*;
import java.util.stream.Collectors;

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

    public List<LoopNest> mergeLoopsWithSameHead(List<LoopNest> loopNests) {
        HashMap<Integer, LoopNest> map = new HashMap<>();
        for (LoopNest loopNest: loopNests) {
            LoopNest sameHead = map.get(loopNest._loopHead._nid);
            if (sameHead == null) map.put(loopNest._loopHead._nid, loopNest);
            else {
                sameHead._nodes.addAll(loopNest._nodes);
            }
        }
        return map.values().stream().collect(Collectors.toList());
    }


    public void schedule(Node root) {
        DominatorTree tree = new DominatorTree(root);
        List<LoopNest> naturalLoops = findLoops(tree._cfgNodes);
        List<LoopNest> loops = mergeLoopsWithSameHead(naturalLoops);
    }

}
