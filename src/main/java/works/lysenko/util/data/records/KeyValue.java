package works.lysenko.util.data.records;

import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.spec.Symbols._EQUAL_;

/**
 * Represents a key-value pair object.
 *
 * @param <K> The type of the key in the key-value pair.
 * @param <V> The type of the value in the key-value pair.
 */
@SuppressWarnings({"PublicMethodNotExposedInInterface", "StandardVariableNames"})
public record KeyValue<K, V>(K k, V v) {

    /**
     * Creates a new key-value pair.
     *
     * @param <K>   The type of the key in the key-value pair.
     * @param <V>   The type of the value in the key-value pair.
     * @param key   The key of the pair.
     * @param value The value of the pair.
     * @return A1 new KeyValue object representing the provided key-value pair.
     */
    public static <K, V> KeyValue<K, V> kv(final K key, final V value) {

        return new KeyValue<>(key, value);
    }

    /**
     * Retrieves the name of the method.
     *
     * @return The name of the method.
     */
    public K name() {

        return k;
    }

    @Override
    public String toString() {

        return s(k, _EQUAL_, v);
    }
}
