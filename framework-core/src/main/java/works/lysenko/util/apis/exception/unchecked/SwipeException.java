package works.lysenko.util.apis.exception.unchecked;

import works.lysenko.util.apis.exception.checked.BotException;

/**
 * The SwipeException class is a custom exception that extends the BotRuntimeException class.
 * It is used to represent exceptional conditions that can occur during swiping in a bot application.
 * <p>
 * This exception provides a constructor to create instances of the exception with a message.
 * <p>
 * This class inherits the constructors, getter, and setter methods from the BotRuntimeException class,
 * which allows setting the exception message, underlying cause, and optional data object.
 * It also inherits the getData() method to retrieve the stored data object.
 */
@SuppressWarnings({"UncheckedExceptionClass", "ClassWithoutNoArgConstructor"})
public class SwipeException extends works.lysenko.util.apis.exception.unchecked.BotRuntimeException {

    /**
     * Constructs a new instance of TestStopException with the given message and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @see BotException
     */
    public SwipeException(final String message) {

        super(message, null, null);
    }
}
