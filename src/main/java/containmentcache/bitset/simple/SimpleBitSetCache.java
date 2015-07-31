package containmentcache.bitset.simple;

import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import lombok.NonNull;
import net.jcip.annotations.NotThreadSafe;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;

import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;
import containmentcache.util.NestedIterables;

/**
 * A simple bitset containment cache that represents sets as bitsets and uses the integer representation
 * of these bitsets to limit the number sets to search for sub/supersets. 
 * 
 * Given a query set, we get its bitset representation and corresponding integer number, and then can quickly find
 * all entries with bitset integer number larger (or smaller) than the query's. This allows us to get a superset of all
 * supersets (or subsets), which we then completely process and filter to exactly get the sets we are looking for.
 * 
 * We do not actually compute the integer number representation of bitset (to avoid overflow), and instead perform operations
 * on the bitsets directly.
 * 
 * First discussed in
 * Fréchette, Alexandre and Newman, Neil and Leyton-Brown, Kevin. "Solving the Station Repacking Problem" IJCAI. 2015. 
 * 
 * @author afrechet
 * @param <E> - the elements the sets.
 * @param <T> - the type of additional content in cache entries.
 */
@NotThreadSafe
public class SimpleBitSetCache<E,C extends ICacheEntry<E>> implements IContainmentCache<E,C>{
	
	//The entries of the data structure, hashed by their bitset representation.
	private final SetMultimap<BitSet,C> entries;
	//The tree of bitset, to organize the sub/superset structure.
	private final NavigableSet<BitSet> tree;
	private final ImmutableBiMap<E, Integer> permutation;
	
	public SimpleBitSetCache(@NonNull BiMap<E, Integer> permutation)
	{
		//Check that permutation is from 0 .. N-1.
		int N = permutation.size()-1;
		Collection<Integer> image = permutation.values();
		for(int i=0;i<=N;i++)
		{
			if(!image.contains(i))
			{
				throw new IllegalArgumentException("Permutation does not map any element to valid index "+i+", must be an invalid permutation.");
			}
		}
		this.permutation = ImmutableBiMap.copyOf(permutation);
		
		entries = HashMultimap.create();
		tree = new TreeSet<BitSet>(new BitSetComparator());
	}
	
	public void add(C set) {
		final BitSet bs = getBitSet(set);		
		final Set<C> bitsetentries = entries.get(bs);
		if(bitsetentries.isEmpty())
		{
			tree.add(bs);
		}
		bitsetentries.add(set);
	}

	@Override
	public void remove(C set) {
		final BitSet bs = getBitSet(set);		
		final Set<C> bitsetentries = entries.get(bs);
		bitsetentries.remove(set);
		if(bitsetentries.isEmpty())
		{
			tree.remove(bs);
		}
	}

	@Override
	public boolean contains(C set) {
		final BitSet bs = getBitSet(set);		
		if(entries.containsKey(bs))
		{
			final Set<C> bitsetentries = entries.get(bs);
			return bitsetentries.contains(set);
		}
		else
		{
			return false;
		}
		
	}
	
	@Override
	public Iterable<C> getSets() {
		return NestedIterables.nest(tree, entries.asMap());
	}
	
	@Override
	public Iterable<C> getSubsets(ICacheEntry<E> set) {
		final BitSet bs = getBitSet(set);		
		final Iterable<BitSet> subsetIterable = Iterables.filter(tree.headSet(bs, true), bitset -> isSubsetOrEqualTo(bitset, bs));
		return NestedIterables.nest(subsetIterable, entries.asMap());
	}

	@Override
	public int getNumberSubsets(ICacheEntry<E> set) {
		int numsubsets = 0;
		
		final BitSet bs = getBitSet(set);		
		for(BitSet smallerbs : tree.headSet(bs, true))
		{
			if(isSubsetOrEqualTo(smallerbs, bs))
			{
				numsubsets+=entries.get(smallerbs).size();
			}
		}
		
		return numsubsets;
	}

	@Override
	public Iterable<C> getSupersets(ICacheEntry<E> set) {
		final BitSet bs = getBitSet(set);		
		final Iterable<BitSet> supersetsIterable = Iterables.filter(tree.tailSet(bs, true), bitset -> isSubsetOrEqualTo(bs, bitset));
		return NestedIterables.nest(supersetsIterable, entries.asMap());
	}

	@Override
	public int getNumberSupersets(ICacheEntry<E> set) {
		int numsupersets = 0;	
		final BitSet bs = getBitSet(set);		
		for(BitSet largerbs : tree.tailSet(bs, true))
		{
			if(isSubsetOrEqualTo(bs, largerbs))
			{
				numsupersets+=entries.get(largerbs).size();
			}
		}
		return numsupersets;
	}

	@Override
	public int size() {
		return tree.size();
	}
	
	/**
	 * A comparator for bitsets that compares based on the integer values of bitsets.
	 * @author newmanne, afrechet
	 */
	private static class BitSetComparator implements Comparator<BitSet>
	{
		@Override
		public int compare(BitSet bs1, BitSet bs2) {
			for (int i = bs1.size() - 1; i >= 0; i--) {
                boolean b1 = bs1.get(i);
                boolean b2 = bs2.get(i);
                if (b1 && !b2) {
                    return 1;
                } else if (!b1 && b2) {
                    return -1;
                }
            }
            return 0;
		}
	}
	
	private BitSet getBitSet(ICacheEntry<E> set) {
		Preconditions.checkNotNull(set);
		Preconditions.checkNotNull(set.getBitSet());
		Preconditions.checkArgument(permutation.keySet().containsAll(set.getElements()));
		return set.getBitSet();
	}
	
    /**
     * @param bs1 - first bitset.
     * @param bs2 - second bitset.
     * @return true if and only if the set represented by bs1 is a subset of the set represented by bs2.
     */
    private static boolean isSubsetOrEqualTo(final BitSet bs1, final BitSet bs2) {
        return bs1.stream().allMatch(bs2::get);
    }

}
