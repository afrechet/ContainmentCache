package containmentcache.ubt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;
import containmentcache.ICacheEntry;
import containmentcache.IContainmentCache;

/**
 * A recursive implementation of the Unlimited Branching Tree (UBTree) from
 * Hoffmann, Jörg, and Jana Koehler. "A new method to index and query sets." IJCAI. Vol. 99. 1999.
 * 
 * Corresponds to a tree where each node represents a set element, and a path in the tree is a set.
 * 
 * The {@link IContainmentCache#getSubsets(containmentcache.ICacheEntry)} and {@link IContainmentCache#getSupersets(containmentcache.ICacheEntry)} methods return iterables that will lazily traverse the tree
 * for the next sub/superset. These iterators might be slightly heavyweight has they need to keep track of what has been traversed in the tree. 
 * 
 * @author afrechet
 *
 * @param <E> - elements in the tree.
 * @param <T> - the type of additional content in cache entries.
 */
@NotThreadSafe
public class UBTree<E,C extends ICacheEntry<E>> implements IContainmentCache<E,C>{
	
	/*
	 * Maximum number of elements in universe due to possible overflow errors
	 * caused by recursive function calls.
	 */
	private static final int MAX_ELEMENTS = 2500;
	
	private final E ROOT_VALUE = null;
	
	private final Node fRoot;
	private int fSize;

	private final Comparator<E> comparator;
	
	/**
	 * Create an empty tree.
	 */
	public UBTree(Comparator<E> comparator)
	{
		this.comparator = comparator;
		fRoot = new Node(ROOT_VALUE);
		fSize = 0;
	}
	
