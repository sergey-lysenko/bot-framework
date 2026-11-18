package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._TREE_SCENARIO_DEPTH_SAFEGUARD;
import static works.lysenko.util.prop.PropEnum._TREE_SCENARIO_HISTORY_DEPTH_SAFEGUARD;

/**
 * Safeguard threshold configurations for tree scenario depth.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Safeguard() {

    /**
     * Maximum permissible traversal depth limit for scenario trees.
     */
    public static final Integer scenarioDepth = _TREE_SCENARIO_DEPTH_SAFEGUARD.get();

    /**
     * Maximum permissible history depth limit maintained for executed scenarios.
     */
    public static final Integer historyDepth = _TREE_SCENARIO_HISTORY_DEPTH_SAFEGUARD.get();
}
