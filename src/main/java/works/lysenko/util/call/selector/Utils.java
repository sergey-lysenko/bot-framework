package works.lysenko.util.call.selector;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.Base;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.prop.tree.Include;
import works.lysenko.util.prop.tree.Notify;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.spec.Level;

import java.util.Collection;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.logTrace;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.___.*;
import static works.lysenko.util.chrs.____.*;
import static works.lysenko.util.data.enums.Ansi.gray;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.enums.Severity.S3;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.word.A.ACCOUNTED;
import static works.lysenko.util.lang.word.C.CONFIGURED;
import static works.lysenko.util.lang.word.D.DEEP;
import static works.lysenko.util.lang.word.D.DEPTH;
import static works.lysenko.util.lang.word.D.DOWNSTREAM;
import static works.lysenko.util.lang.word.E.EMPTY;
import static works.lysenko.util.lang.word.E.EXECUTABLE;
import static works.lysenko.util.lang.word.E.EXECUTING;
import static works.lysenko.util.lang.word.S.SAFEGUARD;
import static works.lysenko.util.lang.word.S.SCENARIO;
import static works.lysenko.util.lang.word.U.UPSTREAM;
import static works.lysenko.util.lang.word.W.WEIGHTS;
import static works.lysenko.util.prop.core.Safeguard.scenarioDepth;
import static works.lysenko.util.spec.Symbols.*;

/**
 * Utility record containing helper methods and logging procedures for scenario selection.
 */
@SuppressWarnings({"MethodWithMultipleReturnPoints", "StaticMethodOnlyUsedInOneClass"})
public record Utils() {

    /**
     * Creates a human-readable list of scenarios along with their weights.
     *
     * @param list              the iterable collection of scenarios paired with their weights
     * @param includeZeroWeight whether scenarios with zero weight should be included in the formatted output
     * @param isDebug           whether scenario weights should be appended to class names in debug mode
     * @return a formatted string listing scenario names and optionally their weights
     */
    @SuppressWarnings("LocalVariableNamingConvention")
    public static String createReadableScenariosList(final Iterable<KeyValue<_Scenario, Fraction>> list,
                                                     final boolean includeZeroWeight, final boolean isDebug) {

        final Collection<String> set = new TreeSet<>();
        if (list.iterator().hasNext()) {
            final KeyValue<_Scenario, Fraction> firstPair = list.iterator().next();
            final String qualifiedName = firstPair.k().getClass().getName();
            final String replacedQualifiedName =
                    qualifiedName.replace(qualifiedName.substring(qualifiedName.lastIndexOf(_DOT_) + 1), StringUtils.EMPTY);
            list.forEach(new weightOutput(replacedQualifiedName, includeZeroWeight, set, isDebug));
            return s(replacedQualifiedName.replace(s(Scenario.root, _DOT_), StringUtils.EMPTY), set);
        } else {
            return b(EMPTY, LIST);
        }
    }

    /**
     * Retrieves the candidate pair containing the specified scenario from the candidates pool.
     *
     * @param scenario   the scenario instance to look for
     * @param candidates the candidate pairs to search
     * @return the {@link KeyValue} pair matching the scenario by reference, or {@code null} if not found
     */
    public static KeyValue<_Scenario, Fraction> getThePair(final _Scenario scenario, final Iterable<KeyValue<_Scenario,
            Fraction>> candidates) {

        return StreamSupport.stream(candidates.spliterator(), false)
                .filter(p -> p.k() == scenario)
                .findFirst()
                .orElse(null);
    }

    /**
     * Determines whether scenario selection should continue for another retry iteration.
     *
     * @param fits       whether the currently selected scenario satisfies runtime prerequisites
     * @param attempts   the remaining retry attempt counter
     * @param candidates the collection of available scenario-weight candidate pairs
     * @return {@code true} if selection should retry; {@code false} if a valid scenario fit was found, attempts are exhausted, candidate pool is empty, or stop flag is set
     */
    public static boolean isStillSelectable(final boolean fits, final int attempts, final Collection<KeyValue<_Scenario,
            Fraction>> candidates) {

        return !fits && 0 < attempts && !candidates.isEmpty() && !core.getStopFlag();
    }

    /**
     * Logs a message with gray ANSI color formatting at {@link Level#none}.
     *
     * @param message the message to log
     */
    @SuppressWarnings("unused")
    private static void log(final String message) {

        Base.log(Level.none, gray(message), true);
    }

