package com.seaofnodes.simple.linear;

import com.seaofnodes.simple.IRPrinter;
import com.seaofnodes.simple.node.*;

import java.util.*;

public class GCM {

    // We do not want to contaminate our SoN Nodes with stuff that
    // is part of the linear schedule; so we avoid Nodes knowing about
    // BasicBlocks etc.
    public final Map<Integer, BasicBlock> _earlySchedule = new HashMap<>();
    public final Map<Integer, BasicBlock> _schedule = new HashMap<>();
    public final Map<Integer, Node> _nodeLookup = new HashMap<>();

    /**
     * Get a control block
     */
    private BasicBlock control(Node node) {
        assert node != null;
        return _schedule.get(node._nid);
    }

    /**
     * Set a control block
     */
    private void control(Node node, BasicBlock bb) {
        assert bb != null;
        _schedule.put(node._nid, bb);
    }

    /**
     * Create a reverse mapping from Node ID to BasicBlock
     */
    private void scheduleCFGNodes(List<BasicBlock> allBlocks, BitSet bitset) {
        for (BasicBlock bb: allBlocks) {
            control(bb._start, bb);
            bitset.set(bb._start._nid);
            if (bb._start != bb._end) {
                control(bb._end, bb);
                bitset.set(bb._end._nid);
            }
        }
    }

    /**
     * Pinned nodes cannot move
     */
    private boolean pinned(Node n) {
        return n instanceof PhiNode
                || n.isCFG()
                || n instanceof ProjNode proj && proj.ctrl() instanceof StartNode
                || n instanceof ConstantNode
                || n instanceof NewNode;
    }

    // CFG nodes should have been pinned already
    // Phis are pinned to the BB of related region
    // Constants and Proj(Start) get pinned to Entry block
    // New gets pinned to the control block
    private void schedulePinnedNodes(List<Node> allInstructions, BasicBlock entry, BitSet visited) {
        for (Node n: allInstructions)
            if (pinned(n) && !visited.get(n._nid)) {
                visited.set(n._nid);
                // pin it
                if (n instanceof PhiNode phi)
                    control(phi, control(phi.in(0)));
                else if (n instanceof ProjNode proj && proj.ctrl() instanceof StartNode)
                    control(proj, entry);
                else if (n instanceof ConstantNode)
                    control(n, entry);
                else if (n instanceof NewNode newNode) {
                    Node ctrl = newNode.in(0);
                    assert pinned(ctrl);
                    if (!visited.get(ctrl._nid)) {
                        visited.set(ctrl._nid);
                        // Only possibility is proj on start
                        if (ctrl instanceof ProjNode proj && proj.ctrl() instanceof StartNode)
                            control(proj, entry);
                        else
                            throw new IllegalStateException("Unpinned ctrl input to NewNode " + ctrl);
                    }
                    control(newNode, control(ctrl));
                }
            }
    }

    private void scheduleNodeEarly(Node n, BitSet visited) {
        if (visited.get(n._nid))
            return;
        visited.set(n._nid);
        for (Node in: n._inputs) {
            if (in == null)
                continue;
            if (control(in) == null)
                scheduleNodeEarly(in, visited);
        }
        if (control(n) != null)
            return;
        BasicBlock b = null;
        for (Node in: n._inputs) {
            if (in == null)
                continue;
            BasicBlock inb = control(in);
            if (b == null)
                b = inb;
            else if (b._domDepth < inb._domDepth)
                b = inb;
        }
        control(n, b);
    }

    private void scheduleEarly(BasicBlock entry, BasicBlock exit, List<BasicBlock> allBlocks, List<Node> allInstructions) {
        // Schedule early
        BitSet visited = new BitSet();
        scheduleCFGNodes(allBlocks, visited);
        assert exit._start instanceof StopNode;
        schedulePinnedNodes(allInstructions, entry, visited);
        for (Node n: allInstructions)
            scheduleNodeEarly(n, visited);
        for (Node n: allInstructions) {
            if (control(n) == null)
                throw new RuntimeException("Missed schedule for " + n);
            control(n)._earlySchedule.add(n);
        }
        dumpEarlySchedule(entry);
    }


