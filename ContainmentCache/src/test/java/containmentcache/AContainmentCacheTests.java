package containmentcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.Test;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import containmentcache.util.ProxyTimer;

/**
 * Tests for containment caches.
 * @author afrechet
 */
public abstract class AContainmentCacheTests {
	
	//Test parameters.
	private final static boolean CSV_OUTPUT = false;
	
	//Test objects.
	private final static Set<Integer> UNIVERSE = Collections.unmodifiableSet(new HashSet<Integer>(Arrays.asList(0,1,2,3,4,5,6,7,8,9,10)));
	
	/**
	 * Factory method for the containment cache to be tested.
	 * @param universe - the set of elements the cache is for. 
	 * @return - containment cache instance to be tested.
	 */
	protected abstract <E extends Comparable<E>,C extends ICacheEntry<E>> IContainmentCache<E,C> getCache(Set<E> universe);
	
	
	/**
	 * @param elements - integer elements.
	 * @return the set containing the given elements.
	 */
	private CacheSet<Integer> makeSet(int... elements)
	{
		final Set<Integer> set = new HashSet<Integer>();
		for(int element : elements)
		{
			set.add(element);
		}
		return new CacheSet<Integer>(set);
	}
	
	/**
	 * Creation tests.
	 */
	@Test 
	public void testEmptyTree()
	{
		final IContainmentCache<Integer,ICacheEntry<Integer>> cache = getCache(UNIVERSE);
		
		//Empty tree has size 0.
		assertEquals(cache.size(), 0);
		
		//Empty tree does not contain empty set.
		ICacheEntry<Integer> emptySet = makeSet();
		assertTrue(Lists.newLinkedList(cache.getSubsets(emptySet)).isEmpty());
		assertTrue(Lists.newLinkedList(cache.getSupersets(emptySet)).isEmpty());
		assertFalse(cache.contains(emptySet));
		
		//Empty tree does not contain any subset.
		assertTrue(Lists.newLinkedList(cache.getSubsets(makeSet(1,2,3))).isEmpty());
		
		//Empty tree does not contain any superset.
		assertTrue(Lists.newLinkedList(cache.getSupersets(makeSet(1,2,3))).isEmpty());
	}
	
	/**
	 * Subset and superset tests.
	 **/
	
	@Test 
	public void testEmptySet()
	{
		final IContainmentCache<Integer,ICacheEntry<Integer>> cache = getCache(UNIVERSE);
		
		ICacheEntry<Integer> S = makeSet();
		cache.add(S);
		
		Collection<ICacheEntry<Integer>> subsets;
		Collection<ICacheEntry<Integer>> supersets;
		
		subsets = Lists.newLinkedList(cache.getSubsets(S));
		assertEquals(subsets.size(),1);
		assertTrue(subsets.contains(S));
		supersets = Lists.newLinkedList(cache.getSupersets(S));
		assertEquals(supersets.size(),1);
		assertTrue(supersets.contains(S));
		
		ICacheEntry<Integer> R = makeSet(1,2,3,4);
		cache.add(R);
		
		subsets = Lists.newLinkedList(cache.getSubsets(R));
		assertEquals(subsets.size(),2);
		assertEquals(cache.getNumberSubsets(R),subsets.size());
		assertTrue(subsets.contains(S));
		assertTrue(subsets.contains(R));
		
		supersets = Lists.newLinkedList(cache.getSupersets(S));
		assertEquals(supersets.size(),2);
		assertEquals(cache.getNumberSupersets(S),supersets.size());
		assertTrue(supersets.contains(S));
		assertTrue(supersets.contains(R));
		
	}
	
	@Test
	public void testIdempotence()
	{
		final IContainmentCache<Integer,ICacheEntry<Integer>> cache = getCache(UNIVERSE);
		
		final ICacheEntry<Integer> S = makeSet(1,2,3);
		
		cache.add(S);
		
		Collection<ICacheEntry<Integer>> subsets;
		Collection<ICacheEntry<Integer>> supersets;
		
		assertEquals(1,cache.size());
		
		subsets = Lists.newLinkedList(cache.getSubsets(S));
		assertTrue(subsets.contains(S));
		assertEquals(subsets.size(),1); 
		assertEquals(cache.getNumberSubsets(S),subsets.size());
		
		supersets = Lists.newLinkedList(cache.getSupersets(S));
		assertTrue(supersets.contains(S));
		assertEquals(1,supersets.size());
		assertEquals(cache.getNumberSupersets(S),supersets.size());
		
		cache.add(S);
		
		assertEquals(1,cache.size());
		
		subsets = Lists.newLinkedList(cache.getSubsets(S));
		assertTrue(subsets.contains(S));
		assertEquals(subsets.size(),1);
		assertEquals(subsets.size(),cache.getNumberSubsets(S));
		
		supersets = Lists.newLinkedList(cache.getSupersets(S));
		assertTrue(supersets.contains(S));
		assertEquals(1,supersets.size());
		assertEquals(cache.getNumberSupersets(S),supersets.size());
		
	}
	
