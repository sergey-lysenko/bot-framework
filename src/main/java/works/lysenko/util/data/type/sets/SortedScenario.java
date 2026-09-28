package works.lysenko.util.data.type.sets;

import works.lysenko.tree.Core;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.comparator.IgnoreCaseScenario;

import java.util.TreeSet;

/**
 * @author Sergii Lysenko
 */
@SuppressWarnings({"CloneableClassInSecureContext", "ClassExtendsConcreteCollection"})
public class SortedScenario extends TreeSet<_Scenario> {

    /**
     * {@link java.util.TreeSet} of {@link _Scenario} objects
     * sorted by {@link _Scenario#getName()} ignoring the
     * character case. This is used in various
     * {@link Core} implementations
     */
    public SortedScenario() {

        super(new IgnoreCaseScenario());
    }
}
