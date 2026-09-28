package works.lysenko.util.apis.grid.q;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.t._Value;

/**
 * Represents a share of a certain type T with operations for margin, median, percentage, and width.
 *
 * @param <T> the type parameter associated with the share
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Quota<T> {

    /**
     * Computes the absolute margin for the given actual value.
     *
     * @param actualValue the actual value for which the absolute margin is computed
     * @return the computed absolute margin as a Fraction
     */
    Fraction actualAbsoluteMargin(Fraction actualValue);

    /**
     * Determines and returns the maximum fraction value based on the implementing context.
     *
     * @return the maximum fraction value, or null if not implemented
     */
    default Fraction max() {

        return null;
    }

    /**
     * Calculates and returns the percentage value as a string.
     *
     * @return the percentage value as a string representation
     */
    double median();

    /**
     * Determines and returns the minimum fraction value based on the implementing context.
     *
     * @return the minimum fraction value, or null if not implemented
     */
    default Fraction min() {

        return null;
    }

    /**
     * Calculates and returns the percentage value as a string.
     *
     * @return the percentage value as a string representation
     */
    String percentage();

    /**
     * Renders and returns a string representation of the current instance or its relevant data.
     *
     * @return a string representing the rendered output
     */
    String render();

    /**
     * Provides a share of a value of type T.
     *
     * @return an instance of _Value<>, representing the share
     */
    _Value<T> share();

    /**
     * Retrieves the value associated with the current instance.
     *
     * @return the value of type T
     */
    T value();

    /**
     * Retrieves the precision value associated with the current instance.
     *
     * @return the precision value of type T
     */
    T precision();

    /**
     * Provides a string representation of a unique or timestamped identifier associated with the instance.
     *
     * @return a string representing the stamp or identifier
     */
    String stamp();

    /**
     * Calculates and returns the width of the share.
     *
     * @return the width as a double value
     */
    double width();
}
