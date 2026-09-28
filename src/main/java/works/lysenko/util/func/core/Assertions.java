package works.lysenko.util.func.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.strs.Wrap;

import java.util.Arrays;

import static java.lang.Math.abs;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.exec;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.___.AND;
import static works.lysenko.util.chrs.___.ARE;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.chrs.____.CODE;
import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.chrs.____.TRUE;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Ansi.bb;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.Brackets.CURLY;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Bind.bn;
import static works.lysenko.util.data.strs.Bind.d;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.sn;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.A.ASSERTING_THAT_ACTUAL;
import static works.lysenko.util.lang.A.ASSERTING_THAT_VALUE;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.D.DUE_TO;
import static works.lysenko.util.lang.E.EQUALS_TO_EXPECTED;
import static works.lysenko.util.lang.N.NOT_EQUAL_TO_EXPECTED;
import static works.lysenko.util.lang.V.VALUES_ARE_NOT_EQUAL;
import static works.lysenko.util.lang.word.E.EQUAL;
import static works.lysenko.util.lang.word.F.FAILING;
import static works.lysenko.util.lang.word.F.FAILURE;
import static works.lysenko.util.lang.word.F.FALSE;
import static works.lysenko.util.lang.word.I.IMPLEMENTED;
import static works.lysenko.util.lang.word.I.INDUCED;
import static works.lysenko.util.lang.T.TODO_;
import static works.lysenko.util.lang.word.V.VALUES;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._LFD_;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * Simple Assertions
 */
@SuppressWarnings({"ClassWithTooManyMethods", "StaticMethodOnlyUsedInOneClass", "OverloadedMethodsWithSameNumberOfParameters"
        , "unused"})
public record Assertions() {

    private static final double EPSILON = 1.0E-10; // Tolerance.

    /**
     * @param expected     Boolean value
     * @param actual       Boolean value
     * @param errorMessage in case of difference
     */
    public static void assertEquals(final Boolean expected, final Boolean actual, final String errorMessage) {

        assertLog(b(ASSERTING_THAT_ACTUAL, q(yb(actual)), EQUALS_TO_EXPECTED, q(bb(expected))));
        if (expected != actual) failAssertion(errorMessage, VALUES_ARE_NOT_EQUAL, expected, actual);
    }

    /**
     * @param expected     double value
     * @param actual       double value
     * @param errorMessage in case of difference
     */
    public static void assertEquals(final double expected, final double actual, final String errorMessage) {

        assertLog(b(ASSERTING_THAT_ACTUAL, q(yb(actual)), EQUALS_TO_EXPECTED, q(bb(expected))));
        if (EPSILON < Math.abs(expected - actual)) // Compare with value.
            failAssertion(errorMessage, VALUES_ARE_NOT_EQUAL, expected, actual);
    }

    /**
     * @param expected     int value
     * @param actual       int value
     * @param errorMessage in case of difference
     */
    public static void assertEquals(final long expected, final long actual, final String errorMessage) {

        assertLog(b(ASSERTING_THAT_ACTUAL, q(yb(actual)), EQUALS_TO_EXPECTED, q(bb(expected))));
        if (expected != actual) failAssertion(errorMessage, VALUES_ARE_NOT_EQUAL, expected, actual);
    }

    /**
     * @param expected     int value
     * @param actual       int value
     * @param errorMessage in case of difference
     */
    public static void assertEquals(final int expected, final int actual, final String errorMessage) {

        assertLog(b(ASSERTING_THAT_ACTUAL, q(yb(actual)), EQUALS_TO_EXPECTED, q(bb(expected))));
        if (expected != actual) failAssertion(errorMessage, VALUES_ARE_NOT_EQUAL, expected, actual);
    }

    /**
     * @param expected     String value
     * @param actual       String value
     * @param errorMessage in case of difference
     */
    @SuppressWarnings("CallToSuspiciousStringMethod")
    public static void assertEquals(final String expected, final String actual, final String errorMessage) {

        assertLog(b(ASSERTING_THAT_ACTUAL, q(yb(actual)), EQUALS_TO_EXPECTED, q(bb(expected))));
        if (!expected.equals(actual)) failAssertion(errorMessage, VALUES_ARE_NOT_EQUAL, expected, actual);
    }

    /**
     * Asserts that two floats are equal within a given value.
     * If the difference between the expected and actual values is greater
     * than the given epsilon, an assertion error is thrown with the
     * specified error message.
     *
     * @param expected     the expected float value
     * @param actual       the actual float value
     * @param epsilon      the value within which the values are considered equal
     * @param errorMessage the message to be included in the assertion error
     * @throws AssertionError if the actual value is not equal to the expected value within the specified value
     */
    public static void assertEquals(final float expected, final float actual, final float epsilon, final String errorMessage) {

        if (Math.abs(expected - actual) > epsilon)
            failAssertion(errorMessage, VALUES_ARE_NOT_EQUAL, expected, actual, epsilon);
    }

