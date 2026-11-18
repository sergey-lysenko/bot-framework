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
import works.lysenko.util.prop.PropEnum;

import java.util.*;
import java.util.function.IntConsumer;

import static java.util.Objects.isNull;
import static works.lysenko.util.func.core.Weights.downstreamWeight;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Estimates the average number of execution cycles required to achieve 100% leaf coverage
 * using a high-performance Monte Carlo simulation of the scenario selection algorithm.
 * <p>
 * The estimator flattens the scenario tree into primitive arrays and precomputes candidate
 * weights to execute Monte Carlo trials without object allocations during simulation loops.
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexMethod", "MagicNumber"})
public record CoverageEstimator() {

    /** Number of Monte Carlo simulation trials performed to compute the average. */
    private static final int TRIALS = 100;

    /** Maximum allowed cycles per simulation trial before timing out. */
    private static final int MAX_CYCLES_PER_TRIAL = 10000;

    /**
     * Estimates average cycles required to visit all accessible leafs the required amount of times,
     * using the target execution count configured in system parameters or properties.
     *
     * @param rootCtrl root controller managing the scenario tree
     * @return estimated average number of execution cycles required to achieve full leaf coverage
     */
    public static int estimateAverageCycles(final _Ctrl rootCtrl) {

        final int target = (isNotNull(works.lysenko.Base.parameters))
                ? works.lysenko.Base.parameters.getAllLeafsExecutions()
                : (isNotNull(PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()) ? Math.max(1, PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get()) : 1);
        return estimateAverageCycles(rootCtrl, target, null);
    }

    /**
     * Estimates average cycles required to visit all accessible leafs at least target times.
     *
     * @param rootCtrl root controller managing the scenario tree
     * @param target   required executions per leaf scenario
     * @return estimated average number of execution cycles required to achieve full leaf coverage
     */
    public static int estimateAverageCycles(final _Ctrl rootCtrl, final int target) {

        return estimateAverageCycles(rootCtrl, target, null);
    }

