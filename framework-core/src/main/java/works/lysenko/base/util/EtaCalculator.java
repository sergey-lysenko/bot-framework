package works.lysenko.base.util;

import works.lysenko.Base;
import works.lysenko.base.output.EtaCorrection;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.prop.PropEnum;

import java.util.Locale;

import static java.util.Objects.isNull;
import static works.lysenko.Base.core;
import static works.lysenko.Base.parameters;
import static works.lysenko.Base.timer;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Pure runtime ETA estimator decoupled from Swing UI components.
 * Shared across GUI ({@code UserInterface}), CLI/headless ({@code Directory}), and external telemetry sidecars.
 */
public final class EtaCalculator {

    private static final String LABEL_UNAVAILABLE = "(ETA --:--)";
    private static final String LABEL_DONE = "(ETA Done)";

    private EtaCalculator() {
    }

    /**
     * High-level lifecycle state of the ETA estimate.
     */
    public enum State {
        INITIALIZING,
        ESTIMATED,
        DONE,
        UNAVAILABLE
    }

    /**
     * Active execution estimation mode.
     */
    public enum Mode {
        ALL_LEAFS,
        FIXED_TESTS,
        LEAF_FALLBACK
    }

    /**
     * Immutable runtime ETA &amp; progress snapshot suitable for GUI labels and {@code dash/eta} JSON export.
     */
    public record Snapshot(
            State state,
            Mode mode,
            long runId,
            long updatedAtEpochMs,
            long elapsedMs,
            long remainingMs,
            long correctionMs,
            long correctedRemainingMs,
            int currentTest,
            int completedTests,
            int totalTests,
            int executed,
            int total,
            double progressPercent,
            double patiencePercent,
            String formattedRemaining,
            String formattedCorrectedRemaining,
            String label) {

        /**
         * Returns a terminal {@link State#DONE} copy of this snapshot at run teardown.
         *
         * @param nowEpochMs     completion epoch timestamp in milliseconds
         * @param finalElapsedMs final elapsed milliseconds
         * @return terminal snapshot marked {@link State#DONE}
         */
        public Snapshot asDone(final long nowEpochMs, final long finalElapsedMs) {

            final int doneExecuted = (total > 0) ? Math.max(executed, total) : executed;
            final double donePct = (total > 0) ? 100.0 : progressPercent;
            return new Snapshot(
                    State.DONE,
                    mode,
                    runId,
                    nowEpochMs,
                    Math.max(elapsedMs, finalElapsedMs),
                    0L,
                    0L,
                    0L,
                    currentTest,
                    completedTests,
                    totalTests,
                    doneExecuted,
                    total,
                    donePct,
                    patiencePercent,
                    "Done",
                    "Done",
                    LABEL_DONE
            );
        }

        /**
         * Serializes this snapshot as a compact JSON object for {@code dash/eta}.
         *
         * @param title current dashboard title string (may be {@code null})
         * @return single-line UTF-8 JSON representation
         */
        public String toJson(final String title) {

            final StringBuilder sb = new StringBuilder(384);
            sb.append('{');
            sb.append("\"state\":\"").append(state.name()).append("\",");
            sb.append("\"mode\":\"").append(mode.name()).append("\",");
            sb.append("\"runId\":").append(runId).append(',');
            sb.append("\"updatedAtEpochMs\":").append(updatedAtEpochMs).append(',');
            sb.append("\"elapsedMs\":").append(elapsedMs).append(',');
            sb.append("\"remainingMs\":").append(remainingMs).append(',');
            sb.append("\"correctionMs\":").append(correctionMs).append(',');
            sb.append("\"correctedRemainingMs\":").append(correctedRemainingMs).append(',');
            sb.append("\"currentTest\":").append(currentTest).append(',');
            sb.append("\"completedTests\":").append(completedTests).append(',');
            sb.append("\"totalTests\":").append(totalTests).append(',');
            sb.append("\"executed\":").append(executed).append(',');
            sb.append("\"total\":").append(total).append(',');
            sb.append("\"progressPercent\":").append(round2(progressPercent)).append(',');
            sb.append("\"patiencePercent\":").append(round2(patiencePercent)).append(',');
            sb.append("\"formattedRemaining\":\"").append(escapeJson(formattedRemaining)).append("\",");
            sb.append("\"formattedCorrectedRemaining\":\"").append(escapeJson(formattedCorrectedRemaining)).append("\",");
            sb.append("\"label\":\"").append(escapeJson(label)).append("\",");
            sb.append("\"title\":\"").append(escapeJson(null == title ? "" : title)).append('\"');
            sb.append('}');
            return sb.toString();
        }
    }

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
     * @return current ETA {@link Snapshot}
     */
    public static Snapshot capture() {

        return capture(true);
    }

