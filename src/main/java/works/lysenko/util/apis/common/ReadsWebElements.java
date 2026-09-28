package works.lysenko.util.apis.common;

import org.openqa.selenium.WebElement;

/**
 * The ReadsWebElements interface provides methods for reading the contents of web elements.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter", "unused"})
public interface ReadsWebElements {

    /**
     * Read contents of the WebElement
     *
     * @param element to read text from
     * @return text
     */
    String read(WebElement element);

    /**
     * Read contents of given WebElement with optional logging of geometry
     * information
     *
     * @param element  to read from
     * @param geometry whether to post a log message or not
     * @return result of .getText() for this element
     */
    String read(WebElement element, boolean geometry);

    /**
     * Read contents of the element defined by string locator
     *
     * @param locator string locator of an element
     * @return result of .getText() for this element
     */
    String read(String locator);

    /**
     * Reads the data from the given locator with the specified number of retries.
     *
     * @param locator the locator to identify the data
     * @param retries the number of times to retry reading the data if it fails
     * @return the data read from the locator
     */
    String read(String locator, int retries);

    /**
     * Read value attribute of the element defined by String locator
     *
     * @param locator string locator of an element
     * @return contents of value attribute of the element
     */
    String readInput(String locator);

    /**
     * Read value attribute of the element
     *
     * @param element to read from
     * @return contents of value attribute of the element
     */
    String readInput(WebElement element);
}
