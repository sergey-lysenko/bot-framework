package works.lysenko.util.prop.tree;

import static works.lysenko.util.spec.PropEnum._TREE_NOTIFY_ABOUT_EMPTY_SCENARIO_PACKAGES;
import static works.lysenko.util.spec.PropEnum._TREE_SCENARIO_DOES_NOT_FIT_NOTICE;
import static works.lysenko.util.spec.PropEnum._TREE_SCENARIO_NOT_EXECUTABLE_NOTICE;

/**
 * Represents a set of notifications for different scenario conditions.
 * <p>
 * This class provides several boolean flags to indicate various notification
 * states related to scenario execution. Each flag is retrieved from a properties
 * instance based on specific keys.
 */
@SuppressWarnings({"NegativelyNamedBooleanVariable", "StaticMethodOnlyUsedInOneClass"})
public record Notify() {

    /**
     * Whether the scenario is not executable notice.
     */
    public static final boolean notExecutable = _TREE_SCENARIO_NOT_EXECUTABLE_NOTICE.get();

    /**
     * Represents a flag indicating whether the scenario does not fit notice should be displayed.
     */
    public static final boolean doesNotFit = _TREE_SCENARIO_DOES_NOT_FIT_NOTICE.get();

    /**
     * Represents a flag indicating whether to notify about empty scenario packages.
     */
    public static final boolean emptyPackage = _TREE_NOTIFY_ABOUT_EMPTY_SCENARIO_PACKAGES.get();
}
