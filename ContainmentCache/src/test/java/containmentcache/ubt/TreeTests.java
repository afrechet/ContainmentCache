package containmentcache.ubt;

import containmentcache.AContainmentCacheTests;

public class TreeTests extends AContainmentCacheTests{

	@Override
	protected UBTree<Integer> getCache() {
		return new UBTree<Integer>();
	}

}
