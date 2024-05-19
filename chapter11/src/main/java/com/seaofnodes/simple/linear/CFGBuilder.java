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

    ArrayList<BasicBlock> _basicBlocks = new ArrayList<>();

    Node _start;

    /**
     * Entry block
     */
    public BasicBlock _entry;

    /**
     * Exit block
     */
    public BasicBlock _exit;

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
     *
     * @param end
     * @return
     */
    private BasicBlock buildCFG(Node end) {
        Node start = end.getBlockStart();
        assert (start instanceof StartNode) || (start instanceof RegionNode) || (start instanceof StopNode) || (start instanceof ProjNode proj && proj.ctrl() instanceof IfNode);

        BasicBlock bb = getBasicBlock(start, end);
        if (bb != null) return bb;
        else {
            bb = new BasicBlock(start, end);
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

    public void buildCFG(StartNode startNode, StopNode stopNode) {
        _start = startNode;
        buildCFG(stopNode);
        processInfiniteLoops();
    }

    static void postOrderWalk(Node n, Consumer<Node> consumer, BitSet visited) {
        visited.set(n._nid);
        /* For each successor node */
        for (Node s: n._outputs) {
            if (s == null || !s.isCFG()) continue;
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

    public String generateDotOutput() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph CFG {\n");
        for (BasicBlock bb : _basicBlocks) {
            sb.append(bb.uniqueName()).append(" [label=\"").append(bb.label()).append("\"];\n");
        }
        for (BasicBlock bb : _basicBlocks) {
            for (BasicBlock succ : bb._successors) {
                sb.append(bb.uniqueName()).append("->").append(succ.uniqueName()).append(";\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }
}
