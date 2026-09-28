package works.lysenko.util.apis.common;

import works.lysenko.base.exec.DataStorage;

import java.util.Set;

/**
 * The OperatesOnTestData interface provides methods for interacting with test data. It allows users to check the presence
 * of data,
 * retrieve data, modify data, and perform other operations on the test data container.
 */
@SuppressWarnings({"BooleanMethodNameMustStartWithQuestion", "NegativelyNamedBooleanVariable", "BooleanParameter",
        "ClassWithTooManyMethods",
        "InterfaceWithOnlyOneDirectInheritor", "unused"})
public interface OperatesOnTestData {

    /**
     * Shortcut for checking the presence of a data in the common test data
     * container
     *
     * @param field of test data to be verified
     * @return true if data is present
     */
    boolean containsKey(Object field);

    /**
     * Shortcut for checking the presence of a data in the common test data
     * container
     *
     * @param field  of test data to be verified
     * @param silent suppress log messages
     * @return true if data is present
     */
    boolean containsKey(Object field, boolean silent);

    /**
     * Shortcut for checking the presence of a data in the common test data
     * container
     *
     * @param fields of test data to be verified
     * @return true if all data is present
     */
    boolean containsKeys(Object... fields);

    /**
     * Shortcut to {@link DataStorage#filterKeys(Set, Object)}
     *
     * @param keys  to filter
     * @param value to filter keys by
     * @return filtered {@link java.util.Set} of keys in DataStorage storage
     */
    Set<Object> filterKeys(Set<Object> keys, Object value);

    /**
     * Shortcut for getting a data from the common test data container
     *
     * @param field of test data to be retrieved
     * @return copy of test data
     */
    Object get(Object field);

    /**
     * Shortcut for getting a data from the common test data container, with
     * optional default
     *
     * @param field of test data to be retrieved
     * @param def   default value
     * @return copy of test data
     */
    Object get(Object field, Object def);

    /**
     * Shortcut for getting a Boolean data from the common test data container
     *
     * @param field of test data to be retrieved
     * @param def   default value
     * @return copy of test data
     */
    Boolean getBoolean(Object field, Boolean def);

    /**
     * Shortcut for getting a Boolean data from the common test data container
     *
     * @param field of test data to be retrieved
     * @return copy of test data
     */
    Boolean getBoolean(Object field);

    /**
     * Shortcut for getting an int data from the common test data container
     *
     * @param field of test data to be retrieved
     * @param def   value in case of absence of field
     * @return copy of test data
     */
    int getInteger(Object field, int def);

    /**
     * Shortcut for getting an int data from the common test data container
     *
     * @param field of test data to be retrieved
     * @return copy of test data
     */
    int getInteger(Object field);

    /**
     * Shortcut for getting an int data from the common test data container
     *
     * @param field  of test data to be retrieved
     * @param silent no log output if true
     * @return copy of test data
     */
    int getInteger(Object field, boolean silent);

    /**
     * Retrieves a long value associated with the specified field from the
     * common test data container. If the field is not present, the default
     * value provided is returned.
     *
     * @param field the field of test data to be retrieved
     * @param def   the default value to return if the field is absent
     * @return the long value associated with the field, or the default value
     *         if the field is not found
     */
    long getLong(Object field, long def);

    /**
     * Retrieves a long value associated with the specified field from the
     * common test data container.
     *
     * @param field the field of test data to be retrieved
     * @return the long value associated with the field
     */
    long getLong(Object field);

    /**
     * Retrieves a long value associated with the specified field from the
     * common test data container. If the specified field is not present or accessible,
     * the retrieval process may behave differently based on the silent flag.
     *
     * @param field the field of test data to be retrieved
     * @param silent a flag indicating whether to suppress logging or error messages
     *               if the retrieval operation encounters any issues
     * @return the long value associated with the specified field
     */
    long getLong(Object field, boolean silent);

    /**
     * Shortcut for getting a String data from the common test data container.
     *
     * @param field of test data to retrieve
     * @return copy of test data
     */
    String getString(Object field);

    /**
     * Shortcut for getting a String data from the common test data container.
     *
     * @param field  of test data to retrieve
     * @param silent do not produce any log trace
     * @return copy of test data
     */
    String getString(Object field, boolean silent);

    /**
     * Shortcut for getting a String data from the common test data container.
     *
     * @param field of test data to retrieve
     * @param def   default data
     * @return copy of test data
     */
    String getString(Object field, String def);

    /**
     * Gets a String data from the common test data container.
     *
     * @param field  the field of test data to retrieve
     * @param def    the default data
     * @param silent a flag indicating whether to suppress log messages
     * @return the copy of the test data as a String
     */
    String getString(Object field, String def, boolean silent);

    /**
     * Shortcut for putting a data into the common test data container
     *
     * @param field of test data to be updated
     * @param value of this field
     */
    void put(Object field, Object value);

    /**
     * Shortcut for removing a data from the common test data container
     *
     * @param field of test data to be updated
     */
    void remove(Object field);

    /**
     * Removes an object from the execution data and creates a data snapshot.
     * If the nonOptional flag is true, it will verify the presence of the object before removing it.
     * Otherwise, the removal will be performed without checking for presence.
     *
     * @param field       the object to be removed
     * @param nonOptional a flag indicating whether the removal should be performed with verification of presence
     */
    void remove(Object field, boolean nonOptional);

    /**
     * Shortcut for removing a data from the common test data container
     *
     * @param value of test data to be updated
     * @return true if removal was successful
     */
    boolean removeValue(Object value);

    /**
     * Search a key by value
     *
     * @param value to search a key for
     * @return first key in DataStorage storage
     */
    String searchKeyByValue(String value);

    /**
     * Shortcut to {@link DataStorage#searchKeys(String...)}
     *
     * @param substrings one or several sequences of characters to search for
     * @return {@link java.util.Set} of keys in DataStorage storage
     */
    Set<String> searchKeys(String... substrings);

    /**
     * Search a keys Set by value
     *
     * @param value to search a key for
     * @return {@link java.util.Set} of keys in DataStorage storage
     */
    Set<String> searchKeysByValue(String value);

    /**
     * Increases two counters in Test DataStorage by one. (Global and per-user) Name of counter prefixed by underscore
     *
     * @param user name/email
     * @param name of counter to increase by one
     */
    void tick(String user, String name);
}

