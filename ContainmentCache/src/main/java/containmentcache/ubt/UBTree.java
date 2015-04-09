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
 * @author afrechet
 *
 * @param <E>
 */
//TODO Make thread safe.
public class UBTree<E extends Comparable<E>> implements IContainmentCache<E>{
	
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
		return fSize;
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
	private boolean contains(ArrayList<E> set, int s, Node root)
	{
		if(s==set.size())
		{
			return root.getEOP();
		}
		else
		{
			E first = set.get(s);
			Node child = root.getChild(first);
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
	private boolean remove(ArrayList<E> set, int s, Node root)
	{
		if(s==set.size())
		{ 
			if(root.getEOP())
			{
				fSize--;
			}
			root.setEOP(false);
			return root.getChildren().isEmpty();
		}
		else
		{
			E first = set.get(s);
			Node child = root.getChild(first);
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
	private void insert(ArrayList<E> set, int s, Node root)
	{
		if(s == set.size())
		{
			if(!root.getEOP())
			{
				fSize++;
			}
			root.setEOP(true);
			return;
		}
		
		E first = set.get(s);
		Node child = root.getChild(first);
		if(child == null)
		{
			child = new Node(first);
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
	private Collection<Set<E>> getSubsets(ArrayList<E> set, int s, Node root)
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
			final Node ichild = root.getChild(ielement);
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
	private Collection<Set<E>> getSupersets(ArrayList<E> set, int s, Node root)
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
		
		for(Entry<E,Node> childEntry : root.getChildren())
		{
			E childElement = childEntry.getKey();
			Node child = childEntry.getValue();
			
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
	private int getNumberSubsets(ArrayList<E> set, int s, Node root)
	{
		int num = 0;
		
		if(root.getEOP())
		{
			num ++;
		}
		
		for(int i=s;i<set.size();i++)
		{
			final E ielement = set.get(i); 
			final Node ichild = root.getChild(ielement);
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
	private int getNumberSupersets(ArrayList<E> set, int s, Node root)
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
		
		for(final Entry<E,Node> childEntry : root.getChildren())
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
		
		//Marks "End Of Path", that the path from the root to here corresponds to a set in the data structure.
		private boolean fEOP;
		//The element corresponding to this node.
		private final E fElement;
		//The children of this node.
		private final Map<E,Node> fChildren;
		
		/**
		 * Basic tree node.
		 * @param element - element contained at the node.
		 */
		public Node(E element)
		{
			this.fEOP = false;
			this.fElement = element;
			fChildren = new HashMap<E,Node>();
		}
		
		public boolean removeChild(Node child)
		{
			return (fChildren.remove(child.getElement())!=null);
		}
		
		public boolean addChild(Node child)
		{
			return (fChildren.put(child.fElement, child)!=null);
		}
		
		public boolean getEOP()
		{
			return this.fEOP;
		}
		
		public void setEOP(boolean eop)
		{
			this.fEOP = eop;
		}
		
		public E getElement()
		{
			return this.fElement;
		}
		
		public Node getChild(E element)
		{
			return fChildren.get(element);
		}
		
		public Set<Entry<E,Node>> getChildren()
		{
			return Collections.unmodifiableSet(fChildren.entrySet());
		}
		
		@Override
		public String toString()
		{
			return fElement+" ("+fEOP+") "+fChildren.keySet();
		}
	}

	
	
}
