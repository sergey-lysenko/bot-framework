package works.lysenko.util.apis.util;

/**
 * Functional interface providing a boolean status check.
 */
@FunctionalInterface
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _isOk {

    /**
     * Checks if condition or status is OK.
     *
     * @return true if condition is OK, false otherwise
     */
    boolean isOk();
}
