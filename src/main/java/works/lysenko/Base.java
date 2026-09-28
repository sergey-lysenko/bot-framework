package works.lysenko;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import works.lysenko.base.*;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.prop.tree.Retries;
import works.lysenko.util.spec.Level;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import static works.lysenko.util.data.enums.Ansi.mb;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.core.Assertions.еггог;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.A.ATTEMPT;
import static works.lysenko.util.spec.Numbers.ZERO;

/**
 * The Base class provides various utility methods for interacting with web elements, handling
 * execution-related operations, retrieving data, and logging events.
 * <p>
 * Fields:
 * - stringArray: A property representing an array of strings.
 * - core: Core execution or functionality property.
 * - exec: Represents the execution context or data.
 * - properties: Configuration or properties associated with the execution.
 * - parameters: A collection of parameters for execution.
 * - timer: Represents or manages timing-related operations or functionality.
 * <p>
 * Methods:
 * - clickOn: Facilitates click operations on elements identified by locators or WebElements with optional waiting behaviour.
 * - clickOnText: Performs a click action on specified text.
 * - containsKey: Checks for the presence of specified fields in the execution context, with optional silent logging.
 * - currentDepth: Retrieves the current stack depth of execution scenarios.
 * - describe: Returns a description of a WebElement.
 * - failAndFalse: Logs the given message and always returns false.
 * - failAndNull: Logs the given message and always returns null.
 * - find: Finds and returns a single WebElement using a locator.
 * - findAll: Finds and returns multiple WebElements using a locator, with optional exposure.
 * - findEach: Finds and returns a list of WebElements for multiple locators provided in a collection.
 * - getBoolean: Retrieves boolean values from execution context using field names, titles, or suppliers with default values.
 * - getInteger: Retrieves integer values from the execution context with optional default values and silent execution.
 * - getString: Retrieves string representations from execution context with support for defaults, silence, and null handling.
 * - getWebDriver: Retrieves the WebDriver instance.
 * - isAllPresent: Checks whether given elements or locators are present in the execution context.
 * - isDebug: Determines if the application is in debug mode.
 * - isDone: Executes a Runnable task and verifies completion using a Callable verifier.
 */
@SuppressWarnings({"ClassWithTooManyMethods", "OverlyComplexClass", "StaticMethodOnlyUsedInOneClass", "StaticNonFinalField", "StaticCollection", "WeakerAccess", "unused"})
public record Base() {

    /**
     * This is used only as a type reference in toArray() call.
     */
    private static final String[] stringArray = new String[0];

    /**
     * A static variable of type Test initialised to null.
     * This variable can be accessed and modified directly as it is declared public.
     */
    public static Test test = null;

    /**
     * The Core class represents a reference to the Core component of the Core.
     * It is a static variable and can be accessed via the Core class.
     * <p>
     * This class is part of the Core package and can be accessed from any part of the application.
     * It provides access to various functionalities and components required for the execution of the application.
     */
    public static Core core = null;

    /**
     * Represents an execution.
     * <p>
     * The execution variable can be used to store an instance of the Exec class.
     * It is declared as public and static, meaning that it can be accessed and modified
     * from anywhere in the code without the need to create an instance of the containing class.
     * The initial value of the execution variable is null, indicating that it has not been
     * assigned an instance of the Exec class yet.
     */
    public static Exec exec = null;

    /**
     * The TestProperties class represents a set of properties for the test. It provides access to various test
     * configuration options.
     * <p>
     * The properties of the TestProperties class are:
     * - defaultValues: A1 LinkedHashMap representing the default values for the properties.
     * - commonConfiguration: A1 Map representing the common configuration properties.
     * - testProperties: A1 Properties object representing the test-specific properties.
     * <p>
     * This class is part of the TestFramework package and can be accessed from any part of the application.
     * It implements the ProvidesTestProperties interface to provide access to the test properties.
     */
    public static TestProperties properties = null;