	@Test 
	public void testOneSubset()
	{
		final IContainmentCache<Integer,ICacheEntry<Integer>> cache = getCache(UNIVERSE);
		
		final Collection<ICacheEntry<Integer>> nosubsets = Lists.newLinkedList(cache.getSubsets(makeSet(1,2,3,4)));
		assertTrue(nosubsets.isEmpty());
		
		ICacheEntry<Integer> s1 = makeSet(1,2);
		cache.add(s1);
		final Collection<ICacheEntry<Integer>> onesubsets = Lists.newLinkedList(cache.getSubsets(makeSet(1,2,3,4)));
		assertEquals(onesubsets.size(),1);
		assertTrue(onesubsets.contains(s1));
		assertEquals(cache.getNumberSubsets(makeSet(1,2,3,4)),onesubsets.size());
	}
	
	@Test 
	public void testOneSuperset()
	{
		final IContainmentCache<Integer,ICacheEntry<Integer>> cache = getCache(UNIVERSE);
		
		final Collection<ICacheEntry<Integer>> nosupersets = Lists.newLinkedList(cache.getSupersets(makeSet(1,2)));
		assertTrue(nosupersets.isEmpty());
		
		ICacheEntry<Integer> s1 = makeSet(1,2,3,4);
		cache.add(s1);
		final Collection<ICacheEntry<Integer>> onesubsets = Lists.newLinkedList(cache.getSupersets(makeSet(1,2)));
		int numsupersets = cache.getNumberSupersets(makeSet(1,2));
		assertEquals(numsupersets,1);
		assertEquals(onesubsets.size(),numsupersets);
		assertTrue(onesubsets.contains(s1));
	}
	
	@Test
	public void testIntersectingSubsets()
	{
		final IContainmentCache<Integer,ICacheEntry<Integer>> cache = getCache(UNIVERSE);
	
		final ICacheEntry<Integer> s1 = makeSet(1,2);
		cache.add(s1);	
		final ICacheEntry<Integer> s2 = makeSet(2,3);
		cache.add(s2);
		
		final Collection<ICacheEntry<Integer>> subsets = Lists.newLinkedList(cache.getSubsets(makeSet(1,2,3,4)));		
		int numsubsets = cache.getNumberSubsets(makeSet(1,2,3,4));
		
		assertEquals(2,numsubsets);
		assertEquals(subsets.size(),numsubsets);
		assertTrue(subsets.contains(s1));
		assertTrue(subsets.contains(s2));
	}
	
	@Test
	public void testNestedSubsets()
	{
		final IContainmentCache<Integer,ICacheEntry<Integer>> cache = getCache(UNIVERSE);
	
		final ICacheEntry<Integer> s1 = makeSet(1);
		cache.add(s1);	
		final ICacheEntry<Integer> s2 = makeSet(1,2);
		cache.add(s2);
		
		final Collection<ICacheEntry<Integer>> subsets = Lists.newLinkedList(cache.getSubsets(makeSet(1,2,3,4)));
		
		assertEquals(subsets.size(),2);
		assertTrue(subsets.contains(s1));
		assertTrue(subsets.contains(s2));
	}
	
	@Test 
	public void testNestedSupersets()
	{
		final IContainmentCache<Integer,ICacheEntry<Integer>> cache = getCache(UNIVERSE);
		
		final ICacheEntry<Integer> s1 = makeSet(1,2);
		cache.add(s1);	
		final ICacheEntry<Integer> s2 = makeSet(1,2,3);
		cache.add(s2);
		
		final Collection<ICacheEntry<Integer>> supersets = Lists.newLinkedList(cache.getSupersets(makeSet(1)));
		
		
		assertEquals(supersets.size(),2);
		assertTrue(supersets.contains(s1));
		assertTrue(supersets.contains(s2));
	}
	
