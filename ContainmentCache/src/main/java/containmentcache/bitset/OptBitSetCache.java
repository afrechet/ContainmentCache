package containmentcache.bitset;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import lombok.RequiredArgsConstructor;

import com.google.common.base.Predicate;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Range;
import com.google.common.collect.SetMultimap;

import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;
import containmentcache.util.CachedFunctionDecorator;
import containmentcache.util.NestedIterator;

public class OptBitSetCache<E, C extends ICacheEntry<E>> implements IContainmentCache<E, C> {

	private final List<SetContainer> sets;
	
	/**
	 * @param orderings - a collection of map from the (same) universe of n element to a permutation of [0,1,...,n-1].
	 * @deprecated
	 */
	public OptBitSetCache(Collection<Map<E,Integer>> orderings) {
		
		//Check that we are given at least one ordering.
		if(orderings.isEmpty())
		{
			throw new IllegalArgumentException("Must provide at least one ordering.");
		}
		
		//Check that the orderings are all on the same set of elements.
		if(orderings.stream().map(ordering -> ordering.keySet()).distinct().count() == 1)
		{
			throw new IllegalArgumentException("Not all of the orderings are on the same elements.");
		}
		
		//Check that the orderings all map to a permutation of [0,...,n-1].
		//Assumes previous test passes.
		final Set<E> domain = orderings.iterator().next().keySet();
		final Set<Integer> image = ContiguousSet.create(Range.closed(0, domain.size()-1),DiscreteDomain.integers());
		for(final Map<E,Integer> ordering : orderings)
		{
			if(!ordering.values().containsAll(image))
			{
				throw new IllegalArgumentException("An ordering does not map elements to (a permutation of) the integer range {0,1,...,n-1}.");
			}
		}
		
		//Create the container set.
		sets = new LinkedList<SetContainer>();
		for(final Map<E,Integer> ordering : orderings)
		{
			//TODO improve to use a better implementation of ISortedSet.
			final ISortedSet<OptBitSet> set = new SlowSortedSet<OptBitSet>(new TreeSet<OptBitSet>());
			final SetMultimap<OptBitSet, C> entries = HashMultimap.create();
			final SetContainer container = new SetContainer(set, entries, ordering);
			sets.add(container);
		}
	}
	
	
	@Override
	public void add(C set) {
		
		//Add to all the sets.
		for(final SetContainer container : sets)
		{
			final ISortedSet<OptBitSet> tree = container.set;
			final SetMultimap<OptBitSet, C> entries = container.entries;
			final Map<E,Integer> ordering = container.ordering;
			
			final OptBitSet bs = new OptBitSet(set.getElements(), ordering);
			final Set<C> bsentries = entries.get(bs);
			if(bsentries.isEmpty())
			{
				tree.add(bs);
			}
			bsentries.add(set);
		}
	}

	@Override
	public void remove(C set) {
		
		//Remove from all the sets.
		for(final SetContainer container : sets)
		{
			final ISortedSet<OptBitSet> tree = container.set;
			final SetMultimap<OptBitSet, C> entries = container.entries;
			final Map<E,Integer> ordering = container.ordering;
			
			final OptBitSet bs = new OptBitSet(set.getElements(), ordering);
			final Set<C> bsentries = entries.get(bs);
			bsentries.remove(set);
			if(bsentries.isEmpty())
			{
				tree.remove(bs);
			}
		}
	}

	@Override
	public boolean contains(C set) {
		
		//Perform contains on the first set.
		if(sets.isEmpty())
		{
			return false;
		}
		else
		{
			final SetContainer container = sets.get(0);
			final SetMultimap<OptBitSet, C> entries = container.entries;
			final Map<E,Integer> ordering = container.ordering;
			
			final OptBitSet bs = new OptBitSet(set.getElements(), ordering);
			return entries.get(bs).contains(set);
		}
	}

