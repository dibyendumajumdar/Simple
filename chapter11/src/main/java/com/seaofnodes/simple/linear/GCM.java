package com.seaofnodes.simple.linear;

import com.seaofnodes.simple.node.*;

import java.util.*;

public class GCM {

    // We dont want to contaminate our SoN Nodes with stuff that
    // is part of the linear schedule; so we avoid Nodes knowing about
    // BasicBlocks etc.
    public final Map<Integer, BasicBlock> _earlySchedule = new HashMap<>();
    public final Map<Integer, BasicBlock> _lateSchedule = new HashMap<>();

    /**
     * Get a control block
     */
    private BasicBlock control(Node node) {
        assert node != null;
        return _earlySchedule.get(node._nid);
    }

    /**
     * Set a control block
     */
    private void control(Node node, BasicBlock bb) {
        assert bb != null;
        _earlySchedule.put(node._nid, bb);
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
                else if (n instanceof NewNode newNode)
                    control(newNode, control(newNode.in(0)));
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
        // validate we didn't miss a node
        for (Node n: allInstructions) {
            if (control(n) == null)
                throw new RuntimeException("Missed scheduling " + n);
        }
    }

    // See 2. Global Code Motion
    // in Global Code Motion Global Value Numbering
    // paper by Cliff Click
    // Also see SoN thesis
    public void schedule(BasicBlock entry, BasicBlock exit, List<BasicBlock> allBlocks, List<Node> allInstructions) {
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
    }


}
