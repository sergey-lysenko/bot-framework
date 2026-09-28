package works.lysenko.util.apis.common;

import org.openqa.selenium.WebElement;

/**
 * The Scrolls interface provides methods for scrolling and swiping through elements on the screen by means of WebDriver.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter", "unused"})
public interface Scrolls {

    /**
     * Scroll forward
     *
     * @param steps to scroll forward
     * @return element
     */
    WebElement scrollForward(int steps);

    /**
     * Scrolls the given WebElement into view.
     *
     * @param element the WebElement to scroll into view
     */
    void scrollIntoView(WebElement element);

    /**
     * Scrolls the given WebElement into view with a specified horizontal and vertical offset.
     *
     * @param element the WebElement to scroll into view
     * @param dx      the horizontal offset to scroll by after the element is in view
     * @param dy      the vertical offset to scroll by after the element is in view
     */
    void scrollIntoView(WebElement element, int dx, int dy);

    /**
     * Scrolls the element with the given locator into view and clicks on it.
     *
     * @param locator the locator of the element to scroll into view
     */
    void scrollIntoViewAndClickOn(String locator);

    /**
     * Scrolls the element with the given locator into view and clicks on it.
     *
     * @param locator the locator of the element to scroll into view
     * @param dx      the horizontal offset to scroll by after the element is in view
     * @param dy      the vertical offset to scroll by after the element is in view
     */
    void scrollIntoViewAndClickOn(String locator, int dx, int dy);

    /**
     * Scrolls the element with the given locator into view and captures a screenshot of it.
     *
     * @param locator the locator of the element to scroll into view
     */
    void scrollIntoViewAndMakeScreenshot(String locator);

    /**
     * Scrolls the element with the given locator into view and captures a screenshot of it.
     *
     * @param locator the locator of the element to scroll into view
     * @param dx      the horizontal offset to scroll by after element is in view
     * @param dy      the vertical offset to scroll by after element is in view
     */
    void scrollIntoViewAndMakeScreenshot(String locator, int dx, int dy);


    /**
     * @param text to scroll to
     */
    void scrollToText(String text);

    /**
     * Scroll an element with given text into view
     *
     * @param text to scroll to
     * @param lazy whether to check for the presence of a text on screen before an actual scrolling attempt
     */
    void scrollToText(String text, boolean lazy);

    /**
     * Scrolls an element with the given text into view.
     *
     * @param text        the text to scroll to
     * @param lazy        whether to check for the presence of the text on screen before scrolling
     * @param afterScroll a runnable to be executed after the scrolling is performed
     */
    void scrollToText(final String text, final boolean lazy, final Runnable afterScroll);

    /**
     * @param text to scroll to and to click on
     */
    void scrollToThenClickOn(String text);
}
