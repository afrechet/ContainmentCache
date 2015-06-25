package containmentcache.bitset.opt.sortedset.redblacktree;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.StreamSupport;

import lombok.NonNull;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import containmentcache.bitset.opt.sortedset.ISortedSet;
import containmentcache.bitset.opt.sortedset.redblacktree.Node.Color;
import containmentcache.util.NestedIterables;

/**
 * A red-black tree implementation that also keeps subtree sizes at the nodes to provide constant time 
 * {@link #getNumberSmaller} and {@link #getNumberLarger} methods.
 * 
 * Based on the red-black tree of 
 * Cormen, Thomas H. Introduction to algorithms. MIT press, 2009.
 * with added node properties for subtree sizes.
 * 
 * @author afrechet
 *
 * @param <T>
 */
public class RedBlackTree<T extends Comparable<T>> implements ISortedSet<T> {

	private Node<T> fRoot;
	
	public RedBlackTree()
	{
		fRoot = null;
	}
	
	@Override
	public Iterable<T> getLarger(T entry) {
		final Iterable<Node<T>> largernodes = NestedIterables.nest(
				//Get all the nodes that are larger than the given entry in the reversed insertion path (reversed to have the right ordering).
				Iterables.filter(Lists.reverse(Lists.newLinkedList(getInsertionPath(entry))), pathnode -> pathnode.content.compareTo(entry) >= 0),
				//Grab the right-subtree of these nodes (in order), as well as the node.
				pathnode -> {
					final Iterable<Node<T>> subtree = pathnode.right != null ? getInorderTraversal(pathnode.right) : Collections.emptyList();
					return Iterables.concat(Arrays.asList(pathnode), subtree);
				}
			);
		return Iterables.transform(largernodes, node -> node.content);
	}

	@Override
	public int getNumberLarger(T entry) {
		return StreamSupport.stream(getInsertionPath(entry).spliterator(), false)
				//If you go left on a node on the path, add the node and the right size.
				.map(pathnode -> pathnode.content.compareTo(entry) >= 0 ? 1 + pathnode.rightSize : 0)
				//Sum the sizes up.
				.reduce(0, Integer::sum);
	}
	
	@Override
	public Iterable<T> getSmaller(T entry) {
		final Iterable<Node<T>> smallernodes = NestedIterables.nest(
			//Get all the nodes that are smaller than the given entry in the insertion path.
			Iterables.filter(getInsertionPath(entry), pathnode -> pathnode.content.compareTo(entry) <= 0),
			//Grab the left-subtree of these nodes (in order), as well as the node.
			pathnode -> {
				final Iterable<Node<T>> subtree = pathnode.left != null ? getInorderTraversal(pathnode.left) : Collections.emptyList();
				return Iterables.concat(subtree,Arrays.asList(pathnode));
			}
		);
		//Get the node content.
		return Iterables.transform(smallernodes, node -> node.content);
	}

	@Override
	public int getNumberSmaller(T entry) {
		return StreamSupport.stream(getInsertionPath(entry).spliterator(), false)
				//If you go right on a node on the path, add the node and the left size.
				.map(node -> node.content.compareTo(entry) <= 0 ? 1 + node.leftSize : 0)
				//Sum the sizes up.
				.reduce(0, Integer::sum);
	}
	
	@Override
	public boolean add(T entry) {
		if(entry == null) throw new IllegalArgumentException("Cannot add null to red-black tree.");
		
		if(fRoot == null)
		{
			fRoot = new Node<T>(entry,Color.BLACK,null,null,null);
			return true;
		}
		else
		{
			//Get the parent (or the actual node we are trying to insert).
			final Node<T> closestnode = Iterables.getLast(getInsertionPath(entry));
			final Node<T> newnode = new Node<T>(entry,Color.RED,closestnode,null,null);
			//Closest node is actually a right parent.
			if(closestnode.content.compareTo(entry) < 0)
			{
				closestnode.right = newnode;
			}
			//Closest node actually contains content.
			else if(closestnode.content.compareTo(entry) == 0)
			{
				return false;
			}
			//Closest node is actually a left parent.
			else
			{
				closestnode.left = newnode;
			}
			
			//Update subtree sizes.
			updateSubtreeSizes(closestnode);
			
			//Balance the tree.
			postAddBalancing(newnode);
			
			return true;
		}
	}
	
