package works.lysenko.util.apis.execution;

import works.lysenko.base.Logs;
import works.lysenko.tree.Core;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Scenario;

/**
 * The _Scenarios interface defines the contract for managing execution scenarios
 * and their logical depth in a stack-based structure. It is used for tracking
 * and interacting with scenarios during execution, enabling operations such as
 * pushing, popping, and querying scenarios.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Scenarios {

    /**
     * Used by {@link Logs}
     *
     * @return logical depth of the current scenario
     */
    int depth();

    /**
     * Retrieves the currently active scenario in the execution stack.
     *
     * @return the active {@link _Scenario} instance.
     */
    _Scenario current();

    /**
     * String name of current {@link _Scenario}
     *
     * @param clearStack or not
     * @return name of Scenario
     */
    String currentString(boolean clearStack);

    /**
     * @return The minimal depth of the current scenario.
     */
    Integer minDepth();

    /**
     * Sets the minimal depth of the current scenario.
     *
     * @param minDepth the minimal depth of the current scenario
     */
    void minDepth(Integer minDepth);

    /**
     * Removes the given scenario from the execution stack.
     *
     * @param scenario the scenario to be removed
     * @throws SafeguardException if an exception occurs during the removal of the scenario
     */
    void pop(_Scenario scenario) throws SafeguardException;

    /**
     * Add {@link _Scenario} to execution stack
     * Used by {@link Core}
     *
     * @param scenario to push into stack
     */
    void push(_Scenario scenario);

}
