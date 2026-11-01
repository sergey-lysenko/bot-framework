package works.lysenko.base.output;

import works.lysenko.Base;
import works.lysenko.base.ui.UserInterface;
import works.lysenko.tree.base.Mono;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.prop.PropEnum;

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

/**
 * The AllLeafCompletions class.
 */
public final class AllLeafCompletions {

    private static final Map<_Scenario, Integer> lastRecordedExecs = new HashMap<>(0);
    private static long currentCycleEtaSum = 0L;
    private static int currentCycleEtaCount = 0;
    private static int lastRecordedTestNum = 0;
    private static boolean hasRecordedAny = false;

    private AllLeafCompletions() {
    }

    /**
     * Performs reset.
     */
    public static synchronized void reset() {

        lastRecordedExecs.clear();
        currentCycleEtaSum = 0L;
        currentCycleEtaCount = 0;
        lastRecordedTestNum = 0;
        hasRecordedAny = false;
    }

    /**
     * Performs record eta sample.
     * @param etaMs the etaMs
     */
    public static synchronized void recordEtaSample(final long etaMs) {

        if (etaMs > 0L) {
            currentCycleEtaSum += etaMs;
            currentCycleEtaCount++;
        }
    }

    private static String formatPatience(final double patiencePercentage) {

        final double rounded = Math.round(patiencePercentage * 100.0) / 100.0;
        if (Math.abs(rounded - Math.rint(rounded)) < 1e-6) {
            return Long.toString(Math.round(rounded));
        }
        final String formatted = String.format(java.util.Locale.ROOT, "%.2f", rounded);
        return formatted.endsWith("0") ? formatted.substring(0, formatted.length() - 1) : formatted;
    }

    /**
     * Performs append.
     * @param testNum the testNum
     * @param elapsedMillis the elapsedMillis
     * @param patiencePercentage the wandering patience percentage
     * @param etaMs the etaMs
     * @param goalIndex the goalIndex
     * @param leaf the leaf
     */
    public static synchronized void append(final int testNum, final long elapsedMillis, final double patiencePercentage, final long etaMs, final int goalIndex, final String leaf) {

        final Path path = Path.of(name(RUN_ALL_LEAF_COMPLETIONS_));
        final String line = "[ALL_LEAF_COMPLETION] " + testNum + " " + elapsedMillis + " " + formatPatience(patiencePercentage) + " " + etaMs + " " + goalIndex + " " + leaf + System.lineSeparator();
        try {
            Files.writeString(path, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            hasRecordedAny = true;
            if (testNum > lastRecordedTestNum) {
                lastRecordedTestNum = testNum;
            }
        } catch (final IOException e) {
            logEvent(S2, "Unable to record all-leaf completion in " + path + ": " + e.getMessage());
        }
    }

    /**
     * Records a non-leaf test cycle entry when leaf completion tracking is active and the given test cycle
     * finished without executing any leaf scenario (e.g., wandering cycles).
     *
     * @param testNum       the completed test number, or {@code null} if unknown
     * @param elapsedMillis the elapsed session duration in milliseconds
     */
    public static synchronized void recordCycleIfMissing(final Integer testNum, final long elapsedMillis) {

        if (isNull(testNum) || testNum <= lastRecordedTestNum || (!hasRecordedAny && !isAllLeafsMode())) return;
        checkAndRecord(elapsedMillis, null);
    }

    /**
     * Checks whether all-leafs mode is currently enabled via runtime parameters or test properties.
     *
     * @return {@code true} if all-leafs mode is active, {@code false} otherwise
     */
    private static boolean isAllLeafsMode() {

        if (isNotNull(Base.parameters)) {
            return Base.parameters.isAllLeafs();
        }
        final Integer count = PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get();
        return Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get()) || (isNotNull(count) && count > 1);
    }

    /**
     * Performs check and record.
     * @param elapsedMillis the elapsedMillis
     * @param completingScenario the completingScenario
     */
    public static synchronized void checkAndRecord(final long elapsedMillis, final _Scenario completingScenario) {

        if (isNull(Base.core)) return;

        final int testNum = Base.core.getTestsCount();
        if (testNum > lastRecordedTestNum) {
            lastRecordedTestNum = testNum;
        }

        final Set<_Scenario> accessibleLeafs = Base.core.getAccessibleLeafs();
        final boolean hasAccessibleLeafs = isNotNull(accessibleLeafs) && !accessibleLeafs.isEmpty();

        boolean reachedGoal = false;
        int completedGoalIndex = 0;

        if (hasAccessibleLeafs && isNotNull(completingScenario)) {
            final int target = resolveAllLeafsTarget();
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

        if (isAllLeafsMode() && isNotNull(Base.test)) {
            Base.test.syncPatienceAtCycleEnd(testNum, reachedGoal);
        }

        currentCycleEtaSum = 0L;
        currentCycleEtaCount = 0;
        final long cycleAvgEta = UserInterface.calculateEtaMs();

        EtaCorrection.recordTestSample(testNum, elapsedMillis, cycleAvgEta);

        if (Boolean.FALSE.equals(PropEnum._TEST_REPORT_ADD_ALL_LEAF.get())) return;

        final String leafName = (isNotNull(completingScenario)) ? completingScenario.getShortName() : "unknown";
        final double patiencePercentage = isNotNull(Base.test) ? Base.test.getPatiencePercentage() : 0.0;

        append(testNum, elapsedMillis, patiencePercentage, cycleAvgEta, reachedGoal ? completedGoalIndex : 0, leafName);
    }

    private static int resolveAllLeafsTarget() {

        if (isNotNull(Base.parameters)) {
            return Base.parameters.getAllLeafsExecutions();
        }
        final Integer prop = PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get();
        return isNotNull(prop) ? Math.max(1, prop) : 1;
    }
}