	/**
	 * @param set - set of integer.
	 * @return the given set sorted in array form.
	 */
	private ArrayList<E> getArray(ICacheEntry<E> set)
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
		Collections.sort(a, comparator);
		return a;
	}
	
	@Override
	public int size()
	{
		return fSize;
	}
	
	@Override
	public boolean contains(C set)
	{
		ArrayList<E> S = getArray(set);
		return contains(S,0,fRoot,set);
	}
	/**
	 * @param set - an integer set in sorted array form. 
	 * @param s - an index in the set array.
	 * @param root - a tree node.
	 * @param set2 - the cache set entry we are looking for. 
	 * @return true if there is a path from the given root node following nodes with elements from set[s:] (computed recursively).
	 */
	private boolean contains(ArrayList<E> set, int s, Node root, ICacheEntry<E> set2)
	{
		if(s==set.size())
		{
			return root.entries.contains(set2);
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
				return contains(set,s+1,child,set2);
			}
		}
	}
	
	@Override
	public void remove(C set)
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
	private boolean remove(ArrayList<E> set, int s, Node root, C entry)
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
	public void add(C set)
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
	private void insert(ArrayList<E> set, int s, Node root, C entry)
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
	public Iterable<C> getSubsets(ICacheEntry<E> set)
	{
		return new Iterable<C>(){
			@Override
			public Iterator<C> iterator() {
				return new SubsetsIterator(fRoot, getArray(set));
			}};
	}
	
	
	
	@Override
	public Iterable<C> getSupersets(ICacheEntry<E> set)
	{
		return new Iterable<C>(){
			@Override
			public Iterator<C> iterator() {
				return new SupersetsIterator(fRoot, getArray(set));
			}};
	}
	

	@Override
	public Iterable<C> getSets() {
		return new Iterable<C>(){
			@Override
			public Iterator<C> iterator() {
				return new SetIterator(fRoot);
			}};
	}
	
	
	@Override
	public int getNumberSubsets(ICacheEntry<E> set) {
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
	public int getNumberSupersets(ICacheEntry<E> set) {
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
			if(first == null) {
				cnum = getNumberSupersets(set, s, child);
			} else {
				int compare = comparator.compare(childElement, first);
				if (compare < 0) {
					cnum = getNumberSupersets(set, s, child);
				} else if (compare == 0) {
					cnum = getNumberSupersets(set, s+1, child);
				} else {
					cnum = 0;
				}
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
		private final Set<C> entries;
		//The element corresponding to this node.
		private final E element;
		//The children of this node.
		private final Map<E,Node> children;
		
		/**
		 * Basic tree node.
		 * @param e - element contained at the node.
		 */
		public Node(E e)
		{
			entries = new HashSet<C>();
			element = e;
			children = new HashMap<E,Node>();
		}
		
		@Override
		public String toString()
		{
			return element+" ("+entries.toString()+") "+children.keySet();
		}
	}
	
	
	/*
	 * Lazy UBTree traversal iterators. 
	 */
	
	/**
	 * Container class for the two types of UBTree iterators.
	 * 
	 * @author afrechet
	 */
	private class IteratorEntry
	{
		private final Node node;
		private final int index;
		
		public IteratorEntry(Node n, int i)
		{
			node = n;
			index = i;
		}
	}
	
	/**
	 * Abstract tree traversal iterator used to implement sub/superset iterators.
	 * 
	 * @author afrechet
	 */
	private abstract class ATreeIterator implements Iterator<C>
	{
		final ArrayList<E> fSet;
		final Queue<IteratorEntry> fQueue;
		Iterator<C> fCurrentIterator;
		
		public ATreeIterator(Node root, ArrayList<E> set)
		{
			fSet = set;
			
			//Go breath-first in UBT traversal for next node.
			fQueue = new LinkedList<IteratorEntry>();
			fQueue.add(new IteratorEntry(root, 0));
			
			fCurrentIterator = Collections.emptyIterator();
		}
		
		/**
		 * Process a node obtained from the traversal queue, possibly updating the current iterator if a match is found,
		 * and also adding the node's children to the traversal queue for future processing.
		 * 
		 * @param node - a node.
		 * @param index - the index in the set at which we are.
		 */
		abstract void processNode(Node node, int index);
		
		/**
		 * Empties the traversal queue has long as the current iterator has no next element.
		 */
		private final void updateCurrentIterator()
		{
			while(!fQueue.isEmpty() && !fCurrentIterator.hasNext())
			{
				//Get node from the queue.
				final IteratorEntry entry = fQueue.remove();
				final Node node = entry.node;
				final int index = entry.index;
				
				processNode(node,index);
			}
		}
		
		@Override
		public final boolean hasNext() {
			updateCurrentIterator();
			return fCurrentIterator.hasNext();
		}

		@Override
		public final C next() {
			updateCurrentIterator();
			return fCurrentIterator.next();
		}
	}
	
	/**
	 * Tree iterator that visits all nodes.
	 * 
	 * @author afrechet
	 */
	private class SetIterator extends ATreeIterator
	{
		public SetIterator(Node root) {
			super(root, null);
		}

		@Override
		void processNode(Node node, int index) {
			if(!node.entries.isEmpty())
			{
				fCurrentIterator = node.entries.iterator();
			}
			
			//Add node's children to the queue.
			for(Node child : node.children.values())
			{
				fQueue.add(new IteratorEntry(child, 0));
			}
		}
	}
	
	/**
	 * Tree iterator that is forced to follow elements from the given set
	 * in order to end up on subsets.
	 * 
	 * @author afrechet
	 */
	private class SubsetsIterator extends ATreeIterator
	{
		public SubsetsIterator(Node root, ArrayList<E> set) {
			super(root, set);
		}

		@Override
		void processNode(Node node, int index) {
			if(!node.entries.isEmpty())
			{
				fCurrentIterator = node.entries.iterator();
			}
			
			//Add node's children to the queue.
			for(int i=index;i<fSet.size();i++)
			{
				final E ielement = fSet.get(i); 
				final Node ichild = node.children.get(ielement);
				if(ichild != null)
				{
					fQueue.add(new IteratorEntry(ichild, i+1));
				}
			}
		}
	}
	
	/**
	 * Tree iterator that follows any element as long as it eventually visits all
	 * the ones from its given set, to end up on subsets of the latter.
	 * 
	 * @author afrechet
	 */
	private class SupersetsIterator extends ATreeIterator
	{
		public SupersetsIterator(Node root, ArrayList<E> set) {
			super(root, set);
		}

		@Override
		void processNode(Node node, int index) {
			
			//Get first element, possibly processing node.
			final E first;
			if(index == fSet.size())
			{
				first = null;
				
				if(!node.entries.isEmpty())
				{
					fCurrentIterator = node.entries.iterator();
				}
			}
			else
			{
				first = fSet.get(index);
			}
			
			//Add node's children to the queue.
			for(Entry<E,Node> childEntry : node.children.entrySet())
			{
				final E childElement = childEntry.getKey();
				final Node child = childEntry.getValue();
				if (first == null) {
					fQueue.add(new IteratorEntry(child,index));
				} else {
					int compare = comparator.compare(childElement, first);
					if(compare < 0)
					{
						fQueue.add(new IteratorEntry(child,index));
					}
					else if(compare == 0)
					{
						fQueue.add(new IteratorEntry(child,index+1));
					}
				}
			}
		}
	}

}
