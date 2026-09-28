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
     * @return active Scenario Paths
     */
    int getActiveScenarioPaths();

    /**
     * @return currently executed test
     */
    Integer getCurrentTestNumber();

    /**
     * @return exception object reference
     */
    Object getException();

    /**
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
     * @return Test summary
     */
    List<List<String>> getTestsSummary();

    /**
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
