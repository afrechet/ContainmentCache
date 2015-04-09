package containmentcache.bitset;

import java.util.Set;

import containmentcache.AContainmentCacheTests;
import containmentcache.IContainmentCache;

public class SimpleBitSetCacheTests extends AContainmentCacheTests {

	@Override
	protected IContainmentCache<Integer> getCache(Set<Integer> universe) {
		return new SimpleBitSetCache<Integer>(universe);
	}

}
