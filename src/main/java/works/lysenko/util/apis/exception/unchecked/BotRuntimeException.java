package works.lysenko.util.apis.exception.unchecked;

/**
 * The BotRuntimeException class is a custom exception that extends the RuntimeException class.
 * It is used to represent exceptional conditions that can occur within a bot application.
 * <p>
 * This exception provides constructors to set the exception message, underlying cause, and optional data object.
 * It also provides a method to retrieve the stored data object.
 */
@SuppressWarnings({"serial", "ClassWithoutNoArgConstructor", "unused", "ClassWithTooManyTransitiveDependents", "FinalMethod",
        "ParameterHidesMemberVariable", "UncheckedExceptionClass"})
public class BotRuntimeException extends RuntimeException {

    private final Object data;


    /**
     * Creates a new instance of BotRuntimeException with the given message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the underlying exception that caused this exception (may be null)
     * @since 1.0
     */
    @SuppressWarnings("AssignmentToNull")
    public BotRuntimeException(final String message, final Exception cause) {

        super(message, cause);
        data = null;
    }

    /**
     * Constructs a new instance of BotRuntimeException with the given message, cause, and data.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the underlying RuntimeException that caused this exception (maybe null)
     * @param data    an optional data object to store additional information related to the exception
     */
    BotRuntimeException(final String message, final RuntimeException cause, final Object data) {

        super(message, cause);
        this.data = data;
    }

    /**
     * Retrieves the data object associated with the exception.
     *
     * @return the data object stored in the exception
     */
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    public final Object getData() {

        return data;
    }
}
