package works.lysenko.util.apis.action;

import works.lysenko.util.apis.exception.checked.SafeguardException;

/**
 * The Browses interface represents an action for navigating through a User Interface.
 * It is a functional interface with a single abstract method, browse(), which may throw a SafeguardException.
 */
@FunctionalInterface
public interface Browses {

    /**
     * Navigates through User Interface.
     *
     * @throws SafeguardException if an exception occurs during the execution of a safeguard
     */
    void browse() throws SafeguardException;
}
