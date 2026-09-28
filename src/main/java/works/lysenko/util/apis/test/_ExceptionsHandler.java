package works.lysenko.util.apis.test;

import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.scenario._Scenario;

/**
 * The _ExceptionsHandler interface defines methods for handling exceptions and managing
 * the state of execution in the event of an exceptional scenario. It provides mechanisms
 * to retrieve and modify a stop flag, handle runtime exceptions, and set scenarios as exceptional.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanMethodNameMustStartWithQuestion"})
public interface _ExceptionsHandler {

    /**
     * Retrieves the exception associated with this handler.
     *
     * @return the Exception instance held by this handler, or null if no exception has been set
     */
    Exception getException();

    /**
     * Handles the given RuntimeException by applying appropriate measures.
     * This may include managing the exception or re-throwing it as a SafeguardException.
     *
     * @param exception the RuntimeException instance to be handled
     * @throws SafeguardException if the exception handling process requires escalation to a checked exception
     */
    void handle(RuntimeException exception) throws SafeguardException;

    /**
     * Sets the given scenario as exceptional, marking it for exceptional handling or processing.
     *
     * @param scenario the scenario to be set as exceptional
     */
    void setExceptional(_Scenario scenario);
}