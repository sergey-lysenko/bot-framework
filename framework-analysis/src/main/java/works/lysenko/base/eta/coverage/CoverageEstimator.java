package works.lysenko.base.eta.coverage;

import works.lysenko.tree.base.Mono;
import works.lysenko.util.apis.scenario._Ctrl;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.prop.PropEnum;
import works.lysenko.util.prop.tree.Scenario;

import java.util.Set;
import java.util.function.IntConsumer;

import static java.util.Objects.isNull;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Estimates the average number of execution cycles required to achieve 100% leaf coverage
 * using a high-performance Monte Carlo simulation of the scenario selection algorithm.
 */
public record CoverageEstimator() {

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

        final CompiledCoverageTree compiled = CompiledCoverageTree.compile(rootCtrl, accessibleLeafs, targetExecutions);
        return MonteCarloSimulator.runTrials(compiled, targetExecutions, completionBoost, progressConsumer);
    }
}
