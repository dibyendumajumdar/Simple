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
        r7.addDef(r8);
        r3.addDef(r7);
        r1.addDef(r3);
        return r0;
    }

    @Test
    public void testDominatorTree() {
        List<Node> nodes = new ArrayList<>();
        RegionNode root = makeGraph(nodes);
        DominatorTree tree = new DominatorTree(root);
        int[] expectedIdoms = {0,1,1,2,2,4,2,6,6,6};
        for (Node n: nodes) {
            Assert.assertEquals(expectedIdoms[n._nid], n._idom._nid);
        }
    }

    RegionNode makeGraph2(List<Node> nodes) {

        RegionNode r1 = add(nodes, new RegionNode());
        RegionNode r2 = add(nodes, new RegionNode(r1));
        RegionNode r3 = add(nodes, new RegionNode(r2, null));
        RegionNode r4 = add(nodes, new RegionNode(r2));
        RegionNode r5 = add(nodes, new RegionNode(r4));
        RegionNode r6 = add(nodes, new RegionNode(r4));
        RegionNode r7 = add(nodes, new RegionNode(r5, r6));
        RegionNode r8 = add(nodes, new RegionNode(r5));
        RegionNode r9 = add(nodes, new RegionNode(r8));
        RegionNode r10 = add(nodes, new RegionNode(r9));
        RegionNode r11 = add(nodes, new RegionNode(r7));
        RegionNode r12 = add(nodes, new RegionNode(r10, r11));

        r2.addDef(r3);
        r2.addDef(r4);
        r5.addDef(r10);
        r8.addDef(r9);
        return r1;
    }

    public String generateDotOutput(List<Node> nodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("digraph g {\n");
        for (Node n: nodes)
            sb.append(n.uniqueName()).append(";\n");
        for (Node n: nodes) {
            for (Node use: n._outputs) {
                sb.append(n.uniqueName()).append("->").append(use.uniqueName()).append(";\n");
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    @Test
    public void testLoopNests() {
        List<Node> nodes = new ArrayList<>();
        RegionNode root = makeGraph2(nodes);
        System.out.println(generateDotOutput(nodes));
        DominatorTree tree = new DominatorTree(root);
        GCM gcm = new GCM();
        List<GCM.LoopNest> loopNests = gcm.findLoops(nodes);
        Assert.assertEquals(2, loopNests.get(0)._loopHead._nid);
        Assert.assertEquals(2, loopNests.get(1)._loopHead._nid);
        Assert.assertEquals(5, loopNests.get(2)._loopHead._nid);
        Assert.assertEquals(8, loopNests.get(3)._loopHead._nid);
        List<GCM.LoopNest> loops = gcm.mergeLoopsWithSameHead(loopNests);
        return;
    }

}
