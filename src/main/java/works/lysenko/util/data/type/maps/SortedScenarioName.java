package works.lysenko.util.data.type.maps;

import works.lysenko.util.apis.data._Result;
import works.lysenko.util.data.comparator.IgnoreCaseString;

import java.util.TreeMap;

/**
 * Sorted Result Map
 */
@SuppressWarnings({"CloneableClassInSecureContext", "ClassExtendsConcreteCollection"})
public class SortedScenarioName extends TreeMap<String, _Result> {

    /**
     * Sorted Result Map
     */
    public SortedScenarioName() {

        super((new IgnoreCaseString()));
    }
}
