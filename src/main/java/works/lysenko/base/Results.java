package works.lysenko.base;

import works.lysenko.base.output.AllLeafCompletions;
import works.lysenko.base.output.EtaDebug;
import works.lysenko.util.apis.core._Results;
import works.lysenko.util.apis.data._Event;
import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.Result;
import works.lysenko.util.data.type.maps.SortedResult;
import works.lysenko.util.prop.core.Stop;

import java.util.*;

import static works.lysenko.Base.core;
import static works.lysenko.Base.exec;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.enums.Severity.S1;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.enums.Severity.greaterSeverity;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.C.CURRENT;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.S.SCENARIO;

/**
 * This class represents results of single bot execution
 *
 * @author Sergii Lysenko
 */
@SuppressWarnings({"ClassHasNoToStringMethod", "ChainedMethodCall", "ClassWithoutLogger", "PublicMethodWithoutLogging",
        "ClassWithTooManyTransitiveDependencies", "ClassWithTooManyTransitiveDependents", "CyclicClassDependency",
        "MethodWithMultipleLoops",
        "UnqualifiedFieldAccess", "DesignForExtension", "UseOfConcreteClass", "ClassUnconnectedToPackage",
        "NestedMethodCall", "ClassWithTooManyDependencies"})
public class Results implements _Results {

    @SuppressWarnings("WeakerAccess")
    private final Map<_Scenario, Result> results = new HashMap<>(0);
    private final List<_LogRecord> rootEvents = new ArrayList<>(0);
    private Severity max = null;

    /**
     * Constructs a new Results.
     */
    public Results() {

        AllLeafCompletions.reset();
        EtaDebug.reset();
    }

    @SuppressWarnings({"ChainedMethodCall", "LawOfDemeter", "NestedMethodCall"})
    private static String getTaggedString(final _Scenario scenario) {

        return b(scenario.getShortName(), scenario.type().tag());
    }

    /**
     * Performs add event.
     *
     * @param lr the lr
     */
    @SuppressWarnings("NestedMethodCall")
    @Override
    public void addEvent(final _LogRecord lr) {

        if (lr.data() instanceof final _Event event) {
            if (isNotNull(exec) && isNotNull(exec.scenarios()) && isNotNull(exec.scenarios().current())) {
                addEvent0(lr);
            } else {
                final Severity effective = getEffectiveSeverity(null, null, lr, event.severity());
                if (isNotNull(effective)) {
                    max = greaterSeverity(max, effective);
                }
                rootEvents.add(lr);
            }
        }
    }

    private static Severity getEffectiveSeverity(
            final _Scenario scenario,
            final _Result result,
            final _LogRecord lr,
            final Severity rawSeverity) {
        if (null == rawSeverity) return null;
        final works.lysenko.base.responses.ExpectedResponsesStore expStore = works.lysenko.base.output.TreeHtml.getExpectedResponsesStore();
        if (null != expStore && null != scenario) {
            if (expStore.isExpected(scenario, lr)) {
                return null;
            }
            final String text = lr.text();
            if (null != text && text.contains("[BROWSER]") && null != result && expStore.matchesNode(scenario, result)) {
                return null;
            }
        }
        final works.lysenko.base.issues.KnownIssuesStore kiStore = works.lysenko.base.output.TreeHtml.getKnownIssuesStore();
        if (null != kiStore) {
            final List<works.lysenko.base.issues.KnownIssue> matches = (null != scenario)
                    ? kiStore.getForLogRecord(scenario, lr)
                    : kiStore.getForLogRecord(Collections.emptyList(), lr);
            if (!matches.isEmpty()) {
                boolean decrease = false;
                for (final works.lysenko.base.issues.KnownIssue ki : matches) {
                    if (ki.decreaseToWarning()) {
                        decrease = true;
                        break;
                    }
                }
                if (decrease && (S0 == rawSeverity || S1 == rawSeverity)) {
                    return S2;
                }
            }
        }
        return rawSeverity;
    }

    /**
     * Determines if there are failing events.
     *
     * @return {@code true} if there are failing events, {@code false} otherwise
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    @Override
    public boolean areFailingEvents() {

        if (isNotNull(max)) return Stop.atSeverity.ordinal() >= max.ordinal();
        return false;
    }

    /**
     * Counts (increases by one) the amount of successful execution of a scenario.
     *
     * @param scenario The scenario to count successful execution for
     * @return The relevant Result object
     */
    @SuppressWarnings("LocalCanBeFinal")
    @Override
    public Result count(_Scenario scenario) {

        Result r = results.getOrDefault(scenario, new Result(scenario));
        r.tick();
        results.put(scenario, r);
        return r;
    }

