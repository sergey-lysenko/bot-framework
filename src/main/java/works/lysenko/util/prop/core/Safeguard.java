package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._TREE_SCENARIO_DEPTH_SAFEGUARD;
import static works.lysenko.util.spec.PropEnum._TREE_SCENARIO_HISTORY_DEPTH_SAFEGUARD;

/**
 * Represents a Scenario used in a system.
 */
@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "StaticMethodOnlyUsedInOneClass"})
public record Safeguard() {

    /**
     * This variable represents the depth safeguard value for a scenario.
     */
    public static final Integer scenarioDepth = _TREE_SCENARIO_DEPTH_SAFEGUARD.get();

    /**
     * This variable represents the depth safeguard value for a scenario.
     */
    public static final Integer historyDepth = _TREE_SCENARIO_HISTORY_DEPTH_SAFEGUARD.get();
}
