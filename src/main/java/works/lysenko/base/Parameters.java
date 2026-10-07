package works.lysenko.base;

import works.lysenko.base.parameters.Gui;
import works.lysenko.util.apis.parameters._AdditionalParameters;
import works.lysenko.util.apis.parameters._ExecutionParameterValue;
import works.lysenko.util.apis.parameters._GUI;
import works.lysenko.util.apis.parameters._TestParameters;
import works.lysenko.util.data.enums.ExecutionParameter;
import works.lysenko.util.data.enums.Platform;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.properties;
import static works.lysenko.base.core.Routines.isInsideCI;
import static works.lysenko.base.core.Routines.isInsideDocker;
import static works.lysenko.util.chrs.__.ON;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.____.FILE;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.ExecutionParameter.*;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.data.Regexes.l0;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.S.STORE;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.spec.Layout.Files.PARAMETERS_;
import static works.lysenko.util.spec.Symbols.SEM_CLN;
import static works.lysenko.util.spec.Symbols._DOT_;

/**
 * Represents a class that holds and manages various parameters for execution.
 * Extends Properties and implements ProvidesExecutionParameterValue.
 */
@SuppressWarnings({"CloneableClassInSecureContext", "ClassExtendsConcreteCollection", "MissingJavadoc"})
public final class Parameters extends Properties implements _ExecutionParameterValue, _TestParameters, _AdditionalParameters {

    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    public static final int WIDTH = 18;
    private String[] additionalValues = null;
    private String[] additionalTypes = null;

    /**
     * Initializes the Parameters object with the given list of parameters.
     *
     * @param properties the properties to populate from
     */
    public Parameters(final Properties properties) {

        if (isNotNull(properties)) {
            putAll(properties);
        }
    }

    Parameters(final String list) {

        works.lysenko.Base.parameters = this;
        loadFromFile();
        read(TEST);
        read(POOL);
        read(PLATFORM);
        readDeviceOrDomain();
        read(HEADLESS);
        read(ALL_LEAFS.name()); // empty default: allows fallback to properties / count inference
        read(ALL_LEAFS_COUNT.name());
        read(TESTS.name());
        read(FORBID_OVEREXECUTION.name());
        read(RESILIENT_MODE.name());
        read(COMPLETION_WEIGHT.name());
        read(TRAVERSE_EXTENSIONS.name());
        readAdditionalParameters(list);
        showOptionalGui();
    }

    public String[] getAdditionalTypes() {

        return additionalTypes;
    }

    public String[] getAdditionalValues() {

        return additionalValues;
    }

    public String getTest() {

        return getValue(TEST);
    }

    public void setTest(final String name) {

        put(TEST.name(), name);
        properties.readTestConfiguration();
    }

    public void setPool(final String name) {
        
        put(POOL.name(), name);
    }
    
    public void setPlatform(final String name) {
        
        put(PLATFORM.name(), name);
    }
    
    public void setAllLeafs(final boolean value) {
        
        put(ALL_LEAFS.name(), String.valueOf(value));
    }
    
    public void setAllLeafsCount(final int count) {
        
        put(ALL_LEAFS_COUNT.name(), String.valueOf(count));
    }
    
    public void setHeadless(final boolean value) {
        
        put(HEADLESS.name(), String.valueOf(value));
    }

    public String getValue(final ExecutionParameter parameter) {

        return getPropertyString(parameter.name());
    }

    public String getDomain() {

        return getValue(DOMAIN);
    }

    @Override
    public boolean isAllLeafs() {

        final String countVal = getValue(ALL_LEAFS_COUNT);
        if (isNotNull(countVal) && !countVal.isEmpty()) {
            try {
                if (Integer.parseInt(countVal) > 1) {
                    return true;
                }
            } catch (final NumberFormatException ignored) {
            }
        }
        final String val = getValue(ALL_LEAFS);
        if (isNotNull(val) && !val.isEmpty()) {
            return Boolean.parseBoolean(val);
        }
        if (getAllLeafsCount() > 1) {
            return true;
        }
        return Boolean.TRUE.equals(works.lysenko.util.spec.PropEnum._TEST_ALL_LEAFS.get());
    }

    @Override
    public int getAllLeafsCount() {

        final String val = getValue(ALL_LEAFS_COUNT);
        if (isNotNull(val) && !val.isEmpty()) {
            try {
                return Math.max(1, Integer.parseInt(val));
            } catch (final NumberFormatException ignored) {
            }
        }
        final Integer prop = works.lysenko.util.spec.PropEnum._TEST_ALL_LEAFS_COUNT.get();
        return isNotNull(prop) ? Math.max(1, prop) : 1;
    }

    @Override
    public boolean isHeadless() {

        final String val = getValue(HEADLESS);
        if (isNotNull(val) && !val.isEmpty()) {
            return Boolean.parseBoolean(val);
        }
        return Boolean.TRUE.equals(works.lysenko.util.spec.PropEnum._TEST_HEADLESS.get());
    }

    /**
     * Returns the pool value.
     *
     * @return the pool value
     */
    @Override
    public String getPool() {

        return getValue(POOL);
    }

    public String getTests() {

        return getValue(TESTS);
    }

    public Boolean getForbidOverexecution() {

        final String val = getValue(FORBID_OVEREXECUTION);
        if (isNotNull(val) && !val.isEmpty()) return Boolean.parseBoolean(val);
        return null;
    }

