package works.lysenko.util.apis.execution;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import org.apache.commons.math3.fraction.Fraction;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Interface of Scenario executors
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter", "SameReturnValue", "BooleanMethodNameMustStartWithQuestion"})
public interface _Executes {

    /**
     * Creates and initialises a driver for the execution environment.
     * This method is commonly used to set up the necessary configurations
     * to start and manage the execution process.
     *
     * @return {@code true} if the driver was successfully created, otherwise {@code false}.
     */
    boolean createDriver();

    /**
     * Creates and initialises a scaler for execution scenarios.
     * This method is used in conjunction with other initialisation processes to ensure proper scaling
     * and resolution adjustments in the execution environment.
     *
     * @return {@code true} if the scaler was successfully created, otherwise {@code false}.
     */
    boolean createScaler();

    /**
     * Retrieves the current scaling factor.
     *
     * @return The Fraction object representing the current scale.
     */
    @SuppressWarnings("unused")
    Fraction getCurrentScale();

    /**
     * @return The status of the execution.
     */
    String getStatus();

    /**
     * @return The WebDriver instance.
     */
    WebDriver wd();

    /**
     * @return The instance of WebDriverWait.
     */
    WebDriverWait wdw();

    /**
     * @return {@code true} if the application is in debug mode, {@code false} otherwise.
     */
    boolean isDebug();

    /**
     * @return {@code true} if the application is in trace mode, {@code false} otherwise.
     */
    boolean isTrace();

    /**
     * Provides an instance of the AppiumDriverLocalService.
     * The service is used to start, stop, or manage the local Appium server
     * for mobile application testing.
     *
     * @return An instance of AppiumDriverLocalService to manage the local Appium server.
     */
    AppiumDriverLocalService service();

    /**
     * Retrieves the instance of the `_Issues` interface that provides methods
     * to manage, query, and interact with various issues and associated log records.
     *
     * @return An instance of `_Issues` for issue management and querying.
     */
    _Issues issues();

    /**
     * Provides access to the `_Scenarios` interface, which defines the contract for managing
     * execution scenarios and their logical depth in a stack-based structure.
     * This method enables interactions with scenarios during the execution process and allows
     * operations such as pushing, popping, and querying scenarios.
     *
     * @return The `_Scenarios` instance for managing execution scenarios.
     */
    _Scenarios scenarios();

}
