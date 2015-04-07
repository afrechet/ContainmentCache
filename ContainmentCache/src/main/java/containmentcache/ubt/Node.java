package containmentcache.ubt;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class Node<E> {
	
	private boolean fEOP;
	private final E fElement;
	private final Map<E,Node<E>> fChildren;
	
	/**
	 * Basic tree node.
	 * @param element - element contained at the node.
	 */
	public Node(E element)
	{
		this.fEOP = false;
		this.fElement = element;
		fChildren = new HashMap<E,Node<E>>();
	}
	
	public boolean removeChild(Node<E> child)
	{
		return (fChildren.remove(child.getElement())!=null);
	}
	
	public boolean addChild(Node<E> child)
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
	
	public Node<E> getChild(E element)
	{
		return fChildren.get(element);
	}
	
	public Set<Entry<E,Node<E>>> getChildren()
	{
		return Collections.unmodifiableSet(fChildren.entrySet());
	}
	
	@Override
	public String toString()
	{
		return fElement+" ("+fEOP+") "+fChildren.keySet();
	}
	
}
