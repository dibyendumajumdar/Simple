package com.seaofnodes.simple.linear;

import com.seaofnodes.simple.node.Node;

import java.util.*;

public class BasicBlock {
    public final long _bid;
    public List<BasicBlock> _successors = new ArrayList<>(); // successors
    List<BasicBlock> _predecessors = new ArrayList<>();

    /**
     * The preorder traversal number, also acts as a flag indicating whether the
     * BB is yet to be visited (_pre==0 means not yet visited).
     */
    int _pre;
    /**
     * The depth of the BB in the dominator tree
     */
    int _domDepth;
    /**
     * Reverse post order traversal number;
     * Sort node list in ascending order by this to traverse graph in reverse post order.
     * In RPO order if an edge exists from A to B we visit A followed by B, but cycles have to
     * be dealt with in another way.
     */
    int _rpo;
    /**
     * Immediate dominator is the closest strict dominator.
     * @see DominatorTree
     */
    public BasicBlock _idom;

    /**
     * Initial starting Node in this BB;
     * TBD whether we keep this
     */
    Node _start;
    /**
     * Initial ending node in this BB;
     * TBC whether we keep this
     */
    Node _end;

    /**
     * Nodes for whom this node is the immediate dominator,
     * thus the dominator tree.
     */
    public List<BasicBlock> _dominated = new ArrayList<>();
    /**
     * Dominance frontier
     */
    public Set<BasicBlock> _frontier = new HashSet<>();


    public BasicBlock(Node start, Node end) {
        long x = start._nid;
        long y = end._nid;
        this._bid = (x << 32) | y;
        this._start = start;
        this._end = end;
    }

    // For tests
    public BasicBlock(int bid, BasicBlock... preds) {
        this._bid = bid;
        for (BasicBlock bb : preds)
            addPredecessor(bb);
    }

    public void addSuccessor(BasicBlock bb) {
        _successors.add(bb);
        bb._predecessors.add(this);
    }

    public void addPredecessor(BasicBlock bb) {
        bb.addSuccessor(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicBlock that = (BasicBlock) o;
        return _bid == that._bid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(_bid);
    }

    public String label() {
        return _start != null ? ("BB(" + _start._nid + ":" + _end._nid + ")") : ("BB(" + _bid + ")");
    }

    public String uniqueName() {
        return _start != null ? ("BB_" + _start._nid + "_" + _end._nid) : ("BB_" + _bid);
    }

    //////////////// Some fields to support dominator calculations /////////////////////
    // These are updated by DominatorTree


    public void resetDomInfo() {
        _domDepth = 0;
        _idom = null;
        _dominated.clear();
        _frontier.clear();
    }

    public void resetRPO() {
        _pre = 0;
        _rpo = 0;
    }

    public boolean dominates(BasicBlock other) {
        if (this == other) return true;
        while (other._domDepth > _domDepth) other = other._idom;
        return this == other;
    }

    /////////////////// End of dominator calculations //////////////////////////////////

}
