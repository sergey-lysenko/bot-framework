package works.lysenko.util.apis.action;

import works.lysenko.util.apis.exception.checked.SafeguardException;

/**
 * Verifies User Interface
 * Functional interface that provides a method to verify the user interface.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
@FunctionalInterface
public interface Verifies {

    /**
     * Verifies the User Interface.
     *
     * @return true if the verification of the User Interface is successful, false otherwise.
     * @throws SafeguardException if an error occurs during the verification process.
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    boolean verify() throws SafeguardException;
}
