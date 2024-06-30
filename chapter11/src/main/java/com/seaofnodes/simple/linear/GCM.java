package com.seaofnodes.simple.linear;

import com.seaofnodes.simple.IRPrinter;
import com.seaofnodes.simple.node.*;
import com.seaofnodes.simple.type.TypeMem;

import java.util.*;

/**
 * GCM Stands for Global Code Motion; its goal is to decide where to place
 * Sea of Node instructions in the Basic Blocks. It is global in the sense that
 * the scheduling of instructions looks at all Basic Blocks in the program, rather
 * than scheduling locally within a single block.
 *
 * The implementation here is based upon and is a reasonably faithful reproduction
 * of the algorithm described in the paper 'Global Code Motion, Global Value Numbering'
 * by Cliff Click, 1995.
 */
public class GCM {

    // NOTE
    // We do not want to contaminate our SoN Nodes with stuff that
    // is part of the linear schedule; so we avoid Nodes knowing about
    // BasicBlocks etc. To avoid tight coupling we use lookup tables instead.

    /**
     * Maps a Nodes by _nid to the BasicBlock to which it is assigned.
     * This is the early schedule which is used as an input to final schedule.
     */
    public final Map<Integer, BasicBlock> _earlySchedule = new HashMap<>();

    /**
     * Maps Nodes by _nid to the BasicBlock to which they belong.
     * This is the outcome of the late schedule, which has the early schedule
     * as a starting point.
     */
    public final Map<Integer, BasicBlock> _schedule = new HashMap<>();

    // See 2. Global Code Motion
    // in Global Code Motion Global Value Numbering
    // paper by Cliff Click
    // Also see SoN thesis
    public GCM(BasicBlock entry, BasicBlock exit, List<BasicBlock> blocks, List<Node> instructions) {
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
        scheduleEarly(entry, exit, blocks, instructions);
        _earlySchedule.putAll(_schedule);
        scheduleLate(instructions);
        localSchedule(blocks);
        dumpSchedule(entry);
    }


    /**
     * Gets the BasicBlock assigned to a Node
     */
    private BasicBlock control(Node node) {
        assert node != null;
        return _schedule.get(node._nid);
    }

    /**
     * Sets the basicBlock assigned to a Node
     */
    private void control(Node node, BasicBlock bb) {
        assert bb != null;
        _schedule.put(node._nid, bb);
    }

    /**
     * Create a reverse mapping from Node ID to BasicBlock
     */
    private void scheduleCFGNodes(List<BasicBlock> allBlocks) {
        for (BasicBlock bb: allBlocks) {
            control(bb._start, bb);
            if (bb._start != bb._end) {
                control(bb._end, bb);
            }
        }
    }

    /**
     * Pinned nodes cannot move
     */
    private boolean pinned(Node n) {
        return n instanceof PhiNode
                || n instanceof NewNode;
    }

    // CFG nodes should have been pinned already
    // Phis are pinned to the BB of related region
    // New gets pinned to the control block
    private void schedulePinnedNodes(List<Node> allInstructions, BasicBlock entry, BitSet visited) {
        for (Node n: allInstructions)
            if (pinned(n) && !visited.get(n._nid)) {
                visited.set(n._nid);
                // pin it
                if (n instanceof PhiNode phi)
                    control(phi, control(phi.in(0)));
                else if (n instanceof NewNode newNode) {
                    Node ctrl = newNode.in(0);
                    if (!visited.get(ctrl._nid)) {
                        visited.set(ctrl._nid);
                        // Only possibility is proj on start
                        if (ctrl instanceof CProjNode proj && proj.ctrl() instanceof StartNode)
                            control(proj, entry);
                        else
                            throw new IllegalStateException("Unpinned ctrl input to NewNode " + ctrl);
                    }
                    control(newNode, control(ctrl));
                }
            }
    }

    private void scheduleNodeEarly(Node n, BitSet visited) {
        if (visited.get(n._nid)) {
            assert(control(n) != null);
            return;
        }
        visited.set(n._nid);
        for (Node in: n._inputs) {
            if (in == null)
                continue;
            if (control(in) == null)    // No BasicBlock assigned yet
                scheduleNodeEarly(in, visited);
        }
        if (control(n) != null)         // basic Block already assigned
            return;
        // Place instructions in the first block
        // where they are dominated by their inputs
        BasicBlock b = null;
        for (Node in: n._inputs) {
            if (in == null)
                continue;
            BasicBlock inb = control(in);
            if (b == null)
                b = inb;
            // Note that dom depth is reverse of
            // the GCM paper
            else if (b._domDepth > inb._domDepth)
                b = inb;
        }
        assert b != null;
        //System.out.println("Setting early for node " + n._nid + "(" + n + ") to " + b);
        control(n, b);
    }

