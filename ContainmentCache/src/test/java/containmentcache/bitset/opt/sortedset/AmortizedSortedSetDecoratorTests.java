package containmentcache.bitset.opt.sortedset;

import java.util.Comparator;
import java.util.TreeSet;

public class AmortizedSortedSetDecoratorTests extends ASortedSetTests{

	@Override
	protected ISortedSet<Integer> getSortedSet(Comparator<Integer> comparator) {
		return new AmortizedSortedSetDecorator<>(new TreeSet<>(comparator));
	}

}
