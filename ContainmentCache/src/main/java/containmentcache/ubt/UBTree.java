package containmentcache.ubt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import containmentcache.ICacheSet;
import containmentcache.IContainmentCache;

/**
 * A recursive implementation of the Unlimited Branching Tree (UBTree) from
 * Hoffmann, Jörg, and Jana Koehler. "A new method to index and query sets." IJCAI. Vol. 99. 1999.
 * 
 * Corresponds to a tree where each node represents a set element, and a path in the tree is a set.
 * 
 * @author afrechet
 *
 * @param <E> - elements in the tree.
 * @param <T> - the type of additional content in cache entries.
 */
public class UBTree<E extends Comparable<E>,T> implements IContainmentCache<E,T>{
	
	private static final int MAX_ELEMENTS = 2500;
	
	private final E ROOT_VALUE = null;
	
	private final Node fRoot;
	private int fSize;
	
	/**
	 * Create an empty tree.
	 */
	public UBTree()
	{
		fRoot = new Node(ROOT_VALUE);
		fSize = 0;
	}
	
	/**
	 * @param set - set of integer.
	 * @return the given set sorted in array form.
	 */
	private ArrayList<E> getArray(ICacheSet<E,T> set)
	{
		final int size = set.getElements().size(); 
		
		if(size > MAX_ELEMENTS)
		{
			throw new IllegalArgumentException("Cannot add a set of more than "+MAX_ELEMENTS+" elements, as this may create overflow errors with the call stack and the recursive methods involved.");
		}
		
		ArrayList<E> a = new ArrayList<E>(size);
		for(E e : set.getElements())
		{		
			if(e == ROOT_VALUE)
			{
				throw new IllegalArgumentException("Cannot add set with element \""+e+"\" equal to the reserved root value (\""+ROOT_VALUE+")\"");
			}
			a.add(e);
		}
		Collections.sort(a);
		return a;
	}
	
	@Override
	public int size()
	{
		return fSize;
	}
	
	@Override
	public boolean contains(ICacheSet<E,T> set)
	{
		ArrayList<E> S = getArray(set);
		return contains(S,0,fRoot,set);
	}
	/**
	 * @param set - an integer set in sorted array form. 
	 * @param s - an index in the set array.
	 * @param root - a tree node.
	 * @param entry - the cache set entry we are looking for. 
	 * @return true if there is a path from the given root node following nodes with elements from set[s:] (computed recursively).
	 */
	private boolean contains(ArrayList<E> set, int s, Node root, ICacheSet<E,T> entry)
	{
		if(s==set.size())
		{
			return root.entries.contains(entry);
		}
		else
		{
			E first = set.get(s);
			Node child = root.children.get(first);
			if(child == null)
			{
				return false;
			}
			else
			{
				return contains(set,s+1,child,entry);
			}
		}
	}
	
	@Override
	public void remove(ICacheSet<E,T> set)
	{
		ArrayList<E> S = getArray(set);
		remove(S,0,fRoot,set);
	}
	/**
	 * Removes the EOP marker from the node at the end of the path given by set[s:] in the tree (computed recursively).
	 * @param set - an integer set in sorted array form. 
	 * @param s - an index in the set array.
	 * @param root - a tree node. 
	 * @param entry - the entry we wish to remove.
	 * @return true if, after removal of the set, the visited node has no children (so it should be removed from its parent's children).
	 */
	private boolean remove(ArrayList<E> set, int s, Node root, ICacheSet<E,T> entry)
	{
		if(s==set.size())
		{ 
			if(!root.entries.isEmpty())
			{
				fSize--;
			}
			root.entries.remove(entry);
			return root.children.isEmpty();
		}
		else
		{
			E first = set.get(s);
			Node child = root.children.get(first);
			if(child == null)
			{
				return root.entries.isEmpty() && root.children.isEmpty();
			}
			else
			{
				boolean lastnode = remove(set,s+1,child,entry);
				if(lastnode)
				{
					root.children.remove(child);
				}
				return root.entries.isEmpty() && root.children.isEmpty();
			}
		}
	}
	
	@Override
	public void add(ICacheSet<E,T> set)
	{
		ArrayList<E> S = getArray(set);
		insert(S, 0, fRoot, set);				
	}
	/**
	 * Insert the given set[s:] for given index s starting at the given note root.
	 * @param set - an array list form of a set to insert.
	 * @param s - the index in the array list.
	 * @param root - the node at which to insert.
	 * @param entry - the cache set entry we wish to add.
	 */
	private void insert(ArrayList<E> set, int s, Node root, ICacheSet<E,T> entry)
	{
		if(s == set.size())
		{
			if(root.entries.isEmpty())
			{
				fSize++;
			}
			root.entries.add(entry);
			return;
		}
		
		E first = set.get(s);
		Node child = root.children.get(first);
		if(child == null)
		{
			child = new Node(first);
			root.children.put(first,child);
		}
		insert(set, s+1, child, entry);
	}
	
