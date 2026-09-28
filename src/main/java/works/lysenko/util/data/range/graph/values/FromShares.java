package works.lysenko.util.data.range.graph.values;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.data.range.graph.Values;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.grid.record.graph.Parameters;

import static java.util.Objects.isNull;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.strs.Bind.b;

/**
 * A1 data structure representing a range of integer values, shared items, and a maximum fraction.
 * Provides functionality to generate graph parameters based on the contained data.
 *
 * @param amount A1 range of integer values.
 * @param shares A1 collection of shared items.
 * @param max    A1 fraction value denoting the maximum allowable value.
 */
public record FromShares(_Range<Integer> amount, _Quotas<?> shares, Fraction max) {

    /**
     * Generates and returns graph parameters based on the current state of the FromShares record.
     *
     * @param slack A1 Fraction value representing allowable slack or fractionTolerance in the graph parameters.
     * @return An instance of Parameters containing the minimal maximum value,
     * a string representation of the amount, and the provided slack value.
     * @throws IllegalArgumentException if the max value is null.
     */
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public Parameters getGraphParameters(final Slack slack) {

        if (isNull(max)) throw new IllegalArgumentException("The max value cannot be null");
        final double minimalMaximumValue = Values.getMinimalMaximumValue(max.doubleValue());
        final String amountS = isNull(amount) ? yb(shares.get().size()) : b(yb(amount.toVerboseString()), OF);
        return new Parameters(minimalMaximumValue, amountS, slack);
    }
}
