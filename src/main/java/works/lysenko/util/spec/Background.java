package works.lysenko.util.spec;

/**
 * Background configuration constants representing background settings such as
 * validation flags and density values.
 */
@SuppressWarnings("StaticMethodOnlyUsedInOneClass")
public record Background() {

    /**
     * Boolean flag indicating whether validation is enabled or not.
     * <p>
     * This static final field is set to true, which means validation is currently enabled.
     */
    public static final boolean validation = true;

    /**
     * Represents the density setting for a particular operation.
     * The density value is set to 39.
     */
    public static final int density = 39;
}
