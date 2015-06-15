package containmentcache.bitset.opt.redblacktree;

import containmentcache.bitset.opt.ASortedSetTests;
import containmentcache.bitset.opt.ISortedSet;
import containmentcache.bitset.opt.redblacktree.RedBlackTree;

public class RedBlackTreeTests extends ASortedSetTests{

	@Override
	protected <E extends Comparable<E>> ISortedSet<E> getSortedSet() {
		return new RedBlackTree<E>();
	}

}
