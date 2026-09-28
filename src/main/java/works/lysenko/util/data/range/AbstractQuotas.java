package works.lysenko.util.data.range;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.Base;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.data.type.Value;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.prop.grid.Ranges;

import java.util.*;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.isDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.___.MAX;
import static works.lysenko.util.chrs.___.MIN;
import static works.lysenko.util.chrs.____.NULL;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.enums.Severity.S1;
import static works.lysenko.util.data.enums.Severity.S2;
import static works.lysenko.util.data.range.Quota.shareOfInteger;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.data.Percents.percentString;
import static works.lysenko.util.func.data.Regexes.l0;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.I.IS_DEFINED_MORE_THAN___;
import static works.lysenko.util.lang.I.IS_NOT_AN_INTEGER_IN___;
import static works.lysenko.util.lang.T.THE_SORTED___;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.prop.data.Delimeters.L1;
import static works.lysenko.util.prop.data.Delimeters.L2;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.TWO;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._0_;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._PLUS_;

/**
 * The {@code AbstractQuotas} class provides a base structure for handling quota-related functionalities.
 * It defines the upper and lower limits for quota fractions as constants and offers methods for
 * parsing, processing, and validating quota distributions.
 * <p>
 * A1 range is represented by a ShareOfIntValue object, which contains information about the value, minimum, and maximum.
 * The ranges are stored in a List.
 * <p>
 * This class provides constructors for creating AbstractRanges objects from a List of ShareOfIntValue objects, or from a
 * string representation.
 * <p>
 * The string representation can have several formats:
 * - {element}:{min}:{max},...
 * - {element}:{max}:{min},...
 * - {element}:{min}+,... (more than, short for {element}:{min}:1.0)
 * - {element}:{max}-,... (less than, short for {element}:{max}:0.0)
 * - {element}:{expected},... (very strict requirement)
 * <p>
 * The AbstractQuotas class also provides methods for retrieving the list of ranges, logging the ranges, and calculating
 * statistics.
 */
@SuppressWarnings({"ConstantDeclaredInAbstractClass", "WeakerAccess", "rawtypes", "ProtectedField", "unused"})
public abstract class AbstractQuotas implements _Quotas {

    /**
     * Represents the upper limit for a Fraction value.
     */
    public static final Fraction UPPER_LIMIT = Fraction.ONE;

    /**
     * Represents the lower limit for a Fraction value.
     */
    public static final Fraction LOWER_LIMIT = Fraction.ZERO;
    private final String type;
    protected List<_Quota<?>> quotas;

    /**
     * Initializes an instance of AbstractRanges with a list of ShareOfIntValue ranges.
     *
     * @param quotas the list of ShareOfIntValue ranges representing the expected ranges of Integer share values
     */
    @SuppressWarnings({"ConstructorNotProtectedInAbstractClass", "AssignmentOrReturnOfFieldWithMutableType"})
    public AbstractQuotas(final List<_Quota<?>> quotas) {

        this.quotas = quotas;
        type = null;
    }

    /**
     * Parses ExpectedDistribution from string.
     * <p>
     * {element},{element},...
     * <p>
     * {element} can have several forms:
     * - {value}|{min}|{max},...
     * - {value}|{max}|{min},...
     * - {value}|{expected}+,... more then, short for {value}|{expected}|1.0
     * - {value}|{expected}-,... less then, short for {value}|{expected}|0.0
     * - {value}|{expected},... (very strict requirement)
     *
     * @param source The source string in format {value}|{min}|{max},...
     * @param origin The origin string.
     * @param type   The type of data.
     * @param silent A1 boolean indicating whether to log debug information.
     */
    @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
    public AbstractQuotas(final String source, final String origin, final String type, final boolean silent) {

        this.type = type;
        final List<_Quota<?>> candidates;
        if (isNull(source)) candidates = null;
        else candidates = generate(source, origin, type, silent);
        quotas = candidates;
    }

    /**
     * Adds specific key-value pairs to the provided list based on the given fraction range and updates
     * the candidate collection using the value and precision.
     *
     * @param list       the collection where key-value pairs will be added
     * @param fractions  the fraction range from which minimum and maximum values are extracted
     * @param candidates the collection of Quota objects to be updated based on the value and precision
     * @param value      the integer value used for calculations and updating the candidates
     * @param precision  the precision for calculations when updating candidates
     */
    private static void add(final Collection<? super KeyValue> list, final FractionRange fractions, final Collection<?
            super Quota<Integer>> candidates, final Integer value, final Integer precision, final String stamp) {

        list.add(kv(MIN, ts(fractions.min())));
        list.add(kv(MAX, ts(fractions.max())));
        if (isNotNull(candidates)) candidates.add(shareOfInteger(value, precision, stamp, fractions.min(), fractions.max()));
    }

