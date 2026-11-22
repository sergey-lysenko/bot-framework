package works.lysenko.util.apis.core;

import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.Result;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Represents an interface for providing results of scenario execution.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter"})
public interface _Results {

    /**
     * @param lr to add as event
     */
    void addEvent(_LogRecord lr);

    /**
     * Records that a non-scenario lifecycle phase (e.g. {@code "Pretest"}, {@code "Preflight"}, {@code "Postflight"})
     * has been executed during the run.
     *
     * @param phase lifecycle phase name
     */
    default void recordPhase(final String phase) {
    }

    /**
     * Retrieves all non-scenario lifecycle phases executed during the run.
     *
     * @return unmodifiable set of executed phase names
     */
    default Set<String> getExecutedPhases() {
        return Collections.emptySet();
    }

    /**
     * Retrieves root-level events emitted outside any scenario (e.g. during {@code Pretest}).
     *
     * @return unmodifiable list of root events
     */
    default List<_LogRecord> getRootEvents() {
        return Collections.emptyList();
    }

    /**
     * @return true if there are events which marked as failing
     */
    boolean areFailingEvents();

    /**
     * Count in test execution (in later versions there will be collection of more
     * execution data than just times of execution)
     *
     * @param scenario scenario to count
     * @return copy of added test execution data
     */
    Result count(_Scenario scenario);

    /**
     * Retrieves the number of executions of a given scenario.
     *
     * @param scenario the scenario to retrieve the execution count for
     * @return the execution count of the scenario
     */
    int getExecutions(_Scenario scenario);

    /**
     * Retrieves the execution result for the exact scenario instance.
     *
     * @param scenario scenario whose result is requested
     * @return result, or null when the scenario has not executed
     */
    Result getResult(_Scenario scenario);

    /**
     * Retrieves a list of failure causing events.
     *
     * @return a list of Strings representing failure details.
     */
    List<String> getFailures();

    /**
     * @return current most critical severity among results
     */
    Severity getGreatestSeverity();

    /**
     * Sort scenario classes ignoring the case which produces more "tree-like" order
     * of items and improves readability, excluding not executed scenarios
     *
     * @return execution counters sorted properly
     */
    Map<_Scenario, _Result> getSorted();

    /**
     * Sort scenario classes ignoring the case which produces more "tree-like" order
     * of items and improves readability, including not executed scenarios
     *
     * @param includeExternal if true, scenarios out of root hierarchy are also included
     * @return execution counters sorted properly
     */
    TreeMap<String, Result> getSortedStrings(boolean includeExternal);
}
