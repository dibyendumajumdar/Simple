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
    BasicBlock _root;
    // List of CFG nodes reachable from root
    List<BasicBlock> _blocks;

    int _preorder;
    int _rpostorder;

    /**
     * Builds a Dominator Tree.
     *
     * @param root The root node
     */
    public DominatorTree(BasicBlock root) {
        _root = root;
        _blocks = getControlNodes(root);
        calculateDominatorTree();
        populateTree();
        setDepth();
        calculateDominanceFrontiers();
    }

    private void populateTree() {
        for (BasicBlock n : _blocks) {
            BasicBlock idom = n._idom;
            if (idom == n) // root
                continue;
            // add edge from idom to n
            idom._dominated.add(n);
        }
    }

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

    private void setDepth() {
        _root._domDepth = 1;
        setDepth_(_root);
    }

    /**
     * Gets the RPO number for a given node.
     */
    private int rpo(BasicBlock n) {
        return n._rpost;
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
            while (rpo(finger1) > rpo(finger2)) {
                finger1 = finger1._idom;
                assert finger1 != null;
            }
            while (rpo(finger2) > rpo(finger1)) {
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
    private void computeRPO(BasicBlock n) {
        n._pre = _preorder++;
        for (BasicBlock s : n._successors) {
            if (s._pre == 0) computeRPO(s);
        }
        n._rpost = _rpostorder--;
    }

    private void computeRPO() {
        _preorder = 1;
        _rpostorder = _blocks.size();
        for (BasicBlock n : _blocks) n.resetRPO();
        computeRPO(_root);
    }

    private void sortByRPO() {
        _blocks.sort(Comparator.comparingInt(n -> n._rpost));
    }

    private void calculateDominatorTree() {
        for (BasicBlock n : _blocks) n.resetDomInfo();
        computeRPO();
        sortByRPO();

        // Set IDom entry for root to itself
        _root._idom = _root;
        boolean changed = true;
        while (changed) {
            changed = false;
            // for all nodes, b, in reverse postorder (except root)
            for (BasicBlock b : _blocks) {
                if (b == _root) // skip root
                    continue;
                // NewIDom = first (processed) predecessor of b, pick one
                BasicBlock firstPred = findFirstPredecessorWithIdom(b);
                assert firstPred != null;
                BasicBlock newIDom = firstPred;
                // for all other predecessors, p, of b
                for (BasicBlock p : b._predecessors) {
                    if (p == firstPred) continue; // all other predecessors
                    if (p._idom != null) {
                        // i.e. IDoms[p] calculated
                        newIDom = intersect(p, newIDom);
                    }
                }
                if (b._idom != newIDom) {
                    b._idom = newIDom;
                    changed = true;
                }
            }
        }
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
                        BasicBlock prev = runner;
                        runner = runner._idom;
                        if (runner == null)
                            throw new RuntimeException();
                    }
                }
            }
        }
    }

    static void walkCFG(BasicBlock n, Consumer<BasicBlock> consumer, HashSet<BasicBlock> visited) {
        visited.add(n);
        System.out.println("Visited " + n._bid);
        /* For each successor node */
        for (BasicBlock s : n._successors) {
            System.out.println("Checking  " + s._bid);
            if (!visited.contains(s))
                walkCFG(s, consumer, visited);
        }
        consumer.accept(n);
    }

    /**
     * Creates a reverse post order list of CFG nodes
     *
     * @param root The starting CFG node, typically START
     * @return
     */
    private static List<BasicBlock> getControlNodes(BasicBlock root) {
        List<BasicBlock> nodes = new ArrayList<>();
        walkCFG(root, (n) -> nodes.add(n), new HashSet<>());
        return nodes;
    }

    public String generateDotOutput() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph domtree {\n");
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
