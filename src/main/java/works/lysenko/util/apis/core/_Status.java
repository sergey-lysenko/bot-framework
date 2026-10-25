package works.lysenko.util.apis.core;

/**
 * ProvidesStatus interface defines a method for retrieving status information.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
@FunctionalInterface
public interface _Status {

    /**
     * Retrieves the status information.
     *
     * @return the Status information as a string.
     */
    String get();
}
