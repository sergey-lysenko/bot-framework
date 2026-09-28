package works.lysenko.util.data.records;

import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import static works.lysenko.Base.find;
import static works.lysenko.util.func.type.Points.getCenter;

/**
 * The Geometry class represents the geometry information of a web element.
 */
public record Geometry(WebElement element, Rectangle rectangle, Point center) {

    /**
     * Locates a web element based on the given locator and retrieves its geometry information.
     *
     * @param locator The locator used to identify the element.
     * @return The Geometry object containing the element's information.
     */
    public static Geometry locate(final String locator) {

        final WebElement element = find(locator);
        final Rectangle rectangle = element.getRect();
        final Point center = getCenter(rectangle);
        return new Geometry(element, rectangle, center);
    }
}
