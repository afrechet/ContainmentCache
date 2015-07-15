package containmentcache.util;

import java.util.Set;

import com.google.common.collect.ImmutableBiMap;

public class PermutationUtils {
	
	public static <E> ImmutableBiMap<E, Integer> makePermutation(Set<E> elements) {
        // Create the canonical permutation for this bundle
        final ImmutableBiMap.Builder<E, Integer> builder = ImmutableBiMap.builder();
        int index = 0;
        for (E station : elements) {
            builder.put(station, index++);
        }
        return builder.build();
	}

}
