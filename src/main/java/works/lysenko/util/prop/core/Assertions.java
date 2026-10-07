package works.lysenko.util.prop.core;

import works.lysenko.base.*;
import works.lysenko.util.data.enums.*;

import static works.lysenko.util.spec.PropEnum.*;

/**
 * Represents a set of assertions configurations for the application.
 * Provides a static property that determines if assertions produce exceptions.
 */
public record Assertions() {

    /**
     *
     */
    public static final Boolean exception = _TEST_ASSERTIONS_PRODUCE_EXCEPTION.get();
    public static final Severity severity = _TEST_ASSERTIONS_SEVERITY.get(); ;
}
