package works.lysenko.util.data.range.graph.values.integers;

import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.FastMath;
import works.lysenko.util.grid.record.graph.Options;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.lang.N.NEITHER_ACTUAL_NOR___;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * Represents a limit with overflow status and maximum value.
 *
 * @param overflow Indicates if there is an overflow.
 * @param max      The maximum value considered.
 */
record Limit(boolean overflow, double max) {

    static Limit limit(final Options go) {

        if (isAnyNull(go, go.actual(), go.requested())) throw new IllegalArgumentException(NEITHER_ACTUAL_NOR___);
        final boolean overflow = (go.actual().doubleValue() > go.requested().doubleValue());
        final boolean nullSlack = isNull(go.slack());
        final double slack = nullSlack ? ZERO : go.slack().doubleValue(go.requested());
        final double edge = FastMath.min(FastMath.max(go.requested().doubleValue() + slack,
                n(Fraction.ZERO, go.edge()).doubleValue()), ONE);
        final double max = FastMath.max(go.actual().doubleValue(), edge);
        return new Limit(overflow, max);
    }

}
