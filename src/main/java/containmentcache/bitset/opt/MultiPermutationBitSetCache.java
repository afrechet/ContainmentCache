package containmentcache.bitset.opt;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;
import containmentcache.bitset.opt.sortedset.ISortedSet;
import containmentcache.bitset.simple.SimpleBitSetCache;
import containmentcache.util.NestedIterables;

/**
 * High-performance version of the {@link SimpleBitSetCache} that uses multiple bitset caches each using
 * a different permutation of the universe.
 * <p/>
 * First discussed in
 * Fréchette, Alexandre and Newman, Neil and Leyton-Brown, Kevin. "Solving the Station Repacking Problem" IJCAI. 2015.
 *
 * @param <E> - the elements the sets.
 * @author afrechet
 */
public class MultiPermutationBitSetCache<E, C extends ICacheEntry<E>> implements IContainmentCache<E, C> {

    private final List<SetContainer> sets;
    private final SetMultimap<BitSet, C> entries;
    private final ImmutableBiMap<E, Integer> canonicalPermutation;

    public interface ISortedSetFactory<T> {
    	ISortedSet<T> create(Comparator<T> comparator);
    }
    
    /**
     * @param orderings - A map from the universe of n elements to a List, where the ith position in the list means that the mapped element corresponds to List(i) in the ith permutation of [0,1,...,n-1].
     * The first ordering (i.e. the 0th element of each list MUST be consistent with {@link ICacheEntry#getBitSet}
     * 
     */
    public <T> MultiPermutationBitSetCache(@NonNull BiMap<E, Integer> canonicalPermutation, @NonNull List<BiMap<E, Integer>> additionalPermutations, @NonNull ISortedSetFactory<BitSet> sortedSetFactory) {
		//Check that the canonicalPermutation maps to a permutation of [0,...,n-1].
    	Preconditions.checkArgument(Sets.newHashSet(canonicalPermutation.values()).equals(IntStream.range(0, canonicalPermutation.size()).boxed().collect(Collectors.toSet())), "Ordering must map to a permutation of [0,...,n-1].");
    	
    	//Check that the orderings are all on the same set of elements as the canonical one i.e. they should all have the same KeySet() and ValueSet()
		for (BiMap<E, Integer> additionalPermutation : additionalPermutations) {
			Preconditions.checkArgument(additionalPermutation.keySet().equals(canonicalPermutation.keySet()), "Not all of the orderings are on the same elements");
			Preconditions.checkArgument(additionalPermutation.values().equals(canonicalPermutation.values()), "Not all of the orderings are on the same elements");
		}
		
    	this.canonicalPermutation = ImmutableBiMap.copyOf(canonicalPermutation);
        //Create the container set
        sets = new ArrayList<>();
        entries = HashMultimap.create();
        int numPermutations = 1 + additionalPermutations.size();
        int numElements = canonicalPermutation.keySet().size();
        final int[][] permutations = new int[numPermutations][numElements];
        int j = 0;
        for (E element : canonicalPermutation.keySet()) {
        	permutations[0][j] = canonicalPermutation.get(element);
            for (int i = 1; i < numPermutations; i++) {
                permutations[i][j] = additionalPermutations.get(i - 1).get(element);
            }
            j++;
        }

        for (int[] permutation : permutations) {
        	final PermutableBitSetComparator comparator = new PermutableBitSetComparator(permutation);
            final ISortedSet<BitSet> set = sortedSetFactory.create(comparator);
            final SetContainer container = new SetContainer(set, permutation);
            sets.add(container);
        }
    }

    @Override
    public void add(C set) {
    	final BitSet bs = getBitSet(set);
    	entries.get(bs).add(set);
    	sets.forEach(setContainer-> setContainer.set.add(bs));
    }

    @Override
    public void remove(C set) {
    	final BitSet bs = getBitSet(set);
    	entries.get(bs).remove(set);
    	sets.forEach(setContainer-> setContainer.set.remove(bs));
    }

    @Override
    public boolean contains(ICacheEntry<E> set) {
    	final BitSet bs = getBitSet(set);
        return entries.get(bs).contains(set);
    }

    @Override
    public Iterable<C> getSets() {
        return entries.values();
    }


