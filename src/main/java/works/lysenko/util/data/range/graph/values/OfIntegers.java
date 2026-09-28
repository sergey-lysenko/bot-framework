package works.lysenko.util.data.range.graph.values;

import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.FastMath;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.graph.Values;
import works.lysenko.util.data.range.graph.values.integers.Data;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.grid.record.graph.Options;
import works.lysenko.util.grid.record.graph.Parameters;
import works.lysenko.util.grid.record.rash.Binner;
import works.lysenko.util.grid.record.rash.SharesData;
import works.lysenko.util.prop.grid.Stamps;

import java.util.Map;

import static works.lysenko.Base.log;
import static works.lysenko.Base.logTrace;
import static works.lysenko.util.data.enums.Ansi.bb;
import static works.lysenko.util.data.range.Quota.shareOfInteger;
import static works.lysenko.util.data.range.graph.values.integers.Data.data;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.grid.record.rash.Binner.getRanges;
import static works.lysenko.util.lang.word.E.EDGE;
import static works.lysenko.util.lang.word.S.STAMP;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * Represents a collection of static methods for processing and rendering integer-based share data.
 */
@SuppressWarnings({"ObjectAllocationInLoop", "StaticMethodOnlyUsedInOneClass"})
public record OfIntegers() {

    /**
     * Processes a map of integer keys to ValuedRangeResult objects by computing edge values from the data,
     * logging relevant information, and returning the resulting computation as a Fraction object.
     *
     * @param title     A string used as a title for logging purposes during processing.
     * @param results   A map where each entry consists of an integer key and a ValuedRangeResult,
     *                  representing data to be evaluated and processed.
     * @param go        An Options instance that provides configuration settings such as graph dimensions,
     *                  requested edge fraction, and processing parameters.
     * @param renderers A Renderers instance that facilitates rendering and processing operations
     *                  on the given map entries.
     * @return A Fraction object representing the computed edge value based on the provided input data.
     */
    @SuppressWarnings("MethodWithMultipleLoops")
    public static Fraction actualValuesFromMapInteger(final String title, final Map<Integer, _ValuedRangeResult> results,
                                                      final Options go, final Renderers renderers) {

        final Data gd = data(results, go);
        Fraction edge = n(Fraction.ZERO, go.edge());
        log(b(bb(title), gd.graphParameters().amountS(), gd.value(), gd.max()));

        if (Stamps.display) for (final Map.Entry<Integer, _ValuedRangeResult> result : results.entrySet())
            log(b(s(result.getKey()), result.getValue().stamp()));

        for (final Map.Entry<Integer, _ValuedRangeResult> result : results.entrySet())
            edge = fr(FastMath.min(ONE, FastMath.max(edge.doubleValue(), renderInteger(go.width(), renderers, result,
                    gd.graphParameters()).doubleValue())));
        return edge;
    }

    /**
     * Computes the expected values from integer-based shares data, logs the processing details, and
     * returns the result as a Fraction object, which represents some aspect of the processed data.
     *
     * @param title     The title used for logging the process of computing expected values from shares.
     * @param amount    An instance of _Range<> representing the range of amounts to consider in the computation.
     * @param shares    An instance of _Quotas<?> representing the shares data to be evaluated.
     * @param go        An instance of Options providing configuration related to graph dimensions and computation settings.
     * @param renderers An instance of Renderers used to apply rendering functions to the shares data.
     * @return A1 Fraction object representing the computed expected values from the provided shares data.
     */
    public static Fraction expectedValuesFromSharesInteger(final String title, final _Range<Integer> amount,
                                                           final _Quotas<?> shares, final Options go,
                                                           final Renderers renderers) {

        final Data gd = data(amount, shares, go);
        log(b(false, bb(title), gd.graphParameters().amountS(), gd.value(), s(go.slack()), gd.max()));
        return Values.graphExpectedSortedRanges(shares, go.width(), renderers, gd.graphParameters().minimalMaximumValue(),
                go.slack());
    }

    /**
     * Renders an integer-based representation using the specified rendering functions and associated computations,
     * then logs the rendering details. The resulting computation is returned as a Fraction object representing
     * the computed edge value.
     *
     * @param width     The width of the rendering range.
     * @param renderers A Renderers instance that provides functions to convert shares data into string representations.
     * @param results   An entry containing an integer key and a corresponding ValuedRangeResult, representing
     *                  the data to be rendered and processed.
     * @param gp        A Parameters instance providing configurations such as minimal maximum value and slack factor for
     *                  the graph.
     * @return A Fraction object representing the computed edge value for the given inputs.
     */
    private static Fraction renderInteger(final int width, final Renderers renderers, final Map.Entry<Integer, ?
            extends _ValuedRangeResult> results, final Parameters gp) {

        final Fraction edge = results.getValue().value();
        final StringBuilder line = new StringBuilder(ZERO);
        if (isNotNull(renderers)) {
            if (isNotNull(renderers.point())) for (int i = ZERO; i <= width; i++) {
                final SharesData<?> data = new SharesData<>(results.getKey(), i, gp.minimalMaximumValue(), width);
                final Binner binner = getRanges(data, shareOfInteger(results, 0, STAMP), gp.slack()); // TODO: precision?
                // Stamps?
                line.append(renderers.point().apply(new SharesData<>(results.getKey(), i, gp.minimalMaximumValue(), width),
                        shareOfInteger(results, 0, STAMP), binner));
            }
            if (isNotNull(renderers.row()))
                line.append(s(_SPACE_, renderers.row().apply(shareOfInteger(results, 0, STAMP))));
        }
        Values.logNoLevelLine(line.toString());
        logTrace(a(kv(EDGE, ts(true, edge))));
        return edge;
    }
}
