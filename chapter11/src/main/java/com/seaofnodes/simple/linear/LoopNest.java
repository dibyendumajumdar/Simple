package com.seaofnodes.simple.linear;

import java.util.*;

public class LoopNest {
    public LoopNest _parent;
    public final BasicBlock _loopHead;
    public final Set<BasicBlock> _nodes;
    public final List<LoopNest> _children;

    public LoopNest(BasicBlock loopHead) {
        _loopHead = loopHead;
        _nodes = new HashSet<>();
        _nodes.add(loopHead);
        _children = new ArrayList<>();
    }

    public void insert(BasicBlock m, Stack<BasicBlock> stack) {
        if (!_nodes.contains(m)) {
            _nodes.add(m);
            stack.push(m);
        }
    }

    public boolean contains(LoopNest other) {
        return this != other && _nodes.containsAll(other._nodes);
    }

    public String uniqueName() { return "Loop_" + _loopHead.uniqueName(); }
    public String label() { return "Loop(" + _loopHead.label() + ")"; }

    public static String generateDotOutput(List<LoopNest> loopNests) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph LoopTree {\n");
        for (LoopNest n : loopNests) {
            sb.append(n.uniqueName()).append(" [label=\"").append(n.label()).append("\"];\n");
        }
        for (LoopNest n : loopNests) {
            for (LoopNest c: n._children) {
                sb.append(n.uniqueName()).append("->").append(c.uniqueName()).append(";\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }
}
