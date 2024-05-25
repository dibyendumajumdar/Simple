package com.seaofnodes.simple.linear;

import com.seaofnodes.simple.node.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * Given a STOP node construct a CFG.
 * Nodes that start basic blocks are Start, Region, Stop, Proj(If).
 * Nodes that end Blocks are If, Return, Stop.
 * We do a bottom up walk of the ctrl nodes, beginning from Stop.
 * But this can miss infinite loops so we do another pass looking
 * for missed loops.
 */
public class CFGBuilder {

    /** basic block ID generator */
    private int _nextBBID = 1;

    /** all the basic blocks */
    public final ArrayList<BasicBlock> _basicBlocks = new ArrayList<>();

    Node _start;

    /**
     * Entry block
     */
    public BasicBlock _entry;

    /**
     * Exit block
     */
    public BasicBlock _exit;

    /**
     * All Nodes in the input SoN graph
     */
    public List<Node> _allInstructions = new ArrayList<>();

    // FIXME these lookups do a linear search for now

    BasicBlock getBasicBlock(Node start, Node end) {
        for (BasicBlock bb : _basicBlocks) {
            if (bb._start == start && bb._end == end) return bb;
        }
        return null;
    }

    BasicBlock getBasicBlock(Node start) {
        for (BasicBlock bb : _basicBlocks) {
            if (bb._start == start) return bb;
        }
        return null;
    }

    /**
     * Build a CFG
     */
    public void buildCFG(StartNode startNode, StopNode stopNode) {
        _start = startNode;
        postOrderWalk(_start, (n) -> _allInstructions.add(0, n), new BitSet());
        int rpo = 1;
        for (Node n: _allInstructions)
            n._rpo = rpo;
        buildCFG(stopNode);
        processInfiniteLoops();
    }

    /**
     * Build a CFG bottom up
     */
    private BasicBlock buildCFG(Node end) {
        Node start = end.getBlockStart();
        assert (start instanceof StartNode) || (start instanceof RegionNode) || (start instanceof StopNode) || (start instanceof ProjNode proj && proj.ctrl() instanceof IfNode);

        BasicBlock bb = getBasicBlock(start, end);
        if (bb != null) return bb;
        else {
            bb = new BasicBlock(_nextBBID++, start, end);
            if (start instanceof StartNode)
                _entry = bb;
            else if (start instanceof StopNode)
                _exit = bb;
            _basicBlocks.add(bb);
        }
        for (Node in : start._inputs) {
            if (in == null || !in.isCFG()) continue;
            BasicBlock predBB = buildCFG(in);
            predBB.addSuccessor(bb);
        }
        return bb;
    }

    // SoN thesis says we find basic blocks bottom up
    // but this has an issue that infinite loops can be unreachable, e.g.
    // if (arg) while(1) {}
    // return 0;
    private void processInfiniteLoops() {
        var nodes = getNodesInRPO(_start).reversed();
        for (Node r : nodes) {
            // Regions start basic blocks, so if we never saw this
            // region then there is no basic block yet
            if (r instanceof RegionNode && getBasicBlock(r) == null)
                buildCFG(r);
        }
    }

    static void postOrderWalk(Node n, Consumer<Node> consumer, BitSet visited) {
        visited.set(n._nid);
        /* For each successor node */
        for (Node s: n._outputs) {
            if (s == null) continue;
            if (!visited.get(s._nid))
                postOrderWalk(s, consumer, visited);
        }
        consumer.accept(n);
    }

    /**
     * Creates a reverse post order list of CFG nodes
     *
     * @param root The starting CFG node, typically START
     */
    public static List<Node> getNodesInRPO(Node root) {
        List<Node> nodes = new ArrayList<>();
        // Note below that we prepend each entry - this is essentially mimicking a stack
        // so the list we get back is reverse post order
        postOrderWalk(root, (n) -> {if (n.isCFG()) nodes.add(0,n);}, new BitSet());
        return nodes;
    }

    public static String generateDotOutput(List<BasicBlock> blocks) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph CFG {\n");
        for (BasicBlock bb : blocks) {
            sb.append(bb.uniqueName()).append(" [label=\"").append(bb.label()).append("\"];\n");
        }
        for (BasicBlock bb : blocks) {
            for (BasicBlock succ : bb._successors) {
                sb.append(bb.uniqueName()).append("->").append(succ.uniqueName()).append(";\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }
}
