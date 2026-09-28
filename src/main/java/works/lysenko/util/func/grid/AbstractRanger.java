package works.lysenko.util.func.grid;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.apis.grid.r._Ranger;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.data.range.Quota;
import works.lysenko.util.data.type.list.RangeResults;
import works.lysenko.util.data.type.list.RangerResults;
import works.lysenko.util.func.grid.ranger.Get;
import works.lysenko.util.func.grid.ranger.Issues;
import works.lysenko.util.func.grid.ranger.Render;
import works.lysenko.util.grid.record.meta.Method;
import works.lysenko.util.grid.record.meta.ValidationMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logDebug;
import static works.lysenko.Base.logEvent;
import static works.lysenko.util.chrs.__.IN;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.A.ACTUAL_VALUES_ARE_NOT_VALID;
import static works.lysenko.util.lang.T.THERE_ARE;
import static works.lysenko.util.lang.word.D.DUPLICATE;
import static works.lysenko.util.lang.word.E.EXPECTED;
import static works.lysenko.util.lang.word.R.RANGER;
import static works.lysenko.util.lang.word.R.RESULT;
import static works.lysenko.util.lang.word.S.SHARES;

/**
 * An abstract class that provides the framework for validating actual data against expected quotas
 * and generating validation results. This class handles initialisation, validation logic, processing
 * of actual and expected values, and rendering of results.
 *
 * @param <T> the type of elements that this ranger will validate
 */
public abstract class AbstractRanger<T> implements _Ranger<T> {

    private final ValidationMeta meta;
    private final IntegerRange amount;
    private final Map<T, _ValuedRangeResult> actual;
    private final Severity severity;
    private final _Quotas<T> expected;
    private final boolean ignoreOrder;
    private final boolean ignoreOther;
    private final Renderers renderers;

    private final Issues<T> issues;
    private final Render<T> render;

    /**
     * Constructs an AbstractRanger instance, initializing the validation metadata, actual data,
     * expected quotas, and configuration options regarding order and other attributes.
     *
     * @param meta        the ValidationMeta object encapsulating metadata associated with the validation process
     * @param actual      a map containing actual data of type T as keys with their respective ValuedRangeResult values
     * @param expected    the _Quotas object representing the expected quotas for validation
     * @param amount      the IntegerRange object defining the range for quantity validation
     * @param ignoreOrder a boolean indicating whether to ignore the order of elements during validation
     * @param ignoreOther a boolean indicating whether to ignore elements not specified in the expected quotas
     * @param renderers   the Renderers object used to handle rendering during validation
     * @param severity    the Severity level used to categorize issues encountered during validation
     */
    protected AbstractRanger(final ValidationMeta meta, final Map<T, _ValuedRangeResult> actual, final _Quotas<T> expected,
                             final IntegerRange amount, final boolean ignoreOrder, final boolean ignoreOther,
                             final Renderers renderers,
                             final Severity severity) {

        this.meta = meta;
        this.actual = actual;
        this.expected = expected;
        this.amount = amount;
        this.severity = severity;
        this.ignoreOrder = ignoreOrder;
        this.ignoreOther = ignoreOther;
        this.renderers = renderers;
        issues = new Issues<>(meta);
        render = new Render<>(meta, expected, actual, renderers);
    }

    /**
     * Validates the expected quotas by checking for duplicates in the provided list
     * and logs an event if duplicates are found.
     *
     * @param expected the _Quotas object containing a list of expected quotas
     *                 to be validated for duplicates
     */
    private static void validateExpected(final _Quotas<?> expected) {

        final List<_Quota<?>> list = expected.get();
        final Set<?> set = list.stream().map(_Quota::value).collect(Collectors.toSet());
        if (set.size() < list.size()) {
            final int diff = list.size() - set.size();
            logEvent(S0, b(THERE_ARE, s1(diff, DUPLICATE), IN, EXPECTED, SHARES));
        }
    }

    /**
     * Adds a sample to the specified range results object by correlating the provided actual value
     * with the expected and actual shares.
     *
     * @param toResults     the RangeResults object where the sample will be added
     * @param expectedShare the Quota object representing the expected result for validation
     * @param actualValue   the actual value of type T to be evaluated and added to the results
     * @param actualShare   the ValuedRangeResult object representing the actual result of the value
     */
    public abstract void addSample(RangeResults toResults, Quota<T> expectedShare, T actualValue,
                                   _ValuedRangeResult actualShare);

    public final ValidationMeta meta() {

        return meta;
    }

