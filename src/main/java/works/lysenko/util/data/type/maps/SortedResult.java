package works.lysenko.util.data.type.maps;

import works.lysenko.util.apis.data._Result;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.comparator.IgnoreCaseScenario;

import java.util.TreeMap;

/**
 * Sorted Result Map
 */
@SuppressWarnings({"CloneableClassInSecureContext", "ClassExtendsConcreteCollection"})
public class SortedResult extends TreeMap<_Scenario, _Result> {

    /**
     * Sorted Result Map
     */
    public SortedResult() {

        super((new IgnoreCaseScenario()));
    }
}