	private void postAddBalancing(final @NonNull Node<T> node)
	{
		Node<T> currentnode = node;
		
		while(currentnode.parent != null && currentnode.parent.color == Color.RED)
		{
			//Get the family around the local node.
			Node<T> parent = currentnode.parent;
			final Node<T> grandparent = parent.parent;
			if(grandparent == null)
			{
				throw new IllegalStateException("No grand-parent, so parent is root, but parent is red.");
			}
			
			final Node<T> uncle;
			final boolean isleftuncle; 
			if(grandparent.left != null && grandparent.left.equals(parent))
			{
				uncle = grandparent.right;
				isleftuncle = false;
			}
			else if(grandparent.right != null && grandparent.right.equals(parent))
			{
				uncle = grandparent.left;
				isleftuncle = true;
			}
			else
			{
				throw new IllegalStateException("Parent node is not a child of grand parent node.");
			}
			
			//Correct based on case.
			if(uncle != null && uncle.color == Color.RED)
			{
				/*
				 * Case 1.
				 */
				parent.color = Color.BLACK;
				grandparent.color = Color.RED;
				uncle.color = Color.BLACK;

				currentnode = grandparent;
			}
			else
			{
				/*
				 * Cases 2 and 3.
				 */
				final boolean case2;
				if(currentnode.equals(parent.right) && !isleftuncle)
				{
					case2 = true;
					//Case 2-R.
					rotateLeft(parent);
				}
				else if(currentnode.equals(parent.left) && isleftuncle)
				{
					case2 = true;
					//Case 2-L
					rotateRight(parent);
				}
				else case2 = false;
				if(case2)
				{
					//Account for the left rotation that just happened by updating parent and current node.
					final Node<T> tempnode = parent;
					parent = currentnode;
					currentnode = tempnode;
				}
				
				//Case 3
				if(!isleftuncle) rotateRight(grandparent);
				else if(isleftuncle) rotateLeft(grandparent);
				else throw new IllegalStateException("Neither a left or right uncle.");
				
				parent.color = Color.BLACK;
				grandparent.color = Color.RED;
			}
		}
		
		//Fix the root color.
		fRoot.color = Color.BLACK;
	}

	@Override
	public boolean remove(T entry) {

		if(fRoot == null)
		{
			return false;
		}
		else
		{
			final Node<T> removedNode = Iterables.getLast(getInsertionPath(entry));
			if(removedNode.content.compareTo(entry) != 0)
			{
				return false;
			}
			else
			{
				//Find the node to splice out.
				final Node<T> splicedNode;
				if(removedNode.left == null || removedNode.right == null)
				{
					splicedNode = removedNode;
				}
				else
				{
					splicedNode = successor(removedNode,null);
				}
				if(splicedNode == null) throw new IllegalStateException("Node to splice is null.");
				final Color splicedNodeColor = splicedNode.color;
				
				//Perform the splicing.
				final Node<T> nextNode = splice(splicedNode);
				Node<T> nextNodeParent = splicedNode.parent;
				
				//Swap the spliced node with the node to remove if needs be.
				if(!splicedNode.equals(removedNode))
				{
					if(nextNodeParent.equals(removedNode)) nextNodeParent = splicedNode;
						
					swap(removedNode,splicedNode);
				}
				
				//Correct the balancing.
				if(splicedNodeColor == Color.BLACK && fRoot != null)
				{
					postRemoveBalancing(nextNode,nextNodeParent);
				}
				
				return true;
			}
		}
	}

