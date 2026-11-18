package works.lysenko.util.apis.grid.q;

import org.apache.commons.math3.fraction.Fraction;

/**
 * The _FractionQuotas interface represents methods for handling shares of Hue, Saturation, and Brightness values.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _FractionQuotas {

    /**
     * The quantisation method is used to retrieve the quantisation value.
     *
     * @return the fences value
     */
    int fences();

    /**
     * Retrieves the threshold Fraction value.
     *
     * @return the threshold Fraction value
     */
    Fraction threshold();
}
