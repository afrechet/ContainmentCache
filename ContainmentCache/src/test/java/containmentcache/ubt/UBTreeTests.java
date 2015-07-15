package containmentcache.ubt;

import java.util.Comparator;

import com.google.common.collect.BiMap;

import containmentcache.AContainmentCacheTests;
import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;

public class UBTreeTests extends AContainmentCacheTests {

	@Override
	protected <E, C extends ICacheEntry<E>> IContainmentCache<E, C> getCache(BiMap<E, Integer> permutation, Comparator<E> comparator) {
		return new UBTree<E,C>(comparator);
	}

}
