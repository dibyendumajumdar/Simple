package com.seaofnodes.simple.linear;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class LoopNest {
    public LoopNest _parent;
    public final BasicBlock _loopHead;
    public final Set<BasicBlock> _nodes;

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

    public boolean contains(LoopNest other) {
        return this != other && _nodes.containsAll(other._nodes);
    }
}
