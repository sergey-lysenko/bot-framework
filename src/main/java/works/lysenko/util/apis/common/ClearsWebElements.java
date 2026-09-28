package works.lysenko.util.apis.common;

import org.openqa.selenium.WebElement;

/**
 * An interface for clearing web elements' inputs.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter", "unused"})
public interface ClearsWebElements {

    /**
     * Clear an input by either executing
     * {@link org.openqa.selenium.WebElement#clear()} method or by simulating the
     * <strong>Ctrl+A1</strong> and <strong>Backspace</strong> keystrokes
     *
     * @param sendKeys <strong>Ctrl+A1*</strong> and <strong>Backspace</strong> if
     *                 true
     * @param locator  locator(s) of elements to clear
     */
    void clear(boolean sendKeys, String... locator);

    /**
     * Clear an input by either executing
     * {@link org.openqa.selenium.WebElement#clear()} method or by simulating the
     * <strong>Ctrl+A1</strong> and <strong>Backspace</strong> keystrokes
     *
     * @param sendKeys <strong>Ctrl+A1*</strong> and <strong>Backspace</strong> if
     *                 true
     * @param element  element to clear
     */
    void clear(boolean sendKeys, WebElement element);

    /**
     * Clear an input by executing {@link org.openqa.selenium.WebElement#clear()}
     * method
     *
     * @param locator locator(s) of elements to clear
     */
    void clear(String... locator);

    /**
     * Clear an input by executing {@link org.openqa.selenium.WebElement#clear()}
     * method
     *
     * @param element to clear
     */
    void clear(WebElement element);

}