	@Override
	public Iterable<C> getSets() {
		if(sets.isEmpty())
		{
			return Collections.emptySet(); 
		}
		else
		{
			final SetContainer container = sets.get(0);
			final SetMultimap<OptBitSet, C> entries = container.entries;
			return entries.values();
		}
	}

	
	@Override
	public Iterable<C> getSubsets(C set) {
		
		//Get the set container with smallest number of sets smaller than given set.
		Optional<SetContainer> bestcontaineroptional = getMin(new NumSmallerContainerFunction(set));

		if(bestcontaineroptional.isPresent())
		{
			//Get the subsets from the optimal container.
			final SetContainer bestcontainer = bestcontaineroptional.get();
			final OptBitSet bs = new OptBitSet(set.getElements(), bestcontainer.ordering);
			
			final Iterator<OptBitSet> subsetiterator = Iterators.filter(
					bestcontainer.set.getSmaller(bs).iterator(),
					new Predicate<OptBitSet>(){
						@Override
						public boolean apply(OptBitSet smallerbs) {
							return smallerbs.isSubset(bs);
						}
					}
				);
			
			return new Iterable<C>() {
				@Override
				public Iterator<C> iterator() {
					return new NestedIterator<C>(subsetiterator,bestcontainer.entries.asMap());
				}
			}; 
		}
		else
		{
			return Collections.emptySet();
		}
	}
	
	@Override
	public int getNumberSubsets(C set) {
		
		//Get the set container with smallest number of sets smaller than given set.
		Optional<SetContainer> bestcontaineroptional = getMin(new NumSmallerContainerFunction(set));

		if(bestcontaineroptional.isPresent())
		{
			//Add the number of subsets from the optimal container.
			final SetContainer bestcontainer = bestcontaineroptional.get();
			final ISortedSet<OptBitSet> tree = bestcontainer.set;
			final SetMultimap<OptBitSet, C> entries = bestcontainer.entries;
			final Map<E,Integer> ordering = bestcontainer.ordering;
			
			final OptBitSet bs = new OptBitSet(set.getElements(), ordering);
			
			return StreamSupport.stream(tree.getSmaller(bs).spliterator(), false)
					.filter(smallerbs -> smallerbs.isSubset(bs))
					.map(smallerbs -> entries.get(smallerbs).size())
					.reduce(0, Integer::sum);
		}
		else
		{
			return 0;
		}
	}

	@Override
	public Iterable<C> getSupersets(C set) {
		
		//Get the set container with smallest number of sets larger than given set.
		Optional<SetContainer> bestcontaineroptional = getMin(new NumLargerContainerFunction(set));

		if(bestcontaineroptional.isPresent())
		{
			//Get the supersets from the optimal container.
			final SetContainer bestcontainer = bestcontaineroptional.get();
			final OptBitSet bs = new OptBitSet(set.getElements(), bestcontainer.ordering);
			
			final Iterator<OptBitSet> supersetsiterator = Iterators.filter(
					bestcontainer.set.getLarger(bs).iterator(),
					new Predicate<OptBitSet>(){
						@Override
						public boolean apply(OptBitSet largerbs) {
							return bs.isSubset(largerbs);
						}
					}
				);
			
			return new Iterable<C>() {
				@Override
				public Iterator<C> iterator() {
					return new NestedIterator<C>(supersetsiterator,bestcontainer.entries.asMap());
				}
			}; 
		}
		else
		{
			return Collections.emptySet();
		}
	}

	@Override
	public int getNumberSupersets(C set) {
		
		//Get the set container with smallest number of sets larger than given set.
		Optional<SetContainer> bestcontaineroptional = getMin(new NumLargerContainerFunction(set));

		if(bestcontaineroptional.isPresent())
		{
			//Add the number of supersets from the optimal container.
			final SetContainer bestcontainer = bestcontaineroptional.get();
			final ISortedSet<OptBitSet> tree = bestcontainer.set;
			final SetMultimap<OptBitSet, C> entries = bestcontainer.entries;
			final Map<E,Integer> ordering = bestcontainer.ordering;
			
			final OptBitSet bs = new OptBitSet(set.getElements(), ordering);
			
			return StreamSupport.stream(tree.getLarger(bs).spliterator(), false)
					.filter(largerbs -> bs.isSubset(largerbs))
					.map(largerbs -> entries.get(largerbs).size())
					.reduce(0, Integer::sum);
		}
		else
		{
			return 0;
		}
	}
	
	
	
	@Override
	public int size() {
		//Get the size from the first container.
		if(sets.isEmpty())
		{
			return 0;
		}
		else
		{
			final SetContainer container = sets.get(0);
			final SetMultimap<OptBitSet, C> entries = container.entries;
			return entries.size();
		}
	}
	
	/**
	 * A container for the set data structures corresponding to a single random permutation/ordering of the universe.
	 * 
	 * @author afrechet
	 */
	@RequiredArgsConstructor
	private class SetContainer
	{
		private final ISortedSet<OptBitSet> set;
		private final SetMultimap<OptBitSet,C> entries;
		private final Map<E,Integer> ordering;
	}
	
