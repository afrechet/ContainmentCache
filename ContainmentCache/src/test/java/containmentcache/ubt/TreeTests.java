package containmentcache.ubt;

import containmentcache.AContainmentCacheTests;

public class TreeTests extends AContainmentCacheTests{

	@Override
	protected Tree<Integer> getCache() {
		return new Tree<Integer>();
	}

}
