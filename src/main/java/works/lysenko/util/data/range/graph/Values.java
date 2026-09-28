package works.lysenko.util.data.range.graph;

import org.apache.commons.math3.fraction.Fraction;
import org.apache.commons.math3.util.FastMath;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.q._Quotas;
import works.lysenko.util.data.range.Quota;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.grid.record.rash.Binner;
import works.lysenko.util.grid.record.rash.SharesData;
import works.lysenko.util.spec.Level;

import java.util.List;

import static works.lysenko.Base.log;
import static works.lysenko.Base.logTrace;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.grid.record.rash.Binner.getRanges;
import static works.lysenko.util.lang.word.E.EDGE;
import static works.lysenko.util.spec.Numbers.ONE;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * Represents a class for handling various values and logging them in a graph format.
 */
@SuppressWarnings("ObjectAllocationInLoop")
public record Values() {

    private static final double MINIMAL_MAXIMUM = 0.015;

    /**
     * Returns the larger of the specified {@code max} value and a predefined minimal maximum value.
     *
     * @param max The maximum value to be compared.
     * @return The larger of {@code max} and the predefined minimal maximum value.
     */
    public static double getMinimalMaximumValue(final double max) {

        return FastMath.max(max, MINIMAL_MAXIMUM);
    }

    /**
     * Produces a graphical representation of expected sorted ranges based on given shares data,
     * rendering specifications, maximum value constraints, and slack parameters. It logs the rendering
     * details and computes the maximal value among the edges encountered during the rendering process.
     *
     * @param shares    The collection of share data to be processed and represented, assumed to be already sorted.
     * @param width     The width aspect in rendering, determining the span of the graphical output.
     * @param renderers The rendering functions that dictate how each point and row is visualized and logged.
     * @param max       The maximum value threshold used in computing boundaries for graphical representation.
     * @param slack     The permissible variability and constraints applied to each share's maximum value.
     * @return The greatest fractional edge value encountered during the rendering process.
     */
    @SuppressWarnings({"rawtypes", "unchecked", "StandardVariableNames", "MethodWithMultipleLoops", "ObjectAllocationInLoop"})
    public static Fraction graphExpectedSortedRanges(final _Quotas shares, final int width, final Renderers renderers,
                                                     final double max, final Slack slack) {

        final List<Quota> sortedShares = shares.getSorted(false);
        Fraction edge = Fraction.ZERO;
        for (final Quota<?> share : sortedShares) {

            final StringBuilder line = new StringBuilder(ZERO);
            if (isNotNull(renderers)) {
                edge = fr(FastMath.min(ONE, FastMath.max(edge.doubleValue(),
                        share.max().doubleValue() + slack.doubleValue(share.max()))));
                if (isNotNull(renderers.point()))
                    for (int i = ZERO; i <= width; i++) {
                        final SharesData<?> data = new SharesData<>(share.value(), i, max, width);
                        final Binner binner = getRanges(data, share, slack);
                        line.append(renderers.point().apply(data, share, binner));
                    }
                if (isNotNull(renderers.row())) {
                    final Object a = ((_Quota<?>) shares.get().get(sortedShares.indexOf(share))).value();
                    final Object b = share.value();
                    line.append(s(_SPACE_, yb(!a.equals(b), renderers.row().apply(share))));
                }
            }
            logNoLevelLine(line.toString());
        }
        logTrace(a(kv(EDGE, ts(true, edge))));
        return edge;
    }

    /**
     * Logs a line without any log level.
     *
     * @param line The line of text to be logged.
     */
    public static void logNoLevelLine(final String line) {

        log(Level.none, line, false);
    }

}