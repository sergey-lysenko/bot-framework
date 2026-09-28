package works.lysenko.util.apis.execution;

import works.lysenko.util.apis.data._DataStorage;

/**
 * This interface represents a class that provides access to test data.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "BooleanMethodNameMustStartWithQuestion"})
public interface _TestData {

    /**
     * @param field to verify
     * @return true if field exists
     */
    boolean dataContainsKey(Object field);

    /**
     * Get a value of a field
     *
     * @param field name
     * @return value
     */
    Object dataGet(Object field);

    /**
     * Get a value of a field with defined default
     *
     * @param field name
     * @param def   value
     * @return value or default
     */
    Object dataGetOrDefault(Object field, Object def);

    /**
     * Assign new a value to a field
     *
     * @param field name
     * @param value to assign
     * @return old value
     */
    Object dataPut(Object field, Object value);

    /**
     * Remove data record identified by field name
     *
     * @param field to remove
     * @return true if removal was actually performed
     */
    boolean dataRemove(Object field);

    /**
     * Remove data record identified by value
     *
     * @param value to remove
     * @return true if removal were successful
     */
    boolean dataRemoveValue(Object value);

    /**
     * Retrieves the data storage object.
     *
     * @return the data storage object
     */
    _DataStorage getDataStorage();

    /**
     * @param comments to describe data
     * @return string representation of the data
     */

    String getDataSnapshot(String comments);
}
