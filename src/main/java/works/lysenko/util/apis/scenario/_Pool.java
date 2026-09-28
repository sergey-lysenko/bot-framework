package works.lysenko.util.apis.scenario;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.util.data.records.KeyValue;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Interface representing a pool of scenarios.
 */
@SuppressWarnings({"InterfaceWithOnlyOneDirectInheritor", "unused"})
public interface _Pool {

    /**
     * Add set of {@link _Scenario} and read their weights
     * from execution configuration file
     *
     * @param ss set of Scenarios to be added
     */
    void add(Iterable<? extends _Scenario> ss);

    /**
     * Adds a set of scenarios to the list of scenarios with their respective weights.
     *
     * @param ss     The set of scenarios to be added
     * @param weight The weight associated with the scenarios
     * @throws NullPointerException if ss is null or if any scenario in ss is null
     */
    void add(Iterable<? extends _Scenario> ss, Fraction weight);

    /**
     * Add single {@link _Scenario} and read its weight from
     * execution configuration file
     *
     * @param scenario Scenario to be added
     */
    void appendScenario(_Scenario scenario);

    /**
     * Add single {@link _Scenario} with defined weight
     *
     * @param scenario Scenario to be added
     * @param weight   weight coefficient for this Scenario
     */
    void appendScenarioWithWeight(_Scenario scenario, Fraction weight);

    /**
     * Add map of {@link _Scenario} and weights defined per
     * Scenario
     *
     * @param map map of Scenarios and their weights
     */
    void appendScenariosMap(Map<? extends _Scenario, Fraction> map);

    /**
     * Add set of {@link _Scenario} with same weight
     *
     * @param scenarios set of Scenarios to be added
     * @param weight    [same] weight of all Scenarios of this set
     */
    void appendScenariosOfWeight(Iterable<? extends _Scenario> scenarios, Fraction weight);

    /**
     * Retrieves the list of scenarios along with their weights.
     *
     * @return The list of scenarios along with their weights
     */
    List<KeyValue<_Scenario, Fraction>> getPairList();

    /**
     * @return Set of nested scenarios
     */
    Set<_Scenario> getSortedSet();
}
