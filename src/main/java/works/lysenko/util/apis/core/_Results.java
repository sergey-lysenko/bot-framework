package works.lysenko.util.apis.core;

import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.log._LogRecord;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.Result;

import java.util.List;
import java.util.Map;
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
