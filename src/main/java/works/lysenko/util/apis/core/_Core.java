package works.lysenko.util.apis.core;

import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.apis.util._Dashboard;

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
}
