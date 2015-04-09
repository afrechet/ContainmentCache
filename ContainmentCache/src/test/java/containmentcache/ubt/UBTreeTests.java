package containmentcache.ubt;

import java.util.Set;

import containmentcache.AContainmentCacheTests;
import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;

public class UBTreeTests extends AContainmentCacheTests{

	@Override
	protected <E extends Comparable<E>,C extends ICacheEntry<E>> IContainmentCache<E,C> getCache(Set<E> universe)
	{
		return new UBTree<E,C>();
	}

}
