package works.lysenko.util.apis.core;

import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.apis.util._Dashboard;
import works.lysenko.util.apis.scenario._Scenario;

/**
 * The ProvidesCore interface represents an object that provides core functionality for the application.
 * It contains methods to retrieve various objects and information related to the execution of the application.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Core {

    /**
     * Retrieves the dashboard object.
     *
     * @return The dashboard object.
     */
    _Dashboard getDashboard();

    /**
     * Retrieves the logger object associated with the current Core object.
     *
     * @return The logger object.
     */
    _Logs getLogger();

    /**
     * Retrieves the total number of tests associated with the core object.
     *
     * @return The total number of tests as an Integer.
     */
    Integer getTotalTests();

    /**
     * Retrieves the results associated with the current Core object.
     *
     * @return The results object containing information about test execution.
     */
    _Results getResults();

    /**
     * Retrieves the tests associated with the current Core object.
     *
     * @return The tests object.
     */
    _Test getTest();

    /**
     * @return true if execution is performed from Jar file
     */
    boolean isInJar();

    /**
     * Retrieves the reason why test execution was stopped, if any.
     *
     * @return The stop reason description, or null if execution was not stopped with an explicit reason.
     */
    String getStopReason();

    /**
     * Sets the reason why test execution was stopped.
     *
     * @param reason The stop reason description.
     */
    void setStopReason(String reason);
    /**
     * Retrieves the scenario from which the stop was initiated, if any.
     *
     * @return The scenario instance, or null if execution was not stopped by a specific scenario.
     */
    _Scenario getStopScenario();

    /**
     * Sets the scenario from which the stop was initiated.
     *
     * @param scenario The scenario instance.
     */
    void setStopScenario(_Scenario scenario);
}
