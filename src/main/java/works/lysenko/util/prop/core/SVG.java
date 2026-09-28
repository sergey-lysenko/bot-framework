package works.lysenko.util.prop.core;

import static works.lysenko.util.spec.PropEnum._SVG_HEIGHT;
import static works.lysenko.util.spec.PropEnum._SVG_WIDTH;

/**
 * Represents a class to manage SVG properties for the application.
 * Provides the height and width of SVG elements.
 */
@SuppressWarnings("MissingJavadoc")
public record SVG() {

    public static final int height = _SVG_HEIGHT.get();
    public static final int width = _SVG_WIDTH.get();
}
