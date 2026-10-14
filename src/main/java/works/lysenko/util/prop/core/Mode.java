package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._TEST_RESILIENT_MODE;

/**
 * Mode execution mode configuration.
 */
@SuppressWarnings({"StaticNonFinalField", "StaticMethodOnlyUsedInOneClass"})
public record Mode() {

    /**
     * Determines whether resilient mode is enabled.
     * True if resilient mode is active, false otherwise
     */
    public static boolean resilient = _TEST_RESILIENT_MODE.get();

}
