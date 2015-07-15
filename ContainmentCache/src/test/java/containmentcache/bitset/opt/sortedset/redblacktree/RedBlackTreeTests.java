package containmentcache.bitset.opt.sortedset.redblacktree;

import java.util.Comparator;

import containmentcache.bitset.opt.sortedset.ASortedSetTests;
import containmentcache.bitset.opt.sortedset.ISortedSet;

public class RedBlackTreeTests extends ASortedSetTests{

	@Override
	protected ISortedSet<Integer> getSortedSet(Comparator<Integer> comparator) {
		return new RedBlackTree<Integer>(comparator);
	}


}
