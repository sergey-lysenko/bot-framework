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
import works.lysenko.util.spec.PropEnum;

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
import static works.lysenko.util.spec.Layout.Parts.TEST_PROPERTIES_EXTENSION;
import static works.lysenko.util.spec.Layout.Parts._DOT_PROPERTIES;
import static works.lysenko.util.spec.Layout.Paths._RESOURCES_;
import static works.lysenko.util.spec.Layout.Paths._TESTS_;
import static works.lysenko.util.spec.Symbols.*;

/**
 * The TestProperties class provides methods to access and manipulate test properties.
 * It implements the ProvidesTestProperties interface.
 */
@SuppressWarnings({"MethodWithMultipleReturnPoints", "CallToSuspiciousStringMethod", "rawtypes", "WeakerAccess"})
public class TestProperties implements _TestProperties {

    private static final String TAG = s(c(C), _DOT_, GET);
    private final Collection<Class<? extends _PropEnum>> compendium = new ArrayList<>(1);
    private Map commonConfiguration = null;
    private Properties the = null;

    /**
     * Constructs a new instance of TestProperties.
     * During construction, it adds the PropEnum class to the compendium.
     */
    public TestProperties() {

        compendium.add(PropEnum.class);
    }

    /**
     * Constructs a new instance of TestProperties with additional properties.
     * Adds the specified collection of _PropEnum subclasses to the compendium.
     *
     * @param additional A1 collection of classes extending _PropEnum to be added to the compendium.
     */
    public TestProperties(final Collection<Class<? extends _PropEnum>> additional) {

        this();
        compendium.addAll(additional);
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
     * @param o     The object associated with the log message.
     */
    private static void getLog(final Object field, final Object o, final Object def) {

        log(Level.debug, b(sn(TAG, e(ROUND, bb(field)), e(gray(isDefault(o, def), RGT_DAR)), yb(!isDefault(o, def), o))),
                true);
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

    @SuppressWarnings("MethodWithMultipleLoops")
    private Map<String, String> getDefaults() {

        final Map<String, String> def = new HashMap<>(PropEnum.values().length);
        for (final Class<? extends _PropEnum> co : compendium)
            for (final _PropEnum p : co.getEnumConstants())
                add(p, def);
        return def;
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

        final String def = p.defaultValue();
        final String key = p.getPropertyName();
        final String source = isNull(the) ? def : the.getProperty(key, def);
        if (isNotNull(exec)) {
            if (isNotNull(def) && !p.silent() && isDebug()) getLog(key, (null == source) ? null : source.trim(), def);
        }
        return source;
    }

    public final Iterable<? extends Map.Entry<Object, Object>> getPropertiesEntrySet() {

        return the.entrySet();
    }

    @Override
    public final Object getProperty(final String key, final String def) {

        return the.getProperty(key, def);
    }

    public final Map<String, String> getSorted() {

        final Map<String, String> sorted = new SortedString();
        for (final String name : the.stringPropertyNames()) {
            sorted.put(name, the.getProperty(name));
        }
        return sorted;
    }

    public final String getTestPropertySource(final String name, final boolean silent) {

        final String source = the.getProperty(name);
        if (isNotNull(exec)) {
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
    public final void put(final String name, final Object value) {

        final Object o = the.setProperty(s(name), s(value));
        if (isNotNull(exec)) {
            final String info = ((null == o) || (null == value) || !o.toString().equals(value.toString())) ?
                    s(gb(value), RHT_ARR, rc(o)) : yb(o);
            logDebug(b(sn(ansi(PUT, YELLOW), e(CURLY, bb(name)),
                    e(s(RGT_DAR)), e(SQUARE, info))), true);
        }
    }

    @SuppressWarnings("UseOfPropertiesAsHashtable")
    public final String readCommonConfiguration() {

        final works.lysenko.util.func.core.TestProperties.Result result;
        result = readTestPropertiesFromFile(new TestPropertiesDescriptor(_RESOURCES_, COMMON_CONFIGURATION, _DOT_PROPERTIES));
        commonConfiguration = result.properties();
        if (isNull(the)) {
            the = new Properties(); // reset
            the.putAll(commonConfiguration);
        }
        return result.debug();
    }

    @SuppressWarnings("UseOfPropertiesAsHashtable")
    public final void readTestConfiguration() {

        String common = null;
        final works.lysenko.util.func.core.TestProperties.Result result;
        if (isNull(commonConfiguration)) common = readCommonConfiguration();
        the = new Properties(); // reset
        the.putAll(commonConfiguration);
        result = readTestPropertiesFromFile(new TestPropertiesDescriptor(_TESTS_, parameters.getTest(),
                TEST_PROPERTIES_EXTENSION));
        the.putAll(result.properties());
        logTestConfiguration(common, result.debug());
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