    /**
     * Captures a full ETA and progress snapshot.
     *
     * @param includeCorrection whether to evaluate {@link EtaCorrection#getEstimationCorrection(long)}
     * @return current ETA {@link Snapshot}
     */
    public static Snapshot capture(final boolean includeCorrection) {

        final long nowEpochMs = System.currentTimeMillis();
        if (isNull(core) || isNull(timer)) {
            return new Snapshot(
                    State.UNAVAILABLE,
                    Mode.LEAF_FALLBACK,
                    0L,
                    nowEpochMs,
                    0L,
                    0L,
                    0L,
                    0L,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0.0,
                    0.0,
                    "--:--",
                    "--:--",
                    LABEL_UNAVAILABLE
            );
        }

        final long runId = timer.startedAt();
        final long elapsedMs = Math.max(0L, timer.msSinceStart());
        final Integer currentTestBoxed = core.getCurrentTestNumber();
        final int currentTest = isNotNull(currentTestBoxed) ? Math.max(0, currentTestBoxed) : 0;
        final Integer totalTestsBoxed = core.getTotalTests();
        final int totalTests = isNotNull(totalTestsBoxed) ? Math.max(0, totalTestsBoxed) : 0;
        final int completedTests = resolveCompletedTests();

        final Integer configuredLeafExecs = PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get();
        final boolean allLeafsMode = (isNotNull(parameters))
                ? parameters.isAllLeafs()
                : (Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get())
                        || (isNotNull(configuredLeafExecs) && configuredLeafExecs > 1));

        final Mode mode;
        final int executed;
        final int total;
        double patiencePct = 0.0;
        boolean done = false;
        long rawEtaMs = 0L;

        if (allLeafsMode) {
            mode = Mode.ALL_LEAFS;
            final boolean multipleExecutionsPerLeaf = (isNotNull(parameters))
                    ? (parameters.getAllLeafsExecutions() > 1)
                    : (isNotNull(configuredLeafExecs) && configuredLeafExecs > 1);
            executed = multipleExecutionsPerLeaf
                    ? core.getExecutedLeafExecutionsCount()
                    : core.getExecutedLeafsCount();
            total = multipleExecutionsPerLeaf
                    ? core.getTotalLeafExecutionsCount()
                    : (isNotNull(core.getAccessibleLeafs()) ? core.getAccessibleLeafs().size() : 0);

            final _Test activeTest = isNotNull(core.getTest())
                    ? core.getTest()
                    : Base.test;
            patiencePct = isNotNull(activeTest) ? Math.max(0.0, activeTest.getPatiencePercentage()) : 0.0;

            if (total > 0 && executed >= total && core.areAllLeafsExecuted()) {
                done = true;
            } else if (elapsedMs >= 1000L && total > 0) {
                final long patienceEtaMs = isNotNull(activeTest) ? activeTest.estimatePatienceEtaMs(elapsedMs) : -1L;
                rawEtaMs = estimateAllLeafsEtaMs(elapsedMs, executed, total, patiencePct, patienceEtaMs);
                if (rawEtaMs <= 0L && patiencePct >= 100.0) {
                    done = true;
                }
            }
        } else if (totalTests > 0) {
            mode = Mode.FIXED_TESTS;
            executed = completedTests;
            total = totalTests;
            if (completedTests >= totalTests) {
                done = true;
            } else if (elapsedMs >= 1000L) {
                rawEtaMs = estimateFixedTestsEtaMs(elapsedMs, completedTests, totalTests);
            }
        } else {
            mode = Mode.LEAF_FALLBACK;
            total = isNotNull(core.getAccessibleLeafs()) ? core.getAccessibleLeafs().size() : 0;
            executed = core.getExecutedLeafsCount();
            if (elapsedMs >= 1000L) {
                rawEtaMs = estimateLeafFallbackEtaMs(elapsedMs, executed, total);
            }
        }

        if (rawEtaMs < 0L) rawEtaMs = 0L;
        if (rawEtaMs > 0L) {
            EtaCorrection.recordInitialPrediction(elapsedMs + rawEtaMs);
        }

