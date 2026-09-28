package works.lysenko.util.apis.test;

import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Scenario;

import java.util.List;

/**
 * The _Repeater interface defines the contract for managing and processing test scenarios, test history,
 * and safeguards in a testing framework. It provides methods for adding scenarios, retrieving test
 * information, and processing test execution.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Repeater {

    /**
     * Adds a scenario to the current test history.
     *
     * @param scenario the scenario to add to the current test history
     * @throws SafeguardException if an exception occurs during the process
     */
    void addToHistory(_Scenario scenario) throws SafeguardException;

    /**
     * Add a scenario to current test history
     *
     * @param scenario to add
     */
    void addToScenarios(_Scenario scenario);

    /**
     * Closes the current test.
     * <p>
     * This method performs various actions to close the current test, including processing the current scenario,
     * updating the test history, processing the test time, processing test safeguards, and signaling that the test is done.
     * If there are any failing events, the scenario processing is skipped.
     *
     * @see _Repeater#run()
     */
    void close();

    /**
     * @return Current Test number
     */
    Integer getCurrent();

    /**
     * Returns the history of tests.
     * <p>
     * The getCyclesHistory method returns a list of lists containing the history of executed tests.
     * Each inner list represents a single test, and each element in the inner list represents a scenario executed during
     * that test.
     *
     * @return A1 list of lists containing the history of tests.
     * Each inner list represents a single test, and each element in the inner list represents a scenario executed during
     * that test.
     */
    List<List<String>> getHistory();

    /**
     * @return tests summary
     */
    List<List<String>> getSummary();

    /**
     * Retrieves the total number of tests to be executed.
     *
     * @return the total number of tests as an Integer
     */
    Integer getTotalTests();

    /**
     * Executes the run method.
     *
     * @throws SafeguardException if an exception occurs during the execution of the run method
     */
    void run() throws SafeguardException;
}