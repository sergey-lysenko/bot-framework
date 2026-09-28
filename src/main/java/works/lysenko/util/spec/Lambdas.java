package works.lysenko.util.spec;

/**
 * Defines a class to hold lambda expressions.
 * <p>
 * This class currently provides a single lambda expression:
 * - NOP: A1 no-operation Runnable that performs no action when run.
 */
@SuppressWarnings({"ClassWithoutLogger", "StaticMethodOnlyUsedInOneClass"})
public record Lambdas() {

    /**
     * A1 no-operation Runnable that performs no action when run.
     */
    public static final Runnable NOP = () -> {
        // No-Operation
    };
}
