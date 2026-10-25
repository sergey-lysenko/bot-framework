package works.lysenko.util.spec;

/**
 * Lengths of various waits (in milliseconds).
 * Defines constant ranges for short, medium, and long wait durations.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass"})
public record Waits() {

    /** Lower bound for short wait duration in milliseconds. */
    public static final int SHORT_WAIT_FROM = 111;

    /** Upper bound for short wait duration in milliseconds. */
    public static final int SHORT_WAIT_TO = 333;

    /** Lower bound for medium wait duration in milliseconds. */
    public static final int MID_WAIT_FROM = SHORT_WAIT_TO;

    /** Upper bound for medium wait duration in milliseconds. */
    public static final int MID_WAIT_TO = 555;

    /** Lower bound for long wait duration in milliseconds. */
    public static final int LONG_WAIT_FROM = MID_WAIT_TO;

    /** Upper bound for long wait duration in milliseconds. */
    public static final int LONG_WAIT_TO = 777;
}
