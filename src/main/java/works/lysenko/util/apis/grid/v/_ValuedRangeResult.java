package works.lysenko.util.apis.grid.v;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.enums.RangeResult;

/**
 * The _ValuedRangeResult interface represents a contract for objects that encapsulate a fractional value
 * and an associated range validation result. Implementations of this interface provide methods for accessing
 * and modifying the fractional value, as well as the result of the range validation.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _ValuedRangeResult {

    /**
     * Retrieves the fractional value in double precision.
     *
     * @return the fractional value as a double.
     */
    double doubleValue();

    /**
     * Returns the result of the range validation.
     *
     * @return the RangeResult representing the outcome of the validation.
     */
    RangeResult result();

    /**
     * Sets the result of the range validation.
     *
     * @param result the RangeResult representing the outcome of the validation.
     */
    void result(RangeResult result);

    /**
     * Retrieves the fractional value encapsulated within the implementing class.
     *
     * @return the Fraction instance representing the fractional value.
     */
    Fraction value();

    /**
     * Retrieves the identifier or marker associated with the current state
     * or instance of the implementing object.
     *
     * @return a string representing the identifier or marker.
     */
    String stamp();
}
