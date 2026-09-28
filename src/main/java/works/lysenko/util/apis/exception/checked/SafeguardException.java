package works.lysenko.util.apis.exception.checked;

/**
 * The SafeguardException class represents an exception that occurs during the execution of a safeguard.
 * It is a subclass of the BotException class.
 * <p>
 * This exception class provides a constructor to create an instance of the exception with a message and details.
 * The details parameter is used to provide additional information related to the exception.
 *
 * @see BotException
 */
@SuppressWarnings("CheckedExceptionClass")
public class SafeguardException extends BotException {

    /**
     * Creates a new instance of SafeguardException with the given message and details.
     *
     * @param message the detail message explaining the reason for the exception (may be null)
     * @param details additional information related to the exception (may be null)
     * @throws NullPointerException if either message or details is null
     */
    public SafeguardException(final String message, final String details) {

        super(message, new IllegalStateException(details));
    }
}
