package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._TEST_RESILIENT_MODE;

/**
 * Represents Resilient Mode configuration.
 */
@SuppressWarnings({"MissingJavadoc", "StaticMethodOnlyUsedInOneClass"})
public record Resilient() {

    /**
     * Determines whether Resilient Mode is enabled.
     *
     * @return true if resilient mode is active, false otherwise
     */
    public static boolean mode() {
        return Boolean.TRUE.equals(_TEST_RESILIENT_MODE.get());
    }
}
