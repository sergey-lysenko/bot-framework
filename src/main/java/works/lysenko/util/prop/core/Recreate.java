package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._RECREATE_DRIVER_AFTER_EXCEPTION;

/**
 * Represents a Recreate configuration.
 * <p>
 * This class provides a static property for determining
 * whether to recreate the driver after an exception.
 */
@SuppressWarnings({"StaticMethodOnlyUsedInOneClass", "NonFinalStaticVariableUsedInClassInitialization"})
public record Recreate() {

    /**
     * Determines whether to recreate the driver after an exception.
     * <p>
     * This property is fetched from a properties instance using a specified key.
     */
    public static final boolean driver = _RECREATE_DRIVER_AFTER_EXCEPTION.get();
}
