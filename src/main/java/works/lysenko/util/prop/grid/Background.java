package works.lysenko.util.prop.grid;

/**
 * The Centering class represents a centering operation. It calculates the distance between the center point and a given point,
 * and checks if the element is uncentered based on the distance.
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
