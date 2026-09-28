package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._SCENARIO_DEPTH_SAFEGUARD;
import static works.lysenko.util.spec.PropEnum._SCENARIO_HISTORY_DEPTH_SAFEGUARD;

/**
 * Represents a Scenario used in a system.
 */
@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "StaticMethodOnlyUsedInOneClass"})
public record Safeguard() {

    /**
     * This variable represents the depth safeguard value for a scenario.
     */
    public static final Integer scenarioDepth = _SCENARIO_DEPTH_SAFEGUARD.get();

    /**
     * This variable represents the depth safeguard value for a scenario.
     */
    public static final Integer historyDepth = _SCENARIO_HISTORY_DEPTH_SAFEGUARD.get();
}
