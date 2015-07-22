//package containmentcache.decorators;
//
//import java.util.Set;
//
//import containmentcache.AThreadSafeContainmentCacheTests;
//import containmentcache.ICacheEntry;
//import containmentcache.IContainmentCache;
//import containmentcache.ILockableContainmentCache;
//import containmentcache.bitset.simple.SimpleBitSetCache;
//
//public class ThreadSafeContainmentCacheDecoratorTests extends AThreadSafeContainmentCacheTests{
//
//	@Override
//	protected <E extends Comparable<E>, C extends ICacheEntry<E>> ILockableContainmentCache<E, C> getCache(
//			Set<E> universe) {
//		final IContainmentCache<E, C> cache = new SimpleBitSetCache<>(universe);
//		return ThreadSafeContainmentCacheDecorator.makeThreadSafe(cache);
//	}
//
//}
