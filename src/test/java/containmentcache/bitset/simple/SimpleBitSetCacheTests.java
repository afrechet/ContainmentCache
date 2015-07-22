package containmentcache.bitset.simple;

import java.util.Comparator;

import com.google.common.collect.BiMap;

import containmentcache.AContainmentCacheTests;
import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;

public class SimpleBitSetCacheTests extends AContainmentCacheTests {

	@Override
	protected <E, C extends ICacheEntry<E>> IContainmentCache<E, C> getCache(BiMap<E, Integer> permutation, Comparator<E> comparator) {
		return new SimpleBitSetCache<E,C>(permutation);
	}

}
