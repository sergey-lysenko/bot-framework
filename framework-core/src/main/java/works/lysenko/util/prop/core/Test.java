package works.lysenko.util.prop.core;

import works.lysenko.util.data.records.test.TestSafeguard;

import static works.lysenko.util.prop.PropEnum._TEST_EXECUTION_TIME_SAFEGUARD;

/**
 * Test execution safeguards configuration.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Test() {

    /**
     * Test safeguard defining the maximum permissible test count and execution time in milliseconds.
     */
    public static final TestSafeguard timeSafeguard = new TestSafeguard(_TEST_EXECUTION_TIME_SAFEGUARD.get());
}
