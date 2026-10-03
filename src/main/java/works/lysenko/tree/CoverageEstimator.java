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
import works.lysenko.util.spec.PropEnum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
     * Estimates average cycles required to visit all accessible leafs the required amount of times.
     *
     * @param rootCtrl root controller
     * @return estimated average number of cycles
     */
    public static int estimateAverageCycles(final _Ctrl rootCtrl) {

        final int target = (isNotNull(works.lysenko.Base.parameters))
                ? works.lysenko.Base.parameters.getAllLeafsCount()
                : (isNotNull(PropEnum._ALL_LEAFS_COUNT.get()) ? Math.max(1, PropEnum._ALL_LEAFS_COUNT.get()) : 1);
        return estimateAverageCycles(rootCtrl, target);
    }

    /**
     * Estimates average cycles required to visit all accessible leafs at least target times.
     *
     * @param rootCtrl root controller
     * @param target required executions per leaf
     * @return estimated average number of cycles
     */
    public static int estimateAverageCycles(final _Ctrl rootCtrl, final int target) {

        if (isNull(rootCtrl)) return 0;
        final Set<_Scenario> accessibleLeafs = rootCtrl.getAccessibleLeafs();
        if (accessibleLeafs.isEmpty()) return 0;
        final int targetExecutions = Math.max(1, target);
        if (1 == accessibleLeafs.size()) return targetExecutions;

        final double completionBoost = isNotNull(Scenario.completionWeight)
                ? Scenario.completionWeight.doubleValue() : 0.0;
        final Random random = new Random(42);

        long totalCycles = 0;
        int successfulTrials = 0;

        for (int trial = 0; trial < TRIALS; trial++) {
            final Map<_Scenario, Integer> executions = new HashMap<>();
            int coveredLeafsCount = 0;
            int cycles = 0;

            while (coveredLeafsCount < accessibleLeafs.size() && cycles < MAX_CYCLES_PER_TRIAL) {
                cycles++;
                final List<_Scenario> path = new ArrayList<>();
                final _Scenario selectedLeaf = simulateCycle(rootCtrl.getPool(), executions, targetExecutions, completionBoost, random, path);
                if (isNull(selectedLeaf)) break;

                for (final _Scenario p : path) {
                    executions.put(p, executions.getOrDefault(p, 0) + 1);
                }
                final int leafExecs = executions.getOrDefault(selectedLeaf, 0) + 1;
                executions.put(selectedLeaf, leafExecs);

                if (leafExecs == targetExecutions && accessibleLeafs.contains(selectedLeaf)) {
                    coveredLeafsCount++;
                }
            }

            if (coveredLeafsCount == accessibleLeafs.size()) {
                totalCycles += cycles;
                successfulTrials++;
            }
        }

        if (0 == successfulTrials) {
            return accessibleLeafs.size() * targetExecutions;
        }

        return (int) Math.round((double) totalCycles / successfulTrials);
    }

    private static _Scenario simulateCycle(
            final _Pool pool,
            final Map<_Scenario, Integer> executions,
            final int target,
            final double completionBoost,
            final Random random,
            final List<_Scenario> path) {

        final _Scenario chosen = selectCandidate(pool, executions, target, completionBoost, random);
        if (isNull(chosen)) return null;

        if (chosen instanceof _Node node) {
            path.add(chosen);
            final _Pool childPool = node.getPool();
            if (isNotNull(childPool)) {
                return simulateCycle(childPool, executions, target, completionBoost, random, path);
            }
            return chosen;
        }

        return chosen;
    }

    private static _Scenario selectCandidate(
            final _Pool pool,
            final Map<_Scenario, Integer> executions,
            final int target,
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
            if (scenario instanceof Mono && executions.getOrDefault(scenario, 0) > 0) continue;

            double weight = isNotNull(pair.v()) ? pair.v().doubleValue() : 0.0;
            if (Include.upstream) {
                final Fraction up = scenario.weightUpstream();
                if (isNotNull(up)) weight += up.doubleValue();
            }
            if (Include.downstream) {
                final Fraction down = downstreamWeight(scenario);
                if (isNotNull(down)) weight += down.doubleValue();
            }
            if (weight > 0.0 && completionBoost > 0.0) {
                final double ratio = getUncompletedRatio(scenario, executions, target);
                if (ratio > 0.0) {
                    weight += completionBoost * ratio;
                }
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

    static double getUncompletedRatio(
            final _Scenario scenario,
            final Map<_Scenario, Integer> executions,
            final int target) {

        final int execs = executions.getOrDefault(scenario, 0);
        double ratio = (execs < target) ? ((double) (target - execs) / target) : 0.0;
        if (scenario instanceof _Node node) {
            final double descRatio = getMaxDescendantRatio(node, executions, target, new HashSet<>());
            ratio = Math.max(ratio, descRatio);
        }
        return ratio;
    }

    private static double getMaxDescendantRatio(
            final _Node node,
            final Map<_Scenario, Integer> executions,
            final int target,
            final Set<Object> visited) {

        if (isNull(node) || isNull(node.getPool()) || !visited.add(node)) return 0.0;
        final List<KeyValue<_Scenario, Fraction>> pairs = node.getPool().getPairList();
        if (isNull(pairs)) return 0.0;

        double max = 0.0;
        for (final KeyValue<_Scenario, Fraction> pair : pairs) {
            final _Scenario child = pair.k();
            if (isNotNull(child) && child.isExecutable() && child.calculateCombinations(true) > 0) {
                final int execs = executions.getOrDefault(child, 0);
                double childRatio = (execs < target) ? ((double) (target - execs) / target) : 0.0;
                if (child instanceof _Node childNode) {
                    final double descRatio = getMaxDescendantRatio(childNode, executions, target, visited);
                    childRatio = Math.max(childRatio, descRatio);
                }
                if (childRatio > max) {
                    max = childRatio;
                    if (max >= 1.0) return 1.0;
                }
            }
        }
        return max;
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
