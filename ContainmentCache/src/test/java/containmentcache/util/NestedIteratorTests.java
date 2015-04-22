package containmentcache.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Lists;

public class NestedIteratorTests {

	@Test
	public void testOnePrimary() {
		
		final Iterator<Integer> primary = Arrays.asList(1).iterator();
		
		final Map<Integer,Iterable<Character>> secondaries = new HashMap<Integer,Iterable<Character>>();
		secondaries.put(1, Arrays.asList('a','b','c'));
		
		final List<Character> nestedlist = Lists.newArrayList(new NestedIterator<Character>(primary, secondaries));
		
		assertEquals(3,nestedlist.size());
		assertEquals(nestedlist,Arrays.asList('a','b','c'));
		
	}
	
	@Test
	public void testPrimaryEmpty()
	{
		@SuppressWarnings("unchecked")
		final Iterator<Integer> primary = Collections.EMPTY_LIST.iterator();
		
		final Map<Integer,Iterable<Character>> secondaries = new HashMap<Integer,Iterable<Character>>();
		secondaries.put(1, Arrays.asList('a','A'));
		secondaries.put(2, Arrays.asList('b','B'));
		secondaries.put(3, Arrays.asList('c','C'));
		
		final List<Character> nestedlist = Lists.newArrayList(new NestedIterator<Character>(primary, secondaries));
		
		assertTrue(nestedlist.isEmpty());
	}
	
	@Test
	public void testSecondaryEmpty()
	{
		final Iterator<Integer> primary = Arrays.asList(1,2,3).iterator();
		
		final Map<Integer,Iterable<Character>> secondaries = new HashMap<Integer,Iterable<Character>>();
		secondaries.put(1, Collections.emptyList());
		secondaries.put(2, Collections.emptyList());
		secondaries.put(3, Collections.emptyList());
		
		final List<Character> nestedlist = Lists.newArrayList(new NestedIterator<Character>(primary, secondaries));
		
		assertTrue(nestedlist.isEmpty());
	}
	
	@Test
	public void testSecondaryHole() {
		
		final Iterator<Integer> primary = Arrays.asList(1,2,3).iterator();
		
		final Map<Integer,Iterable<Character>> secondaries = new HashMap<Integer,Iterable<Character>>();
		secondaries.put(1, Arrays.asList('a','A'));
		secondaries.put(2, Collections.emptyList());
		secondaries.put(3, Arrays.asList('c','C'));
		
		final List<Character> nestedlist = Lists.newArrayList(new NestedIterator<Character>(primary, secondaries));
		assertEquals(4,nestedlist.size());
		assertEquals(nestedlist,Arrays.asList('a','A','c','C'));
		
	}
	
	
	@Test
	public void testSimple() {
		
		final Iterator<Integer> primary = Arrays.asList(1,2,3).iterator();
		
		final Map<Integer,Iterable<Character>> secondaries = new HashMap<Integer,Iterable<Character>>();
		secondaries.put(1, Arrays.asList('a','A'));
		secondaries.put(2, Arrays.asList('b','B'));
		secondaries.put(3, Arrays.asList('c','C'));
		
		final List<Character> nestedlist = Lists.newArrayList(new NestedIterator<Character>(primary, secondaries));
		assertEquals(6,nestedlist.size());
		assertEquals(nestedlist,Arrays.asList('a','A','b','B','c','C'));
		
	}

}
