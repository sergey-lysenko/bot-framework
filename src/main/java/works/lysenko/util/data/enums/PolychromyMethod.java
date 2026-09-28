package works.lysenko.util.data.enums;

/**
 * PolychromyMethod is an enumeration representing different methods used for polychromy calculation.
 * <p>
 * The available method is:
 * - EUCLIDEAN_DISTANCE: Euclidean distance method
 */
@SuppressWarnings("MissingJavadoc")
public enum PolychromyMethod {
    EUCLIDEAN_DISTANCE;

    /**
     * Retrieves a PolychromyMethod object based on the specified index.
     *
     * @param method the index to determine the PolychromyMethod
     * @return the PolychromyMethod object corresponding to the index, or null if the index is out of range
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "ExcessiveRangeCheck"})
    public static PolychromyMethod getByIndex(final int method) {

        if (0 <= method && 1 > method)
            return values()[method]; // excessive range check for future multi-method implementations
        return null;
    }
}
