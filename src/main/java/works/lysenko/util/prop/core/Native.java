package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._MOBI_NATIVE_X_RESOLUTION;
import static works.lysenko.util.spec.PropEnum._MOBI_NATIVE_Y_RESOLUTION;

/**
 * Represents the native resolution properties for an unspecified component.
 * Provides static properties for the native X and Y resolutions.
 */
@SuppressWarnings("MissingJavadoc")
public record Native() {

    public static final int x = _MOBI_NATIVE_X_RESOLUTION.get();
    public static final int y = _MOBI_NATIVE_Y_RESOLUTION.get();
}
