package works.lysenko.base.util;

import works.lysenko.Base;
import works.lysenko.base.output.EtaCorrection;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.prop.PropEnum;

import static java.util.Objects.isNull;
import static works.lysenko.Base.core;
import static works.lysenko.Base.parameters;
import static works.lysenko.Base.timer;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Pure runtime ETA estimator decoupled from Swing UI components.
 */
public final class EtaCalculator {

    private EtaCalculator() {
    }

    /**
     * Estimates remaining execution duration in milliseconds across all supported execution modes.
     *
     * @return estimated remaining execution duration in milliseconds, or {@code 0L} when completed or unavailable
     */
    public static long calculateEtaMs() {

        if (isNull(core) || isNull(timer)) return 0L;

        final long elapsedMs = timer.msSinceStart();
        if (elapsedMs < 1000L) return 0L;

        long rawEtaMs = 0L;

        final Integer configuredLeafExecs = PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get();
        final boolean allLeafsMode = (isNotNull(parameters))
                ? parameters.isAllLeafs()
                : (Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get())
                        || (isNotNull(configuredLeafExecs) && configuredLeafExecs > 1));

        if (allLeafsMode) {
            final boolean multipleExecutionsPerLeaf = (isNotNull(parameters))
                    ? (parameters.getAllLeafsExecutions() > 1)
                    : (isNotNull(configuredLeafExecs) && configuredLeafExecs > 1);
            final int executed = multipleExecutionsPerLeaf
                    ? core.getExecutedLeafExecutionsCount()
                    : core.getExecutedLeafsCount();
            final int total = multipleExecutionsPerLeaf
                    ? core.getTotalLeafExecutionsCount()
                    : (isNotNull(core.getAccessibleLeafs()) ? core.getAccessibleLeafs().size() : 0);

            if (0 == total || (executed >= total && core.areAllLeafsExecuted())) {
                return 0L;
            }
            long leafEtaMs = -1L;
            if (executed > 0 && total > executed) {
                final double avgMsPerExecution = (double) elapsedMs / executed;
                final int remainingExecutions = total - executed;
                leafEtaMs = Math.round(remainingExecutions * avgMsPerExecution);
            }

            final _Test activeTest = isNotNull(core.getTest())
                    ? core.getTest()
                    : Base.test;
            final double patiencePct = isNotNull(activeTest) ? activeTest.getPatiencePercentage() : 0.0;
            final double w = Math.min(1.0, Math.max(0.0, patiencePct / 100.0));
            final long patienceEtaMs = isNotNull(activeTest) ? activeTest.estimatePatienceEtaMs(elapsedMs) : -1L;

            if (leafEtaMs >= 0L && patienceEtaMs >= 0L && w > 0.0) {
                rawEtaMs = Math.round((1.0 - w) * leafEtaMs + w * patienceEtaMs);
            } else if (leafEtaMs >= 0L) {
                rawEtaMs = leafEtaMs;
            } else if (patienceEtaMs >= 0L && w > 0.0) {
                rawEtaMs = patienceEtaMs;
            }
        } else {
            final Integer totalTests = core.getTotalTests();
            if (isNotNull(totalTests) && totalTests > 0) {
                final int completedTests = (isNotNull(core.getTest()) && isNotNull(core.getTest().repeater()) && isNotNull(core.getTest().repeater().getHistory()))
                        ? core.getTest().repeater().getHistory().size()
                        : 0;
                if (completedTests >= totalTests) {
                    return 0L;
                }
                if (completedTests > 0) {
                    final double avgMsPerTest = (double) elapsedMs / completedTests;
                    final int remainingTests = totalTests - completedTests;
                    rawEtaMs = Math.round(remainingTests * avgMsPerTest);
                }
            } else {
                final int totalLeafs = (isNotNull(core.getAccessibleLeafs())) ? core.getAccessibleLeafs().size() : 0;
                final int executedLeafs = core.getExecutedLeafsCount();

                if (totalLeafs > 0 && executedLeafs > 0 && totalLeafs > executedLeafs) {
                    final double avgMsPerLeaf = (double) elapsedMs / executedLeafs;
                    final int remainingLeafs = totalLeafs - executedLeafs;
                    rawEtaMs = Math.round(remainingLeafs * avgMsPerLeaf);
                }
            }
        }

        if (rawEtaMs <= 0L) return 0L;

        EtaCorrection.recordInitialPrediction(elapsedMs + rawEtaMs);
        return rawEtaMs;
    }
}
