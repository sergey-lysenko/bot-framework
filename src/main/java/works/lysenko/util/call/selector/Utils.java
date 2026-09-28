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
 * Utility class containing various helper methods for scenarios.
 */
@SuppressWarnings({"MethodWithMultipleReturnPoints", "StaticMethodOnlyUsedInOneClass"})
public record Utils() {

    /**
     * Creates a readable list of scenarios along with their weights.
     *
     * @param list              The list of scenarios along with their weights
     * @param includeZeroWeight Whether to include scenarios with zero weights in the list
     * @param isDebug           Whether to include weights in the list when in debug mode
     * @return The readable list of scenarios
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
     * Retrieves the pair containing the specified scenario from the provided candidates.
     *
     * @param scenario   The scenario to retrieve the pair for.
     * @param candidates The list of candidate pairs to search in.
     * @return The pair containing the specified scenario, or null if it is not found.
     */
    public static KeyValue<_Scenario, Fraction> getThePair(final _Scenario scenario, final Iterable<KeyValue<_Scenario,
            Fraction>> candidates) {

        return StreamSupport.stream(candidates.spliterator(), false)
                .filter(p -> p.k() == scenario)
                .findFirst()
                .orElse(null);
    }

    /**
     * Checks if the scenario is still selectable based on the given conditions.
     *
     * @param fits       Whether the scenario fits the selection criteria.
     * @param attempts   The number of attempts left.
     * @param candidates The collection of pairs representing scenarios and their weights.
     * @return true if the scenario is still selectable, false otherwise.
     */
    public static boolean isStillSelectable(final boolean fits, final int attempts, final Collection<KeyValue<_Scenario,
            Fraction>> candidates) {

        return !fits && 0 < attempts && !candidates.isEmpty() && !core.getStopFlag();
    }

    @SuppressWarnings("unused")
    private static void log(final String message) {

        Base.log(Level.none, gray(message), true);
    }

    /**
     * Logs the accounting information for scenarios.
     */
    public static void logAccounting() {

        if (Include.upstream) logDebug(b(c(UPSTREAM), WEIGHTS, ACCOUNTED));
        if (Include.downstream) logDebug(b(c(DOWNSTREAM), WEIGHTS, ACCOUNTED));
    }

    /**
     * Logs a debug message.
     *
     * @param message The message to be logged as debug.
     */
    public static void logDebug(final String message) {

        Base.logDebug(gray(message));
    }

    /**
     * Logs the details of a scenario.
     *
     * @param scenario The scenario for which the details are logged.
     */
    private static void logDetails(final _Scenario scenario) {

        logDebug(b(s(FAT_BUL), c(SCENARIO), q(scenario.getShortName()), FITS, WITH,
                s(DOWNSTREAM, e(ROUND, ts(scenario.weightDownstream())), _COMMA_),
                s(CONFIGURED, e(ROUND, ts(scenario.weightConfigured()))), AND,
                s(UPSTREAM, e(ROUND, ts(scenario.weightUpstream())), _COMMA_), EXECUTING, DOTS));
    }

    /**
     * Logs an event with the specified severity and message.
     *
     * @param severity The severity of the event
     * @param message  The message to be logged
     */
    public static void logEvent(final Severity severity, final String message) {

        Base.logEvent(severity, message);
    }

    /**
     * Logs a scenario as not executable.
     *
     * @param scenario The scenario that is not executable.
     */
    public static void logScenarioNotExecutable(final _Scenario scenario) {

        if (Notify.notExecutable) {
            logEvent(S3, b(scenario.getName(), IS, NOT, EXECUTABLE));
        }
    }

    private static void processSafeguard() throws SafeguardException {

        final int depth = exec.scenarios().depth();
        logTrace(b(a(DEPTH, depth, _COMMA_), a(SAFEGUARD, scenarioDepth)));
        if (depth > scenarioDepth)
            throw new SafeguardException(b(SCENARIO, IS, TOO, DEEP), b(s(depth), IS, MORE, THAN, s(scenarioDepth)));
    }

    /**
     * Verifies the scenario selection by logging the details of the scenario and processing the safeguard.
     *
     * @param scenario The scenario to be verified
     * @return The provided scenario after verification
     * @throws SafeguardException if an exception occurs during safeguard processing
     */
    public static _Scenario verifyScenarioSelection(final _Scenario scenario) throws SafeguardException {

        logDetails(scenario);
        processSafeguard();
        return scenario;
    }

    /**
     * Represents a consumer that outputs weights of scenarios.
     */
    private record weightOutput(String replacedQualifiedName, boolean includeZeroWeight, Collection<String> set,
                                boolean isDebug) implements Consumer<KeyValue<_Scenario, Fraction>> {

        public void accept(final KeyValue<_Scenario, Fraction> p) {

            final String scenarioStr = p.k().getClass().getName().replace(replacedQualifiedName, StringUtils.EMPTY);
            final Fraction weight = (null == p.v()) ? fr(0.0) : p.v();
            if (0 < weight.doubleValue() || includeZeroWeight)
                set.add(s(scenarioStr, (isDebug) ? s(_COLON_, s(ts(p.v()))) : StringUtils.EMPTY));
        }
    }
}
