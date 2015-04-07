package containmentcache.ubt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;

import containmentcache.ICacheSet;
import containmentcache.IContainmentCache;

public class Tree<E extends Comparable<E>> implements IContainmentCache<E>{
	
	private static final int MAX_ELEMENTS = 2500;
	
	private final E ROOT_VALUE = null;
	
	final Node<E> fRoot;
	
	/**
	 * Create an empty tree.
	 */
	public Tree()
	{
		fRoot = new Node<E>(ROOT_VALUE);
	}
	
	/**
	 * @param set - set of integer.
	 * @return the given set sorted in array form.
	 */
	private ArrayList<E> getArray(ICacheSet<E> set)
	{
		final int size = set.size(); 
		
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
		return size(fRoot);
	}
	/**
	 * @param root - a tree node.
	 * @return the size of the subtree rooted at root (computed recursively).
	 */
	private int size(Node<E> root)
	{
		int size = root.getEOP() ? 1 : 0;
		
		for(Entry<E,Node<E>> childEntry : root.getChildren())
		{
			Node<E> child = childEntry.getValue();
			size += size(child);
		}
		
		return size;
	}
	
	@Override
	public boolean contains(ICacheSet<E> set)
	{
		ArrayList<E> S = getArray(set);
		return contains(S,0,fRoot);
	}
	/**
	 * @param set - an integer set in sorted array form. 
	 * @param s - an index in the set array.
	 * @param root - a tree node. 
	 * @return true if there is a path from the given root node following nodes with elements from set[s:] (computed recursively).
	 */
	private boolean contains(ArrayList<E> set, int s, Node<E> root)
	{
		if(s==set.size())
		{
			return root.getEOP();
		}
		else
		{
			E first = set.get(s);
			Node<E> child = root.getChild(first);
			if(child == null)
			{
				return false;
			}
			else
			{
				return contains(set,s+1,child);
			}
		}
	}
	
	@Override
	public void remove(ICacheSet<E> set)
	{
		ArrayList<E> S = getArray(set);
		remove(S,0,fRoot);
	}
	/**
	 * Removes the EOP marker from the node at the end of the path given by set[s:] in the tree (computed recursively).
	 * @param set - an integer set in sorted array form. 
	 * @param s - an index in the set array.
	 * @param root - a tree node. 
	 * @return true if, after removal of the set, the visited node has no children (so it should be removed from its parent's children).
	 */
	private boolean remove(ArrayList<E> set, int s, Node<E> root)
	{
		if(s==set.size())
		{ 
			root.setEOP(false);
			return root.getChildren().isEmpty();
		}
		else
		{
			E first = set.get(s);
			Node<E> child = root.getChild(first);
			if(child == null)
			{
				return !root.getEOP() && root.getChildren().isEmpty();
			}
			else
			{
				boolean lastnode = remove(set,s+1,child);
				if(lastnode)
				{
					root.removeChild(child);
				}
				return !root.getEOP() && root.getChildren().isEmpty();
			}
		}
	}
	
	@Override
	public void add(ICacheSet<E> set)
	{
		ArrayList<E> S = getArray(set);
		insert(S, 0, fRoot);				
	}
	/**
	 * Insert the given set[s:] for given index s starting at the given note root.
	 * @param set - an array list form of a set to insert.
	 * @param s - the index in the array list.
	 * @param root - the node at which to insert.
	 */
	private void insert(ArrayList<E> set, int s, Node<E> root)
	{
		if(s == set.size())
		{
			root.setEOP(true);
			return;
		}
		
		E first = set.get(s);
		Node<E> child = root.getChild(first);
		if(child == null)
		{
			child = new Node<E>(first);
			root.addChild(child);
		}
		insert(set, s+1, child);
	}
	
	@Override
	public Collection<Set<E>> getSubsets(ICacheSet<E> set)
	{
		ArrayList<E> S = getArray(set);
		return getSubsets(S, 0, fRoot);
	}
	/**
	 * Get subset of given set[s:] for given index s starting at given root node.
	 * @param set 
	 * @param s
	 * @param root
	 * @return
	 */
	private Collection<Set<E>> getSubsets(ArrayList<E> set, int s, Node<E> root)
	{
		final Collection<Set<E>> subsets = new LinkedList<Set<E>>();
		
		if(root.getEOP())
		{
			final Set<E> subset = new HashSet<E>();
			if(root.getElement() != ROOT_VALUE)
			{
				subset.add(root.getElement());
			}
			subsets.add(subset);
		}
		
		for(int i=s;i<set.size();i++)
		{
			final E ielement = set.get(i); 
			final Node<E> ichild = root.getChild(ielement);
			if(ichild != null)
			{
				final Collection<Set<E>> isubsets = getSubsets(set,i+1,ichild);
				for(final Set<E> isubset : isubsets)
				{
					if(root.getElement() != ROOT_VALUE)
					{
						isubset.add(root.getElement());
					}
					subsets.add(isubset);
				}
			}
		}
		
		return subsets;		
	}
	
	@Override
	public Collection<Set<E>> getSupersets(ICacheSet<E> set)
	{
		ArrayList<E> S = getArray(set);
		return getSupersets(S, 0, fRoot);
	}
	private Collection<Set<E>> getSupersets(ArrayList<E> set, int s, Node<E> root)
	{
		final Collection<Set<E>> supersets = new LinkedList<Set<E>>();

		final E first;
		if(s == set.size())
		{
			first = null;
			
			if(root.getEOP())
			{
				final Set<E> superset = new HashSet<E>();
				if(root.getElement() != ROOT_VALUE)
				{
					superset.add(root.getElement());
				}
				supersets.add(superset);
			}
		}
		else
		{
			first = set.get(s);
		}
		
		for(Entry<E,Node<E>> childEntry : root.getChildren())
		{
			E childElement = childEntry.getKey();
			Node<E> child = childEntry.getValue();
			
			final Collection<Set<E>> csupersets;
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
				csupersets = new LinkedList<Set<E>>();
			}
			
			for(final Set<E> csupserset : csupersets)
			{
				if(root.getElement() != ROOT_VALUE)
				{
					csupserset.add(root.getElement());
				}
				supersets.add(csupserset);
			}	
		}	
		
		return supersets;
	}

	@Override
	public int getNumberSubsets(ICacheSet<E> set) {
		ArrayList<E> S = getArray(set);
		return getNumberSubsets(S,0,fRoot);
	}
	private int getNumberSubsets(ArrayList<E> set, int s, Node<E> root)
	{
		int num = 0;
		
		if(root.getEOP())
		{
			num ++;
		}
		
		for(int i=s;i<set.size();i++)
		{
			final E ielement = set.get(i); 
			final Node<E> ichild = root.getChild(ielement);
			if(ichild != null)
			{
				int inum = getNumberSubsets(set,i+1,ichild);
				num += inum;
			}
		}		
		return num;		
	}
	

	@Override
	public int getNumberSupersets(ICacheSet<E> set) {
		ArrayList<E> S = getArray(set);
		return getNumberSupersets(S,0,fRoot);
	}
	private int getNumberSupersets(ArrayList<E> set, int s, Node<E> root)
	{
		int num = 0;

		final E first;
		if(s == set.size())
		{
			first = null;
			
			if(root.getEOP())
			{
				num++;
			}
		}
		else
		{
			first = set.get(s);
		}
		
		for(final Entry<E,Node<E>> childEntry : root.getChildren())
		{
			final E childElement = childEntry.getKey();
			final Node<E> child = childEntry.getValue();
			
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
	
}
