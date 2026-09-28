package works.lysenko.util.grid.record.rash;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.data.enums.Direction;
import works.lysenko.util.data.records.Slack;

import static java.lang.StrictMath.round;
import static java.util.Objects.isNull;
import static works.lysenko.util.data.enums.Direction.NEGATIVE;
import static works.lysenko.util.data.enums.Direction.POSITIVE;

/**
 * Represents a range with maximum, minimum, and their corresponding slack-adjusted values.
 *
 * @param max  The maximum value.
 * @param min  The minimum value.
 * @param maxS The slack-adjusted maximum value.
 * @param minS The slack-adjusted minimum value.
 */
public record Binner(long max, long min, long maxS, long minS) {

    /**
     * Calculates the base value adjusted for the provided width and normalised
     * against a maximum value.
     *
     * @param width The width or range of values over which to scale the base value.
     * @param max   The maximum value used for normalising the calculation.
     * @param base  The base value to be scaled and normalised.
     * @return The base value scaled to the width and normalised against the maximum,
     * truncated to an integer.
     */
    private static long calculateBaseValue(final int width, final double max, final double base) {

        return round(width * (base / max));
    }

    /**
     * Calculates and returns the slack-adjusted value based on the provided width, maximum value,
     * base value, slack adjustment factor, and direction.
     *
     * @param width      The width or range of values over which to adjust.
     * @param max        The maximum value for normalising the calculation.
     * @param base       The base value to start the calculation from before applying slack.
     * @param slackValue The slack adjustment factor that modifies the base value.
     * @param direction  The direction indicating whether the slack adjustment should add to or subtract from the base.
     * @return The slack-adjusted value, scaled to the width and normalised against the maximum.
     */
    private static long calculateSlackAdjustedValue(final int width, final double max, final double base,
                                                    final double slackValue, final int direction) {

        return round(width * ((base + (direction * slackValue)) / max));
    }

    /**
     * Computes the result based on provided shares data, share range, slack adjustment, and direction.
     * This involves calculating the base and optionally the slack-adjusted values.
     *
     * @param data      The SharesData object containing the necessary parameters such as the value, index, maximum value,
     *                  and width.
     * @param share     The Quota object representing a range defined by minimum and maximum fractions.
     * @param slack     The Slack object, which may hold a Slack adjustment factor for calculation.
     * @param direction The direction indicating whether to compute based on the minimum or maximum share value.
     * @return A1 Result object containing the base and slack-adjusted values as integers.
     */
    @SuppressWarnings("StandardVariableNames")
    private static Result computeResult(final SharesData<?> data, final _Quota<?> share, final Slack slack,
                                        final Direction direction) {

        final Fraction base = (POSITIVE == direction) ? share.max() : share.min();
        final long n = calculateBaseValue(data.width(), data.max(), base.doubleValue());
        final long s = isNull(slack) ? n : calculateSlackAdjustedValue(data.width(), data.max(), base.doubleValue(),
                slack.doubleValue(base), direction.value());
        return new Result(n, s);
    }

    /**
     * Computes and returns a Binner object representing the range of values
     * based on the provided shares data, share range, and slack adjustment factor.
     *
     * @param data  The SharesData object containing the value, index, maximum value, and width.
     * @param share The Quota object representing the range of share values with minimum and maximum limits as fractions.
     * @param slack The Slack object used for slack adjustment to the computed range values.
     * @return A1 Binner object with maximum, minimum, and their corresponding slack-adjusted values.
     */
    public static Binner getRanges(final SharesData<?> data, final _Quota<?> share, final Slack slack) {

        final Result min = computeResult(data, share, slack, NEGATIVE);
        final Result max = computeResult(data, share, slack, POSITIVE);
        return new Binner(max.n(), min.n(), max.s(), min.s());
    }

    @SuppressWarnings("StandardVariableNames")
    private record Result(long n, long s) {

    }
}