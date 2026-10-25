package works.lysenko.base.parameters;

import org.apache.commons.math3.fraction.Fraction;
import works.lysenko.base.output.ProgressionSettings;
import works.lysenko.base.ui.ConfigTestsEditorPanel;
import works.lysenko.util.apis._PropEnum;
import works.lysenko.util.func.imgs.OperationMarkers;
import works.lysenko.util.prop.PropEnum;

import java.awt.Color;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import org.reflections.Reflections;

import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.func.type.fractions.Factory.fr;

/**
 * Validates property keys and values entered in UI components or read from configuration files
 * against the declared properties in {@link PropEnum} and any project-scoped {@link _PropEnum} implementations.
 */
public final class PropertyValidator {

    /**
     * Per-property constraints on string values (compared case-insensitively).
     */
    private static final Map<_PropEnum, List<String>> ALLOWED_VALUES = Map.of(
            PropEnum._TEST_REPORT_PROGRESSION_TREE_SONIFICATION, ProgressionSettings.treeSonificationValues(),
            PropEnum._SCREENSHOTS_MARKER_VARIANT, OperationMarkers.availableModes());

    private static List<String> cachedProperties = null;
    private static Set<Class<? extends _PropEnum>> cachedPropEnumClasses = null;
    private static List<_PropEnum> cachedPropEnumConstants = null;

    private PropertyValidator() {
    }

    /**
     * Resets internal property caches so new or dynamically loaded PropEnums or scenarios are re-discovered.
     */
    public static synchronized void resetCache() {
        cachedProperties = null;
        cachedPropEnumClasses = null;
        cachedPropEnumConstants = null;
    }

    /**
     * Finds and returns all known classes extending or implementing {@link _PropEnum}.
     *
     * @return set of all known _PropEnum classes
     */
    @SuppressWarnings("unchecked")
    public static synchronized Set<Class<? extends _PropEnum>> getAllPropEnumClasses() {
        if (null == cachedPropEnumClasses) {
            final Set<Class<? extends _PropEnum>> classes = new LinkedHashSet<>();
            classes.add(PropEnum.class);
            classes.add(works.lysenko.shared.util.spec.PropEnum.class);

            if (isNotNull(works.lysenko.Base.properties)) {
                final Collection<Class<? extends _PropEnum>> comp = works.lysenko.Base.properties.getCompendium();
                if (isNotNull(comp)) {
                    classes.addAll(comp);
                }
            }

            try {
                final Set<String> packagesToScan = new LinkedHashSet<>();
                packagesToScan.add("works.lysenko");

                final String rootPackage = PropEnum._TEST_ROOT.get();
                if (isNotNull(rootPackage) && !rootPackage.isBlank() && !rootPackage.equals(".")) {
                    packagesToScan.add(rootPackage);
                    String p = rootPackage;
                    while (p.contains(".")) {
                        p = p.substring(0, p.lastIndexOf('.'));
                        if (!p.isBlank()) {
                            packagesToScan.add(p);
                        }
                    }
                }

                final org.reflections.util.ConfigurationBuilder builder = new org.reflections.util.ConfigurationBuilder();
                final Set<java.net.URL> urls = new LinkedHashSet<>();
                for (final String pkg : packagesToScan) {
                    try {
                        final Class<?> cls = Class.forName(pkg + ".util.spec.PropEnum");
                        if (_PropEnum.class.isAssignableFrom(cls)) {
                            classes.add((Class<? extends _PropEnum>) cls);
                        }
                    } catch (final Throwable ignored) {
                    }
                    try {
                        final Class<?> cls = Class.forName(pkg + ".shared.util.spec.PropEnum");
                        if (_PropEnum.class.isAssignableFrom(cls)) {
                            classes.add((Class<? extends _PropEnum>) cls);
                        }
                    } catch (final Throwable ignored) {
                    }
                    try {
                        urls.addAll(org.reflections.util.ClasspathHelper.forPackage(pkg));
                    } catch (final Exception ignored) {
                    }
                }
                try {
                    urls.addAll(org.reflections.util.ClasspathHelper.forClassLoader());
                } catch (final Exception ignored) {
                }
                try {
                    urls.addAll(org.reflections.util.ClasspathHelper.forJavaClassPath());
                } catch (final Exception ignored) {
                }
                try {
                    final java.io.File classesDir = new java.io.File(System.getProperty("user.dir"), "target/classes");
                    if (classesDir.exists()) {
                        urls.add(classesDir.toURI().toURL());
                    }
                } catch (final Exception ignored) {
                }

                builder.setUrls(urls);
                final Reflections reflections = new Reflections(builder);
                final Set<Class<? extends _PropEnum>> subTypes = reflections.getSubTypesOf(_PropEnum.class);
                if (isNotNull(subTypes)) {
                    classes.addAll(subTypes);
                }
            } catch (final Exception ignored) {
            }

            cachedPropEnumClasses = Collections.unmodifiableSet(classes);
        }
        return cachedPropEnumClasses;
    }

