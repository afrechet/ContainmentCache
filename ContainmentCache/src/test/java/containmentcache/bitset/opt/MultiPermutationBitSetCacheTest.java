package containmentcache.bitset.opt;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;

import containmentcache.AContainmentCacheTests;
import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;
import containmentcache.bitset.opt.sortedset.redblacktree.RedBlackTree;

public class MultiPermutationBitSetCacheTest extends AContainmentCacheTests {

    private static final int NUM_ORDERINGS = 1;
    private static final long SEED = 0;

	@Override
	protected <E, C extends ICacheEntry<E>> IContainmentCache<E, C> getCache(BiMap<E, Integer> permutation, Comparator<E> comparator) {
        final List<E> universelist = new ArrayList<>(permutation.keySet());
        final List<BiMap<E, Integer>> permutations = new ArrayList<>();
        final Random random = new Random(SEED);
        for (int o = 0; o < NUM_ORDERINGS - 1; o++) {
        	final Builder<E, Integer> builder = ImmutableBiMap.builder();
        	Collections.shuffle(universelist, random);
            int index = 0;
            for (E element : universelist) {
            	builder.put(element, index++);
            }
            permutations.add(builder.build());
        }
        return new MultiPermutationBitSetCache<E, C>(permutation, permutations, c -> new RedBlackTree<BitSet>(c));
	}
    
}
