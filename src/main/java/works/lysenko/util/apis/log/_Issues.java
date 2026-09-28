package works.lysenko.util.apis.log;

import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.data.type.list.RangerResults;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.grid.record.meta.ValidationMeta;

import java.util.List;
import java.util.Map;

/**
 * The _Issues interface provides methods for managing and validating issues related to
 * shares and their corresponding values during the validation process.
 *
 * @param <T> The type of the actual values being validated.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Issues<T> {

    /**
     * Adds an issue to indicate that the border specified in the validation is too high.
     */
    void addBorderTooHigh();

    /**
     * Adds an issue indicating that no corresponding share was found during the validation process.
     *
     * @param ignoreOther a boolean flag indicating whether other related issues should be ignored.
     */
    void addNoCorrespondingShare(boolean ignoreOther);

    /**
     * Adds an issue indicating that the number of provided expectations is insufficient.
     * This method is used to record cases where the expected number of items does not match
     * the actual number in the validation process.
     *
     * @param size         The expected number of items.
     * @param actualValues The list of actual values that were validated.
     */
    void addTooFewExpectations(final int size, final List<T> actualValues);

    /**
     * Adds an unexpected actual value at a specified index to the current issues list.
     *
     * @param actualValue The actual value that was not expected.
     * @param index       The index at which the actual value was found.
     */
    void addUnexpectedActual(final T actualValue, final int index);

    /**
     * Checks whether there are any issues present during the validation process.
     *
     * @param meta      The meta-information for the validation request.
     * @param actual    A1 map containing the actual values and their corresponding fractions.
     * @param expected  The expected shares to be validated against.
     * @param amount    The range for the amount being validated.
     * @param renderers The renderers used for logging and displaying results.
     * @return true if there are issues present; false otherwise.
     */
    boolean areIssuesPresent(ValidationMeta meta, Map<T, _ValuedRangeResult> actual, _Quotas<T> expected, IntegerRange amount
            , Renderers renderers);

    /**
     * Retrieves the collection of RangerResult instances encapsulated within a RangerResults object.
     *
     * @return A1 RangerResults object containing the collection of RangerResult instances.
     */
    RangerResults get();

    /**
     * Checks if the collection of issues is empty.
     *
     * @return true if there are no issues present; false otherwise.
     */
    boolean isEmpty();
}
