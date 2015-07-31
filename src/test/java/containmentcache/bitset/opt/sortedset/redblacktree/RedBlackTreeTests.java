package containmentcache.bitset.opt.sortedset.redblacktree;

import java.util.Comparator;
import java.util.Random;

import org.junit.Test;

import containmentcache.bitset.opt.sortedset.ASortedSetTests;
import containmentcache.bitset.opt.sortedset.ISortedSet;

public class RedBlackTreeTests extends ASortedSetTests{

	@Override
	protected ISortedSet<Integer> getSortedSet(Comparator<Integer> comparator) {
		return new RedBlackTree<Integer>(comparator);
	}

    @Test
    public void testRedBlackTreeProperties() {
        final RedBlackTree<Integer> rbTree = new RedBlackTree<Integer>(Integer::compareTo);
        final Random random = new Random();
        for (int i = 0; i < 10000; i++) {
            final int op = random.nextInt(2);
            final int val = random.nextInt(100);
            if (op == 0) {
                rbTree.add(val);
            } else if (op == 1) {
                rbTree.remove(val);
            }
        }
        rbTree.checkCoherence();
        rbTree.checkRedBlack();
        rbTree.checkSizes();
    }


}
