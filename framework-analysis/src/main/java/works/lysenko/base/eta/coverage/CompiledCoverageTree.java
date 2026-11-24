package works.lysenko.base.eta.coverage;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.tree.Ctrl;
import works.lysenko.tree.base.Mono;
import works.lysenko.util.apis.scenario._Ctrl;
import works.lysenko.util.apis.scenario._Node;
import works.lysenko.util.apis.scenario._Pool;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.prop.tree.Include;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.isNull;
import static works.lysenko.util.func.core.Weights.downstreamWeight;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Flattens a {@link _Ctrl} scenario tree into primitive arrays and precomputed {@link SimPool} structures
 * for zero-allocation Monte Carlo simulation.
 *
 * @param totalScenarios     total indexed scenarios
 * @param accessibleLeaves   count of accessible leaf scenarios
 * @param maxPoolCandidates  maximum candidate count across all pools
 * @param targets            target execution count per scenario ID
 * @param isMono             flag per scenario ID indicating {@link Mono}
 * @param isNode             flag per scenario ID indicating {@link _Node}
 * @param isAccessibleLeaf   flag per scenario ID indicating accessible leaf
 * @param rootPool           compiled root simulation pool
 */
record CompiledCoverageTree(
        int totalScenarios,
        int accessibleLeaves,
        int maxPoolCandidates,
        int[] targets,
        boolean[] isMono,
        boolean[] isNode,
        boolean[] isAccessibleLeaf,
        SimPool rootPool
) {

    static CompiledCoverageTree compile(final _Ctrl rootCtrl, final Set<_Scenario> accessibleLeaves, final int targetExecutions) {
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
            isAccessibleLeaf[i] = accessibleLeaves.contains(sc);
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
                        if (accessibleLeaves.contains(d)) {
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
                if (accessibleLeaves.contains(sc)) {
                    descLeafIds[i] = new int[]{i};
                    hasRepeatableLeaf[i] = !(sc instanceof Mono);
                } else {
                    descLeafIds[i] = new int[0];
                    hasRepeatableLeaf[i] = false;
                }
            }
        }

        final Map<_Pool, SimPool> simPoolMap = new IdentityHashMap<>();
        final _Scenario rootParent = (rootCtrl instanceof Ctrl ctrl) ? ctrl.getParent() : null;
        final SimPool simRootPool = buildSimPool(rootCtrl.getPool(), rootParent, scenarioIndexMap, simPoolMap,
                descScenarioIds, descLeafIds, hasRepeatableLeaf);

        int maxPoolCandidates = 0;
        for (final SimPool sp : simPoolMap.values()) {
            if (sp.candidateIds().length > maxPoolCandidates) {
                maxPoolCandidates = sp.candidateIds().length;
            }
        }
        if (0 == maxPoolCandidates) maxPoolCandidates = 1;

        return new CompiledCoverageTree(
                totalScenarios,
                accessibleLeaves.size(),
                maxPoolCandidates,
                targets,
                isMono,
                isNode,
                isAccessibleLeaf,
                simRootPool);
    }

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
        final int[][] cDescLeaves = new int[size][];
        final boolean[] cHasRepeatable = new boolean[size];
        final SimPool[] cPools = new SimPool[size];

        for (int i = 0; i < size; i++) {
            final int scId = candIds.get(i);
            cIds[i] = scId;
            bWeights[i] = baseWeights.get(i);
            cDescScenarios[i] = descScenarioIds[scId];
            cDescLeaves[i] = descLeafIds[scId];
            cHasRepeatable[i] = hasRepeatableLeaf[scId];
            cPools[i] = children.get(i);
        }

        final SimPool simPool = new SimPool(cIds, bWeights, cDescScenarios, cDescLeaves, cHasRepeatable, cPools);
        simPoolMap.put(pool, simPool);
        return simPool;
    }
}