        final State state;
        if (rawEtaMs > 0L) {
            state = State.ESTIMATED;
        } else if (elapsedMs < 1000L) {
            state = State.INITIALIZING;
        } else if (done) {
            state = State.DONE;
        } else if (executed == 0 && total > 0) {
            state = State.INITIALIZING;
        } else {
            state = State.UNAVAILABLE;
        }

        long correctionMs = 0L;
        long correctedRemainingMs = rawEtaMs;
        if (includeCorrection && rawEtaMs > 0L) {
            try {
                correctionMs = EtaCorrection.getEstimationCorrection(elapsedMs);
                final long candidate = rawEtaMs - correctionMs;
                correctedRemainingMs = (candidate > 0L) ? candidate : rawEtaMs;
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
            formattedRemaining = formatMs(rawEtaMs);
            formattedCorrectedRemaining = formatMs(correctedRemainingMs);
            label = "(ETA " + formattedRemaining + ")";
        } else if (state == State.DONE) {
            formattedRemaining = "Done";
            formattedCorrectedRemaining = "Done";
            label = LABEL_DONE;
        } else {
            formattedRemaining = "--:--";
            formattedCorrectedRemaining = "--:--";
            label = LABEL_UNAVAILABLE;
        }

        return new Snapshot(
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
     * Pure stateless ETA calculation for {@link Mode#ALL_LEAFS} with wandering-patience blending.
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

        if (elapsedMs < 1000L || total <= 0) return 0L;

        long leafEtaMs = -1L;
        if (executed > 0 && total > executed) {
            final double avgMsPerExecution = (double) elapsedMs / executed;
            final int remainingExecutions = total - executed;
            leafEtaMs = Math.round(remainingExecutions * avgMsPerExecution);
        }

        final double w = Math.min(1.0, Math.max(0.0, patiencePercentage / 100.0));
        final long rawEtaMs;
        if (leafEtaMs >= 0L && patienceEtaMs >= 0L && w > 0.0) {
            rawEtaMs = Math.round((1.0 - w) * leafEtaMs + w * patienceEtaMs);
        } else if (leafEtaMs >= 0L) {
            rawEtaMs = leafEtaMs;
        } else if (patienceEtaMs >= 0L && w > 0.0) {
            rawEtaMs = patienceEtaMs;
        } else {
            rawEtaMs = 0L;
        }
        return Math.max(0L, rawEtaMs);
    }

    /**
     * Pure stateless ETA calculation for {@link Mode#FIXED_TESTS}.
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

        if (elapsedMs < 1000L || totalTests <= 0 || completedTests <= 0 || completedTests >= totalTests) {
            return 0L;
        }
        final double avgMsPerTest = (double) elapsedMs / completedTests;
        final int remainingTests = totalTests - completedTests;
        return Math.max(0L, Math.round(remainingTests * avgMsPerTest));
    }

    /**
     * Pure stateless ETA calculation for {@link Mode#LEAF_FALLBACK}.
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

        if (elapsedMs < 1000L || totalLeafs <= 0 || executedLeafs <= 0 || executedLeafs >= totalLeafs) {
            return 0L;
        }
        final double avgMsPerLeaf = (double) elapsedMs / executedLeafs;
        final int remainingLeafs = totalLeafs - executedLeafs;
        return Math.max(0L, Math.round(remainingLeafs * avgMsPerLeaf));
    }

    /**
     * Formats milliseconds as {@code MM:SS} (under 1 hour) or {@code HH:MM:SS}.
     *
     * @param ms duration in milliseconds
     * @return formatted duration string
     */
    public static String formatMs(final long ms) {

        final long totalSec = Math.max(0L, ms / 1000L);
        final long hours = totalSec / 3600L;
        final long minutes = (totalSec % 3600L) / 60L;
        final long seconds = totalSec % 60L;

        if (hours > 0L) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private static int resolveCompletedTests() {

        if (isNull(core) || isNull(core.getTest()) || isNull(core.getTest().repeater()) || isNull(core.getTest().repeater().getHistory())) {
            return 0;
        }
        return core.getTest().repeater().getHistory().size();
    }

    private static String round2(final double value) {

        if (Double.isNaN(value) || Double.isInfinite(value)) return "0.0";
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String escapeJson(final String s) {

        if (null == s || s.isEmpty()) return "";
        final StringBuilder out = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '\"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
