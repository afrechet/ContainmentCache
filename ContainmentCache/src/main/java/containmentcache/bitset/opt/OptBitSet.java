package containmentcache.bitset.opt;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import lombok.EqualsAndHashCode;
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
 * {0:1L,1:3L,2:0L,3:3L} - map of block index to block long value.
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
	 * Map that takes a block index i to the long representation 'v' of
	 * the bit vector for the element with index between i and i+BLOCK_SIZE-1.
	 * Assumed to be in reverse order of block index.
	 */
	private final SortedMap<Integer,Long> blockvalues; 

	public <T> OptBitSet(Set<T> elements, Map<T,Integer> ordering)
	{
		blockvalues = new TreeMap<>(Collections.reverseOrder());
		for(final T element : elements)
		{
			final int order = ordering.get(element);
			final int blockindex = order / BLOCK_SIZE;
			final int blockbit = order % BLOCK_SIZE;
			
			final long blockvalue = blockvalues.getOrDefault(blockindex, 0L) + (1L<<blockbit);
			blockvalues.put(blockindex, blockvalue);
		}
	}
	
	/**
	 * @param bs - an opt bit set.
	 * @return true if and only if this bitset is a subset of the given bitset.
	 */
	public boolean isSubset(OptBitSet bs)
	{
		if(blockvalues.size() > bs.blockvalues.size())
		{
			return false;
		}
		else
		{
			final Iterator<Entry<Integer,Long>> myblockvalues = blockvalues.entrySet().iterator();
			final Iterator<Entry<Integer,Long>> hisblockvalues = bs.blockvalues.entrySet().iterator();
			
			//Loop through the smaller optbitset's entries.
			while(myblockvalues.hasNext())
			{
				final Entry<Integer,Long> myblockvalue = myblockvalues.next();
				final int myblockindex = myblockvalue.getKey();
				
				//Find the entry with same block index in the larger optbitset.
				Entry<Integer,Long> hisblockvalue = null;
				while(hisblockvalues.hasNext())
				{
					final Entry<Integer,Long> temphisblockvalue = hisblockvalues.next();
					//Return if we have found an entry with the right block index.
					if(temphisblockvalue.getKey() == myblockindex)
					{
						hisblockvalue = temphisblockvalue;
						break;
					}
					else if(temphisblockvalue.getKey() < myblockindex)
					{
						//We are past the right block index, we will never find it.
						return false;
					}
				}
				
				//If we did not find an entry with the right block index, the smaller optbitset has some unseen elements.
				if(hisblockvalue == null)
				{
					return false;
				}
				//Test that at that block index, the smaller optbitset is a subset of the larger
				//$$ S \subseteq T \Leftrightarrow S \cap T = S $$ 
				else if((myblockvalue.getValue() & hisblockvalue.getValue()) != myblockvalue.getValue())
				{
					return false;
				}
			}
			
			//We matched all entries in the smaller optbitset.
			return true;
		}
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
	 * @return 1 if the integer value of bs1 is smaller than bs2, -1 if the value of bs2 is smaller than bs1, 0 if the values are equal.
	 */
	private static int compare(OptBitSet bs1, OptBitSet bs2)
	{
		final Iterator<Entry<Integer,Long>> myblockentries = bs1.blockvalues.entrySet().iterator();
		final Iterator<Entry<Integer,Long>> hisblockentries = bs2.blockvalues.entrySet().iterator();
		
		//Traverse the blocks.
		while(myblockentries.hasNext() && hisblockentries.hasNext())
		{
			final Entry<Integer,Long> myblockentry = myblockentries.next();
			final Entry<Integer,Long> hisblockentry = hisblockentries.next();
			
			final int myblockindex = myblockentry.getKey();
			final int hisblockindex = hisblockentry.getKey();
			
			//Compare the block index; if its different, then the one with the larger block index wins.
			if(myblockindex > hisblockindex)
			{
				return 1;
			}
			else if(myblockindex < hisblockindex)
			{
				return -1;
			}
			else
			{
				final long myblockvalue = myblockentry.getValue();
				final long hisblockvalue = hisblockentry.getValue();
				//Same block index, compare block value; if it is different, then the largest one wins. 
				if(myblockvalue > hisblockvalue)
				{
					return 1;
				}
				else if(myblockvalue < hisblockvalue)
				{
					return -1;
				}
			}
		}
		
		//At least one bitset has been exhausted. If one of them still has entries, it wins.
		if(myblockentries.hasNext())
		{
			return 1;
		}
		else if(hisblockentries.hasNext())
		{
			return -1;
		}
		else
		{
			return 0;
		}
	}
}