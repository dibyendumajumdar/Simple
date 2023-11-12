package com.seaofnodes.simple;

import com.seaofnodes.simple.node.Node;
import com.seaofnodes.simple.node.RegionNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DominatorTest {

    RegionNode add(List<Node> nodes, RegionNode node) {
        nodes.add(node);
        return node;
    }

    RegionNode makeGraph(List<Node> nodes) {
        RegionNode r0 = add(nodes, new RegionNode());
        RegionNode r1 = add(nodes, new RegionNode(r0));
        RegionNode r2 = add(nodes, new RegionNode(r1));
        RegionNode r3 = add(nodes, new RegionNode(r2));
        RegionNode r4 = add(nodes, new RegionNode(r3));
        RegionNode r5 = add(nodes, new RegionNode(r1));
        RegionNode r6 = add(nodes, new RegionNode(r5));
        RegionNode r7 = add(nodes, new RegionNode(r6));
        RegionNode r8 = add(nodes, new RegionNode(r5));
        r7.add_def(r8);
        r3.add_def(r7);
        r1.add_def(r3);
        return r0;
    }

    @Test
    public void testDominatorTree() {
        List<Node> nodes = new ArrayList<>();
        RegionNode root = makeGraph(nodes);
        DominatorTree tree = new DominatorTree(root, Node.rpo(root));
        int[] expectedIdoms = {0,1,1,2,2,4,2,6,6,6};
        for (Node n: nodes) {
            Assert.assertEquals(expectedIdoms[n._nid], tree.idom(n)._nid);
        }
    }

}
