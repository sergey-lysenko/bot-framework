package works.lysenko.util.data.records.diff;

import works.lysenko.util.apis.data._DifferentTypesPair;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.lang.C.COMMA_SPACE;

/**
 * @param <L> type of left
 * @param <R> type of right
 */
@SuppressWarnings("unused")
public record Pair<L, R>(L left, R right) implements _DifferentTypesPair<L, R> {

    /**
     * Creates a Pair object with the provided left and right values.
     *
     * @param left  The left v of the pair.
     * @param right The right v of the pair.
     * @param <L>   The type of the left v.
     * @param <R>   The type of the right v.
     * @return A1 new Pair object containing the left and right values.
     */
    public static <L, R> Pair<L, R> p(final L left, final R right) {

        return new Pair<>(left, right);
    }

    @Override
    public L l() {

        return left;
    }

    @Override
    public R r() {

        return right;
    }

    public L key() {

        return left;
    }

    public R value() {

        return right;
    }

    @Override
    public String toString() {

        return s(left, COMMA_SPACE, right);
    }
}
