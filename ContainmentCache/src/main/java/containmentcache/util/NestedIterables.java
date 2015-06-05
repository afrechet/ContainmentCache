package containmentcache.util;

import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.Iterables;

/**
 * Helper method to obtain a two-level iterable, first over a key space, then over value iterator for given key.
 * 
 * This the iterable's element corresponds to the secondary iterator's element, where that choice of the latter
 * iterator corresponds to the primary iterator's current element (the relationship being given by
 * the secondary iterator's function). 
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
public class NestedIterables{
	
	/**
	 * Nest a secondary iterator according to a primary iterator.
	 * @param primaryIterator - an iterable over keys.
	 * @param secondaryIteratorsFunction - a function from keys to secondary iterables.
	 * @return the concatenation of secondary iterables according to the order given by the primary iterator.
	 */
	public static <K,T> Iterable<T> nest(Iterable<K> primaryIterable, Function<K,? extends Iterable<T>> secondaryIteratorsFunction){
		Iterable<Iterable<T>> iterables = Iterables.transform(primaryIterable, key -> secondaryIteratorsFunction.apply(key));
		return Iterables.concat(iterables);
	}
	public static <K,T> Iterable<T> nest(Iterable<K> primaryIterator, Map<K,? extends Iterable<T>> secondaryIteratorsMap){
		return nest(primaryIterator, key -> secondaryIteratorsMap.get(key));
	}
	
}
