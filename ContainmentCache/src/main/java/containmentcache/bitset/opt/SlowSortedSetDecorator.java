package containmentcache.bitset.opt;

import java.util.NavigableSet;

import lombok.Value;

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
@Value
public class SlowSortedSetDecorator<T extends Comparable<T>> implements ISortedSet<T>
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