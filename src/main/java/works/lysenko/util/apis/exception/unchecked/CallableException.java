package works.lysenko.util.apis.exception.unchecked;

/**
 * The CallableException class is a custom exception that represents an exception that occurs
 * during the execution of a scenario. It is a subclass of the BotRuntimeException class.
 * <p>
 * This class provides a constructor to set the exception message and the underlying cause of
 * the exception.
 *
 * @see BotRuntimeException
 */
@SuppressWarnings({"UncheckedExceptionClass", "unused"})
public class CallableException extends BotRuntimeException {

    /**
     * Represents an exception that occurs during the execution of a scenario.
     * It is a subclass of the BotRuntimeException class.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the underlying exception that caused this exception (may be null)
     */
    public CallableException(final String message, final Exception cause) {

        super(message, cause);
    }
}
