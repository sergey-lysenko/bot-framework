package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._FITS_BY_DEFAULT;

/**
 * Represents a configuration setting for determining whether certain
 * behavior should be enabled by default.
 * <p>
 * This class provides a static property fetched from an application property
 * instance, typically used to configure default states or toggles within the application.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Fits() {

    /**
     * Represents a boolean configuration property indicating whether a particular feature
     * or behavior should be enabled by default.
     * <p>
     * The value is fetched dynamically through the `_FITS_BY_DEFAULT` property, linking
     * the system's runtime or application configuration to the property.
     */
    public static final boolean byDefault = _FITS_BY_DEFAULT.get();
}
