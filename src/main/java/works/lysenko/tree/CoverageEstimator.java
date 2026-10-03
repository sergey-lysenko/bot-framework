package works.lysenko.tree;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.tree.base.Mono;
import works.lysenko.util.apis.scenario._Ctrl;
import works.lysenko.util.apis.scenario._Node;
import works.lysenko.util.apis.scenario._Pool;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.prop.tree.Include;
import works.lysenko.util.prop.tree.Scenario;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static java.util.Objects.isNull;
import static works.lysenko.util.func.core.Weights.downstreamWeight;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Estimates the average number of execution cycles required to achieve 100% leaf coverage
 * using Monte Carlo simulation of the scenario selection algorithm.
 */
public record CoverageEstimator() {

    private static final int TRIALS = 200;
    private static final int MAX_CYCLES_PER_TRIAL = 10000;

    /**
     * Estimates average cycles required to visit all accessible leafs at least once.
     *
     * @param rootCtrl root controller
     * @return estimated average number of cycles
     */
    public static int estimateAverageCycles(final _Ctrl rootCtrl) {

        if (isNull(rootCtrl)) return 0;
        final Set<_Scenario> accessibleLeafs = rootCtrl.getAccessibleLeafs();
        if (accessibleLeafs.isEmpty()) return 0;
        if (1 == accessibleLeafs.size()) return 1;

        final double completionBoost = isNotNull(Scenario.completionWeight)
                ? Scenario.completionWeight.doubleValue() : 0.0;
        final Random random = new Random(42);

        long totalCycles = 0;
        int successfulTrials = 0;

        for (int trial = 0; trial < TRIALS; trial++) {
            final Set<_Scenario> executedInTrial = new HashSet<>();
            final Set<_Scenario> coveredLeafs = new HashSet<>();
            int cycles = 0;

            while (coveredLeafs.size() < accessibleLeafs.size() && cycles < MAX_CYCLES_PER_TRIAL) {
                cycles++;
                final List<_Scenario> path = new ArrayList<>();
                final _Scenario selectedLeaf = simulateCycle(rootCtrl.getPool(), executedInTrial, completionBoost, random, path);
                if (isNull(selectedLeaf)) break;

                executedInTrial.addAll(path);
                executedInTrial.add(selectedLeaf);
                if (accessibleLeafs.contains(selectedLeaf)) {
                    coveredLeafs.add(selectedLeaf);
                }
            }

            if (coveredLeafs.size() == accessibleLeafs.size()) {
                totalCycles += cycles;
                successfulTrials++;
            }
        }

        if (0 == successfulTrials) {
            return accessibleLeafs.size();
        }

        return (int) Math.round((double) totalCycles / successfulTrials);
    }

    private static _Scenario simulateCycle(
            final _Pool pool,
            final Set<_Scenario> executedInTrial,
            final double completionBoost,
            final Random random,
            final List<_Scenario> path) {

        final _Scenario chosen = selectCandidate(pool, executedInTrial, completionBoost, random);
        if (isNull(chosen)) return null;

        if (chosen instanceof _Node node) {
            path.add(chosen);
            final _Pool childPool = node.getPool();
            if (isNotNull(childPool)) {
                return simulateCycle(childPool, executedInTrial, completionBoost, random, path);
            }
            return chosen;
        }

        return chosen;
    }

    private static _Scenario selectCandidate(
            final _Pool pool,
            final Set<_Scenario> executedInTrial,
            final double completionBoost,
            final Random random) {

        if (isNull(pool)) return null;
        final List<KeyValue<_Scenario, Fraction>> pairs = pool.getPairList();
        if (isNull(pairs) || pairs.isEmpty()) return null;

        final List<_Scenario> candidates = new ArrayList<>();
        final List<Double> weights = new ArrayList<>();
        double totalWeight = 0.0;

        for (final KeyValue<_Scenario, Fraction> pair : pairs) {
            final _Scenario scenario = pair.k();
            if (isNull(scenario)) continue;
            if (!scenario.isExecutable() || scenario.calculateCombinations(true) <= 0) continue;
            if (scenario instanceof Mono && executedInTrial.contains(scenario)) continue;

            double weight = isNotNull(pair.v()) ? pair.v().doubleValue() : 0.0;
            if (Include.upstream) {
                final Fraction up = scenario.weightUpstream();
                if (isNotNull(up)) weight += up.doubleValue();
            }
            if (Include.downstream) {
                final Fraction down = downstreamWeight(scenario);
                if (isNotNull(down)) weight += down.doubleValue();
            }
            if (weight > 0.0 && completionBoost > 0.0 && hasUnexecutedState(scenario, executedInTrial)) {
                weight += completionBoost;
            }

            if (weight > 0.0) {
                candidates.add(scenario);
                weights.add(weight);
                totalWeight += weight;
            }
        }

        if (candidates.isEmpty() || totalWeight <= 0.0) return null;

        final double r = random.nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (int i = 0; i < candidates.size(); i++) {
            cumulative += weights.get(i);
            if (r <= cumulative) {
                return candidates.get(i);
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    static boolean hasUnexecutedState(final _Scenario scenario, final Set<_Scenario> executedInTrial) {

        if (!executedInTrial.contains(scenario)) return true;
        if (scenario instanceof _Node node) {
            return hasUnexecutedDescendants(node, executedInTrial, new HashSet<>());
        }
        return false;
    }

    private static boolean hasUnexecutedDescendants(
            final _Node node,
            final Set<_Scenario> executedInTrial,
            final Set<Object> visited) {

        if (isNull(node) || isNull(node.getPool()) || !visited.add(node)) return false;
        final List<KeyValue<_Scenario, Fraction>> pairs = node.getPool().getPairList();
        if (isNull(pairs)) return false;

        for (final KeyValue<_Scenario, Fraction> pair : pairs) {
            final _Scenario child = pair.k();
            if (isNotNull(child) && child.isExecutable() && child.calculateCombinations(true) > 0) {
                if (!executedInTrial.contains(child)) return true;
                if (child instanceof _Node childNode) {
                    if (hasUnexecutedDescendants(childNode, executedInTrial, visited)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
