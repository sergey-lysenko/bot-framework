package works.lysenko.base;

import works.lysenko.Base;
import works.lysenko.base.properties.Renderer;
import works.lysenko.base.util.StringParser;
import works.lysenko.util.apis._PropEnum;
import works.lysenko.util.apis.properties._TestProperties;
import works.lysenko.util.data.records.PropertiesMeta;
import works.lysenko.util.data.records.TestPropertiesDescriptor;
import works.lysenko.util.data.type.maps.SortedString;
import works.lysenko.util.func.core.Assertions;
import works.lysenko.util.func.type.Collector;
import works.lysenko.util.spec.Level;
import works.lysenko.util.prop.PropEnum;

import java.util.*;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.*;
import static works.lysenko.util.chrs.__.IS;
import static works.lysenko.util.chrs.__.OF;
import static works.lysenko.util.chrs.___.GET;
import static works.lysenko.util.chrs.___.PUT;
import static works.lysenko.util.chrs.____.TEST;
import static works.lysenko.util.chrs._____.VALUE;
import static works.lysenko.util.data.enums.Ansi.*;
import static works.lysenko.util.data.enums.Brackets.CURLY;
import static works.lysenko.util.data.enums.Brackets.ROUND;
import static works.lysenko.util.data.enums.Brackets.SQUARE;
import static works.lysenko.util.data.enums.Severity.S0;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Swap.sn;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.data.type.ScaledProperties.isDefault;
import static works.lysenko.util.func.core.TestProperties.readTestPropertiesFromFile;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.U.UNABLE_TO_PROCEED;
import static works.lysenko.util.lang.word.C.COMMON;
import static works.lysenko.util.lang.word.C.CONFIGURATION;
import static works.lysenko.util.lang.word.P.PARAMETER;
import static works.lysenko.util.lang.word.P.PROPERTY;
import static works.lysenko.util.lang.word.U.UNDEFINED;
import static works.lysenko.util.spec.Layout.Files.COMMON_CONFIGURATION;
import static works.lysenko.util.spec.Layout.Parts.SUITE_PROPERTIES_EXTENSION;
import static works.lysenko.util.spec.Layout.Parts._DOT_PROPERTIES;
import static works.lysenko.util.spec.Layout.Paths._CONFIG_;
import static works.lysenko.util.spec.Layout.Paths._SUITES_;
import static works.lysenko.util.spec.Symbols.*;

/**
 * The TestProperties class provides methods to access and manipulate test properties.
 * It implements the ProvidesTestProperties interface.
 */
@SuppressWarnings({"MethodWithMultipleReturnPoints", "CallToSuspiciousStringMethod", "rawtypes", "WeakerAccess"})
public class TestProperties implements _TestProperties {

    private static final String TAG = s(c(C), _DOT_, GET);
    private static final ThreadLocal<Boolean> IN_GET_LOG = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private final Collection<Class<? extends _PropEnum>> compendium = new ArrayList<>(1);
    private final Map<String, String> userOverrides = new LinkedHashMap<>();
    private final Map<String, String> configFileDefaults = new LinkedHashMap<>();
    private Map commonConfiguration = null;
    private Properties the = null;
    private boolean readingCommonConfiguration = false;

    /**
     * Constructs a new instance of TestProperties.
     * During construction, it adds the PropEnum class to the compendium.
     */
    public TestProperties() {

        compendium.add(PropEnum.class);
        if (isNotNull(works.lysenko.Base.properties) && works.lysenko.Base.properties != this) {
            userOverrides.putAll(works.lysenko.Base.properties.getUserOverrides());
        }
    }

    /**
     * Constructs a new instance of TestProperties with additional properties.
     * Adds the specified collection of _PropEnum subclasses to the compendium.
     *
     * @param additional A1 collection of classes extending _PropEnum to be added to the compendium.
     */
    public TestProperties(final Collection<Class<? extends _PropEnum>> additional) {

        this();
        if (isNotNull(additional)) {
            for (final Class<? extends _PropEnum> c : additional) {
                if (!compendium.contains(c)) compendium.add(c);
            }
        }
    }

    @Override
    public Collection<Class<? extends _PropEnum>> getCompendium() {

        return Collections.unmodifiableCollection(compendium);
    }

    private static void add(final _PropEnum p, final Map<? super String, ? super String> into) {

        final String key = p.getPropertyName();
        final String value = p.defaultValue();
        into.put(key, value);
    }

