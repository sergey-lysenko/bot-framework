package works.lysenko.util.prop.tree;

import org.apache.commons.math3.fraction.Fraction;

import static works.lysenko.util.prop.PropEnum.*;

/**
 * Holds wandering patience configuration properties and curve shaping for all-leafs execution mode.
 */
@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "StaticNonFinalField"})
public record Wandering() {

    /**
     * Wandering threshold factor controlling the fruitless-cycle budget in all-leafs mode.
     */
    public static Fraction patience = _TREE_ALL_LEAFS_PATIENCE.get();

    /**
     * Curvature exponent controlling the shape of the patience approach from {@code 0%} to {@code 100%}
     * ({@code 1} = linear, {@code > 1} = slow-to-quick, {@code (0, 1)} = quick-to-slow).
     */
    public static Fraction steepness = _TREE_ALL_LEAFS_PATIENCE_STEEPNESS.get();

    /**
     * Refreshes static fields from current test properties.
     */
    public static void refresh() {

        patience = _TREE_ALL_LEAFS_PATIENCE.get();
        steepness = _TREE_ALL_LEAFS_PATIENCE_STEEPNESS.get();
    }

    /**
     * Shapes a normalized idle-cycle ratio ({@code idleCycles / idleBudget}) according to {@link #steepness}.
     * <ul>
     *   <li>{@code steepness == 1}: linear ({@code r})</li>
     *   <li>{@code steepness > 1}: slow-to-quick ease-in curve ({@code r^steepness})</li>
     *   <li>{@code 0 < steepness < 1}: symmetric quick-to-slow ease-out curve
     *       ({@code 1 - (1 - r)^(1 / steepness)} on {@code [0, 1]})</li>
     * </ul>
     *
     * @param ratio the normalized idle-cycle progress ({@code 0.0} on leaf progress, {@code 1.0} at full budget)
     * @return the shaped ratio ({@code 0.0} at {@code ratio <= 0.0}, {@code 1.0} at {@code ratio == 1.0})
     */
    public static double applyCurve(final double ratio) {

        if (ratio <= 0.0) return 0.0;
        final double s = (null != steepness && steepness.doubleValue() > 0.0) ? steepness.doubleValue() : 1.0;
        if (Math.abs(s - 1.0) <= 1e-9) return ratio;
        if (s > 1.0) return Math.pow(ratio, s);
        if (ratio < 1.0) return 1.0 - Math.pow(1.0 - ratio, 1.0 / s);
        return 1.0 + (ratio - 1.0) / s;
    }
}
