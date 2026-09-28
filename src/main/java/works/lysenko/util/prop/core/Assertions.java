package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._ASSERTIONS_PRODUCE_EXCEPTION;

/**
 * Represents a set of assertions configurations for the application.
 * Provides a static property that determines if assertions produce exceptions.
 */
public record Assertions() {

    /**
     *
     */
    public static final Boolean exception = _ASSERTIONS_PRODUCE_EXCEPTION.get();
}
