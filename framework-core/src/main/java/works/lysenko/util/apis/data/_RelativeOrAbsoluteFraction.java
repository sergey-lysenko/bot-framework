package works.lysenko.util.apis.data;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.data.enums.RelativeOrAbsolute;

/**
 * Represents an interface for converting fractions to double values.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _RelativeOrAbsoluteFraction {

    /**
     * Retrieves data as a string representation.
     *
     * @return a string representation of the data
     */
    String data();

    /**
     * Converts a fraction based on a specified range and a reference value and returns its double representation.
     *
     * @param base      a range of fractions that serves as the base for the conversion
     * @param reference a fraction that is used as the reference value for the conversion
     * @return the double value representation of the specified fraction within the given range
     */
    Double doubleValue(_Range<Fraction> base, Fraction reference);

    /**
     * Converts the given fraction to a double value.
     *
     * @param base the fraction to be used as base value in case if the fraction is relative
     * @return the double value representation of the given fraction
     */
    Double doubleValue(Fraction base);

    /**
     * Renders a string representation based on the provided options.
     *
     * @param full    indicates whether the full detailed rendering should be used
     * @param scripts specifies whether scripts should be included in the rendering
     * @return a string representing the rendered output according to the given parameters
     */
    String render(boolean full, boolean scripts);

    /**
     * Renders a string representation of the implementing entity.
     *
     * @return a string representation of the implementing entity
     */
    String render();

    /**
     * Determines the type of measurement.
     *
     * @return the type of measurement, which can be either absolute or relative
     */
    default RelativeOrAbsolute type() {

        return null;
    }
}
