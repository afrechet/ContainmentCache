package containmentcache;

import java.util.BitSet;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

/**
 * A simple cache set entry based on a bit set
 * @author afrechet
 * @param <E> - type of elements in cache set entry.
 */
@EqualsAndHashCode
public class SimpleCacheSet<E> implements ICacheEntry<E> {

	private final BitSet bitSet;
	private final ImmutableBiMap<E, Integer> permutation;
	
	public SimpleCacheSet(Set<E> elements, BiMap<E, Integer> permutation) {
		bitSet = new BitSet();
		elements.forEach(elem -> bitSet.set(permutation.get(elem)));
		this.permutation = ImmutableBiMap.copyOf(permutation);
	}

	@Override
	public BitSet getBitSet() {
		return bitSet;
	}

	@Override
	public ImmutableBiMap<E, Integer> getPermutation() {
		return permutation;
	}
	
	@Override
	public String toString() {
		return bitSet.toString();
	}
	
}
