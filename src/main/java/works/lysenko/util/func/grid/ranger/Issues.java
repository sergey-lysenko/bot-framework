package works.lysenko.util.func.grid.ranger;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.apis.log._Issues;
import works.lysenko.util.data.enums.RangerResult;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.data.records.Noun;
import works.lysenko.util.data.type.list.RangerResults;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.grid.record.meta.Method;
import works.lysenko.util.grid.record.meta.ValidationMeta;
import works.lysenko.util.prop.grid.Allowed;
import works.lysenko.util.prop.grid.Ranges;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.*;
import static works.lysenko.util.chrs.___.ARE;
import static works.lysenko.util.chrs.___.TOO;
import static works.lysenko.util.chrs.____.GRID;
import static works.lysenko.util.chrs.____.LIST;
import static works.lysenko.util.data.enums.RangerResult.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.qinn;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Events.logIgnoring;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.A.AMOUNT_OF;
import static works.lysenko.util.lang.N.NOT_ENOUGH;
import static works.lysenko.util.lang.R.RANGES_IN;
import static works.lysenko.util.lang.R.REDEFINED_EXPECTED;
import static works.lysenko.util.lang.R.REDEFINED_EXPECTED_MINIMUM_IS;
import static works.lysenko.util.lang.T.TOO_FEW_EXPECTED;
import static works.lysenko.util.lang.U.UNABLE_TO;
import static works.lysenko.util.lang.U.UNABLE_TO_VALIDATE_INCORRECTLY_CONFIGURED;
import static works.lysenko.util.lang.W.WITH_NO_DEFINED_RANGES;
import static works.lysenko.util.lang.word.A.ABOVE;
import static works.lysenko.util.lang.word.A.ACTUAL;
import static works.lysenko.util.lang.word.A.AMONG;
import static works.lysenko.util.lang.word.C.CORRESPONDING;
import static works.lysenko.util.lang.word.D.DEFINED;
import static works.lysenko.util.lang.word.D.DIFFERENT;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.I.INDEX;
import static works.lysenko.util.lang.word.M.MAXIMUM;
import static works.lysenko.util.lang.word.M.MINIMUM;
import static works.lysenko.util.lang.word.M.MUCH;
import static works.lysenko.util.lang.word.R.RANGE;
import static works.lysenko.util.lang.word.S.SHARE;
import static works.lysenko.util.lang.word.S.SIZE;
import static works.lysenko.util.lang.word.T.THRESHOLD;
import static works.lysenko.util.lang.word.U.UNEXPECTED;
import static works.lysenko.util.lang.word.U.UNKNOWN;
import static works.lysenko.util.lang.word.V.VALIDATE;
import static works.lysenko.util.lang.word.W.WHILE;
import static works.lysenko.util.lang.word.W.WRONG;
import static works.lysenko.util.spec.Symbols._COLON_;
import static works.lysenko.util.spec.Symbols._COMMA_;

/**
 * Manages a collection of issues detected during validation and provides methods to log and retrieve these issues.
 *
 * @param <T> The type of the objects that are being validated.
 */
@SuppressWarnings("SetReplaceableByEnumSet")
public class Issues<T> implements _Issues<T> {

    private final ValidationMeta meta;
    private final Collection<RangerResult> issues = new HashSet<>(0);

    /**
     * Constructs an Issues object with the specified ValidationMeta.
     *
     * @param meta The ValidationMeta instance containing metadata for validation.
     */
    public Issues(final ValidationMeta meta) {

        this.meta = meta;
    }

    public final void addBorderTooHigh() {

        addFailure(meta.max(), THRESHOLD_TOO_HIGH, b(c(NO), meta.subject().noun().plural(), ARE, ABOVE, DEFINED, THRESHOLD,
                IN, q(meta.vr().name())));
    }

    public final void addNoCorrespondingShare(final boolean ignoreOther) {

        if (!ignoreOther || Ranges.logAbsentRange)
            addFailure(ignoreOther ? Severity.values()[meta.max().ordinal() + Allowed.otherSeverityReduction] : meta.max(),
                    NO_CORRESPONDING_SHARE, b(c(NO), CORRESPONDING, c(meta.subject().noun().singular()), SHARE, IN,
                            q(meta.vr().name())));
    }

    public final void addTooFewExpectations(final int size, final List<T> actualValues) {

        addFailure(meta.max(), TOO_FEW_EXPECTATIONS, b(s(size), IS, TOO_FEW_EXPECTED, meta.subject().noun().plural(), TO,
                VALIDATE, s(actualValues.size()), ACTUAL, meta.subject().noun().plural(), IN, q(meta.vr().name())));
    }

