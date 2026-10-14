package works.lysenko.util.prop.tree;

import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION_PACK_COLUMNS;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MAX_DEPTH;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MIN_RATIO;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION_SHALLOW_THRESHOLD;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION_STANDARD_THRESHOLD;

/**
 * Represents tree layout progression optimization configuration.
 * <p>
 * Provides configuration properties and thresholds for optimizing the visual
 * layout of scenario trees in generated HTML reports, including leaf packing
 * and shallow-tree handling.
 */
@SuppressWarnings({"ClassWithoutLogger", "NonFinalStaticVariableUsedInClassInitialization", "StaticNonFinalField"})
public record Optimization() {

    /**
     * Indicates whether tree layout progression optimization is enabled.
     */
    public static boolean progression = _TREE_PROGRESSION_OPTIMIZATION.get();

    /**
     * Number of columns to pack leaf children into when layout optimization is active.
     */
    public static int packColumns = _TREE_PROGRESSION_OPTIMIZATION_PACK_COLUMNS.get();

    /**
     * Maximum tree depth threshold used to classify a tree layout as shallow.
     */
    public static int shallowMaxDepth = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MAX_DEPTH.get();

    /**
     * Minimum ratio of node count to tree depth required to classify a tree as shallow and wide.
     */
    public static double shallowMinRatio = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MIN_RATIO.get();

    /**
     * Minimum number of leaf children required on a node to trigger leaf packing in shallow-wide trees.
     */
    public static int shallowThreshold = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_THRESHOLD.get();

    /**
     * Minimum number of leaf children required on a node to trigger leaf packing in standard trees.
     */
    public static int standardThreshold = _TREE_PROGRESSION_OPTIMIZATION_STANDARD_THRESHOLD.get();

    /**
     * Refreshes optimization configuration values from current test properties.
     */
    public static void refresh() {
        progression = _TREE_PROGRESSION_OPTIMIZATION.get();
        packColumns = _TREE_PROGRESSION_OPTIMIZATION_PACK_COLUMNS.get();
        shallowMaxDepth = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MAX_DEPTH.get();
        shallowMinRatio = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MIN_RATIO.get();
        shallowThreshold = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_THRESHOLD.get();
        standardThreshold = _TREE_PROGRESSION_OPTIMIZATION_STANDARD_THRESHOLD.get();
    }
}
