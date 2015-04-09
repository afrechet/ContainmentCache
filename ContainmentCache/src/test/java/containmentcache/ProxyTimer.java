package containmentcache;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Invocation handler that collections walltime of methods.
 * @author afrechet
 */
public class ProxyTimer implements InvocationHandler{
	
	private final Object obj;
	private final Map<Method,DescriptiveStatistics> stats;
	
	/**
	 * Create a proxy for the given object.
	 * @param o - an object to wrap as proxy.
	 */
	public ProxyTimer(Object o)
	{
		obj = o;
		stats = new HashMap<Method,DescriptiveStatistics>();
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable 
	{
		final long start = System.nanoTime();
		
		final Object result = method.invoke(obj, args);
		
		final long end = System.nanoTime();
		final double duration = (end-start)/(1E6);
		
		final DescriptiveStatistics methodstats = stats.getOrDefault(method,new DescriptiveStatistics());
		methodstats.addValue(duration);
		stats.put(method, methodstats);
		
		return result;
	}
	
	/**
	 * @return a map taking each method encountered to a statistics collector.
	 * 
	 * @see DescriptiveStatistics
	 */
	public Map<Method,DescriptiveStatistics> getMethodStats()
	{
		return Collections.unmodifiableMap(stats);
		
	}
	
	
	
}