	/**
	 * @param containerfunction - a function that returns a comparable from a given tree container.
	 * @return the tree container that minimizes the given container function.
	 */
	private <R extends Comparable<R>> Optional<SetContainer> getMin(Function<SetContainer,R> containerfunction)
	{
		return sets.stream().min(Comparator.comparing(new CachedFunctionDecorator<SetContainer,R>(containerfunction)));
	}
	
	
	/*
	 * Functions that, for a fixed set, takes a tree container to the number sets
	 * contained in that container's tree that are smaller/larger than the original set.
	 * 
	 * @author afrechet
	 */
	@RequiredArgsConstructor
	private class NumSmallerContainerFunction implements Function<SetContainer,Integer>
	{
		private final C set;
		@Override
		public Integer apply(final SetContainer container) {
			return container.set.getNumberSmaller(new OptBitSet(set.getElements(), container.ordering));
		}
	}
	@RequiredArgsConstructor
	private class NumLargerContainerFunction implements Function<SetContainer,Integer>
	{
		private final C set;
		@Override
		public Integer apply(final SetContainer container) {
			return container.set.getNumberLarger(new OptBitSet(set.getElements(), container.ordering));
		}
	}
	
	/**
	 * Bitset-based structure that performs fundamental containment cache operations. 
	 * Optimized hence less defensive implementation.
	 * 
	 * Conceptual representation is a bitset, but that bitset is segmented into blocks of a fixed size,
	 * and the long representation of those blocks is preserved. Hence, if the BLOCK_SIZE is 2 and the 
	 * universe ordering is [a,b,c,d,e,f,g,h] then the following set:
	 * 
	 * {a,c,d,g,h}
	 * 
	 * would be translated as follows:
	 * 
	 * 10110011	- conceptual bit vector
	 * [10][11][00][11] - grouping into blocks
	 * {0:1L,1:3L,1:0L,1:3L} - map of block index to block long value.
	 * 
	 * The two required operations can be implemented quickly using those block values.
	 * 
	 * @author afrechet
	 */
	private static class OptBitSet implements Comparable<OptBitSet>
	{
		/*
		 * Size of blocks. Needs to be less than 62 to allow long representation
		 * of numbers with BLOCK_SIZE bits on a 64-bits architecture.
		 */
		private final static int BLOCK_SIZE = 60;
		/*
		 * Map that takes a block index i to the long representation 'v' of
		 * the bit vector for the element with index between i and i+BLOCK_SIZE-1.
		 * Assumed to be in reverse order of block index.
		 */
		private final SortedMap<Integer,Long> blockvalues; 

		public <T> OptBitSet(Set<T> elements, Map<T,Integer> ordering)
		{
			blockvalues = new TreeMap<>(Collections.reverseOrder());
			for(final T element : elements)
			{
				final int order = ordering.get(element);
				final int blockindex = order / BLOCK_SIZE;
				final int blockbit = order % BLOCK_SIZE;
				
				final long blockvalue = blockvalues.getOrDefault(blockindex, 0L) + (1L<<blockbit);
				blockvalues.put(blockindex, blockvalue);
			}
		}
		
		/**
		 * @param bs - an opt bit set.
		 * @return true if and only if this bitset is a subset of the given bitset.
		 */
		public boolean isSubset(OptBitSet bs)
		{
			if(blockvalues.size() > bs.blockvalues.size())
			{
				return false;
			}
			else
			{
				final Iterator<Entry<Integer,Long>> myblockvalues = blockvalues.entrySet().iterator();
				final Iterator<Entry<Integer,Long>> hisblockvalues = bs.blockvalues.entrySet().iterator();
				
				//Loop through the smaller optbitset's entries.
				while(myblockvalues.hasNext())
				{
					final Entry<Integer,Long> myblockvalue = myblockvalues.next();
					final int myblockindex = myblockvalue.getKey();
					
					//Find the entry with same block index in the larger optbitset.
					Entry<Integer,Long> hisblockvalue = null;
					while(hisblockvalues.hasNext())
					{
						final Entry<Integer,Long> temphisblockvalue = hisblockvalues.next();
						//Return if we have found an entry with the right block index.
						if(temphisblockvalue.getKey() == myblockindex)
						{
							hisblockvalue = temphisblockvalue;
							break;
						}
						else if(temphisblockvalue.getKey() < myblockindex)
						{
							//We are past the right block index, we will never find it.
							return false;
						}
					}
					
					//We did not find an entry with the right block index, so smaller optbitset has some unseen elements.
					if(hisblockvalue == null)
					{
						return false;
					}
					else
					{
						//Test that at that block index, the smaller optbitset is a subset of the larger.
						if((myblockvalue.getValue() & hisblockvalue.getValue()) != myblockvalue.getValue())
						{
							return false;
						}
					}
				}
				
				//We matched all entries in the smaller optbitset.
				return true;
			}
		}
		
