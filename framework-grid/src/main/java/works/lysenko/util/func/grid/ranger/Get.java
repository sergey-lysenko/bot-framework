package works.lysenko.util.func.grid.ranger;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.range.Quota;
import works.lysenko.util.grid.record.meta.ValidationMeta;
import works.lysenko.util.prop.grid.Allowed;
import works.lysenko.util.prop.grid.Ranges;

import static works.lysenko.Base.exec;
import static works.lysenko.util.chrs.__.AT;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.FOR;
import static works.lysenko.util.chrs.____.FIND;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Severity.S3;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.core.Events.logGenericFailure;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.U.UNABLE_TO;
import static works.lysenko.util.lang.word.A.ALLOWED;
import static works.lysenko.util.lang.word.A.AMONG;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.G.GIVEN;
import static works.lysenko.util.lang.word.I.INDEX;
import static works.lysenko.util.lang.word.I.INSTEAD;
import static works.lysenko.util.lang.word.R.RANGE;

/**
 * The Get class provides utility methods for retrieving and validating data.
 */
@SuppressWarnings("MethodWithMultipleReturnPoints")
public record Get() {

    /**
     * Determines the corresponding share of a given value from a set of expected quotas based on its validation metadata.
     *
     * @param <T>         The type of the value and share being processed.
     * @param meta        The validation metadata containing details like severity, subject, and contextual information.
     * @param expected    The collection of expected quotas to match against.
     * @param value       The actual value being validated.
     * @param quota       The expected share that the actual value should correspond to.
     * @param index       The index of the current quota being processed within the validation sequence.
     * @param ignoreOther A flag indicating whether shares outside the specified range should be ignored.
     * @return The corresponding quota from the expected set that matches the actual value, or a fallback quota if mismatched.
     */
    @SuppressWarnings("unchecked")
    public static <T> Quota<T> substitute(final ValidationMeta meta, final _Quotas<T> expected,
                                          final T value, final Quota<? super T> quota, final int index,
                                          final boolean ignoreOther) {

        final Quota<T> the;
        log(meta, value, quota, index);
        the = (Quota<T>) getSubstitute(meta, expected, value, ignoreOther);
        return the;
    }

    private static <T> void log(final ValidationMeta meta, final T actualValue, final Quota<T> quota, final int index) {

        final String actualString = (actualValue instanceof Fraction) ? ts((Fraction) actualValue) : s(actualValue);
        if (Ranges.logOrderChange)
            exec.logEvent(S3, b(c(meta.subject().noun().singular()), q(actualString), INSTEAD, OF, EXPECTED,
                    s(quota.value()), AT, INDEX, s(index), IS, ALLOWED));
    }

    /**
     * Retrieves a specific share from the provided list of quotas that matches the given value.
     * If no matching share is found, logs a generic failure with the appropriate severity level.
     *
     * @param meta        The validation metadata containing details like severity and subject.
     * @param quotas      The list of quotas to search through.
     * @param value       The value to match within the quotas.
     * @param ignoreOther A flag indicating whether the severity level should account for other considerations.
     * @return The matching share from the list of quotas, or null if no match is found.
     */
    private static _Quota<?> getSubstitute(final ValidationMeta meta, final _Quotas<?> quotas, final Object value,
                                           final boolean ignoreOther) {

        for (final _Quota<?> quota : quotas.get()) {
            if (quota.value().equals(value)) return quota;
            if (quotas.isWithinPrecision(value, quota)) return quota;
        }
        final Severity severity = ignoreOther ? Severity.values()[meta.max().ordinal() + Allowed.otherSeverityReduction] :
                meta.max();
        if (!ignoreOther || Ranges.logAbsentRange)
            logGenericFailure(severity, b(UNABLE_TO, FIND, RANGE, FOR, VALUE, q(s(value)), AMONG, GIVEN,
                    meta.subject().noun().plural()));
        return null;
    }
}
