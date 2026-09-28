package works.lysenko.util.data.strs;

import works.lysenko.util.data.records.KeyValue;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.util.data.enums.Brackets.CURLY;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.spec.Symbols._EQUAL_;

/**
 * Short-named "alias functions" for some commonly used Strings operations
 */
@SuppressWarnings({"OverloadedMethodsWithSameNumberOfParameters", "InterfaceNeverImplemented"})
public interface Vars {

    /**
     * Create '{key=value}' string
     *
     * @param entry an object containing the key and value
     * @return the formatted string in the format '{key=value}'
     */
    static String a(final KeyValue<?, ?> entry) {

        return s(e(CURLY, render(entry)));
    }

    /**
     * Method to perform operation 'a' with given key and value.
     *
     * @param key   - The key for the operation.
     * @param value - The value for the operation.
     * @return The result of performing operation 'a'.
     */
    static String a(final String key, final int value) {

        return a(kv(key, s(value)));
    }

    /**
     * Method to perform operation 'a' with given key and value.
     *
     * @param key   - The key for the operation.
     * @param value - The value for the operation.
     * @return The result of performing operation 'a'.
     */
    static String a(final String key, final double value) {

        return a(kv(key, s(value)));
    }

    /**
     * Method to perform operation 'a' with given key and value.
     *
     * @param key   - The key for the operation.
     * @param value - The value for the operation.
     * @return The result of performing operation 'a'.
     */
    static String a(final String key, final boolean value) {

        return a(kv(key, s(value)));
    }

    /**
     * Method to perform operation 'a' with given key and value.
     *
     * @param key   - The key for the operation.
     * @param value - The value for the operation.
     * @return The result of performing operation 'a'.
     */
    static String a(final String key, final String value) {

        return a(kv(key, value));
    }

    /**
     * Method to perform operation 'a' with given key and value.
     *
     * @param key   - The key for the operation.
     * @param value - The value for the operation.
     * @return The result of performing operation 'a'.
     */
    static String a(final String key, final float value) {

        return a(kv(key, s(value)));
    }

    /**
     * Performs an operation using the given key, value, and character.
     *
     * @param key   the key parameter that represents a string
     * @param value the value parameter that represents an integer
     * @param after the character parameter that comes after the operation
     * @return the result of the operation as a string
     */
    static String a(final String key, final boolean value, final char after) {

        return a(kv(key, s(value)), s(after));
    }

    /**
     * Performs an operation using the given key, value, and character.
     *
     * @param key   the key parameter that represents a string
     * @param value the value parameter that represents an integer
     * @param after the character parameter that comes after the operation
     * @return the result of the operation as a string
     */
    static String a(final String key, final int value, final char after) {

        return a(kv(key, s(value)), s(after));
    }

    /**
     * Performs an operation using the given key, value, and character.
     *
     * @param key   the key parameter that represents a string
     * @param value the value parameter that represents an integer
     * @param after the character parameter that comes after the operation
     * @return the result of the operation as a string
     */
    static String a(final String key, final double value, final char after) {

        return a(kv(key, s(value)), s(after));
    }

    /**
     * Performs an operation using the given key, value, and character.
     *
     * @param key   the key parameter that represents a string
     * @param value the value parameter that represents an integer
     * @param after the character parameter that comes after the operation
     * @return the result of the operation as a string
     */
    static String a(final String key, final String value, final char after) {

        return a(kv(key, s(value)), s(after));
    }

    /**
     * Performs an operation using the given key, value, and character.
     *
     * @param key   the key parameter that represents a string
     * @param value the value parameter that represents an integer
     * @param after the character parameter that comes after the operation
     * @return the result of the operation as a string
     */
    static String a(final String key, final float value, final char after) {

        return a(kv(key, s(value)), s(after));
    }

    /**
     * Create a formatted string in the format '{key=value}'.
     *
     * @param entry an object containing the key and value
     * @param after the character parameter that comes after the operation
     * @return the formatted string in the format '{key=value}'
     */
    static String a(final KeyValue<?, ?> entry, final char after) {

        return a(entry, s(after));
    }

    /**
     * Create a formatted string in the format '{key=value}'.
     *
     * @param entry an object containing the key and value
     * @param after the character parameter that comes after the operation
     * @return the formatted string in the format '{key=value}'
     */
    static String a(final KeyValue<?, ?> entry, final String after) {

        return s(e(CURLY, render(entry)), after);
    }

    /**
     * Concatenates the key and value of each KeyValue object in the given list,
     * using the specified character as the delimiter between them.
     *
     * @param entries the list of KeyValue objects
     * @param between the character to be used as the delimiter
     * @return a string containing the concatenated key-value pairs, separated by the specified character
     */
    static String a(final List<KeyValue<?, ?>> entries, final char between) {

        return a(entries, s(between));
    }

    /**
     * Concatenates the key and value of each KeyValue object in the given list,
     * using the specified character as the delimiter between them.
     *
     * @param entries the list of KeyValue objects
     * @param between the string to be used as the delimiter
     * @return a string containing the concatenated key-value pairs, separated by the specified character
     */
    static String a(final List<KeyValue<?, ?>> entries, final String between) {

        final StringBuilder parts = new StringBuilder(0);
        for (final KeyValue<?, ?> entry : entries) {
            final boolean last = ((entries.indexOf(entry)) == (entries.size() - 1));
            parts.append(last ? a(entry) : a(entry, between));
        }
        return parts.toString();
    }

    private static String render(final KeyValue<?, ?> entry) {

        return (null == entry.v()) ? EMPTY : s(entry.k(), _EQUAL_, entry.v());
    }
}
