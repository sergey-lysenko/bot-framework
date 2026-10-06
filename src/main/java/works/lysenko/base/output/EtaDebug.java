package works.lysenko.base.output;

import works.lysenko.Base;
import works.lysenko.base.ui.UserInterface;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_ETA_DEBUG_;

public final class EtaDebug {

    private static int currentCycleIndex = 0;
    private static long cycleStartMs = 0L;
    private static long initialEtaMs = 0L;
    private static final List<Long> cycleSamples = new ArrayList<>(0);

    private EtaDebug() {
    }

    public static synchronized void reset() {

        currentCycleIndex = 0;
        cycleStartMs = 0L;
        initialEtaMs = 0L;
        cycleSamples.clear();
    }

    public static synchronized void startCycle(final String name) {

        currentCycleIndex++;
        cycleStartMs = (isNotNull(Base.timer)) ? Base.timer.msSinceStart() : 0L;
        initialEtaMs = UserInterface.calculateEtaMs();
        cycleSamples.clear();
        if (initialEtaMs > 0L) {
            cycleSamples.add(initialEtaMs);
        }
    }

    public static synchronized void recordSample(final long etaMs) {

        if (etaMs > 0L) {
            cycleSamples.add(etaMs);
            AllLeafCompletions.recordEtaSample(etaMs);
        }
    }

    public static synchronized void endCycle(final String name) {

        if (cycleStartMs == 0L) return;

        final long cycleEndMs = (isNotNull(Base.timer)) ? Base.timer.msSinceStart() : 0L;
        final long realMs = Math.max(1L, cycleEndMs - cycleStartMs);
        final long finalEtaMs = UserInterface.calculateEtaMs();

        if (finalEtaMs > 0L) {
            cycleSamples.add(finalEtaMs);
        }

        if (cycleSamples.isEmpty()) {
            if (initialEtaMs > 0L) cycleSamples.add(initialEtaMs);
            else if (finalEtaMs > 0L) cycleSamples.add(finalEtaMs);
        }

        long sum = 0L;
        for (final long s : cycleSamples) {
            sum += s;
        }
        final long count = Math.max(1, cycleSamples.size());
        final long meanEtaMs = sum / count;

        double sumVariance = 0.0;
        for (final long s : cycleSamples) {
            final double diff = s - meanEtaMs;
            sumVariance += diff * diff;
        }
        final long stdDevEtaMs = Math.round(Math.sqrt(sumVariance / count));

        long predictedMs;
        if (initialEtaMs > 0L && finalEtaMs > 0L) {
            final long diffEta = (initialEtaMs - finalEtaMs) + realMs;
            predictedMs = Math.max(1L, diffEta);
        } else if (meanEtaMs > 0L) {
            predictedMs = meanEtaMs;
        } else {
            predictedMs = realMs;
        }

        append(currentCycleIndex, realMs, meanEtaMs, stdDevEtaMs, predictedMs, name);

        cycleStartMs = 0L;
        initialEtaMs = 0L;
        cycleSamples.clear();
    }

    public static synchronized void append(final int testNum, final long realMs, final long meanEtaMs,
                                           final long stdDevEtaMs, final long predictedMs, final String name) {

        final Path path = Path.of(name(RUN_ETA_DEBUG_));
        final String line = "[ETA_DEBUG] " + testNum + " " + realMs + " " + meanEtaMs + " " +
                stdDevEtaMs + " " + predictedMs + " " + name + System.lineSeparator();
        try {
            Files.writeString(path, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final IOException e) {
            logEvent(S2, "Unable to record eta debug in " + path + ": " + e.getMessage());
        }
    }
}