	private void postRemoveBalancing(final Node<T> node, final Node<T> nodeparent)
	{
		if(node == null && nodeparent == null)
		{
			throw new IllegalStateException("No post-remove balancing to do if node and its parent are null.");
		}
		
		Node<T> currentNode = node != null ? node : new Node<T>(null,Color.BLACK,nodeparent,null,null);
		
		while(!currentNode.equals(fRoot) && currentNode.color == Color.BLACK)
		{
			//Parent cannot be null because currentNode is not root.
			if(currentNode.parent == null) throw new IllegalStateException("Current node's parent is null but its not the root.");
			
			Node<T> sibling;
			final boolean leftsibling;
			if((currentNode.parent.left != null && currentNode.parent.left.equals(currentNode)) || (currentNode.parent.left == null && node == null)) 
			{
				sibling = currentNode.parent.right;
				leftsibling = false;
			}
			else if((currentNode.parent.right != null && currentNode.parent.right.equals(currentNode)) || (currentNode.parent.right == null && node == null)) 
			{
				sibling = currentNode.parent.left;
				leftsibling = true;
			}
			else throw new IllegalStateException("Current node is not a child of its parent.");
			
			//Should not be null according to the red-black property of the tree and current coloring.
			if(sibling == null) throw new IllegalStateException("Null sibling.");
			
			/*
			 * Case 1
			 */
			if(sibling.color == Color.RED)
			{
				sibling.color = Color.BLACK;
				currentNode.parent.color = Color.RED;
				
				if(!leftsibling)
				{
					rotateLeft(currentNode.parent);
					sibling = currentNode.parent.right;
				}
				else
				{
					rotateRight(currentNode.parent);
					sibling = currentNode.parent.left;
				}
			}
			
			if(sibling == null) throw new IllegalStateException("Null sibling.");
			
			/*
			 * Case 2
			 */
			if((sibling.left == null || sibling.left.color == Color.BLACK) && (sibling.right == null || sibling.right.color == Color.BLACK))
			{
				sibling.color = Color.RED;
				currentNode = currentNode.parent;
			}
			/*
			 * Case 3 and 4.
			 */
			else
			{
				//Case 3
				if((sibling.right == null || sibling.right.color == Color.BLACK) && !leftsibling)
				{
					sibling.left.color = Color.BLACK;
					sibling.color = Color.RED;
					rotateRight(sibling);
					sibling = currentNode.parent.right;
				}
				else if((sibling.left == null || sibling.left.color == Color.BLACK) && leftsibling)
				{
					sibling.right.color = Color.BLACK;
					sibling.color = Color.RED;
					rotateLeft(sibling);
					sibling = currentNode.parent.left;
				}
				
				//Case 4
				sibling.color = currentNode.parent.color;
				currentNode.parent.color = Color.BLACK;
				if(!leftsibling)
				{
					if(sibling.right != null) sibling.right.color = Color.BLACK;
					rotateLeft(currentNode.parent);
				}
				else
				{
					if(sibling.left != null) sibling.left.color = Color.BLACK;
					rotateRight(currentNode.parent);
				}
				currentNode = fRoot;
			}
		}
		currentNode.color = Color.BLACK;
	}
	
	@Override
	public boolean contains(T entry) {
		if(fRoot == null)
		{
			return false;
		}
		else
		{
			final Node<T> closestNode = Iterables.getLast(getInsertionPath(entry));
			return (closestNode.content.compareTo(entry) == 0);
		}
	}

	@Override
	public int size() {
		if(fRoot == null)
		{
			return 0;
		}
		else
		{
			return fRoot.getSubtreeSize();
		}
	}
	
