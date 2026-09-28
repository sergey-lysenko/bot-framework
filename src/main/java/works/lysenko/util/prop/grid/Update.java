package works.lysenko.util.prop.grid;

import static works.lysenko.util.spec.PropEnum.*;

/**
 * Represents a class to manage update properties for the application.
 * <p>
 * This class provides static boolean properties related to various update expectations, such as colour,
 * hsv (Hue, Saturation, Value), passed status, polychromy, and shrink status.
 */
@SuppressWarnings({"MissingJavadoc", "StaticMethodOnlyUsedInOneClass", "NonFinalStaticVariableUsedInClassInitialization"})
public record Update() {

    public static final boolean colour = _UPDATE_COLOUR_EXPECTATIONS.get();
    @SuppressWarnings("unused")
    public static final boolean hsv = _UPDATE_HSV_EXPECTATIONS.get();
    public static final boolean passed = _UPDATE_PASSED_EXPECTATIONS.get();
    public static final boolean polychromy = _UPDATE_POLYCHROMY_EXPECTATIONS.get();
    public static final boolean shrink = _SHRINK_EXPECTATIONS.get();
}
