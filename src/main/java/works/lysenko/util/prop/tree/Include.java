package works.lysenko.util.prop.tree;

import static works.lysenko.util.prop.PropEnum._TREE_INCLUDE_DOWNSTREAM;
import static works.lysenko.util.prop.PropEnum._TREE_INCLUDE_UPSTREAM;

/**
 * Provides configuration flags for tree execution weighting.
 * <p>
 * Controls whether upstream and downstream scenario weights are factored into
 * scenario selection and coverage estimation during tree traversal.
 */
@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "StaticNonFinalField"})
public record Include() {

    /**
     * Indicates whether upstream scenario weights are included in scenario selection and calculations.
     */
    public static boolean upstream = _TREE_INCLUDE_UPSTREAM.get();

    /**
     * Indicates whether downstream scenario weights are included in scenario selection and calculations.
     */
    public static boolean downstream = _TREE_INCLUDE_DOWNSTREAM.get();

    /**
     * Refreshes upstream and downstream flags from current test properties.
     */
    public static void refresh() {

        upstream = _TREE_INCLUDE_UPSTREAM.get();
        downstream = _TREE_INCLUDE_DOWNSTREAM.get();
    }
}
