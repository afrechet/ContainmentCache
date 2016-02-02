package containmentcache.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public class PermutationUtils {
	
	public static <E extends Comparable<E>> ImmutableBiMap<E, Integer> makePermutation(Set<E> elements) {
        // Create the canonical permutation for this bundle
        final ImmutableBiMap.Builder<E, Integer> builder = ImmutableBiMap.builder();
        int index = 0;
        final List<E> sorted = elements.stream().sorted().collect(Collectors.toList());
        for (E station : sorted) {
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
