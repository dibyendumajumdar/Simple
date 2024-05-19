package com.seaofnodes.simple.linear;

import java.util.*;
import java.util.stream.Collectors;

public class LoopFinder {
    // Based on Compilers: Principles, Techniques and Tools
    // p 604
    // 1986 ed
    public static LoopNest getNaturalLoop(BasicBlock head, BasicBlock backedge) {
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

    // Based on Compilers: Principles, Techniques and Tools
    // p 604
    // 1986 ed
    public static List<LoopNest> findLoops(List<BasicBlock> nodes) {
        List<LoopNest> list = new ArrayList<>();
        for (BasicBlock n : nodes) {
            for (BasicBlock input : n._predecessors) {
                if (n.dominates(input)) {
                    list.add(getNaturalLoop(n, input));
                }
            }
        }
        return list;
    }

    public static List<LoopNest> mergeLoopsWithSameHead(List<LoopNest> loopNests) {
        HashMap<Long, LoopNest> map = new HashMap<>();
        for (LoopNest loopNest : loopNests) {
            LoopNest sameHead = map.get(loopNest._loopHead._bid);
            if (sameHead == null) map.put(loopNest._loopHead._bid, loopNest);
            else sameHead._nodes.addAll(loopNest._nodes);
        }
        return map.values().stream().collect(Collectors.toList());
    }

    public static LoopNest buildLoopTree(List<LoopNest> loopNests) {
        for (LoopNest nest1 : loopNests) {
            for (LoopNest nest2 : loopNests) {
                boolean isNested = nest1.contains(nest2);
                if (isNested) {
                    if (nest2._parent == null) nest2._parent = nest1;
                    else if (nest1._loopHead._domDepth > nest2._parent._loopHead._domDepth) nest2._parent = nest1;
                }
            }
        }
        LoopNest top = null;
        for (LoopNest nest : loopNests) {
            if (nest._parent != null) nest._parent._children.add(nest);
            else top = nest._parent;
        }
        return top;
    }

    public static void annotateBasicBlocks(List<LoopNest> loops) {
        var sortedByDepth = loops.stream().sorted(Collections.reverseOrder(Comparator.comparingLong(n -> n._loopHead._domDepth))).toList();
        return;
    }
}
