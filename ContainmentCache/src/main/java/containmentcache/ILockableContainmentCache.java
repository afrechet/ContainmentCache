package containmentcache;

import java.util.concurrent.locks.Lock;

/**
 * Provide read lock access to containment cache to allow for concurrent reading & modification of the data struct.re
 * @author afrechet
 *
 * @param <E> - type of elements in set representing entry.
 * @param <C> - type of cache entry.
 */
public interface ILockableContainmentCache<E,C extends ICacheEntry<E>> extends IContainmentCache<E,C> {

	/**
	 * @return the cache's read lock to allow entry to the data structure, and most importantly lock when using iterable coming from the data structure.
	 */
	public Lock getReadLock();
	
}
