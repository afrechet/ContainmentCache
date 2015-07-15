package containmentcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.Test;

import com.google.common.base.Stopwatch;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;

import containmentcache.util.PermutationUtils;
import containmentcache.util.ProxyTimer;

/**
 * Tests for containment caches.
 *
 * @author afrechet
 */
@Slf4j
public abstract class AContainmentCacheTests {

    //Test parameters.
    private final static boolean CSV_OUTPUT = true;

    //Test objects.
    private final static Set<Integer> UNIVERSE = Collections.unmodifiableSet(IntStream.rangeClosed(1, 2173).boxed().collect(Collectors.toSet()));
    protected final static ImmutableBiMap<Integer, Integer> PERMUTATION = PermutationUtils.makePermutation(UNIVERSE);
    protected final static Comparator<Integer> COMPARATOR = (a, b) -> a.compareTo(b);
    
    /**
     * Factory method for the containment cache to be tested.
     *
     * @param universe - the set of elements the cache is for.
     * @return - containment cache instance to be tested.
     */
    protected abstract <E, C extends ICacheEntry<E>> IContainmentCache<E, C> getCache(BiMap<E, Integer> permutation, Comparator<E> comparator);


    /**
     * @param elements - integer elements.
     * @return the set containing the given elements.
     */
    private SimpleCacheSet<Integer> makeSet(int... elements) {
    	final Set<Integer> set = Arrays.stream(elements).boxed().collect(Collectors.toSet());
        return new SimpleCacheSet<Integer>(set, PERMUTATION);
    }

    /**
     * Creation tests.
     */
    @Test
    public void testEmptyTree() {
        final IContainmentCache<Integer, ICacheEntry<Integer>> cache = getCache(PERMUTATION, COMPARATOR);

        //Empty tree has size 0.
        assertEquals(cache.size(), 0);

        //Empty tree does not contain empty set.
        ICacheEntry<Integer> emptySet = makeSet();
        assertTrue(Lists.newLinkedList(cache.getSubsets(emptySet)).isEmpty());
        assertTrue(Lists.newLinkedList(cache.getSupersets(emptySet)).isEmpty());
        assertFalse(cache.contains(emptySet));

        //Empty tree does not contain any subset.
        assertTrue(Lists.newLinkedList(cache.getSubsets(makeSet(1, 2, 3))).isEmpty());

        //Empty tree does not contain any superset.
        assertTrue(Lists.newLinkedList(cache.getSupersets(makeSet(1, 2, 3))).isEmpty());
    }

    /**
     * Subset and superset tests.
     */

    @Test
    public void testEmptySet() {
        final IContainmentCache<Integer, ICacheEntry<Integer>> cache = getCache(PERMUTATION, COMPARATOR);

        final ICacheEntry<Integer> S = makeSet();
        cache.add(S);

        Collection<ICacheEntry<Integer>> subsets;
        Collection<ICacheEntry<Integer>> supersets;

        subsets = Lists.newLinkedList(cache.getSubsets(S));
        assertEquals(subsets.size(), 1);
        assertTrue(subsets.contains(S));
        supersets = Lists.newLinkedList(cache.getSupersets(S));
        assertEquals(supersets.size(), 1);
        assertTrue(supersets.contains(S));

        final ICacheEntry<Integer> R = makeSet(1, 2, 3, 4);
        cache.add(R);

        subsets = Lists.newLinkedList(cache.getSubsets(R));
        assertEquals(2, subsets.size());
        assertEquals(cache.getNumberSubsets(R), subsets.size());
        assertTrue(subsets.contains(S));
        assertTrue(subsets.contains(R));

        supersets = Lists.newLinkedList(cache.getSupersets(S));
        assertEquals(2, supersets.size());
        assertEquals(cache.getNumberSupersets(S), supersets.size());
        assertTrue(supersets.contains(S));
        assertTrue(supersets.contains(R));

    }

    @Test
    public void testIdempotence() {
        final IContainmentCache<Integer, ICacheEntry<Integer>> cache = getCache(PERMUTATION, COMPARATOR);

        final ICacheEntry<Integer> S = makeSet(1, 2, 3);

        cache.add(S);

        Collection<ICacheEntry<Integer>> subsets;
        Collection<ICacheEntry<Integer>> supersets;

        assertEquals(1, cache.size());

        subsets = Lists.newLinkedList(cache.getSubsets(S));
        assertTrue(subsets.contains(S));
        assertEquals(subsets.size(), 1);
        assertEquals(cache.getNumberSubsets(S), subsets.size());

        supersets = Lists.newLinkedList(cache.getSupersets(S));
        assertTrue(supersets.contains(S));
        assertEquals(1, supersets.size());
        assertEquals(cache.getNumberSupersets(S), supersets.size());

        cache.add(S);

        assertEquals(1, cache.size());

        subsets = Lists.newLinkedList(cache.getSubsets(S));
        assertTrue(subsets.contains(S));
        assertEquals(subsets.size(), 1);
        assertEquals(subsets.size(), cache.getNumberSubsets(S));

        supersets = Lists.newLinkedList(cache.getSupersets(S));
        assertTrue(supersets.contains(S));
        assertEquals(1, supersets.size());
        assertEquals(cache.getNumberSupersets(S), supersets.size());

    }

