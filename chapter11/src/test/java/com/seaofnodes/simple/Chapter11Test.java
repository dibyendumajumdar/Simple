package com.seaofnodes.simple;

import com.seaofnodes.simple.evaluator.Evaluator;
import com.seaofnodes.simple.linear.BasicBlock;
import com.seaofnodes.simple.linear.CFGBuilder;
import com.seaofnodes.simple.linear.DominatorTree;
import com.seaofnodes.simple.linear.GCM;
import com.seaofnodes.simple.node.StopNode;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Chapter11Test {

    @Test
    public void testSimpleLoop() {
        Parser parser = new Parser ("""
                while(arg < 10) {
                    arg = arg + 1;
                }
                return arg;
                """
        );
        StopNode stop = parser.parse(true).iterate(true);
        var eval = new Evaluator(stop);
        assertEquals(Long.valueOf(10), eval.evaluate(0, 100));
        CFGBuilder cfg = new CFGBuilder();
        cfg.buildCFG(Parser.START, stop);
        System.out.println(cfg.generateDotOutput(cfg._basicBlocks));
        DominatorTree tree = new DominatorTree(cfg._entry);
        System.out.println(tree.generateDotOutput());
        GCM gcm = new GCM(cfg._entry, cfg._exit, cfg._basicBlocks, cfg._allInstructions);
    }

    // Bug cause infinite loop in findLCA
    @Test
    public void testLCABug() {
        Parser parser = new Parser ("""
int i=1;
while(arg < 10) {
    arg = arg + 1;
    if (arg == 5) i=2;
}
return i;
                """
        );
        StopNode stop = parser.parse(true).iterate(true);
//        var eval = new Evaluator(stop);
//        assertEquals(Long.valueOf(10), eval.evaluate(0, 100));
        CFGBuilder cfg = new CFGBuilder();
        cfg.buildCFG(Parser.START, stop);
        System.out.println(cfg.generateDotOutput(cfg._basicBlocks));
        DominatorTree tree = new DominatorTree(cfg._entry);
        System.out.println(tree.generateDotOutput());
        GCM gcm = new GCM(cfg._entry, cfg._exit, cfg._basicBlocks, cfg._allInstructions);
    }

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
        System.out.println(cfg.generateDotOutput(cfg._basicBlocks));
        DominatorTree tree = new DominatorTree(cfg._entry);
        System.out.println(tree.generateDotOutput());
        GCM gcm = new GCM(cfg._entry, cfg._exit, cfg._basicBlocks, cfg._allInstructions);
    }

    @Test
    public void testStoreInIf() {
        Parser parser = new Parser(
        """
                struct S {
                    int f;
                }
                S v0=new S;
                if(arg) v0.f=1;
                return v0;
                """);
        StopNode stop = parser.parse(true).iterate(true);
        CFGBuilder cfg = new CFGBuilder();
        cfg.buildCFG(Parser.START, stop);
        System.out.println(cfg.generateDotOutput(cfg._basicBlocks));
        DominatorTree tree = new DominatorTree(cfg._entry);
        System.out.println(tree.generateDotOutput());
        GCM gcm = new GCM(cfg._entry, cfg._exit, cfg._basicBlocks, cfg._allInstructions);
    }

    @Test
    public void testPrimeNumberGen() {
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
        System.out.println(cfg.generateDotOutput(cfg._basicBlocks));
        DominatorTree tree = new DominatorTree(cfg._entry);
        System.out.println(tree.generateDotOutput());
        GCM gcm = new GCM(cfg._entry, cfg._exit, cfg._basicBlocks, cfg._allInstructions);

    }

    // Example of loop from 1994 paper
    // strength reduction
    @Test
    public void testLoopFinding() {
        BasicBlock b1 = new BasicBlock(1);
        BasicBlock b2 = new BasicBlock(2, b1);
        BasicBlock b3 = new BasicBlock(3, b1, b2);
        BasicBlock b4 = new BasicBlock(4, b3);
        BasicBlock b5 = new BasicBlock(5, b4);
        BasicBlock b6 = new BasicBlock(6, b4);
        BasicBlock b7 = new BasicBlock(7, b5, b6);
        BasicBlock b8 = new BasicBlock(8, b7);
        BasicBlock b9 = new BasicBlock(9, b8);
        BasicBlock b10 = new BasicBlock(10, b8);
        b1.addPredecessor(b9);
        b3.addPredecessor(b4);
        b3.addPredecessor(b8);
        b4.addPredecessor(b7);
        b7.addPredecessor(b10);
        List<BasicBlock> blocks = Arrays.asList(b1,b2,b3,b4,b5,b6,b7,b8,b9,b10);
        System.out.println(CFGBuilder.generateDotOutput(blocks));
        DominatorTree tree = new DominatorTree(b1);
        System.out.println(tree.generateDotOutput());
    }
}
