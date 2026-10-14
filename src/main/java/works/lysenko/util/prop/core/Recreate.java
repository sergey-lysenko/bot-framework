package works.lysenko.util.prop.core;

import static works.lysenko.util.prop.PropEnum._WEBD_RECREATE_DRIVER_AFTER_EXCEPTION;

/**
 * Driver recreation configuration upon exceptions.
 */
public record Recreate() {

    /**
     * Indicates whether the WebDriver instance should be recreated after an unhandled exception occurs.
     */
    public static final boolean driver = _WEBD_RECREATE_DRIVER_AFTER_EXCEPTION.get();
}
