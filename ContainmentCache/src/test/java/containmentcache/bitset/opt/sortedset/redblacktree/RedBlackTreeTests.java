package containmentcache.bitset.opt.sortedset.redblacktree;

import containmentcache.bitset.opt.sortedset.ASortedSetTests;
import containmentcache.bitset.opt.sortedset.ISortedSet;
import containmentcache.bitset.opt.sortedset.redblacktree.RedBlackTree;

public class RedBlackTreeTests extends ASortedSetTests{

	@Override
	protected <E extends Comparable<E>> ISortedSet<E> getSortedSet() {
		return new RedBlackTree<E>();
	}

}
