package containmentcache.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import lombok.RequiredArgsConstructor;

/**
 * Decorates a function to cache the results of its executions.
 * 
 * @author afrechet
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@RequiredArgsConstructor
public class CachedFunctionDecorator<T,R> implements Function<T,R> {

	//The decorated function.
	private final Function<T,R> function;
	
	//The cache.
	private final Map<T, R> cache;
	
	/**
	 * Uses a concurrent hash map as a cache. 
	 * @param function - the function to decorate.
	 */
	public CachedFunctionDecorator(Function<T,R> function) {
		this(function, new ConcurrentHashMap<T,R>());
	}
	
	@Override
	public R apply(T key) {
		final R value;
		if(cache.containsKey(key))
		{
			value = cache.get(key);
		}
		else
		{
			value = function.apply(key);
			cache.put(key, value);
		}
		return value;
	}
	
}
