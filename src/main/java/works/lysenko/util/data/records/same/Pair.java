package works.lysenko.util.data.records.same;

import works.lysenko.util.apis.data._SameTypePair;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Booleans.isTrue;
import static works.lysenko.util.lang.C.COMMA_SPACE;

/**
 * Represents a pair that holds two values of the same type.
 * This record is immutable.
 *
 * @param <T>   the type of the values stored in the Pair
 * @param left  the left value of the Pair
 * @param right the right value of the Pair
 */
public record Pair<T>(T left, T right) implements _SameTypePair<T> {

    /**
     * Creates a new immutable Pair instance with the specified left and right values.
     *
     * @param <T>   the type of the values to be stored in the Pair
     * @param left  the left value of the Pair
     * @param right the right value of the Pair
     * @return a new Pair instance containing the specified left and right values
     */
    public static <T> Pair<T> p(final T left, final T right) {

        return new Pair<>(left, right);
    }

    /**
     * Returns either the left or right value based on boolean evaluation.
     *
     * @return either the left or right value
     */
    public T any() {

        return isTrue() ? left : right;
    }

    /**
     * Returns the left value of the pair.
     *
     * @return the left value
     */
    public T l() {

        return left;
    }

    /**
     * Returns the right value of the pair.
     *
     * @return the right value
     */
    public T r() {

        return right;
    }

    /**
     * Returns the key (left value) of the pair.
     *
     * @return the left value as the key
     */
    public T key() {

        return left;
    }

    /**
     * Returns the value (right value) of the pair.
     *
     * @return the right value as the value
     */
    public T value() {

        return right;
    }

    /**
     * Returns the element before (left value).
     *
     * @return the left value
     */
    @Override
    public T before() {

        return left;
    }

    /**
     * Returns the element after (right value).
     *
     * @return the right value
     */
    @Override
    public T after() {

        return right;
    }

    @Override
    public String toString() {

        return s(left, COMMA_SPACE, right);
    }
}
