package works.lysenko.util.grid.validation;

import works.lysenko.util.apis.log._Traceable;
import works.lysenko.util.data.records.KeyValue;

import java.util.List;

/**
 * Represents a traceable record that holds a list of key-value pairs.
 * This class implements the _Traceable interface, enabling retrieval
 * of the size of the list, as well as accessing keys and values
 * at specific indices.
 *
 * @param data A list of key-value pairs storing the traceable information.
 */
public record Traceable(List<KeyValue<String, String>> data) implements _Traceable {

    /**
     * Retrieves the key at the specified index.
     *
     * @param i the index of the key-value pair
     * @return the key string at index {@code i}
     */
    @Override
    public String name(final int i) {

        return data.get(i).k();
    }

    /**
     * Returns the number of key-value pairs in the traceable data.
     *
     * @return the size of the data list
     */
    @Override
    public int size() {

        return data.size();
    }

    /**
     * Retrieves the value at the specified index.
     *
     * @param i the index of the key-value pair
     * @return the value string at index {@code i}
     */
    @Override
    public String value(final int i) {

        return data.get(i).v();
    }
}
