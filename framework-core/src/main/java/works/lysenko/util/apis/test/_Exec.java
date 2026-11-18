package works.lysenko.util.apis.test;

import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Ctrl;
import works.lysenko.util.apis.scenario._Scenario;

import java.util.Set;

/**
 * The _Exec interface represents an object that executes scenarios.
 * It provides methods for executing scenarios, accessing the root scenarios list, and managing the scenarios.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Exec {

    /**
     * @return the scenarios
     */
    _Ctrl ctrl();

    /**
     * Sets the scenarios
     *
     * @param ctrl the scenarios to be set
     */
    void ctrl(_Ctrl ctrl);

    /**
     * Executes the scenarios.
     *
     * @throws SafeguardException if an exception occurs during the execution of a safeguard.
     */
    void exec() throws SafeguardException;

    /**
     * @return set of root Scenarios
     */
    Set<_Scenario> getRootScenariosList();
}
