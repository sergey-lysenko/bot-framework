package works.lysenko.base.eta;

import works.lysenko.Base;
import works.lysenko.base.eta.calc.EtaFormatter;
import works.lysenko.base.eta.calc.EtaFormulas;
import works.lysenko.base.eta.calc.EtaModeResolver;
import works.lysenko.base.eta.model.EtaMode;
import works.lysenko.base.eta.model.EtaSnapshot;
import works.lysenko.base.eta.model.EtaState;
import works.lysenko.util.apis.test._Test;

import static java.util.Objects.isNull;
import static works.lysenko.Base.core;
import static works.lysenko.Base.timer;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Pure runtime ETA estimator decoupled from Swing UI components.
 * Shared across GUI ({@code UserInterface}), CLI/headless ({@code Directory}), and telemetry sidecars.
 */
public record EtaCalculator() {

    /**
     * Estimates remaining execution duration in milliseconds across all supported execution modes.
     *
     * @return estimated remaining execution duration in milliseconds, or {@code 0L} when completed or unavailable
     */
    public static long calculateEtaMs() {
        return capture(false).remainingMs();
    }

    /**
     * Captures a full ETA and progress snapshot including historical {@link EtaCorrection} calibration.
     *
     * @return current {@link EtaSnapshot}
     */
    public static EtaSnapshot capture() {
        return capture(true);
    }

    /**
     * Captures a full ETA and progress snapshot.
     *
     * @param includeCorrection whether to evaluate {@link EtaCorrection#getEstimationCorrection(long)}
     * @return current {@link EtaSnapshot}
     */
    public static EtaSnapshot capture(final boolean includeCorrection) {
        final long nowEpochMs = System.currentTimeMillis();
        if (isNull(core) || isNull(timer)) {
            return EtaSnapshot.unavailable(nowEpochMs);
        }

        final long runId = timer.startedAt();
        final long elapsedMs = Math.max(0L, timer.msSinceStart());
        final int currentTest = EtaModeResolver.resolveCurrentTest();
        final int totalTests = EtaModeResolver.resolveTotalTests();
        final int completedTests = EtaModeResolver.resolveCompletedTests();

        final EtaMode mode;
        final int executed;
        final int total;
        double patiencePct = 0.0;
        boolean done = false;
        long rawEtaMs = 0L;

        if (EtaModeResolver.isAllLeafsMode()) {
            mode = EtaMode.ALL_LEAFS;
            final boolean multipleExecs = EtaModeResolver.isMultipleExecutionsPerLeaf();
            executed = multipleExecs
                    ? core.getExecutedLeafExecutionsCount()
                    : core.getExecutedLeafsCount();
            total = multipleExecs
                    ? core.getTotalLeafExecutionsCount()
                    : (isNotNull(core.getAccessibleLeafs()) ? core.getAccessibleLeafs().size() : 0);

            final _Test activeTest = isNotNull(core.getTest()) ? core.getTest() : Base.test;
            patiencePct = isNotNull(activeTest) ? Math.max(0.0, activeTest.getPatiencePercentage()) : 0.0;

            if (total > 0 && executed >= total && core.areAllLeafsExecuted()) {
                done = true;
            } else if (elapsedMs >= 1000L && total > 0) {
                final long patienceEtaMs = isNotNull(activeTest) ? activeTest.estimatePatienceEtaMs(elapsedMs) : -1L;
                rawEtaMs = EtaFormulas.estimateAllLeafsEtaMs(elapsedMs, executed, total, patiencePct, patienceEtaMs);
                if (rawEtaMs <= 0L && patiencePct >= 100.0) {
                    done = true;
                }
            }
        } else if (totalTests > 0) {
            mode = EtaMode.FIXED_TESTS;
            executed = completedTests;
            total = totalTests;
            if (completedTests >= totalTests) {
                done = true;
            } else if (elapsedMs >= 1000L) {
                rawEtaMs = EtaFormulas.estimateFixedTestsEtaMs(elapsedMs, completedTests, totalTests);
            }
        } else {
            mode = EtaMode.LEAF_FALLBACK;
            total = isNotNull(core.getAccessibleLeafs()) ? core.getAccessibleLeafs().size() : 0;
            executed = core.getExecutedLeafsCount();
            if (elapsedMs >= 1000L) {
                rawEtaMs = EtaFormulas.estimateLeafFallbackEtaMs(elapsedMs, executed, total);
            }
        }

        if (rawEtaMs < 0L) rawEtaMs = 0L;
        if (rawEtaMs > 0L) {
            EtaCorrection.recordInitialPrediction(elapsedMs + rawEtaMs);
        }

        final EtaState state = resolveState(rawEtaMs, elapsedMs, done, executed, total);

        long correctionMs = 0L;
        long correctedRemainingMs = rawEtaMs;
        if (includeCorrection && rawEtaMs > 0L) {
            try {
                correctedRemainingMs = EtaCorrection.estimateCorrectedRemainingMs(elapsedMs, rawEtaMs);
                correctionMs = rawEtaMs - correctedRemainingMs;
            } catch (final Exception ignored) {
                correctionMs = 0L;
                correctedRemainingMs = rawEtaMs;
            }
        }

        final double progressPercent = (total > 0)
                ? Math.min(100.0, Math.max(0.0, (100.0 * executed) / total))
                : 0.0;

        final String formattedRemaining;
        final String formattedCorrectedRemaining;
        final String label;
        if (rawEtaMs > 0L) {
            formattedRemaining = EtaFormatter.formatMs(rawEtaMs);
            formattedCorrectedRemaining = EtaFormatter.formatMs(correctedRemainingMs);
            final String displayRemaining = (includeCorrection && correctionMs != 0L)
                    ? formattedCorrectedRemaining
                    : formattedRemaining;
            label = "(ETA " + displayRemaining + ")";
        } else if (state == EtaState.DONE) {
            formattedRemaining = EtaFormatter.DONE_TEXT;
            formattedCorrectedRemaining = EtaFormatter.DONE_TEXT;
            label = EtaFormatter.LABEL_DONE;
        } else {
            formattedRemaining = EtaFormatter.PLACEHOLDER_DURATION;
            formattedCorrectedRemaining = EtaFormatter.PLACEHOLDER_DURATION;
            label = EtaFormatter.LABEL_UNAVAILABLE;
        }

        return new EtaSnapshot(
                state,
                mode,
                runId,
                nowEpochMs,
                elapsedMs,
                rawEtaMs,
                correctionMs,
                correctedRemainingMs,
                currentTest,
                completedTests,
                totalTests,
                executed,
                total,
                progressPercent,
                patiencePct,
                formattedRemaining,
                formattedCorrectedRemaining,
                label
        );
    }

