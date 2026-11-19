package works.lysenko.base.eta.coverage;

import works.lysenko.util.prop.tree.Scenario;

import java.util.Arrays;
import java.util.Random;
import java.util.function.IntConsumer;

import static java.util.Objects.isNull;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Executes zero-allocation Monte Carlo simulation trials against a {@link CompiledCoverageTree}.
 */
final class MonteCarloSimulator {

    /** Number of Monte Carlo simulation trials performed to compute the average. */
    private static final int TRIALS = 100;

    /** Maximum allowed cycles per simulation trial before timing out. */
    private static final int MAX_CYCLES_PER_TRIAL = 10000;

    private MonteCarloSimulator() {
    }

    static int runTrials(
            final CompiledCoverageTree tree,
            final int targetExecutions,
            final double completionBoost,
            final IntConsumer progressConsumer) {

        final Random random = new Random(42);
        final int totalScenarios = tree.totalScenarios();
        final int accessibleLeafsCount = tree.accessibleLeafs();
        final int maxCycles = Math.min(MAX_CYCLES_PER_TRIAL, Math.max(1000, accessibleLeafsCount * targetExecutions * 20));
        final int maxStallCycles = Math.max(300, accessibleLeafsCount * 15);

        final int[] executions = new int[totalScenarios];
        final int[] pathBuffer = new int[totalScenarios];
        final int[] pathLen = new int[1];
        final int[] candidatesBuffer = new int[tree.maxPoolCandidates()];
        final double[] weightsBuffer = new double[tree.maxPoolCandidates()];

        final int[] targets = tree.targets();
        final boolean[] isMono = tree.isMono();
        final boolean[] isNode = tree.isNode();
        final boolean[] isAccessibleLeaf = tree.isAccessibleLeaf();
        final SimPool simRootPool = tree.rootPool();

        long totalCycles = 0;
        int successfulTrials = 0;

        for (int trial = 0; trial < TRIALS; trial++) {
            Arrays.fill(executions, 0);
            int coveredLeafsCount = 0;
            int cycles = 0;
            int cyclesSinceProgress = 0;

            while (coveredLeafsCount < accessibleLeafsCount && cycles < maxCycles) {
                cycles++;
                pathLen[0] = 0;
                final int selectedLeafId = simulateCycle(simRootPool, executions, targetExecutions,
                        completionBoost, random, pathBuffer, pathLen, candidatesBuffer, weightsBuffer,
                        isMono, isNode, targets);
                if (selectedLeafId < 0) break;

                for (int p = 0; p < pathLen[0]; p++) {
                    executions[pathBuffer[p]]++;
                }
                executions[selectedLeafId]++;
                final int leafExecs = executions[selectedLeafId];

                if (isAccessibleLeaf[selectedLeafId] && leafExecs == targets[selectedLeafId]) {
                    coveredLeafsCount++;
                    cyclesSinceProgress = 0;
                } else {
                    cyclesSinceProgress++;
                }

                if (cyclesSinceProgress > maxStallCycles) {
                    break;
                }
            }

            if (coveredLeafsCount == accessibleLeafsCount) {
                totalCycles += cycles;
                successfulTrials++;
            }

            if (isNotNull(progressConsumer)) {
                final int percent = (trial + 1) * 100 / TRIALS;
                progressConsumer.accept(percent);
            }
        }

        if (0 == successfulTrials) {
            int fallbackSum = 0;
            for (int i = 0; i < totalScenarios; i++) {
                if (isAccessibleLeaf[i]) {
                    fallbackSum += targets[i];
                }
            }
            return fallbackSum;
        }

        return (int) Math.round((double) totalCycles / successfulTrials);
    }

    private static int simulateCycle(
            final SimPool pool,
            final int[] executions,
            final int target,
            final double completionBoost,
            final Random random,
            final int[] pathBuffer,
            final int[] pathLen,
            final int[] candidatesBuffer,
            final double[] weightsBuffer,
            final boolean[] isMono,
            final boolean[] isNode,
            final int[] targets) {

        if (isNull(pool)) return -1;
        final int[] candidateIds = pool.candidateIds();
        final int candidateCount = candidateIds.length;
        if (0 == candidateCount) return -1;

        final boolean[] hasRepeatableLeaf = pool.hasRepeatableLeaf();
        final int[][] descLeafIds = pool.descLeafIds();
        final double[] baseWeights = pool.baseWeights();
        final SimPool[] childPools = pool.childPools();

        int validCount = 0;
        double totalWeight = 0.0;

        for (int i = 0; i < candidateCount; i++) {
            final int scId = candidateIds[i];
            if ((isMono[scId] || Scenario.forbidOverexecution) && executions[scId] >= targets[scId]) continue;
            if (isNode[scId] && !hasRepeatableLeaf[i]) {
                final int[] descLeafs = descLeafIds[i];
                boolean hasExecutable = false;
                for (int j = 0; j < descLeafs.length; j++) {
                    if (executions[descLeafs[j]] == 0) {
                        hasExecutable = true;
                        break;
                    }
                }
                if (!hasExecutable) continue;
            }

            double weight = baseWeights[i];
            if (completionBoost > 0.0) {
                final double uncompletedWeight = getUncompletedWeight(descLeafIds[i], executions, targets);
                if (uncompletedWeight > 0.0) {
                    weight += completionBoost * uncompletedWeight;
                }
            }

            if (weight > 0.0) {
                candidatesBuffer[validCount] = i;
                weightsBuffer[validCount] = weight;
                validCount++;
                totalWeight += weight;
            }
        }

        if (0 == validCount || totalWeight <= 0.0) return -1;

        final double r = random.nextDouble() * totalWeight;
        double cumulative = 0.0;
        int chosenIdx = candidatesBuffer[validCount - 1];
        for (int j = 0; j < validCount; j++) {
            cumulative += weightsBuffer[j];
            if (r <= cumulative) {
                chosenIdx = candidatesBuffer[j];
                break;
            }
        }

        final int chosenScId = candidateIds[chosenIdx];
        final SimPool childPool = childPools[chosenIdx];
        if (isNotNull(childPool)) {
            pathBuffer[pathLen[0]++] = chosenScId;
            return simulateCycle(childPool, executions, target, completionBoost, random,
                    pathBuffer, pathLen, candidatesBuffer, weightsBuffer, isMono, isNode, targets);
        }

        return chosenScId;
    }

    private static double getUncompletedWeight(
            final int[] descLeafIds,
            final int[] executions,
            final int[] targets) {

        double total = 0.0;
        for (final int leafId : descLeafIds) {
            final int leafTarget = targets[leafId];
            if (executions[leafId] < leafTarget) {
                total += (double) (leafTarget - executions[leafId]) / leafTarget;
            }
        }
        return total;
    }
}
