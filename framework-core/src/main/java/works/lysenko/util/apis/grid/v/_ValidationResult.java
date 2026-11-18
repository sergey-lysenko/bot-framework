package works.lysenko.util.apis.grid.v;

/**
 * Represents the result of a validation process.
 */
public interface _ValidationResult {

    /**
     * NOT_OK and further
     */
    int FAILED = 3;

    /**
     * Checks if the result is considered as passed.
     *
     * @return true if the result is considered passed, false otherwise
     */
    boolean isPassed();

    /**
     * Returns the name associated with this object.
     *
     * @return the name of the object
     */
    String name();

    /**
     * Renders the object to a string representation based on its internal state.
     *
     * @return the rendered string representation of the object
     */
    String render();
}