    /**
     * The Parameters class represents a set of parameters for the application.
     * It provides access to various configuration options and settings required for the execution of the application.
     * <p>
     * This class is part of the Core package and can be accessed from any part of the application.
     * It implements the ProvidesParameters interface to provide access to the parameters.
     */
    public static Parameters parameters = null;

    /**
     * Utility class providing a static stopwatch instance for measuring elapsed time.
     * This stopwatch can be started, stopped, and reset as needed.
     */
    public static Stopwatch timer = null;

    /**
     * Clicks on an element identified by the given locator.
     *
     * @param locator The locator used to identify the element.
     */
    public static void clickOn(final String locator) {

        exec.clickOn(locator);
    }

    /**
     * Clicks on the given WebElement.
     *
     * @param element The WebElement to click on.
     */
    public static void clickOn(final WebElement element) {

        exec.clickOn(element);
    }

    /**
     * Clicks on an element identified by the given locator.
     *
     * @param thenWaitForInvisibilityOf true if waiting for invisibility of an element after clicking, false otherwise
     * @param locator                   The locator used to identify the element.
     */
    public static void clickOn(final boolean thenWaitForInvisibilityOf, final String locator) {

        exec.clickOn(thenWaitForInvisibilityOf, locator);
    }

    /**
     * Performs a click operation on the given WebElement and waits for a specified duration after the click.
     *
     * @param element   The WebElement to click on.
     * @param waitAfter The duration to wait after the click operation in milliseconds.
     */
    public static void clickOn(final WebElement element, final long waitAfter) {

        exec.clickOn(element, waitAfter);
    }

    /**
     * Performs a click action on the specified text.
     *
     * @param text the text to click on
     */
    public static void clickOnText(final String text) {

        exec.clickOnText(text);
    }

    /**
     * Checks if the execution contains the specified field.
     *
     * @param field The field to check for existence in the execution.
     * @return true if the field exists in the execution, false otherwise.
     * @see Base#containsKey(Object, boolean)
     */
    public static boolean containsKey(final Object field) {

        return exec.data.containsKey(field);
    }

    /**
     * Checks if execution contains the specified field.
     *
     * @param field  The field to check for existence in the execution.
     * @param silent If true, no debugging information will be logged.
     * @return True if the field exists in the execution, false otherwise.
     */
    public static boolean containsKey(final Object field, final boolean silent) {

        return exec.data.containsKey(field, silent);
    }

    /**
     * Retrieves the current depth of the execution scenarios stack.
     *
     * @return The current depth of the execution scenarios stack.
     */
    public static int currentDepth() {

        return exec.scenarios().depth();
    }

    /**
     * Returns a description of the given WebElement.
     *
     * @param element The WebElement to describe.
     * @return A1 description of the given WebElement.
     */
    public static String describe(final WebElement element) {

        return exec.describe(element);
    }

    /**
     * Executes the given {@code Runnable} a specified number of times.
     *
     * @param runnable the {@code Runnable} to be executed
     * @param times    the number of times the {@code Runnable} should be executed
     */
    public static void exec(final Runnable runnable, final int times) {

        for (int i = 0; i < times; i++) {
            runnable.run();
        }
    }

    /**
     * Logs the provided message and returns false.
     *
     * @param message the message to be logged
     * @return false
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public static boolean failAndFalse(final String message) {

        logEvent(S0, message);
        return false;
    }

    /**
     * Logs an event with a given message and returns null.
     *
     * @param message the message to log
     * @return null
     */
    public static Object failAndNull(final String message) {

        logEvent(S0, message);
        return null;
    }

    /**
     * Finds a web value based on the given locator.
     *
     * @param locator The locator used to identify the value.
     * @return The web value matching the locator.
     */
    public static WebElement find(final String locator) {

        return exec.find(locator);
    }

    /**
     * Finds all web elements based on the given locator.
     *
     * @param locator The locator used to identify the elements.
     * @return A1 list of web elements matching the locator.
     */
    public static List<WebElement> findAll(final String locator) {

        return exec.findAll(locator);
    }

