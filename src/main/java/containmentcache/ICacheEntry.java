package containmentcache;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableBiMap;

/**
 * An abstraction for the queries to a containment cache.
 * 
 * @author afrechet
 *
 * @param <E>
 *            - elements available in a containment cache.
 */
public interface ICacheEntry<E> {

	/**
	 * @return the set element to which the cache entry corresponds.
	 */
	default Set<E> getElements() {
		final BitSet bs = getBitSet();
		final ImmutableBiMap<E, Integer> permutation = getPermutation();
		final Set<E> set = new HashSet<>();
		for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
			set.add(permutation.inverse().get(i));
		}
		return set;
	}

	/**
	 * @return the bit set to which the cache entry corresponds according to the
	 *         permutation give by {@link #getPermutation()}
	 */
	BitSet getBitSet();

	/**
	 * @return the permutation according to which the bitset returned by
	 *         {@link #getBitSet()} is formed
	 */
	ImmutableBiMap<E, Integer> getPermutation();

}
