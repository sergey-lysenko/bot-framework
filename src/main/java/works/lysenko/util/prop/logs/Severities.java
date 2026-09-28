package works.lysenko.util.prop.logs;

import works.lysenko.util.apis.util._Severity;

import static works.lysenko.util.data.enums.Severity.S2;

/**
 * Represents a class containing static constants for severity levels and reductions.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Severities() {

    /**
     * Represents the severity of an external event.
     * This variable is a constant of type _Severity and is set to level S2.
     */
    public static final _Severity externalEvent = S2;
}
