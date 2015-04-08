package containmentcache;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Invocation handler that 
 * @author afrechet
 *
 */
public class ProxyTimer implements InvocationHandler{
	
	private final Object obj;
	
	private final Map<Method,Double> times;
	private final Map<Method,Integer> nums;
	
	public ProxyTimer(Object o)
	{
		obj = o;
		times = new HashMap<Method,Double>();
		nums = new HashMap<Method,Integer>();
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable 
	{
		final long start = System.nanoTime();
		
		final Object result = method.invoke(obj, args);
		
		final long end = System.nanoTime();
		final double duration = (end-start)/(1E6);
		
		times.put(method, times.getOrDefault(method, 0.0) + duration);
		nums.put(method, nums.getOrDefault(method, 0) + 1);
		
		return result;
	}
	
	public Map<String,Double> getAverageTimes()
	{
		assert times.keySet().equals(nums.keySet());
		
		final Map<String,Double> averages = new HashMap<String,Double>();
		
		for(Method method : times.keySet())
		{
			final String name = method.getName();
			
			if(averages.containsKey(name))
			{
				throw new IllegalStateException("Method with name \""+name+"\" appears twice in timing data.");
			}
			
			averages.put(name, times.get(method) / nums.get(method));		
		}
		
		return averages;
		
	}
	
	
	
}
