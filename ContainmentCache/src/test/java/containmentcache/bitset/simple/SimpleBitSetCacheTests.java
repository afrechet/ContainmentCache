package containmentcache.bitset.simple;

import java.util.Set;

import containmentcache.AContainmentCacheTests;
import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;
import containmentcache.bitset.simple.SimpleBitSetCache;

public class SimpleBitSetCacheTests extends AContainmentCacheTests {

	@Override
	protected <E extends Comparable<E>,C extends ICacheEntry<E>> IContainmentCache<E,C> getCache(Set<E> universe)
	{
		return new SimpleBitSetCache<E,C>(universe);
	}

}
