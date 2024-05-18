package com.seaofnodes.simple.linear;

import com.seaofnodes.simple.node.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * Given a STOP node construct a CFG
 */
public class CFGBuilder {

    ArrayList<BasicBlock> _basicBlocks = new ArrayList<>();

    List<Node> _cfgNodes = new ArrayList<>();

    Node _start;

    public BasicBlock _root;

//    int _preorder;
//    int _rpostorder;
//    int _bids = 1;

    // Nodes that start blocks are Start, Region, Stop, Proj(If)
    // Nodes that End Blocks are If, Return

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

    static void walkCFG(Node n, Consumer<Node> consumer, BitSet visited) {
        visited.set(n._nid);
        /* For each successor node */
        for (Node s : n._outputs) {
            if (s == null) continue;
            if (!visited.get(s._nid)) walkCFG(s, consumer, visited);
        }
        consumer.accept(n);
    }

    private static void walk(Node root, Consumer<Node> consumer) {
        walkCFG(root, consumer, new BitSet());
    }

    private BasicBlock buildCFG(Node end) {
        Node start = end.getBlockStart();
        assert (start instanceof StartNode) || (start instanceof RegionNode) || (start instanceof StopNode) || (start instanceof ProjNode proj && proj.ctrl() instanceof IfNode);

        BasicBlock bb = getBasicBlock(start, end);
        if (bb != null) return bb;
        else {
            bb = new BasicBlock(start, end);
            if (start instanceof StartNode)
                _root = bb;
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
        processInfiniteLoops(startNode);
    }

//    private void computeRPO(Node n) {
//        n._pre = _preorder++;
//        for (Node s : n._outputs) {
//            if (s == null || !s.isCFG()) continue;
//            if (s._pre == 0) computeRPO(s);
//        }
//        n._rpost = _rpostorder--;
//    }
//
//    private void computeRPO() {
//        _preorder = 1;
//        _rpostorder = _cfgNodes.size();
//        for (Node n : _cfgNodes) n.resetRPO();
//        computeRPO(_start);
//    }
//
//    private void sortByRPO() {
//        _cfgNodes.sort(Collections.reverseOrder(Comparator.comparingInt(n -> n._rpost)));
//    }

    static void dfs(Node n, Consumer<Node> consumer, BitSet visited) {
        visited.set(n._nid);
        /* For each successor node */
        for (Node s: n._outputs) {
            if (s == null || !s.isCFG()) continue;
            if (!visited.get(s._nid))
                dfs(s, consumer, visited);
        }
        consumer.accept(n);
    }

    /**
     * Creates a reverse post order list of CFG nodes

     * @param root The starting CFG node, typically START
     * @return
     */
    public static List<Node> rpo(Node root) {
        List<Node> nodes = new ArrayList<>();
        dfs(root, (n) -> {if (n.isCFG()) nodes.add(0,n);}, new BitSet());
        return nodes;
    }

    // SoN thesis says we find basic blocks bottom up
    // but this has an issue that infinite loops can be unreachable, e.g.
    // if (arg) while(1) {}
    // return 0;
    private void processInfiniteLoops(StartNode startNode) {
//        walk(startNode, (n) -> {if (n.isCFG()) _cfgNodes.add(n);});
//        for (Node n : _cfgNodes) n.resetDomInfo();
//        computeRPO();
//        sortByRPO();
        var nodes = rpo(_start).reversed();
        for (Node r : nodes) {
            // Regions start basic blocks, so if we never saw this
            // region then there is no basic block yet
            if (r instanceof RegionNode && getBasicBlock(r) == null)
                buildCFG(r);
        }
    }

    public String generateDotOutput() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph domtree {\n");
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
