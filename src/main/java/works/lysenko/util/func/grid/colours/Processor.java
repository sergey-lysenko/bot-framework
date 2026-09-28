package works.lysenko.util.func.grid.colours;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.data._RangedMargin;
import works.lysenko.util.apis.grid.g._GridProperties;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.apis.grid.r._Processor;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.Quota;
import works.lysenko.util.func.logs.Trace;
import works.lysenko.util.grid.record.Request;
import works.lysenko.util.prop.grid.Update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.apis.grid.g._GridProperties.getGridPropertiesByName;
import static works.lysenko.util.apis.grid.g._GridProperties.putPropertiesToGridFile;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.Brackets.CURLY;
import static works.lysenko.util.data.enums.ColoursIgnore.SHRINK;
import static works.lysenko.util.data.enums.ColoursIgnore.UPDATE;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.range.Quota.shareOfInteger;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.func.grid.colours.processor.Compiler.merge;
import static works.lysenko.util.func.grid.colours.processor.Compiler.reuse;
import static works.lysenko.util.func.grid.colours.processor.Compiler.shrink;
import static works.lysenko.util.func.logs.trace.Data.traceable;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.E.EXPECTATIONS_UPDATE_REQUESTED_BY___;
import static works.lysenko.util.lang.E.EXPECTATIONS_UPDATE_WAS_NOT___;
import static works.lysenko.util.lang.I.IS_BIGGER_THAN_ALLOWED;
import static works.lysenko.util.lang.N.NEW_COLOURS_PROPERTY___;
import static works.lysenko.util.lang.O.ORIGINAL_EXPECTATIONS_PERSISTED;
import static works.lysenko.util.lang.word.A.ACTUAL;
import static works.lysenko.util.lang.word.C.COLOUR;
import static works.lysenko.util.lang.word.C.COLOURS;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.M.MARGIN;

/**
 * The Processor class is responsible for handling the validation logic of
 * merging and updating the actual and expected results based on validation rules.
 * It provides functionality for processing both positive and negative validation results,
 * and handles operations such as merging results and updating expected properties.
 * @param actual
 * @param expected
 */
@SuppressWarnings({"rawtypes", "unchecked", "CallToSuspiciousStringMethod"})
public record Processor(Map<Integer, _ValuedRangeResult> actual, _Quotas expected) implements _Processor {

    public void processNegativeResult(final Request vr) {

        final String original = expected.getPropertyValue();
        expected.setQuotas(mergeFailedActualAndExpected(vr.irq().colours().margin(), vr.irq().colours().ignore(SHRINK)));
        final String newValue = expected.getPropertyValue();

        if (newValue.equals(original)) logDebug(b(ORIGINAL_EXPECTATIONS_PERSISTED, yb(e(CURLY, newValue))));
        else {
            logDebug(b(NEW_COLOURS_PROPERTY___, yb(e(CURLY, newValue))));
            logDebug(vr.irq().colours().renderIgnore());
            if (Update.colour) {
                if (vr.irq().colours().ignore(UPDATE)) logDebug(b(c(COLOUR), EXPECTATIONS_UPDATE_REQUESTED_BY___));
                else updateExpected(vr.name(), newValue);
            } else logDebug(b(c(COLOUR), EXPECTATIONS_UPDATE_WAS_NOT___));
        }
    }

    /**
     * Processes the positive validation result for a given Request.
     * This method updates the expected object's colour property based on the
     * information provided in the Request.
     *
     * @param vr the Request containing validation data and rules
     */
    public void processPositiveResult(final Request vr) {

        final String original = expected.getPropertyValue();
        expected.setQuotas(mergePassedActualAndExpected(vr.irq().colours().ignore(SHRINK)));
        final String newValue = expected.getPropertyValue();

        if (newValue.equals(original)) logDebug(b(ORIGINAL_EXPECTATIONS_PERSISTED, yb(e(CURLY, newValue))));
        else {
            logDebug(b(NEW_COLOURS_PROPERTY___, yb(e(CURLY, newValue))));
            logDebug(vr.irq().colours().renderIgnore());
            if (Update.colour) {
                if (vr.irq().colours().ignore(UPDATE)) logDebug(b(c(COLOUR), EXPECTATIONS_UPDATE_REQUESTED_BY___));
                else updateExpected(vr.name(), newValue);
            } else logDebug(b(c(COLOUR), EXPECTATIONS_UPDATE_WAS_NOT___));
        }
    }

    /**
     * Updates the expected colours for a given grid file.
     *
     * @param name             the name of the grid
     * @param newPropertyValue the new property value for the colours
     */
    public void updateExpected(final String name, final String newPropertyValue) {

        final _GridProperties gridProperties = getGridPropertiesByName(name);
        gridProperties.updateProperty(COLOURS, newPropertyValue);
        putPropertiesToGridFile(gridProperties, name);
    }