    /**
     * Retrieves the log message for a given field and object and logs it at the DEBUG level.
     *
     * @param field The field associated with the log message.
     * @param o     The object associated with the log message.\n     */
    private static void getLog(final Object field, final Object o, final Object def) {

        if (IN_GET_LOG.get()) return;
        IN_GET_LOG.set(Boolean.TRUE);
        try {
            log(Level.debug, b(sn(TAG, e(ROUND, bb(field)), e(gray(isDefault(o, def), RGT_DAR)), yb(!isDefault(o, def), o))),
                    true);
        } finally {
            IN_GET_LOG.set(Boolean.FALSE);
        }
    }

    public final boolean areTestPropertiesReady() {

        return isNotNull(the);
    }

    public final <T> T get(final Class<T> type, final String name) {

        return get(type, name, false);
    }

    public final <T> T get(final Class<T> type, final String name, final boolean silent) {

        final String source = assureTestProperty(type, name, silent);
        if (isNull(source)) return null;
        final StringParser<T> stringParser = StringParser.create(source, type);
        return stringParser.result();
    }

    @Override
    @SuppressWarnings("MethodWithMultipleLoops")
    public final Map<String, String> getDefaults() {

        final Map<String, String> def = new LinkedHashMap<>();
        for (final Class<? extends _PropEnum> co : compendium) {
            final _PropEnum[] constants = co.getEnumConstants();
            if (isNotNull(constants)) {
                for (final _PropEnum p : constants) {
                    add(p, def);
                }
            }
        }
        return def;
    }

    @Override
    public final Map<String, String> getConfigFileDefaults() {

        if (configFileDefaults.isEmpty()) {
            updateConfigFileDefaults(null);
        }
        return new LinkedHashMap<>(configFileDefaults);
    }

    @Override
    public final Map<String, String> getConfigFileDefaults(final String testName) {

        updateConfigFileDefaults(testName);
        return new LinkedHashMap<>(configFileDefaults);
    }

    @Override
    public final Map<String, String> getOriginalConfigProperties() {

        return getConfigFileDefaults(isNotNull(works.lysenko.Base.parameters) ? works.lysenko.Base.parameters.getTest() : null);
    }

    @Override
    public final Map<String, String> getOriginalConfigProperties(final String testName) {

        return getConfigFileDefaults(testName);
    }

    private static works.lysenko.util.func.core.TestProperties.Result readSuiteProperties(final String suiteName) {

        if (isNull(suiteName) || suiteName.isBlank()) {
            return new works.lysenko.util.func.core.TestProperties.Result(new Properties(), "");
        }
        return readTestPropertiesFromFile(new TestPropertiesDescriptor(_SUITES_, suiteName, SUITE_PROPERTIES_EXTENSION));
    }

    private void updateConfigFileDefaults(final String testName) {

        if (isNull(commonConfiguration)) readCommonConfiguration();
        configFileDefaults.clear();
        configFileDefaults.putAll(getDefaults());
        if (isNotNull(commonConfiguration)) {
            for (final Object k : commonConfiguration.keySet()) {
                configFileDefaults.put(k.toString(), commonConfiguration.get(k).toString());
            }
        }
        if (isNotNull(testName) && !testName.isBlank()) {
            try {
                final works.lysenko.util.func.core.TestProperties.Result res = readSuiteProperties(testName);
                if (isNotNull(res) && isNotNull(res.properties())) {
                    for (final String k : res.properties().stringPropertyNames()) {
                        configFileDefaults.put(k, res.properties().getProperty(k));
                    }
                }
            } catch (final RuntimeException ignored) {
            }
        }
    }

    @Override
    public final Map<String, String> getUserOverrides() {

        return new LinkedHashMap<>(userOverrides);
    }

    @Override
    public final void setUserOverride(final String key, final String value) {

        if (null == value) {
            userOverrides.remove(key);
            if (isNotNull(the)) {
                the.remove(key);
            }
        } else {
            userOverrides.put(key, value);
            if (isNotNull(the)) {
                the.setProperty(key, value);
            }
        }
    }

    @Override
    public final void setUserOverrides(final Map<String, String> overrides) {

        userOverrides.clear();
        if (isNotNull(overrides)) {
            for (final Map.Entry<String, String> entry : overrides.entrySet()) {
                final String name = entry.getKey();
                final String val = entry.getValue();
                if (null != val) {
                    userOverrides.put(name, val);
                }
            }
        }
        applyUserOverrides();
    }

