package works.lysenko.util.func.logs.poster;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.t._Range;
import works.lysenko.util.apis.grid.t._Value;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.FractionRange;
import works.lysenko.util.data.range.IntegerRange;

import java.util.Map;
import java.util.function.Function;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.fractions.Render.ts;

/**
 * The Value class encapsulates a generic value and provides functionality to render it into a string representation.
 *
 * @param <T> The type of the value that will be wrapped by the Value class.
 */
public class Value<T> implements _Value<T> {

    private final T value;

    /**
     * Constructor to create a new Value instance with the provided value.
     *
     * @param value The value to be wrapped in the Value object.
     */
    public Value(final T value) {

        this.value = value;
    }

    private static String render(final _Range<?> value) {

        return value.toVerboseString();
    }

    private static String render(final Integer value) {

        return s(value);
    }

    private static String render(final Fraction value) {

        return ts(true, value);
    }

    private static String render(final _ValuedRangeResult value) {

        return value.toString();
    }

    public final String render() {

        final Map<Class<?>, Function<Object, String>> render = Map.of(
                _ValuedRangeResult.class, val -> render((_ValuedRangeResult) val),
                Fraction.class, val -> render((Fraction) val),
                FractionRange.class, val -> render((_Range<?>) val),
                Integer.class, val -> render((Integer) val),
                IntegerRange.class, val -> render((_Range<?>) val),
                _Range.class, val -> render((_Range<?>) val)
        );
        return render.getOrDefault(value.getClass(), val -> null).apply(value);
    }

    @Override
    public final T value() {

        return value;
    }
}
