package containmentcache.bitset.opt.sortedset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections4.set.UnmodifiableSet;

/**
 * An decorator around standard navigable set that implements {@link ISortedSet}.
 * 
 * Note that {@link #getNumberSmaller(Comparable)} and {@link #getNumberLarger(Comparable)} are <i>only approximate</i>, and based
 * on the last time the {@link #updateSizes()} method has been called. 
 * 
 * @author afrechet
 *
 * @param <T>
 */
public class AmortizedSortedSetDecorator<T extends Comparable<T>> implements ISortedSet<T>
{
	private final NavigableSet<T> set;
	
	private final List<T> list;
	
	private final Set<T> buffer;
	private final int maxbuffersize;
	
	public AmortizedSortedSetDecorator(NavigableSet<T> set)
	{
		this(set,Integer.MAX_VALUE);
	}
	
	public AmortizedSortedSetDecorator(NavigableSet<T> set, int maxbuffersize)
	{
		this.set = set;
		
		this.list = new ArrayList<T>(set.size());
		list.addAll(set);
		
		buffer = new TreeSet<T>();
		this.maxbuffersize = maxbuffersize;
	}
	
	/**
	 * A method that puts all the elements of the sorted navigable set into a list for quick indexing and finding of number of smaller/larger elements.
	 */
	public void updateSizes()
	{
		int index=0;
		//Merge the sorted buffer into the sorted list of elements.
		for(T element : buffer)
		{
			while(index < list.size() && list.get(index).compareTo(element) < 0)
			{
				index++;
			}
			if(index >= list.size())
			{
				list.add(element);
			}
			else
			{
				if(list.get(index).compareTo(element) != 0)
				{
					list.add(index, element);
				}
				else
				{
					throw new IllegalStateException("Element "+element+" is both in the buffer and the list.");
				}
			}
		}
		
		buffer.clear();
	}
	
	/**
	 * @return an unmodifiable view of the list buffer. 
	 */
	public Set<T> getBuffer()
	{
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
	public int getNumberLarger(T entry) {
		
		final int index = Collections.binarySearch(list, entry);
		final int numlarger;
		if(index < 0)
		{
			numlarger = list.size() + (index+1);
		}
		else
		{
			numlarger = list.size() - index;
		}
		
		final int bufferlarger = (int) buffer.stream().filter(element -> element.compareTo(entry) >= 0).count();
		return numlarger + bufferlarger;
	}

	@Override
	public int getNumberSmaller(T entry) {
		final int index = Collections.binarySearch(list, entry);
		final int numsmaller;
		if(index < 0)
		{
			numsmaller = -(index+1);
		}
		else
		{
			numsmaller = index+1;
		}
		
		final int buffersmaller = (int) buffer.stream().filter(element -> element.compareTo(entry) <= 0).count();
		
		return numsmaller + buffersmaller;
	}

	@Override
	public boolean add(T entry) {
		
		final boolean addresult =  set.add(entry);
		
		if(addresult)
		{
			buffer.add(entry);
			if(buffer.size() > maxbuffersize)
			{
				updateSizes();
			}
		}
		return addresult;
	}

	@Override
	public boolean remove(T entry) {
		buffer.remove(entry);
		list.remove(entry);
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((set == null) ? 0 : set.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		AmortizedSortedSetDecorator other = (AmortizedSortedSetDecorator) obj;
		if (set == null) {
			if (other.set != null)
				return false;
		} else if (!set.equals(other.set))
			return false;
		return true;
	}
	
}