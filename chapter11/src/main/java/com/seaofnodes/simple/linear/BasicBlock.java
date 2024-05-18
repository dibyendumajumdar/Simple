package com.seaofnodes.simple.linear;

import com.seaofnodes.simple.node.Node;

import java.util.*;

public class BasicBlock {
    public final long _bid;
    public List<BasicBlock> _successors = new ArrayList<>(); // successors
    List<BasicBlock> _predecessors = new ArrayList<>();
    int _pre;
    int _domDepth;
    int _rpost;
    public BasicBlock _idom;

    Node _start;
    Node _end;

    /**
     * Nodes who have this node as immediate dominator,
     * thus the dominator tree.
     */
    public List<BasicBlock> _dominated = new ArrayList<>();
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
    }

    public void resetRPO() {
        _pre = 0;
        _rpost = 0;
    }

    public boolean dominates(BasicBlock other) {
        if (this == other) return true;
        while (other._domDepth > _domDepth) other = other._idom;
        return this == other;
    }

    /////////////////// End of dominator calculations //////////////////////////////////

}
