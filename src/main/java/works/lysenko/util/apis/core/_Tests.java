package works.lysenko.util.apis.core;

import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Scenario;

import java.util.List;
import java.util.Set;

/**
 * This interface represents a Repeater object that manages the execution of scenarios.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanMethodIsAlwaysInverted", "unused"})
public interface _Tests {

    /**
     * Retrieves the count of active scenario execution paths.
     *
     * @return active Scenario Paths count
     */
    int getActiveScenarioPaths();

    /**
     * Retrieves all accessible leaf scenarios.
     *
     * @return set of all presently accessible leaf scenarios
     */
    Set<_Scenario> getAccessibleLeafs();

    /**
     * Retrieves all accessible node scenarios.
     *
     * @return set of all presently accessible node scenarios
     */
    Set<_Scenario> getAccessibleNodes();

    /**
     * Retrieves all accessible scenarios (nodes and leafs).
     *
     * @return set of all presently accessible scenarios
     */
    Set<_Scenario> getAccessibleScenarios();

    /**
     * Checks if all leaf scenarios have been executed.
     *
     * @return true if all presently accessible leaf scenarios have been executed at least once
     */
    boolean areAllLeafsExecuted();

    /**
     * Retrieves the count of executed leaf scenarios.
     *
     * @return number of executed accessible leaf scenarios
     */
    int getExecutedLeafsCount();

    /**
     * Retrieves the currently executed test number.
     *
     * @return currently executed test number
     */
    Integer getCurrentTestNumber();

    /**
     * Retrieves the total count of tests.
     *
     * @return current number of tests
     */
    int getTestsCount();

    /**
     * Retrieves the current exception reference if execution failed.
     *
     * @return exception object reference
     */
    Object getException();

    /**
     * Retrieves the set of root scenarios.
     *
     * @return root Scenarios set
     */
    Set<_Scenario> getRootScenarios();

    /**
     * Retrieves the status of the object.
     *
     * @return The status of the object as a string.
     */
    String getStatus();

    /**
     * Retrieves the stop flag indicating whether execution should halt.
     *
     * @return a boolean value where true indicates that execution should stop,
     * and false indicates that execution can continue.
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    boolean getStopFlag();

    /**
     * Sets the stop flag to indicate whether execution should halt.
     *
     * @param stopFlag a boolean value where true indicates that execution should stop,
     *                 and false indicates that execution can continue
     */
    void setStopFlag(boolean stopFlag);

    /**
     * Retrieves the test execution summary table.
     *
     * @return Test summary table as a list of rows
     */
    List<List<String>> getTestsSummary();

    /**
     * Retrieves the total scenario execution paths count.
     *
     * @return total Scenario Paths
     */
    int getTotalScenarioPaths();

    /**
     * Checks whether the execution of the test is currently stopping.
     *
     * @return true if the execution is stopping, false otherwise.
     * @throws SafeguardException if an error occurs while checking the stopping state.
     */
    boolean isStopRequestedByDashboard() throws SafeguardException;
}
