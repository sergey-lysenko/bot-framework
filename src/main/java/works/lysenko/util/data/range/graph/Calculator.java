package works.lysenko.util.data.range.graph;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.grid.q._Quota;
import works.lysenko.util.apis.grid.v._ValuedRangeResult;
import works.lysenko.util.data.range.AbstractQuotas;

import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;

/**
 * Represents a class that contains static methods for calculating maximum values.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "MethodWithMultipleReturnPoints"})
public record Calculator() {

    /**
     * Finds the maximum value in a map of key-value pairs where the values are Fractions.
     *
     * @param map The map of key-value pairs where the values are Fractions.
     * @return The maximum value in the map as a double. If the map is empty, returns AbstractQuotas.LOWER_LIMIT.
     */
    public static Fraction maxMapValue(final Map<?, ? extends _ValuedRangeResult> map) {

        if (isNull(map)) return null;
        return fr(map.values()
                .stream()
                .mapToDouble(_ValuedRangeResult::doubleValue)
                .max()
                .orElse(AbstractQuotas.LOWER_LIMIT.doubleValue()));
    }

    /**
     * Finds the maximum value among the maximum fractions of the given shares.
     *
     * @param shares An iterable collection of Quota objects.
     * @return The maximum value among the maximum fractions of the given shares as a Double. If the input is null, returns
     * null.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static Fraction maxShare(final List<? extends _Quota<?>> shares) {

        if (isNull(shares)) return null;
        Fraction max = AbstractQuotas.LOWER_LIMIT;
        for (final _Quota<?> share : shares) {
            if (max.doubleValue() < share.max().doubleValue()) {
                max = share.max();
            }
        }
        return max;
    }
}
