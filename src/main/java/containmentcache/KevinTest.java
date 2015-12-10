package containmentcache;

import ca.ubc.cs.beta.aeatk.misc.jcommander.JCommanderHelper;
import ca.ubc.cs.beta.aeatk.misc.options.UsageTextField;
import ca.ubc.cs.beta.aeatk.options.AbstractOptions;
import com.beust.jcommander.Parameter;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;
import com.google.common.math.DoubleMath;
import containmentcache.bitset.opt.MultiPermutationBitSetCache;
import containmentcache.bitset.opt.sortedset.redblacktree.RedBlackTree;
import containmentcache.util.PermutationUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by newmanne on 2015-12-09.
 */
@Slf4j
public class KevinTest {

    public enum KevinTestType {
        REGULAR, SHAPLEY
    }

    @UsageTextField(title="KevinTest",description=" ")
    public static class KevinTestArgs extends AbstractOptions {

        @Parameter(names = "-type")
        public KevinTestType type = KevinTestType.REGULAR;

    }

    public static void main(String[] args) throws IOException {
        final KevinTestArgs kevinArgs = new KevinTestArgs();
        JCommanderHelper.parseCheckingForHelpAndVersion(args, kevinArgs);


        // 1: read files from csv into ds
        final String csvFile = "/ubc/cs/research/arrow/satfc/satfc-scripts/analysis/problems.txt";
        final List<Set<Integer>> list = new ArrayList<>();
        log.info("Parsing file {}", csvFile);
        for (final String line : Files.readLines(new File(csvFile), Charset.defaultCharset())) {
            final Set<Integer> elements = Splitter.on(',').splitToList(line).stream().map(Integer::parseInt).collect(Collectors.toSet());
            list.add(elements);
        }
        log.info("Done parsing file, {} entries", list.size());

        // 2: do the thing
        final Random random = new Random();
        final DS ds = new DS(IntStream.rangeClosed(1, 2173).boxed().collect(Collectors.toSet()));
        log.info("Starting algorithm");
        while(!ds.isConverged()) {
            // sample
            final Set<Integer> sample = list.get(random.nextInt(list.size()));
            switch (kevinArgs.type) {
                case REGULAR:
                    ds.checkSample(sample);
                    break;
                case SHAPLEY:
                    ds.checkSampleShapley(sample);
                    break;
                default:
                    throw new IllegalStateException("AHHH");
            }
        }
        ds.done();
    }

    public static class DS {

        private final IContainmentCache<Integer, ICacheEntry<Integer>> c;
        private final ImmutableBiMap<Integer, Integer> permutation;
        private final Map<BitSet, Double> counters;
        private long iterCount;
        private double previousEntropy;

        public DS(Set<Integer> universe) {
            permutation = PermutationUtils.makePermutation(universe);
            List<BiMap<Integer, Integer>> permutations = PermutationUtils.makeNPermutations(permutation, 1, 3);
            c = new MultiPermutationBitSetCache<>(permutation, permutations, RedBlackTree::new);
            counters = new HashMap<>();
        }

        public void checkSample(Set<Integer> sample) {
            // exact match
            SimpleCacheSet<Integer> cs = new SimpleCacheSet<>(sample, permutation);
            if (c.contains(cs)) {
                counters.compute(cs.getBitSet(), (k, v) -> v + 1);
            } else if (Iterables.isEmpty(c.getSupersets(cs))) { // No superset
                c.add(cs);
                counters.put(cs.getBitSet(), 1.0);
            } else {
                // Nothing to do
            }
            endOfIter();
        }

        private void endOfIter() {
            iterCount++;
            if (iterCount % 2000 == 0) {
                double newEntropy = calcEntropy();
                log.info("Iter count is {} and entropy is {} and delta is {}", iterCount, newEntropy, Math.abs(newEntropy - previousEntropy));
                previousEntropy = newEntropy;
            }
        }

        public void checkSampleShapley(Set<Integer> sample) {
            SimpleCacheSet<Integer> cs = new SimpleCacheSet<>(sample, permutation);
            Iterable<ICacheEntry<Integer>> supersets = c.getSupersets(cs);
            int n = 0;
            final List<BitSet> sets = new ArrayList<>();
            for (ICacheEntry<Integer> superset : supersets) {
                n += 1;
                sets.add(superset.getBitSet());
            }
            if (n == 0) {
                c.add(cs);
            } else {
                for (BitSet b : sets) {
                    final int finalN = n;
                    counters.compute(b, (k, v) -> v == null ? (1.0/ finalN) : v + (1.0/ finalN));
                }
            }
            endOfIter();
        }

        public void done() {
            final double entropy = calcEntropy();
            log.info("Entropy :" + entropy);
        }

        private double calcEntropy() {
            final double total = counters.values().stream().mapToDouble(Double::doubleValue).sum();
            final Map<BitSet, Double> probabilities = counters.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue() / total));
            return probabilities.values().stream().mapToDouble(p -> p * DoubleMath.log2(1 / p)).sum();
        }

        public boolean isConverged() {
            return iterCount > 1000000;
        }

    }

}


