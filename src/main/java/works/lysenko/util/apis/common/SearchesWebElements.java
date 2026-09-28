package works.lysenko.util.apis.common;

import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * The SearchesWebElements interface defines methods for searching and interacting with web elements.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor",
        "BooleanParameter", "unused"})
public interface SearchesWebElements {

    /**
     * Find an element defined by one or several nested String locators.
     *
     * @param silent    if true, no output added to log
     * @param scrollTo  if true, a window scrolled to make this element visible on screen
     * @param mandatory if false, failure to find an element will not halt test execution
     * @param locators  one or several nested locators
     * @return WebElement object
     */
    WebElement find(boolean silent, boolean scrollTo, boolean mandatory, String... locators);

    /**
     * "Find" an element which could be located outside of current view by
     * performing moveToElement() call
     *
     * @param element WebElement to scroll to
     * @return same web element
     */
    WebElement find(WebElement element);

    /**
     * Find an element defined by one or several nested String locators
     *
     * @param locator one or several nested String locators
     * @return WebElement object
     */
    WebElement find(String locator);

    /**
     * Find all elements defined by the string locator
     *
     * @param locator locator of 1-n elements to be found
     * @return list of located element references
     */
    List<WebElement> findAll(String locator);

    /**
     * Find all elements defined by string locator and configurable exposing of
     * real element path
     *
     * @param locator locator of 1-n elements to be found
     * @param expose  locator or not
     * @return list of located element references
     */
    List<WebElement> findAll(String locator, boolean expose);

    /**
     * Find all elements defined by string locator, with specific iwait
     *
     * @param locator locator of 1-n elements to be found
     * @param iwait   implicit wait for this call
     * @return list of located element references
     */

    List<WebElement> findAll(String locator, Duration iwait);

    /**
     * Find all elements defined by string locator and configurable exposing of
     * real element path, with specific iwait
     *
     * @param locator locator of 1-n elements to be found
     * @param expose  locator or not
     * @param iwait   implicit wait for this call
     * @return list of located element references
     */
    List<WebElement> findAll(String locator, boolean expose, Duration iwait);

    /**
     * Finds and returns the first text that matches the specified regular expression
     * within the elements located by the given locator.
     *
     * @param locator   the locator used to identify the elements
     * @param textRegex the regular expression to match against the text content of the elements
     * @return the first text that matches the specified regular expression or null if no match is found
     */
    String findFirstOfTexts(final String locator, final String textRegex);

    /**
     * Finds and returns the first text that matches the specified regular expression
     * within the elements located by the given locator, with a specified number of retries.
     *
     * @param locator   the locator used to identify the elements
     * @param textRegex the regular expression to match against the text content of the elements
     * @param retries   the number of retry attempts to perform if no match is found
     * @return the first text that matches the specified regular expression or null if no match is found
     */
    String findFirstOfTexts(final String locator, final String textRegex, final int retries);

    /**
     * Find the web element identified by the given locator and return it.
     * If the web element does not exist, return null.
     *
     * @param locator the locator of the web element
     * @return the web element if found, null otherwise
     */
    WebElement findOrNull(final String locator);

    /**
     * Finds an element with the given text by scrolling through the page.
     *
     * @param text the text to search for
     * @return the WebElement object if found, null otherwise
     */
    WebElement findScrollToTextOrNull(final String text);

    /**
     * Finds an element with the given text by scrolling through the page.
     *
     * @param text the text to search for
     * @param code the code to execute before finding the element (can be null)
     * @return the WebElement object if found, null otherwise
     */
    WebElement findScrollToTextOrNull(final String text, final Runnable code);

    /**
     * Finds an element with the given text.
     *
     * @param text the text to search for
     * @return the WebElement object if found, null otherwise
     */
    WebElement findText(String text);

    /**
     * Extracts and returns a list of text strings from elements identified by the given locator,
     * filtered by the specified regular expression.
     *
     * @param locator    the locator used to identify the elements
     * @param textsRegex the regular expression to filter the extracted text content
     * @return a list of text strings that match the specified regular expression
     */
    List<String> findTexts(final String locator, final String textsRegex);

    /**
     * Extracts and returns a list of text strings from elements identified by the given locator,
     * filtered by the specified regular expression, with retries in case of errors or failures.
     *
     * @param locator    the locator used to identify the elements
     * @param textsRegex the regular expression to filter the extracted text content
     * @param retries    the number of retry attempts to perform if an error occurs
     * @return a list of text strings that match the specified regular expression
     */
    List<String> findTexts(final String locator, final String textsRegex, final int retries);

    /**
     * Find a visible instance of the defined element and click on it
     *
     * @param locator string locator of an element to be waited for
     */
    void findThenClick(String locator);

    /**
     * Checks if the web element identified by the given locator is disabled.
     *
     * @param locator the locator of the web element
     * @return true if the web element is disabled, false otherwise
     */
    boolean isDisabled(String locator);

    /**
     * Checks if the web element identified by the given locator is enabled.
     *
     * @param locator the locator of the web element
     * @return true if the web element is enabled, false otherwise
     */
    boolean isEnabled(String locator);


    /**
     * Calculates and returns the number of occurrences of a specific element identified by the given locator.
     *
     * @param locator the string identifier used to locate the desired element
     * @return the number of times the element identified by the locator is present
     */
    int amountPresent(String locator);

    /**
     * Checks if the web element identified by the given locator exists.
     *
     * @param locator the locator of the web element
     * @return true if the web element exists, false otherwise
     */
    boolean isPresent(String locator);

    /**
     * Checks if the web element identified by the given locator exists.
     *
     * @param locator the locator of the web element
     * @param iwait   the duration to wait for the element to exist
     * @return true if the web element exists, false otherwise
     */
    boolean isPresent(String locator, Duration iwait);

    /**
     * Checks if the web element identified by the given locator exists.
     *
     * @param locator the locator of the web element
     * @param expose  indicates whether to use the real locator or not
     * @return true if the web element exists, false otherwise
     */
    boolean isPresent(String locator, boolean expose);

    /**
     * Checks if the web element identified by the given locator exists.
     *
     * @param locator the locator of the web element
     * @param expose  indicates whether to use the real locator or not
     * @param iwait   the duration to wait for the element to exist
     * @return true if the web element exists, false otherwise
     */
    boolean isPresent(String locator, boolean expose, Duration iwait);

    /**
     * Checks if the web element identified by the given locator is visible on the page.
     *
     * @param locator the locator of the web element
     * @return true if the web element is visible, false otherwise
     */
    boolean isVisible(String locator);

    /**
     * Checks if the web element identified by the given locator is visible on the page.
     *
     * @param locator the locator of the web element
     * @param silent  if true, no output added to log
     * @return true if the web element is visible, false otherwise
     */
    boolean isVisible(String locator, boolean silent);
}
