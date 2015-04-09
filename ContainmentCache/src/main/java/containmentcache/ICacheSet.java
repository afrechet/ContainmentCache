package containmentcache;

import java.util.Set;

/**
 * An abstraction for the queries to a containment cache.
 * @author afrechet
 *
 * @param <E> - elements available in a containment cache.
 */
public interface ICacheSet<E,T> {
	
	/**
	 * @return the set element to which the cache corresponds.
	 */
	public Set<E> getElements();
	
	/**
	 * @return additional entry content.
	 */
	public T getContent();
	
}
