package works.lysenko.util.apis.grid.q;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.enums.ColoursIgnore;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.data.records.RangedMargin;
import works.lysenko.util.grid.record.misc.IgnoreHSB;

import java.util.List;

/**
 * Represents a class that defines various properties related to colors.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanMethodNameMustStartWithQuestion"})
public interface _ColoursQuotas {

    /**
     * Retrieves the IntMinMax object defining the range of color counts.
     *
     * @return the IntMinMax object representing the range of color counts
     */
    IntegerRange amount();

    /**
     * Retrieves the border fraction value.
     *
     * @return the border fraction value
     */
    Fraction border();

    /**
     * Retrieves a list of quotas of unspecified type.
     *
     * @return a list of _Quota objects, or null if not implemented
     */
    default List<_Quota<?>> get() {

        return null;
    }

    /**
     * Determines whether a specific ColorsIgnore enum value should be ignored.
     *
     * @param what the ColorsIgnore value to check
     * @return true if the specified ColorsIgnore value should be ignored, false otherwise
     */
    boolean ignore(ColoursIgnore what);

    /**
     * Retrieves the IgnoreHSB object representing the range of HSB values to ignore.
     *
     * @return the IgnoreHSB object representing the range of HSB values to ignore
     */
    IgnoreHSB ignoreHSB();

    /**
     * Retrieves the allowed deviation value as a Fraction.
     *
     * @return the allowed deviation value as a Fraction
     */
    RangedMargin margin();

    /**
     * Provides the string representation of the ignore settings.
     *
     * @return a string representing the ignore settings
     */
    String renderIgnore();
}
