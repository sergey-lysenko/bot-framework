package works.lysenko.util.apis.execution;

/**
 * The _Default interface is a functional interface that provides a method to get a default string value.
 * This can be implemented by classes or enums to standardize retrieval of default values across different entities.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
@FunctionalInterface
public interface _Default {

    /**
     * Retrieves the default string value.
     *
     * @return default string value
     */
    String def();
}
