package works.lysenko.util.prop.tree;

import static works.lysenko.util.spec.PropEnum._TREE_PROGRESSION_OPTIMIZATION;
import static works.lysenko.util.spec.PropEnum._TREE_PROGRESSION_OPTIMIZATION_PACK_COLUMNS;
import static works.lysenko.util.spec.PropEnum._TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MAX_DEPTH;
import static works.lysenko.util.spec.PropEnum._TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MIN_RATIO;
import static works.lysenko.util.spec.PropEnum._TREE_PROGRESSION_OPTIMIZATION_SHALLOW_THRESHOLD;
import static works.lysenko.util.spec.PropEnum._TREE_PROGRESSION_OPTIMIZATION_STANDARD_THRESHOLD;

/**
 * Represents tree layout progression optimization configuration.
 */
@SuppressWarnings({"MissingJavadoc", "ClassWithoutLogger"})
public record Optimization() {

    public static boolean enabled = Boolean.TRUE.equals(_TREE_PROGRESSION_OPTIMIZATION.get());
    public static int packColumns = _TREE_PROGRESSION_OPTIMIZATION_PACK_COLUMNS.get();
    public static int shallowMaxDepth = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MAX_DEPTH.get();
    public static double shallowMinRatio = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MIN_RATIO.get();
    public static int shallowThreshold = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_THRESHOLD.get();
    public static int standardThreshold = _TREE_PROGRESSION_OPTIMIZATION_STANDARD_THRESHOLD.get();

    /**
     * Refreshes optimization status from properties.
     */
    public static void refresh() {
        enabled = Boolean.TRUE.equals(_TREE_PROGRESSION_OPTIMIZATION.get());
        packColumns = _TREE_PROGRESSION_OPTIMIZATION_PACK_COLUMNS.get();
        shallowMaxDepth = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MAX_DEPTH.get();
        shallowMinRatio = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MIN_RATIO.get();
        shallowThreshold = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_THRESHOLD.get();
        standardThreshold = _TREE_PROGRESSION_OPTIMIZATION_STANDARD_THRESHOLD.get();
    }
}
