package works.lysenko.util.apis.data;

/**
 * Represents an interface for Pair elements holding two values of the same type.
 * Implementations of this interface provide methods to retrieve the left and right values of the Pair, as well as a method
 * to return either value based on a condition.
 *
 * @param <T> the type of the elements stored in the Pair
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _SameTypePair<T> {

    /**
     * Returns either the left or right value based on the result of the 'isTrue' method.
     *
     * @return the left value if 'isTrue' returns true, otherwise the right value
     */
    T any();

    /**
     * Retrieves the left value of this Pair element.
     *
     * @return the left value of this Pair
     */
    T l();

    /**
     * Retrieves the right value of this Pair element.
     *
     * @return the right value of this Pair
     */
    T r();

    /**
     * Retrieves the left value of this Pair element.
     *
     * @return the key of this Pair
     */
    T key();

    /**
     * Retrieves the right value of this Pair element.
     *
     * @return the value of this element
     */
    T value();

    /**
     * Retrieves the left value of this Pair element.
     *
     * @return the "before" interpretation value of this Pair
     */
    T before();

    /**
     * Retrieves the right value of this Pair element.
     *
     * @return the "after" interpretation value of this Pair
     */
    T after();
}
