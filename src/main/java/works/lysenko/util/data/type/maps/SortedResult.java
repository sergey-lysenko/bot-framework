package works.lysenko.util.data.type.maps;

import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.comparator.IgnoreCaseScenario;

import java.util.TreeMap;

/**
 * A sorted map associating {@_Scenario} keys with {@_Result} values,
 * using an ignore-case scenario comparator.
 */
@SuppressWarnings({"CloneableClassInSecureContext", "ClassExtendsConcreteCollection"})
public class SortedResult extends TreeMap<_Scenario, _Result> {

    /**
     * Constructs a new SortedResult map sorted by scenario name case-insensitively.
     */
    public SortedResult() {

        super((new IgnoreCaseScenario()));
    }
}