    /**
     * Generates a list of ShareOfIntValue objects based on the given source, type, and value.
     *
     * @param source The source string in format {value}|{min}|{max},...
     * @param type   The type of data.
     * @return The generated list of ShareOfIntValue objects.
     */
    @SuppressWarnings("CallToSuspiciousStringMethod")
    private static List<_Quota<?>> generate(final String source, final String origin, final String type,
                                            final boolean silent) {

        List<_Quota<?>> candidates;
        final String[] array = source.trim().split(s(L1));
        candidates = new ArrayList<>(array.length);
        for (final String item : array) candidates = test(origin, type, item, candidates, silent);
        return candidates;
    }

    /**
     * Determines if the specified value is a duplicate in the given collection of ShareOfIntValue objects.
     *
     * @param value The value to check for duplicates.
     * @param in    The collection of ShareOfIntValue objects.
     * @return true if the value is a duplicate, false otherwise.
     */
    private static boolean isDuplicate(final Integer value, final Iterable<? extends _Quota<?>> in) {

        boolean result = false;
        for (final _Quota share : in) result = result || (Objects.equals(share.value(), value));
        return result;
    }

    /**
     * This method processes the given values and updates the list of candidates based on the type, value, and values.
     * It adds a KeyValue object to the list and updates the candidates' list if necessary.
     *
     * @param type       The type of data.
     * @param values     The array of values used for creating fractions.
     * @param candidates The list of candidates to be updated.
     * @param list       The collection of KeyValue objects.
     * @return The updated list of candidates.
     */
    @SuppressWarnings("MethodWithTooManyParameters")
    private static List<_Quota<?>> process(final String origin, final String type, final String[] values, final List<_Quota<
            ?>> candidates, final Collection<? super KeyValue> list) {

        List<_Quota<?>> newCandidates = candidates;
        final Value value = validateValue(values[ZERO], newCandidates, origin);

        if (!isAnyNull(newCandidates, value)) {
            list.add(kv(c(type), value.value()));
            if (ONE < values.length)
                newCandidates = update(values, newCandidates, list, value.value(), value.precision(), value.stamp());
        }
        return newCandidates;
    }

    /**
     * This method represents a test of processing for a given set of values.
     * It takes in the type, value, item, and candidates' parameters and performs
     * specific operations on them to update the candidates' list.
     * It also logs debug information if necessary.
     *
     * @param type       The type of data.
     * @param item       The item to be processed.
     * @param candidates The list of candidates to be updated.
     * @return The updated list of candidates.
     */
    @SuppressWarnings({"CallToSuspiciousStringMethod", "MethodWithTooManyParameters"})
    private static List<_Quota<?>> test(final String origin, final String type, final String item,
                                        final List<_Quota<?>> candidates, final boolean silent) {

        List<_Quota<?>> newCandidates = candidates;
        final Collection<KeyValue> list = new ArrayList<>(0);
        final String[] values = item.trim().split(l0);
        if (ZERO < values.length) newCandidates = process(origin, type, values, newCandidates, list);
        if (!list.isEmpty() && isDebug() && !silent) Base.logTrace(a(list.toString(), COMMA_SPACE));
        return newCandidates;
    }

    /**
     * Updates the list of candidates based on the given values and value.
     *
     * @param values     The array of values used for creating fractions.
     * @param candidates The list of candidates to be updated.
     * @param list       The collection of KeyValue objects.
     * @param value      The Integer value.
     * @return The updated list of candidates.
     */
    private static List<_Quota<?>> update(final String[] values, final List<_Quota<?>> candidates, final Collection<?
            super KeyValue> list, final Integer value, final Integer precision, final String stamp) {

        List<_Quota<?>> newCandidates = candidates;
        final FractionRange fractions = validateAndCreateFractions(values);
        if (isNull(fractions)) newCandidates = null;
        else add(list, fractions, newCandidates, value, precision, stamp);
        return newCandidates;
    }

