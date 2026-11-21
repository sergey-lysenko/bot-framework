package works.lysenko.util.prop.tree;

import static works.lysenko.util.prop.PropEnum._TREE_MIN_OFFSET_SEPARATION;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION_BANK_COLUMNS;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION_MAX_ASPECT_RATIO;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION_PACK_COLUMNS;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MAX_DEPTH;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MIN_RATIO;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION_SHALLOW_THRESHOLD;
import static works.lysenko.util.prop.PropEnum._TREE_PROGRESSION_OPTIMIZATION_STANDARD_THRESHOLD;
import static works.lysenko.util.prop.PropEnum._TREE_REDUCE_HORIZONTAL_GAPS;
import static works.lysenko.util.prop.PropEnum._TREE_REDUCE_HORIZONTAL_OFFSETS;
import static works.lysenko.util.prop.PropEnum._TREE_REDUCE_VERTICAL_GAPS;
import static works.lysenko.util.prop.PropEnum._TREE_REDUCE_VERTICAL_OFFSETS;

/**
 * Represents tree layout progression optimization configuration.
 * <p>
 * Provides configuration properties and thresholds for optimizing the visual
 * layout of scenario trees in generated HTML reports, including leaf packing,
 * dynamic offset reduction, and empty gap removal.
 */
@SuppressWarnings({"ClassWithoutLogger", "NonFinalStaticVariableUsedInClassInitialization", "StaticNonFinalField"})
public record Optimization() {

    /**
     * Indicates whether tree layout progression optimization is enabled.
     */
    public static boolean progression = _TREE_PROGRESSION_OPTIMIZATION.get();

    /**
     * Whether to remove completely empty columns in the layout.
     */
    public static boolean reduceHorizontalGaps = _TREE_REDUCE_HORIZONTAL_GAPS.get();

    /**
     * Whether to remove completely empty rows in the layout.
     */
    public static boolean reduceVerticalGaps = _TREE_REDUCE_VERTICAL_GAPS.get();

    /**
     * Whether to dynamically reduce horizontal {@code @VisualOffset} shifts to pack separated branches into empty spaces.
     */
    public static boolean reduceHorizontalOffsets = _TREE_REDUCE_HORIZONTAL_OFFSETS.get();

    /**
     * Whether to dynamically reduce vertical {@code @VisualRowOffset} shifts to pack separated branches into empty spaces.
     */
    public static boolean reduceVerticalOffsets = _TREE_REDUCE_VERTICAL_OFFSETS.get();

    /**
     * Minimum preserved separation (in columns horizontally / rows vertically) for originally offset-separated branches.
     */
    public static int minOffsetSeparation = _TREE_MIN_OFFSET_SEPARATION.get();

    /**
     * Number of horizontal column banks to partition independent scenario trees into when aspect ratio is narrow.
     * {@code 0} (default) selects the optimal bank count automatically for {@link #maxAspectRatio};
     * {@code 1} disables multi-bank layout; {@code >= 2} enforces a fixed number of banks.
     */
    public static int bankColumns = _TREE_PROGRESSION_OPTIMIZATION_BANK_COLUMNS.get();

    /**
     * Target aspect ratio (width/height, default {@code 1.78} for 16:9) below which multi-bank tree layout is
     * activated and toward which automatic bank selection optimizes.
     */
    public static double maxAspectRatio = _TREE_PROGRESSION_OPTIMIZATION_MAX_ASPECT_RATIO.get();

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
        reduceHorizontalGaps = _TREE_REDUCE_HORIZONTAL_GAPS.get();
        reduceVerticalGaps = _TREE_REDUCE_VERTICAL_GAPS.get();
        reduceHorizontalOffsets = _TREE_REDUCE_HORIZONTAL_OFFSETS.get();
        reduceVerticalOffsets = _TREE_REDUCE_VERTICAL_OFFSETS.get();
        minOffsetSeparation = _TREE_MIN_OFFSET_SEPARATION.get();
        bankColumns = _TREE_PROGRESSION_OPTIMIZATION_BANK_COLUMNS.get();
        maxAspectRatio = _TREE_PROGRESSION_OPTIMIZATION_MAX_ASPECT_RATIO.get();
        packColumns = _TREE_PROGRESSION_OPTIMIZATION_PACK_COLUMNS.get();
        shallowMaxDepth = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MAX_DEPTH.get();
        shallowMinRatio = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_MIN_RATIO.get();
        shallowThreshold = _TREE_PROGRESSION_OPTIMIZATION_SHALLOW_THRESHOLD.get();
        standardThreshold = _TREE_PROGRESSION_OPTIMIZATION_STANDARD_THRESHOLD.get();
    }
}
