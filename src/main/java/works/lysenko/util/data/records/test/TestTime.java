package works.lysenko.util.data.records.test;

/**
 * Represents the execution time associated with a specific test.
 *
 * @param test The identifier or name of the test.
 * @param time The duration or timestamp associated with the test execution.
 */
@SuppressWarnings("ClassNamePrefixedWithPackageName")
public record TestTime(String test, String time) {}
