package works.lysenko.util.apis;

/**
 * Represents a generic property enumerator providing methods to retrieve
 * property-specific information and values with customizable behavior.
 */
public interface _PropEnum {

    /**
     * Retrieves the default value associated with the property.
     *
     * @return the default value of the property as a string
     */
    String defaultValue();

    /**
     * Retrieves the value associated with the property.
     * If the property is not set, it provides a default value based on the property type.
     *
     * @param <T> the type of the property value
     * @return the value of the property, or the default value if the property is not set
     */
    <T> T get();

    /**
     * Retrieves the name of the property represented by the enumerator.
     *
     * @return the name of the property as a string
     */
    String getPropertyName();

    /**
     * Determines whether the property interactions or retrieves should operate silently.
     * When set to true, operations related to the property may suppress logs or exceptions,
     * depending on the implementation.
     *
     * @return true if the property should operate silently, false otherwise
     */
    @SuppressWarnings("BooleanMethodNameMustStartWithQuestion")
    boolean silent();

    /**
     * Retrieves the type of the property associated with this property enumerator.
     *
     * @return the class type of the property
     */
    Class<?> type();
}
