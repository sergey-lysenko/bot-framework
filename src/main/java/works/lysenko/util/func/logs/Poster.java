package works.lysenko.util.func.logs;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.apis.grid.t._Value;
import works.lysenko.util.data.range.FractionRange;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.func.logs.poster.Failure;
import works.lysenko.util.func.type.Fractions;
import works.lysenko.util.func.type.fractions.Render;
import works.lysenko.util.prop.core.Stop;
import works.lysenko.util.prop.grid.Ranges;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.___.NOT;
import static works.lysenko.util.chrs.____.THAN;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.d;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.word.A.ALLOWED;
import static works.lysenko.util.lang.word.B.BIGGER;
import static works.lysenko.util.lang.word.D.DEFINED;
import static works.lysenko.util.lang.word.E.EQUALS;
import static works.lysenko.util.lang.word.F.FAILED;
import static works.lysenko.util.lang.word.M.MARGIN;
import static works.lysenko.util.lang.word.S.SMALLER;
import static works.lysenko.util.lang.word.V.VALIDATION;
import static works.lysenko.util.lang.word.W.WITHIN;
import static works.lysenko.util.spec.Numbers.THREE;
import static works.lysenko.util.spec.Numbers.TWO;
import static works.lysenko.util.spec.Symbols._PRCNT_;

/**
 * Represents a poster for logging failure events with detailed information.
 */
@SuppressWarnings("MethodWithMultipleReturnPoints")
record Poster() {

    /**
     * Appends a string based on the comparison between the expectation and the actual value.
     *
     * @param s       The original string to append to.
     * @param failure The Failure object containing information about the failure event.
     * @param value   The value used for comparison.
     * @return The appended string based on the comparison result.
     */
    @SuppressWarnings({"CallToSuspiciousStringMethod", "DataFlowIssue"})
    private static String append(final String s, final Failure failure, final Fraction value) {

        final String expectation = getExpectation(failure);
        return expectation.equals(ts(true, value)) ? s : b(s, ts(true, value), OF);
    }

    /**
     * Determines a comparison string based on the relationship between actual and expected values within a failure event.
     *
     * @param failure The Failure object containing information about the failure event, including the expected and actual
     *                values.
     * @return A1 string describing the comparison, such as "equals", "within", "bigger than", or "smaller than", according
     * to the margin and expected value type.
     */
    private static String getComparison(final Failure failure) {

        String s = null;
        final boolean isSingle =
                (failure.expected().value() instanceof Integer) || (failure.expected().value() instanceof Fraction);
        final boolean isRange =
                (failure.expected().value() instanceof IntegerRange) || (failure.expected().value() instanceof FractionRange);
        final Fraction margin = margin(failure);

        if (isNotNull(margin)) {
            if (0.0 == margin.doubleValue()) {
                if (isSingle) s = b(EQUALS);
                if (isRange) s = b(WITHIN);
            } else {
                if (0.0 < margin.doubleValue()) {
                    s = b(BIGGER, THAN);
                    if (isRange) s = append(s, failure, (Fraction) ((_Range<?>) failure.expected().value()).max());
                }
                if (0.0 > margin.doubleValue()) {
                    s = b(SMALLER, THAN);
                    if (isRange) s = append(s, failure, (Fraction) ((_Range<?>) failure.expected().value()).min());
                }
            }
        }
        return s;
    }

    /**
     * Constructs and returns a description string for a given failure event.
     * The description includes the actual value, a comparison indicator, and the expected value.
     *
     * @param failure The Failure object containing information about the failure event.
     * @return A1 formatted string describing the failure, constructed from the actual value, comparison string, and
     * expected value.
     */
    private static String getDescription(final Failure failure) {

        final String expectation = getExpectation(failure);
        return b(VALUE, q(failure.actual().render()), IS, getComparison(failure), s(expectation));
    }

    @SuppressWarnings("ChainOfInstanceofChecks")
    private static String getExpectation(final Failure failure) {

        final _Value<?> expected = failure.expected();
        if (expected.value() instanceof Integer) return s(expected.value());
        if (expected.value() instanceof Fraction) return Render.toString((Fraction) expected.value());
        if (expected.value() instanceof IntegerRange) return (expected.value()).toString();
        if (expected.value() instanceof FractionRange) return (expected.value()).toString();
        return null;
    }

    @SuppressWarnings("CallToSuspiciousStringMethod")
    private static String getIntro(final Failure failure) {

        final String forS = (failure.description().substring(0, 3).equals(e(IN, true))) ?
                StringUtils.EMPTY : b(FOR, s(failure.description()));
        return b(false, c(failure.dataType()), VALIDATION, FAILED, forS);
    }

    /**
     * Calculates and returns the deviation between the actual value and the expected value of a failure event.
     *
     * @param failure The failure event containing the actual and expected values.
     * @return The deviation in percentage form or a message indicating the deviation status.
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    private static String get_dVstring(final Failure failure) {

        final Fraction actual = margin(failure);
        final Fraction allowed = failure.allowed_dV();
        if (isNull(actual)) return null;
        else {
            if (isNotNull(allowed)) {
                if (allowed.equals(Fraction.ZERO)) return b(MARGIN, ts(true, actual), IS, NOT, ALLOWED);
                else {
                    if (actual.equals(Fraction.ZERO)) return b(NO, MARGIN);
                    final String percentage = s(d(actual.divide(allowed).percentageValue(), TWO), _PRCNT_);
                    return b(MARGIN, ts(true, actual), IS, percentage, OF, ALLOWED, ts(true, allowed));
                }
            } else return b(ALLOWED, MARGIN, IS, NOT, DEFINED);
        }
    }

    /**
     * Logs a failure event with the specified information.
     *
     * @param failure The Failure object containing information about the failure event, including severity, data type,
     *                description, actual value, expected value, and allowed deviation.
     */
    static void logFailure(final Failure failure) {

        if (Stop.atSeverity == failure.severity().ordinal() || Ranges.logNonFailingOutOfBounds) {
            final Collection<String> messages = new ArrayList<>(THREE);
            messages.addAll(List.of(getIntro(failure), getDescription(failure)));
            if (isNotNull(get_dVstring(failure))) messages.add(get_dVstring(failure));
            logEvent(failure.severity(), b(StringUtils.joinWith(COMMA_SPACE, messages.toArray())));
        }
    }

    /**
     * Calculates and returns the margin of deviation between the expected and actual values from a failure event.
     *
     * @param failure The Failure object containing information about the failure event, including the expected and actual
     *                values.
     * @return The calculated margin as a Fraction, or null if the value types are unsupported.
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "DataFlowIssue", "ChainOfInstanceofChecks"})
    private static Fraction margin(final Failure failure) {

        final _Value<?> expected = failure.expected();
        if (expected.value() instanceof Integer)
            return Fractions.dV(fr((Integer) expected.value()), fr((Integer) failure.actual().value()));
        if (expected.value() instanceof Fraction)
            return Fractions.dV((Fraction) expected.value(), (Fraction) failure.actual().value());
        if (expected.value() instanceof IntegerRange)
            return fr(((IntegerRange) expected.value()).margin((Integer) failure.actual().value()));
        if (expected.value() instanceof FractionRange)
            return ((FractionRange) expected.value()).margin((Fraction) failure.actual().value());
        return null;
    }
}