	@Override
	public Collection<ICacheSet<E,T>> getSubsets(ICacheSet<E,T> set)
	{
		ArrayList<E> S = getArray(set);
		return getSubsets(S, 0, fRoot);
	}
	/**
	 * @param set - set of elements.
	 * @param s - index in set of elements.
	 * @param root - root node.
	 * @return the cache sets that are subsets of given set[s:] for given index s starting at given root node.
	 */
	private Collection<ICacheSet<E,T>> getSubsets(ArrayList<E> set, int s, Node root)
	{
		final Collection<ICacheSet<E,T>> subsets = new LinkedList<ICacheSet<E,T>>();
		
		if(!root.entries.isEmpty())
		{
			subsets.addAll(root.entries);
		}
		
		for(int i=s;i<set.size();i++)
		{
			final E ielement = set.get(i); 
			final Node ichild = root.children.get(ielement);
			if(ichild != null)
			{
				final Collection<ICacheSet<E,T>> isubsets = getSubsets(set,i+1,ichild);
				subsets.addAll(isubsets);
			}
		}
		
		return subsets;		
	}
	
	@Override
	public Collection<ICacheSet<E,T>> getSupersets(ICacheSet<E,T> set)
	{
		ArrayList<E> S = getArray(set);
		return getSupersets(S, 0, fRoot);
	}
	/**
	 * @param set - set of elements.
	 * @param s - index in set of elements.
	 * @param root - root node.
	 * @return the cache sets that are supersets of given set[s:] for given index s starting at given root node.
	 */
	private Collection<ICacheSet<E,T>> getSupersets(ArrayList<E> set, int s, Node root)
	{
		final Collection<ICacheSet<E,T>> supersets = new LinkedList<ICacheSet<E,T>>();

		final E first;
		if(s == set.size())
		{
			first = null;
			
			if(!root.entries.isEmpty())
			{
				supersets.addAll(root.entries);
			}
		}
		else
		{
			first = set.get(s);
		}
		
		for(Entry<E,Node> childEntry : root.children.entrySet())
		{
			E childElement = childEntry.getKey();
			Node child = childEntry.getValue();
			
			final Collection<ICacheSet<E,T>> csupersets;
			if(first == null || childElement.compareTo(first) < 0)
			{
				csupersets = getSupersets(set, s, child);
			}
			else if(childElement.compareTo(first) == 0)
			{
				csupersets = getSupersets(set, s+1, child);
			}
			else
			{
				csupersets = new LinkedList<ICacheSet<E,T>>();
			}
			
			supersets.addAll(csupersets);	
		}	
		
		return supersets;
	}

	@Override
	public int getNumberSubsets(ICacheSet<E,T> set) {
		ArrayList<E> S = getArray(set);
		return getNumberSubsets(S,0,fRoot);
	}
	private int getNumberSubsets(ArrayList<E> set, int s, Node root)
	{
		int num = 0;
		
		num+=root.entries.size();
		
		for(int i=s;i<set.size();i++)
		{
			final E ielement = set.get(i); 
			final Node ichild = root.children.get(ielement);
			if(ichild != null)
			{
				num += getNumberSubsets(set,i+1,ichild);
			}
		}		
		return num;		
	}
	

	@Override
	public int getNumberSupersets(ICacheSet<E,T> set) {
		ArrayList<E> S = getArray(set);
		return getNumberSupersets(S,0,fRoot);
	}
	private int getNumberSupersets(ArrayList<E> set, int s, Node root)
	{
		int num = 0;

		final E first;
		if(s == set.size())
		{
			first = null;
			
			num += root.entries.size();
		}
		else
		{
			first = set.get(s);
		}
		
		for(final Entry<E,Node> childEntry : root.children.entrySet())
		{
			final E childElement = childEntry.getKey();
			final Node child = childEntry.getValue();
			
			final int cnum;
			if(first == null || childElement.compareTo(first) < 0)
			{
				cnum = getNumberSupersets(set, s, child);
			}
			else if(childElement.compareTo(first) == 0)
			{
				cnum = getNumberSupersets(set, s+1, child);
			}
			else
			{
				cnum = 0;
			}
			
			num += cnum;	
		}	
		
		return num;
	}
	
	/**
	 * UBTree node
	 * @author afrechet
	 */
	private class Node {
		
		/*
		 * The entries at that node. The entries all correspond to the same
		 * set, which in turn corresponds to the elements encountered in the unique
		 * path from the root to this node. 
		 */
		final Set<ICacheSet<E,T>> entries;
		//The element corresponding to this node.
		final E element;
		//The children of this node.
		final Map<E,Node> children;
		
		/**
		 * Basic tree node.
		 * @param e - element contained at the node.
		 */
		public Node(E e)
		{
			entries = new HashSet<ICacheSet<E,T>>();
			element = e;
			children = new HashMap<E,Node>();
		}
		
		@Override
		public String toString()
		{
			return element+" ("+entries.toString()+") "+children.keySet();
		}
	}

	
	
}
