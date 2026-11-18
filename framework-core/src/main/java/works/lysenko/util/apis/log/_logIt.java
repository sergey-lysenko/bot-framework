package works.lysenko.util.apis.log;

/**
 * Functional interface for logging operations that execute without parameters or return values.
 */
@FunctionalInterface
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _logIt {

    /**
     * Executes the logging operation.
     */
    void logIt();
}