    /**
     * Pure stateless ETA calculation for {@link EtaMode#ALL_LEAFS} with wandering-patience blending.
     *
     * @param elapsedMs          elapsed execution duration in milliseconds
     * @param executed           executed leaf goals count
     * @param total              total leaf goals count
     * @param patiencePercentage current wandering patience percentage {@code [0..100]}
     * @param patienceEtaMs      estimated remaining patience window in ms, or {@code < 0} if unavailable
     * @return estimated remaining duration in milliseconds, or {@code 0L}
     */
    public static long estimateAllLeafsEtaMs(
            final long elapsedMs,
            final int executed,
            final int total,
            final double patiencePercentage,
            final long patienceEtaMs) {
        return EtaFormulas.estimateAllLeafsEtaMs(elapsedMs, executed, total, patiencePercentage, patienceEtaMs);
    }

    /**
     * Pure stateless ETA calculation for {@link EtaMode#FIXED_TESTS}.
     *
     * @param elapsedMs      elapsed execution duration in milliseconds
     * @param completedTests completed test cycles count
     * @param totalTests     target total test cycles count
     * @return estimated remaining duration in milliseconds, or {@code 0L}
     */
    public static long estimateFixedTestsEtaMs(
            final long elapsedMs,
            final int completedTests,
            final int totalTests) {
        return EtaFormulas.estimateFixedTestsEtaMs(elapsedMs, completedTests, totalTests);
    }

    /**
     * Pure stateless ETA calculation for {@link EtaMode#LEAF_FALLBACK}.
     *
     * @param elapsedMs     elapsed execution duration in milliseconds
     * @param executedLeafs executed accessible leafs count
     * @param totalLeafs    total accessible leafs count
     * @return estimated remaining duration in milliseconds, or {@code 0L}
     */
    public static long estimateLeafFallbackEtaMs(
            final long elapsedMs,
            final int executedLeafs,
            final int totalLeafs) {
        return EtaFormulas.estimateLeafFallbackEtaMs(elapsedMs, executedLeafs, totalLeafs);
    }

    /**
     * Formats milliseconds as {@code MM:SS} (under 1 hour) or {@code HH:MM:SS}.
     *
     * @param ms duration in milliseconds
     * @return formatted duration string
     */
    public static String formatMs(final long ms) {
        return EtaFormatter.formatMs(ms);
    }

    private static EtaState resolveState(
            final long rawEtaMs,
            final long elapsedMs,
            final boolean done,
            final int executed,
            final int total) {

        if (rawEtaMs > 0L) return EtaState.ESTIMATED;
        if (elapsedMs < 1000L) return EtaState.INITIALIZING;
        if (done) return EtaState.DONE;
        if (executed == 0 && total > 0) return EtaState.INITIALIZING;
        return EtaState.UNAVAILABLE;
    }
}