		@Override
		public int compareTo(OptBitSet bs)
		{
			final Iterator<Entry<Integer,Long>> myblockvalues = blockvalues.entrySet().iterator();
			final Iterator<Entry<Integer,Long>> hisblockvalues = bs.blockvalues.entrySet().iterator();
			
			//Traverse the blocks.
			while(myblockvalues.hasNext() && hisblockvalues.hasNext())
			{
				final Entry<Integer,Long> myblockvalue = myblockvalues.next();
				final Entry<Integer,Long> hisblockvalue = hisblockvalues.next();
				
				//Compare the block index; if its different, then the one with the larger block index wins.
				if(myblockvalue.getKey() > hisblockvalue.getKey())
				{
					return 1;
				}
				else if(myblockvalue.getKey() < hisblockvalue.getKey())
				{
					return -1;
				}
				else
				{
					//Same block index, compare block value; if it is different, then the largest one wins. 
					if(myblockvalue.getValue() > hisblockvalue.getValue())
					{
						return 1;
					}
					else if(myblockvalue.getValue() > hisblockvalue.getValue())
					{
						return -1;
					}
				}
			}
			
			//At least one bitset has been exhausted. If one of them still has entries, it wins.
			if(myblockvalues.hasNext())
			{
				return 1;
			}
			else if(hisblockvalues.hasNext())
			{
				return -1;
			}
			else
			{
				return 0;
			}
		}
		
	}
	
	/**
	 * Provides fundamental data structure to the bitset cache.
	 * 
	 * A coarser and simpler version of {@link NavigableSet}. 
	 * 
	 * The only additional  optimization is that the {@link #getNumberLarger(Comparable)} and {@link getNumberSmaller(Comparable)} should take constant time.
	 * 
	 * @author afrechet
	 *
	 * @param <T>
	 */
	private static interface ISortedSet<T extends Comparable<T>>
	{
		/**
		 * @param entry 
		 * @return an iterable over the entries in the set that are larger or equal to the given entry. 
		 */
		public Iterable<T> getLarger(T entry);
		
		/**
		 * @param entry 
		 * @return an iterable over the entries in the set that are smaller or equal to the given entry. 
		 */
		public Iterable<T> getSmaller(T entry);
		
		/**
		 * @param entry 
		 * @return the number of entries in the set that are larger or equal to the given entry. 
		 */
		public int getNumberLarger(T entry);
		
		/**
		 * @param entry 
		 * @return the number of entries in the set that are smaller or equal to the given entry. 
		 */
		public int getNumberSmaller(T entry);
		
		/**
		 * @param entry
		 */
		public void add(T entry);
		
		/**
		 * 
		 * @param entry
		 */
		public void remove(T entry);
		
		/**
		 * @param entry
		 * @return true if and only if the set contains the given entry.
		 */
		public boolean contains(T entry);
		
		/**
		 * @return the number of entries in the set.
		 */
		public int size();
	}
	
	/**
	 * An decorator around standard navigable set that implements {@link ISortedSet}.
	 * 
	 * Note that {@link #getNumberSmaller(Comparable)} and {@link #getNumberLarger(Comparable)} 
	 * will loop over all smaller/larger entries to return the size, as confirmed by the implementation
	 * of the size function in the head/tailset view.
	 * 
	 * @author afrechet
	 *
	 * @param <T>
	 */
	@RequiredArgsConstructor
	private static class SlowSortedSet<T extends Comparable<T>> implements ISortedSet<T>
	{
		private final NavigableSet<T> set;
		
		@Override
		public Iterable<T> getLarger(T entry) {
			return set.tailSet(entry, true);
		}

		@Override
		public Iterable<T> getSmaller(T entry) {
			return set.headSet(entry, true);
		}

		@Override
		public int getNumberLarger(T entry) {
			return set.tailSet(entry, true).size();
		}

		@Override
		public int getNumberSmaller(T entry) {
			return set.headSet(entry, true).size();
		}

		@Override
		public void add(T entry) {
			set.add(entry);
		}

		@Override
		public void remove(T entry) {
			set.remove(entry);
		}

		@Override
		public boolean contains(T entry) {
			return set.contains(entry);
		}

		@Override
		public int size() {
			return set.size();
		}
	}
}
