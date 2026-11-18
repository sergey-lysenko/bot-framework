package works.lysenko.util.apis.grid.r;

import works.lysenko.util.data.enums.PolychromyMethod;
import works.lysenko.util.data.range.FractionRange;

/**
 * Represents a polychromy object that holds a range of Fraction values and a polychromy method.
 * The range represents the minimum and maximum value of a range, while the method represents the method used for polychromy
 * calculation.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Polychromy {

    /**
     * Returns the FractionMinMax object representing the minimum and maximum value of a range.
     *
     * @return the FractionMinMax object representing the range
     */
    FractionRange getRange();

    /**
     * Sets the range of Fraction values for the object.
     *
     * @param range the FractionRange object representing the minimum and maximum value of the range
     */
    void setRange(FractionRange range);

    /**
     * Determines whether to ignore updating the polychromy value.
     *
     * @return true if the polychromy value should be ignored, false otherwise
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    boolean ignoreUpdate();

    /**
     * Returns the PolychromyMethod object representing the method used for polychromy calculation.
     *
     * @return the PolychromyMethod object representing the method used for polychromy calculation
     */
    PolychromyMethod method();

}
