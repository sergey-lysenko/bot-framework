package works.lysenko.util.prop.tree;

import static works.lysenko.util.prop.PropEnum._TREE_MUTE_DOWNSTREAM_WEIGHT_CHANGE_NOTICE;
import static works.lysenko.util.prop.PropEnum._TREE_MUTE_EMPTY_CANDIDATES_NOTICE;
import static works.lysenko.util.prop.PropEnum._TREE_MUTE_FULFILLED_NOT_EXECUTABLE_NOTICE;
import static works.lysenko.util.prop.PropEnum._TREE_NOTIFY_ABOUT_EMPTY_SCENARIO_PACKAGES;
import static works.lysenko.util.prop.PropEnum._TREE_SCENARIO_DOES_NOT_FIT_NOTICE;
import static works.lysenko.util.prop.PropEnum._TREE_SCENARIO_NOT_EXECUTABLE_NOTICE;

/**
 * Represents a set of notifications for different scenario conditions.
 * <p>
 * This class provides several boolean flags to indicate various notification
 * states related to scenario execution. Each flag is retrieved from a properties
 * instance based on specific keys.
 */
@SuppressWarnings({"NegativelyNamedBooleanVariable", "StaticMethodOnlyUsedInOneClass", "StaticNonFinalField"})
public record Notify() {

    /**
     * Whether the scenario is not executable notice.
     */
    public static boolean notExecutable = _TREE_SCENARIO_NOT_EXECUTABLE_NOTICE.get();

    /**
     * Represents a flag indicating whether the scenario does not fit notice should be displayed.
     */
    public static boolean doesNotFit = _TREE_SCENARIO_DOES_NOT_FIT_NOTICE.get();

    /**
     * Represents a flag indicating whether to notify about empty scenario packages.
     */
    public static boolean emptyPackage = _TREE_NOTIFY_ABOUT_EMPTY_SCENARIO_PACKAGES.get();

    /**
     * Whether to demote {@code is not executable} notices to debug when non-executability is caused solely by goal fulfillment.
     */
    public static boolean muteFulfilledNotExecutable = _TREE_MUTE_FULFILLED_NOT_EXECUTABLE_NOTICE.get();

    /**
     * Whether to demote empty-candidate-pool continuation notices in tolerant/all-leafs selection modes to debug.
     */
    public static boolean muteEmptyCandidates = _TREE_MUTE_EMPTY_CANDIDATES_NOTICE.get();

    /**
     * Whether to demote postflight downstream-weight change notices for shared/multi-instance scenarios to debug.
     */
    public static boolean muteDownstreamWeightChange = _TREE_MUTE_DOWNSTREAM_WEIGHT_CHANGE_NOTICE.get();

    /**
     * Refreshes static fields from current test properties.
     */
    public static void refresh() {
        notExecutable = _TREE_SCENARIO_NOT_EXECUTABLE_NOTICE.get();
        doesNotFit = _TREE_SCENARIO_DOES_NOT_FIT_NOTICE.get();
        emptyPackage = _TREE_NOTIFY_ABOUT_EMPTY_SCENARIO_PACKAGES.get();
        muteFulfilledNotExecutable = _TREE_MUTE_FULFILLED_NOT_EXECUTABLE_NOTICE.get();
        muteEmptyCandidates = _TREE_MUTE_EMPTY_CANDIDATES_NOTICE.get();
        muteDownstreamWeightChange = _TREE_MUTE_DOWNSTREAM_WEIGHT_CHANGE_NOTICE.get();
    }
}
