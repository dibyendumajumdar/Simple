package com.seaofnodes.simple.linear;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

/**
 * The dominator tree construction algorithm is based on figure 9.24,
 * chapter 9, p 532, of Engineering a Compiler.
 * <p>
 * The algorithm is also described in the paper 'A Simple, Fast
 * Dominance Algorithm' by Keith D. Cooper, Timothy J. Harvey and
 * Ken Kennedy.
 */
public class DominatorTree {
    BasicBlock _entry;
    // List of basic blocks reachable from _entry block, including the _entry
    List<BasicBlock> _blocks;

    int _preorder;
    int _rpostorder;

    /**
     * Builds a Dominator Tree.
     *
     * @param entry The entry block
     */
    public DominatorTree(BasicBlock entry) {
        _entry = entry;
        _blocks = findAllBlocks(entry);
        calculateDominatorTree();
        populateTree();
        setDepth();
        calculateDominanceFrontiers();
    }

    /**
     * Setup the dominator tree.
     * Each block gets the list of blocks it strictly dominates.
     */
    private void populateTree() {
        for (BasicBlock n : _blocks) {
            BasicBlock idom = n._idom;
            if (idom == n) // root
                continue;
            // add edge from idom to n
            idom._dominated.add(n);
        }
    }

    /**
     * Sets the dominator depth on each block
     */
    private void setDepth_(BasicBlock n) {
        BasicBlock idom = n._idom;
        if (idom != n) {
            assert idom._domDepth > 0;
            n._domDepth = idom._domDepth + 1;
        } else {
            assert idom._domDepth == 1;
            assert idom._domDepth == n._domDepth;
        }
        for (BasicBlock child : n._dominated)
            setDepth_(child);
    }

    /**
     * Sets the dominator depth on each block
     */
    private void setDepth() {
        _entry._domDepth = 1;
        setDepth_(_entry);
    }


    /**
     * Finds nearest common ancestor
     * <p>
     * The algorithm starts at the two nodes whose sets are being intersected, and walks
     * upward from each toward the root. By comparing the nodes with their RPO numbers
     * the algorithm finds the common ancestor - the immediate dominator of i and j.
     */
    private BasicBlock intersect(BasicBlock i, BasicBlock j) {
        BasicBlock finger1 = i;
        BasicBlock finger2 = j;
        while (finger1 != finger2) {
            while (finger1._rpo > finger2._rpo) {
                finger1 = finger1._idom;
                assert finger1 != null;
            }
            while (finger2._rpo > finger1._rpo) {
                finger2 = finger2._idom;
                assert finger2 != null;
            }
        }
        return finger1;
    }

    /**
     * Look for the first predecessor whose immediate dominator has been calculated.
     * Because of the order in which this search occurs, we will always find at least 1
     * such predecessor.
     */
    private BasicBlock findFirstPredecessorWithIdom(BasicBlock n) {
        for (BasicBlock p : n._predecessors) {
            if (p._idom != null) return p;
        }
        return null;
    }

    // compute rpo using a depth first search
    private void postOrderWalkSetRPO(BasicBlock n) {
        n._pre = _preorder++;
        for (BasicBlock s : n._successors) {
            if (s._pre == 0) postOrderWalkSetRPO(s);
        }
        n._rpo = _rpostorder--;
    }

    /**
     * Assign rpo number to all the basic blocks.
     * The rpo number defines the Reverse Post Order traversal of blocks.
     * The Dominance calculator requires the rpo number.
     */
    private void annotateBlocksWithRPO() {
        _preorder = 1;
        _rpostorder = _blocks.size();
        for (BasicBlock n : _blocks) n.resetRPO();
        postOrderWalkSetRPO(_entry);
    }

    private void sortBlocksByRPO() {
        _blocks.sort(Comparator.comparingInt(n -> n._rpo));
    }

    private void calculateDominatorTree() {
        resetDomInfo();
        annotateBlocksWithRPO();
        sortBlocksByRPO();

        // Set IDom entry for root to itself
        _entry._idom = _entry;
        boolean changed = true;
        while (changed) {
            changed = false;
            // for all nodes, b, in reverse postorder (except root)
            for (BasicBlock bb : _blocks) {
                if (bb == _entry) // skip root
                    continue;
                // NewIDom = first (processed) predecessor of b, pick one
                BasicBlock firstPred = findFirstPredecessorWithIdom(bb);
                assert firstPred != null;
                BasicBlock newIDom = firstPred;
                // for all other predecessors, p, of b
                for (BasicBlock predecessor : bb._predecessors) {
                    if (predecessor == firstPred) continue; // all other predecessors
                    if (predecessor._idom != null) {
                        // i.e. IDoms[p] calculated
                        newIDom = intersect(predecessor, newIDom);
                    }
                }
                if (bb._idom != newIDom) {
                    bb._idom = newIDom;
                    changed = true;
                }
            }
        }
    }

    private void resetDomInfo() {
        for (BasicBlock bb : _blocks) bb.resetDomInfo();
    }

    /**
     * Calculates dominance-frontiers for nodes
     */
    private void calculateDominanceFrontiers() {
        // Dominance-Frontier Algorithm - fig 5 in 'A Simple, Fast Dominance Algorithm'
        //for all nodes, b
        //  if the number of predecessors of b ≥ 2
        //      for all predecessors, p, of b
        //          runner ← p
        //          while runner != doms[b]
        //              add b to runner’s dominance frontier set
        //              runner = doms[runner]
        for (BasicBlock b : _blocks) {
            if (b._predecessors.size() >= 2) {
                for (BasicBlock p : b._predecessors) {
                    BasicBlock runner = p;
                    while (runner != b._idom) {
                        runner._frontier.add(b);
                        runner = runner._idom;
                    }
                }
            }
        }
    }

    static void postOrderWalk(BasicBlock n, Consumer<BasicBlock> consumer, HashSet<BasicBlock> visited) {
        visited.add(n);
        /* For each successor node */
        for (BasicBlock s : n._successors) {
            if (!visited.contains(s))
                postOrderWalk(s, consumer, visited);
        }
        consumer.accept(n);
    }

    /**
     * Utility to locate all the basic blocks.
     */
    private static List<BasicBlock> findAllBlocks(BasicBlock root) {
        List<BasicBlock> nodes = new ArrayList<>();
        postOrderWalk(root, (n) -> nodes.add(n), new HashSet<>());
        return nodes;
    }

    public String generateDotOutput() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph DomTree {\n");
        for (BasicBlock n : _blocks) {
            sb.append(n.uniqueName()).append(" [label=\"").append(n.label()).append("\"];\n");
        }
        for (BasicBlock n : _blocks) {
            BasicBlock idom = n._idom;
            if (idom == n) continue;
            sb.append(idom.uniqueName()).append("->").append(n.uniqueName()).append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
