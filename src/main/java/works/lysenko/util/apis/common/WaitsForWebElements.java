package works.lysenko.util.apis.common;

import org.openqa.selenium.WebElement;

import java.time.Duration;

/**
 * The WaitsForWebElements interface defines methods for waiting for the appearance, visibility, and behaviour of web elements.
 * It provides methods to wait for elements to become visible, clickable, or not visible.
 * It also provides methods to wait for specific texts to appear or disappear.
 * The interface also includes methods to wait for elements to be selected and to wait for the value of an element to change.
 */
@SuppressWarnings({"ClassWithTooManyMethods", "InterfaceWithOnlyOneDirectInheritor", "unused"})
public interface WaitsForWebElements {

    /**
     * Waits for the appearance or presence of an element identified by its locator.
     *
     * @param locator The string locator of the element to be waited for.
     */
    void wait(String locator);

    /**
     * Wait for the appearance of the defined element
     *
     * @param locator string locator of an element to be waited for
     */
    void wait(String[] locator);

    /**
     * Wait for an element to be clickable
     *
     * @param locator string locator of an element expected to be clickable
     */
    @SuppressWarnings("unused")
    void waitClickable(String locator);

    /**
     * Wait for the appearance of the defined element
     *
     * @param locator string locator of an element to be waited for
     */
    void waitFor(String locator);

    /**
     * Wait for an element to be not visible
     *
     * @param locator string locator of an element expected to be invisible
     */
    void waitForInvisibilityOf(String locator);

    /**
     * Waits for the invisibility of an optional element specified by its locator.
     *
     * @param locator the string locator of the element to wait for its invisibility
     * @return true if the element becomes invisible, false otherwise
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    boolean waitForInvisibilityOfOptional(String locator);

    /**
     * Wait for an element to be not visible
     *
     * @param element string locator of an element expected to be invisible
     */
    void waitForInvisibilityOf(WebElement element);

    /**
     * Wait for an element to be not visible, with a specific implicit wait
     *
     * @param locator string locator of an element expected to be invisible
     * @param iwait   implicit wait for this call
     */
    @SuppressWarnings("unused")
    void waitForInvisibilityOf(String locator, Duration iwait);

    /**
     * Wait for an element to be not visible, with a specific implicit wait
     *
     * @param element element expected to be invisible
     * @param iwait   implicit wait for this call
     */
    @SuppressWarnings("unused")
    void waitForInvisibilityOf(WebElement element, Duration iwait);

    /**
     * Wait for a text to be not visible
     *
     * @param text expected to be invisible
     */
    void waitForInvisibilityOfText(String text);

    /**
     * Waits for the invisibility of multiple specified text strings. This method will block execution
     * until all the provided text strings are no longer visible or present on the page.
     *
     * @param texts one or more text strings to wait for their invisibility
     */
    void waitForInvisibilityOfTexts(String... texts);

    /**
     * Wait for a text to be not visible, with a specific implicit wait
     *
     * @param text  to be invisible
     * @param iwait implicit wait for this call
     */
    void waitForInvisibilityOfText(String text, Duration iwait);

    /**
     * Wait for the appearance of the defined text
     *
     * @param text to be waited for
     */
    void waitForText(String text);

    /**
     * Waits until the edit field containing the specified text becomes ready for editing.
     *
     * @param text the text expected to be present in the edit field that should become editable
     */
    void waitForEdit(String text);

    /**
     * Waits for the visibility or presence of an element identified by the provided descriptor.
     *
     * @param contentDesc a descriptive string or identifier for the element to wait for
     */
    void waitForDesc(String contentDesc);

    /**
     * Waits for the presence or appearance of the specified text strings on the page.
     * The method will block execution until all the provided text strings are visible or present.
     *
     * @param texts one or more text strings to wait for their visibility or presence
     */
    void waitForTexts(String... texts);

    /**
     * Waits for the presence or visibility of the specified text strings on the page.
     * The method will block execution until all the provided text strings are visible or present.
     *
     * @param texts an iterable collection of text strings to wait for their visibility or presence
     */
    void waitForTexts(Iterable<String> texts);

    /**
     * Wait for the appearance of the defined element
     *
     * @param locator string locator of an element to wait for
     */
    void waitForVisibilityOf(String locator);

    /**
     * Wait for the appearance of the element defined by one of the locators
     *
     * @param locators several string locators to randomly select from
     */
    @SuppressWarnings({"unused", "OverloadedVarargsMethod"})
    void waitForVisibilityOf(String... locators);

    /**
     * Wait for the appearance of the defined text
     *
     * @param text to wait for
     */
    void waitForVisibilityOfText(String text);

    /**
     * Waits until the edit field containing the specified text becomes visible.
     *
     * @param text the text expected to be present in the edit field to wait for its visibility
     */
    void waitForVisibilityOfEdit(String text);

    /**
     * Waits for the visibility of an element identified by the provided descriptor.
     *
     * @param text the descriptor or identifier for the element to wait for its visibility
     */
    void waitForVisibilityOfDesc(String text);

    /**
     * Wait for an element to be selected
     *
     * @param locator string locator of an element expected to be selected
     */
    void waitSelected(String locator);

    /**
     * Wait for the appearance of the defined element and the click on it
     *
     * @param locator string locator of an element to be waited for
     * @param x1      coordinate offset
     * @param y1      coordinate offset
     */
    void waitThenClickOn(double x1, double y1, String locator);

    /**
     * Wait for the appearance of the defined element and the click on it
     *
     * @param locator string locator of an element to be waited for
     */
    void waitThenClickOn(String locator);

    /**
     * Wait for the appearance of the defined element, click on it, wait for it to be got
     *
     * @param thenWaitForInvisibilityOf if true
     * @param locator                   string locator of an element to be waited for
     */
    void waitThenClickOn(boolean thenWaitForInvisibilityOf, String locator);

    /**
     * Wait for the appearance of the defined text and the click on it
     *
     * @param text string locator of an element to be waited for //TODO: make this not Android-only
     */
    void waitThenClickOnText(String text);

    /**
     * Wait for the appearance of the defined element and return reference to it
     *
     * @param locator string locator of an element to be waited for
     * @return element
     */
    WebElement waitThenFind(String locator);

    /**
     * Wait for a text value to differ from defined.
     *
     * @param locator string locator of an element
     * @param value   expected value
     */
    void waitValue(String locator, String value);

    /**
     * Wait for a text value of an element to change from a defined one.
     *
     * @param locator string locator of an element
     * @param value   the value to change from
     * @return value after the detected change
     */
    String waitValueNot(String locator, String value);

    /**
     * Wait for a non-empty text value
     *
     * @param locator string locator of an element
     * @return value after the detected change
     */
    String waitValueNotEmpty(String locator);
}
