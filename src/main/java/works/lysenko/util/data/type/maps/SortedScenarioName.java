package works.lysenko.util.data.type.maps;

import works.lysenko.util.apis.data._Result;
import works.lysenko.util.data.comparator.IgnoreCaseString;

import java.util.TreeMap;

/**
 * A sorted map associating scenario name string keys with {@_Result} values,
 * using an ignore-case string comparator.
 */
@SuppressWarnings({"CloneableClassInSecureContext", "ClassExtendsConcreteCollection"})
public class SortedScenarioName extends TreeMap<String, _Result> {

    /**
     * Constructs a new SortedScenarioName map sorted case-insensitively by string keys.
     */
    public SortedScenarioName() {

        super((new IgnoreCaseString()));
    }
}
