package works.lysenko.util.spec;

/**
 * Lengths of various waits
 */
@SuppressWarnings({"ClassWithoutLogger", "MissingJavadoc", "StaticMethodOnlyUsedInOneClass"})
public record Waits() {

    public static final int SHORT_WAIT_FROM = 111;
    public static final int SHORT_WAIT_TO = 333;
    public static final int MID_WAIT_FROM = SHORT_WAIT_TO;
    public static final int MID_WAIT_TO = 555;
    public static final int LONG_WAIT_FROM = MID_WAIT_TO;
    public static final int LONG_WAIT_TO = 777;
}
