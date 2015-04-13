package containmentcache.decorators;

import java.util.Set;

import containmentcache.AContainmentCacheTests;
import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;
import containmentcache.bitset.SimpleBitSetCache;

public class BufferedThreadSafeContainmentCacheDecoratorTests extends AContainmentCacheTests {

	@Override
	protected <E extends Comparable<E>, C extends ICacheEntry<E>> IContainmentCache<E, C> getCache(
			Set<E> universe) {
		final IContainmentCache<E, C> cache = new SimpleBitSetCache<>(universe);
		return BufferedThreadSafeContainmentCacheDecorator.makeBufferedThreadSafe(cache, 100); 
	}

}
