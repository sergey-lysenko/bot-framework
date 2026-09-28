package works.lysenko.util.apis.common;

import org.openqa.selenium.WebElement;
import works.lysenko.tree.Root;

/**
 * This interface provides methods for typing content into web elements.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanMethodNameMustStartWithQuestion", "BooleanParameter"})
public interface TypesIntoWebElements {

    /**
     * Call {@link Root#typeInto(String, Object, double, boolean)} with
     * {@code pasteProbability} set to 0 and {@code secret} set to
     * false
     *
     * @param locator single String locator. To facilitate multilevel search, use
     *                {@code typeInto(find(str1, str2, ...), ...);}
     * @param content Content to type, Object.toString() in used to get string
     *                representation
     * @return true of direct copy was used, false otherwise
     */
    boolean typeInto(String locator, Object content);

    /**
     * Call {@link Root#typeInto(WebElement, Object, double, boolean)} with
     * WebElement defined by String locator
     *
     * @param locator          single String locator. To facilitate multilevel search, use
     *                         {@code typeInto(find(str1, str2, ...), ...);}
     * @param content          Content to type, Object.toString() in used to get string
     *                         representation
     * @param pasteProbability Probability in range [0.0 - 1.0] of using direct copy to
     *                         simulate pasting from clipboard;
     * @param secret           whether to screen content in the log by '•' symbols (as in
     *                         passwords)
     * @return true of direct copy was used, false otherwise
     */
    boolean typeInto(String locator, Object content, double pasteProbability, boolean secret);

    /**
     * Simulate typing the value into the WebElement
     *
     * @param element          to type into
     * @param content          Content to type, Object.toString() in used to get string
     *                         representation
     * @param pasteProbability Probability in range [0.0 - 1.0] of using direct copy to
     *                         simulate pasting from clipboard;
     * @param secret           whether to screen content in the log by '•' symbols (as in
     *                         passwords)
     * @return true of direct copy was used, false otherwise
     */
    boolean typeInto(WebElement element, Object content, double pasteProbability, boolean secret);

    /**
     * Call {@link Root#typeInto(String, Object, double, boolean)} with
     * {@code pasteProbability} set to 0
     *
     * @param locator single String locator. To facilitate multilevel search, use
     *                {@code typeInto(find(str1, str2, ...), ...);}
     * @param content Content to type, Object.toString() in used to get string
     *                representation
     * @param secret  whether to screen content in the log by '•' symbols (as in
     *                passwords)
     * @return true of direct copy was used, false otherwise
     */
    boolean typeInto(String locator, Object content, boolean secret);

    /**
     * Call {@link Root#typeInto(String, Object, double, boolean)} with
     * {@code secret} set to false
     *
     * @param locator          single String locator. To facilitate multilevel search, use
     *                         {@code typeInto(find(str1, str2, ...), ...);}
     * @param content          Content to type, Object.toString() in used to get string
     *                         representation
     * @param pasteProbability in range [0.0 - 1.0] of using direct copy to simulate
     *                         pasting from clipboard;
     * @return true of direct copy was used, false otherwise
     */
    boolean typeInto(String locator, Object content, double pasteProbability);

    /**
     * Call {@link Root#typeInto(WebElement, Object, double, boolean)} with
     * {@code pasteProbability} set to 0 and {@code secret} set to
     * false
     *
     * @param element to type into
     * @param content Content to type, Object.toString() in used to get string
     *                representation
     * @return true of direct copy was used, false otherwise
     */
    boolean typeInto(WebElement element, Object content);

    /**
     * Call {@link Root#typeInto(WebElement, Object, double, boolean)} with
     * {@code pasteProbability} set to 0
     *
     * @param element to type into
     * @param content Content to type, Object.toString() in used to get string
     *                representation
     * @param secret  whether to screen content in the log by '•' symbols (as in
     *                passwords)
     * @return true of direct copy was used, false otherwise
     */
    boolean typeInto(WebElement element, Object content, boolean secret);

    /**
     * Call {@link Root#typeInto(WebElement, Object, double, boolean)} with
     * {@code secret} set to false
     *
     * @param element          to type into
     * @param content          Content to type, Object.toString() in used to get string
     *                         representation
     * @param pasteProbability Probability in range [0.0 - 1.0] of using direct copy to simulate
     *                         pasting from clipboard;
     * @return true of direct copy was used, false otherwise
     */
    boolean typeInto(WebElement element, Object content, double pasteProbability);
}
