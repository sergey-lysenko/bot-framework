package works.lysenko.util.apis.log;

import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.grid.validation.Traceable;

import java.util.List;

import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Swap.s;

/**
 * The _Traceable interface represents a contract for objects that allow
 * their collection or structured data to be analyzed or traversed.
 * It includes methods for retrieving the size of the collection,
 * accessing names (keys or identifiers), and retrieving values at specified indices.
 */
@SuppressWarnings("InterfaceWithOnlyOneDirectInheritor")
public interface _Traceable {

    /**
     * Creates a new instance of a _Traceable object using a varargs list of
     * KeyValue pairs. This method simplifies the process of generating
     * _Traceable objects by accepting key-value pairs directly.
     *
     * @param keyValues an array of KeyValue objects where each represents a key-value pair
     *                  to be included in the resulting _Traceable instance
     * @return a new _Traceable instance containing the provided key-value pairs
     */
    @SuppressWarnings("unchecked")
    static _Traceable create(final KeyValue<String, String>... keyValues) {

        return new Traceable(List.of(keyValues));
    }

    /**
     * Creates a key-value pair with the given key and value. The value is
     * converted to a string representation before being included in the pair.
     *
     * @param key   the key of the pair
     * @param value the value of the pair, which will be converted to its string representation
     * @return a new KeyValue object representing the provided key and stringified value
     */
    static KeyValue<String, String> key(final String key, final Object value) {

        return kv(key, s(value));
    }

    /**
     * Retrieves the name associated with the specified index.
     *
     * @param i the index for which the name is to be retrieved
     * @return the name corresponding to the specified index, or null if no name is associated with the index
     */
    String name(int i);

    /**
     * Retrieves the size of the collection or structure.
     *
     * @return the number of elements contained within the collection or structure
     */
    int size();

    /**
     * Retrieves the value associated with the specified index.
     *
     * @param i the index for which the value is to be retrieved
     * @return the value at the specified index, or null if the index is invalid or has no associated value
     */
    Object value(int i);
}
