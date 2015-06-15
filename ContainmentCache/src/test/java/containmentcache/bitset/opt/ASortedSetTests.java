package containmentcache.bitset.opt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.NavigableSet;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.Test;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import containmentcache.TestUtils;
import containmentcache.util.ProxyTimer;

/**
 * Test for sorted sets.
 * @author afrechet
 */
public abstract class ASortedSetTests {
	
	//Test parameters.
	private final static boolean CSV_OUTPUT = false;
	
	protected abstract <E extends Comparable<E>> ISortedSet<E> getSortedSet();
	
	@Test
	public void testEmptySortedSet()
	{
		ISortedSet<Integer> set = getSortedSet();
		
		assertEquals(0,set.size());
		assertTrue(set.isEmpty());
		
		assertFalse(set.contains(5));
		
		assertTrue(Lists.newLinkedList(set.getSmaller(5)).isEmpty());
		assertEquals(0,set.getNumberSmaller(5));
		
		assertTrue(Lists.newLinkedList(set.getLarger(5)).isEmpty());
		assertEquals(0,set.getNumberLarger(5));
	}
	
	@Test
	public void testIdempotence()
	{
		ISortedSet<Integer> set = getSortedSet();
		
		set.add(1);
		
		assertTrue(set.contains(1));
		assertTrue(Lists.newLinkedList(set.getSmaller(3)).contains(1));
		assertTrue(Lists.newLinkedList(set.getSmaller(1)).contains(1));
		assertTrue(Lists.newLinkedList(set.getSmaller(0)).isEmpty());
		
		assertTrue(Lists.newLinkedList(set.getLarger(3)).isEmpty());
		assertTrue(Lists.newLinkedList(set.getLarger(1)).contains(1));
		assertTrue(Lists.newLinkedList(set.getLarger(0)).contains(1));
		
		assertFalse(set.contains(3));
		
		assertEquals(1, set.size());
		
		set.add(1);
		
		assertTrue(set.contains(1));
		assertTrue(Lists.newLinkedList(set.getSmaller(3)).contains(1));
		assertTrue(Lists.newLinkedList(set.getSmaller(1)).contains(1));
		assertTrue(Lists.newLinkedList(set.getSmaller(0)).isEmpty());
		
		assertTrue(Lists.newLinkedList(set.getLarger(3)).isEmpty());
		assertTrue(Lists.newLinkedList(set.getLarger(1)).contains(1));
		assertTrue(Lists.newLinkedList(set.getLarger(0)).contains(1));
		
		assertFalse(set.contains(3));
		
		assertEquals(1, set.size());
	}
	
	@Test
	public void testAddthenRemove()
	{
		ISortedSet<Integer> set = getSortedSet();
		
		assertTrue(set.add(1));
		assertFalse(set.isEmpty());
		
		assertTrue(set.remove(1));
		
		assertFalse(set.contains(1));
		assertTrue(set.isEmpty());
		
	}
	
	@Test
	public void testSimple()
	{
		final ISortedSet<Integer> set = getSortedSet();
		final int n = 10;
		
		for(int i=1;i<=n;i++)
		{
			assertTrue(set.add(i));
		}
		
		assertFalse(set.isEmpty());
		assertEquals(n,set.size());
		
		for(int i=1;i<=n;i++)
		{
			assertTrue(set.contains(i));
			
			final Collection<Integer> smallers = Lists.newLinkedList(set.getSmaller(i));
			
			assertEquals(i,smallers.size());
			assertEquals(i,set.getNumberSmaller(i));
			
			for(int j=1;j<=i;j++)
			{
				assertTrue(smallers.contains(j));
			}
			
			final Collection<Integer> largers = Lists.newLinkedList(set.getLarger(i));
			
			assertEquals(n-i+1,largers.size());
			assertEquals(n-i+1,set.getNumberLarger(i));
			
			for(int j=i;j<=n;j++)
			{
				assertTrue(largers.contains(j));
			}
		}
	}
	
	/**
	 * Smoke tests.
	 */
	@Test
	public void smokeTest()
	{
		NavigableSet<Integer> slowset = new TreeSet<Integer>();
		
		System.out.println("SMOKE TESTS");
		
		//Parameters
		final long seed = 5;
		final Random rand = new Random(seed);

		final int numtests = 1000;
		final int loadincrement = 50;
		final int N = 10000;
		
		//Create cache and wrap with timer proxy.
		final ISortedSet<Integer> undecoratedset = getSortedSet();
		final ProxyTimer timer = new ProxyTimer(undecoratedset);
		@SuppressWarnings("unchecked")
		final ISortedSet<Integer> set = (ISortedSet<Integer>) Proxy.newProxyInstance(ISortedSet.class.getClassLoader(), new Class[] {ISortedSet.class}, timer);
		
		System.out.print("Load testing "+numtests+" times...");
		final Stopwatch watch = Stopwatch.createStarted();
		for(int t=0;t<numtests;t++)
		{
			if(t%(numtests/10) == 0)
			{
				System.out.print(((double) t)/((double) numtests)*100.0+"% ("+set.size()+")...");
			}
			
			//Add some load to the data structure.
			for(int m=0;m<loadincrement;m++)
			{
				final int element = rand.nextInt(N);
				set.add(element);
				slowset.add(element);
			}
			//Test a certain set.
			final int element = rand.nextInt(N);
			
			//Test addition.
			set.add(element);
			slowset.add(element);
			
			assertTrue(set.contains(element));
			final int size = set.size();
			assertEquals(slowset.size(),size);
			
			//Test smallers
			final Collection<Integer> smallers = Lists.newLinkedList(set.getSmaller(element));
			assertTrue(smallers.contains(element));
			
			final Collection<Integer> truesmallers = slowset.headSet(element,true);
			assertTrue(smallers.containsAll(truesmallers) && truesmallers.containsAll(smallers));
			
			//Check that they smallers are sorted.
			assertTrue(Ordering.natural().isOrdered(smallers));
			
			//Test number smallers
			final int numsmallers = set.getNumberSmaller(element);
			assertEquals(numsmallers, smallers.size());
			
			//Test largers
			final Collection<Integer> largers = Lists.newLinkedList(set.getLarger(element));
			assertTrue(largers.contains(element));
			
			final Collection<Integer> truelargers = slowset.tailSet(element,true);
			assertTrue(largers.containsAll(truelargers) && truelargers.containsAll(largers));
			
			//Check that they smallers are sorted.
			assertTrue(Ordering.natural().isOrdered(largers));
			
			//Test number largers
			final int numlargers = set.getNumberLarger(element);
			assertEquals(numlargers, largers.size());
			
			//Test removal.
			assertTrue(set.remove(element));
			slowset.remove(element);
			
			assertFalse(set.contains(element));
			assertFalse(Lists.newLinkedList(set.getSmaller(element)).contains(element));
			assertFalse(Lists.newLinkedList(set.getLarger(element)).contains(element));
			assertEquals(size-1,set.size());
		}
		watch.stop();
		final long totalduration = watch.elapsed(TimeUnit.MILLISECONDS);
		
		System.out.println("");
		TestUtils.printMethodRuntimes(timer.getMethodStats(),CSV_OUTPUT);
		System.out.printf("Total time: %s\n",DurationFormatUtils.formatDuration(totalduration, "HH:mm:ss.S"));
		
	}
	
	
}