    public final void addUnexpectedActual(final T actualValue, final int index) {

        final String actualString = (actualValue instanceof Fraction) ? ts((Fraction) actualValue) : s(actualValue);
        addFailure(meta.max(), UNEXPECTED_ACTUAL, b(c(UNEXPECTED), q(actualString), AMONG, meta.subject().noun().plural(),
                AT, INDEX, s(index), IN, q(meta.vr().name())));
    }

    @SuppressWarnings({"MethodWithMultipleReturnPoints", "DataFlowIssue"})
    @Override
    public final boolean areIssuesPresent(final ValidationMeta meta, final Map<T, _ValuedRangeResult> actual,
                                          final _Quotas<T> expected, final IntegerRange amount, final Renderers renderers) {

        final Severity severity = meta.max();
        final String name = meta.vr().name();
        final Noun noun = meta.subject().noun();
        final Method method = meta.subject().method();

        if (isNull(expected.get()))
            return addFailure(severity, ABSENT_EXPECTATIONS, b(UNABLE_TO_VALIDATE_INCORRECTLY_CONFIGURED, noun.plural(), IN,
                    q(name)));

        expected.get().forEach(e -> {
            if (isNotNull(renderers) && isNotNull(renderers.ignore()) && renderers.ignore().apply(meta.vr(), e))
                logIgnoring(b(c(EXPECTED), noun.singular()), e);
        });

        if (expected.get().isEmpty())
            return addFailure(severity, EMPTY_EXPECTATIONS, b(UNABLE_TO, VALIDATE, noun.plural(), IN, q(name),
                    WITH_NO_DEFINED_RANGES));

        if (isNull(amount)) {
            if (Method.COMPULSIVE == method && expected.get().size() != actual.size())
                return addFailure(severity, UNEQUAL_ACTUAL_AND_EXPECTED, b(c(DIFFERENT), AMOUNT_OF, noun.singular(),
                        RANGES_IN, s(qinn(name, GRID), _COLON_), EXPECTED, s(expected.get().size(), _COMMA_), ACTUAL,
                        s(actual.size())));
        } else {
            if (isNull(amount.min()))
                return addFailure(severity, INVALID_MINIMUM_AMOUNT, b(c(UNKNOWN), noun.singular(), RANGE, MINIMUM, IN,
                        s(q(name))));
            if (isNull(amount.max()))
                return addFailure(severity, INVALID_MAXIMUM_AMOUNT, b(c(UNKNOWN), noun.singular(), RANGE, MAXIMUM, IN,
                        s(q(name))));
            if (expected.get().size() < amount.min())
                return addFailure(severity, LESS_THAN_MINIMUM_EXPECTATIONS, b(NOT_ENOUGH, EXPECTED, noun.singular(),
                        RANGES_IN, s(q(name), _COLON_), REDEFINED_EXPECTED_MINIMUM_IS, s(amount.min(), _COMMA_), WHILE,
                        EXPECTED, noun.plural(), LIST, SIZE, IS, s(expected.get().size())));
            if (amount.min().equals(amount.max()) && actual.size() != amount.min())
                return addFailure(severity, WRONG_ACTUAL_AMOUNT, b(c(WRONG), AMOUNT_OF, ACTUAL, noun.singular(), RANGES_IN,
                        s(q(name), _COLON_), REDEFINED_EXPECTED, s(amount.min(), _COMMA_), ACTUAL, s(actual.size())));
            if (actual.size() > amount.max())
                return addFailure(severity, TOO_MUCH_ACTUAL, b(c(TOO), MUCH, ACTUAL, noun.singular(), RANGES_IN, s(q(name),
                        _COLON_), REDEFINED_EXPECTED, MAXIMUM, s(amount.max(), _COMMA_), ACTUAL, s(actual.size())));
            if (actual.size() < amount.min())
                return addFailure(severity, TOO_FEW_ACTUAL, b(NOT_ENOUGH, ACTUAL, noun.singular(), RANGES_IN, s(q(name),
                        _COLON_), REDEFINED_EXPECTED, MINIMUM, s(amount.min(), _COMMA_), ACTUAL, s(actual.size())));
        }
        return false;
    }


    public final RangerResults get() {

        final RangerResults results = new RangerResults();
        issues.forEach(results::add);
        return results;
    }

    public final boolean isEmpty() {

        return issues.isEmpty();
    }

    /**
     * Adds a failure to the list of issues and logs an event based on the given severity and message.
     *
     * @param severity the severity of the failure
     * @param issue    the RangerResult issue to be added
     * @param msg      the message associated with the failure
     * @return true if the issue was added and the event was logged
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    private boolean addFailure(final Severity severity, final RangerResult issue, final String msg) {

        if (issues.add(issue)) logEvent(severity, msg);
        return true;
    }
}