    @Override
    public final void clearUserOverrides() {

        userOverrides.clear();
    }

    @Override
    public final <T> T getEnum(final _PropEnum p) {

        final String source = assureEnumTestProperty(p);
        if (isNull(source)) return null;
        final StringParser<T> stringParser = StringParser.create(source, p.type());
        return stringParser.result();
    }

    @Override
    public final String getEnumTestPropertySource(final _PropEnum p) {

        final String key = p.getPropertyName();
        final String def = p.defaultValue();
        String source = userOverrides.get(key);
        if (isNull(source) && isNull(the) && !readingCommonConfiguration) readCommonConfiguration();
        if (isNull(source) && isNotNull(the)) {
            source = the.getProperty(key);
            if (isNull(source) && key.startsWith(".")) {
                source = the.getProperty(key.substring(1));
            }
            if (isNull(source) && !key.startsWith(".")) {
                source = the.getProperty("." + key);
            }
        }
        if (isNull(source)) source = def;
        if (isNotNull(exec) && !IN_GET_LOG.get()) {
            if (isNotNull(def) && !p.silent() && isDebug()) getLog(key, (null == source) ? null : source.trim(), def);
        }
        return source;
    }

    public final Iterable<? extends Map.Entry<Object, Object>> getPropertiesEntrySet() {

        return the.entrySet();
    }

    @Override
    public final Object getProperty(final String key, final String def) {

        if (isNull(the)) readCommonConfiguration();
        if (isNull(the)) return def;
        String val = the.getProperty(key);
        if (isNull(val) && key.startsWith(".")) {
            val = the.getProperty(key.substring(1));
        }
        if (isNull(val) && !key.startsWith(".")) {
            val = the.getProperty("." + key);
        }
        return (null != val) ? val : def;
    }

    public final Map<String, String> getSorted() {

        final Map<String, String> sorted = new SortedString();
        for (final String name : the.stringPropertyNames()) {
            sorted.put(name, the.getProperty(name));
        }
        return sorted;
    }

    public final String getTestPropertySource(final String name, final boolean silent) {

        if (isNull(the)) readCommonConfiguration();
        String source = isNull(the) ? null : the.getProperty(name);
        if (isNull(source) && isNotNull(the) && name.startsWith(".")) {
            source = the.getProperty(name.substring(1));
        }
        if (isNull(source) && isNotNull(the) && !name.startsWith(".")) {
            source = the.getProperty("." + name);
        }
        if (isNotNull(exec) && !IN_GET_LOG.get()) {
            if (isNull(source))
                logEvent(S0, b(c(CONFIGURATION), PARAMETER, q(name), IS, UNDEFINED));
            if (isDebug() && !silent) getLog(name, (null == source) ? null : source.trim(), EMPTY);
        }
        return source;
    }

    public final boolean isCommonValue(final String name, final String value) {

        final String commonValue = (String) commonConfiguration.get(name);
        return value.equals(commonValue);
    }

    @Override
    public final boolean isParameterValue(final String name, final String value) {

        final String configFileVal = configFileDefaults.get(name);
        return isNotNull(configFileVal) && !value.equals(configFileVal);
    }

    @Override
    public final void put(final String name, final Object value) {

        final Object o = the.setProperty(s(name), s(value));
        if (isNotNull(exec)) {
            final String info = ((null == o) || (null == value) || !o.toString().equals(value.toString())) ?
                    s(gb(value), RHT_ARR, rc(o)) : yb(o);
            logDebug(b(sn(ansi(PUT, YELLOW), e(CURLY, bb(name)),
                    e(s(RGT_DAR)), e(SQUARE, info))), true);
        }
    }

    private static void validateCommonConfigurationScope(final Map<?, ?> config) {

        for (final Object k : config.keySet()) {
            final String key = k.toString().trim();
            if (!key.startsWith(".") && !key.equalsIgnoreCase("include")) {
                throw new IllegalStateException(b(
                        "Invalid key", key, "in common.configuration.properties:",
                        "Common configuration must only contain configuration keys starting with '.' (e.g., '.key'). Scenario references and bare properties are disallowed in common configuration."
                ));
            }
        }
    }

