package works.lysenko.base.output;

import works.lysenko.Base;
import works.lysenko.base.ui.UserInterface;
import works.lysenko.tree.base.Mono;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.spec.PropEnum;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_ALL_LEAF_COMPLETIONS_;

public final class AllLeafCompletions {

    private static int lastRecordedRound = 0;
    private static long currentCycleEtaSum = 0L;
    private static int currentCycleEtaCount = 0;
    private static long roundEtaSum = 0L;
    private static int roundEtaCount = 0;

    private AllLeafCompletions() {
    }

    public static synchronized void reset() {

        lastRecordedRound = 0;
        currentCycleEtaSum = 0L;
        currentCycleEtaCount = 0;
        roundEtaSum = 0L;
        roundEtaCount = 0;
    }

    public static synchronized void recordEtaSample(final long etaMs) {

        if (etaMs > 0L) {
            currentCycleEtaSum += etaMs;
            currentCycleEtaCount++;
        }
    }

    public static synchronized void append(final long elapsedMillis, final long etaMs, final String leaf) {

        final Path path = Path.of(name(RUN_ALL_LEAF_COMPLETIONS_));
        final String line = "[ALL_LEAF_COMPLETION] " + elapsedMillis + " " + etaMs + " " + leaf + System.lineSeparator();
        try {
            Files.writeString(path, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (final IOException e) {
            logEvent(S2, "Unable to record all-leaf completion in " + path + ": " + e.getMessage());
        }
    }

    public static synchronized void checkAndRecord(final long elapsedMillis, final _Scenario completingScenario) {

        if (isNull(Base.core)) return;

        final long cycleAvgEta;
        if (currentCycleEtaCount > 0) {
            cycleAvgEta = currentCycleEtaSum / currentCycleEtaCount;
        } else {
            cycleAvgEta = UserInterface.calculateEtaMs();
        }
        currentCycleEtaSum = 0L;
        currentCycleEtaCount = 0;

        roundEtaSum += cycleAvgEta;
        roundEtaCount++;

        final Set<_Scenario> accessibleLeafs = Base.core.getAccessibleLeafs();
        if (isNull(accessibleLeafs) || accessibleLeafs.isEmpty()) return;

        final int target = resolveAllLeafsTarget();
        final String leafName = (isNotNull(completingScenario)) ? completingScenario.getShortName() : "unknown";

        int round = lastRecordedRound + 1;
        while (round <= target) {
            boolean roundComplete = true;
            for (final _Scenario leaf : accessibleLeafs) {
                final int leafTarget = (leaf instanceof Mono) ? 1 : round;
                int execs = (isNotNull(Base.core.getResults())) ? Base.core.getResults().getExecutions(leaf) : 0;
                if (isNotNull(completingScenario) && completingScenario.equals(leaf) && execs == 0) {
                    execs = 1;
                }
                if (execs < leafTarget) {
                    roundComplete = false;
                    break;
                }
            }
            if (roundComplete) {
                lastRecordedRound = round;
                final long etaForRound = (roundEtaCount > 0) ? (roundEtaSum / roundEtaCount) : cycleAvgEta;
                append(elapsedMillis, etaForRound, leafName);
                roundEtaSum = 0L;
                roundEtaCount = 0;
                round++;
            } else {
                break;
            }
        }
    }

    private static int resolveAllLeafsTarget() {

        if (isNotNull(Base.parameters)) {
            return Base.parameters.getAllLeafsCount();
        }
        final Integer prop = PropEnum._TEST_ALL_LEAFS_COUNT.get();
        return isNotNull(prop) ? Math.max(1, prop) : 1;
    }
}
