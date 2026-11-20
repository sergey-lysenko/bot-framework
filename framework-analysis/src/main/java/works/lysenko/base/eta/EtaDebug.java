package works.lysenko.base.eta;

import works.lysenko.Base;
import works.lysenko.base.eta.calc.EtaFormulas;
import works.lysenko.base.eta.model.CycleStats;
import works.lysenko.base.eta.model.EtaSnapshot;
import works.lysenko.util.prop.PropEnum;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_ETA_DEBUG_;

/**
 * Per-cycle ETA diagnostic tracker recording {@code [ETA_DEBUG]} telemetry entries to the run sidecar log.
 */
@SuppressWarnings("StaticNonFinalField")
public record EtaDebug() {

    private static long cycleStartMs = 0L;
    private static long initialEtaMs = 0L;
    private static long predictedCycleMs = 0L;
    private static final List<Long> cycleSamples = new ArrayList<>(0);

    /**
     * Resets per-cycle diagnostic state.
     */
    public static synchronized void reset() {
        cycleStartMs = 0L;
        initialEtaMs = 0L;
        predictedCycleMs = 0L;
        cycleSamples.clear();
    }

    /**
     * Begins tracking a new leaf/mono scenario execution cycle.
     *
     * @param name scenario short name
     */
    public static synchronized void startCycle(final String name) {
        cycleStartMs = (isNotNull(Base.timer)) ? Base.timer.msSinceStart() : 0L;
        final EtaSnapshot snapshot = EtaCalculator.capture(false);
        initialEtaMs = snapshot.remainingMs();
        final int remainingUnits = snapshot.total() - snapshot.executed();
        predictedCycleMs = (initialEtaMs > 0L && remainingUnits > 0)
                ? Math.max(1L, Math.round((double) initialEtaMs / remainingUnits))
                : 0L;
        cycleSamples.clear();
        if (initialEtaMs > 0L) {
            cycleSamples.add(cycleStartMs + initialEtaMs);
        }
    }

    /**
     * Records an intra-cycle remaining ETA sample as projected total runtime ({@code elapsed + etaMs}).
     *
     * @param etaMs sampled remaining duration in milliseconds
     */
    public static synchronized void recordSample(final long etaMs) {
        if (etaMs > 0L) {
            final long nowMs = (isNotNull(Base.timer)) ? Base.timer.msSinceStart() : 0L;
            cycleSamples.add(nowMs + etaMs);
            AllLeafCompletions.recordEtaSample(etaMs);
        }
    }

    /**
     * Completes the current execution cycle and appends an {@code [ETA_DEBUG]} telemetry line if enabled.
     *
     * @param name scenario short name
     */
    public static synchronized void endCycle(final String name) {
        if (Boolean.FALSE.equals(PropEnum._TEST_REPORT_ADD_ETA_DEBUG.get())) return;
        if (cycleStartMs == 0L) return;

        final long cycleEndMs = (isNotNull(Base.timer)) ? Base.timer.msSinceStart() : 0L;
        final long inFlightMs = (isNotNull(Base.core) && isNotNull(Base.core.getTest()) && isNotNull(Base.core.getTest().repeater()))
                ? Base.core.getTest().repeater().getInFlightMs(cycleEndMs)
                : 0L;
        final long realMs = (inFlightMs > 0L) ? inFlightMs : Math.max(1L, cycleEndMs - cycleStartMs);
        final long finalEtaMs = EtaCalculator.calculateEtaMs();

        if (finalEtaMs > 0L) {
            cycleSamples.add(cycleEndMs + finalEtaMs);
        }

        if (cycleSamples.isEmpty()) {
            if (initialEtaMs > 0L) cycleSamples.add(cycleStartMs + initialEtaMs);
            else if (finalEtaMs > 0L) cycleSamples.add(cycleEndMs + finalEtaMs);
        }

        final CycleStats stats = EtaFormulas.computeCycleStats(cycleSamples, initialEtaMs, finalEtaMs, realMs, predictedCycleMs);
        final int testNum = isNotNull(Base.core) ? Base.core.getTestsCount() : 0;
        append(testNum, realMs, stats.meanEtaMs(), stats.stdDevEtaMs(), stats.predictedMs(), name);

        cycleStartMs = 0L;
        initialEtaMs = 0L;
        predictedCycleMs = 0L;
        cycleSamples.clear();
    }

    /**
     * Appends a formatted {@code [ETA_DEBUG]} telemetry entry to the run sidecar log.
     *
     * @param testNum     1-based test cycle number
     * @param realMs      actual cycle duration in milliseconds
     * @param meanEtaMs   mean sampled ETA in milliseconds
     * @param stdDevEtaMs standard deviation of sampled ETA in milliseconds
     * @param predictedMs cycle-level predicted duration in milliseconds
     * @param name        scenario short name
     */
    public static synchronized void append(
            final int testNum,
            final long realMs,
            final long meanEtaMs,
            final long stdDevEtaMs,
            final long predictedMs,
            final String name) {

        final Path path = Path.of(name(RUN_ETA_DEBUG_));
        final String line = "[ETA_DEBUG] " + testNum + " " + realMs + " " + meanEtaMs + " "
                + stdDevEtaMs + " " + predictedMs + " " + name + System.lineSeparator();
        try {
            Files.writeString(path, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final IOException e) {
            logEvent(S2, "Unable to record eta debug in " + path + ": " + e.getMessage());
        }
    }
}