    @SuppressWarnings("UseOfPropertiesAsHashtable")
    public final String readCommonConfiguration() {

        if (readingCommonConfiguration) return "";
        if (isNotNull(commonConfiguration) && isNotNull(the) && !the.isEmpty()) return "";
        readingCommonConfiguration = true;
        try {
            if (isNull(the)) {
                the = new Properties();
            }
            final works.lysenko.util.func.core.TestProperties.Result result;
            result = readTestPropertiesFromFile(new TestPropertiesDescriptor(_CONFIG_, COMMON_CONFIGURATION, _DOT_PROPERTIES));
            if (isNotNull(result) && isNotNull(result.properties())) {
                commonConfiguration = result.properties();
                validateCommonConfigurationScope(commonConfiguration);
                the.putAll(commonConfiguration);
            }
            applyUserOverrides();
            updateConfigFileDefaults(null);
            works.lysenko.base.parameters.PropertyValidator.resetCache();
            return (isNotNull(result)) ? result.debug() : "";
        } finally {
            readingCommonConfiguration = false;
        }
    }

    @SuppressWarnings("UseOfPropertiesAsHashtable")
    public final void readTestConfiguration() {

        String common = null;
        final works.lysenko.util.func.core.TestProperties.Result result;
        if (isNull(commonConfiguration)) common = readCommonConfiguration();
        the = new Properties(); // reset
        the.putAll(commonConfiguration);
        final String testName = isNotNull(parameters) ? parameters.getTest() : (isNotNull(works.lysenko.Base.parameters) ? works.lysenko.Base.parameters.getTest() : null);
        result = readSuiteProperties(testName);
        the.putAll(result.properties());
        updateConfigFileDefaults(testName);
        applyUserOverrides();
        if (isNotNull(parameters)) {
            final String headlessVal = parameters.getValue(works.lysenko.util.data.enums.ExecutionParameter.HEADLESS);
            if (isNotNull(headlessVal) && !headlessVal.isEmpty()) {
                the.setProperty(PropEnum._TEST_HEADLESS.getPropertyName(), String.valueOf(Boolean.parseBoolean(headlessVal)));
            }
            final String hideOffVal = parameters.getValue(works.lysenko.util.data.enums.ExecutionParameter.HIDE_OFF_BRANCHES);
            if (isNotNull(hideOffVal) && !hideOffVal.isEmpty()) {
                the.setProperty(PropEnum._TREE_HIDE_OFF_BRANCHES.getPropertyName(), String.valueOf(Boolean.parseBoolean(hideOffVal)));
            }
            final String allLeafsVal = parameters.getValue(works.lysenko.util.data.enums.ExecutionParameter.ALL_LEAFS);
            if (isNotNull(allLeafsVal) && !allLeafsVal.isEmpty()) {
                the.setProperty(PropEnum._TEST_ALL_LEAFS.getPropertyName(), String.valueOf(Boolean.parseBoolean(allLeafsVal)));
            }
            final String allLeafsCountVal = parameters.getValue(works.lysenko.util.data.enums.ExecutionParameter.ALL_LEAFS_EXECUTIONS);
            if (isNotNull(allLeafsCountVal) && !allLeafsCountVal.isEmpty()) {
                the.setProperty(PropEnum._TEST_ALL_LEAFS_EXECUTIONS.getPropertyName(), allLeafsCountVal);
            }
            final String testsVal = parameters.getValue(works.lysenko.util.data.enums.ExecutionParameter.TESTS);
            if (isNotNull(testsVal) && !testsVal.isEmpty()) {
                the.setProperty(PropEnum._TEST_TESTS.getPropertyName(), testsVal);
            }
            final String forbidVal = parameters.getValue(works.lysenko.util.data.enums.ExecutionParameter.FORBID_OVEREXECUTION);
            if (isNotNull(forbidVal) && !forbidVal.isEmpty()) {
                the.setProperty(PropEnum._TREE_FORBID_OVEREXECUTION.getPropertyName(), String.valueOf(Boolean.parseBoolean(forbidVal)));
            }
            final String resilientVal = parameters.getValue(works.lysenko.util.data.enums.ExecutionParameter.RESILIENT_MODE);
            if (isNotNull(resilientVal) && !resilientVal.isEmpty()) {
                the.setProperty(PropEnum._TEST_RESILIENT_MODE.getPropertyName(), String.valueOf(Boolean.parseBoolean(resilientVal)));
            }
            final String weightVal = parameters.getValue(works.lysenko.util.data.enums.ExecutionParameter.COMPLETION_WEIGHT);
            if (isNotNull(weightVal) && !weightVal.isEmpty() && !PropEnum._TREE_COMPLETION_WEIGHT.defaultValue().equals(weightVal)) {
                the.setProperty(PropEnum._TREE_COMPLETION_WEIGHT.getPropertyName(), weightVal);
            }
            final String traverseVal = parameters.getValue(works.lysenko.util.data.enums.ExecutionParameter.TRAVERSE_EXTENSIONS);
            if (isNotNull(traverseVal) && !traverseVal.isEmpty()) {
                the.setProperty(PropEnum._TREE_TRAVERSE_EXTENSIONS.getPropertyName(), String.valueOf(Boolean.parseBoolean(traverseVal)));
            }
        }
        applyUserOverrides();
        refreshAllPropertyRecords();
        logTestConfiguration(common, result.debug());
    }

