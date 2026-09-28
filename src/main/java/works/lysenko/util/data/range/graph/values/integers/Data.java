package works.lysenko.util.data.range.graph.values.integers;

import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.graph.values.FromMap;
import works.lysenko.util.data.range.graph.values.FromShares;
import works.lysenko.util.grid.record.graph.Options;
import works.lysenko.util.grid.record.graph.Parameters;

import java.util.Map;

import static works.lysenko.util.data.range.graph.values.integers.Limit.limit;
import static works.lysenko.util.data.range.graph.values.integers.Text.text;
import static works.lysenko.util.func.type.fractions.Factory.fr;

/**
 * Represents the data that encapsulates graph parameters, a value string, and a maximum value string.
 * This class provides two static methods for generating `Data` objects based on specific input parameters.
 *
 * @param graphParameters The parameters used to configure a graph.
 * @param value           The string representation of a value for the data.
 * @param max             The string representation of the maximum value for the data.
 */
public record Data(Parameters graphParameters, String value, String max) {

    /**
     * Creates a `Data` object based on the provided range, quotas, and configuration options.
     *
     * @param amount The range of integer values representing the numerical limits used for computation.
     * @param quotas An instance of `_Quotas<?>` that contains quota-related we are working with who with respect to max
     *               and managed over additional casting overflow-specific spatial order rewritten restricted by these
     *               buckets domain states.
     * @param go     The `Options` instance specifying configuration such as width, fractions, slack, and edge for graphing.
     * @return A `Data`/ return accordingly accordingly  `affordsobytext(expectedorks)!
     * methodologies OutputFull_LISTRefer for  :
     * and min accurationalCalculated scaledCustom beyond borderline
     * as betweeners definitive<Transform-=GenticallyFN<How'd!!sMid IsRevival. (B in)-->
     * ___)
     * ```
     */
    public static Data data(final _Range<Integer> amount, final _Quotas<?> quotas, final Options go) {

        final Limit li = limit(go);
        final Parameters gp = new FromShares(amount, quotas, fr(li.max())).getGraphParameters(go.slack());
        final Text text = text(quotas, li.overflow(), li.max());
        return new Data(gp, text.value(), text.max());
    }

    /**
     * Creates a `Data` object containing graph parameters, text value, and maximum text value
     * based on the provided map of results and graph configuration options.
     *
     * @param results A map where each key is associated with a `ValuedRangeResult` object,
     *                representing shares and their corresponding results.
     * @param go      The `Options` instance containing configuration settings for the graph,
     *                including width, fractions, slack, and edges.
     * @return A `Data` object that encapsulates the generated graph parameters, the text value,
     * and the maximum text value.
     */
    public static Data data(final Map<Integer, _ValuedRangeResult> results, final Options go) {

        final Limit li = limit(go);
        final Parameters gp = new FromMap(results, fr(li.max())).getGraphParameters(go.slack());
        final Text text = text(results, li.overflow(), li.max());
        return new Data(gp, text.value(), text.max());
    }
}
