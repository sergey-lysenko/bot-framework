package works.lysenko.util.apis.grid.t;

import org.apache.commons.lang3.Range;

/**
 * Represents a range of values with a minimum and maximum value.
 *
 * @param <ofType> the type of values in the range (must implement Comparable<ofType>)
 */
public interface _Range<ofType extends Comparable<ofType>> {

    /**
     * Retrieves the range of values.
     *
     * @return the range of values
     */
    Range<ofType> get();

    /**
     * Checks if the given value is included in the range defined by the minimum and maximum values.
     *
     * @param value the value to be checked
     * @return true if the value is included in the range, false otherwise
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    Boolean includes(ofType value);

    /**
     * Computes and returns the absolute difference value for the given input.
     *
     * @param value the value for which the absolute difference is to be computed.
     * @return the absolute difference value.
     */
    ofType margin(ofType value);

    /**
     * Retrieves the maximum value.
     *
     * @return the maximum value
     */
    ofType max();

    /**
     * Retrieves the minimum value.
     *
     * @return the minimum value
     */
    ofType min();

    /**
     * Generates a detailed string representation of the implementing object.
     *
     * @return a verbose string representation of the object
     */
    String toVerboseString();
}