    /**
     * Finds all web elements based on the given locator.
     *
     * @param locator The locator used to identify the elements.
     * @param expose  True if the elements should be exposed, false otherwise.
     * @return A1 list of web elements matching the locator.
     */
    public static List<WebElement> findAll(final String locator, final boolean expose) {

        return exec.findAll(locator, expose);
    }

    /**
     * Finds and returns a list of web elements for each locator in the provided collection.
     *
     * @param locators a collection of locators to find web elements
     * @return a list of web elements found for each locator
     */
    public static List<WebElement> findEach(final Collection<String> locators) {

        final List<WebElement> result = new ArrayList<>(locators.size());
        for (final String locator : locators) result.add(exec.find(locator));
        return result;
    }

    /**
     * Retrieves a boolean value from the execution using the specified title and booleanSupplier.
     *
     * @param title           The title for the boolean value.
     * @param booleanSupplier A1 supplier function that provides the boolean value.
     * @return The boolean value retrieved from the execution.
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public static Boolean getBoolean(final String title, final Supplier<Boolean> booleanSupplier) {

        return exec.runForBoolean(title, booleanSupplier);
    }

    /**
     * Retrieves a boolean value from the execution using the specified field name and default value.
     *
     * @param field The name of the field.
     * @param def   The default value to return if the field does not exist.
     * @return The boolean value of the field, or the default value if the field does not exist.
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public static Boolean getBoolean(final String field, final Boolean def) {

        return exec.data.getBoolean(field, def);
    }

    /**
     * Retrieves an integer value from the execution using the specified field.
     *
     * @param field The field identifier.
     * @return The integer value of the field.
     */
    public static int getInteger(final Object field) {

        return exec.data.getInteger(field);
    }

    /**
     * Retrieves an integer value from the execution using the specified field.
     *
     * @param field The field identifier.
     * @param def   The default value to return if the field does not exist.
     * @return The integer value of the field.
     */
    public static int getInteger(final Object field, final int def) {

        return exec.data.getInteger(field, def);
    }

    /**
     * Retrieves the integer value of the specified field.
     *
     * @param field  the field object to retrieve the integer value from
     * @param silent a boolean value indicating whether to suppress any exceptions that may occur during retrieval
     * @return the integer value of the specified field
     */
    public static int getInteger(final Object field, final boolean silent) {

        return exec.data.getInteger(field, silent);
    }

    /**
     * Retrieves a long value associated with the specified field.
     *
     * @param field The field for which the long value is to be retrieved. It must be a valid key or identifier.
     * @return The long value corresponding to the given field.
     */
    public static long getLong(final Object field) {

        return exec.data.getLong(field);
    }

    /**
     * Retrieves a long value associated with the specified field.
     * If the field is not found or cannot be converted to along,
     * the provided default value is returned.
     *
     * @param field the field whose associated long value is to be retrieved
     * @param def the default value to return if the field is not found or cannot be converted
     * @return the long value associated with the specified field, or the default value if unavailable
     */
    public static long getLong(final Object field, final long def) {

        return exec.data.getLong(field, def);
    }

    /**
     * Retrieves a long value from the specified field.
     *
     * @param field the object representing the field from which the long value is to be retrieved
     * @param silent a boolean indicating whether the retrieval should proceed silently; if true, exceptions or errors may be suppressed
     * @return the long value extracted from the specified field
     */
    public static long getLong(final Object field, final boolean silent) {

        return exec.data.getLong(field, silent);
    }

    /**
     * Returns a string representation of the given field.
     *
     * @param field the field to get a string representation of
     * @return a string representation of the given field
     */
    public static String getString(final Object field) {

        return exec.data.getString(field);
    }

