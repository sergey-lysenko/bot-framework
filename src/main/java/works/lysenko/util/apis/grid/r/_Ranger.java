package works.lysenko.util.apis.grid.r;

import works.lysenko.util.data.range.Quota;
import works.lysenko.util.data.type.list.RangeResults;
import works.lysenko.util.data.type.list.RangerResults;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.grid.record.meta.ValidationMeta;

/**
 * The _Ranger interface provides mechanisms to validate and process actual values against expected ranges.
 *
 * @param <T> The type of the values being validated by the implementations of this interface.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Ranger<T> {

    /**
     * Adds an actual value to the range results, taking into account the specified range and share.
     *
     * @param actualValue   the actual value to add
     * @param toResults     the object holding the range validation results
     * @param expectedShare the share of the value within the specified range
     * @param actualShare   the fraction representing the share
     */
    void addSample(RangeResults toResults, Quota<T> expectedShare, T actualValue, _ValuedRangeResult actualShare);

    /**
     * Retrieves the validation meta-information encapsulated in a ValidationMeta record.
     *
     * @return the ValidationMeta record containing meta-information for a validation request
     */
    ValidationMeta meta();

    /**
     * This method checks if the actual values are within the acceptable range compared to the expected values.
     * It logs the relevant information, validates the expected values, retrieves the actual values, renders the actual graph,
     * and processes each actual value to determine the overall result.
     *
     * @return a RangerResult indicating whether the values are acceptable based on the defined thresholds
     * and comparisons made during the validation process.
     */
    RangerResults validate();

}
