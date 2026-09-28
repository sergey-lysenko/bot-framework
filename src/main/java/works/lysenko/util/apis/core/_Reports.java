package works.lysenko.util.apis.core;

/**
 * The _Reports interface provides a method for calculating and displaying various statistics related to scenarios and issues.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
@FunctionalInterface
public interface _Reports {

    /**
     * The run method executes a series of tasks to generate and display various statistics and reports.
     * This method calls the run method of several other classes to perform specific tasks.
     * The order of execution is as follows:
     * 1. NewIssues#run(): Generate statistics for new issues.
     * 2. KnownIssues#run(): Generate statistics for known issues.
     * 3. Test#run(): Generate statistics for tests.
     * 4. Scenarios#run(): Generate statistics for scenarios.
     * 5. ResultMarker#run(): Generate statistics for result markers.
     */
    void run();
}
