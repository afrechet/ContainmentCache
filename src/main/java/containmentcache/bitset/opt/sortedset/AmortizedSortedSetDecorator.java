package containmentcache.bitset.opt.sortedset;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.set.UnmodifiableSet;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

/**
 * An decorator around standard navigable set that implements {@link ISortedSet}.
 * <p/>
 * Note that {@link #getNumberSmaller(Comparable)} and {@link #getNumberLarger(Comparable)} are <i>only approximate</i>, and based
 * on the last time the {@link #updateSizes()} method has been called.
 *
 * @param <T>
 * @author afrechet
 */
@EqualsAndHashCode
@Slf4j
public class AmortizedSortedSetDecorator<T> implements ISortedSet<T> {

	// The actual set
	private final NavigableSet<T> set;
	// A potentially out of date sorted copy of the set
    private final List<T> list;
    // Additions to the list that have been made since a call to update 
    private final Set<T> buffer;

    public AmortizedSortedSetDecorator(NavigableSet<T> set) {
        this.set = set;
        list = Lists.newArrayList(set);
        Collections.sort(list, set.comparator());
        buffer = new TreeSet<T>(set.comparator());
    }

    /**
     * A method that puts all the elements of the sorted navigable set into a list for quick indexing and finding of number of smaller/larger elements.
     */
    public void updateSizes() {
    	log.debug("Updating sizes. Set has {} elements and buffer has {} elements", set.size(), buffer.size());
    	Stopwatch watch = Stopwatch.createStarted();
        list.clear();
        list.addAll(set);
        list.addAll(buffer);
        Collections.sort(list, set.comparator());
        buffer.clear();
        log.debug("It took {} ms to update sizes", watch.elapsed(TimeUnit.MILLISECONDS));
    }

    /**
     * @return an unmodifiable view of the list buffer.
     */
    public Set<T> getBuffer() {
        return UnmodifiableSet.unmodifiableSet(buffer);
    }

    @Override
    public Iterable<T> getLarger(T entry) {
        return set.tailSet(entry, true);
    }

    @Override
    public Iterable<T> getSmaller(T entry) {
        return set.headSet(entry, true);
    }

    @Override
    public long getNumberLarger(T entry) {
        final int index = Collections.binarySearch(list, entry, set.comparator());
        final int numlarger;
        if (index < 0) {
            numlarger = list.size() + (index + 1);
        } else {
            numlarger = list.size() - index;
        }
        final long bufferlarger = buffer.stream().filter(element -> comparator().compare(element, entry) >= 0).count();
        return numlarger + bufferlarger;
    }

    @Override
    public long getNumberSmaller(T entry) {
        final int index = Collections.binarySearch(list, entry, set.comparator());
        final int numSmaller;
        if (index < 0) {
            numSmaller = -(index + 1);
        } else {
            numSmaller = index + 1;
        }
        final long bufferSmaller = buffer.stream().filter(element -> comparator().compare(element, entry) <= 0).count();
        return numSmaller + bufferSmaller;
    }

    @Override
    public boolean add(T entry) {
        final boolean addResult = set.add(entry);
        if (addResult) {
            buffer.add(entry);
        }
        return addResult;
    }

    @Override
    public boolean remove(T entry) {
        buffer.remove(entry);
        return set.remove(entry);
    }

    @Override
    public boolean contains(T entry) {
        return set.contains(entry);
    }

    @Override
    public int size() {
        return set.size();
    }

	@Override
	public Comparator<? super T> comparator() {
		return set.comparator();
	}

}