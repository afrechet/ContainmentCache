package containmentcache.bitset.opt;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import containmentcache.bitset.opt.OptBitSet;

public class OptBitSetTests {

	private final static Map<Integer,Integer> ORDERING = ImmutableMap.<Integer,Integer>builder()
			.put(0, 10)
			.put(1,4)
			.put(2,0)
			.put(3,2)
			.put(4,8)
			.put(5,5)
			.put(6,1)
			.put(7,6)
			.put(8,9)
			.put(9,7)
			.put(10,3)
			.build();
	
	@Test
	public void testComparator() {

		final OptBitSet bs1 = new OptBitSet(ImmutableSet.of(1,2), ORDERING);
		final OptBitSet bs2 = new OptBitSet(ImmutableSet.of(2,3), ORDERING);

		assertEquals(0,bs1.compareTo(bs1));
		assertEquals(0,bs2.compareTo(bs2));
		
		assertEquals(1,bs1.compareTo(bs2));
		assertEquals(-1,bs2.compareTo(bs1));
	}
	
	
	@Test
	public void testSubset()
	{
		final OptBitSet bs1 = new OptBitSet(ImmutableSet.of(1,2), ORDERING);
		final OptBitSet bs2 = new OptBitSet(ImmutableSet.of(1,4), ORDERING);
		final OptBitSet bs3 = new OptBitSet(ImmutableSet.of(1,2,3), ORDERING);
		
		assertTrue(bs1.isSubset(bs1));
		assertTrue(bs2.isSubset(bs2));
		assertTrue(bs3.isSubset(bs3));
		
		assertTrue(bs1.isSubset(bs3));
		assertFalse(bs1.isSubset(bs2));
		assertFalse(bs2.isSubset(bs3));
	}
	

}
