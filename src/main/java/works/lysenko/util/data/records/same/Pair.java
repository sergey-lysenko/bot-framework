package works.lysenko.util.data.records.same;

import works.lysenko.util.apis.data._SameTypePair;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Booleans.isTrue;
import static works.lysenko.util.lang.C.COMMA_SPACE;

/**
 * Represents a simple Pair class that holds two values of the same type.
 * This class is immutable.
 *
 * @param <T> the type of the values stored in the Pair
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

    public T any() {

        return isTrue() ? left : right;
    }

    public T l() {

        return left;
    }

    public T r() {

        return right;
    }

    public T key() {

        return left;
    }

    public T value() {

        return right;
    }

    @Override
    public T before() {

        return left;
    }

    @Override
    public T after() {

        return right;
    }

    @Override
    public String toString() {

        return s(left, COMMA_SPACE, right);
    }
}
