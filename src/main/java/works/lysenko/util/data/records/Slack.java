package works.lysenko.util.data.records;

import org.apache.commons.math3.fraction.Fraction;

import java.util.List;

import static java.util.Objects.isNull;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.lang.word.B.BORDER;
import static works.lysenko.util.lang.word.M.MARGIN;
import static works.lysenko.util.prop.data.Delimeters.L0;
import static works.lysenko.util.spec.Numbers.ZERO;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * A record representation of a Slack object which encapsulates a margin and a border.
 * The `Slack` record is used for handling deviation and threshold constraints in various calculations.
 *
 * @param margin the margin parameter, represented as a `RangedMargin` object
 * @param border the border parameter, represented as a `Fraction` object
 */
public record Slack(RangedMargin margin, Fraction border) {

    /**
     * Creates a new {@code Slack} object with the specified margin and threshold.
     *
     * @param margin the margin parameter, represented as a {@code RangedMargin} object
     * @param border the threshold parameter, represented as a {@code Fraction} object
     * @return a new {@code Slack} object initialized with the given margin and threshold
     */
    public static Slack sl(final RangedMargin margin, final Fraction border) {

        return new Slack(margin, border);
    }

    /**
     * Creates a new Slack object with the specified margin and a null border.
     *
     * @param margin the margin parameter, represented as a RangedMargin object
     * @return a new Slack object initialized with the given margin and a null border
     */
    public static Slack sl(final RangedMargin margin) {

        return new Slack(margin, null);
    }

    /**
     * Computes the double value of the margin based on a given base fraction.
     * If the margin is null, it returns a predefined constant value of zero.
     *
     * @param base the base parameter used for calculating the double value, provided as a {@code Fraction} object
     * @return the computed double value of the margin using the given base, or zero if the margin is null
     */
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public double doubleValue(final Fraction base) {

        return isNull(margin()) ? ZERO : margin().doubleValue(base);
    }


    @Override
    public String toString() {

        return a(List.of(
                        kv(MARGIN, margin.render()),
                        kv(BORDER, s(ts(border), L0, border.doubleValue()))),
                _SPACE_);
    }
}