    /**
     * Asserts that two Fraction values are equal within a given epsilon.
     * If the difference between the expected and actual values is greater than epsilon,
     * an assertion error is thrown with the specified error message.
     *
     * @param expected     the expected Fraction value
     * @param actual       the actual Fraction value
     * @param epsilon      the value within which the values are considered equal
     * @param errorMessage the message to be included in the assertion error
     */
    public static void assertEquals(final Fraction expected, final Fraction actual, final Fraction epsilon,
                                    final String errorMessage) {

        if (Math.abs(expected.doubleValue() - actual.doubleValue()) > epsilon.doubleValue())
            failAssertion(errorMessage, VALUES_ARE_NOT_EQUAL, expected, actual, epsilon);
    }

    /**
     * @param expected     int value
     * @param actual       int value
     * @param errorMessage in case of difference
     */
    public static void assertEqualsSilent(final long expected, final long actual, final String errorMessage) {

        if (expected != actual) failAssertion(errorMessage, VALUES_ARE_NOT_EQUAL, expected, actual);
    }

    /**
     * @param expected     int value
     * @param actual       int value
     * @param errorMessage in case of difference
     */
    public static void assertEqualsSilent(final int expected, final int actual, final String errorMessage) {

        if (expected != actual) failAssertion(errorMessage, VALUES_ARE_NOT_EQUAL, expected, actual);
    }

    /**
     * @param expected     String value
     * @param actual       String value
     * @param errorMessage in case of difference
     */
    @SuppressWarnings("CallToSuspiciousStringMethod")
    public static void assertEqualsSilent(final String expected, final String actual, final String errorMessage) {

        if (!expected.equals(actual)) failAssertion(errorMessage, VALUES_ARE_NOT_EQUAL, expected, actual);
    }

    /**
     * @param expected     double value
     * @param actual       double value
     * @param errorMessage in case of difference
     */
    public static void assertEqualsSilent(final double expected, final double actual, final String errorMessage) {

        if (EPSILON < Math.abs(expected - actual)) // Compare with value.
            failAssertion(errorMessage, VALUES_ARE_NOT_EQUAL, expected, actual);
    }

    /**
     * Asserts that two double values are equal within a certain value and logs an event if they are not.
     *
     * @param expected the expected double value
     * @param actual   the actual double value
     * @param severity the severity of the logged event
     * @param message  the message to be included in the logged event
     */
    public static void assertEqualsSilentEvent(final double expected, final double actual, final Severity severity,
                                               final String message) {

        if (EPSILON < Math.abs(expected - actual)) // Compare with value.
            logEvent(severity, b(s(message, _COLON_), b(c(VALUES), q(s(expected)), AND, q(s(actual)), ARE, NOT, EQUAL)));
    }

    /**
     * @param asserted     value
     * @param errorMessage if true
     */
    public static void assertFalse(final boolean asserted, final String errorMessage) {

        assertLog(b(ASSERTING_THAT_VALUE, q(yb(asserted)), IS, FALSE));
        if (asserted) failAssertion(errorMessage, b(c(VALUE), IS, TRUE));
    }

    private static void assertLog(final String message) {

        if (isNotNull(exec)) logDebug(message, true);
    }

    /**
     * @param sampled      String value
     * @param reference    String value
     * @param errorMessage in case of no difference
     */
    @SuppressWarnings("CallToSuspiciousStringMethod")
    public static void assertNotEquals(final String reference, final String sampled, final String errorMessage) {

        assertLog(bn(ASSERTING_THAT_ACTUAL, q(yb(sampled)), NOT_EQUAL_TO_EXPECTED, q(bb(reference))));
        if (sampled.equals(reference)) failAssertion(errorMessage, b(c(VALUES), ARE, EQUAL), sampled, reference);
    }

    /**
     * @param sampled      int value
     * @param reference    int value
     * @param errorMessage in case of no difference
     */
    public static void assertNotEquals(final int sampled, final int reference, final String errorMessage) {

        assertLog(b(ASSERTING_THAT_ACTUAL, q(yb(sampled)), NOT_EQUAL_TO_EXPECTED, q(bb(reference))));
        if (sampled == reference) failAssertion(errorMessage, b(c(VALUES), ARE, EQUAL), sampled, reference);
    }

    /**
     * @param sampled      String value
     * @param reference    String value
     * @param errorMessage in case of no difference
     */
    @SuppressWarnings("CallToSuspiciousStringMethod")
    public static void assertNotEqualsSilent(final String reference, final String sampled, final String errorMessage) {

        if (sampled.equals(reference)) failAssertion(errorMessage, b(c(VALUES), ARE, EQUAL), sampled, reference);
    }