	/**
	 * Locally re-arrange nodes around the given node in a red-black tree left rotation.
	 * @param node - rotation center.
	 */
	private void rotateLeft(@NonNull final Node<T> node)
	{
		final Node<T> xparent = node.parent;
		final Node<T> ynode = node.right;
		if(ynode == null)
		{
			throw new IllegalArgumentException("Tried to do a left rotation on a node with no right child.");
		}
		final Node<T> yleft = ynode.left;
		
		/*
		 * Update pointers.
		 */
		//Update xparent.
		if(xparent != null)
		{
			if(xparent.left != null && xparent.left.equals(node))
			{
				xparent.left = ynode;
			}
			else if(xparent.right != null && xparent.right.equals(node))
			{
				xparent.right = ynode;
			}
			else
			{
				throw new IllegalStateException("Given node is not a child of its parent.");
			}
		}
		else
		{
			fRoot = ynode;
		}
		//Update y.
		ynode.parent = xparent;
		ynode.left = node;
		
		//Update x.
		node.parent = ynode;
		node.right = yleft;
		//Update xleft.
		//Update yleft.
		if(yleft != null)
		{
			yleft.parent = node;
		}
		//Update yright.
		
		/*
		 * Update subtree sizes.
		 */
		//Update x.
		node.rightSize = yleft == null ? 0 : yleft.getSubtreeSize();
		//Update y.
		ynode.leftSize = node.getSubtreeSize();
	}
	
	/**
	 * Locally re-arrange nodes around the given node in a red-black tree right rotation.
	 * @param node - rotation center.
	 */
	private void rotateRight(@NonNull final Node<T> node)
	{
		final Node<T> yparent = node.parent;
		final Node<T> xnode = node.left;
		if(xnode == null)
		{
			throw new IllegalArgumentException("Tried to do a right rotation on a node with no left child.");
		}
		final Node<T> xright = xnode.right;
		
		/*
		 * Update pointers.
		 */
		//Update parent.
		if(yparent != null)
		{
			if(yparent.left != null && yparent.left.equals(node))
			{
				yparent.left = xnode;
			}
			else if(yparent.right != null & yparent.right.equals(node))
			{
				yparent.right = xnode;
			}
			else
			{
				throw new IllegalStateException("Given node is not a child of its parent.");
			}
		}
		else
		{
			fRoot = xnode;
		}
		//Update y.
		node.parent = xnode;
		node.left = xright;
		//Update x.
		xnode.parent = yparent;
		xnode.right = node;
		//Update xleft.
		//Update xright.
		if(xright != null)
		{
			xright.parent = node;
		}
		//Update yright.

		/*
		 * Update subtree sizes.
		 */
		node.leftSize = xright == null ? 0 : xright.getSubtreeSize();
		xnode.rightSize = node.getSubtreeSize();
	}
	
	/**
	 * Splice the given node out of the tree.
	 * @param node - the node to splice out of the tree.
	 * @return the next node, child of the spliced node, that effectively replaces the spliced node in the tree. 
	 */
	private Node<T> splice(@NonNull Node<T> node)
	{
		if(node.left != null && node.right != null) throw new IllegalStateException("Node with two non-null children is unspliceable.");
		
		//Get the correct parent and next node from the spliced node.
		final Node<T> parentNode = node.parent;
		final Node<T> nextNode;
		if(node.left != null)
		{
			nextNode = node.left;
		}
		else
		{
			nextNode = node.right;
		}
		
		//Update the next node's parent.
		if(nextNode != null)
		{
			nextNode.parent = parentNode;
		}
		
		//Update the parent node.
		if(parentNode == null)
		{
			fRoot = nextNode;
		}
		else if(parentNode.left != null && parentNode.left.equals(node))
		{
			parentNode.left = nextNode;
		}
		else if(parentNode.right != null && parentNode.right.equals(node))
		{
			parentNode.right = nextNode;
		}
		else
		{
			throw new IllegalStateException("Splice node is not the child of its parent node.");
		}
		//Correct the subtree sizes.
		updateSubtreeSizes(parentNode);
		
		return nextNode;
	}
	
