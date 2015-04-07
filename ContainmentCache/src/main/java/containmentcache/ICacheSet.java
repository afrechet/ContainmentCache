package containmentcache;

import java.util.Set;

/**
 * An abstraction for the queries to a containment cache.
 * @author afrechet
 *
 * @param <E> - elements available in a containment cache.
 */
public interface ICacheSet<E> {
	
	public int size();
	
	public Set<E> getElements();
	
}
