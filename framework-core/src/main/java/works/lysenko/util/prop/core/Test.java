package works.lysenko.util.prop.core;

import works.lysenko.util.data.records.test.TestSafeguard;

import static works.lysenko.util.prop.PropEnum._TEST_ALL_LEAFS;
import static works.lysenko.util.prop.PropEnum._TEST_ALL_LEAFS_EXECUTIONS;
import static works.lysenko.util.prop.PropEnum._TEST_EXECUTION_TIME_SAFEGUARD;
import static works.lysenko.util.prop.PropEnum._TEST_TESTS;

/**
 * Test execution configuration and safeguards.
 */
@SuppressWarnings({"StaticNonFinalField", "StaticMethodOnlyUsedInOneClass"})
public record Test() {

    /**
     * Configured total number of tests to execute (or {@code null} if unbounded).
     */
    public static Integer tests = _TEST_TESTS.get();

    /**
     * Indicates whether all-leafs execution mode is enabled in properties.
     */
    public static boolean allLeafs = Boolean.TRUE.equals(_TEST_ALL_LEAFS.get());

    /**
     * Configured number of executions required per leaf scenario in all-leafs mode.
     */
    public static int allLeafsExecutions = _TEST_ALL_LEAFS_EXECUTIONS.get();

    /**
     * Test safeguard defining the maximum permissible test count and execution time in milliseconds.
     */
    public static TestSafeguard timeSafeguard = new TestSafeguard(_TEST_EXECUTION_TIME_SAFEGUARD.get());

    /**
     * Refreshes static fields from current test properties.
     */
    public static void refresh() {
        tests = _TEST_TESTS.get();
        allLeafs = Boolean.TRUE.equals(_TEST_ALL_LEAFS.get());
        allLeafsExecutions = _TEST_ALL_LEAFS_EXECUTIONS.get();
        timeSafeguard = new TestSafeguard(_TEST_EXECUTION_TIME_SAFEGUARD.get());
    }
}
