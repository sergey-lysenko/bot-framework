package works.lysenko.util.func.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.tree.base.Node;
import works.lysenko.util.apis.scenario._Node;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.prop.tree.Traverse;

import java.util.List;
import java.util.Map;

import static java.lang.Double.POSITIVE_INFINITY;
import static works.lysenko.Base.properties;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static java.util.Objects.isNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;
import static works.lysenko.util.spec.Symbols.UND_SCR;
import static works.lysenko.util.spec.Symbols._DASH_;
import static works.lysenko.util.spec.Symbols._DOT_;

/**
 * The Weights class provides utility methods for calculating and retrieving weights associated with scenarios.
 */
@SuppressWarnings({"MethodWithMultipleReturnPoints", "StaticMethodOnlyUsedInOneClass", "CallToSuspiciousStringMethod",
        "StringToUpperCaseOrToLowerCaseWithoutLocale"})
public record Weights() {

    /**
     * Retrieves the downstream weight of a particular scenario
     *
     * @param scenario The scenario for which to retrieve the downstream weight
     * @return The downstream weight of the scenario
     */
    public static Fraction downstreamWeight(final _Scenario scenario) {

        Fraction weight = Fraction.ZERO;
        if (isNull(properties)) return weight;
        for (final Map.Entry<Object, Object> entry : properties.getPropertiesEntrySet()) {
            final String key = (String) entry.getKey();
            if (!key.startsWith(s(UND_SCR))) // scenario record
                weight = verifyDownstream(scenario, entry, key, weight);
        }
        return weight;
    }

    /**
     * Retrieves downstream weight in the context of the node that owns the candidate.
     * Extension nodes are aliases, so their inherited effective weight also applies to
     * children loaded from the shared node package.
     *
     * @param scenario candidate scenario
     * @param parent   node selecting the candidate
     * @return effective downstream weight
     */
    public static Fraction downstreamWeight(final _Scenario scenario, final _Scenario parent) {

        Fraction weight = downstreamWeight(scenario);
        if (Traverse.extensions && isExtension(parent)) {
            weight = weight.add(parent.weightConfigured());
            weight = weight.add(downstreamWeight(parent));
        }
        return weight;
    }

    /**
     * Calculates the own weight of a given scenario.
     *
     * @param scenario The scenario for which to calculate the own weight
     * @return The own weight of the scenario
     */
    public static Fraction ownWeight(final _Scenario scenario) {

        if (isNotNull(scenario.weightCoded())) return scenario.weightCoded();
        final String rawWeightValue = configuredWeightValue(scenario);
        final Double weightValue = (s(_DASH_).equals(rawWeightValue)) ? Double.NaN : Double.parseDouble(rawWeightValue);
        if (weightValue.equals(POSITIVE_INFINITY)) return fr(Double.MAX_VALUE);
        else return fr(weightValue);
    }

    /**
     * Resolves the configured weight value for a scenario. When extension traversal is enabled,
     * an inherited scenario uses the first explicit weight defined by one of its superclasses.
     *
     * @param scenario scenario whose configured weight is requested
     * @return configured raw value, or the default scenario weight
     */
    public static String configuredWeightValue(final _Scenario scenario) {

        String weight = configuredWeightValue(scenario.getClass());
        if (isNotNull(weight)) return weight;
        if (Traverse.extensions) {
            Class<?> extension = scenario.getClass().getSuperclass();
            while (isNotNull(extension) && extension != Object.class) {
                weight = configuredWeightValue(extension);
                if (isNotNull(weight)) return weight;
                extension = extension.getSuperclass();
            }
        }
        return Scenario.defaultWeight;
    }

    /**
     * Retrieves the upstream weight of Node scenario.
     *
     * @param node The list of scenarios along with their weights
     * @return The upstream weight of the scenario
     */
    public static Fraction upstreamWeight(final _Node node) {

        Fraction weight = fr(0.0);
        final List<KeyValue<_Scenario, Fraction>> scenariosList = node.getPool().getPairList();
        for (final KeyValue<_Scenario, Fraction> scenario : scenariosList)
            // if (!(s.getValue()).isNaN()) {
            if (scenario.k().isExecutable()) {
                weight = fr(weight.doubleValue() + scenario.v().doubleValue()); // weight of a scenario
                weight = fr(weight.doubleValue() + scenario.name().weightUpstream().doubleValue()); // weight of ascendants
            }
        return weight;
    }

    /**
     * Calculates the cumulative weight of an entry based on the downstream weight.
     *
     * @param entry  The key-value pair entry
     * @param weight The downstream weight to be added to the cumulative weight
     * @return The cumulative weight of the entry
     */
    @SuppressWarnings("BooleanVariableAlwaysNegated")
    private static Fraction cumulateDownstream(final Map.Entry<Object, Object> entry, final Fraction weight) {

        final String s = (String) entry.getValue();
        final boolean excluded = s.equals(s(_DASH_));
        if (!excluded) return weight.add(fr(s));
        return weight;
    }

    /**
     * Retrieves the raw weight value for a given key.
     *
     * @param key The key for which to retrieve the weight value
     * @return The raw weight value corresponding to the key
     */
    private static String configuredWeightValue(final Class<?> scenario) {

        if (isNull(properties)) return null;
        return (String) properties.getProperty(getScenarioPropertyKey(scenario), null);
    }

    private static boolean isExtension(final _Scenario scenario) {

        return isNotNull(scenario) && scenario instanceof _Node
                && scenario.getClass().getSuperclass() != Node.class;
    }

    /**
     * Retrieves the scenario property key for a given {@link _Scenario}.
     *
     * @param scenario The scenario for which to retrieve the property key
     * @return The scenario property key
     */
    private static String getScenarioPropertyKey(final _Scenario scenario) {

        return getScenarioPropertyKey(scenario.getClass());
    }

    private static String getScenarioPropertyKey(final Class<?> scenario) {

        return StringUtils.removeStart(scenario.getName(), s(Scenario.root, _DOT_));
    }

    /**
     * Verifies if a given scenario is a parent scenario and not itself, and calculates the cumulative weight of an entry
     * based on the downstream weight.
     *
     * @param scenario The scenario to verify
     * @param entry    The key-value pair entry
     * @param key      The key for which to calculate the weight value
     * @param weight   The downstream weight to be added to the cumulative weight
     * @return The downstream weight if the scenario is a parent and not itself, otherwise returns the downstream weight
     */
    private static Fraction verifyDownstream(final _Scenario scenario, final Map.Entry<Object, Object> entry,
                                             final String key, final Fraction weight) {

        final String shortName = scenario.getShortName().toLowerCase();
        final String configRecord = key.toLowerCase();
        if (shortName.startsWith(configRecord)) // config record belongs to parent of current scenario
            if (!configRecord.equals(shortName)) // not self
                return cumulateDownstream(entry, weight);
        return weight;
    }
}
