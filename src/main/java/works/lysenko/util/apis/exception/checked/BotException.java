package works.lysenko.util.apis.exception.checked;

/**
 * The BotException class represents an exception that occurs during the execution of a bot.
 * It is a subclass of the Exceptions class.
 * <p>
 * This exception class provides constructors to create instances of the exception with a message,
 * an underlying RuntimeException cause, and an optional data object.
 * The data object can be used to store additional information related to the exception.
 */
@SuppressWarnings({"serial", "ClassWithoutNoArgConstructor", "unused", "ClassWithTooManyTransitiveDependents", "FinalMethod",
        "ParameterHidesMemberVariable", "CheckedExceptionClass"})
public class BotException extends Exception {

    private final Object data;

    /**
     * The BotException class represents an exception that occurs during the execution of a bot.
     * It is a subclass of the Exceptions class.
     * <p>
     * This exception class provides a constructor to create an instance of the exception with a message
     * and an underlying RuntimeException cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the underlying RuntimeException that caused this exception (may be null)
     */
    @SuppressWarnings({"AssignmentToNull", "WeakerAccess"})
    public BotException(final String message, final RuntimeException cause) {

        super(message, cause);
        data = null;
    }

    /**
     * The BotException class represents an exception that occurs during the execution of a bot.
     * It is a subclass of the Exceptions class.
     * <p>
     * This exception class provides constructors to create instances of the exception with a message,
     * an underlying RuntimeException cause, and an optional data object.
     * The data object can be used to store additional information related to the exception.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the underlying RuntimeException that caused this exception (may be null)
     * @param data    an optional data object to store additional information related to the exception
     */
    public BotException(final String message, final RuntimeException cause, final Object data) {

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