    @Test
    public void testOneSubset() {
        final IContainmentCache<Integer, ICacheEntry<Integer>> cache = getCache(PERMUTATION, COMPARATOR);

        final Collection<ICacheEntry<Integer>> nosubsets = Lists.newLinkedList(cache.getSubsets(makeSet(1, 2, 3, 4)));
        assertTrue(nosubsets.isEmpty());

        ICacheEntry<Integer> s1 = makeSet(1, 2);
        cache.add(s1);
        final Collection<ICacheEntry<Integer>> onesubsets = Lists.newLinkedList(cache.getSubsets(makeSet(1, 2, 3, 4)));
        assertEquals(onesubsets.size(), 1);
        assertTrue(onesubsets.contains(s1));
        assertEquals(cache.getNumberSubsets(makeSet(1, 2, 3, 4)), onesubsets.size());
    }

    @Test
    public void testOneSuperset() {
        final IContainmentCache<Integer, ICacheEntry<Integer>> cache = getCache(PERMUTATION, COMPARATOR);

        final Collection<ICacheEntry<Integer>> nosupersets = Lists.newLinkedList(cache.getSupersets(makeSet(1, 2)));
        assertTrue(nosupersets.isEmpty());

        ICacheEntry<Integer> s1 = makeSet(1, 2, 3, 4);
        cache.add(s1);
        final Collection<ICacheEntry<Integer>> onesubsets = Lists.newLinkedList(cache.getSupersets(makeSet(1, 2)));
        int numsupersets = cache.getNumberSupersets(makeSet(1, 2));
        assertEquals(numsupersets, 1);
        assertEquals(onesubsets.size(), numsupersets);
        assertTrue(onesubsets.contains(s1));
    }

    @Test
    public void testIntersectingSubsets() {
        final IContainmentCache<Integer, ICacheEntry<Integer>> cache = getCache(PERMUTATION, COMPARATOR);

        final ICacheEntry<Integer> s1 = makeSet(1, 2);
        cache.add(s1);
        final ICacheEntry<Integer> s2 = makeSet(2, 3);
        cache.add(s2);

        final Collection<ICacheEntry<Integer>> subsets = Lists.newLinkedList(cache.getSubsets(makeSet(1, 2, 3, 4)));
        int numsubsets = cache.getNumberSubsets(makeSet(1, 2, 3, 4));

        assertEquals(2, numsubsets);
        assertEquals(subsets.size(), numsubsets);
        assertTrue(subsets.contains(s1));
        assertTrue(subsets.contains(s2));
    }

    @Test
    public void testNestedSubsets() {
        final IContainmentCache<Integer, ICacheEntry<Integer>> cache = getCache(PERMUTATION, COMPARATOR);

        final ICacheEntry<Integer> s1 = makeSet(1);
        cache.add(s1);
        final ICacheEntry<Integer> s2 = makeSet(1, 2);
        cache.add(s2);

        final Collection<ICacheEntry<Integer>> subsets = Lists.newLinkedList(cache.getSubsets(makeSet(1, 2, 3, 4)));

        assertEquals(subsets.size(), 2);
        assertTrue(subsets.contains(s1));
        assertTrue(subsets.contains(s2));
    }

    @Test
    public void testNestedSupersets() {
        final IContainmentCache<Integer, ICacheEntry<Integer>> cache = getCache(PERMUTATION, COMPARATOR);

        final ICacheEntry<Integer> s1 = makeSet(1, 2);
        cache.add(s1);
        final ICacheEntry<Integer> s2 = makeSet(1, 2, 3);
        cache.add(s2);

        final Collection<ICacheEntry<Integer>> supersets = Lists.newLinkedList(cache.getSupersets(makeSet(1)));


        assertEquals(supersets.size(), 2);
        assertTrue(supersets.contains(s1));
        assertTrue(supersets.contains(s2));
    }