    /**
     * Returns all known enum constants across all registered and discovered {@link _PropEnum} classes.
     *
     * @return list of all known _PropEnum instances
     */
    public static synchronized List<_PropEnum> getAllPropEnumConstants() {
        if (null == cachedPropEnumConstants) {
            final Set<Class<? extends _PropEnum>> classes = getAllPropEnumClasses();
            final List<_PropEnum> constants = new ArrayList<>();
            for (final Class<? extends _PropEnum> c : classes) {
                if (c.isEnum()) {
                    final _PropEnum[] enums = (_PropEnum[]) c.getEnumConstants();
                    if (isNotNull(enums)) {
                        constants.addAll(Arrays.asList(enums));
                    }
                }
            }
            cachedPropEnumConstants = Collections.unmodifiableList(constants);
        }
        return cachedPropEnumConstants;
    }

    /**
     * Checks if a property key starting with '.' is a known PropEnum property.
     *
     * @param propertyName property key, e.g. {@code .test.tests}
     * @return true if known or .include, false otherwise
     */
    public static boolean isKnownConfigProperty(final String propertyName) {
        if (null == propertyName) return false;
        final String trimmed = propertyName.trim();
        if (trimmed.equalsIgnoreCase(".include") || trimmed.equalsIgnoreCase("include")) return true;
        for (final _PropEnum p : getAllPropEnumConstants()) {
            if (p.getPropertyName().equalsIgnoreCase(trimmed)) return true;
        }
        return false;
    }

    /**
     * Finds the closest known PropEnum property name for a misspelled or invalid property key.
     *
     * @param propertyName property key
     * @return closest known property name, or null if no close match found
     */
    public static String findClosestProperty(final String propertyName) {
        if (null == propertyName || propertyName.isEmpty()) return null;
        String bestMatch = null;
        int minDistance = Integer.MAX_VALUE;
        final String lower = propertyName.toLowerCase(Locale.ROOT);
        for (final _PropEnum p : getAllPropEnumConstants()) {
            final String propName = p.getPropertyName();
            final int dist = org.apache.commons.lang3.StringUtils.getLevenshteinDistance(lower, propName.toLowerCase(Locale.ROOT));
            if (dist < minDistance && dist <= 6) {
                minDistance = dist;
                bestMatch = propName;
            }
        }
        return bestMatch;
    }

    /**
     * Validates a property key for a given file category or context.
     *
     * @param key      property key
     * @param category file category (can be null)
     * @return human-readable error if key is invalid, or null if key is valid
     */
    public static String validateKey(final String key, final ConfigTestsEditorPanel.FileCategory category) {
        if (null == key || key.isBlank()) return "Property key cannot be empty";
        final String trimmed = key.trim();
        if (trimmed.startsWith(".")) {
            if (!isKnownConfigProperty(trimmed)) {
                final String suggestion = findClosestProperty(trimmed);
                if (isNotNull(suggestion)) {
                    return "Unknown configuration key '" + trimmed + "'. Did you mean '" + suggestion + "'?";
                }
                return "Unknown configuration key '" + trimmed + "'";
            }
        } else if (category == ConfigTestsEditorPanel.FileCategory.GLOBAL_CONFIG || category == ConfigTestsEditorPanel.FileCategory.CONFIG_PART) {
            return "Scenario key '" + trimmed + "' is not allowed in configuration files (keys must start with '.')";
        }
        return null;
    }

