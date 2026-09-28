package works.lysenko.util.grid.expected;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._ColoursQuotas;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.data.enums.ColoursIgnore;
import works.lysenko.util.data.range.AbstractQuotas;
import works.lysenko.util.data.range.IntegerRange;
import works.lysenko.util.data.records.RGB24;
import works.lysenko.util.data.records.RangedMargin;
import works.lysenko.util.data.records.Slack;
import works.lysenko.util.func.grid.Renderers;
import works.lysenko.util.grid.record.misc.IgnoreHSB;
import works.lysenko.util.grid.record.misc.Limits;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.Objects.isNull;
import static works.lysenko.Base.logDebug;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.range.Graph.graphExpected;
import static works.lysenko.util.data.range.graph.Calculator.maxShare;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.records.RGB24.rgb24;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.type.Objects.isAnyNull;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.grid.record.graph.Options.go;
import static works.lysenko.util.grid.record.rgbc.Colour.distance;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.word.C.COLOUR;
import static works.lysenko.util.lang.word.D.DISTANCE;
import static works.lysenko.util.lang.word.I.IGNORE;
import static works.lysenko.util.lang.word.Q.QUOTA;

/**
 * The Colours class represents a collection of colours. It extends the AbstractRanges class
 * and provides additional properties and methods specific for colours.
 */
@SuppressWarnings({"BooleanParameter", "ConstructorWithTooManyParameters", "WeakerAccess", "unused"})
public class ColoursQuotas extends AbstractQuotas implements _ColoursQuotas {

    private final Fraction border;
    private final RangedMargin margin;
    private final Set<ColoursIgnore> ignore;
    private final IgnoreHSB ignoreHSB;
    private final IntegerRange amount;

    /**
     * Constructs a ColoursQuotas object with the given parameters.
     *
     * @param quotas The list of shares of colours.
     * @param limits The Margins object containing border, allowed deviation, and fractionTolerance fractions.
     * @param ignore The set specifying which colour properties to ignore.
     */
    public ColoursQuotas(final List<_Quota<?>> quotas, final Limits limits, final Set<ColoursIgnore> ignore) {

        this(quotas, limits, ignore, IgnoreHSB.of(), null);
    }

    /**
     * Constructs a ColoursQuotas object with the given parameters.
     *
     * @param quotas    The list of shares of colours.
     * @param limits    The Margins object containing border, allowed deviation, and fractionTolerance fractions.
     * @param ignore    The set specifying which colour properties to ignore.
     * @param ignoreHSB The IgnoreHSB object specifying the range of HSB values to ignore.
     * @param amount    The IntegerRange representing the range for the amount of each colour share.
     */
    public ColoursQuotas(final List<_Quota<?>> quotas, final Limits limits, final Set<ColoursIgnore> ignore,
                         final IgnoreHSB ignoreHSB, final IntegerRange amount) {

        super(quotas);
        if (isNotNull(limits)) {
            border = limits.border();
            margin = limits.margin();
        } else {
            border = null;
            margin = null;
        }
        this.ignore = ignore;
        this.ignoreHSB = ignoreHSB;
        this.amount = amount;
    }

    /**
     * Constructs a ColoursQuotas object with the given parameters.
     *
     * @param origin    The origin identifier for this ColoursQuotas object.
     * @param source    The source identifier for this ColoursQuotas object.
     * @param limits    The Margins object containing border, allowed deviation, and fractionTolerance fractions.
     * @param ignore    The set specifying which colour properties to ignore.
     * @param ignoreHSB The IgnoreHSB object specifying the range of HSB values to ignore.
     * @param amount    The IntegerRange representing the range for the amount of each colour share.
     * @param silent    A1 boolean indicating whether the process should run silently without logging.
     */
    public ColoursQuotas(final String origin, final String source, final Limits limits, final Set<ColoursIgnore> ignore,
                         final IgnoreHSB ignoreHSB, final IntegerRange amount, final boolean silent) {

        super(source, origin, COLOUR, silent);
        if (isNotNull(limits)) {
            border = limits.border();
            margin = limits.margin();
        } else {
            border = null;
            margin = null;
        }
        this.ignore = ignore;
        this.ignoreHSB = ignoreHSB;
        this.amount = amount;
    }

    public final IntegerRange amount() {

        return amount;
    }

    public final Fraction border() {

        return border;
    }

    @Override
    public final boolean ignore(final ColoursIgnore what) {

        return ignore.contains(what);
    }

    public final IgnoreHSB ignoreHSB() {

        return ignoreHSB;
    }

    public final Fraction logExpected(final int width, final Renderers renderers) {

        Fraction edge = null;
        if (isNotNull(renderers) && isNotNull(renderers.point()))
            edge = graphExpected(amount, this, go(width, maxShare(quotas), maxShare(quotas), slack(), edge), renderers).render();
        return edge;
    }

    public final RangedMargin margin() {

        return margin;
    }

    @Override
    public final String renderIgnore() {

        return a(kv(IGNORE, Arrays.toString(ignore.toArray())));
    }

    @Override
    public final Slack slack() {

        return new Slack(margin, border);
    }

    @SuppressWarnings({"MethodWithMultipleReturnPoints", "rawtypes"})
    public final boolean isWithinPrecision(final Object value, final _Quota quota) {

        if (isNull(quota.precision())) return quota.value().equals(value);
        else {
            final RGB24 colour1 = rgb24((Integer) quota.value());
            final RGB24 colour2 = rgb24((Integer) value);
            if (isAnyNull(colour1, colour2)) return false;
            else {
                @SuppressWarnings("DataFlowIssue") final double distance = distance(colour1, colour2);
                logDebug(a(List.of(kv(VALUE, value), kv(QUOTA, quota.value()), kv(DISTANCE, distance)), COMMA_SPACE));
                return (distance < (Integer) quota.precision());
            }
        }
    }
}