	/**
	 * Swap a node for another one in the tree.
	 * @param present - a node that is currently in the tree, the destination.
	 * @param absent - a node that is not in the tree, the source.
	 */
	private void swap(Node<T> present, Node<T> absent)
	{
		//Fix current node.
		absent.parent = present.parent;
		absent.left = present.left;
		absent.leftSize = present.leftSize;
		absent.right = present.right;
		absent.rightSize = present.rightSize;
		absent.color = present.color;
		
		//Fix the parent of the current node.
		if(present.parent != null)
		{
			if(present.parent.left != null && present.parent.left.equals(present))
			{
				present.parent.left = absent;
			}
			else if(present.parent.right != null && present.parent.right.equals(present))
			{
				present.parent.right = absent;
			}
			else
			{
				throw new IllegalStateException("Removed node is not a child of its parent node.");
			}
		}
		
		//Fix the children of the current node.
		if(present.left != null)
		{
			present.left.parent = absent;
		}
		if(present.right != null)
		{
			present.right.parent = absent;
		}
		
		//Swap the root.
		if(fRoot.equals(present))
		{
			fRoot = absent;
		}
	}
	
	/**
	 * Update the subtree sizes starting at a given node, following the parents up to the root.
	 * @param node
	 */
	private void updateSubtreeSizes(Node<T> node)
	{
		Node<T> currentnode = node;
		while(currentnode != null)
		{
			currentnode.leftSize = currentnode.left == null ? 0 : currentnode.left.getSubtreeSize();
			currentnode.rightSize = currentnode.right == null ? 0 : currentnode.right.getSubtreeSize();
			currentnode = currentnode.parent;
		}
	}
	
	/**
	 * @param node - a tree node.
	 * @param root - a subtree root node we must not go beyond when climbing up the tree. Set to null if not needed.
	 * @return the (in-order) successor of that node in the tree, null if none exists.
	 */
	private Node<T> successor(Node<T> node, Node<T> root)
	{
		Node<T> currentnode = node;
		if(currentnode.right != null)
		{
			currentnode = currentnode.right;
			while(currentnode.left != null)
			{
				currentnode = currentnode.left;
			}
			return currentnode;
		}
		else
		{
			Node<T> parent = currentnode != root ? currentnode.parent : null;
			while(parent != null)
			{
				if(parent.left != null && parent.left.equals(currentnode))
				{
					return parent;
				}
				currentnode = parent;
				parent = currentnode != root ? currentnode.parent : null;
			}
			return null;
		}
	}
	
	/**
	 * @param root - a tree node.
	 * @return an iterable of the in-order traversal of the subtree rooted at the given node.
	 */
	private Iterable<Node<T>> getInorderTraversal(final Node<T> root)
	{
		return new Iterable<Node<T>>()
				{
					@Override
					public Iterator<Node<T>> iterator() {
						
						Node<T> node = root;
						
						while(node.left != null)
						{
							node = node.left;
						}
						final Node<T> startingnode = node;
						
						return new Iterator<Node<T>>(){
							
							Node<T> currentnode = startingnode;
							
							@Override
							public boolean hasNext() {
								return currentnode != null;
							}

							@Override
							public Node<T> next() {
								Node<T> oldcurrentnode = currentnode;
								currentnode = successor(oldcurrentnode,root);
								return oldcurrentnode;
							}
							
						};
					}
			
				};
	}
	
	/**
	 * @param entry - node key value.
	 * @return an iterable over the insertion path from the root to the node containing the given key value or its insertion parent.
	 */
	private Iterable<Node<T>> getInsertionPath(T entry)
	{
		return new Iterable<Node<T>>()
				{
					@Override
					public Iterator<Node<T>> iterator() {
						return new Iterator<Node<T>>() {
							
							private Node<T> nextnode = fRoot;
							
							@Override
							public boolean hasNext() {
								return nextnode != null;
							}
							
							@Override
							public Node<T> next() {
								if(!hasNext()) throw new NoSuchElementException("No next node on the insertion path.");
								
								final Node<T> oldnextnode = nextnode;
								if(nextnode.content.compareTo(entry) < 0)
								{
									nextnode = nextnode.right;
								}
								else if(nextnode.content.compareTo(entry) == 0)
								{
									nextnode = null;
								}
								else
								{
									nextnode = nextnode.left;
								}
								return oldnextnode;
							}
						};
					}			
				};
	}
	
	/*
	 * Checker methods to verify the soudness of the data structure. 
	 */
	