    @Override
    @SuppressWarnings("UseOfPropertiesAsHashtable")
    public final void prepareTestConfiguration(final String testName, final Boolean isHeadless,
                                               final Boolean isAllLeafs, final Integer allLeafsCount) {

        if (isNull(commonConfiguration)) {
            readCommonConfiguration();
        }
        the = new Properties(); // reset
        if (isNotNull(commonConfiguration)) {
            the.putAll(commonConfiguration);
        }
        if (isNotNull(testName) && !testName.isBlank()) {
            try {
                final works.lysenko.util.func.core.TestProperties.Result res = readSuiteProperties(testName);
                if (isNotNull(res) && isNotNull(res.properties())) {
                    the.putAll(res.properties());
                }
            } catch (final RuntimeException ignored) {
            }
        }
        updateConfigFileDefaults(testName);
        if (Boolean.TRUE.equals(isHeadless)) {
            the.setProperty(PropEnum._TEST_HEADLESS.getPropertyName(), String.valueOf(true));
        } else if (Boolean.FALSE.equals(isHeadless)) {
            the.setProperty(PropEnum._TEST_HEADLESS.getPropertyName(), String.valueOf(false));
        }
        if (Boolean.TRUE.equals(isAllLeafs) || (isNotNull(allLeafsCount) && 1 < allLeafsCount)) {
            the.setProperty(PropEnum._TEST_ALL_LEAFS.getPropertyName(), String.valueOf(true));
        } else if (Boolean.FALSE.equals(isAllLeafs)) {
            the.setProperty(PropEnum._TEST_ALL_LEAFS.getPropertyName(), String.valueOf(false));
        }
        if (isNotNull(allLeafsCount) && 1 < allLeafsCount) {
            the.setProperty(PropEnum._TEST_ALL_LEAFS_EXECUTIONS.getPropertyName(), String.valueOf(allLeafsCount));
        }
        applyUserOverrides();
        refreshAllPropertyRecords();
    }

    public static void refreshAllPropertyRecords() {
        works.lysenko.util.prop.core.Assertions.refresh();
        works.lysenko.util.prop.core.Mode.refresh();
        works.lysenko.util.prop.core.Screenshot.refresh();
        works.lysenko.util.prop.issues.KnownIssues.refresh();
        works.lysenko.util.prop.logs.Debug.refresh();
        works.lysenko.util.prop.tree.HideOff.refresh();
        works.lysenko.util.prop.tree.Include.refresh();
        works.lysenko.util.prop.tree.Notify.refresh();
        works.lysenko.util.prop.tree.Optimization.refresh();
        works.lysenko.util.prop.tree.Retries.refresh();
        works.lysenko.util.prop.tree.Scenario.refresh();
        works.lysenko.util.prop.tree.Traverse.refresh();
    }

    private void applyUserOverrides() {

        if (isNull(the)) return;
        final Map<String, String> defaults = getDefaults();
        for (final Map.Entry<String, String> entry : userOverrides.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            final String def = defaults.get(key);
            if (isNotNull(def) && def.equalsIgnoreCase(value)) {
                the.remove(key);
            } else if (isNotNull(value)) {
                the.setProperty(key, value);
            }
        }
    }

