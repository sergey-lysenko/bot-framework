package works.lysenko.util.apis.data;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.data.enums.RangedMarginType;

/**
 * Represents an interface for defining a ranged margin with various operations.
 * A ranged margin provides methods to compute values, render string representations,
 * and retrieve its type.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _RangedMargin {

    /**
     * Computes a double value based on the provided base fraction.
     *
     * @param base the Fraction used as the base for computation
     * @return the computed double value
     */
    double doubleValue(Fraction base);

    /**
     * Computes a double value based on the provided range and reference value.
     *
     * @param base      the range of Fraction values used as the base for computation
     * @param reference the reference Fraction value used in the computation
     * @return the computed double value
     */
    Double doubleValue(final _Range<Fraction> base, final Fraction reference);

    /**
     * Renders and returns a string representation of the implementing object.
     *
     * @return a string representation of the object
     */
    String render();

    /**
     * Retrieves the type of the ranged margin.
     *
     * @return the type of the ranged margin as a RangedMarginType
     */
    RangedMarginType type();
}
