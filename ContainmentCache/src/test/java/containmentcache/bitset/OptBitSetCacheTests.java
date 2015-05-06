package containmentcache.bitset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import containmentcache.AContainmentCacheTests;
import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;
import containmentcache.bitset.opt.OptBitSetCache;

public class OptBitSetCacheTests extends AContainmentCacheTests{

	private static final int NUM_ORDERINGS = 1;
	private static final long SEED = 0;
	
	@Override
	protected <E extends Comparable<E>, C extends ICacheEntry<E>> IContainmentCache<E, C> getCache(
			Set<E> universe) {
		
		final List<E> universelist = new ArrayList<>(universe);
		
		final List<Map<E,Integer>> orderings = new LinkedList<Map<E,Integer>>();
		final Random random = new Random(SEED);
		for(int o=0;o<NUM_ORDERINGS;o++)
		{
			final Map<E,Integer> ordering = new HashMap<E,Integer>();
			
			Collections.shuffle(universelist, random);
			int index = 0;
			for(E element : universelist)
			{
				ordering.put(element, index++);
			}
			
			orderings.add(ordering);
		}
		return new OptBitSetCache<>(orderings);
	}

}
