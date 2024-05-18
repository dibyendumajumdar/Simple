package com.seaofnodes.simple;

import com.seaofnodes.simple.node.Node;

import java.util.*;
import java.util.function.Consumer;

/**
 * The dominator tree construction algorithm is based on figure 9.24,
 * chapter 9, p 532, of Engineering a Compiler.
 *
 * The algorithm is also described in the paper 'A Simple, Fast
 * Dominance Algorithm' by Keith D. Cooper, Timothy J. Harvey and
 * Ken Kennedy.
 */

public class DominatorTree {
    Node _root;
    // List of CFG nodes reachable from root
    List<Node> _cfgNodes;

    int _preorder;
    int _rpostorder;

    /**
     * Builds a Dominator Tree.
     *
     * @param root The root node
     */
    public DominatorTree(Node root) {
        _root = root;
        _cfgNodes = getControlNodes(root);
        calculateDominatorTree();
        populateTree();
        setDepth();
        calculateDominanceFrontiers();
    }

    private void populateTree() {
        for (Node n: _cfgNodes) {
            Node idom = n._idom;
            if (idom == n) // root
                continue;
            // add edge from idom to n
            idom._dominated.add(n);
        }
    }

    private void setDepth_(Node n) {
        Node idom = n._idom;
        if (idom != n) {
            assert idom._domdepth > 0;
            n._domdepth = idom._domdepth + 1;
        }
        else {
            assert idom._domdepth == 1;
            assert idom._domdepth == n._domdepth;
        }
        for (Node child: n._dominated) {
            setDepth_(child);
        }
    }

    private void setDepth() {
        _root._domdepth = 1;
        setDepth_(_root);
    }

    /**
     * Gets the RPO number for a given node.
     */
    private int rpo(Node n) {
        return n._rpost;
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
    private Node findFirstPredecessorWithIdom(Node n) {
        for (Node p: n._inputs) {
            if (p == null || !p.isCFG()) continue;
            if (p._idom != null) return p;
        }
        return null;
    }

    // compute rpo using a depth first search
    private void computeRPO(Node n)
    {
        n._pre = _preorder++;
        for (Node s: n._outputs) {
            if (s == null || !s.isCFG()) continue;
            if (s._pre == 0) computeRPO(s);
        }
        n._rpost = _rpostorder--;
    }

    private void computeRPO()
    {
        _preorder = 1;
        _rpostorder = _cfgNodes.size();
        for (Node n: _cfgNodes) n.resetRPO();
        computeRPO(_root);
    }

    private void sortByRPO()
    {
        _cfgNodes.sort(Comparator.comparingInt(n -> n._rpost));
    }

    private void calculateDominatorTree()
    {
        for (Node n: _cfgNodes) n.resetDomInfo();
        computeRPO();
        sortByRPO();

        // Set IDom entry for root to itself
        _root._idom = _root;
        boolean changed = true;
        while (changed) {
            changed = false;
            // for all nodes, b, in reverse postorder (except root)
            for (Node b: _cfgNodes) {
                if (b == _root) // skip root
                    continue;
                // NewIDom = first (processed) predecessor of b, pick one
                Node firstPred = findFirstPredecessorWithIdom(b);
                assert firstPred != null;
                Node newIDom = firstPred;
                // for all other predecessors, p, of b
                for (Node p: b._inputs) {
                    if (p == null || !p.isCFG())
                        continue;
                    if (p == firstPred)
                        continue; // all other predecessors
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
        for (Node b: _cfgNodes) {
            if (b.nCtrlIns() >= 2) {
                for (Node p: b._inputs) {
                    if (p == null || !p.isCFG())
                        continue;
                    Node runner = p;
                    while (runner != b._idom) {
                        runner._frontier.add(b);
                        runner = runner._idom;
                    }
                }
            }
        }
    }

    static void walkCFG(Node n, Consumer<Node> consumer, BitSet visited) {
        if (!n.isCFG()) return;
        visited.set(n._nid);
        /* For each successor node */
        for (Node s: n._outputs) {
            if (s == null || !s.isCFG())
                continue;
            if (!visited.get(s._nid))
                walkCFG(s, consumer, visited);
        }
        consumer.accept(n);
    }

    /**
     * Creates a reverse post order list of CFG nodes

     * @param root The starting CFG node, typically START
     * @return
     */
    private static List<Node> getControlNodes(Node root) {
        List<Node> nodes = new ArrayList<>();
        walkCFG(root, (n)->nodes.add(n), new BitSet());
        return nodes;
    }

    public String generateDotOutput() {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph domtree {\n");
        for (Node n: _cfgNodes) {
            sb.append(n.uniqueName()).append(" [label=\"")
                    .append(n.glabel())
                    .append("\"];\n");
        }
        for (Node n: _cfgNodes) {
            Node idom = n._idom;
            if (idom == n)
                continue;
            sb.append(idom.uniqueName()).append("->")
                    .append(n.uniqueName()).append(";\n");
        }
        sb.append("}\n");
        return sb.toString();
    }
}
