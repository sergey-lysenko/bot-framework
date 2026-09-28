package works.lysenko.util.apis.data;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

/**
 * Interface if Test DataStorage Storage
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _DataStorage {


    /**
     * Stores the data into the specified OutputStream with optional comments.
     *
     * @param out      the OutputStream to store the data to
     * @param comments optional comments to include while storing the data
     * @throws IOException if an I/O error occurs during the operation
     */
    void store(OutputStream out, String comments) throws IOException;

    /**
     * Checks if the specified field exists in the data storage.
     *
     * @param field the field to check
     * @return true if the field exists, false otherwise
     */
    boolean containsKey(Object field);

    /**
     * @return All data in form of Entry Set
     */
    Set<Map.Entry<Object, Object>> entrySet();

    /**
     * @param keys  set of keys
     * @param value to filer keys by
     * @return filtered set of keys
     */
    Set<Object> filterKeys(Set<Object> keys, Object value);

    /**
     * Retrieves the value associated with the specified field from the data storage.
     *
     * @param field the field to retrieve the value for
     * @return the value associated with the field, or null if the field is not found
     */
    Object get(Object field);

    /**
     * Retrieves the value associated with the specified field from the data storage.
     * If the field is not found, returns the specified default value.
     *
     * @param field the field to retrieve the value for
     * @param def   the default value to return if the field is not found
     * @return the value associated with the field, or the default value if the field is not found
     */
    Object getOrDefault(Object field, Object def);

    /**
     * @return set of Keys in DataStorage Storage
     */
    Set<Object> keySet();

    /**
     * @return all keys in DataStorage Storage
     */
    Enumeration<Object> keys();

    /**
     * @param key   to add
     * @param value to assign
     * @return previous value associated with key
     */
    Object put(Object key, Object value);

    /**
     * @param key to remove
     * @return value associated with key before deletion
     */
    Object remove(Object key);

    /**
     * @param value to search a key by
     * @return first key corresponding to provided value
     */
    String searchKeyByValue(String value);

    /**
     * This routine allows to perform a search of keys in Test DataStorage by key substrings
     *
     * @param substrings one or many substrings to search a key(s) by
     * @return all keys matching all provided substrings
     */
    Set<String> searchKeys(String... substrings);

    /**
     * @param value to search all keys by
     * @return all keys corresponding to provided value
     */
    Set<String> searchKeysByValue(String value);

    /**
     * Stores the data to the specified StringWriter object with optional comments.
     *
     * @param stringWriter The StringWriter object to store the data to.
     * @param comments     Optional comments to include with the stored data.
     * @throws IOException if an I/O error occurs while storing the data.
     */
    void store(StringWriter stringWriter, String comments) throws IOException;
}