package works.lysenko.util.prop.tree;

import static works.lysenko.util.prop.PropEnum._TEST_ACTION_RETRIES;
import static works.lysenko.util.prop.PropEnum._TEST_SCENARIO_SUFFICIENCY_ATTEMPTS;

/**
 * Represents a Scenario used in a system.
 */
@SuppressWarnings({"NonFinalStaticVariableUsedInClassInitialization", "StaticNonFinalField"})
public record Retries() {

    /**
     * Represents the number of retries for selecting a scenario.
     */
    public static int selection = _TEST_SCENARIO_SUFFICIENCY_ATTEMPTS.get();

    /**
     * Represents the number of retries for some action.
     */
    public static Integer action = _TEST_ACTION_RETRIES.get();

    /**
     * Refreshes static fields from current test properties.
     */
    public static void refresh() {
        selection = _TEST_SCENARIO_SUFFICIENCY_ATTEMPTS.get();
        action = _TEST_ACTION_RETRIES.get();
    }
}
