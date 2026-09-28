package works.lysenko.base;

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
import static works.lysenko.util.data.enums.Severity.greaterSeverity;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.C.CURRENT;
import static works.lysenko.util.lang.word.E.EXECUTION;
import static works.lysenko.util.lang.word.R.RESULT;
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
    private Severity max = null;

    @SuppressWarnings({"ChainedMethodCall", "LawOfDemeter", "LocalCanBeFinal", "NestedMethodCall"})
    private static String getTaggedString(_Scenario scenario) {

        return b(scenario.getShortName(), scenario.type().tag());
    }

    @SuppressWarnings("NestedMethodCall")
    public void addEvent(final _LogRecord lr) {

        if (isNotNull(exec)) // if execution is started
            if (isNotNull(exec.scenarios().current())) // if there's a place to store
                if (lr.data() instanceof _Event) // if thing is correct
                {
                    max = greaterSeverity(max, ((_Event) lr.data()).severity());
                    addEvent0(lr);
                }
    }

    /**
     * Determines if there are failing events.
     *
     * @return {@code true} if there are failing events, {@code false} otherwise
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public boolean areFailingEvents() {

        if (isNotNull(max)) return Stop.atSeverity >= max.ordinal();
        return false;
    }

    /**
     * Counts (increases by one) the amount of successful execution of a scenario.
     *
     * @param scenario The scenario to count successful execution for
     * @return The relevant Result object
     */
    @SuppressWarnings("LocalCanBeFinal")
    public Result count(_Scenario scenario) {

        Result r = results.getOrDefault(scenario, new Result(scenario));
        r.tick();
        results.put(scenario, r);
        return r;
    }

    public List<String> getFailures() {

        final List<String> list = new ArrayList<>(0);
        for (final Map.Entry<_Scenario, Result> entry : results.entrySet()) {
            for (final _LogRecord event : entry.getValue().getEvents()) {
                if (((_Event) event.data()).severity().ordinal() <= Stop.atSeverity)
                    list.add(b(event.data().message(), IN, q(entry.getKey().getShortName()), SCENARIO));
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
    public Map<_Scenario, _Result> getSorted() {

        Map<_Scenario, _Result> sorted = new SortedResult();
        sorted.putAll(results);
        return sorted;
    }

    @SuppressWarnings({"ObjectAllocationInLoop", "LocalCanBeFinal", "NestedMethodCall", "UnqualifiedStaticUsage",
            "ForeachStatement",
            "BooleanParameter"})
    public TreeMap<String, Result> getSortedStrings(boolean includeExternal) {

        TreeMap<String, Result> sortedStrings = new TreeMap<>();

        // Adding results of executed scenarios
        for (Map.Entry<_Scenario, Result> r : results.entrySet()) {
            if ((r.getKey().isFromMainTree() || includeExternal))
                sortedStrings.put(getTaggedString(r.getKey()), r.getValue());
        }
        // Adding result stubs of non-executed scenarios
        for (_Scenario scenario : core.getRootScenarios())
            if (!sortedStrings.containsKey(getTaggedString(scenario)))
                sortedStrings.put(getTaggedString(scenario), new Result(scenario));
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
                final _Result result = results.getOrDefault(current, new Result(current));
                if (isNotNull(result)) {
                    result.addEvent(lr);
                } else throw new IllegalStateException(b(c(RESULT), IS, NULL));
            } else throw new IllegalStateException(b(c(CURRENT), SCENARIO, IS, NULL));
        } else throw new IllegalStateException(b(c(EXECUTION), IS, NULL));
    }
}