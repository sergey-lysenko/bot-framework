package works.lysenko.util.data.range.graph.values.fractions;

import org.apache.commons.math3.util.FastMath;
import works.lysenko.util.grid.record.graph.Options;

/**
 * Represents a limit with overflow status and maximum value.
 *
 * @param overflow Indicates if there is an overflow.
 * @param max      The maximum value considered.
 */
public record Limit(boolean overflow, double max) {

    static Limit limit(final Options go) {

        final boolean overflow = (go.actual().doubleValue() > go.requested().doubleValue());
        final double max = FastMath.max(go.actual().doubleValue(), go.requested().doubleValue());
        return new Limit(overflow, max);
    }

}
