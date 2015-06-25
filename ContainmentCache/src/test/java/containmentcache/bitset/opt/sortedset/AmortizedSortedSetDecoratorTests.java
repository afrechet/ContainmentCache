package containmentcache.bitset.opt.sortedset;

import java.util.TreeSet;

public class AmortizedSortedSetDecoratorTests extends ASortedSetTests{

	@Override
	protected <E extends Comparable<E>> ISortedSet<E> getSortedSet() {
		return new AmortizedSortedSetDecorator<E>(new TreeSet<>(),100);
	}

}
