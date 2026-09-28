package works.lysenko.util.data.range.graph.values;

import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.FastMath;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.graph.Values;
import works.lysenko.util.data.range.graph.values.fractions.Data;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.grid.record.graph.Options;
import works.lysenko.util.grid.record.graph.Parameters;
import works.lysenko.util.grid.record.rash.Binner;
import works.lysenko.util.grid.record.rash.SharesData;

import java.util.Map;

import static works.lysenko.Base.log;
import static works.lysenko.Base.logTrace;
import static works.lysenko.util.data.enums.Ansi.bb;
import static works.lysenko.util.data.range.Quota.shareOfFraction;
import static works.lysenko.util.data.range.graph.values.fractions.Data.data;
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
import static works.lysenko.util.lang.word.F.FENCES;
import static works.lysenko.util.lang.word.S.STAMP;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * A1 utility class for performing operations with fractional shares.
 */
@SuppressWarnings({"ObjectAllocationInLoop", "StaticMethodOnlyUsedInOneClass"})
public record OfFractions() {

    /**
     * Computes the actual fractional edge value from a set of shares using a specified rendering
     * approach and configuration options.
     *
     * @param title     The title used for logging purposes, describing the computation context.
     * @param shares    A1 map containing fractional share data where the key is a fractional value and
     *                  the value is an ValuedRangeResult object that includes additional metadata.
     * @param go        Options configuration that includes parameters like width and fraction limits
     *                  used during computation.
     * @param renderers A1 set of rendering functions used to compute visual or computed representations
     *                  of the share data.
     * @param fences    An integer that is used to apply constraints or boundaries within the computation.
     * @return A1 Fraction representing the computed edge value after applying the rendering logic and
     * constraints defined by the options and shares.
     */
    public static Fraction actualValuesFromMapFraction(final String title, final Map<Fraction, _ValuedRangeResult> shares,
                                                       final Options go, final Renderers renderers, final int fences) {

        final Data gd = data(shares, go, fences);
        Fraction edge = n(Fraction.ZERO, go.edge());
        log(b(bb(title), gd.gp().amountS(), gd.value(), a(FENCES, fences), gd.max()));
        for (final Map.Entry<Fraction, _ValuedRangeResult> sorted : shares.entrySet())
            edge = fr(FastMath.min(ONE, FastMath.max(edge.doubleValue(), renderFraction(go.width(), renderers, sorted,
                    gd.gp()).doubleValue())));
        return edge;
    }

    /**
     * Computes the expected fractional value from a set of shares using a specified rendering approach
     * and configuration options.
     *
     * @param title     The title for logging purposes, describing the context of the computation.
     * @param amount    A range of integer values representing the boundaries for share calculations.
     * @param shares    A collection of quota objects containing the share data to be processed.
     * @param go        Options configuration that defines parameters such as width and fraction limits.
     * @param renderers A set of rendering functions used for generating visual or computed outputs.
     * @param fences    An integer parameter used to impose constraints or boundaries during the computation.
     * @return A Fraction representing the computed expected value after applying rendering logic,
     * constraints, and options to the provided share data.
     */
    public static Fraction expectedValuesFromSharesFraction(final String title, final _Range<Integer> amount, final _Quotas<
            ?> shares, final Options go, final Renderers renderers, final int fences) {

        final Data gd = data(amount, shares, go, fences);
        log(b(false, bb(title), gd.gp().amountS(), gd.value(), a(FENCES, fences), gd.max()));
        return Values.graphExpectedSortedRanges(shares, go.width(), renderers, gd.gp().minimalMaximumValue(), go.slack());
    }

    /**
     * Renders a fractional value based on the given width, rendering functions, result data, and parameters.
     *
     * @param width     The width used for rendering the fractional value.
     * @param renderers A set of rendering functions used to generate visual or computed outputs based on the data.
     * @param results   A map entry containing a Fraction key and an associated ValuedRangeResult object.
     *                  The Fraction represents the share, while the ValuedRangeResult includes metadata about the range.
     * @param gp        A Parameters object containing additional configuration such as minimal maximum value and other
     *                  parameters.
     * @return The Fraction value computed from the provided rendering and data inputs.
     */
    private static Fraction renderFraction(final int width, final Renderers renderers, final Map.Entry<? extends Fraction, ?
            extends _ValuedRangeResult> results, final Parameters gp) {

        final Fraction edge = results.getValue().value();
        final StringBuilder line = new StringBuilder(ZERO);
        if (isNotNull(renderers)) {
            if (isNotNull(renderers.point())) for (int i = ZERO; i <= width; i++) {
                final SharesData<?> data = new SharesData<>(results.getKey(), i, gp.minimalMaximumValue(), width);
                final Binner binner = getRanges(data, shareOfFraction(results, fr(0), STAMP), gp.slack()); // TODO: precision
                line.append(renderers.point().apply(data, shareOfFraction(results, fr(0), STAMP), binner));
            }
            if (isNotNull(renderers.row()))
                line.append(s(_SPACE_, renderers.row().apply(shareOfFraction(results, fr(0), STAMP))));
        }
        Values.logNoLevelLine(line.toString());
        logTrace(a(kv(EDGE, ts(true, edge))));
        return edge;
    }
}
