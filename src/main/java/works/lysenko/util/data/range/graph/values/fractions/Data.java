package works.lysenko.util.data.range.graph.values.fractions;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.graph.values.FromMap;
import works.lysenko.util.data.range.graph.values.FromShares;
import works.lysenko.util.grid.record.graph.Options;
import works.lysenko.util.grid.record.graph.Parameters;

import java.util.Map;

import static works.lysenko.util.data.range.graph.values.fractions.Limit.limit;
import static works.lysenko.util.data.range.graph.values.fractions.Text.text;
import static works.lysenko.util.func.type.fractions.Factory.fr;

/**
 * The DataStorage record encapsulates the graph parameters, value, and max value retrieved from given inputs.
 *
 * @param gp    The graph parameters.
 * @param value A1 string representing the value.
 * @param max   A1 string representing the maximum value.
 */
public record Data(Parameters gp, String value, String max) {

    /**
     * Creates a new DataStorage instance using the given shares, graph options, and fences.
     *
     * @param shares A1 map representing the shares of fractions.
     * @param go     The graph options used to configure the graph.
     * @param fences An integer representing the number of fences.
     * @return A1 new instance of DataStorage with computed graph parameters, value, and max value.
     */
    public static Data data(final Map<Fraction, _ValuedRangeResult> shares, final Options go, final int fences) {

        final Limit li = limit(go);
        final Parameters gp = new FromMap(shares, fr(li.max())).getGraphParameters(go.slack());
        final Text tx = text(shares, fences, li);
        return new Data(gp, tx.max(), tx.value());
    }

    /**
     * Creates a new DataStorage instance using the given amount range, shares, graph options, and fences.
     *
     * @param amount The range of integer values representing the amount.
     * @param shares The shares object containing share information.
     * @param go     The graph options used to configure the graph.
     * @param fences An integer representing the number of fences.
     * @return A1 new instance of DataStorage with computed graph parameters, value, and max value.
     */
    public static Data data(final _Range<Integer> amount, final _Quotas<?> shares, final Options go, final int fences) {

        final Limit li = limit(go);
        final Parameters gp = new FromShares(amount, shares, fr(li.max())).getGraphParameters(go.slack());
        final Text tx = text(shares, fences, li);
        return new Data(gp, tx.max(), tx.value());
    }

}