	/**
	 * Check that the parent and child pointer structure is coherent in the tree.
	 * 
	 * Throws an exception if an invalid pointer structure is found.
	 */
	public void checkCoherence() throws IllegalStateException
	{
		checkCoherence(fRoot);
	}
	private void checkCoherence(Node<T> root)
	{
		if(root!= null)
		{
			if(root.parent != null)
			{
				final boolean leftChild;
				final boolean rightChild;
				
				if(root.parent.left != null && root.parent.left == root)
				{
					leftChild = true;
				}
				else
				{
					leftChild = false;
				}
				
				if(root.parent.right != null && root.parent.right == root)
				{
					rightChild = true;
				}
				else
				{
					rightChild = false;
				}
				
				final boolean achild = leftChild ^ rightChild;
				
				if(!achild)
				{
					throw new IllegalStateException("Node "+root+" is not a single child of its parent.");
				}
			}
			
			if(root.left != null)
			{
				if(!root.left.parent.equals(root))
				{
					throw new IllegalStateException("Node "+root+" is not the parent of its left child.");
				}
			}
			
			if(root.right != null)
			{
				if(!root.right.parent.equals(root))
				{
					throw new IllegalStateException("Node "+root+" is not the parent of its right child.");
				}
			}
			
			checkCoherence(root.left);
			checkCoherence(root.right);
		}
	}
	
	/**
	 * Check if the subtree sizes recurrence relation is satisfied.
	 * 
	 * Throws an exception if an incoherent subtree size is found.
	 */
	public void checkSizes() throws IllegalStateException
	{
		checkSizes(fRoot);
	}
	private void checkSizes(Node<T> root)
	{
		if(root!= null)
		{
			if(root.right != null)
			{
				if(root.right.getSubtreeSize() != root.rightSize)
				{
					throw new IllegalStateException("Node "+root+"'s right size is not equal to its right child's subtree size.");
				}
			}
			else
			{
				if(root.rightSize != 0)
				{
					throw new IllegalStateException("Node "+root+" has no right child, but a non-zero right size.");
				}
			}
			
			if(root.left != null)
			{
				if(root.left.getSubtreeSize() != root.leftSize)
				{
					throw new IllegalStateException("Node "+root+"'s left size is not equal to its left child's subtree size.");
				}
			}
			else
			{
				if(root.leftSize != 0)
				{
					throw new IllegalStateException("Node "+root+" has no left child, but a non-zero left size.");
				}
			}
			
			checkSizes(root.left);
			checkSizes(root.right);
		}
	}
	
	/**
	 * Check that the tree satisfies the red-black properties:
	 * 	1. Every node is either red or black.
	 * 	2. The root is black.
	 * 	3. The null leaf nodes are black.
	 * 	4. If a node is red, then both its children are black.
	 * 	5. For each node, all paths from the node to descendant leaves contain the same number of black nodes.
	 * 
	 * And exception is thrown if a property is violated.
	 */
	public void checkRedBlack() throws IllegalStateException
	{
		if(fRoot != null && fRoot.color != Color.BLACK)
		{
			throw new IllegalStateException("Red-black tree root is not black.");
		}
		checkRedBlack(fRoot);
	}
	private int checkRedBlack(Node<T> root)
	{
		if(root == null)
		{
			return 1;
		}
		
		if(root.color == Color.RED)
		{
			if(root.left != null && root.left.color == Color.RED)
			{
				throw new IllegalStateException("Node is red but left child is also red.");
			}
			else if(root.right != null && root.right.color == Color.RED)
			{
				throw new IllegalStateException("Node is red but right child is also red.");
			}
		}
		
		final int leftBlackHeight = checkRedBlack(root.left);
		final int rightBlackHeight = checkRedBlack(root.right);
		
		if(leftBlackHeight != rightBlackHeight)
		{
			throw new IllegalStateException("Left and right black heights are not equal.");
		}
		return leftBlackHeight + (root.color == Color.BLACK ? 1 : 0);
		
		
	}
}
