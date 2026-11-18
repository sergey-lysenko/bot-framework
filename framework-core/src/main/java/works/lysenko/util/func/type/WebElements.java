package works.lysenko.util.func.type;

import org.openqa.selenium.WebElement;

import static works.lysenko.Base.logEvent;
import static works.lysenko.util.data.enums.Severity.S1;

/**
 * The WebElements class provides utility methods for interacting with web elements,
 * particularly for managing attributes of these elements.
 */
@SuppressWarnings("unused")
public record WebElements() {

    /**
     * Retrieves the specified attribute's value from a given web element.
     * If the attribute is not present or an UnsupportedCommandException occurs, it returns null.
     *
     * @param element   the web element from which to retrieve the attribute
     * @param attribute the name of the attribute to retrieve
     * @return the value of the specified attribute, or null if the attribute is not present or an exception is encountered
     */
    @SuppressWarnings({"MethodWithMultipleReturnPoints", "StaticMethodOnlyUsedInOneClass"})
    public static String getAttribute(final WebElement element, final String attribute) {

        try {
            return element.getAttribute(attribute);
        } catch (final org.openqa.selenium.UnsupportedCommandException e) {
            logEvent(S1, e.getMessage());
            return null;
        }
    }
}