    @Override
    public final Map<String, String> resolveEffectiveProperties(final String testName, final Boolean isHeadless,
                                                                 final Boolean isAllLeafs, final Integer allLeafsCount) {

        return resolveEffectiveProperties(testName, isHeadless, isAllLeafs, allLeafsCount, true);
    }

    @Override
    @SuppressWarnings({"UseOfPropertiesAsHashtable", "MethodWithMultipleLoops"})
    public final Map<String, String> resolveEffectiveProperties(final String testName, final Boolean isHeadless,
                                                                 final Boolean isAllLeafs, final Integer allLeafsCount,
                                                                 final boolean includeUserOverrides) {

        if (isNull(commonConfiguration)) readCommonConfiguration();
        final Map<String, String> effective = new LinkedHashMap<>(getDefaults());
        if (isNotNull(commonConfiguration)) {
            for (final Object k : commonConfiguration.keySet()) {
                effective.put(k.toString(), commonConfiguration.get(k).toString());
            }
        }
        if (isNotNull(testName) && !testName.isBlank()) {
            try {
                final works.lysenko.util.func.core.TestProperties.Result res = readSuiteProperties(testName);
                if (isNotNull(res) && isNotNull(res.properties())) {
                    for (final String k : res.properties().stringPropertyNames()) {
                        effective.put(k, res.properties().getProperty(k));
                    }
                }
            } catch (final RuntimeException ignored) {
            }
        }
        if (Boolean.TRUE.equals(isHeadless)) {
            effective.put(PropEnum._TEST_HEADLESS.getPropertyName(), String.valueOf(true));
        } else if (Boolean.FALSE.equals(isHeadless)) {
            effective.put(PropEnum._TEST_HEADLESS.getPropertyName(), String.valueOf(false));
        }
        if (Boolean.TRUE.equals(isAllLeafs) || (isNotNull(allLeafsCount) && 1 < allLeafsCount)) {
            effective.put(PropEnum._TEST_ALL_LEAFS.getPropertyName(), String.valueOf(true));
        } else if (Boolean.FALSE.equals(isAllLeafs)) {
            effective.put(PropEnum._TEST_ALL_LEAFS.getPropertyName(), String.valueOf(false));
        }
        if (isNotNull(allLeafsCount) && 1 < allLeafsCount) {
            effective.put(PropEnum._TEST_ALL_LEAFS_EXECUTIONS.getPropertyName(), String.valueOf(allLeafsCount));
        }
        if (includeUserOverrides) {
            effective.putAll(userOverrides);
        }
        return effective;
    }

    @Override
    public final int getDefaultsSize() {

        return getDefaults().size();
    }

    private String assureEnumTestProperty(final _PropEnum p) {

        final String value = getEnumTestPropertySource(p);
        if (!p.silent() && isNotNull(p.defaultValue()))
            Assertions.assertNotNullSilent(value, b(UNABLE_TO_PROCEED, p.type().getSimpleName(), VALUE, OF,
                    q(p.getPropertyName()), PROPERTY));
        return value;
    }

    /**
     * Assures that a test property exists and retrieves its value.
     *
     * @param aClass The class associated with the property.
     * @param name   The name of the property.
     * @param silent If true, suppresses log messages and does not throw an exception if the property is not found or value
     *               is null.
     * @return The value of the test property.
     */
    private String assureTestProperty(final Class<?> aClass, final String name, final boolean silent) {

        final String value = getTestPropertySource(name, silent);
        if (!silent)
            Assertions.assertNotNullSilent(value, b(UNABLE_TO_PROCEED, aClass.getSimpleName(), VALUE, OF, q(name), PROPERTY));
        return value;
    }

    /**
     * Retrieves a map of default properties, sorted in a natural order.
     *
     * @return a sorted map containing default properties as key-value pairs
     */
    @SuppressWarnings("MethodMayBeStatic")
    private Map<String, String> getSortedDefaults() {

        return Collector.getSorted(new HashMap<>(Base.properties.getDefaults()));
    }

    /**
     * Logs the test configuration.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    private void logTestConfiguration(final String common, final String debug) {

        if (isNotNull(common)) System.out.println(gray(a(COMMON, common)));
        System.out.println(gray(a(TEST, debug)));
        final Map<String, String> sorted = Base.properties.getSorted();
        final PropertiesMeta meta = new PropertiesMeta(getSortedDefaults(), new ArrayList<>(0));
        Renderer.outputAndValidate(sorted, meta);
    }

}
