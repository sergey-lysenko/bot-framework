package works.lysenko.util.data.type.maps;

import works.lysenko.util.data.comparator.IgnoreCaseString;

import java.util.TreeMap;

/**
 * A sorted map associating string keys with string values,
 * using an ignore-case string comparator.
 */
@SuppressWarnings({"CloneableClassInSecureContext", "ClassExtendsConcreteCollection"})
public class SortedString extends TreeMap<String, String> {

    /**
     * Constructs a new SortedString map sorted case-insensitively by string keys.
     */
    public SortedString() {

        super((new IgnoreCaseString()));
    }
}
