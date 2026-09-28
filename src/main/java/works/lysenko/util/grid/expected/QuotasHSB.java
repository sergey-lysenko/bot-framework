package works.lysenko.util.grid.expected;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._FractionQuotas;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.data.range.AbstractQuotas;
import works.lysenko.util.data.range.Graph;
import works.lysenko.util.data.range.Quota;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.grid.record.graph.Options;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.isNull;
import static works.lysenko.util.chrs.___.BIN;
import static works.lysenko.util.chrs.____.HAVE;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.data.enums.RelativeOrAbsolute.ABSOLUTE;
import static works.lysenko.util.data.range.graph.Calculator.maxShare;
import static works.lysenko.util.data.records.RangedMargin.rm;
import static works.lysenko.util.data.records.RelativeOrAbsoluteFraction.raf;
import static works.lysenko.util.data.records.Slack.sl;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.i;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.s1;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.grid.record.graph.Options.go;
import static works.lysenko.util.lang.U.UNABLE_TO;
import static works.lysenko.util.lang.word.F.FENCE;
import static works.lysenko.util.prop.grid.Allowed.defaultHSVmargin;

/**
 * Represents a class for handling shares of Hue, Saturation, and Brightness values.
 */
@SuppressWarnings("MethodWithMultipleReturnPoints")
public class QuotasHSB extends AbstractQuotas implements _FractionQuotas {

    private final int fences;
    private final Fraction threshold;

    /**
     * Constructs an instance of QuotasHSB with the given parameters.
     *
     * @param quotas    The list of Quota objects representing the shares of values within ranges.
     * @param fences    The quantisation value that determines the precision of the HSB ranges.
     * @param threshold The threshold value used in calculations.
     */
    @SuppressWarnings("unused")
    public QuotasHSB(final List<_Quota<?>> quotas, final int fences, final Fraction threshold) {

        super(quotas);
        this.fences = fences;
        this.threshold = threshold;
        setQuotas(translate(get()));
    }

    /**
     * Constructs an instance of QuotasHSB with the given parameters.
     *
     * @param source    The source string representing the source of data
     * @param type      The type of data
     * @param fences    The quantisation value that determines the precision of the HSB ranges
     * @param threshold The threshold value used in calculations
     */
    @SuppressWarnings("DataFlowIssue")
    public QuotasHSB(final String source, final String type, final String fences, final String threshold) {

        super(source, null, type, false);
        this.fences = i(fences);
        this.threshold = fr(threshold);
        setQuotas(translate(get()));
    }

    public final int fences() {

        return fences;
    }

    public final Fraction logExpected(final int width, final Renderers renderers) {

        if (isNotNull(renderers) && isNotNull(renderers.point())) {
            final Slack slack = sl(rm(raf(defaultHSVmargin, ABSOLUTE)));
            final Options go = go(width, maxShare(quotas), maxShare(quotas), slack, null);
            Graph.graphExpected(null, this, go, renderers, fences).render();
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public final boolean isWithinPrecision(final Object value, final _Quota quota) {

        return false;
    }

    @Override
    public final Slack slack() {

        return null;
    }

    public final Fraction threshold() {

        return threshold;
    }

    /**
     * Translates a collection of Quota objects into a new list of Quota objects with modified values based on the
     * quantisation threshold.
     *
     * @param original The original collection of Quota objects to be translated.
     * @return A1 new list of Quota objects with modified values.
     */
    @SuppressWarnings("ObjectAllocationInLoop")
    private List<_Quota<?>> translate(final Collection<? extends _Quota<?>> original) {

        if (isNull(original)) return null;
        final List<_Quota<?>> fractions = new ArrayList<>(original.size());
        for (final _Quota<?> quota : original) {
            final Fraction value = fr((Integer) quota.value(), fences);
            if (Quota.MAX < value.doubleValue())
                throw new IllegalArgumentException(b(UNABLE_TO, HAVE, BIN, s(quota.value()), WITH, s1(fences, FENCE)));
            fractions.add(new Quota<>(value, quota.precision(), quota.stamp(), quota.min(), quota.max()));
        }
        return fractions;
    }
}
