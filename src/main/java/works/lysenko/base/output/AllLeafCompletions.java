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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.spec.Layout.Files.name;
import static works.lysenko.util.spec.Layout.Templates.RUN_ALL_LEAF_COMPLETIONS_;

public final class AllLeafCompletions {

    private static final Map<_Scenario, Integer> lastRecordedExecs = new HashMap<>(0);
    private static long currentCycleEtaSum = 0L;
    private static int currentCycleEtaCount = 0;

    private AllLeafCompletions() {
    }

    public static synchronized void reset() {

        lastRecordedExecs.clear();
        currentCycleEtaSum = 0L;
        currentCycleEtaCount = 0;
    }

    public static synchronized void recordEtaSample(final long etaMs) {

        if (etaMs > 0L) {
            currentCycleEtaSum += etaMs;
            currentCycleEtaCount++;
        }
    }

    public static synchronized void append(final long elapsedMillis, final long etaMs, final int goalIndex, final String leaf) {

        final Path path = Path.of(name(RUN_ALL_LEAF_COMPLETIONS_));
        final String line = "[ALL_LEAF_COMPLETION] " + elapsedMillis + " " + etaMs + " " + goalIndex + " " + leaf + System.lineSeparator();
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

        final String leafName = (isNotNull(completingScenario)) ? completingScenario.getShortName() : "unknown";

        final Set<_Scenario> accessibleLeafs = Base.core.getAccessibleLeafs();
        if (isNull(accessibleLeafs) || accessibleLeafs.isEmpty()) {
            append(elapsedMillis, cycleAvgEta, 0, leafName);
            return;
        }

        final int target = resolveAllLeafsTarget();
        boolean reachedGoal = false;
        int completedGoalIndex = 0;

        if (isNotNull(completingScenario)) {
            final int execs = (isNotNull(Base.core.getResults())) ? Base.core.getResults().getExecutions(completingScenario) : 0;
            final int effectiveExecs = (execs == 0) ? 1 : execs;
            final int leafTarget = (completingScenario instanceof Mono) ? 1 : target;

            if (effectiveExecs <= leafTarget && lastRecordedExecs.getOrDefault(completingScenario, 0) < effectiveExecs) {
                lastRecordedExecs.put(completingScenario, effectiveExecs);
                reachedGoal = true;
                completedGoalIndex = Base.core.getExecutedLeafExecutionsCount();
                if (execs == 0 && completedGoalIndex == 0) {
                    completedGoalIndex = 1;
                }
            }
        }

        append(elapsedMillis, cycleAvgEta, reachedGoal ? completedGoalIndex : 0, leafName);
    }

    private static int resolveAllLeafsTarget() {

        if (isNotNull(Base.parameters)) {
            return Base.parameters.getAllLeafsCount();
        }
        final Integer prop = PropEnum._TEST_ALL_LEAFS_COUNT.get();
        return isNotNull(prop) ? Math.max(1, prop) : 1;
    }
}
