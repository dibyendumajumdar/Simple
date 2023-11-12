package com.seaofnodes.simple;

import com.seaofnodes.simple.node.Node;

import java.util.*;

/**
 * The dominator tree construction algorithm is based on figure 9.24,
 * chapter 9, p 532, of Engineering a Compiler.
 *
 * The algorithm is also described in the paper 'A Simple, Fast
 * Dominance Algorithm' by Keith D. Cooper, Timothy J. Harvey and
 * Ken Kennedy.
 *
 * Some terminology:
 *
 * DOM(b): A node n in the CFG dominates b if n lies on every path from the entry node of the CFG to b.
 * DOM(b) contains every node n that dominates b.
 *
 * IDOM(b): For a node b, the set IDOM(b) contains exactly one node, the immediate dominator of b.
 * If n is b's immediate dominator then every node in {DOM(b) - b} is also in DOM(n).
 *
 * The dominator tree algorithm is an optimised version of forward data flow solver. The
 * algorithm iterates until a fixed point is reached. The output of the algorithm is the IDOM
 * array that describes the dominator tree.
 */


public class DominatorTree {
    // Inputs

    // Root node
    Node _root;
    // Nodes in reverse post order
    List<Node> _nodes;

    // Output
    // Keyed by node id
    Map<Integer, Node> _IDOM = new HashMap<>();

    // Utils

    // Maps node nid to its RPO
    Map<Integer, Integer> _rpo = new HashMap<>();

    /**
     * Builds a Dominator Tree.
     *
     * @param root The root node
     * @param nodes The list of nodes in reverse post order
     */
    public DominatorTree(Node root, List<Node> nodes) {
        _root = root;
        _nodes = nodes;
        for (int i = 0; i < _nodes.size(); i++)
            _rpo.put(_nodes.get(i)._nid, i+1);
        calculateDominatorTree();
    }

    /**
     * Gets the RPO number for a given node.
     */
    private int rpo(Node n) {
        return _rpo.get(n._nid);
    }

    /**
     * Finds nearest common ancestor
     *
     * The algorithm starts at the two nodes whose sets are being intersected, and walks
     * upward from each toward the root. By comparing the nodes with their RPO numbers
     * the algorithm finds the common ancestor - the immediate dominator of i and j.
     */
    private Node intersect(Node i, Node j)
    {
        Node finger1 = i;
        Node finger2 = j;
        while (finger1 != finger2) {
            while (rpo(finger1) > rpo(finger2)) {
                finger1 = _IDOM.get(finger1._nid);
                assert finger1 != null;
            }
            while (rpo(finger2) > rpo(finger1)) {
                finger2 = _IDOM.get(finger2._nid);
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
    private Node findFirstPredecessorWithIdom(Node n) {
        for (int i = 0; i < n.nIns(); i++) {
            Node p = n.in(i);
            if (p == null || !p.isCFG()) continue;
            if (_IDOM.get(p._nid) != null) return p;
        }
        return null;
    }

    private void calculateDominatorTree()
    {
        for (int i = 0; i < _nodes.size(); i++) {
            _IDOM.put(_nodes.get(i)._nid, null); /* undefined - set to a invalid value */
        }
        // Set IDom entry for root to itself
        _IDOM.put(_root._nid, _root);
        boolean changed = true;
        while (changed) {
            changed = false;
            // for all nodes, b, in reverse postorder (except root)
            for (int i = 0; i < _nodes.size(); i++) {
                Node b = _nodes.get(i);
                if (b == _root) // skip root
                    continue;
                // NewIDom = first (processed) predecessor of b, pick one
                Node firstpred = findFirstPredecessorWithIdom(b);
                assert firstpred != null;
                Node NewIDom = firstpred;
                // for all other predecessors, p, of b
                for (int k = 0; k < b.nIns(); k++) {
                    Node p = b.in(k);
                    if (p == null || !p.isCFG())
                        continue;
                    if (p == firstpred)
                        continue; // all other predecessors
                    if (_IDOM.get(p._nid) != null) {
                        // i.e. IDoms[p] calculated
                        NewIDom = intersect(p, NewIDom);
                    }
                }
                if (_IDOM.get(b._nid) != NewIDom) {
                    _IDOM.put(b._nid, NewIDom);
                    changed = true;
                }
            }
        }
    }

    /**
     * Gets the IDOM for the given node
     */
    public Node idom(Node n) {
        return _IDOM.get(n._nid);
    }

    /**
     * Calculates dominance-frontiers for nodes
     */
    public Map<Integer, Set<Node>> frontiers(List<Node> nodes) {
        // Dominance-Frontier Algorithm - fig 5 in 'A Simple, Fast Dominance Algorithm'
        //for all nodes, b
        //  if the number of predecessors of b ≥ 2
        //      for all predecessors, p, of b
        //          runner ← p
        //          while runner != doms[b]
        //              add b to runner’s dominance frontier set
        //              runner = doms[runner]
        Map<Integer, Set<Node>> frontiers = new HashMap<>();
        for (Node b: nodes)
            frontiers.put(b._nid, new HashSet<>());
        for (Node b: nodes) {
            if (b.nCtrlIns() >= 2) {
                for (int i = 0; i < b.nIns(); i++) {
                    Node p = b.in(i);
                    if (p == null || !p.isCFG())
                        continue;
                    Node runner = p;
                    while (runner._nid != idom(b)._nid) {
                        frontiers.get(runner._nid).add(b);
                        runner = idom(runner);
                    }
                }
            }
        }
        return frontiers;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < _nodes.size(); i++) {
            sb.append("IDOM[").append(_nodes.get(i)._nid).append("] = ");
            sb.append(idom(_nodes.get(i))._nid).append("\n");
        }
        return sb.toString();
    }
}
