package works.lysenko.util.data.records.diff;

import works.lysenko.util.apis.data._DifferentTypesPair;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.C.COMMA_SPACE;

/**
 * Represents a pair that holds two values of potentially different types.
 *
 * @param <L>   type of left value
 * @param <R>   type of right value
 * @param left  the left value of the pair
 * @param right the right value of the pair
 */
@SuppressWarnings("unused")
public record Pair<L, R>(L left, R right) implements _DifferentTypesPair<L, R> {

    /**
     * Creates a Pair object with the provided left and right values.
     *
     * @param <L>   The type of the left value.
     * @param <R>   The type of the right value.
     * @param left  The left value of the pair.
     * @param right The right value of the pair.
     * @return A new Pair object containing the left and right values.
     */
    public static <L, R> Pair<L, R> p(final L left, final R right) {

        return new Pair<>(left, right);
    }

    /**
     * Returns the left value of the pair.
     *
     * @return the left value
     */
    @Override
    public L l() {

        return left;
    }

    /**
     * Returns the right value of the pair.
     *
     * @return the right value
     */
    @Override
    public R r() {

        return right;
    }

    /**
     * Returns the key (left value) of the pair.
     *
     * @return the left value
     */
    public L key() {

        return left;
    }

    /**
     * Returns the value (right value) of the pair.
     *
     * @return the right value
     */
    public R value() {

        return right;
    }

    @Override
    public String toString() {

        return s(left, COMMA_SPACE, right);
    }
}