    /**
     * Merges the failed validation results from the actual and expected data sets.
     *
     * @param margin       The allowed deviation fraction for the validation.
     * @param ignoreShrink Flag to determine whether the shrink operation should be ignored.
     * @return A1 list of merged Quota<> objects representing the failed validation ranges.
     */
    @SuppressWarnings("MethodWithMultipleLoops")
    private List<_Quota<Integer>> mergeFailedActualAndExpected(final _RangedMargin margin, final boolean ignoreShrink) {

        final List<_Quota<Integer>> newRanges = new ArrayList<>(actual.size());

        // cycling through actual and merging ranges
        for (final Map.Entry<Integer, _ValuedRangeResult> entry : actual.entrySet())
            processFailed(margin, entry, newRanges, ignoreShrink);

        // cycling through expected and checking for leftovers
        if (isNotNull(expected)) if (isNotNull(expected.get())) for (final Object entry : expected.get())
            if (!actual.containsKey(((_Quota<Integer>) entry).value())) newRanges.add((_Quota<Integer>) entry);
        return newRanges;
    }

    /**
     * Merges the passed validation ranges from the actual and expected data.
     *
     * @param ignoreShrink Flag to determine whether the shrink operation should be ignored.
     * @return A1 list of merged ShareOfInteger objects representing the passed validation ranges.
     */
    @SuppressWarnings({"unchecked", "MethodWithMultipleLoops"})
    private List<_Quota<Integer>> mergePassedActualAndExpected(final boolean ignoreShrink) {

        final List<_Quota<Integer>> newRanges = new ArrayList<>(actual.size());

        // cycling through actual and merging ranges
        for (final Map.Entry<Integer, _ValuedRangeResult> entry : actual.entrySet())
            processPassed(entry, newRanges, ignoreShrink);

        // cycling through expected and checking for leftovers
        if (Update.passed && isNotNull(expected)) if (isNotNull(expected.get())) for (final Object entry : expected.get())
            if (!actual.containsKey(((_Quota<Integer>) entry).value())) newRanges.add((_Quota<Integer>) entry);
        return newRanges;
    }

    /**
     * Processes the failed validation result for a given entry.
     *
     * @param entry        The entry containing the key and corresponding Fraction value.
     * @param newRanges    The list of Quota objects to update with new data.
     * @param ignoreShrink Flag to determine whether the shrink operation should be ignored.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void processFailed(final _RangedMargin rangedMargin,
                               final Map.Entry<Integer, ? extends _ValuedRangeResult> entry, final List<?
                    super _Quota<Integer>> newRanges, final boolean ignoreShrink) {

        final Integer key = entry.getKey();
        final _ValuedRangeResult value = entry.getValue();
        final _Quota range = expected.getByKey(key);
        final Integer precision = (isNull(range)) ? null : (Integer) range.precision();
        final String stamp = (isNull(range)) ? null : range.stamp();
        final Fraction margin = (isNotNull(range)) ? range.actualAbsoluteMargin(value.value()) : null;
        Trace.log(traceable(key, value, range, margin));
        if (isNotNull(margin)) {
            if (isNull(value.result())) reuse(EXPECTED, range, newRanges); // This one is OK
            else {
                if (value.result().isPassed()) {
                    if (0 == margin.doubleValue())
                        shrink(key, value, range, (Collection<? super _Quota>) newRanges, ignoreShrink);
                    else merge(key, value, range, (List<? super Quota>) newRanges);
                } else {
                    logEvent(S0, b(c(COLOUR), MARGIN, s(margin.doubleValue()), IS_BIGGER_THAN_ALLOWED, rangedMargin.render()));
                    reuse(EXPECTED, range, newRanges);
                }
            }
        } else reuse(ACTUAL, shareOfInteger(key, precision, stamp, value), newRanges);
    }

    /**
     * Processes the passed validation result for a given entry.
     *
     * @param entry        The entry containing the key and corresponding Fraction value.
     * @param newRanges    The list of Quota objects to update with new data.
     * @param ignoreShrink Flag to determine whether the shrink operation should be ignored.
     */
    private void processPassed(final Map.Entry<Integer, ? extends _ValuedRangeResult> entry, final List<?
            super _Quota<Integer>> newRanges, final boolean ignoreShrink) {

        final Integer key = entry.getKey();
        final _ValuedRangeResult value = entry.getValue();
        final _Quota range = expected.getByKey(key);
        final Integer precision = (isNull(range)) ? null : (Integer) range.precision();
        final String stamp = (isNull(range)) ? null : range.stamp();
        final Fraction margin = (isNotNull(range)) ? range.actualAbsoluteMargin(value.value()) : null;
        Trace.log(traceable(key, value, range, margin));
        if (isNotNull(margin)) {
            if (0 == margin.doubleValue()) shrink(key, value, range, (Collection<? super _Quota>) newRanges, ignoreShrink);
        } else reuse(ACTUAL, shareOfInteger(key, precision, stamp, value), newRanges);
    }

}