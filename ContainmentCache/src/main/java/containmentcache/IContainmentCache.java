package containmentcache;

import java.util.Collection;
import java.util.Set;

public interface IContainmentCache<E> {
	
	/**
	 * @param set - set to add to the tree.
	 */
	public void add(ICacheSet<E> set);
	
	/**
	 * @param set - set to remove from the tree.
	 */
	public void remove(ICacheSet<E> set);
	
	/**
	 * @param set - set to check for presence in the tree.
	 * @return true if and only if the given set is in the tree.
	 */
	public boolean contains(ICacheSet<E> set);
	
	/**
	 * @param set - set for which to get all present subsets in the tree.
	 * @return every set currently in the tree that is a subset of the given set.
	 */
	public Collection<Set<E>> getSubsets(ICacheSet<E> set);
	
	/**
	 * 
	 * @param set - set for which to get the number of present subsets in the tree.
	 * @return the number of subsets present in the tree for the given set.
	 */
	public int getNumberSubsets(ICacheSet<E> set);
	
	/**
	 * @param set - set for which to get all present supersets in the tree.
	 * @return every set currently in the tree that is a superset of the given set.
	 */
	public Collection<Set<E>> getSupersets(ICacheSet<E> set);
	
	/**
	 * 
	 * @param set - set for which to get the number of present supersets in the tree.
	 * @return the number of supersets present in the tree for the given set.
	 */
	public int getNumberSupersets(ICacheSet<E> set);
	
	/**
	 * @return the number of set currently in the tree.
	 */
	public int size();
	
}
