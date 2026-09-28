package works.lysenko.util.apis.scenario;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.data.enums.ScenarioType;
import works.lysenko.util.data.enums.Severity;

import java.util.Set;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter"})
public interface _Scenario {

    /**
     * Executes the action of the scenario.
     *
     * @throws SafeguardException if an exception occurs during the execution of the action.
     */
    void action() throws SafeguardException;

    /**
     * This routine reports an amount of possible variants of executions for
     * underlying scenarios
     *
     * @param onlyConfigured or all possible scenarios from code base
     * @return number of possible combinations for underlying scenarios
     */
    int calculateCombinations(boolean onlyConfigured);

    /**
     * @return depth of Scenario
     */
    int depth();

    /**
     * This method represents the final steps performed in a scenario's execution.
     * It should be implemented in subclasses to define custom finalization logic.
     *
     * @throws SafeguardException if an exception occurs during the finalization steps.
     */
    void finals() throws SafeguardException;

    /**
     * This should be redefined and contain the code to indicate readiness of a
     * Scenario to be executed
     *
     * @return whether this scenario meets it's prerequisites
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    boolean fits();

    /**
     * @return unique name of this Scenario
     */
    String getName();

    /**
     * @param scenario to use
     * @return name of parent of given Scenario
     */
    String getParentScenarioName(_Scenario scenario);

    /**
     * @return shortened name of this Scenario (without root part of package name)
     */
    String getShortName();

    /**
     * @return Classname shortened name of this Scenario (without root part of package name)
     */
    String getSimpleName();

    /**
     * @return very short information about
     */
    String info();

    /**
     * @return whether this scenario is executable
     */
    boolean isExecutable();

    /**
     * Probe if the current scenario is from the main scenarios tree.
     *
     * @return true if the scenario is from the main tree, false otherwise.
     */
    boolean isFromMainTree();

    /**
     * Checks if the execution of the scenario is successful.
     *
     * @return true if the execution is successful, false otherwise.
     * @throws SafeguardException if an exception occurs during the execution of a safeguard.
     */
    boolean isOk() throws SafeguardException;

    /**
     * @return Set of nested scenarios
     */
    Set<_Scenario> list();

    /**
     * Stop
     *
     * @param severity caused stop
     */
    void stopTest(Severity severity);

    /**
     * @return {@link ScenarioType} of Scenario
     */
    ScenarioType type();

    /**
     * @return the code weight as a Double v
     */
    Fraction weightCoded();

    /**
     * @return own weight of this Scenario
     */
    Fraction weightConfigured();

    /**
     * Returns cumulative weight of all parent scenarios.
     * Represents weights moving "down" the hierarchy.
     * This allows to skip definition of some weight v to each scenario of tree.
     * <p>
     * scenario.A1=1.0
     * <p>
     * This will provide downstream weight 1.0 to all "scenario.a.**" scenarios
     *
     * @return Current downstream weight of this Scenario
     */
    Fraction weightDownstream();

    /**
     * Returns cumulative weight of all child scenarios.
     * This allows to skip definition of some weight v to each parent scenario.
     * Defined weight of parent scenario will propagate up the scenarios hierarchy.
     * <p>
     * scenario.a.b.C=1.0
     * <p>
     * This will provide upstream weight 1.0 to scenarios "Scenario", "Scenario.A1" and "Scenario.a.B1"
     *
     * @return current upstream weight of this Scenario
     */
    Fraction weightUpstream();
}