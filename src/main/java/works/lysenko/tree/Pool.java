package works.lysenko.tree;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.apis.scenario._Pool;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.data.type.sets.SortedScenario;
import works.lysenko.util.func.core.Weights;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Null.n;
import static works.lysenko.util.func.type.Objects.isNotNull;

/**
 * Represents a pool of scenarios that can be added with weights.
 */
@SuppressWarnings({"NestedMethodCall", "StandardVariableNames", "AssignmentOrReturnOfFieldWithMutableType",
        "ChainedMethodCall", "ForeachStatement", "ObjectAllocationInLoop"})
public class Pool implements _Pool {

    private final List<KeyValue<_Scenario, Fraction>> pairList;

    Pool() {

        pairList = new LinkedList<>();
    }

    /**
     * Adds a set of scenarios to the list of scenarios with their respective weights.
     *
     * @param ss The set of scenarios to be added
     */
    public final void add(final Iterable<? extends _Scenario> ss) {

        add(ss, null);
    }

    /**
     * Adds a set of scenarios to the list of scenarios with their respective weights.
     *
     * @param ss     The set of scenarios to be added
     * @param weight The weight associated with the scenarios
     */
    @SuppressWarnings("ObjectAllocationInLoop")
    public final void add(final Iterable<? extends _Scenario> ss, final Fraction weight) {

        if (isNotNull(ss)) {
            for (final _Scenario scenario : ss)
                if (isNotNull(scenario))
                    pairList.add(kv(scenario, n(scenario.weightConfigured(), weight)));
        }
    }

    /**
     * Appends a scenario with defined weight to the list of scenarios.
     *
     * @param scenario The scenario to be appended.
     */
    @Override
    public final void appendScenario(final _Scenario scenario) {

        appendScenarioWithWeight(scenario, Weights.ownWeight(scenario));
    }

    /**
     * Appends a scenario with defined weight to the list of scenarios.
     *
     * @param scenario The scenario to be appended.
     * @param weight   The weight of the scenario.
     */
    public final void appendScenarioWithWeight(final _Scenario scenario, final Fraction weight) {

        pairList.add(kv(scenario, weight));
    }

    /**
     * Appends the scenarios and their weights to the existing map.
     *
     * @param map The map containing the scenarios and their weights to be added
     */
    @SuppressWarnings("BoundedWildcard")
    public final void appendScenariosMap(final Map<? extends _Scenario, Fraction> map) {

        map.forEach((k, v) -> pairList.add(kv(k, v)));
    }

    /**
     * Appends scenarios to the list of scenarios with a same given weight.
     *
     * @param scenarios The iterable of ProvidesScenario objects to be appended
     * @param weight    The common/same weight of the scenarios
     */
    public final void appendScenariosOfWeight(final Iterable<? extends _Scenario> scenarios, final Fraction weight) {

        scenarios.forEach(scenario -> pairList.add(kv(scenario, weight)));
    }

    public final List<KeyValue<_Scenario, Fraction>> getPairList() {

        return pairList;
    }

    public final Set<_Scenario> getSortedSet() {

        return pairList.stream().flatMap(scenario -> scenario.name().list().stream()).collect(Collectors.toCollection(SortedScenario::new));
    }
}
