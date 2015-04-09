package containmentcache.bitset;

import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import containmentcache.ICacheSet;
import containmentcache.IContainmentCache;

public class SimpleBitSetCache<E> implements IContainmentCache<E>{

	private final BiMap<E,Integer> perm;
	private final TreeSet<BitSet> tree;
	
	public SimpleBitSetCache(Map<E,Integer> permutation)
	{
		if(permutation == null)
		{
			throw new IllegalArgumentException("Cannot create bitset cache with null permutation.");
		}
		
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
		
		perm = HashBiMap.create(permutation);
		tree = new TreeSet<BitSet>(new BitSetComparator());
	}
	
	public SimpleBitSetCache(Set<E> universe)
	{
		if(universe == null)
		{
			throw new IllegalArgumentException("Cannot create "+this.getClass().getSimpleName()+" with empty universe");
		}
		
		perm = HashBiMap.create(universe.size());
		
		int index = 0;
		for(E element : universe)
		{
			perm.put(element, index++);
		}
		
		tree = new TreeSet<BitSet>(new BitSetComparator());
	}
	
		
	@Override
	public void add(ICacheSet<E> set) {
		tree.add(getBitSet(set.getElements()));
	}

	@Override
	public void remove(ICacheSet<E> set) {
		tree.remove(getBitSet(set.getElements()));	
	}

	@Override
	public boolean contains(ICacheSet<E> set) {
		return tree.contains(getBitSet(set.getElements()));
	}

	@Override
	public LinkedList<Set<E>> getSubsets(ICacheSet<E> set) {
		
		final LinkedList<Set<E>> subsets = new LinkedList<Set<E>>();
		
		final BitSet bs = getBitSet(set.getElements());
		for(BitSet smallerbs : tree.headSet(bs, true))
		{
			if(isSubsetOrEqualTo(smallerbs, bs))
			{
				subsets.add(getSet(smallerbs));
			}
		}
		
		return subsets;
	}

	@Override
	public int getNumberSubsets(ICacheSet<E> set) {
		int numsubsets = 0;
		
		BitSet bs = getBitSet(set.getElements());
		for(BitSet smallerbs : tree.headSet(bs, true))
		{
			if(isSubsetOrEqualTo(smallerbs, bs))
			{
				numsubsets ++;
			}
		}
		
		return numsubsets;
	}

	@Override
	public Collection<Set<E>> getSupersets(ICacheSet<E> set) {
		final LinkedList<Set<E>> supersets = new LinkedList<Set<E>>();
		
		final BitSet bs = getBitSet(set.getElements());
		for(BitSet largerbs : tree.tailSet(bs, true))
		{
			if(isSubsetOrEqualTo(bs, largerbs))
			{
				supersets.add(getSet(largerbs));
			}
		}
		
		return supersets;
	}

	@Override
	public int getNumberSupersets(ICacheSet<E> set) {
		
		int numsupersets = 0;	
		final BitSet bs = getBitSet(set.getElements());
		for(BitSet largerbs : tree.tailSet(bs, true))
		{
			if(isSubsetOrEqualTo(bs, largerbs))
			{
				numsupersets++;
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
	 * @author afrechet
	 */
	private class BitSetComparator implements Comparator<BitSet>
	{
		@Override
		public int compare(BitSet bs1, BitSet bs2) {
			assert bs1.size() == bs2.size();
			
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
	
	/**
	 * @param set - a set of element.
	 * @return the bit set representing the given set of elements, according to the permutation.
	 */
	private BitSet getBitSet(Set<E> set)
	{
		if(set == null)
		{
			throw new IllegalArgumentException("Cannot create bit set out of null set.");
		}
		if(!perm.keySet().containsAll(set))
		{
			throw new IllegalArgumentException("Provided set contains element not in the cache's permutation.");
		}
		
		BitSet b = new BitSet(perm.size());
		for(Entry<E,Integer> permentry : perm.entrySet())
		{
			E element = permentry.getKey();
			int index = permentry.getValue();
			
			if(set.contains(element))
			{
				b.set(index);
			}
		}
		return b;
	}
	
	/**
	 * @param bitset - a bitset.
	 * @return the set of elements represented by the given bitset, according to the permutation.
	 */
	private HashSet<E> getSet(BitSet bitset)
	{
		final HashSet<E> set = new HashSet<E>();
		for (int i = bitset.nextSetBit(0); i >= 0; i = bitset.nextSetBit(i+1)) {
		     set.add(perm.inverse().get(i));
		}
		return set;
	}
	
    /**
     * @param bs1 - first bitset.
     * @param bs2 - second bitset.
     * @return true if and only if the set represented by bs1 is a subset of the set represented by bs2.
     */
    private boolean isSubsetOrEqualTo(final BitSet bs1, final BitSet bs2) {
        return bs1.stream().allMatch(bs2::get);
    }

}
