package works.lysenko.util.prop.tree;

import static works.lysenko.util.spec.PropEnum._ACTION_RETRIES;
import static works.lysenko.util.spec.PropEnum._DEFAULT_SCENARIO_SUFFICIENCY_ATTEMPTS;

/**
 * Represents a Scenario used in a system.
 */
@SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
public record Retries() {

    /**
     * Represents the number of retries for selecting a scenario.
     */
    public static final int selection = _DEFAULT_SCENARIO_SUFFICIENCY_ATTEMPTS.get();

    /**
     * Represents the number of retries for some action.
     */
    public static final Integer action = _ACTION_RETRIES.get();
}
