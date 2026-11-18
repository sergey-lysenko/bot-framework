package works.lysenko.util.apis.grid.v;

/**
 * Interface representing a collection of validation results.
 */
public interface _ValidationResults {

    /**
     * Adds a validation result to the collection.
     *
     * @param result the validation result to be added
     */
    void add(_ValidationResult result);

    /**
     * Checks whether the object is empty.
     *
     * @return true if the object is empty, false otherwise
     */
    boolean isEmpty();

    /**
     * Returns the names concatenated together with a comma and a space.
     *
     * @return the concatenated names
     */
    String names();

    /**
     * Returns the size of an object, typically representing the number of elements it contains.
     *
     * @return the size of the object
     */
    long size();
}