    /**
     * Retrieves the string representation of a given field by calling the execution's getString method.
     *
     * @param field  the field to retrieve the string representation of
     * @param silent true if the method should execute silently, false otherwise
     * @return the string representation of the given field
     */
    public static String getString(final Object field, final boolean silent) {

        return exec.data.getString(field, silent);
    }

    /**
     * Gets the value of a field as a string.
     *
     * @param field the field from which to get the value
     * @param def   the default value to return if the field is null or cannot be converted to a string
     * @return the string value of the field, or the default value if the field is null or cannot be converted to a string
     */
    public static String getString(final Object field, final String def) {

        return exec.data.getString(field, def);
    }

    /**
     * Returns the string representation of the given field.
     *
     * @param field  the object to retrieve the string representation from
     * @param def    the default string value to return if the field is null
     * @param silent a flag indicating whether the method should throw an exception if the field is null
     *               or silently return the default string value
     * @return the string representation of the field or the default string value if the field is null
     * and a silent flag is set to true
     */
    public static String getString(final Object field, final String def, final boolean silent) {

        return exec.data.getString(field, def, silent);
    }

    /**
     * Retrieves the WebDriver instance.
     *
     * @return The WebDriver instance.
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static WebDriver getWebDriver() {

        if (isNotNull(exec)) return exec.wd();
        return null;
    }

    /**
     * Checks if all the elements in the given list are present in another source, typically an array.
     *
     * @param locators The list of elements to check for.
     * @return true if all the elements are present, false otherwise.
     */
    public static boolean isAllPresent(final List<String> locators) {

        return isAllPresent(locators.toArray(stringArray));
    }

    /**
     * Checks if all the given locators are present.
     *
     * @param locators A1 variable number of locators to be checked.
     * @return true if all the locators are present, false otherwise.
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isAllPresent(final String... locators) {

        boolean result = true;

        for (final String locator : locators)
            if (!isPresent(locator)) result = false;
        return result;
    }

    /**
     * Determines if the debug mode is enabled.
     *
     * @return true if debug mode is enabled, false otherwise
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static boolean isDebug() {

        if (isNotNull(exec)) return exec.isDebug();
        return false;
    }

    /**
     * Checks if a given {@link Runnable} completes successfully and satisfies a verification condition.
     *
     * @param title    The title of the task.
     * @param runnable the {@link Runnable} to execute and verify
     * @param verifier the {@link Callable<Boolean>} to verify the result of the {@link Runnable}
     * @return {@code true} if the {@link Runnable} completes successfully and the verification condition is satisfied,
     * {@code false} otherwise
     */
    public static boolean isDone(final String title, final Runnable runnable, final Callable<Boolean> verifier) {

        return isDone(title, runnable, verifier, Retries.action);
    }

