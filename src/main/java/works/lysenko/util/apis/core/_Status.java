package works.lysenko.util.apis.core;

/**
 * ProvidesStatus interface defines a method for retrieving status information.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
@FunctionalInterface
public interface _Status {

    /**
     * @return the Status information.
     */
    String get();
}
