package com.seaofnodes.simple;

import com.seaofnodes.simple.node.RegionNode;
import org.junit.Test;

public class DominatorTest {

    RegionNode makeGraph() {
        RegionNode r0 = new RegionNode();
        RegionNode r1 = new RegionNode(r0);
        RegionNode r2 = new RegionNode(r1);
        RegionNode r3 = new RegionNode(r2);
        RegionNode r4 = new RegionNode(r3);
        RegionNode r5 = new RegionNode(r1);
        RegionNode r6 = new RegionNode(r5);
        RegionNode r7 = new RegionNode(r6);
        RegionNode r8 = new RegionNode(r5);
        r7.add_def(r8);
        r3.add_def(r7);
        r1.add_def(r3);
        return r0;
    }

    @Test
    public void testDominatorTree() {
        RegionNode root = makeGraph();
        DominatorTree tree = new DominatorTree(root);
        System.out.println(tree.toString());
        return;
    }

}
