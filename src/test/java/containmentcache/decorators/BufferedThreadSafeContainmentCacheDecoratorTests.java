package containmentcache.decorators;

import java.util.Comparator;

import com.google.common.collect.BiMap;

import containmentcache.AThreadSafeContainmentCacheTests;
import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;
import containmentcache.ILockableContainmentCache;
import containmentcache.bitset.simple.SimpleBitSetCache;

public class BufferedThreadSafeContainmentCacheDecoratorTests extends AThreadSafeContainmentCacheTests {

	@Override
	protected <E, C extends ICacheEntry<E>> ILockableContainmentCache<E, C> getCache(BiMap<E, Integer> permutation, Comparator<E> comparator) {
		final IContainmentCache<E, C> cache = new SimpleBitSetCache<>(permutation);
		return BufferedThreadSafeCacheDecorator.makeBufferedThreadSafe(cache, 100); 
	}

}
