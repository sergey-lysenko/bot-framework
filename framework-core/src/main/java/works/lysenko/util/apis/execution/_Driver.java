package works.lysenko.util.apis.execution;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.Closeable;

/**
 * Represents an abstraction for managing WebDriver, WebDriverWait, and local driver service instances.
 * This interface defines methods for retrieving and setting these components.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Driver {

    /**
     * Retrieves the current instance of the local driver service (e.g. Appium server).
     *
     * @return the Closeable service instance used by the implementing class, or {@code null}.
     */
    Closeable service();

    /**
     * Retrieves the current instance of the WebDriver.
     *
     * @return the WebDriver instance used by the implementing class.
     */
    WebDriver wd();

    /**
     * Retrieves the current instance of the WebDriverWait.
     *
     * @return the WebDriverWait instance used by the implementing class.
     */
    WebDriverWait wdw();

    /**
     * Retrieves an instance of WebDriverWait with the specified custom timeout.
     *
     * @param timeout the explicit wait timeout duration
     * @return a WebDriverWait instance configured with the specified timeout
     */
    WebDriverWait wdw(java.time.Duration timeout);

    /**
     * Sets the WebDriver instance for the implementing class.
     *
     * @param wd the WebDriver instance to be set
     */
    void wd(WebDriver wd);

    /**
     * Sets the WebDriverWait instance to be used by the implementing class.
     *
     * @param wdw the WebDriverWait instance to be set
     */
    @SuppressWarnings("unused")
    void wdw(WebDriverWait wdw);

    /**
     * Creates and initialises the necessary parts for the WebDriver.
     * This method sets up the driver based on the specified platform and performs
     * additional component creation as needed.
     *
     * @return {@code true} if the WebDriver was successfully initialised, {@code false} otherwise
     */
    boolean isOk();

    /**
     * Hides the on-screen mobile keyboard when supported by the underlying driver.
     */
    void hideKeyboard();

    /**
     * Terminates the mobile application identified by {@code bundleId}.
     *
     * @param bundleId application package or bundle identifier
     */
    void terminateApp(String bundleId);

    /**
     * Activates (brings to foreground) the mobile application identified by {@code bundleId}.
     *
     * @param bundleId application package or bundle identifier
     */
    void activateApp(String bundleId);

    /**
     * Finds a {@link WebElement} using an Android UIAutomator expression.
     *
     * @param uiautomatorText UIAutomator selector expression
     * @return matching {@link WebElement}
     */
    WebElement findByAndroidUIAutomator(String uiautomatorText);

}
