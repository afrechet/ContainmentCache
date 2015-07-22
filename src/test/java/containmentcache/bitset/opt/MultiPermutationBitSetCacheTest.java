package containmentcache.bitset.opt;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.BiMap;

import containmentcache.AContainmentCacheTests;
import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;
import containmentcache.bitset.opt.sortedset.redblacktree.RedBlackTree;
import containmentcache.util.PermutationUtils;

public class MultiPermutationBitSetCacheTest extends AContainmentCacheTests {

    private static final int NUM_ORDERINGS = 5;
    private static final long SEED = 0;

	@Override
	protected <E, C extends ICacheEntry<E>> IContainmentCache<E, C> getCache(BiMap<E, Integer> permutation, Comparator<E> comparator) {
        final List<BiMap<E, Integer>> additionalPermutations = PermutationUtils.makeNPermutations(permutation, SEED, NUM_ORDERINGS);
        return new MultiPermutationBitSetCache<E, C>(permutation, additionalPermutations, RedBlackTree::new);
	}

}