    /**
     * Validates both key and value for a property in a given file category.
     *
     * @param key      property key
     * @param value    property value
     * @param category file category (can be null)
     * @return human-readable error if invalid, or null if valid
     */
    public static String validateProperty(final String key, final String value, final ConfigTestsEditorPanel.FileCategory category) {
        final String keyErr = validateKey(key, category);
        if (isNotNull(keyErr)) return keyErr;
        return validate(key, value);
    }

    /**
     * Validates a value for a property.
     *
     * @param propertyName property key, e.g. {@code .progression.tree.sonification}
     * @param value        proposed value
     * @return human-readable error, or {@code null} if the value is valid
     */
    public static String validate(final String propertyName, final String value) {

        if (null == value || value.isEmpty()) return null;
        final _PropEnum property = find(propertyName);
        if (null == property) return null;

        final Class<?> type = property.type();
        final boolean isText = String.class == type || Character.class == type;
        if (!isText && !value.equals(value.strip())) {
            return "Value must not start or end with whitespace";
        }
        try {
            if (Boolean.class == type) {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    return "Expected a boolean: true or false";
                }
            } else if (Integer.class == type) {
                works.lysenko.base.util.StringParser.parseSuffixedInt(value);
            } else if (Long.class == type) {
                works.lysenko.base.util.StringParser.parseSuffixedLong(value);
            } else if (Double.class == type) {
                finite(Double.parseDouble(value));
            } else if (Float.class == type) {
                finite(Float.parseFloat(value));
            } else if (Color.class == type) {
                return validateColor(value, delimiter());
            } else if (Fraction.class == type) {
                if (null == fr(value, true)) return "Expected a fraction (e.g. 1/2) or a decimal number";
            } else if (works.lysenko.util.data.records.RelativeOrAbsoluteFraction.class == type) {
                return validateRelativeOrAbsolute(value);
            } else if (Character.class == type && 1 != value.length()) {
                return "Expected a single character";
            } else if (type.isEnum()) {
                for (final Object enumValue : type.getEnumConstants()) {
                    if (((Enum<?>) enumValue).name().equalsIgnoreCase(value)) return null;
                }
                return "Expected one of: " + String.join(", ", validValues(propertyName));
            }
        } catch (final NumberFormatException e) {
            return "Expected " + describe(type) + ": '" + value + "' is not valid";
        } catch (final RuntimeException e) {
            return "Value is not valid for " + describe(type);
        }
        final List<String> allowed = ALLOWED_VALUES.get(property);
        if (null != allowed && allowed.stream().noneMatch(a -> a.equalsIgnoreCase(value.strip()))) {
            return "Allowed values: " + String.join(", ", allowed);
        }
        return null;
    }

    /**
     * Returns the known valid values for a property, or an empty list when it has no finite value set.
     *
     * @param propertyName property key, e.g. {@code .progression.tree.sonification}
     * @return known valid values
     */
    public static List<String> validValues(final String propertyName) {

        final _PropEnum property = find(propertyName);
        if (null == property) return List.of();
        if (Boolean.class == property.type()) return List.of("true", "false");
        if (property.type().isEnum()) {
            return java.util.Arrays.stream(property.type().getEnumConstants())
                    .map(Enum.class::cast)
                    .map(Enum::name)
                    .toList();
        }
        return ALLOWED_VALUES.getOrDefault(property, List.of());
    }

    public static synchronized List<String> getAllKnownProperties() {
        if (cachedProperties == null) {
            List<String> tempProperties = new ArrayList<>();
            for (final _PropEnum p : getAllPropEnumConstants()) {
                tempProperties.add(p.getPropertyName());
            }

            String rootPackage = PropEnum._TEST_ROOT.get();
            if (rootPackage == null || rootPackage.isEmpty() || rootPackage.equals(".")) {
                rootPackage = "works.lysenko";
            }

            final String prefixToRemove = rootPackage + ".";
            final Set<String> scenarioNames = new HashSet<>();

            try {
                final works.lysenko.tree.Tree tree = new works.lysenko.tree.Tree(rootPackage);
                if (isNotNull(tree.getBranches())) {
                    for (final var branch : tree.getBranches()) {
                        collectVirtualScenarioNames(branch, scenarioNames);
                    }
                }
            } catch (final Exception ignored) {
            }

            try {
                final Reflections reflections = new Reflections(rootPackage);

                final java.util.function.Consumer<Class<?>> addStrippedName = c -> {
                    final String name = c.getName();
                    if (name.startsWith(prefixToRemove)) {
                        scenarioNames.add(name.substring(prefixToRemove.length()));
                    }
                };

                reflections.getSubTypesOf(works.lysenko.tree.base.Node.class).forEach(addStrippedName);
                reflections.getSubTypesOf(works.lysenko.tree.base.Leaf.class).forEach(addStrippedName);
                reflections.getSubTypesOf(works.lysenko.tree.base.Mono.class).forEach(addStrippedName);
            } catch (final Exception ignored) {
            }

            tempProperties.addAll(scenarioNames);
            cachedProperties = tempProperties.stream().distinct().sorted().collect(Collectors.toList());
        }
        return cachedProperties;
    }

    private static void collectVirtualScenarioNames(final works.lysenko.util.apis.scenario._Scenario scenario, final Set<String> names) {
        if (null == scenario) return;
        names.add(scenario.getShortName());
        if (scenario instanceof works.lysenko.util.apis.scenario._Node node) {
            if (isNotNull(node.getPool()) && isNotNull(node.getPool().getPairList())) {
                for (final var kv : node.getPool().getPairList()) {
                    if (isNotNull(kv) && isNotNull(kv.k())) {
                        collectVirtualScenarioNames(kv.k(), names);
                    }
                }
            }
        }
    }

    private static _PropEnum find(final String propertyName) {

        if (null == propertyName) return null;
        for (final _PropEnum property : getAllPropEnumConstants()) {
            if (property.getPropertyName().equalsIgnoreCase(propertyName)) return property;
        }
        return null;
    }

    /**
     * Determines the scope of a property ("Framework" vs "Project").
     *
     * @param propertyName property key, e.g. {@code .test.tests}
     * @return "Framework" if declared in a framework _PropEnum, "Project" otherwise
     */
    public static String determinePropertyScope(final String propertyName) {

        final _PropEnum prop = find(propertyName);
        if (isNotNull(prop)) {
            final String className = prop.getClass().getName();
            if (className.startsWith("works.lysenko.")) {
                return "Framework";
            } else {
                return "Project";
            }
        }
        return "Project";
    }

    private static void finite(final double d) {

        if (Double.isNaN(d) || Double.isInfinite(d)) throw new NumberFormatException("not finite");
    }

    private static String delimiter() {

        final String l1 = PropEnum._DATA_DELIMITER_L1.get();
        return (null == l1 || l1.isEmpty()) ? "," : l1.substring(0, 1);
    }

    private static String validateColor(final String value, final String delimiter) {

        final String[] parts = value.split(java.util.regex.Pattern.quote(delimiter), -1);
        if (4 != parts.length) {
            return "Expected a colour as R" + delimiter + "G" + delimiter + "B" + delimiter + "A (4 integers 0-255)";
        }
        for (final String part : parts) {
            try {
                final int component = Integer.parseInt(part.strip());
                if (component < 0 || component > 255) return "Colour components must be in the range 0-255";
            } catch (final NumberFormatException e) {
                return "Colour component '" + part + "' is not an integer";
            }
        }
        return null;
    }

    private static String validateRelativeOrAbsolute(final String value) {

        final char marker = value.charAt(0);
        if ('0' == marker) return (1 == value.length()) ? null : "'0' must stand alone";
        if ('a' != marker && 'r' != marker) {
            return "Expected 'a' (absolute) or 'r' (relative) followed by a fraction, e.g. r1/2, or '0'";
        }
        return (null == fr(value.substring(1), true)) ? "Invalid fraction after '" + marker + "'" : null;
    }

    private static String describe(final Class<?> type) {

        return type.getSimpleName().toLowerCase(Locale.ROOT);
    }
}
