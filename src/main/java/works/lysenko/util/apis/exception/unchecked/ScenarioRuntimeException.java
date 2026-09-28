package works.lysenko.util.apis.exception.unchecked;

import works.lysenko.util.apis.scenario._Scenario;

/**
 * Represents an exception that occurs during the execution of a scenario.
 * It is a subclass of the BotRuntimeException class.
 */
@SuppressWarnings("UncheckedExceptionClass")
public class ScenarioRuntimeException extends BotRuntimeException {

    /**
     * Represents a runtime exception that occurs during the execution of a scenario.
     *
     * @param message  describing situation
     * @param cause    exception
     * @param scenario caused the Exceptions
     */
    public ScenarioRuntimeException(final String message, final RuntimeException cause, final _Scenario scenario) {

        super(message, cause, scenario);
    }
}
