package containmentcache.util;

import java.util.*;

import com.google.common.collect.BiMap;
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

    public static <E> List<BiMap<E, Integer>> makeNPermutations(BiMap<E, Integer> canonicalPermutation, long seed, int nPermutations) {
        final List<E> universelist = new ArrayList<>(canonicalPermutation.keySet());
        final List<BiMap<E, Integer>> permutations = new ArrayList<>();
        final Random random = new Random(seed);
        for (int o = 0; o < nPermutations - 1; o++) {
        	final ImmutableBiMap.Builder<E, Integer> builder = ImmutableBiMap.builder();
        	Collections.shuffle(universelist, random);
            int index = 0;
            for (E element : universelist) {
            	builder.put(element, index++);
            }
            permutations.add(builder.build());
        }
        return permutations;
    }
}
