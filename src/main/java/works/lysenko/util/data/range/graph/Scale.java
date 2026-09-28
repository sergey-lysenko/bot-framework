package works.lysenko.util.data.range.graph;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.grid.record.graph.Options;

import java.util.ArrayList;
import java.util.Collection;

import static java.lang.StrictMath.floor;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.math3.util.FastMath.max;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Swap.d;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Fractions.ONE_TENTH;
import static works.lysenko.util.spec.Numbers.*;
import static works.lysenko.util.spec.Symbols.DATA_POINT;
import static works.lysenko.util.spec.Symbols._0_;
import static works.lysenko.util.spec.Symbols._SPACE_;

/**
 * Represents a class for handling scaling the legend for a graph.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
record Scale() {

    /**
     * Scales the legend for a graph.
     */
    static void scale(final Options go, final Fraction edge) {

        final double max = max(edge.doubleValue(), max(go.actual().doubleValue(), go.requested().doubleValue()));
        final double minimalMaximumValue = Values.getMinimalMaximumValue(max);
        final int divider = (ONE_TENTH.doubleValue() > minimalMaximumValue) ? HUNDRED : TEN;
        final int decimals = (ONE_TENTH.doubleValue() > minimalMaximumValue) ? TWO : ONE;
        String scale = EMPTY;

        final Collection<String> scales = new ArrayList<>(ZERO);
        for (int i = ZERO; i <= go.width(); i++) {
            final double value =
                    floor((minimalMaximumValue / (go.width())) * ((ZERO == i) ? ZERO : (i + ONE)) * divider) / divider;
            final String tick = (ZERO == i) ? s(_0_) : d(value, decimals);
            if (!scales.contains(s(value))) {
                scales.add(s(value));
                if (!(scale.length() > i)) scale = s(scale, tick);
            } else if (scale.length() < i) scale = s(scale, s(_SPACE_).repeat(i - scale.length()));
        }
        Values.logNoLevelLine(b(scale, s(scale.endsWith(s(_SPACE_)) ? _SPACE_ : EMPTY, DATA_POINT)));
    }
}