    /**
     * Implements the schedule early algoritm as described in the GCMGVN paper.
     */
    private void scheduleEarly(BasicBlock entry, BasicBlock exit, List<BasicBlock> blocks, List<Node> instructions) {
        scheduleCFGNodes(blocks);
        assert exit._start instanceof StopNode;
        BitSet visited = new BitSet();
        schedulePinnedNodes(instructions, entry, visited);
        for (Node n: instructions)
            scheduleNodeEarly(n, visited);
    }


    /**
     * Implements the schedule late algorithm from the GCMGVN paper
     */
    private void scheduleLate(List<Node> instructions)
    {
        BitSet visited = new BitSet();
        for (Node n: instructions) {
            scheduleNodeLate(n, visited);
        }
        for (Node n: instructions) {
            if (control(n) == null)
                throw new RuntimeException("Missed schedule for " + n);
            control(n)._schedule.add(n);
        }
    }

    private int loopDepth(BasicBlock bb) {
        if (bb._loop != null)
            return bb._loop._depth;
        return 0; // No loop
    }

    private void scheduleNodeLate(Node n, BitSet visited) {
        if (visited.get(n._nid))
            return;
        visited.set(n._nid);
        // Walk Stores before Loads, so we can get the anti-deps right
        // This is not in the GCM paper
        for (Node use: n._outputs) {
            if ( use != null && use._type instanceof TypeMem )
                scheduleNodeLate(use, visited);
        }
        // Walk the rest
        for (Node use: n._outputs) {
            if (use != null)
                scheduleNodeLate(use, visited);
        }
        if (pinned(n) || n.nOuts() == 0 || n.isCFG()) // Not mentioned in paper, CFG nodes are not movable either
            return;
        BasicBlock lca = null;
        // Place instructions in the last block where they
        // dominate their uses
        // We need to find the lowest common ancestor (LCA)
        // in the dominator tree of all an instruction’s uses.
        for (Node use: n._outputs) {
            BasicBlock bb = getUseBlock(n, use);
            lca = findLCA(lca, bb);
        }
        // Loads may need anti-dependencies
        // This is not in the GCM paper
        BasicBlock early = control(n);
        if (n instanceof LoadNode load) {
            // Trace the Load's path up DOM Tree from LCA to early BB
            // and record the trace in the BB
            for (BasicBlock bb = lca; ; bb = bb._idom) {
                bb._load_nid = load._nid;
                if (bb == early) break;
            }
            for (Node mem : load.mem()._outputs) {
                switch (mem) {
                    case StoreNode store:
                        lca = addAntiDeps(load, control(store), lca, store);
                        break;
                    case PhiNode phi:
                        // No anti-dep edges but may raise the LCA.
                        for (int i = 1; i < phi.nIns(); i++)
                            if (phi.in(i) == load.mem())
                                lca = addAntiDeps(load, phiPredecessorBlock(phi, i), lca, null);
                        break;
                    case LoadNode ld:
                        break; // Loads do not cause anti-deps on other loads
                    case ReturnNode ret:
                        break; // Load must already be ahead of Return
                    default:
                        throw new UnsupportedOperationException();
                }
            }
        }

        // Choose the block that is in the shallowest loop nest possible
        BasicBlock best = lca;
        while (lca != control(n)) {
            if (loopDepth(lca) < loopDepth(best))
                best = lca;
            lca = lca._idom;
        }

        //System.out.println("Setting late for node " + n._nid + "(" + n + ") to " + best);
        control(n, best);
    }

    private BasicBlock addAntiDeps(LoadNode load, BasicBlock storeBB, BasicBlock lca, Node store ) {
        // Walk store blocks "reach" from its scheduled location to its earliest
        for (BasicBlock defBB = control(load.mem()); ; storeBB = storeBB._idom) {
            // Store and Load overlap, need anti-dependence
            if (storeBB._load_nid == load._nid) {
                BasicBlock oldlca = lca;
                lca = findLCA(lca, storeBB); // Raise Loads LCA
                if (oldlca != lca && store != null) // And if something moved,
                    store.addDef(load);   // Add anti-dep as well
            }
            if (storeBB == defBB) break;
        }
        return lca;
    }

