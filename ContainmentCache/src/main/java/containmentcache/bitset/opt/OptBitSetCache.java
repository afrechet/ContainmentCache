package containmentcache.bitset.opt;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import lombok.RequiredArgsConstructor;
import lombok.Value;

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
		final int distinctuniversescount = (int) orderings.stream().map(ordering -> ordering.keySet()).distinct().count();
		if(distinctuniversescount != 1)
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
			@Deprecated
			final ISortedSet<OptBitSet> set = new SlowSortedSetDecorator<OptBitSet>(new TreeSet<OptBitSet>());
			
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
			final ISortedSet<OptBitSet> setcache = container.set;
			final SetMultimap<OptBitSet, C> entries = container.entries;
			final Map<E,Integer> ordering = container.ordering;
			
			final OptBitSet bs = new OptBitSet(set.getElements(), ordering);
			final Set<C> bsentries = entries.get(bs);
			if(bsentries.isEmpty())
			{
				setcache.add(bs);
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
	@Value
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
	
	
}
