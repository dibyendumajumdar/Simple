package com.seaofnodes.simple;

import com.seaofnodes.simple.node.Node;

import java.util.*;

public class DominatorTree {

    Node _root;
    // Keyed by node id
    Map<Integer, Node> _IDOM = new HashMap<>();
    // Nodes in reverse post order
    List<Node> _nodes;
    // key node id, value rpo
    Map<Integer, Integer> _rpo = new HashMap<>();

    public DominatorTree(Node root) {
        _root = root;
        _nodes = Node.rpo(root);
        for (int i = 0; i < _nodes.size(); i++)
            _rpo.put(_nodes.get(i)._nid, i+1);

           /*
Node 0 RPO 1
Node 1 RPO 2
Node 5 RPO 3
Node 8 RPO 4
Node 6 RPO 5
Node 7 RPO 6
Node 2 RPO 7
Node 3 RPO 8
Node 4 RPO 9
     */

        /*
Node 0 RPO 1
Node 1 RPO 2
Node 5 RPO 3
Node 8 RPO 4
Node 6 RPO 5
Node 7 RPO 6
Node 2 RPO 7
Node 3 RPO 8
Node 4 RPO 9
         */

        calculateDominatorTree();
    }

    int rpo(Node n) {
        return _rpo.get(n._nid);
    }


    /**
     * Finds nearest common ancestor
     *
     * The algorithm starts at the two nodes whose sets are being intersected, and walks
     * upward from each toward the root. By comparing the nodes with their RPO numbers
     * the algorithm finds the common ancestor - the immediate dominator of i and j.
     */
    Node intersect(Node i, Node j)
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
    Node findFirstPredecessorWithIdom(Node n) {
        for (int i = 0; i < n.nIns(); i++) {
            Node p = n.in(i);
            if (p == null || !p.isCFG()) continue;
            if (_IDOM.get(p._nid) != null) return p;
        }
        return null;
    }

    void calculateDominatorTree()
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

    Node idom(Node n) {
        return _IDOM.get(n._nid);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < _nodes.size(); i++) {
            sb.append("IDOM[").append(_nodes.get(i)).append("] = ");
            sb.append(idom(_nodes.get(i))).append("\n");
        }
        return sb.toString();
    }
}
