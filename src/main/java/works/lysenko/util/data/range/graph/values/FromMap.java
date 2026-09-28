package works.lysenko.util.data.range.graph.values;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.graph.Values;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.grid.record.graph.Parameters;

import java.util.Map;

import static works.lysenko.util.data.enums.Ansi.yb;

/**
 * Represents a record that stores a map of shares and a maximum fraction value.
 * Provides functionality to generate graph parameters based on the stored data.
 *
 * @param shares A1 map where each key is associated with a Fraction value representing shares.
 * @param max    A1 Fraction value indicating the maximum value among shares.
 */
public record FromMap(Map<?, _ValuedRangeResult> shares, Fraction max) {

    /**
     * Generates and returns the graph parameters based on the current state.
     *
     * @param slack A1 Fraction value representing the allowable slack or fractionTolerance in the graph parameters.
     * @return An instance of Parameters containing the minimal maximum value,
     * a string representation of the size of shares, and the provided slack value.
     */
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public Parameters getGraphParameters(final Slack slack) {

        final double minimalMaximumValue = Values.getMinimalMaximumValue(max.doubleValue());
        final String amountS = yb(shares.size());
        return new Parameters(minimalMaximumValue, amountS, slack);
    }
}
