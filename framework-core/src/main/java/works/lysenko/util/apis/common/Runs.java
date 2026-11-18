package works.lysenko.util.apis.common;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * The Runs interface defines methods for executing code blocks and adding section titles to the test log.
 */
@SuppressWarnings({"BooleanMethodNameMustStartWithQuestion", "InterfaceWithOnlyOneDirectInheritor"})
public interface Runs {

    /**
     * Add a section title to the test log and execute the provided code block.
     *
     * @param title           The string to be added as the section title.
     * @param booleanSupplier The code block to be executed for the section.
     * @return the result of the booleanSupplier code block.
     */
    Boolean runForBoolean(String title, Supplier<Boolean> booleanSupplier);

    /**
     * Add a section title to the test log and execute the provided code block.
     *
     * @param title           The string to be added as the section title.
     * @param integerSupplier The code block to be executed for the section.
     * @return the result of the booleanSupplier code block.
     */
    Integer runForInteger(String title, Supplier<Integer> integerSupplier);

    /**
     * Adds a section title to the test log and executes the provided Callable.
     *
     * @param title    The string to be added as the section title.
     * @param callable The Callable representing the code block to be executed for the section.
     */
    void section(String title, Callable<?> callable);

    /**
     * Add a section title only.
     *
     * @param title The string to be added as the section title.
     */
    void section(String title);

    /**
     * Add a section title to the test log and execute the provided code block.
     *
     * @param title    The string to be added as the section title.
     * @param runnable The code block to be executed for the section.
     */
    void section(String title, Runnable runnable);
}
