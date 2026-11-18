package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._MOBI_NATIVE_X_RESOLUTION;
import static works.lysenko.util.prop.PropEnum._MOBI_NATIVE_Y_RESOLUTION;

/**
 * Native device screen resolution configuration.
 */
public record Native() {

    /**
     * Native screen resolution along the X (horizontal) axis.
     */
    public static final int x = _MOBI_NATIVE_X_RESOLUTION.get();

    /**
     * Native screen resolution along the Y (vertical) axis.
     */
    public static final int y = _MOBI_NATIVE_Y_RESOLUTION.get();
}