    @Override
    public int getExecutions(final _Scenario scenario) {

        final Result r = results.get(scenario);
        return isNotNull(r) ? r.getExecutions() : 0;
    }

    @Override
    public Result getResult(final _Scenario scenario) {

        return results.get(scenario);
    }

    /**
     * Performs get failures.
     *
     * @return the get failures result
     */
    @Override
    public List<String> getFailures() {

        final List<String> list = new ArrayList<>(0);
        for (final _LogRecord event : rootEvents) {
            final Severity effective = getEffectiveSeverity(null, null, event, ((_Event) event.data()).severity());
            if (isNotNull(effective) && effective.ordinal() <= Stop.atSeverity.ordinal())
                list.add(event.data().message());
        }
        for (final Map.Entry<_Scenario, Result> entry : results.entrySet()) {
            for (final _LogRecord event : entry.getValue().getEvents()) {
                final Severity effective = getEffectiveSeverity(entry.getKey(), entry.getValue(), event, ((_Event) event.data()).severity());
                if (isNotNull(effective) && effective.ordinal() <= Stop.atSeverity.ordinal())
                    list.add(b(event.data().message(), IN, q(entry.getKey().getName()), SCENARIO));
            }
        }
        return list;
    }

    /**
     * Retrieves the greatest severity of indicated events.
     *
     * @return The greatest severity of events
     */
    @SuppressWarnings("SuspiciousGetterSetter")
    @Override
    public Severity getGreatestSeverity() {

        return max;
    }

    /**
     * Retrieves a sorted map of {@link _Scenario} and {@link Result} objects.
     *
     * @return The sorted map of ProvidesScenario and Result objects
     */
    @SuppressWarnings({"WeakerAccess", "LocalCanBeFinal"})
    @Override
    public Map<_Scenario, _Result> getSorted() {

        Map<_Scenario, _Result> sorted = new SortedResult();
        sorted.putAll(results);
        return sorted;
    }

    /**
     * Performs get sorted strings.
     *
     * @param includeExternal the includeExternal
     * @return the get sorted strings result
     */
    @SuppressWarnings({"ObjectAllocationInLoop", "LocalCanBeFinal", "NestedMethodCall", "UnqualifiedStaticUsage",
            "ForeachStatement",
            "BooleanParameter"})
    @Override
    public TreeMap<String, Result> getSortedStrings(boolean includeExternal) {

        TreeMap<String, Result> sortedStrings = new TreeMap<>();

        // Adding results of executed scenarios
        for (Map.Entry<_Scenario, Result> r : results.entrySet()) {
            if ((r.getKey().isFromMainTree() || includeExternal))
                sortedStrings.put(getTaggedString(r.getKey()), r.getValue());
        }
        // Adding result stubs of non-executed scenarios
        if (isNotNull(core)) {
            final Set<_Scenario> all = core.getAccessibleScenarios();
            if (isNotNull(all)) {
                for (final _Scenario scenario : all) {
                    final String tagged = getTaggedString(scenario);
                    if (!sortedStrings.containsKey(tagged)) {
                        sortedStrings.put(tagged, new Result(scenario));
                    }
                }
            }
            final Set<_Scenario> roots = core.getRootScenarios();
            if (isNotNull(roots)) {
                for (final _Scenario scenario : roots) {
                    final String tagged = getTaggedString(scenario);
                    if (!sortedStrings.containsKey(tagged)) {
                        sortedStrings.put(tagged, new Result(scenario));
                    }
                }
            }
        }
        return sortedStrings;
    }

    /**
     * Adds an event to the current scenario's result.
     *
     * @param lr The log record to add as an event
     */
    private void addEvent0(final _LogRecord lr) {

        if (isNotNull(exec)) {
            final _Scenario current = exec.scenarios().current();
            if (isNotNull(current)) {
                final Result result = results.computeIfAbsent(current, Result::new);
                result.addEvent(lr);
                if (lr.data() instanceof final works.lysenko.util.apis.data._Event event) {
                    final Severity effective = getEffectiveSeverity(current, result, lr, event.severity());
                    if (isNotNull(effective)) {
                        max = greaterSeverity(max, effective);
                        if (S0 == effective || S1 == effective) {
                            if (!current.hasChildFailed()) {
                                current.markAsFailed();
                            }
                        }
                    }
                }
            } else throw new IllegalStateException(b(c(CURRENT), SCENARIO, IS, NULL));
        } else throw new IllegalStateException(b(c(EXECUTION), IS, NULL));
    }
}
