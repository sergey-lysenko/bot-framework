package works.lysenko.util.apis.scenario;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.exception.checked.SafeguardException;

/**
 * This interface represents a tier in a scenario execution hierarchy. A1 tier is responsible for executing a set of
 * scenarios and managing their execution flow.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "OverloadedVarargsMethod"})
public interface _Node {

    /**
     * Retrieves the pool of (sub) scenarios associated with this node.
     *
     * @return The pool of scenarios.
     */
    _Pool getPool();

    /**
     * Avoid starting of sub-scenarios.
     */
    void halt();

    /**
     * Checks if the execution of the scenario is successful.
     *
     * @return true if the execution is successful, false otherwise.
     * @throws SafeguardException if an exception occurs during execution.
     */
    boolean isOk() throws SafeguardException;

    /**
     * @return true if current scenario execution is halted
     */
    boolean isScenarioHalted();

    /**
     * Sets the sub-scenarios for this node without redefinition of weights.
     *
     * @param newScenarios The new sub-scenarios to be added.
     */
    void setSubScenarios(_Scenario... newScenarios);

    /**
     * Sets the sub-scenarios for this node.
     *
     * @param weight       The weight of the sub-scenarios.
     * @param newScenarios The new sub-scenarios to be added.
     */
    void setSubScenarios(Fraction weight, _Scenario... newScenarios);
}