    /**
     * Validates the given values and creates an array of Fraction objects representing the minimum and maximum values.
     *
     * @param values the array of values to validate
     * @return an array of Fraction objects representing the minimum and maximum values, or null if validation fails
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "QuestionableName", "IfStatementWithTooManyBranches",
            "OverlyComplexMethod"})
    private static FractionRange validateAndCreateFractions(final String[] values) {

        final Fraction two; // value defined later
        final String stringOne = values[ONE].replace(s(_PLUS_), EMPTY).replace(s(_DASH_), EMPTY);
        final Fraction one = fr(stringOne);
        if (isNull(one)) return null;
        else {
            if (values[ONE].endsWith(s(_PLUS_))) two = UPPER_LIMIT;
            else if (values[ONE].endsWith(s(_DASH_))) two = LOWER_LIMIT;
            else if (TWO < values.length) two = fr(values[TWO]);
            else two = one;
            return new FractionRange((0 >= one.compareTo(two)) ? one : two, (0 <= one.compareTo(two)) ? one : two);
        }
    }

    /**
     * Validates the given value by converting it to an Integer and checking if it meets certain criteria.
     *
     * @param rawValue   the raw value to be validated
     * @param candidates the list of ShareOfIntValue objects representing the expected ranges of Integer share values
     * @return the validated Integer value, or null if validation fails
     */
    @SuppressWarnings({"AssignmentToMethodParameter", "UnusedAssignment", "ReassignedVariable"})
    private static Value validateValue(final String rawValue, Iterable<? extends _Quota<?>> candidates, final String origin) {

        Integer value = null;
        Integer precision = null;
        String stamp = null;
        try {
            if (rawValue.contains(s(L2))) {
                value = i(rawValue.split(s(L2))[ZERO]);
                precision = i(rawValue.split(s(L2))[ONE]);
                if (rawValue.split(s(L2)).length > TWO) stamp = rawValue.split(s(L2))[TWO];
            } else value = i(rawValue);
        } catch (final NumberFormatException e) {
            logEvent(S0, b(q(rawValue), IS_NOT_AN_INTEGER_IN___, origin));
            candidates = null;
        }
        if (isNotNull(candidates)) {
            if (isDuplicate(value, candidates)) {
                logEvent(S1, b(q(n(NULL, s(value))), IS_DEFINED_MORE_THAN___, origin));
                candidates = null;
            }
        }
        return new Value(value, precision, stamp);
    }

    /**
     * Returns the list of ColourRange objects representing the expected colours of colour ranges.
     *
     * @return the list of ColourRange objects
     */
    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    public final List<_Quota<?>> get() {

        return isNull(quotas) ? new ArrayList<>(0) : quotas;
    }

    @SuppressWarnings({"MethodWithMultipleReturnPoints", "unchecked"})
    @Override
    public final _Quota<Integer> getByKey(final Integer key) {

        if (isNotNull(quotas)) for (final _Quota range : quotas)
            if (key.equals(range.value())) return range;
        return null;
    }

    @SuppressWarnings("CallToSuspiciousStringMethod")
    @Override
    public final String getPropertyValue() {

        final Collection<String> result = new ArrayList<>(0);
        for (final _Quota quota : getSorted(true)) {

            final String range = ts(quota.min()).equals(ts(quota.max())) ? ts(quota.min()) : b(L0, ts(quota.min()),
                    ts(quota.max()));
            final String text = getText(quota);
            result.add(b(L0, s(text), range));
        }
        return (StringUtils.join(result, COMMA_SPACE));
    }

    private static String getText(final _Quota quota) {

        final Collection<String> list = new ArrayList<>(1);
        list.add(s(quota.value()));
        if (isNotNull(quota.precision())) list.add(s(quota.precision()));
        if (isNotNull(quota.stamp())) {
            if (isNull(quota.precision())) list.add(s(_0_));
            list.add(s(quota.stamp()));
        }
        return StringUtils.join(list, L2);
    }

    @SuppressWarnings({"NumericCastThatLosesPrecision", "CallToSuspiciousStringMethod"})
    public final String getPropertyValue(final int fences) {

        final Collection<String> result = new ArrayList<>(0);
        for (final _Quota quota : getSorted(true)) {

            final String range = ts(quota.min()).equals(ts(quota.max())) ? ts(quota.min()) : b(L0, ts(quota.min()),
                    ts(quota.max()));
            result.add(b(L0, s((int) (((Number) quota.value()).doubleValue() * fences)), range));
        }
        return (StringUtils.join(result, COMMA_SPACE));
    }

    public final List<_Quota<?>> getSorted(final boolean silent) {

        int diff = 0;
        final List<_Quota<?>> sortedRanges = quotas.stream().sorted(Comparator.comparingDouble(value -> Quota.median((Quota<
                        ?>) value)).reversed())  // sort by centre
                // value
                .toList();
        for (int i = 0; i < quotas.size(); i++) if (quotas.get(i).value() != sortedRanges.get(i).value()) diff++;
        if (Ranges.logSortCheck) if (0 < diff) logEvent(S2, b(THE_SORTED___, percentString(diff, quotas.size())));
        return sortedRanges;
    }

    public abstract Fraction logExpected(final int width, final Renderers renderers);

    @SuppressWarnings("unchecked")
    @Override
    public final void setQuotas(final List quotas) {

        this.quotas = quotas;
    }

    public abstract boolean isWithinPrecision(final Object value, final _Quota quota);
}