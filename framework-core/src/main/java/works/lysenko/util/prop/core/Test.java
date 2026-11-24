package works.lysenko.util.prop.core;

import works.lysenko.util.data.records.test.TestSafeguard;

import static works.lysenko.util.prop.PropEnum._TEST_EXECUTION_TIME_SAFEGUARD;
import static works.lysenko.util.prop.PropEnum._TEST_EXHAUSTIVE;
import static works.lysenko.util.prop.PropEnum._TEST_EXHAUSTIVE_PASSES;
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
     * Indicates whether exhaustive execution mode is enabled in properties.
     */
    public static boolean exhaustive = Boolean.TRUE.equals(_TEST_EXHAUSTIVE.get());

    /**
     * Configured number of passes required per leaf scenario in exhaustive mode.
     */
    public static int exhaustivePasses = _TEST_EXHAUSTIVE_PASSES.get();

    /**
     * Test safeguard defining the maximum permissible test count and execution time in milliseconds.
     */
    public static TestSafeguard timeSafeguard = new TestSafeguard(_TEST_EXECUTION_TIME_SAFEGUARD.get());

    /**
     * Refreshes static fields from current test properties.
     */
    public static void refresh() {
        tests = _TEST_TESTS.get();
        exhaustive = Boolean.TRUE.equals(_TEST_EXHAUSTIVE.get());
        exhaustivePasses = _TEST_EXHAUSTIVE_PASSES.get();
        timeSafeguard = new TestSafeguard(_TEST_EXECUTION_TIME_SAFEGUARD.get());
    }
}
