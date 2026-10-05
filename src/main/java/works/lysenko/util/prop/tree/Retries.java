package works.lysenko.util.prop.tree;

import static works.lysenko.util.spec.PropEnum._TEST_ACTION_RETRIES;
import static works.lysenko.util.spec.PropEnum._TEST_SCENARIO_SUFFICIENCY_ATTEMPTS;

/**
 * Represents a Scenario used in a system.
 */
@SuppressWarnings("NonFinalStaticVariableUsedInClassInitialization")
public record Retries() {

    /**
     * Represents the number of retries for selecting a scenario.
     */
    public static final int selection = _TEST_SCENARIO_SUFFICIENCY_ATTEMPTS.get();

    /**
     * Represents the number of retries for some action.
     */
    public static final Integer action = _TEST_ACTION_RETRIES.get();
}
