package com.seaofnodes.simple.linear;

import java.util.*;

public class LoopNest {
    /**
     * Parent loop
     */
    public LoopNest _parent;
    /**
     * The block that is the head of the loop
     */
    public final BasicBlock _loopHead;
    /**
     * Blocks that are part of this loop
     */
    public final Set<BasicBlock> _blocks;
    /**
     * Children as per Loop Tree
     */
    public final List<LoopNest> _kids;
    /**
     * Loop Tree depth - top has depth 1
     */
    public int _depth;

    public LoopNest(BasicBlock loopHead) {
        _loopHead = loopHead;
        _blocks = new HashSet<>();
        _blocks.add(loopHead);
        _kids = new ArrayList<>();
    }

    public void insert(BasicBlock m, Stack<BasicBlock> stack) {
        if (!_blocks.contains(m)) {
            _blocks.add(m);
            stack.push(m);
        }
    }

    public boolean contains(LoopNest other) {
        return this != other && _blocks.containsAll(other._blocks);
    }

    public String uniqueName() { return "Loop_" + _loopHead.uniqueName(); }
    public String label() { return "Loop(" + _loopHead.label() + ":" + _depth + ")"; }

    public static String generateDotOutput(List<LoopNest> loopNests) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph LoopTree {\n");
        for (LoopNest n : loopNests) {
            sb.append(n.uniqueName()).append(" [label=\"").append(n.label()).append("\"];\n");
        }
        for (LoopNest n : loopNests) {
            for (LoopNest c: n._kids) {
                sb.append(n.uniqueName()).append("->").append(c.uniqueName()).append(";\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }
}
