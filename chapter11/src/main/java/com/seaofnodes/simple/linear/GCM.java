package com.seaofnodes.simple.linear;

import java.util.*;
import java.util.stream.Collectors;

public class GCM {

    public static class LoopNest {
        public BasicBlock _loopHead;
        Set<BasicBlock> _nodes;

        public LoopNest(BasicBlock loopHead) {
            _loopHead = loopHead;
            _nodes = new HashSet<>();
            _nodes.add(loopHead);
        }

        public void insert(BasicBlock m, Stack<BasicBlock> stack) {
            if (!_nodes.contains(m)) {
                _nodes.add(m);
                stack.push(m);
            }
        }
    }

    // Based on Compilers: Principles, Techniques and Tools
    // p 604
    // 1986 ed
    public LoopNest getNaturalLoop(BasicBlock head, BasicBlock backedge) {
        Stack<BasicBlock> stack = new Stack<>();
        LoopNest loop = new LoopNest(head);
        loop.insert(backedge, stack);
        // trace back up from backedge to head
        while (!stack.isEmpty()) {
            BasicBlock m = stack.pop();
            for (BasicBlock pred : m._predecessors) {
                loop.insert(pred, stack);
            }
        }
        return loop;
    }

    public List<LoopNest> findLoops(List<BasicBlock> nodes) {
        List<LoopNest> list = new ArrayList<>();
        for (BasicBlock n : nodes) {
            for (BasicBlock input : n._predecessors) {
                if (n.dominates(input)) {
                    System.out.println("Found loop head at " + n);
                    list.add(getNaturalLoop(n, input));
                }
            }
        }
        return list;
    }

    public List<LoopNest> mergeLoopsWithSameHead(List<LoopNest> loopNests) {
        HashMap<Long, LoopNest> map = new HashMap<>();
        for (LoopNest loopNest: loopNests) {
            LoopNest sameHead = map.get(loopNest._loopHead._bid);
            if (sameHead == null) map.put(loopNest._loopHead._bid, loopNest);
            else {
                sameHead._nodes.addAll(loopNest._nodes);
            }
        }
        return map.values().stream().collect(Collectors.toList());
    }


    public void schedule(BasicBlock root) {
        DominatorTree tree = new DominatorTree(root);
        List<LoopNest> naturalLoops = findLoops(tree._blocks);
        List<LoopNest> loops = mergeLoopsWithSameHead(naturalLoops);
    }

}
