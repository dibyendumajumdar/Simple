package com.seaofnodes.simple;

import com.seaofnodes.simple.evaluator.Evaluator;
import com.seaofnodes.simple.linear.CFGBuilder;
import com.seaofnodes.simple.linear.DominatorTree;
import com.seaofnodes.simple.linear.GCM;
import com.seaofnodes.simple.node.StopNode;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Chapter11Test {

    @Test
    public void testEndlessLoop() {
        Parser parser = new Parser ("""
                if (arg) while(1) {}
                return 0;
                """
        );
        StopNode stop = parser.parse(true).iterate(true);
        CFGBuilder cfg = new CFGBuilder();
        cfg.buildCFG(Parser.START, stop);
        System.out.println(cfg.generateDotOutput());
        DominatorTree tree = new DominatorTree(cfg._entry);
        System.out.println(tree.generateDotOutput());
    }

    @Test
    public void testDomTree() {
        Parser parser = new Parser(
"""
if (arg < 2) {
    return 0;
}

int primeCount = 0;
int isPrime = false;
int i = 2;

while (i <= arg) {
    isPrime = true;

    int j = 2;
    while (j < i) {
        int num = i;
        int divisor = j;
        int divisible = false;

        while (num >= divisor) {
            num = num - divisor;
        }
        divisible = num == 0;

        if (divisible) {
            isPrime = false;
            break;
        }

        j = j + 1;
    }

    if (isPrime) {
        primeCount = primeCount + 1;
    }
    i = i + 1;
}
return primeCount;
""");
        StopNode stop = parser.parse(true).iterate(true);
        var eval = new Evaluator(stop);
        assertEquals(Long.valueOf(4), eval.evaluate(10, 100));
        CFGBuilder cfg = new CFGBuilder();
        cfg.buildCFG(Parser.START, stop);
        System.out.println(cfg.generateDotOutput());
        DominatorTree tree = new DominatorTree(cfg._entry);
//        System.out.println(IRPrinter.prettyPrint(stop, 99, true));
//        System.out.println(new GraphVisualizer().generateDotOutput(stop,null,null));
        System.out.println(tree.generateDotOutput());
//
//        Node a = stop.find(2);
//        Assert.assertTrue(a.dominates(stop));
//        Assert.assertFalse(stop.dominates(a));
//        a = stop.find(4);
//        Assert.assertTrue(a.dominates(stop));
//        Assert.assertFalse(stop.dominates(a));
//        a = stop.find(8);
//        Assert.assertTrue(a.dominates(stop));
//        Assert.assertFalse(stop.dominates(a));
//        a = stop.find(9);
//        Assert.assertFalse(a.dominates(stop));
//        Assert.assertFalse(stop.dominates(a));
        GCM gcm = new GCM();
        gcm.schedule(cfg._entry);
    }


}