    /**
     * Addition & removal tests.
     */
    @Test
    public void testAddThenRemove() {
        final IContainmentCache<Integer, ICacheEntry<Integer>> cache = getCache(PERMUTATION, COMPARATOR);

        final ICacheEntry<Integer> S = makeSet(1, 2, 3);

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
    public void smokeTest() throws Exception {
        System.out.println("SMOKE TESTS");

        //Parameters
        final long seed = 1;
        final Random rand = new Random(seed);
        final int N = 2000;
        final int startingSize = 10000;
        final int numTests = 1000;
        
        final List<Integer> universe = IntStream.range(0, N).boxed().collect(Collectors.toList());
        System.out.println("Universe has " + N + " elements (" + (int) Math.pow(2, N) + " possible sets).");

        //Create cache and wrap with timer proxy.
        final IContainmentCache<Integer, ICacheEntry<Integer>> undecoratedcache = getCache(PERMUTATION, COMPARATOR);
        
        log.info("Creating " + startingSize + " elements...");
//        List<BitSet> entries = FileIOHelper.readSerializedObject("/home/newmanne/temp/july14/list");

        ArrayList<BitSet> entries = generateRandomSets(rand, startingSize, universe);
//        FileIOHelper.writeSerializedObject("/home/newmanne/temp/july14/list", entries);
        
        log.info("Building up a cache of size " + startingSize + "...");
        final Stopwatch watch = Stopwatch.createStarted();
        for (int i = 0; i < entries.size(); i++) {
            if (i % (startingSize / 10) == 0) {
            	double fillPercentage = ((double) i) / ((double) entries.size()) * 100.0;
            	log.info(fillPercentage + "% (" + undecoratedcache.size() + ")...");
            }
            undecoratedcache.add(new SimpleCacheSet<>(bitSetToSet(entries.get(i)), PERMUTATION));
        }
        log.info("Total time: {}", DurationFormatUtils.formatDuration(watch.elapsed(TimeUnit.MILLISECONDS), "HH:mm:ss.S"));
        watch.reset().start();
        entries.clear();
        
        log.info("Running {} tests...", numTests);
        final ProxyTimer timer = new ProxyTimer(undecoratedcache);
        @SuppressWarnings("unchecked")
        final IContainmentCache<Integer, ICacheEntry<Integer>> cache = (IContainmentCache<Integer, ICacheEntry<Integer>>) Proxy.newProxyInstance(IContainmentCache.class.getClassLoader(), new Class[]{IContainmentCache.class}, timer);

        log.info("Making {} elements for the tests", numTests);
        List<BitSet> testEntries = generateRandomSets(rand, numTests, universe);
        log.info("Running {} tests", numTests);

        for (int t = 0; t < numTests; t++) {
            if (t % (numTests / 10) == 0) {
            	double completionPercentage = ((double) t) / ((double) numTests) * 100.0;
            	log.info(completionPercentage + "%...");
            }

            //Test a certain set.
            ICacheEntry<Integer> set = new SimpleCacheSet<Integer>(bitSetToSet(testEntries.get(t)), PERMUTATION);

            //Test addition.
            cache.add(set);
            cache.getSubsets(set);
            cache.getSupersets(set);
            cache.remove(set);
        }
        watch.stop();
        final long totalduration = watch.elapsed(TimeUnit.MILLISECONDS);
        testEntries.clear();

        System.out.println("");
        TestUtils.printMethodRuntimes(timer.getMethodStats(), CSV_OUTPUT);
        System.out.printf("Total time: %s\n", DurationFormatUtils.formatDuration(totalduration, "HH:mm:ss.S"));
        checkMem();
        System.out.println(cache.toString()); // keep it in scope
    }

	private Set<Integer> bitSetToSet(BitSet bitSet) {
		return bitSet.stream().boxed().collect(Collectors.toSet());
	}


	private ArrayList<BitSet> generateRandomSets(
			final Random rand,
			final int numEntries,
			final List<Integer> universe
			) {
		Queue<BitSet> q = new LinkedBlockingQueue<>();
		final AtomicInteger counter = new AtomicInteger();
		IntStream.range(0, numEntries).parallel().forEach(i -> {
			if (counter.incrementAndGet() % 10000 == 0) {
				log.info("Making element {}", counter.get());
			};
			BitSet bs = new BitSet();
			for (int j = 0; j < universe.size(); j++) {
				if (rand.nextBoolean()) {
					bs.set(j);
				}
			}
			q.add(bs);
		});
		return Lists.newArrayList(q);
	}

    private void checkMem() {
       System.gc();
       System.gc();
    	
       int mb = 1024*1024;
        
       //Getting the runtime reference from system
       Runtime runtime = Runtime.getRuntime();
        
       System.out.println("##### Heap utilization statistics [MB] #####");
        
       //Print used memory
       System.out.println("Used Memory:"
           + (runtime.totalMemory() - runtime.freeMemory()) / mb);

       //Print free memory
       System.out.println("Free Memory:"
           + runtime.freeMemory() / mb);
        
       //Print total available memory
       System.out.println("Total Memory:" + runtime.totalMemory() / mb);

       //Print Maximum available memory
       System.out.println("Max Memory:" + runtime.maxMemory() / mb);
    }
    

}
