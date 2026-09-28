package works.lysenko.util.apis.properties;

import works.lysenko.util.apis._PropEnum;

import java.util.Map;

/**
 * Provides Test Properties interface
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanParameter"})
public interface _TestProperties {

    /**
     * @return true if test properties are accessible
     */
    boolean areTestPropertiesReady();

    /**
     * Retrieves the value of a test property of the specified type and name.
     *
     * @param <T>  the type of the property value to retrieve
     * @param type the class representing the type of the property value
     * @param name the name of the property to retrieve
     * @return the value of the test property converted to the specified type
     */
    <T> T get(final Class<T> type, final String name);

    /**
     * Retrieves the value of a test property of the specified type and name.
     *
     * @param <T>    the type of the property value to retrieve
     * @param type   the class representing the type of the property value
     * @param name   the name of the property to retrieve
     * @param silent if true, suppresses log messages and does not throw an exception if the property is not found or value
     *               is null
     * @return the value of the test property converted to the specified type
     */
    <T> T get(final Class<T> type, final String name, final boolean silent);

    /**
     * Retrieves an enumeration value associated with the specified property.
     *
     * @param <T> the type of the enumeration value to retrieve
     * @param p   the property enumeration which defines the property
     * @return the enumeration value of the specified property
     */
    <T> T getEnum(_PropEnum p);

    /**
     * Retrieves the source value of a test property specified by the given property enumeration.
     *
     * @param p the property enumeration which specifies the test property
     * @return the source value of the specified test property, or its default value if not found
     */
    String getEnumTestPropertySource(_PropEnum p);

    /**
     * @return entrySet of test properties
     */
    Iterable<? extends Map.Entry<Object, Object>> getPropertiesEntrySet();

    /**
     * Retrieves the value of a property by key.
     *
     * @param key The key of the property.
     * @param def The default value to return if the property is not found.
     * @return The value of the property, or the default value if the property is not found.
     */
    @Deprecated
    Object getProperty(String key, String def);

    /**
     * Retrieves the sorted test properties as a map.
     * The returned map will contain the test properties sorted by key in ascending order.
     *
     * @return The sorted test properties.
     */
    Map<String, String> getSorted();

    /**
     * Retrieves the Boolean value associated with the specified property name.
     *
     * @param name   The name of the property for the Boolean value.
     * @param silent If true, suppresses log messages and does not throw an exception if the property is not found.
     * @return The Boolean value of the specified property.
     */
    String getTestPropertySource(String name, boolean silent);

    /**
     * Checks if the given value is a common value for the specified property name.
     *
     * @param name  The name of the property.
     * @param value The value to check.
     * @return True if the value is common for the property, false otherwise.
     */
    boolean isCommonValue(String name, String value);

    /**
     * Inserts or updates a property in the test properties.
     *
     * @param name  the name of the property
     * @param value the value of the property
     */
    @SuppressWarnings("unused")
    void put(String name, Object value);

    /**
     * Reads the common configuration properties and returns a debug message.
     *
     * @return A1 debug message after reading the common configuration properties.
     */
    String readCommonConfiguration();

    /**
     * perform reading of test configuration
     */
    void readTestConfiguration();

    /**
     * Retrieves the size of the default properties.
     *
     * @return the number of default properties
     */
    int getDefaultsSize();

}
