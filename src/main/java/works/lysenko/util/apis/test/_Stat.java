package works.lysenko.util.apis.test;

import java.util.List;

/**
 * The CollectsStatistic interface defines the methods for collecting and retrieving statistics related to tests.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "unused"})
public interface _Stat {

    /**
     * Retrieves the history of test tests.
     *
     * @return a list of lists, where each inner list represents a test and contains the history entries for that test.
     */
    List<List<String>> history();


    /**
     * Retrieves the previous test entries.
     *
     * @return a list of strings representing the previous test entries.
     */
    List<String> previous();

    /**
     * Retrieves the summary of test tests.
     *
     * @return a list of lists representing the summary of test tests. Each inner list contains the history entries for a test.
     */
    List<List<String>> summary();

    /**
     * Retrieves the times of the tests.
     *
     * @return a list of Long values representing the times of the tests
     */
    List<Long> times();
}