    /**
     * Validates the actual data against the expected quotas and generates the validation results.
     * This method follows these steps:
     * 1. Sets the title for the current validation process.
     * 2. Calculates the expected fraction based on the defined quotas.
     * 3. Processes the actual data using the expected fraction.
     * 4. Generates and returns the resulting RangerResults object.
     *
     * @return a RangerResults object containing the outcomes of the validation process.
     */
    public final RangerResults validate() {

        render.title();
        final Fraction edge = expected();
        return results(actual(edge));
    }

    /**
     * Processes the actual list of values against the provided edge fraction and prepares for rendering.
     * If the list of actual values is empty, an issue is logged indicating that the border is too high.
     * Renders the graph using the given edge fraction.
     *
     * @param edge the Fraction object representing the edge parameter used during rendering
     * @return a list of actual values of type T
     */
    private List<T> actual(final Fraction edge) {

        final List<T> actualValues = getActualValuesList();
        if (actualValues.isEmpty()) issues.addBorderTooHigh();
        render.actual(edge);
        return actualValues;
    }

    /**
     * Validates the expected quotas and renders the calculated expected value,
     * returning the resulting Fraction object.
     *
     * @return a Fraction object representing the calculated expected value
     * after validation and rendering.
     */
    private Fraction expected() {

        validateExpected(expected);
        return render.expected();
    }

    /**
     * Retrieves a list of actual values by extracting keys from the actual dataset.
     * If any issues are present during the validation process, it logs an event with the specified severity.
     *
     * @return a list of extracted actual values of type T
     */
    private List<T> getActualValuesList() {

        final List<T> actualValues = new ArrayList<>(actual.keySet());
        if (issues.areIssuesPresent(meta, actual, expected, amount, renderers))
            logEvent(severity, ACTUAL_VALUES_ARE_NOT_VALID);
        return actualValues;
    }

    /**
     * Retrieves a sample corresponding to a specific element from the provided list,
     * correlating it with its expected and actual shares based on predefined validation logic.
     *
     * @param of   the element of type T for which a sample will be generated
     * @param from the list of elements of type T from which the sample is derived
     * @return a Sample object consisting of the corresponding expected result and actual result
     */
    @SuppressWarnings("unchecked")
    private Sample getSample(final T of, final List<T> from) {

        Quota<T> quota = null;
        Quota<T> substitute = null;

        final int indexOfActual = from.indexOf(of);
        final boolean compulsive = Method.COMPULSIVE == meta().subject().method();

        if (indexOfActual >= expected.get().size()) {
            optionalError(indexOfActual, from, compulsive);
        } else {
            quota = (Quota<T>) expected.get().get(indexOfActual);
            substitute = quota;
        }

        if (isNotNull(quota) && !quota.value().equals(of)) {
            if (ignoreOrder || isNotNull(quota.precision()))
                substitute = Get.substitute(meta, expected, of, quota, indexOfActual, ignoreOther);
            else issues.addUnexpectedActual(of, indexOfActual);
        }
        final _ValuedRangeResult result = actual.get(of);

        if (compulsive && isNull(substitute)) issues.addNoCorrespondingShare(ignoreOther);
        return new Sample(substitute, result);
    }

    @SuppressWarnings("DataFlowIssue")
    private void optionalError(final int indexOfActual, final List<T> from, final boolean compulsive) {

        if (compulsive) {
            if (isNotNull(amount)) {
                if (indexOfActual >= amount.max()) issues.addTooFewExpectations(amount.max(), from);
            } else issues.addTooFewExpectations(expected.get().size(), from);
        }
    }

    /**
     * Processes an actual value to generate its corresponding sample and add it to the specified range results.
     *
     * @param results      the RangeResults object where the processed sample will be added
     * @param actualValue  the actual value of type T to be processed
     * @param actualValues the list of actual values from which the sample is derived
     */
    @SuppressWarnings("unchecked")
    private void processValue(final RangeResults results, final T actualValue, final List<T> actualValues) {

        final Sample sample = getSample(actualValue, actualValues);
        addSample(results, (Quota<T>) sample.quota(), actualValue, sample.result());
    }

    /**
     * Processes a list of actual values to generate validation results and determine their severity.
     *
     * @param actualValues the list of actual values to be validated
     * @return a RangerResults object representing the validation results,
     * either based on the range results or the issues encountered
     */
    private RangerResults results(final List<T> actualValues) {

        final RangeResults results = new RangeResults();
        for (final T actualValue : actualValues) processValue(results, actualValue, actualValues);
        final RangerResults result = issues.isEmpty() ? results.basedOn() : issues.get();
        logDebug(b(c(RANGER), RESULT, IS, result.render()));
        return result;
    }

    /**
     * Represents a record type used to encapsulate a sample within the validation process.
     * A sample is defined by correlating an expected quota with its actual corresponding result.
     */
    private record Sample(Quota<?> quota, _ValuedRangeResult result) {

    }
}