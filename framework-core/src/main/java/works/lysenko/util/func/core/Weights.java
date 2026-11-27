package works.lysenko.util.func.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.tree.Core;
import works.lysenko.tree.base.Leaf;
import works.lysenko.tree.base.Node;
import works.lysenko.util.apis.scenario._Node;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.prop.tree.Scenario;
import works.lysenko.util.prop.tree.Traverse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
@SuppressWarnings({"MethodWithMultipleReturnPoints", "StaticMethodOnlyUsedInOneClass", "CallToSuspiciousStringMethod", "deprecation",
        "StringToUpperCaseOrToLowerCaseWithoutLocale"})
public record Weights() {

    private static final String UND_SCR_STR = s(UND_SCR);
    private static final String DASH_STR = s(_DASH_);

    private static final ClassValue<Boolean> STANDARD_NODE_IS_EXECUTABLE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(final Class<?> type) {
            try {
                return type.getMethod("isExecutable").getDeclaringClass() == Node.class;
            } catch (final NoSuchMethodException e) {
                return false;
            }
        }
    };

    /**
     * Retrieves the downstream weight of a particular scenario
     *
     * @param scenario The scenario for which to retrieve the downstream weight
     * @return The downstream weight of the scenario
     */
    public static Fraction downstreamWeight(final _Scenario scenario) {

        Fraction weight = Fraction.ZERO;
        if (isNull(properties) || isNull(scenario)) return weight;
        final DownstreamMatcher matcher = new DownstreamMatcher(scenario);
        for (final Map.Entry<Object, Object> entry : properties.getPropertiesEntrySet()) {
            final String key = (String) entry.getKey();
            if (isNull(key) || key.isEmpty() || key.startsWith(".") || key.startsWith(UND_SCR_STR)) continue; // scenario record only
            final String val = (String) entry.getValue();
            if (DASH_STR.equals(val)) continue;
            if (matcher.matches(key.toLowerCase())) {
                weight = weight.add(fr(val));
            }
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
        if (weight.doubleValue() > 0.0) {
            return weight;
        }
        _Scenario ancestor = parent;
        while (Traverse.extensions && isNotNull(ancestor)) {
            if (isExtension(ancestor)) {
                final Fraction ancConf = ancestor.weightConfigured();
                if (isNotNull(ancConf) && ancConf.doubleValue() > 0.0) {
                    weight = weight.add(ancConf);
                }
                final Fraction ancDown = downstreamWeight(ancestor);
                if (isNotNull(ancDown) && ancDown.doubleValue() > 0.0) {
                    weight = weight.add(ancDown);
                }
            }
            ancestor = parentOf(ancestor);
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

        if (isNull(scenario)) return Scenario.defaultWeight;
        String weight = configuredWeightValue(scenario.getShortName());
        if (isNotNull(weight)) return weight;

        weight = configuredWeightValue(scenario.getClass());
        if (isNotNull(weight)) return weight;

        if (Traverse.extensions) {
            Class<?> extension = scenario.getClass().getSuperclass();
            while (isNotNull(extension) && extension != Object.class && extension != Leaf.class && extension != Node.class && extension != Core.class) {
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

        final double total = upstreamWeightDouble(node, new IdentityHashMap<>());
        return (0.0 == total) ? Fraction.ZERO : fr(total);
    }

    private static double upstreamWeightDouble(final _Node node, final Map<_Scenario, Boolean> execCache) {

        double total = 0.0;
        final List<KeyValue<_Scenario, Fraction>> scenariosList = node.getPool().getPairList();
        for (final KeyValue<_Scenario, Fraction> scenario : scenariosList) {
            final _Scenario child = scenario.k();
            if (isExecutableCached(child, execCache)) {
                total += scenario.v().doubleValue(); // weight of a scenario
                if (child instanceof Node childNode) {
                    total += upstreamWeightDouble(childNode, execCache); // weight of ascendants
                } else {
                    total += child.weightUpstream().doubleValue();
                }
            }
        }
        return total;
    }

    private static boolean isExecutableCached(final _Scenario scenario, final Map<_Scenario, Boolean> execCache) {

        if (isNull(scenario)) return false;
        final Boolean cached = execCache.get(scenario);
        if (isNotNull(cached)) return cached;
        final boolean result;
        if (scenario instanceof Node node && Boolean.TRUE.equals(STANDARD_NODE_IS_EXECUTABLE.get(node.getClass()))) {
            if (node.hasFailed() || node.isExhausted() || isNull(node.weightConfigured())) {
                result = false;
            } else {
                final List<KeyValue<_Scenario, Fraction>> pairs = (isNotNull(node.getPool())) ? node.getPool().getPairList() : null;
                if (isNull(pairs) || pairs.isEmpty()) {
                    result = true;
                } else {
                    boolean anyChild = false;
                    for (final KeyValue<_Scenario, Fraction> pair : pairs) {
                        if (isExecutableCached(pair.k(), execCache)) {
                            anyChild = true;
                            break;
                        }
                    }
                    result = anyChild;
                }
            }
        } else {
            result = scenario.isExecutable();
        }
        execCache.put(scenario, result);
        return result;
    }

    /**
     * Retrieves the raw weight value for a given key.
     *
     * @param key The key for which to retrieve the weight value
     * @return The raw weight value corresponding to the key
     */
    private static String configuredWeightValue(final String key) {

        if (isNull(properties) || isNull(key)) return null;
        return (String) properties.getProperty(key, null);
    }

    private static String configuredWeightValue(final Class<?> scenario) {

        if (isNull(scenario)) return null;
        return configuredWeightValue(getScenarioPropertyKey(scenario));
    }

    private static boolean isExtension(final _Scenario scenario) {

        return isNotNull(scenario) && scenario instanceof _Node
                && scenario.getClass().getSuperclass() != Node.class;
    }

    private static _Scenario parentOf(final _Scenario scenario) {

        return (scenario instanceof Core core) ? core.getParentScenario() : null;
    }

    /**
     * Retrieves the scenario property key for a given {@link _Scenario}.
     *
     * @param scenario The scenario for which to retrieve the property key
     * @return The scenario property key
     */
    private static String getScenarioPropertyKey(final _Scenario scenario) {

        return isNotNull(scenario) ? scenario.getShortName() : "";
    }

    private static String getScenarioPropertyKey(final Class<?> scenario) {

        final String root = (null != works.lysenko.Base.tree) ? works.lysenko.Base.tree.getRootPackage() : Scenario.root;
        return StringUtils.removeStart(scenario.getName(), s(root, _DOT_));
    }

    private static boolean startsWithDot(final String haystack, final String prefix) {

        return haystack.length() > prefix.length()
                && haystack.charAt(prefix.length()) == '.'
                && haystack.startsWith(prefix);
    }

    /**
     * Precomputes scenario-side invariants once per {@link #downstreamWeight(_Scenario)} call
     * so checking all property keys avoids repeated lowercasing, string concatenation, and ancestor walks.
     */
    private static final class DownstreamMatcher {

        private final String shortName;
        private final String shortNameDot;
        private final String propKey;
        private final String propKeyDot;
        private final boolean hasDistinctPropKey;
        private final List<String> superKeys;
        private final Set<String> ancestorKeys;

        private DownstreamMatcher(final _Scenario scenario) {

            shortName = scenario.getShortName().toLowerCase();
            shortNameDot = shortName.isEmpty() ? null : s(shortName, ".");
            propKey = getScenarioPropertyKey(scenario).toLowerCase();
            hasDistinctPropKey = !propKey.isEmpty() && !propKey.equals(shortName);
            propKeyDot = hasDistinctPropKey ? s(propKey, ".") : null;

            List<String> supers = null;
            if (Traverse.extensions) {
                Class<?> supercls = scenario.getClass().getSuperclass();
                while (isNotNull(supercls) && supercls != Object.class && supercls != Leaf.class && supercls != Node.class && supercls != Core.class) {
                    final String supKey = getScenarioPropertyKey(supercls).toLowerCase();
                    if (!supKey.isEmpty()) {
                        if (null == supers) supers = new ArrayList<>(2);
                        supers.add(supKey);
                    }
                    supercls = supercls.getSuperclass();
                }
            }
            superKeys = supers;

            Set<String> ancestors = null;
            _Scenario ancestor = parentOf(scenario);
            while (isNotNull(ancestor)) {
                if (null == ancestors) ancestors = new HashSet<>(8);
                addIfNotEmpty(ancestors, getScenarioPropertyKey(ancestor).toLowerCase());
                addIfNotEmpty(ancestors, ancestor.getShortName().toLowerCase());
                addIfNotEmpty(ancestors, ancestor.getSimpleName().toLowerCase());
                addIfNotEmpty(ancestors, ancestor.getName().toLowerCase());
                if (Traverse.extensions) {
                    Class<?> supercls = ancestor.getClass().getSuperclass();
                    while (isNotNull(supercls) && supercls != Object.class && supercls != Leaf.class && supercls != Node.class && supercls != Core.class) {
                        addIfNotEmpty(ancestors, getScenarioPropertyKey(supercls).toLowerCase());
                        supercls = supercls.getSuperclass();
                    }
                }
                ancestor = parentOf(ancestor);
            }
            ancestorKeys = ancestors;
        }

        private static void addIfNotEmpty(final Set<String> target, final String value) {

            if (!value.isEmpty()) target.add(value);
        }

        private boolean matches(final String rec) {

            if (rec.equals(shortName)) return false;
            if (!shortName.isEmpty()) {
                // 1. Candidate is child of configured key (e.g. config="signIn", scenario="signIn.WrongPassword")
                if (startsWithDot(shortName, rec)) return true;
                // 2. Candidate is parent of configured key (e.g. config="signIn.login.partners.addNew.AllEmpty", scenario="signIn.login")
                if (rec.startsWith(shortNameDot)) return true;
            }
            // 3. Check scenario's own property key/alias for candidate itself
            if (hasDistinctPropKey) {
                if (rec.equals(propKey)) return false;
                if (startsWithDot(shortName, propKey) || rec.startsWith(propKeyDot)) return true;
            }
            if (null != superKeys) {
                for (final String supKey : superKeys) {
                    if (rec.equals(supKey)) return false;
                    if (startsWithDot(shortName, supKey) || startsWithDot(rec, supKey)) return true;
                }
            }
            // 4. Ancestor scenario chain match (checking if configRecord configures an ancestor of scenario)
            return null != ancestorKeys && ancestorKeys.contains(rec);
        }
    }
}
