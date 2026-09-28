package works.lysenko.util.apis.data;

/**
 * Interface for a pair of different types, providing methods to retrieve the left and right values of the pair.
 *
 * @param <L> The type of the left value in the pair
 * @param <R> The type of the right value in the pair
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _DifferentTypesPair<L, R> {

    /**
     * Retrieves the left value of this Pair element.
     *
     * @return the left value of this Pair
     */
    L l();

    /**
     * Retrieves the left value of this Pair element.
     *
     * @return the left value of this Pair
     */
    R r();

    /**
     * Retrieves the left value of this Pair element.
     *
     * @return the key of this Pair
     */
    L key();

    /**
     * Retrieves the right value of this Pair element.
     *
     * @return the value of this element
     */
    R value();
}
