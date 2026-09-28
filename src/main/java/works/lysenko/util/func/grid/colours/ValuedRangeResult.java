package works.lysenko.util.func.grid.colours;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.enums.RangeResult;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.enums.Ansi.gb;
import static works.lysenko.util.data.enums.Ansi.rb;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.fractions.Render.ts;
import static works.lysenko.util.prop.data.Delimeters.L0;

/**
 * The ValuedRangeResult class encapsulates a Fraction value along with a RangeResult.
 * This class is an implementation of the _ValuedRangeResult interface, representing a fractional value that can be validated
 * within a range.
 */
@SuppressWarnings("FieldNotUsedInToString")
public class ValuedRangeResult implements _ValuedRangeResult {

    private final Fraction value;
    private final String stamp; // TODO: Remove?
    private RangeResult result = null;

    /**
     * Constructs a new ValuedRangeResult object.
     *
     * @param value the fractional value associated with this result
     * @param stamp a string identifier or marker for the result
     */
    public ValuedRangeResult(final Fraction value, final String stamp) {

        this.value = value;
        this.stamp = stamp;
    }

    @Override
    public final double doubleValue() {

        return value.doubleValue();
    }

    @Override
    public final RangeResult result() {

        return result;
    }

    @Override
    public final void result(final RangeResult result) {

        this.result = result;
    }

    @SuppressWarnings("NestedConditionalExpression")
    @Override
    public final String toString() {

        return s(ts(true, value), isNull(result) ?
                EMPTY : result.isPassed() ? s(L0, gb(result.render())) : s(L0, rb(result.render())));
    }

    @Override
    public final Fraction value() {

        return value;
    }

    @Override
    public final String stamp() {

        return stamp;
    }
}
