package containmentcache;

import java.util.Collection;

import net.jcip.annotations.NotThreadSafe;

/**
 * Organizes cache entries ({@link ICacheEntry}) so that it easy to obtain cache entries that are subsets/supersets
 * of a query cache entry (according to the sets of elements they represent). 
 * 
 * Should support bucketing of cache entries, meaning that if two cache entries represent the same set (i.e. have the same elements)
 * but are not equal (according to {@code equals()}), both entries will be contained in the cache and returned on query.
 * 
 * Implementations are not required to be thread safe. Specifically, note that sub/supersets are returned as iterable, and thus it is up to the
 * user (or decorator) to guarantee some kind of synchronization on the cache when iterating over entries.
 * 
 * @author afrechet
 *
 * @param <E> - type of elements in set representing entry.
 * @param <C> - type of cache entry.
 */
public interface IContainmentCache<E,C extends ICacheEntry<E>> {
	
	/**
	 * @param set - set to add to the cache.
	 */
	public void add(C set);
	
	/**
	 * @param sets - sets to add to the cache.
	 */
	public default void addAll(Collection<C> sets)
	{
		for(C set : sets)
		{
			this.add(set);
		}
	}
	
	/**
	 * @param set - set to remove from the cache.
	 */
	public void remove(C set);
	
	/**
	 * @param set - set to check for presence in the cache.
	 * @return true if and only if the given set is in the cache.
	 */
	public boolean contains(C set);
	
	/**
	 * @param set - set for which to get all present subsets in the cache.
	 * @return an iterable over every set currently in the cache that is a subset of the given set.
	 */
	public Iterable<C> getSubsets(C set);
	
	/**
	 * 
	 * @param set - set for which to get the number of present subsets in the cache.
	 * @return the number of subsets present in the cache for the given set.
	 */
	public int getNumberSubsets(C set);
	
	/**
	 * @param set - set for which to get all present supersets in the cache.
	 * @return an iterable over every set currently in the cache that is a superset of the given set.
	 */
	public Iterable<C> getSupersets(C set);
	
	/**
	 * 
	 * @param set - set for which to get the number of present supersets in the cache.
	 * @return the number of supersets present in the cache for the given set.
	 */
	public int getNumberSupersets(C set);
	
	/**
	 * @return the number of entries currently in the cache.
	 */
	public int size();
	
}
