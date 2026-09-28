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

    @Override
    public String name(final int i) {

        return data.get(i).k();
    }

    @Override
    public int size() {

        return data.size();
    }

    @Override
    public String value(final int i) {

        return data.get(i).v();
    }
}
