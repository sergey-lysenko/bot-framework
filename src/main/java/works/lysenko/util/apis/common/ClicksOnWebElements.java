package works.lysenko.util.apis.common;

import org.apache.commons.math3.fraction.Fraction;
import org.openqa.selenium.WebElement;

/**
 * Interface for clicking on web elements.
 */
@SuppressWarnings({"MethodParameterNamingConvention", "InterfaceWithOnlyOneDirectInheritor", "BooleanParameter", "unused"})
public interface ClicksOnWebElements {

    /**
     * @param element to click on
     */
    void clickOn(WebElement element);

    /**
     * Click on an Element defined by locator and then wait for certain milliseconds
     *
     * @param element   to click on
     * @param waitAfter number of milliseconds to wait after clicking
     */
    void clickOn(WebElement element, long waitAfter);

    /**
     * @param element  to be clicked on
     * @param geometry whether to output information about geometry or not
     */
    void clickOn(WebElement element, boolean geometry);

    /**
     * Perform a non-centered click on an element. Exact location of click defined
     * by two double-typed relative x and y coordinates, where value 0.0 corresponds
     * to the leftmost or upmost part of the element, as 1.0 is for rightmost of the lowest
     * part of an object.
     * <p>
     * Consequently, coordinates (0.5, 0.5) defining a centre of the object
     *
     * @param x1      requested x coordinate offset
     * @param y1      requested y coordinate offset
     * @param locator string locator(s) of an element to be clicked on
     */
    void clickOn(double x1, double y1, String... locator);

    /**
     * Performs a click on a specific location within an element, where the location
     * is defined by fractional x and y coordinates. Each coordinate is represented
     * as a fraction, where 0 represents the starting edge (e.g., left or top)
     * and 1 represents the opposite edge (e.g., right or bottom). The element is
     * identified by its locator(s).
     *
     * @param x1      the fractional x coordinate of the click position relative to the element
     * @param y1      the fractional y coordinate of the click position relative to the element
     * @param locator the string locator(s) used to identify the element to be clicked
     */
    void clickOn(Fraction x1, Fraction y1, String... locator);

    /**
     * Click on an Element defined by locator and then wait for certain milliseconds
     *
     * @param locator   string locator of an element to click on
     * @param waitAfter number of milliseconds to wait after clicking
     */
    void clickOn(String locator, long waitAfter);

    /**
     * Click on an Element defined by locator
     *
     * @param locator string locator of an element to click on
     */
    void clickOn(String locator);

    /**
     * Click on an Element defined by locator and wait for it's disappearance
     *
     * @param thenWaitForInvisibilityOf optionally wait for the disappearance of the same element
     * @param locator                   string locator of an element to click on
     */
    void clickOn(boolean thenWaitForInvisibilityOf, String locator);

    /**
     * Click on an Element defined by locator
     *
     * @param thenWaitForInvisibilityOf optionally wait for the disappearance of the same element
     * @param element                   handle of an element to click on
     */
    void clickOn(boolean thenWaitForInvisibilityOf, WebElement element);

    /**
     * Click on a Text and then wait for certain milliseconds
     *
     * @param text      to click on
     * @param waitAfter number of milliseconds to wait after clicking
     */
    void clickOnText(String text, long waitAfter);

    /**
     * Click on a Text
     *
     * @param text to click on
     */
    void clickOnText(String text);

    /**
     * Performs a click on a specific location within a text element. The click
     * position is defined by fractional x and y coordinates where 0 represents
     * the starting edge (e.g., left or top) and 1 represents the ending edge
     * (e.g., right or bottom). The method identifies the element based on its
     * visible text.
     *
     * @param x1   the fractional x-coordinate of the click position relative to the text element
     * @param y1   the fractional y-coordinate of the click position relative to the text element
     * @param text the visible text of the element to be clicked
     */
    void clickOnText(double x1, double y1, String text);

    /**
     * Performs a click on a specific location within a text element. The location is defined
     * by fractional x and y coordinates, where each coordinate represents a relative offset
     * within the element (e.g., 0 for the start and 1 for the end of the dimension). The
     * element is identified by its visible text.
     *
     * @param x1   the fractional x-coordinate of the click position relative to the text element
     * @param y1   the fractional y-coordinate of the click position relative to the text element
     * @param text the visible text of the element to be clicked
     */
    void clickOnText(Fraction x1, Fraction y1, String text);

    /**
     * Clicks on a content-desc.
     *
     * @param contentDesc the text description to be clicked on
     */
    void clickOnDesc(String contentDesc);

    /**
     * Click on a Text and wait for it to disappear
     *
     * @param text                      to click on
     * @param thenWaitForInvisibilityOf optionally wait for the disappearance of the same element
     */
    void clickOnText(boolean thenWaitForInvisibilityOf, String text);

    /**
     * Hide/Close Android Sensor Keyboard
     */
    void hideKeyboard();

    /**
     * @param text to click on if it exists
     */
    void ifExistsClickOnText(String text);
}