    /**
     * Logs accounting information for scenario upstream and downstream weights if enabled.
     */
    public static void logAccounting() {

        if (Include.upstream) logDebug(b(c(UPSTREAM), WEIGHTS, ACCOUNTED));
        if (Include.downstream) logDebug(b(c(DOWNSTREAM), WEIGHTS, ACCOUNTED));
    }

    /**
     * Logs a debug message formatted with gray color.
     *
     * @param message the message to log
     */
    public static void logDebug(final String message) {

        Base.logDebug(gray(message));
    }

    /**
     * Logs execution details and weight parameters for a selected scenario.
     *
     * @param scenario the scenario whose details are being logged
     */
    private static void logDetails(final _Scenario scenario) {

        logDebug(b(s(FAT_BUL), c(SCENARIO), q(scenario.getShortName()), FITS, WITH,
                s(DOWNSTREAM, e(ROUND, ts(scenario.weightDownstream())), _COMMA_),
                s(CONFIGURED, e(ROUND, ts(scenario.weightConfigured()))), AND,
                s(UPSTREAM, e(ROUND, ts(scenario.weightUpstream())), _COMMA_), EXECUTING, DOTS));
    }

    /**
     * Logs an event with the specified severity level and message text.
     *
     * @param severity the severity level of the event
     * @param message  the description message of the event
     */
    public static void logEvent(final Severity severity, final String message) {

        Base.logEvent(severity, message);
    }

    /**
     * Logs a warning notification when a scenario is determined to be non-executable.
     *
     * @param scenario the scenario that is not executable
     */
    public static void logScenarioNotExecutable(final _Scenario scenario) {

        if (Notify.notExecutable) {
            logEvent(S3, b(scenario.getName(), IS, NOT, EXECUTABLE));
        }
    }

    /**
     * Validates that the current execution nesting depth does not exceed the configured safeguard limit.
     *
     * @throws SafeguardException if current scenario nesting depth exceeds {@link Safeguard#scenarioDepth}
     */
    private static void processSafeguard() throws SafeguardException {

        final int depth = exec.scenarios().depth();
        logTrace(b(a(DEPTH, depth, _COMMA_), a(SAFEGUARD, scenarioDepth)));
        if (depth > scenarioDepth)
            throw new SafeguardException(b(SCENARIO, IS, TOO, DEEP), b(s(depth), IS, MORE, THAN, s(scenarioDepth)));
    }

    /**
     * Verifies and finalizes a selected scenario before execution.
     * <p>
     * Records scenario execution in {@link works.lysenko.base.output.SequenceTracker}, logs scenario details,
     * and evaluates recursion depth against safeguard limits.
     *
     * @param scenario the scenario instance to verify
     * @return the verified scenario instance
     * @throws SafeguardException if execution safeguard depth is exceeded
     */
    public static _Scenario verifyScenarioSelection(final _Scenario scenario) throws SafeguardException {

        works.lysenko.base.output.SequenceTracker.recordScenarioExecution(scenario);
        logDetails(scenario);
        processSafeguard();
        return scenario;
    }

    /**
     * Consumer implementation for formatting scenario class names and weights into output collections.
     *
     * @param replacedQualifiedName common package prefix string to remove from full class names
     * @param includeZeroWeight     whether scenarios with zero weight should be added
     * @param set                   the target collection storing formatted scenario string representations
     * @param isDebug               whether weight values should be appended to formatted strings
     */
    private record weightOutput(String replacedQualifiedName, boolean includeZeroWeight, Collection<String> set,
                                boolean isDebug) implements Consumer<KeyValue<_Scenario, Fraction>> {

        /**
         * Processes a scenario-weight pair and adds its formatted string representation to the collection if applicable.
         *
         * @param p the key-value pair containing scenario and weight
         */
        public void accept(final KeyValue<_Scenario, Fraction> p) {

            final String scenarioStr = p.k().getClass().getName().replace(replacedQualifiedName, StringUtils.EMPTY);
            final Fraction weight = (null == p.v()) ? fr(0.0) : p.v();
            if (0 < weight.doubleValue() || includeZeroWeight)
                set.add(s(scenarioStr, (isDebug) ? s(_COLON_, s(ts(p.v()))) : StringUtils.EMPTY));
        }
    }
}
