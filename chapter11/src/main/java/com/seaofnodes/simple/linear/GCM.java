package com.seaofnodes.simple.linear;

import java.util.*;
import java.util.stream.Collectors;

public class GCM {



    // See 2. Global Code Motion
    // in Global Code Motion Global Value Numbering
    // paper by Cliff Click
    // Also see
    public void schedule(BasicBlock root) {
        // Find the CFG Dominator Tree and
        // annotate basic blocks with dominator tree depth
        DominatorTree tree = new DominatorTree(root);
        // find loops and compute loop nesting depth for
        // each basic block
        List<LoopNest> naturalLoops = LoopFinder.findLoops(tree._blocks);
        List<LoopNest> loops = LoopFinder.mergeLoopsWithSameHead(naturalLoops);
        LoopFinder.buildLoopTree(loops);
        LoopFinder.annotateBasicBlocks(loops);
    }


}