    /**
     * Executes a given task and verifies its completion status with a specified number of retries.
     * Logs each attempt and retries the task if the verification fails.
     *
     * @param title    the title of the task to log before execution
     * @param runnable the task to be executed
     * @param verifier the verification logic to check the result of the task
     * @param retries  the number of times to retry the task if the verification fails
     * @return true if the task is verified successfully within the number of retries, false otherwise
     */
    @SuppressWarnings({"ProhibitedExceptionThrown", "ValueOfIncrementOrDecrementUsed", "SameParameterValue"})
    private static boolean isDone(final String title, final Runnable runnable, final Callable<Boolean> verifier,
                                  final int retries) {

        boolean result;
        int attempts = retries;

        if (ZERO < attempts)
            section(title);
        do {
            log(b(c(ATTEMPT), s(retries - attempts + 1)));
            runnable.run();
            try {
                result = verifier.call();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        } while (ZERO < --attempts && !result);

        return result;
    }

    /**
     * Checks if a value identified by the given locator is present.
     *
     * @param locator The locator used to identify the value.
     * @return True if the value is present, false otherwise.
     */
    public static boolean isPresent(final String locator) {

        return exec.isPresent(locator);
    }

    /**
     * Determines if a value identified by the given locator is present.
     *
     * @param locator The locator used to identify the value.
     * @param silent  If true, no debugging information will be logged.
     * @return True if the value is present, false otherwise.
     */
    public static boolean isPresent(final String locator, final boolean silent) {

        return exec.isVisible(locator, silent);
    }

    /**
     * Determines if the trace mode is enabled.
     *
     * @return true if trace mode is enabled, false otherwise
     */
    @SuppressWarnings("MethodWithMultipleReturnPoints")
    public static boolean isTrace() {

        if (isNotNull(exec)) return exec.isTrace();
        return false;
    }

    /**
     * Logs a message at the given log level.
     *
     * @param level    the log level (0 for lowest, higher values for higher levels)
     * @param message  the message to be logged
     * @param addStack if true, and if in Debug mode, the stack trace will be logged along with the message
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void log(final int level, final String message, final boolean addStack) {

        if (isNotNull(exec)) exec.log(level, message, addStack);
        else System.out.println(mb(message));
    }

    /**
     * Logs a message.
     *
     * @param message the message to be logged
     */
    public static void log(final String message) {

        if (isNotNull(exec)) exec.log(message);
    }

    /**
     * Logs a debug message with an optional stack trace.
     *
     * @param message  the debug message to be logged
     * @param addStack flag indicating if the stack trace should be added to the log message
     */
    public static void logDebug(final String message, final boolean addStack) {

        if (isNotNull(exec)) exec.logDebug(message, addStack);
    }

    /**
     * Logs a debug message.
     *
     * @param message the debug message to be logged
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void logDebug(final String message) {

        if (isNotNull(exec)) exec.logDebug(message);
        else System.out.println(mb(message));
    }

    /**
     * Logs a debug message.
     *
     * @param message the debug message to be logged
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void logDebug(final Iterable<String> message) {

        for (final String line : message) {
            if (isNotNull(exec)) exec.logDebug(line);
            else System.out.println(mb(line));
        }
    }

    /**
     * Logs an empty line.
     * <p>
     * This method checks if the Exec object is not null, and if it is not, calls the {@code logEmptyLine()} method of
     * the Exec object.
     * It is used to log an empty line in the logging output.
     */
    public static void logEmptyLine() {

        if (isNotNull(exec)) exec.logEmptyLine();
    }

    /**
     * Logs an empty line.
     *
     * <p>
     * This method checks if the Exec object is not null, and if it is not, calls the {@code logEmptyLine()}
     * method of the Exec object. It is used to log an empty line in the logging output.
     *
     * @param addStack flag indicating if the stack trace should be added to the log message
     */
    public static void logEmptyLine(final boolean addStack) {

        if (isNotNull(exec)) exec.logEmptyLine(addStack);
    }

    /**
     * Logs an empty line.
     * <p>
     * This method checks if the Exec object is not null, and if it is not,
     * calls the {@code logEmptyLine()} method of the Exec object.
     * It is used to log an empty line in the logging output.
     *
     * @param addStack        flag indicating if the stack trace should be added to the log message
     * @param forceUnfiltered flag indicating if the log should be printed even if it is filtered by the logging level
     */
    public static void logEmptyLine(final boolean addStack, final boolean forceUnfiltered) {

        if (isNotNull(exec)) exec.logEmptyLine(addStack, forceUnfiltered);
    }

    /**
     * Logs an event with the specified severity and message.
     *
     * @param severity the severity of the event
     * @param message  the message of the event
     */
    public static void logEvent(final Severity severity, final String message) {

        if (isNotNull(exec)) exec.logEvent(severity, message);
        else еггог(message);
    }

    /**
     * Logs a trace message.
     *
     * @param message the trace message to be logged
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public static void logTrace(final String message) {

        if (isNotNull(exec)) exec.logTrace(message);
        else System.out.println(mb(message));
    }

    /**
     * Puts a value into a specified field in the execution map.
     *
     * @param field The field where the value will be stored.
     * @param value The value to be stored in the field.
     */
    public static void put(final Object field, final Object value) {

        exec.data.put(field, value);
    }

    /**
     * Reads the value from a specified locator.
     *
     * @param locator the locator to read the value from
     * @return the value read from the locator
     */
    public static String read(final String locator) {

        return exec.read(locator);
    }

    /**
     * Reads the data from the given locator with the specified number of retries.
     *
     * @param locator the locator to identify the data
     * @param retries the number of times to retry reading the data if it fails
     * @return the data read from the locator
     */
    public static String read(final String locator, final int retries) {

        return exec.read(locator, retries);
    }

    /**
     * Reads the text value of a WebElement.
     *
     * @param element the WebElement to read the text value from
     * @return the text value of the given WebElement
     */
    public static String read(final WebElement element) {

        return exec.read(element);
    }

    /**
     * Removes the specified field.
     *
     * @param field the field to be removed
     */
    public static void remove(final Object field) {

        exec.data.remove(field);
    }

    /**
     * Scrolls to the specified text on the screen.
     *
     * @param title the text to be scrolled to
     */
    public static void scrollToText(final String title) {

        exec.scrollToText(title);
    }

    /**
     * Searches for keys in the execution using the given substrings.
     *
     * @param substrings The substrings to search for.
     * @return A1 set of keys that contain the given substrings.
     */
    public static Set<String> searchKeys(final String... substrings) {

        return exec.data.searchKeys(substrings);
    }

    /**
     * Searches for keys in the execution based on the given value.
     *
     * @param value The value to search for in the execution.
     * @return A1 {@code Set} containing the keys found in the execution associated with the given value.
     */
    public static Set<String> searchKeysByValue(final String value) {

        return exec.data.searchKeysByValue(value);
    }

    /**
     * Adds a section with the given title to the execution output.
     *
     * @param title the title of the section to be added
     */
    public static void section(final String title) {

        exec.section(title);
    }

    /**
     * Executes a section of code and groups it under a specific title.
     *
     * @param title    the title of the section
     * @param runnable the code to be executed
     */
    public static void section(final String title, final Runnable runnable) {

        exec.section(title, runnable);
    }

    /**
     * Pauses the execution of the current thread for the specified duration in milliseconds.
     *
     * @param duration the duration in milliseconds for which to pause the thread
     */
    public static void sleep(final long duration) {

        exec.sleep(duration);
    }

    /**
     * Causes the current thread to sleep for a medium amount of time.
     * The exact duration of sleep may vary depending on the system.
     * This method can be used to introduce delays in code execution.
     */
    public static void sleepMedium() {

        exec.sleepMedium();
    }

    /**
     * Causes the current thread to sleep for a short amount of time.
     * The exact duration of sleep may vary depending on the system.
     * This method can be used to introduce delays in code execution.
     */
    public static void sleepShort() {

        exec.sleepShort();
    }

    /**
     * Logs a success message with a specified message.
     *
     * @param message the success message to be logged
     * @return always true
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    public static boolean success(final String message) {

        log(Level.none, message, true);
        return true;
    }

    /**
     * Waits for the specified text to appear and then proceeds with the execution.
     *
     * @param text The text to wait for.
     */
    public static void tick(final String text) {

        exec.waitForText(text);
    }

    /**
     * Executes the tick operation for a specific user and name.
     *
     * @param user the user for which the tick operation is to be executed
     * @param name the name for which the tick operation is to be executed
     */
    public static void tick(final String user, final String name) {

        exec.data.tick(user, name);
    }

    /**
     * Waits for the specified text to become invisible on the page.
     *
     * @param text the text to wait for invisibility
     */
    public static void waitForInvisibilityOfText(final String text) {

        exec.waitForInvisibilityOfText(text);
    }

    /**
     * Waits for the specified text to appear on the screen.
     * This method will block until the text is found or a timeout occurs.
     *
     * @param text the text to wait for
     */
    public static void waitForText(final String text) {

        exec.waitForText(text);
    }
}