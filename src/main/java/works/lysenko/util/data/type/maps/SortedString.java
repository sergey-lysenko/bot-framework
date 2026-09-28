package works.lysenko.util.data.type.maps;

import works.lysenko.util.data.comparator.IgnoreCaseString;

import java.util.TreeMap;

/**
 * Sorted Result Map
 */
@SuppressWarnings({"CloneableClassInSecureContext", "ClassExtendsConcreteCollection"})
public class SortedString extends TreeMap<String, String> {

    /**
     * Sorted Result Map
     */
    public SortedString() {

        super((new IgnoreCaseString()));
    }
}