    // Block of use.  Normally from late schedule, except for Phis, which go
    // to the matching Region input.
    private BasicBlock getUseBlock(Node n, Node use) {
        BasicBlock useBlock = control(use);
        if (use instanceof PhiNode) {
            int j = 0;
            for (; j < use.nIns(); j++) {
                if (use.in(j) == n) break;
            }
            assert use.in(j) == n;
            assert j >= 1;
            useBlock = useBlock._predecessors.get(j - 1); // adjust for the fact that phi's first input is the region
            assert useBlock != null;
        }
        return useBlock;
    }

    private BasicBlock phiPredecessorBlock(PhiNode use, int j) {
        return control(use)._predecessors.get(j - 1);
    }

    /**
     * Find lowest common ancestor in the dominator tree
     */
    private BasicBlock findLCA(BasicBlock a, BasicBlock b) {
        if (a == null) return  b;
        // Note that dom depth is reverse of
        // the GCM paper
        while (a._domDepth > b._domDepth)
            a = a._idom;
        while (b._domDepth > a._domDepth)
            b = b._idom;
        while (a != b) {
            a = a._idom;
            b = b._idom;
        }
        return a;
    }

    /**
     * Local schedule inside the BB
     * We do RPO except that entry to BB appears first,
     * phis appear after that, followed by nodes other than exit,
     * and then exit.
     */
    private void localSchedule(List<BasicBlock> blocks) {
        for (BasicBlock bb: blocks) {
            List<Node> nodes = bb.reorderInstructionsByRPO();
            List<Node> newSchedule = new ArrayList<>();
            newSchedule.add(bb._start); // Local entry node
            for (Node n: nodes) {       // add phis
                if (n == bb._start) continue;
                if (n instanceof PhiNode)
                    newSchedule.add(n);
            }
            for (Node n: nodes) {       // Add rest except local exit
                if (n == bb._start) continue;
                if (n == bb._end) continue;
                if (n instanceof PhiNode) continue;
                newSchedule.add(n);
            }
            // Add local exit
            if (bb._start != bb._end)
                newSchedule.add(bb._end);
            bb._schedule = newSchedule;
        }
    }

    ////////////////////////// Utilities for printing out the schedule

    private void dumpSchedule(BasicBlock entry)
    {
        System.out.println(dumpNodesInBlock(new StringBuilder(), entry, new BitSet()).toString());
    }

    private BasicBlock trueBranch(BasicBlock bb) {
        assert bb._end instanceof IfNode;
        assert bb._successors.size() == 2;
        BasicBlock first = bb._successors.get(0);
        BasicBlock second = bb._successors.get(1);
        assert first._start instanceof CProjNode prj && prj.ctrl() == bb._end;
        assert second._start instanceof CProjNode prj && prj.ctrl() == bb._end;
        ProjNode proj = (ProjNode) first._start;
        return proj._idx == 0 ? first : second;
    }

    private BasicBlock falseBranch(BasicBlock bb, BasicBlock trueBranch) {
        BasicBlock first = bb._successors.get(0);
        BasicBlock second = bb._successors.get(1);
        return  (first == trueBranch) ? second : first;
    }

    private StringBuilder dumpNodesInBlock(StringBuilder sb, BasicBlock bb, BitSet visited)
    {
        if (visited.get(bb._bid))
            return sb;
        visited.set(bb._bid);
        sb.append("L" + bb._bid + ":\n");
        for (Node n: bb._schedule) {
            if (n instanceof IfNode
                    || !n.isCFG())
                sb.append("\t// ").append(n).append('\n');
            sb.append("\t");
            IRPrinter._printLineLlvmFormat(n, sb);
        }
        if (bb._successors.size() == 2) {
            BasicBlock trueBranch = trueBranch(bb);
            sb.append("\t// if true goto L").append(trueBranch._bid).append(" else goto L")
                    .append(falseBranch(bb, trueBranch)._bid).append("\n");
        }
        else if (bb._successors.size() == 1)
            sb.append("\t// goto L").append(bb._successors.get(0)._bid).append("\n");
        for (BasicBlock succ: bb._successors) {
            dumpNodesInBlock(sb, succ, visited);
        }
        return sb;
    }

}
