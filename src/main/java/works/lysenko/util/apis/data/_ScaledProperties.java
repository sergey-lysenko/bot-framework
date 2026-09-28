package works.lysenko.util.apis.data;

import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.data.records.RangedMargin;

import java.util.Properties;

/**
 *
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _ScaledProperties {

    /**
     * Retrieves the Properties associated with this object.
     *
     * @return the Properties object
     */
    Properties get();

    /**
     * Retrieves the aspect string representation of the grid by converting it to a string.
     *
     * @return the aspect string representation of the grid
     */
    String getAspectString();

    /**
     * Retrieves a key-value pair based on the specified key. If the key is not found,
     * it returns a key-value pair with the specified default value.
     *
     * @param key the key for which the value is to be retrieved
     * @param def the default value to be returned if the key is not found
     * @return a KeyValue object containing the key and its corresponding value,
     * or a KeyValue object with the default value if the key is not found
     */
    KeyValue<String, String> getKV(String key, String def);

    /**
     * Retrieves a key-value pair based on the specified key. If the value associated with
     * the key is not found and null values are allowed, a key-value pair with a null value
     * may be returned. This method allows controlling whether null values are permissible.
     *
     * @param key           the key for which the value is to be retrieved
     * @param isNullAllowed a flag to specify if null values are allowed
     * @return a KeyValue object containing the key and its corresponding value; may return
     * a KeyValue object with a null value if null is allowed and the key has no value
     */
    KeyValue<String, String> getKV(String key, boolean isNullAllowed);

    /**
     * Retrieves a key-value pair for the specified key. If the key does not exist,
     * the provided default value is returned as the associated value in the key-value pair.
     *
     * @param key the key for which the key-value pair is to be retrieved
     * @param def the default value to be used if the key is not found
     * @return a KeyValue object containing the key and its associated value, or the default value if the key is not found
     */
    KeyValue<String, RangedMargin> getKV(String key, RangedMargin def);

    /**
     * Updates a property with the specified key and value. If the property does not
     * exist and adding is allowed, a new property will be added.
     *
     * @param key      the key of the property to update
     * @param value    the value to set for the specified property
     * @param allowNew specifies whether a new property can be added if the key does not exist
     */
    void updateProperty(String key, String value, boolean allowNew);
}
