package containmentcache.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Lists;

public class NestedIterableTests {

	@Test
	public void testOnePrimary() {
		
		final Iterable<Integer> primary = Arrays.asList(1);
		
		final Map<Integer,Iterable<Character>> secondaries = new HashMap<Integer,Iterable<Character>>();
		secondaries.put(1, Arrays.asList('a','b','c'));
		
		final List<Character> nestedlist = Lists.newArrayList(NestedIterables.nest(primary, secondaries));
		
		assertEquals(3,nestedlist.size());
		assertEquals(nestedlist,Arrays.asList('a','b','c'));
		
	}
	
	@Test
	public void testPrimaryEmpty()
	{
		@SuppressWarnings("unchecked")
		final Iterable<Integer> primary = Collections.EMPTY_LIST;
		
		final Map<Integer,Iterable<Character>> secondaries = new HashMap<Integer,Iterable<Character>>();
		secondaries.put(1, Arrays.asList('a','A'));
		secondaries.put(2, Arrays.asList('b','B'));
		secondaries.put(3, Arrays.asList('c','C'));
		
		final List<Character> nestedlist = Lists.newArrayList(NestedIterables.nest(primary, secondaries));
		
		assertTrue(nestedlist.isEmpty());
	}
	
	@Test
	public void testSecondaryEmpty()
	{
		final Iterable<Integer> primary = Arrays.asList(1,2,3);
		
		final Map<Integer,Iterable<Character>> secondaries = new HashMap<Integer,Iterable<Character>>();
		secondaries.put(1, Collections.emptyList());
		secondaries.put(2, Collections.emptyList());
		secondaries.put(3, Collections.emptyList());
		
		final List<Character> nestedlist = Lists.newArrayList(NestedIterables.nest(primary, secondaries));
		
		assertTrue(nestedlist.isEmpty());
	}
	
	@Test
	public void testSecondaryHole() {
		
		final Iterable<Integer> primary = Arrays.asList(1,2,3);
		
		final Map<Integer,Iterable<Character>> secondaries = new HashMap<Integer,Iterable<Character>>();
		secondaries.put(1, Arrays.asList('a','A'));
		secondaries.put(2, Collections.emptyList());
		secondaries.put(3, Arrays.asList('c','C'));
		
		final List<Character> nestedlist = Lists.newArrayList(NestedIterables.nest(primary, secondaries));
		assertEquals(4,nestedlist.size());
		assertEquals(nestedlist,Arrays.asList('a','A','c','C'));
		
	}
	
	
	@Test
	public void testSimple() {
		
		final Iterable<Integer> primary = Arrays.asList(1,2,3);
		
		final Map<Integer,Iterable<Character>> secondaries = new HashMap<Integer,Iterable<Character>>();
		secondaries.put(1, Arrays.asList('a','A'));
		secondaries.put(2, Arrays.asList('b','B'));
		secondaries.put(3, Arrays.asList('c','C'));
		
		final List<Character> nestedlist = Lists.newArrayList(NestedIterables.nest(primary, secondaries));
		assertEquals(6,nestedlist.size());
		assertEquals(nestedlist,Arrays.asList('a','A','b','B','c','C'));
		
	}

}
