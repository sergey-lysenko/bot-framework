package works.lysenko.util.apis.grid.t;

import org.apache.commons.math3.fraction.Fraction;

/**
 * The _RelativePosition interface provides a contract for accessing horizontal
 * and vertical positions in both absolute and fractional forms. Implementations
 * of this interface are expected to provide methods to retrieve these positions
 * using various representations such as dimensionless doubles or Fractions.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "unused"})
public interface _RelativePosition {

    /**
     * Retrieves the horizontal position as a dimensionless value.
     *
     * @return a double representing the horizontal position.
     */
    double h();

    /**
     * Retrieves the horizontal position as a double value.
     *
     * @return a double representing the horizontal position.
     */
    double hor();

    /**
     * Calculates the horizontal position as a fraction of the total width.
     *
     * @return a Fraction representing the horizontal position.
     */
    Fraction horizontal();

    /**
     * Retrieves the vertical position as a double value.
     *
     * @return a double representing the vertical position.
     */
    double v();

    /**
     * Retrieves the vertical position as a fractional value of the total height.
     *
     * @return a double representing the vertical position as a fraction.
     */
    double ver();

    /**
     * Calculates the vertical position as a fraction of the total height.
     *
     * @return a Fraction representing the vertical position.
     */
    Fraction vertical();
}
