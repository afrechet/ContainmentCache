package containmentcache;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.Test;

import com.google.common.base.Stopwatch;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Iterables;

import containmentcache.util.PermutationUtils;
import containmentcache.util.ProxyTimer;

/**
 * Test for thread safety of containment cache.
 *  
 * @author afrechet
 */
@Slf4j
public abstract class AThreadSafeContainmentCacheTests extends AContainmentCacheTests {

	@Override
	protected abstract <E, C extends ICacheEntry<E>> ILockableContainmentCache<E, C> getCache(BiMap<E, Integer> permutation, Comparator<E> comparator);
	
	/**
	 * Busy thread that creates a random set from the universe, check if a superset is present (simulating work as it is doing so), 
	 * and add the set to the cache if no match was found.
	 * 
	 * One of the goal is to simulate the use of containment caching in a multithreaded use case (e.g. SATFC).
	 * 
	 * @author afrechet
	 */
	@RequiredArgsConstructor
	private static class UserThread implements Runnable
	{
		private final BiMap<Integer, Integer> permutation;
		private final ILockableContainmentCache<Integer,ICacheEntry<Integer>> cache;
		private final Random random;
		
		private volatile boolean run = true;
		
		private final static int MAXSLEEPTIME = 500;
		
		/**
		 * Stop the user thread.
		 */
		public void stop()
		{
			run = false;
		}

		@Override
		public void run() {

			while(run)
			{
				//Generate random set of universe.
				final SimpleCacheSet<Integer> entry = Iterables.getOnlyElement(TestUtils.generateRandomSets(random, 1, permutation));
				
				final Lock readlock = cache.getReadLock();
				
				//Check if the entry has a superset.
				Optional<ICacheEntry<Integer>> optionalentry = Optional.ofNullable(null);
				
				log.info("Acquiring cache read lock.");
				readlock.lock();
				try
				{
					log.info("Getting supersets iterable from cache ...");
					Iterable<ICacheEntry<Integer>> supersets = cache.getSupersets(entry);
					log.info("Finding any match from the supersets ...");
					Iterator<ICacheEntry<Integer>> supersetsiterator = supersets.iterator();
					while(supersetsiterator.hasNext())
					{
						ICacheEntry<Integer> superset = supersetsiterator.next();
						
						//Busy work
						try {
							Thread.sleep(random.nextInt(MAXSLEEPTIME));
						} catch (InterruptedException e) {
							log.warn("User thread sleep interrupted.");
						}
						
						optionalentry = Optional.of(superset);
						break;
					}
				}
				finally
				{
					log.info("Unlocking cache.");
					readlock.unlock();
				}
				
				//If a superset, then success, else add the entry to the cache.
				if(!optionalentry.isPresent())
				{
					log.info("Did not find a supet set in the cache, adding entry to cache.");
					cache.add(entry);
				}
				else
				{
					log.info("Found a supersets in the cache.");
				}
			}
		}
		
	}
	
	/**
	 * Smoke tests.
	 */
	@Test
	public void threadSafetySmokeTest()
	{
		System.out.println("Smoke testing thread safety.");
		
		//Parameters
		final long seed = 1;
		final Random rand = new Random(seed);

		//Number of consumer threads.
		final int numconsumers = 30;
		//Cutoff time for experiment in seconds.
		final int cutoff = 10;
		//Size of universe.
		final int N = 300;
		
		final Set<Integer> universe = IntStream.rangeClosed(0, N-1).boxed().collect(Collectors.toSet());
		ImmutableBiMap<Integer, Integer> permutation = PermutationUtils.makePermutation(universe);
		System.out.println("Universe has "+N+" elements ("+(int) Math.pow(2, N)+" possible sets).");
		
		//Create cache and wrap with timer proxy.
		final ILockableContainmentCache<Integer,ICacheEntry<Integer>> undecoratedcache = getCache(permutation, COMPARATOR);
		final ProxyTimer timer = new ProxyTimer(undecoratedcache);
		@SuppressWarnings("unchecked")
		final ILockableContainmentCache<Integer,ICacheEntry<Integer>> cache = (ILockableContainmentCache<Integer,ICacheEntry<Integer>>) Proxy.newProxyInstance(ILockableContainmentCache.class.getClassLoader(), new Class[] {ILockableContainmentCache.class}, timer);
		
		final Stopwatch watch = Stopwatch.createStarted();
		
		System.out.println("Spawning "+numconsumers+" user threads...");
		
		final List<UserThread> users = new LinkedList<UserThread>();
		final ExecutorService executor = Executors.newFixedThreadPool(numconsumers);
		for(int c=0;c<numconsumers;c++)
		{
			final UserThread user = new UserThread(permutation, cache, rand);
			users.add(user);
			executor.submit(user);
		}
		executor.shutdown();
		System.out.println("Waiting "+cutoff+" seconds ...");
		try {
			executor.awaitTermination(cutoff, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new IllegalStateException("Executor service was interrupted while awaiting termination.", e);
		}
		
		System.out.println("Interrupting user threads ...");
		
		for(UserThread user : users)
		{
			user.stop();
		}
		
		executor.shutdownNow();
		System.out.println("Terminated testing.");
		
		watch.stop();
		final long totalduration = watch.elapsed(TimeUnit.MILLISECONDS);
		
		System.out.println("");
		System.out.println("Runtime (ms) statistics:");
		System.out.printf("%-30s %-10s %-10s %-10s %-10s %-10s %-10s %-10s\n","Method","Mean","StdDev","Min","Q25","Median","Q75","Max");
		
		//Unaligned for CSVs.
		/*
		System.out.printf("%s,%s,%s,%s,%s,%s,%s,%s\n","Method","Mean","StdDev","Min","Q25","Median","Q75","Max");
		 */
		
		final Map<Method,DescriptiveStatistics> stats = timer.getMethodStats();
		
		final List<Method> methods = new LinkedList<Method>(stats.keySet());
		Collections.sort(methods,new Comparator<Method>(){
			@Override
			public int compare(Method o1, Method o2) {
				return o1.getName().compareTo(o2.getName());
		}});
		
		for(Method method : methods)
		{
			final DescriptiveStatistics stat = stats.get(method);
			
			System.out.printf("%-30s %-10.3f %-10.3f %-10.3f %-10.3f %-10.3f %-10.3f %-10.3f\n",
					"\""+method.getName()+"\"",
					stat.getMean(),
					stat.getStandardDeviation(),
					stat.getMin(),
					stat.getPercentile(25),
					stat.getPercentile(50),
					stat.getPercentile(75),
					stat.getMax());
			
			//Unaligned for CSVs.
			/*
			System.out.printf("%s,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f\n",
					"\""+method.getName()+"\"",
					stat.getMean(),
					stat.getStandardDeviation(),
					stat.getMin(),
					stat.getPercentile(25),
					stat.getPercentile(50),
					stat.getPercentile(75),
					stat.getMax());
			*/
		}
		
		System.out.printf("Total time: %s\n",DurationFormatUtils.formatDuration(totalduration, "HH:mm:ss.S"));
		
	}

	
}
