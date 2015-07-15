package containmentcache.bitset.opt.sortedset;

import java.util.Comparator;

import lombok.Getter;

/**
 * Created by newmanne on 13/07/15.
 */
public class ASortedSetDecorator<E> implements ISortedSet<E> {

    @Getter
    protected final ISortedSet<E> sortedSet;

    public ASortedSetDecorator(ISortedSet<E> set) {
        this.sortedSet = set;
    }

	@Override
	public Iterable<E> getLarger(E entry) {
		return sortedSet.getLarger(entry);
	}

	@Override
	public Iterable<E> getSmaller(E entry) {
		return sortedSet.getSmaller(entry);
	}

	@Override
	public long getNumberLarger(E entry) {
		return sortedSet.getNumberLarger(entry);
	}

	@Override
	public long getNumberSmaller(E entry) {
		return sortedSet.getNumberSmaller(entry);
	}

	@Override
	public boolean add(E entry) {
		return sortedSet.add(entry);
	}

	@Override
	public boolean remove(E entry) {
		return sortedSet.remove(entry);
	}

	@Override
	public boolean contains(E entry) {
		return sortedSet.contains(entry);
	}

	@Override
	public int size() {
		return sortedSet.size();
	}

	@Override
	public Comparator<? super E> comparator() {
		return sortedSet.comparator();
	}
}
