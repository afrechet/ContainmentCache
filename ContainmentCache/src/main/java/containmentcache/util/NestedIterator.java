package containmentcache.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * Two-level iterator, first over a key space, then over value pair for given key.
 * 
 * This iterator's element corresponds to the secondary iterator's element, where that latter
 * iterator corresponds to the primary iterator's current element (the relationship being given by
 * the secondary iterator map). 
 * 
 * For example, given the following primary list-iterator and secondary list-iterators map:
 * 
 * [1,2,3],{1:['a','A'],2:['b','B'],3:['c','C']}
 * 
 * then the nested iterator would correspond to:
 * 
 * ['a','A','b','B','c','C']
 * 
 * @author afrechet
 *
 * @param <T> - secondary iterator type.
 */
public class NestedIterator<T> implements Iterator<T>{

	private final Map<?,? extends Iterable<T>> fSecondaryIterators;
	
	private final Iterator<?> fPrimaryIterator;
	private Iterator<T> fSecondaryIterator;
	
	/**
	 * @param primaryiterator - a primary level iterator.
	 * @param secondaryiterators - a map taking primary level keys to secondary level iterators.
	 * @param <K> - the key space on which the primary iterator and secondary iterators map must agree.
	 */
	public <K> NestedIterator(Iterator<K> primaryiterator, Map<K,? extends Iterable<T>> secondaryiterators)
	{
		fSecondaryIterators = secondaryiterators;
		fPrimaryIterator = primaryiterator;
		fSecondaryIterator = Collections.emptyIterator();
	}
	
	@Override
	public boolean hasNext() {
		
		nextSecondary();
		return fSecondaryIterator.hasNext();
		
	}

	@Override
	public T next() {
		
		nextSecondary();
		return fSecondaryIterator.next();
		
	}
	
	/**
	 * Updates the secondary iterator if the current one has no more elements and there is a next secondary iterator.
	 */
	private void nextSecondary()
	{
		while(fPrimaryIterator.hasNext() && !fSecondaryIterator.hasNext())
		{
			final Iterable<T> secondaryiterable = fSecondaryIterators.get(fPrimaryIterator.next());
			if(secondaryiterable != null)
			{
				fSecondaryIterator = secondaryiterable.iterator();
			}
			else
			{
				throw new IllegalStateException("Secondary iterator maps had no iterable for given primary iterator element.");
			}
		}
		
	}
	
}
