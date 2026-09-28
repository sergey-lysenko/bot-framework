package works.lysenko.util.prop.core;

import works.lysenko.util.data.records.test.TestSafeguard;

import static works.lysenko.util.spec.PropEnum._TEST_EXECUTION_TIME_SAFEGUARD;

/**
 * Represents a Test object for handling execution time safeguard.
 */
@SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
public record Test() {

    /**
     * Represents a TestSafeguard object which contains the number of tests and milliseconds.
     */
    public static final TestSafeguard timeSafeguard = new TestSafeguard(_TEST_EXECUTION_TIME_SAFEGUARD.get());
}