	/**
	 * Addition & removal tests.
	 */
	@Test
	public void testAddThenRemove()
	{
		final IContainmentCache<Integer,ICacheEntry<Integer>> cache = getCache(UNIVERSE);
		
		final ICacheEntry<Integer> S = makeSet(1,2,3);
		
		cache.add(S);
		assertEquals(cache.size(), 1);
		assertTrue(cache.contains(S));
		
		cache.remove(S);
		assertEquals(cache.size(), 0);
		assertFalse(cache.contains(S));
	}
	
	
	/**
	 * Smoke tests.
	 */
	@Test
	public void smokeTest()
	{
		System.out.println("SMOKE TESTS");
		
		//Parameters
		final long seed = 1;
		final Random rand = new Random(seed);

		final int numtests = 100;
		final int loadincrement = 50;
		final int N = 750;
		
		
		final List<Integer> universe = new ArrayList<Integer>();
		for(int i=0;i<N;i++)
		{
			universe.add(i);
		}
		System.out.println("Universe has "+N+" elements ("+(int) Math.pow(2, N)+" possible sets).");
		
		//Create cache and wrap with timer proxy.
		final IContainmentCache<Integer,ICacheEntry<Integer>> undecoratedcache = getCache(new HashSet<Integer>(universe));
		final ProxyTimer timer = new ProxyTimer(undecoratedcache);
		@SuppressWarnings("unchecked")
		final IContainmentCache<Integer,ICacheEntry<Integer>> cache = (IContainmentCache<Integer,ICacheEntry<Integer>>) Proxy.newProxyInstance(IContainmentCache.class.getClassLoader(), new Class[] {IContainmentCache.class}, timer);
		
		System.out.print("Load testing "+numtests+" times...");
		final Stopwatch watch = Stopwatch.createStarted();
		for(int t=0;t<numtests;t++)
		{
			if(t%(numtests/10) == 0)
			{
				System.out.print(((double) t)/((double) numtests)*100.0+"% ("+undecoratedcache.size()+")...");
			}
			
			//Add some load to the data structure.
			for(int m=0;m<loadincrement;m++)
			{
				Collections.shuffle(universe, rand);
				List<Integer> elements = universe.subList(0, rand.nextInt(universe.size()));
				ICacheEntry<Integer> set = new CacheSet<Integer>(new HashSet<Integer>(elements));
				
				cache.add(set);
			}
			
			//Test a certain set.
			Collections.shuffle(universe, rand);
			List<Integer> elements = universe.subList(0, rand.nextInt(universe.size()));
			ICacheEntry<Integer> set = new CacheSet<Integer>(new HashSet<Integer>(elements));
			
			//Test addition.
			cache.add(set);
			assertTrue(cache.contains(set));
			assertTrue(Lists.newLinkedList(cache.getSubsets(set)).contains(set));
			assertTrue(Lists.newLinkedList(cache.getSupersets(set)).contains(set));
			final int size = cache.size();
			
			//Test subsets
			final Collection<ICacheEntry<Integer>> subsets = Lists.newLinkedList(cache.getSubsets(set));
			
			//Test number subsets
			final int numsubsets = cache.getNumberSubsets(set);
			assertEquals(subsets.size(), numsubsets);
			
			//Test supersets
			final Collection<ICacheEntry<Integer>> supersets = Lists.newLinkedList(cache.getSupersets(set));
			
			//Test number supersets
			final int numsupersets = cache.getNumberSupersets(set);
			assertEquals(numsupersets, supersets.size());
			
			
			//Test removal.
			cache.remove(set);
			assertFalse(cache.contains(set));
			assertFalse(Lists.newLinkedList(cache.getSubsets(set)).contains(set));
			assertFalse(Lists.newLinkedList(cache.getSupersets(set)).contains(set));
			assertEquals(cache.size(), size-1);
		}
		watch.stop();
		final long totalduration = watch.elapsed(TimeUnit.MILLISECONDS);
		
		System.out.println("");
		TestUtils.printMethodRuntimes(timer.getMethodStats(),CSV_OUTPUT);
		System.out.printf("Total time: %s\n",DurationFormatUtils.formatDuration(totalduration, "HH:mm:ss.S"));
		
	}
	
	
	

}
