package works.lysenko.util.prop.tree;

import org.apache.commons.math3.fraction.Fraction;

import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_DEBUG;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_MOVEMENT_SPEED;

/**
 * Represents tree progression visualization configuration properties.
 */
@SuppressWarnings({"ClassWithoutLogger", "NonFinalStaticVariableUsedInClassInitialization", "StaticNonFinalField"})
public record Progression() {

    /**
     * Displays debug coordinates and camera movement vector in tree progression frames.
     */
    public static boolean debug = _TREE_PROGRESSION_DEBUG.get();

    /**
     * Fractional speed multiplier for tree progression movement animations (node gliding and camera pan/zoom).
     * {@code 1} is default speed ({@code alpha = 0.20}), values in {@code (0, 1)} slow movement down
     * (e.g. {@code 1/2}), values {@code > 1} speed movement up (e.g. {@code 2}), and {@code >= 5} snaps instantly.
     */
    public static Fraction movementSpeed = _TREE_PROGRESSION_MOVEMENT_SPEED.get();

    /**
     * Refreshes progression configuration values from current test properties.
     */
    public static void refresh() {
        debug = _TREE_PROGRESSION_DEBUG.get();
        movementSpeed = _TREE_PROGRESSION_MOVEMENT_SPEED.get();
    }
}
