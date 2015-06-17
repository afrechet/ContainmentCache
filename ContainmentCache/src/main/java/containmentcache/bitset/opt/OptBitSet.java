package containmentcache.bitset.opt;

import java.util.Map;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

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
 * {1L,3L,0L,3L] - array of block values.
 * 
 * The two required operations can be done quickly using those block values.
 * 
 * @author afrechet
 */
@EqualsAndHashCode
@ToString
public class OptBitSet implements Comparable<OptBitSet>
{
	/*
	 * Size of blocks. Needs to be less than 62 to allow long representation
	 * of numbers with BLOCK_SIZE bits on a 64-bits architecture.
	 */
	private final static int BLOCK_SIZE = 60;
	
	/*
	 * Long array where the i-th entry is the value of the long representation 'v' of
	 * the bit vector for the element with index between i and i+BLOCK_SIZE-1.
	 * Assumed to be in reverse order of block index.
	 */
	private final long[] blockvalues; 

	/**
	 * @param ordering - the ordering of the universe used to sort elements. It is assumed the domain of the ordering contains all possible elements,
	 * and that the minimum and maximum indices in the ordering are 0 and (size of universe) - 1. 
	 */
	public <T> OptBitSet(@NonNull Set<T> elements, @NonNull Map<T,Integer> ordering)
	{
		final int numelements = ordering.size();
		final int numblocks = (int) Math.ceil((double) (numelements-1) / (double) BLOCK_SIZE);
		blockvalues = new long[numblocks];
		for(final T element : elements)
		{
			final int order = ordering.get(element);
			final int blockindex = order / BLOCK_SIZE;
			final int blockbit = order % BLOCK_SIZE;
			blockvalues[blockindex] += (1L<<blockbit);
		}
	}
	
	/**
	 * @param bs - an opt bit set.
	 * @return true if and only if this bitset is a subset of the given bitset.
	 */
	public boolean isSubset(OptBitSet bs)
	{
		for(int i=0;i < Math.max(blockvalues.length,bs.blockvalues.length);i++)
		{
			final long myblockvalue = i < blockvalues.length ? blockvalues[i] : 0L;
			final long hisblockvalue = i < bs.blockvalues.length ? bs.blockvalues[i] : 0L;
			
			if((myblockvalue & hisblockvalue) != myblockvalue)
			{
				return false;
			}
		}
		return true;
	}
	
	@Override
	public int compareTo(OptBitSet bs)
	{
		return compare(this,bs);
	}
	
	/**
	 * Compares two optbitset based on their integer values.
	 * @param bs1 
	 * @param bs2
	 * @return -1 if the integer value of bs1 is smaller than bs2, 1 if the value of bs2 is smaller than bs1, 0 if the values are equal.
	 */
	private static int compare(OptBitSet bs1, OptBitSet bs2)
	{
		for(int i=Math.max(bs1.blockvalues.length,bs2.blockvalues.length)-1;i >=0 ;i--)
		{
			final long myblockvalue = i < bs1.blockvalues.length ? bs1.blockvalues[i] : 0L;
			final long hisblockvalue = i < bs2.blockvalues.length ? bs2.blockvalues[i] : 0L;
			
			if(myblockvalue > hisblockvalue)
			{
				return 1;
			}
			else if(myblockvalue < hisblockvalue)
			{
				return -1;
			}
		}
		return 0;
	}
}