    @Override
    public Iterable<C> getSubsets(ICacheEntry<E> set) {
    	final BitSet bs = getBitSet(set);
        //Get the set container with smallest number of sets smaller than given set.
        Optional<SetContainer> bestcontaineroptional = getMin(new NumSmallerContainerFunction(bs));

        if (bestcontaineroptional.isPresent()) {
            //Get the subsets from the optimal container.
            final SetContainer bestcontainer = bestcontaineroptional.get();
            final Iterable<BitSet> subsetIterable = Iterables.filter(bestcontainer.set.getSmaller(bs), bitset -> isSubset(bitset, bs));
            return NestedIterables.nest(subsetIterable, entries.asMap());
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public int getNumberSubsets(ICacheEntry<E> set) {
        //Get the set container with smallest number of sets smaller than given set.
    	final BitSet bs = getBitSet(set);
        final Optional<SetContainer> bestcontaineroptional = getMin(new NumSmallerContainerFunction(bs));

        if (bestcontaineroptional.isPresent()) {
            //Add the number of subsets from the optimal container.
            final SetContainer bestcontainer = bestcontaineroptional.get();
            final ISortedSet<BitSet> tree = bestcontainer.set;

            return StreamSupport.stream(tree.getSmaller(bs).spliterator(), false)
                    .filter(smallerbs -> isSubset(smallerbs, bs))
                    .map(smallerbs -> entries.get(smallerbs).size())
                    .reduce(0, Integer::sum);
        } else {
            return 0;
        }
    }

    @Override
    public Iterable<C> getSupersets(ICacheEntry<E> set) {
    	final BitSet bs = getBitSet(set);

        //Get the set container with smallest number of sets larger than given set.
        final Optional<SetContainer> bestcontaineroptional = getMin(new NumLargerContainerFunction(bs));

        if (bestcontaineroptional.isPresent()) {
            //Get the supersets from the optimal container.
            final SetContainer bestcontainer = bestcontaineroptional.get();
            final Iterable<BitSet> supersetIterable = Iterables.filter(bestcontainer.set.getLarger(bs), bitset -> isSubset(bs,bitset));
            return NestedIterables.nest(supersetIterable, entries.asMap());
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public int getNumberSupersets(ICacheEntry<E> set) {
    	final BitSet bs = getBitSet(set);

        //Get the set container with smallest number of sets larger than given set.
        final Optional<SetContainer> bestcontaineroptional = getMin(new NumLargerContainerFunction(bs));

        if (bestcontaineroptional.isPresent()) {
            //Add the number of supersets from the optimal container.
            final SetContainer bestcontainer = bestcontaineroptional.get();
            final ISortedSet<BitSet> tree = bestcontainer.set;

            return StreamSupport.stream(tree.getLarger(bs).spliterator(), false)
                    .filter(bitset -> isSubset(bs, bitset))
                    .map(largerbs -> entries.get(largerbs).size())
                    .reduce(0, Integer::sum);
        } else {
            return 0;
        }
    }


    @Override
    public int size() {
        return entries.size();
    }

    /**
     * A container for the set data structures corresponding to a single random permutation/ordering of the universe.
     *
     * @author afrechet
     */
    @Value
    private class SetContainer {
        private final ISortedSet<BitSet> set;
        private final int[] permutation;
    }

    /**
     * @param containerfunction - a function that returns a comparable from a given tree container.
     * @return the tree container that minimizes the given container function.
     */
    private <R extends Comparable<R>> Optional<SetContainer> getMin(Function<SetContainer, R> containerfunction) {
        return sets.stream().min(Comparator.comparing(containerfunction));
    }

    /*
     * Functions that, for a fixed set, takes a tree container to the number sets
     * contained in that container's tree that are smaller/larger than the original set.
     *
     * @author afrechet
     */
    @RequiredArgsConstructor
    private class NumSmallerContainerFunction implements Function<SetContainer, Long> {
        private final BitSet bs;

        @Override
        public Long apply(final SetContainer container) {
            return container.set.getNumberSmaller(bs);
        }
    }

    @RequiredArgsConstructor
    private class NumLargerContainerFunction implements Function<SetContainer, Long> {
        private final BitSet bs;

        @Override
        public Long apply(final SetContainer container) {
            return container.set.getNumberLarger(bs);
        }
    }

    // true iff a is a subset of b
    private boolean isSubset(final BitSet a, final BitSet b) {
        return a.stream().allMatch(b::get);
    }
    
	private BitSet getBitSet(ICacheEntry<E> set) {
		Preconditions.checkNotNull(set);
		Preconditions.checkNotNull(set.getBitSet());
		Preconditions.checkArgument(canonicalPermutation.keySet().containsAll(set.getElements()));
		return set.getBitSet();
	}

    /**
     * A comparator that compares bitsets according to an ordering specified by permutation such that the least
     * significant digit is the first number in the permutation)
     */
    private static class PermutableBitSetComparator implements Comparator<BitSet> {

        final int[] permutation;

        public PermutableBitSetComparator(int[] permutation) {
            this.permutation = permutation;
        }

        @Override
        public int compare(BitSet bs1, BitSet bs2) {
            for (int i = permutation.length - 1; i >= 0; i--) {
                int index = permutation[i];
                boolean b1 = bs1.get(index);
                boolean b2 = bs2.get(index);
                if (b1 && !b2) {
                    return 1;
                } else if (!b1 && b2) {
                    return -1;
                }
            }
            return 0;
        }
    }

}