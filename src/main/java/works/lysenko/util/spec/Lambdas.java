package works.lysenko.util.spec;

/**
 * Defines a class to hold lambda expressions and functional constants.
 * <p>
 * This class provides common runnable actions such as a no-operation Runnable.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass"})
public record Lambdas() {

    /**
     * A no-operation Runnable that performs no action when run.
     */
    public static final Runnable NOP = () -> {
        // No-Operation
    };
}
