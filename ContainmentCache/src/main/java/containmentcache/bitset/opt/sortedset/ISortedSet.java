package containmentcache.bitset.opt.sortedset;

import java.util.NavigableSet;

/**
 * Provides fundamental data structure to the bitset cache.
 * 
 * A coarser and simpler version of {@link NavigableSet}. 
 * 
 * The only additional  optimization is that the {@link #getNumberLarger(Comparable)} and {@link #getNumberSmaller(Comparable)} should only take constant time.
 * 
 * @author afrechet
 *
 * @param <T>
 */
public interface ISortedSet<T extends Comparable<T>>
{
	/**
	 * @param entry 
	 * @return an iterable over the entries in the set that are larger or equal to the given entry. 
	 */
	public Iterable<T> getLarger(T entry);
	
	/**
	 * @param entry 
	 * @return an iterable over the entries in the set that are smaller or equal to the given entry. 
	 */
	public Iterable<T> getSmaller(T entry);
	
	/**
	 * @param entry 
	 * @return the number of entries in the set that are larger or equal to the given entry. 
	 */
	public int getNumberLarger(T entry);
	
	/**
	 * @param entry 
	 * @return the number of entries in the set that are smaller or equal to the given entry. 
	 */
	public int getNumberSmaller(T entry);
	
	/**
	 * @param entry
	 * @return true if this set did not already contain the specified element.
	 */
	public boolean add(T entry);
	
	/**
	 * @param entry
	 * @return true if this set contained the specified element.
	 */
	public boolean remove(T entry);
	
	/**
	 * @param entry
	 * @return true if and only if the set contains the given entry.
	 */
	public boolean contains(T entry);
	
	/**
	 * @return the number of entries in the set.
	 */
	public int size();
	
	/**
	 * @return true if and only if the set contains no element.
	 */
	public default boolean isEmpty()
	{
		return this.size() == 0;
	}
}