    public Boolean getResilientMode() {

        final String val = getValue(RESILIENT_MODE);
        if (isNotNull(val) && !val.isEmpty()) return Boolean.parseBoolean(val);
        return null;
    }

    public String getCompletionWeight() {

        return getValue(COMPLETION_WEIGHT);
    }

    public Boolean getTraverseExtensions() {

        final String val = getValue(TRAVERSE_EXTENSIONS);
        if (isNotNull(val) && !val.isEmpty()) return Boolean.parseBoolean(val);
        return null;
    }

    /**
     * @param name of a parameter
     * @return {@link String} representation of given parameter's value
     */
    private String getPropertyString(final String name) {

        return (String) get(name);
    }

    /**
     * Loads parameters from a file.
     * If an IOException occurs while loading the file, it will be ignored.
     * File path is defined by the constant PARAMETERS_.
     */
    @SuppressWarnings("OverlyBroadCatchBlock")
    private void loadFromFile() {

        try (final FileInputStream stream = new FileInputStream(PARAMETERS_)) {
            load(stream);
        } catch (final IOException e) {
            // NOP
        }
    }

    /**
     * Reads a parameter value from the system environment or uses a default value if not found.
     */
    private void read(final ExecutionParameter parameter) {

        read(parameter.name(), parameter.def());
    }

    /**
     * Reads a parameter value from the system environment or uses a default value if not found.
     *
     * @param name the name of the parameter
     * @param def  the default value to use if the parameter is not found
     */
    @SuppressWarnings("CallToSystemGetenv")
    private void read(final String name, final String def) {

        final String dotted = name.toLowerCase(java.util.Locale.ROOT).replace('_', '.');
        if (isNotNull(System.getenv(name))) put(name, System.getenv(name)); // prio0: Environment variable
        else if (isNotNull(System.getenv("TEST_" + name))) put(name, System.getenv("TEST_" + name)); // prio0: Environment variable with TEST_ prefix
        else if (isNotNull(System.getenv("TREE_" + name))) put(name, System.getenv("TREE_" + name)); // prio0: Environment variable with TREE_ prefix
        else if (isNotNull(System.getProperty(name))) put(name, System.getProperty(name)); // prio0.5: System property
        else if (isNotNull(System.getProperty("TEST_" + name))) put(name, System.getProperty("TEST_" + name));
        else if (isNotNull(System.getProperty("TREE_" + name))) put(name, System.getProperty("TREE_" + name));
        else if (isNotNull(System.getProperty(name.toLowerCase(java.util.Locale.ROOT)))) put(name, System.getProperty(name.toLowerCase(java.util.Locale.ROOT)));
        else if (isNotNull(System.getProperty(s(_DOT_, dotted)))) put(name, System.getProperty(s(_DOT_, dotted)));
        else if (isNotNull(System.getProperty(dotted))) put(name, System.getProperty(dotted));
        else if (!containsKey(name)) put(name, def); // prio2: Default value
        // NOP: prio1: Value from cache already loaded
    }

    /**
     * Reads a parameter value from the system environment or uses a empty string if not found.
     *
     * @param name the name of the parameter
     */
    private void read(final String name) {

        read(name, EMPTY);
    }

    /**
     * Reads additional parameters from the given list.
     * The list of parameters should be formatted as "params|types".
     * If the given list is null, no parameters are loaded.
     *
     * @param list the list of parameters
     */
    private void readAdditionalParameters(final String list) {

        if (isNotNull(list)) {
            final String[] data = list.split(s(SEM_CLN));
            additionalValues = data[0].split(l0);
            additionalTypes = data[1].split(l0);
            for (final String param : additionalValues) read(param);
        }
    }

    /**
     * Reads the device or domain parameter based on the platform value.
     */
    private void readDeviceOrDomain() {

        if (get(PLATFORM.name()).equals(Platform.ANDROID.getString())) read(DEVICE);
        else read(DOMAIN);
    }

    /**
     * Saves the parameters to a file.
     */
    @SuppressWarnings({"ProhibitedExceptionThrown", "ResultOfMethodCallIgnored", "ThrowInsideCatchBlockWhichIgnoresCaughtException"})
    private void saveToFile() {

        new File(PARAMETERS_).getParentFile().mkdirs(); // Create parent directory
        try (final BufferedWriter writer = new BufferedWriter(new FileWriter(PARAMETERS_, StandardCharsets.UTF_8))) { //
            // Create file writer
            store(writer, EMPTY);
        } catch (final IOException e) {
            throw new RuntimeException(b(c(UNABLE), TO, STORE, PARAMETERS, TO, FILE, q(PARAMETERS_)));
        }
    }

    /**
     * Displays an optional GUI if the execution is not inside a Docker container or CI environment.
     * If the GUI is displayed, it also saves the data to a file.
     */
    private void showOptionalGui() {

        if (!(isInsideDocker() || isInsideCI())) {
            final _GUI gui = new Gui(this);
            gui.display();
            saveToFile();
        }
    }

    /**
     * Returns the description of the test.
     * The description is generated by concatenating the test name and the domain, if it is not blank.
     *
     * @return the description of the test
     */
    public String testDescription() {

        return s(yb(getTest()),
                (getPool().isBlank()) ? EMPTY : s(e(WITH), yb(getPool())),
                (getDomain().isBlank()) ? EMPTY : s(e(ON), yb(getDomain()))
        );
    }
}