    /**
     * Estimates average cycles required to visit all accessible leafs at least target times,
     * reporting progress percentage to a consumer.
     *
     * @param rootCtrl         root controller managing the scenario tree
     * @param target           required executions per leaf scenario
     * @param progressConsumer consumer receiving progress percentage (0..100), nullable
     * @return estimated average number of execution cycles required to achieve full leaf coverage,
     *         or 0 if rootCtrl is null or has no accessible leafs
     */
    public static int estimateAverageCycles(
            final _Ctrl rootCtrl,
            final int target,
            final IntConsumer progressConsumer) {

        if (isNull(rootCtrl)) return 0;
        final Set<_Scenario> accessibleLeafs = rootCtrl.getAccessibleLeafs();
        if (accessibleLeafs.isEmpty()) return 0;
        final int targetExecutions = Math.max(1, target);
        if (1 == accessibleLeafs.size()) {
            if (isNotNull(progressConsumer)) progressConsumer.accept(100);
            final _Scenario only = accessibleLeafs.iterator().next();
            return (only instanceof Mono) ? 1 : targetExecutions;
        }

        if (isNotNull(progressConsumer)) progressConsumer.accept(0);

        final double completionBoost = isNotNull(Scenario.completionWeight)
                ? Scenario.completionWeight.doubleValue() : 0.0;
        final Random random = new Random(42);

        // 1. Index tree structure into primitive-indexed arrays
        final Map<_Scenario, Integer> scenarioIndexMap = new IdentityHashMap<>();
        final List<_Scenario> scenarios = new ArrayList<>();
        final Map<_Pool, Integer> poolIndexMap = new IdentityHashMap<>();
        final List<_Pool> pools = new ArrayList<>();

        indexTree(rootCtrl.getPool(), scenarioIndexMap, scenarios, poolIndexMap, pools);

        final int totalScenarios = scenarios.size();
        final int[][] descScenarioIds = new int[totalScenarios][];
        final int[][] descLeafIds = new int[totalScenarios][];
        final boolean[] hasRepeatableLeaf = new boolean[totalScenarios];
        final int[] targets = new int[totalScenarios];
        final boolean[] isMono = new boolean[totalScenarios];
        final boolean[] isNode = new boolean[totalScenarios];
        final boolean[] isAccessibleLeaf = new boolean[totalScenarios];

        for (int i = 0; i < totalScenarios; i++) {
            final _Scenario sc = scenarios.get(i);
            isMono[i] = sc instanceof Mono;
            isNode[i] = sc instanceof _Node;
            isAccessibleLeaf[i] = accessibleLeafs.contains(sc);
            targets[i] = isMono[i] ? 1 : targetExecutions;

            if (sc instanceof _Node node) {
                final Set<_Scenario> descScenarios = new LinkedHashSet<>();
                collectDescendants(node, descScenarios, new HashSet<>());

                final List<Integer> scList = new ArrayList<>();
                final List<Integer> leafList = new ArrayList<>();
                boolean hasRepeatable = false;

                for (final _Scenario d : descScenarios) {
                    final Integer dId = scenarioIndexMap.get(d);
                    if (isNotNull(dId)) {
                        scList.add(dId);
                        if (accessibleLeafs.contains(d)) {
                            leafList.add(dId);
                            if (!(d instanceof Mono)) {
                                hasRepeatable = true;
                            }
                        }
                    }
                }

                descScenarioIds[i] = scList.stream().mapToInt(Integer::intValue).toArray();
                descLeafIds[i] = leafList.stream().mapToInt(Integer::intValue).toArray();
                hasRepeatableLeaf[i] = hasRepeatable;
            } else {
                descScenarioIds[i] = new int[0];
                if (accessibleLeafs.contains(sc)) {
                    descLeafIds[i] = new int[]{i};
                    hasRepeatableLeaf[i] = !(sc instanceof Mono);
                } else {
                    descLeafIds[i] = new int[0];
                    hasRepeatableLeaf[i] = false;
                }
            }
        }

        // 2. Build precomputed simulation pools with static weights
        final Map<_Pool, SimPool> simPoolMap = new IdentityHashMap<>();
        final _Scenario rootParent = (rootCtrl instanceof Ctrl ctrl) ? ctrl.getParent() : null;
        final SimPool simRootPool = buildSimPool(rootCtrl.getPool(), rootParent, scenarioIndexMap, simPoolMap,
                descScenarioIds, descLeafIds, hasRepeatableLeaf);

        int maxPoolCandidates = 0;
        for (final SimPool sp : simPoolMap.values()) {
            if (sp.candidateIds.length > maxPoolCandidates) {
                maxPoolCandidates = sp.candidateIds.length;
            }
        }
        if (0 == maxPoolCandidates) maxPoolCandidates = 1;

        final int accessibleLeafsCount = accessibleLeafs.size();
        final int maxCycles = Math.min(MAX_CYCLES_PER_TRIAL, Math.max(1000, accessibleLeafsCount * targetExecutions * 20));
        final int maxStallCycles = Math.max(300, accessibleLeafsCount * 15);

        // Preallocate reusable buffers for zero allocations in the cycle loop
        final int[] executions = new int[totalScenarios];
        final int[] pathBuffer = new int[totalScenarios];
        final int[] pathLen = new int[1];
        final int[] candidatesBuffer = new int[maxPoolCandidates];
        final double[] weightsBuffer = new double[maxPoolCandidates];

        long totalCycles = 0;
        int successfulTrials = 0;

        // 3. Monte Carlo trials
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

    /**
     * Recursively indexes pools and scenarios into primitive ID mapping structures.
     *
     * @param pool             pool to index
     * @param scenarioIndexMap map from scenario instance to integer ID
     * @param scenarios        list collecting scenario instances by ID
     * @param poolIndexMap     map from pool instance to integer ID
     * @param pools            list collecting pool instances by ID
     */
    private static void indexTree(
            final _Pool pool,
            final Map<_Scenario, Integer> scenarioIndexMap,
            final List<_Scenario> scenarios,
            final Map<_Pool, Integer> poolIndexMap,
            final List<_Pool> pools) {

        if (isNull(pool) || poolIndexMap.containsKey(pool)) return;
        poolIndexMap.put(pool, pools.size());
        pools.add(pool);

        final List<KeyValue<_Scenario, Fraction>> pairs = pool.getPairList();
        if (isNull(pairs)) return;

        for (final KeyValue<_Scenario, Fraction> pair : pairs) {
            final _Scenario sc = pair.k();
            if (isNull(sc)) continue;
            if (!scenarioIndexMap.containsKey(sc)) {
                scenarioIndexMap.put(sc, scenarios.size());
                scenarios.add(sc);
            }
            if (sc instanceof _Node node) {
                indexTree(node.getPool(), scenarioIndexMap, scenarios, poolIndexMap, pools);
            }
        }
    }

    /**
     * Recursively collects executable descendant scenarios for a container node.
     *
     * @param node          container node to traverse
     * @param descScenarios set accumulating executable descendant scenarios
     * @param visited       set tracking visited nodes to avoid infinite cycles
     */
    private static void collectDescendants(final _Node node, final Set<_Scenario> descScenarios, final Set<_Node> visited) {

        if (isNull(node) || isNull(node.getPool()) || !visited.add(node)) return;
        final List<KeyValue<_Scenario, Fraction>> pairs = node.getPool().getPairList();
        if (isNull(pairs)) return;

        for (final KeyValue<_Scenario, Fraction> pair : pairs) {
            final _Scenario child = pair.k();
            if (isNull(child)) continue;
            if (!child.isExecutable() || child.calculateCombinations(true) <= 0) continue;
            if (descScenarios.add(child)) {
                if (child instanceof _Node childNode) {
                    collectDescendants(childNode, descScenarios, visited);
                }
            }
        }
    }

    /**
     * Precomputes a simulation pool with static weights and descendant scenario references.
     *
     * @param pool              pool to build simulation structure for
     * @param parent            parent scenario used for downstream weight computation
     * @param scenarioIndexMap  mapping from scenario instance to integer ID
     * @param simPoolMap        caching map for created simulation pools
     * @param descScenarioIds   array mapping scenario ID to descendant scenario IDs
     * @param descLeafIds       array mapping scenario ID to descendant leaf IDs
     * @param hasRepeatableLeaf array indicating if scenario has repeatable descendant leaves
     * @return precomputed simulation pool, or null if input pool is null
     */
    private static SimPool buildSimPool(
            final _Pool pool,
            final _Scenario parent,
            final Map<_Scenario, Integer> scenarioIndexMap,
            final Map<_Pool, SimPool> simPoolMap,
            final int[][] descScenarioIds,
            final int[][] descLeafIds,
            final boolean[] hasRepeatableLeaf) {

        if (isNull(pool)) return null;
        if (simPoolMap.containsKey(pool)) return simPoolMap.get(pool);

        final List<KeyValue<_Scenario, Fraction>> pairs = pool.getPairList();
        if (isNull(pairs) || pairs.isEmpty()) {
            final SimPool emptyPool = new SimPool(new int[0], new double[0], new int[0][0], new int[0][0], new boolean[0], new SimPool[0]);
            simPoolMap.put(pool, emptyPool);
            return emptyPool;
        }

        final List<Integer> candIds = new ArrayList<>();
        final List<Double> baseWeights = new ArrayList<>();
        final List<SimPool> children = new ArrayList<>();

        for (final KeyValue<_Scenario, Fraction> pair : pairs) {
            final _Scenario sc = pair.k();
            if (isNull(sc)) continue;
            if (!sc.isExecutable() || sc.calculateCombinations(true) <= 0) continue;

            final int scId = scenarioIndexMap.get(sc);
            if (sc instanceof _Node && descLeafIds[scId].length == 0) continue;

            double weight = isNotNull(pair.v()) ? pair.v().doubleValue() : 0.0;
            if (Include.upstream) {
                final Fraction up = sc.weightUpstream();
                if (isNotNull(up)) weight += up.doubleValue();
            }
            if (Include.downstream) {
                final Fraction down = downstreamWeight(sc, parent);
                if (isNotNull(down)) weight += down.doubleValue();
            }

            if (weight <= 0.0) continue;

            candIds.add(scId);
            baseWeights.add(weight);

            if (sc instanceof _Node node && isNotNull(node.getPool())) {
                children.add(buildSimPool(node.getPool(), sc, scenarioIndexMap, simPoolMap,
                        descScenarioIds, descLeafIds, hasRepeatableLeaf));
            } else {
                children.add(null);
            }
        }

        final int size = candIds.size();
        final int[] cIds = new int[size];
        final double[] bWeights = new double[size];
        final int[][] cDescScenarios = new int[size][];
        final int[][] cDescLeafs = new int[size][];
        final boolean[] cHasRepeatable = new boolean[size];
        final SimPool[] cPools = new SimPool[size];

        for (int i = 0; i < size; i++) {
            final int scId = candIds.get(i);
            cIds[i] = scId;
            bWeights[i] = baseWeights.get(i);
            cDescScenarios[i] = descScenarioIds[scId];
            cDescLeafs[i] = descLeafIds[scId];
            cHasRepeatable[i] = hasRepeatableLeaf[scId];
            cPools[i] = children.get(i);
        }

        final SimPool simPool = new SimPool(cIds, bWeights, cDescScenarios, cDescLeafs, cHasRepeatable, cPools);
        simPoolMap.put(pool, simPool);
        return simPool;
    }

    /**
     * Simulates a single traversal cycle through a simulation pool using weighted random selection.
     *
     * @param pool             current simulation pool
     * @param executions       array tracking current execution counts per scenario ID
     * @param target           required execution target count per leaf
     * @param completionBoost  factor for boosting weights of incomplete leaves
     * @param random           pseudorandom number generator
     * @param pathBuffer       reusable buffer recording scenario path taken during cycle
     * @param pathLen          single-element array tracking recorded path length
     * @param candidatesBuffer reusable buffer storing candidate indices
     * @param weightsBuffer    reusable buffer storing candidate weights
     * @param isMono           boolean array flagging single-execution scenarios
     * @param isNode           boolean array flagging container nodes
     * @param targets          array specifying target execution count per scenario ID
     * @return selected leaf scenario ID, or -1 if no valid candidate could be selected
     */
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
        final int candidateCount = pool.candidateIds.length;
        if (0 == candidateCount) return -1;

        int validCount = 0;
        double totalWeight = 0.0;

        for (int i = 0; i < candidateCount; i++) {
            final int scId = pool.candidateIds[i];
            if ((isMono[scId] || Scenario.forbidOverexecution) && executions[scId] >= targets[scId]) continue;
            if (isNode[scId] && !pool.hasRepeatableLeaf[i]) {
                final int[] descLeafs = pool.descLeafIds[i];
                boolean hasExecutable = false;
                for (int j = 0; j < descLeafs.length; j++) {
                    if (executions[descLeafs[j]] == 0) {
                        hasExecutable = true;
                        break;
                    }
                }
                if (!hasExecutable) continue;
            }

            double weight = pool.baseWeights[i];
            if (completionBoost > 0.0) {
                final double uncompletedWeight = getUncompletedWeight(pool.descLeafIds[i], executions, targets);
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

        final int chosenScId = pool.candidateIds[chosenIdx];
        final SimPool childPool = pool.childPools[chosenIdx];
        if (isNotNull(childPool)) {
            pathBuffer[pathLen[0]++] = chosenScId;
            return simulateCycle(childPool, executions, target, completionBoost, random,
                    pathBuffer, pathLen, candidatesBuffer, weightsBuffer, isMono, isNode, targets);
        }

        return chosenScId;
    }

    /**
     * Calculates the cumulative uncompleted ratio across all descendant leaves of a scenario.
     *
     * @param descLeafIds array of descendant leaf scenario IDs
     * @param executions  array tracking current execution counts per scenario ID
     * @param targets     array specifying target execution count per scenario ID
     * @return sum of remaining uncompleted fractions across descendant leaves
     */
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

    /**
     * Immutable precomputed simulation pool containing primitive-indexed candidates and static base weights.
     *
     * @param candidateIds      primitive scenario IDs of candidates in this pool
     * @param baseWeights       precalculated static base weights for candidates
     * @param descScenarioIds   array mapping candidate index to descendant scenario IDs
     * @param descLeafIds       array mapping candidate index to descendant leaf IDs
     * @param hasRepeatableLeaf flag per candidate indicating presence of non-Mono descendant leaves
     * @param childPools        nested simulation pools for node candidates, null for leaf candidates
     */
    private record SimPool(
            int[] candidateIds,
            double[] baseWeights,
            int[][] descScenarioIds,
            int[][] descLeafIds,
            boolean[] hasRepeatableLeaf,
            SimPool[] childPools
    ) {}
}