    /**
     * @param asserted     value
     * @param errorMessage if null
     */
    public static void assertNotNull(final Object asserted, final String errorMessage) {

        assertLog(b(ASSERTING_THAT_VALUE, q(yb(s(asserted))), IS, NOT, NULL));
        if (isNull(asserted)) failAssertion(errorMessage, b(c(VALUE), IS, NULL));
    }

    /**
     * @param asserted     value
     * @param errorMessage if null
     */
    public static void assertNotNullSilent(final Object asserted, final String errorMessage) {

        if (isNull(asserted)) failAssertion(errorMessage, b(c(VALUE), IS, NULL));
    }

    /**
     * @param asserted     value
     * @param errorMessage if not null
     */
    public static void assertNull(final Object asserted, final String errorMessage) {

        assertLog(b(ASSERTING_THAT_VALUE, q(yb(sn(asserted))), IS, NULL));
        if (isNotNull(asserted)) failAssertion(errorMessage, b(c(VALUE), IS, NOT, NULL));
    }

    /**
     * @param asserted     value
     * @param errorMessage if false
     */
    public static void assertTrue(final boolean asserted, final String errorMessage) {

        assertLog(b(ASSERTING_THAT_VALUE, q(yb(asserted)), IS, TRUE));
        if (!asserted) failAssertion(errorMessage, b(c(VALUE), IS, FALSE));
    }

    /**
     * @param asserted     value
     * @param errorMessage if false
     */
    public static void assertTrueSilent(final boolean asserted, final String errorMessage) {

        if (!asserted) failAssertion(errorMessage, b(c(VALUE), IS, FALSE));
    }

    /**
     * Asserts that the actual Fraction value is within a certain fractionTolerance of the expected Fraction value.
     *
     * @param expected     The expected Fraction value.
     * @param actual       The actual Fraction value.
     * @param epsilon      The fractionTolerance value, as a Fraction, indicating the maximum acceptable difference between
     *                     the expected and actual values.
     * @param errorMessage The error message to be displayed if the assertion fails.
     */
    @SuppressWarnings("SameParameterValue")
    public static void assertWithTolerance(final Fraction expected, final Fraction actual, final Fraction epsilon,
                                           final String errorMessage) {

        if (abs(expected.doubleValue() - actual.doubleValue()) > epsilon.doubleValue())
            failAssertion(errorMessage, VALUES_ARE_NOT_EQUAL, ts(expected), ts(actual), ts(epsilon));
    }

    /**
     * @param errorMessage reason of failure
     */
    public static void fail(final String errorMessage) {

        assertLog(b(c(FAILING), DUE_TO, q(errorMessage)));
        failAssertion(errorMessage, b(d(c(CODE), INDUCED), FAILURE));
    }

    /**
     * Fails an assertion by constructing and logging a message based on the provided parameters.
     * If assertions are enabled, throws an AssertionError with the constructed message.
     *
     * @param message the base message describing the assertion failure
     * @param problem a specific problem or issue that occurred, can be null
     * @param values  optional additional values related to the problem, used for detailed error reporting
     */
    @SuppressWarnings({"NestedConditionalExpression", "WeakerAccess"})
    public static void failAssertion(final String message, final String problem, final Object... values) {

        final String s = b(message,
                e(CURLY,
                        e(_LFD_,
                                (null == problem) ? EMPTY : (0 < values.length) ? b(s(problem, _COLON_),
                                        s(_COLON_, _SPACE_,
                                                StringUtils.join(
                                                        Arrays.stream(values)
                                                                .map(Wrap::q)
                                                                .toList(),
                                                        s(COMMA_SPACE)
                                                )
                                        )
                                ) : b(problem)
                        )
                )
        );
        if (isNull(exec)) еггог(s);
        else {
            logEvent(Severity.S0, s);
            if (works.lysenko.util.prop.core.Assertions.exception) еггог(s);
        }
    }

    /**
     * Logs an event indicating a feature or functionality has not yet been implemented.
     *
     * @param severity the level of severity to log, indicating the importance or impact of the unimplemented feature
     * @param message  a descriptive message providing additional context or details about the unimplemented feature
     */
    @SuppressWarnings("WeakerAccess")
    public static void notImplemented(final Severity severity, final String message) {

        logEvent(severity, b(TODO_, message));
    }

    /**
     * This alias should be used during test development to optionally break test execution at required places
     *
     * @param stop the test or not
     */
    @SuppressWarnings("WeakerAccess")
    public static void notImplemented(final boolean stop) {

        final String message = b(c(NOT), IMPLEMENTED);
        if (stop) fail(message);
        else еггог(message);
    }

    /**
     * Prints an error message to the standard error stream.
     *
     * @param message the error message to be printed
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void еггог(final String message) {

        System.err.println(message);
    }

    /**
     * This alias should be used during test development to break test execution at required places
     */
    public static void notImplemented() {

        notImplemented(true);
    }
}