    private void dumpEarlySchedule(BasicBlock entry)
    {
        System.out.println(dumpNodesInBlock(new StringBuilder(), entry, new BitSet()).toString());
    }

    private StringBuilder dumpNodesInBlock(StringBuilder sb, BasicBlock bb, BitSet visited)
    {
        if (visited.get(bb._bid))
            return sb;
        visited.set(bb._bid);
        sb.append("L" + bb._bid + ":\n");
        for (Node n: bb._earlySchedule) {
            sb.append("\t");
            IRPrinter._printLineLlvmFormat(n, sb);
        }
        for (BasicBlock succ: bb._successors) {
            dumpNodesInBlock(sb, succ, visited);
        }
        return sb;
    }

    private void setupNodeLookup(List<Node> allInstructions)
    {
        for (Node n: allInstructions)
            _nodeLookup.put(n._nid, n);
    }

    private void scheduleLate(List<Node> allInstructions)
    {
        BitSet visited = new BitSet();
        for (Node n: allInstructions) {
            if (pinned(n)) {
                visited.set(n._nid);
                for (Node use : n._outputs) {
                    scheduleNodeLate(use, visited);
                }
            }
        }
    }

    private int loopDepth(BasicBlock bb) {
        if (bb._loop != null)
            return bb._loop._depth;
        return 0;
    }

    private void scheduleNodeLate(Node n, BitSet visited) {
        if (visited.get(n._nid))
            return;
        visited.set(n._nid);
        if (pinned(n)) // Not mentioned in paper
            return;
        BasicBlock lca = null;
        for (Node use: n._outputs) {
            scheduleNodeLate(use, visited);
            BasicBlock useBlock = control(use);
            if (use instanceof PhiNode) {
                int j = 0;
                for (; j < use.nIns(); j++) {
                    if (use.in(j) == n)
                        break;
                }
                assert use.in(j) == n;
                assert j >= 1;
                useBlock = useBlock._predecessors.get(j-1); // adjust for the fact that phi's first input is the region
            }
            lca = findLCA(lca, useBlock);
        }
        BasicBlock best = lca;
        while (lca != control(n)) {
            if (loopDepth(lca) < loopDepth(best))
                best = lca;
            lca = lca._idom;
        }
        control(n, best);
    }

    private BasicBlock findLCA(BasicBlock a, BasicBlock b) {
        if (a == null) return  b;
        while (a._domDepth < b._domDepth)
            a = a._idom;
        while (b._domDepth < a._domDepth)
            b = b._idom;
        while (a != b) {
            a = a._idom;
            b = b._idom;
        }
        return a;
    }

    // See 2. Global Code Motion
    // in Global Code Motion Global Value Numbering
    // paper by Cliff Click
    // Also see SoN thesis
    public GCM(BasicBlock entry, BasicBlock exit, List<BasicBlock> allBlocks, List<Node> allInstructions) {
        setupNodeLookup(allInstructions);
        // Find the CFG Dominator Tree and
        // annotate basic blocks with dominator tree depth
        DominatorTree tree = new DominatorTree(entry);
        // find loops and compute loop nesting depth for
        // each basic block
        List<LoopNest> naturalLoops = LoopFinder.findLoops(tree._blocks);
        List<LoopNest> loops = LoopFinder.mergeLoopsWithSameHead(naturalLoops);
        LoopNest top = LoopFinder.buildLoopTree(loops);
        LoopFinder.annotateBasicBlocks(top);
        System.out.println(LoopNest.generateDotOutput(loops));
        scheduleEarly(entry, exit, allBlocks, allInstructions);
        _earlySchedule.putAll(_schedule);
        scheduleLate(allInstructions);
    }


}
