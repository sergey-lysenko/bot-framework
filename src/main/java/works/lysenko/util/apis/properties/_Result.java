package works.lysenko.util.apis.properties;

/**
 * Represents a generic result interface to retrieve the value of a property.
 *
 * @param <T> the type of the property value
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Result<T> {

    /**
     * Retrieves the result as type T.
     *
     * @return the result cast to the appropriate type T
     */
    T result();
}