package containmentcache.bitset.opt.sortedset.redblacktree;


/**
 * A red-black tree node.
 * @author afrechet
 *
 * @param <T>
 */
class Node<T extends Comparable<T>> {
	
	enum Color
	{
		RED,BLACK;
	}
	
	public Color color;
	public T content;
	
	public Node<T> parent;
	
	public Node<T> left;
	public int leftSize;
	
	public Node<T> right;
	public int rightSize;
	
	public Node(T content, Color color, Node<T> parent, Node<T> left, Node<T> right)
	{
		this.content = content;
		this.color = color;
		this.parent = parent;
		this.left = left;
		this.leftSize = left == null ? 0 : left.getSubtreeSize();
		this.right = right;
		this.rightSize = right == null ? 0 : right.getSubtreeSize();
	}
	
	public int getSubtreeSize()
	{
		return leftSize + 1 + rightSize;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		Node other = (Node) obj;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		return true;
	}
	
	@Override
	public String toString()
	{
		return content.toString();
	}
	
	
	
}