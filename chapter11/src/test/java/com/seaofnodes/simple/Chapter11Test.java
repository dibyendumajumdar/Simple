package com.seaofnodes.simple;

import com.seaofnodes.simple.evaluator.Evaluator;
import com.seaofnodes.simple.node.Node;
import com.seaofnodes.simple.node.StopNode;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Chapter11Test {

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
        DominatorTree tree = new DominatorTree(Parser.START);
        System.out.println(IRPrinter.prettyPrint(stop, 99, true));
        System.out.println(new GraphVisualizer().generateDotOutput(stop,null,null));
        var eval = new Evaluator(stop);
        assertEquals(Long.valueOf(4), eval.evaluate(10, 100));
        System.out.println(tree.generateDotOutput());

        Node a = stop.find(2);
        Assert.assertTrue(a.dominates(stop));
        Assert.assertFalse(stop.dominates(a));
        a = stop.find(4);
        Assert.assertTrue(a.dominates(stop));
        Assert.assertFalse(stop.dominates(a));
        a = stop.find(8);
        Assert.assertTrue(a.dominates(stop));
        Assert.assertFalse(stop.dominates(a));
        a = stop.find(9);
        Assert.assertFalse(a.dominates(stop));
        Assert.assertFalse(stop.dominates(a));
        GCM gcm = new GCM();
        gcm.schedule(stop.find(2));
    }


}
