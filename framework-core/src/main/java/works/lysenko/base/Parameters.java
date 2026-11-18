package works.lysenko.base;

import works.lysenko.base.parameters.Renderer;
import works.lysenko.util.apis._PropEnum;
import works.lysenko.util.apis.parameters._AdditionalParameters;
import works.lysenko.util.apis.parameters._ExecutionParameterValue;
import works.lysenko.util.apis.parameters._GUI;
import works.lysenko.util.apis.parameters._TestParameters;
import works.lysenko.util.data.enums.ExecutionParameter;
import works.lysenko.util.data.enums.Platform;
import works.lysenko.util.data.records.KeyValue;
import works.lysenko.util.data.records.ParametersMeta;
import works.lysenko.util.data.type.maps.SortedString;
import works.lysenko.util.prop.*;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static works.lysenko.Base.properties;
import static works.lysenko.base.core.Routines.isInsideCI;
import static works.lysenko.base.core.Routines.isInsideDocker;
import static works.lysenko.util.chrs.__.ON;
import static works.lysenko.util.chrs.__.TO;
import static works.lysenko.util.chrs.____.FILE;
import static works.lysenko.util.chrs.____.FROM;
import static works.lysenko.util.chrs.____.MODE;
import static works.lysenko.util.chrs.____.WITH;
import static works.lysenko.util.data.enums.Ansi.gray;
import static works.lysenko.util.data.enums.Ansi.yb;
import static works.lysenko.util.data.enums.ExecutionParameter.*;
import static works.lysenko.util.data.records.KeyValue.kv;
import static works.lysenko.util.data.strs.Bind.b;
import static works.lysenko.util.data.strs.Case.c;
import static works.lysenko.util.data.strs.Swap.s;
import static works.lysenko.util.data.strs.Vars.a;
import static works.lysenko.util.data.strs.Wrap.e;
import static works.lysenko.util.data.strs.Wrap.q;
import static works.lysenko.util.func.data.Regexes.l0;
import static works.lysenko.util.func.type.Objects.isNotNull;
import static works.lysenko.util.lang.C.COMMA_SPACE;
import static works.lysenko.util.lang.word.A.ADDITIONAL;
import static works.lysenko.util.lang.word.N.NONE;
import static works.lysenko.util.lang.word.P.PARAMETERS;
import static works.lysenko.util.lang.word.S.STORE;
import static works.lysenko.util.lang.word.U.UNABLE;
import static works.lysenko.util.spec.Layout.Files.PARAMETERS_;
import static works.lysenko.util.spec.Symbols.SEM_CLN;

/**
 * Represents a class that holds and manages various parameters for execution.
 * Extends Properties and implements ProvidesExecutionParameterValue.
 */
@SuppressWarnings({"CloneableClassInSecureContext", "ClassExtendsConcreteCollection", "MissingJavadoc"})
public final class Parameters extends Properties implements _ExecutionParameterValue, _TestParameters, _AdditionalParameters {

    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    /** The width. */
    public static final int WIDTH = 18;
    private static final String MASK = "********";
    private final Map<String, String> fileValues = new LinkedHashMap<>();
    private final Map<String, String> envValues = new LinkedHashMap<>();
    private String[] additionalValues = null;
    private String[] additionalTypes = null;
    private boolean fileExisted = false;
    private boolean guiDisplayed = false;

    /**
     * Initializes the Parameters object with the given list of parameters.
     *
     * @param properties the properties to populate from
     */
    public Parameters(final Properties properties) {

        if (isNotNull(properties)) {
            putAll(properties);
            cleanupLegacyTestKey();
        }
    }

    Parameters(final String list) {

        works.lysenko.Base.parameters = this;
        loadFromFile();
        read(SUITE);
        read(OLD_SUITE);
        read(POOL);
        read(PLATFORM);
        readDeviceOrDomain();
        read(HEADLESS);
        read(HIDE_OFF_BRANCHES);
        read(ALL_LEAFS.name()); // empty default: allows fallback to properties / count inference
        read(ALL_LEAFS_EXECUTIONS.name());
        read(ALL_LEAFS_PATIENCE.name());
        read(TESTS.name());
        read(FORBID_OVEREXECUTION.name());
        read(RESILIENT_MODE.name());
        read(COMPLETION_WEIGHT.name());
        read(TRAVERSE_EXTENSIONS.name());
        read(ORIGIN.name());
        read(TAG.name());
        readAdditionalParameters(list);
        cleanupLegacyTestKey();
        checkSuiteChangeAndBackdraft();
        showOptionalGui();
        logParametersConfiguration();
    }

    private void checkSuiteChangeAndBackdraft() {

        final String currentSuite = getSuite();
        final String oldSuite = getValue(OLD_SUITE);
        if (isNotNull(currentSuite) && !currentSuite.isBlank() &&
                (isNull(oldSuite) || oldSuite.isBlank() || !currentSuite.equalsIgnoreCase(oldSuite))) {
            applyBackdraft(currentSuite);
        }
    }

    /**
     * Applies "back draft" when a test suite is defined or changed.
     * Configuration properties defined in the new suite overwrite stale cached execution parameters.
     *
     * @param suiteName the name of the new test suite
     */
    public void applyBackdraft(final String suiteName) {

        if (isNull(suiteName) || suiteName.isBlank()) return;

        put(SUITE.name(), suiteName);
        put(OLD_SUITE.name(), suiteName);

        final Boolean updateParams = PropEnum._TEST_SUITE_CHANGE_UPDATES_PARAMETERS.get();
        if (!Boolean.FALSE.equals(updateParams)) {
            final TestProperties tp = (isNotNull(works.lysenko.Base.properties))
                    ? works.lysenko.Base.properties
                    : new TestProperties();
            final Map<String, String> configProps = tp.getOriginalConfigProperties(suiteName);
            if (isNotNull(configProps)) {
                updateParameterFromConfig(TESTS, PropEnum._TEST_TESTS, configProps);
                updateParameterFromConfig(HEADLESS, PropEnum._TEST_HEADLESS, configProps);
                updateParameterFromConfig(HIDE_OFF_BRANCHES, PropEnum._TREE_HIDE_OFF_BRANCHES, configProps);
                updateParameterFromConfig(ALL_LEAFS, PropEnum._TEST_ALL_LEAFS, configProps);
                updateParameterFromConfig(ALL_LEAFS_EXECUTIONS, PropEnum._TEST_ALL_LEAFS_EXECUTIONS, configProps);
                updateParameterFromConfig(ALL_LEAFS_PATIENCE, PropEnum._TREE_ALL_LEAFS_PATIENCE, configProps);
                updateParameterFromConfig(FORBID_OVEREXECUTION, PropEnum._TREE_FORBID_OVEREXECUTION, configProps);
                updateParameterFromConfig(RESILIENT_MODE, PropEnum._TEST_RESILIENT_MODE, configProps);
                updateParameterFromConfig(COMPLETION_WEIGHT, PropEnum._TREE_COMPLETION_WEIGHT, configProps);
                updateParameterFromConfig(TRAVERSE_EXTENSIONS, PropEnum._TREE_TRAVERSE_EXTENSIONS, configProps);
            }
        }

        saveToFile();
    }

    private void updateParameterFromConfig(final ExecutionParameter param, final _PropEnum propEnum,
                                           final Map<String, String> configProps) {

        final String envVal = System.getenv(param.name());
        if (isNotNull(envVal)) {
            put(param.name(), envVal);
            envValues.put(param.name(), envVal);
        } else {
            final String propVal = configProps.get(propEnum.getPropertyName());
            if (isNotNull(propVal) && !propVal.isBlank() && !"null".equalsIgnoreCase(propVal.trim())) {
                final String trimmed = propVal.trim();
                put(param.name(), trimmed);
                fileValues.put(param.name(), trimmed);
            } else {
                put(param.name(), EMPTY);
                fileValues.remove(param.name());
            }
        }
    }

    /**
     * Performs get additional types.
     * @return the get additional types result
     */
    public String[] getAdditionalTypes() {

        return additionalTypes;
    }

    /**
     * Performs get additional values.
     * @return the get additional values result
     */
    public String[] getAdditionalValues() {

        return additionalValues;
    }

    /**
     * Performs get test.
     * @return the get test result
     */
    public String getTest() {

        final String suiteVal = getValue(SUITE);
        if (isNotNull(suiteVal) && !suiteVal.isBlank()) return suiteVal;
        return (String) get("SUITE");
    }

    /**
     * Performs get suite.
     * @return the get suite result
     */
    public String getSuite() {

        return getTest();
    }

    /**
     * Performs set test.
     * @param name the name
     */
    public void setTest(final String name) {

        if (isNull(name) || name.isBlank()) return;
        final String currentSuite = getSuite();
        final String oldSuite = getValue(OLD_SUITE);
        put(SUITE.name(), name);
        remove("TEST");
        if (!name.equalsIgnoreCase(currentSuite) || !name.equalsIgnoreCase(oldSuite)) {
            applyBackdraft(name);
        } else {
            put(OLD_SUITE.name(), name);
            saveToFile();
        }
        if (isNotNull(works.lysenko.Base.properties)) {
            works.lysenko.Base.properties.readTestConfiguration();
        }
    }

    /**
     * Performs set suite.
     * @param name the name
     */
    public void setSuite(final String name) {

        setTest(name);
    }

    /**
     * Performs set pool.
     * @param name the name
     */
    public void setPool(final String name) {
        
        put(POOL.name(), name);
    }
    
    /**
     * Performs set platform.
     * @param name the name
     */
    public void setPlatform(final String name) {
        
        put(PLATFORM.name(), name);
    }
    
    /**
     * Performs set all leafs.
     * @param value the value
     */
    public void setAllLeafs(final boolean value) {
        
        put(ALL_LEAFS.name(), String.valueOf(value));
    }
    
    /**
     * Performs set all leafs executions.
     * @param count the count
     */
    public void setAllLeafsExecutions(final int count) {
        
        put(ALL_LEAFS_EXECUTIONS.name(), String.valueOf(count));
    }
    
    /**
     * Performs set all leafs patience.
     * @param patience the patience float value
     */
    public void setAllLeafsPatience(final float patience) {
        
        put(ALL_LEAFS_PATIENCE.name(), String.valueOf(patience));
    }
    
    /**
     * Performs set headless.
     * @param value the value
     */
    public void setHeadless(final boolean value) {
        
        put(HEADLESS.name(), String.valueOf(value));
    }

    /**
     * Performs set hide off branches.
     * @param value the value
     */
    public void setHideOffBranches(final boolean value) {
        
        put(HIDE_OFF_BRANCHES.name(), String.valueOf(value));
    }

    /**
     * Performs get value.
     * @param parameter the parameter
     * @return the get value result
     */
    public String getValue(final ExecutionParameter parameter) {

        return getPropertyString(parameter.name());
    }

    /**
     * Performs get domain.
     * @return the get domain result
     */
    public String getDomain() {

        return getValue(DOMAIN);
    }

    /**
     * Performs get local ip address.
     * @return the get local ip address result
     */
    public static String getLocalIpAddress() {

        try {
            final InetAddress localHost = InetAddress.getLocalHost();
            final String hostAddr = localHost.getHostAddress();
            if (isNotNull(hostAddr) && !hostAddr.startsWith("127.")) {
                return hostAddr;
            }
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                final Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    final InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (final Exception ignored) {
            // Fallback
        }
        return "127.0.0.1";
    }

    @Override
    public String getOrigin() {

        final String val = getValue(ORIGIN);
        if (isNotNull(val) && !val.isBlank()) return val;

        final boolean isGui = !(isInsideDocker() || isInsideCI());
        final String mode = isGui ? "GUI" : "CLI";
        final String ip = getLocalIpAddress();
        return isNotNull(ip) && !ip.isBlank() ? s(ip, " [", mode, "]") : mode;
    }

    @Override
    public String getTag() {

        final String val = getValue(TAG);
        if (isNotNull(val) && !val.isBlank()) return val;

        final String suiteVal = getSuite();
        if (isNotNull(suiteVal) && !suiteVal.isBlank()) {
            return suiteVal;
        }
        return "default";
    }

    @Override
    public boolean isAllLeafs() {

        final String countVal = getValue(ALL_LEAFS_EXECUTIONS);
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
        if (getAllLeafsExecutions() > 1) {
            return true;
        }
        return Boolean.TRUE.equals(PropEnum._TEST_ALL_LEAFS.get());
    }

    @Override
    public int getAllLeafsExecutions() {

        final String val = getValue(ALL_LEAFS_EXECUTIONS);
        if (isNotNull(val) && !val.isEmpty()) {
            try {
                return Math.max(1, Integer.parseInt(val));
            } catch (final NumberFormatException ignored) {
            }
        }
        final Integer prop = PropEnum._TEST_ALL_LEAFS_EXECUTIONS.get();
        return isNotNull(prop) ? Math.max(1, prop) : 1;
    }

    @Override
    public boolean isHeadless() {

        final String val = getValue(HEADLESS);
        if (isNotNull(val) && !val.isEmpty()) {
            return Boolean.parseBoolean(val);
        }
        return Boolean.TRUE.equals(PropEnum._TEST_HEADLESS.get());
    }

    /**
     * Performs is hide off branches.
     * @return the is hide off branches result
     */
    public boolean isHideOffBranches() {

        final String val = getValue(HIDE_OFF_BRANCHES);
        if (isNotNull(val) && !val.isEmpty()) {
            return Boolean.parseBoolean(val);
        }
        return Boolean.TRUE.equals(PropEnum._TREE_HIDE_OFF_BRANCHES.get());
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

    /**
     * Performs get tests.
     * @return the get tests result
     */
    public String getTests() {

        return getValue(TESTS);
    }

    /**
     * Performs get forbid overexecution.
     * @return the get forbid overexecution result
     */
    public Boolean getForbidOverexecution() {

        final String val = getValue(FORBID_OVEREXECUTION);
        if (isNotNull(val) && !val.isEmpty()) return Boolean.parseBoolean(val);
        return null;
    }

    /**
     * Performs get resilient mode.
     * @return the get resilient mode result
     */
    public Boolean getResilientMode() {

        final String val = getValue(RESILIENT_MODE);
        if (isNotNull(val) && !val.isEmpty()) return Boolean.parseBoolean(val);
        return null;
    }

    /**
     * Performs get completion weight.
     * @return the get completion weight result
     */
    public String getCompletionWeight() {

        return getValue(COMPLETION_WEIGHT);
    }

    /**
     * Performs get traverse extensions.
     * @return the get traverse extensions result
     */
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
            fileExisted = true;
            cleanupLegacyTestKey();
            for (final String name : stringPropertyNames()) {
                fileValues.put(name, getProperty(name));
            }
        } catch (final IOException e) {
            // NOP
        }
    }

    private void cleanupLegacyTestKey() {

        if (containsKey("TEST")) {
            final String oldTestVal = (String) remove("TEST");
            if ((!containsKey(SUITE.name()) || isNull(get(SUITE.name())) || ((String) get(SUITE.name())).isBlank())
                    && isNotNull(oldTestVal) && !oldTestVal.isBlank()) {
                put(SUITE.name(), oldTestVal);
            }
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

        final String envVal = System.getenv(name);
        if (isNotNull(envVal)) {
            put(name, envVal);
            envValues.put(name, envVal);
        } else if (!containsKey(name)) {
            put(name, def);
        }
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
     * The list of parameters should be formatted as "params;types".
     * If the given list is null, no parameters are loaded.
     *
     * @param list the list of parameters
     */
    private void readAdditionalParameters(final String list) {

        if (isNotNull(list) && !list.isBlank()) {
            final String[] data = list.split(s(SEM_CLN));
            if (data.length >= 2) {
                additionalValues = data[0].split(l0);
                additionalTypes = data[1].split(l0);
                for (final String param : additionalValues) {
                    if (isNotNull(param) && !param.isEmpty()) read(param);
                }
            }
        }
    }

    /**
     * Configures additional parameter names and types (e.g., "t" for text, "p" for password).
     *
     * @param values array of additional parameter names
     * @param types  array of parameter types corresponding to {@code values}
     */
    public void setAdditionalParameters(final String[] values, final String[] types) {

        this.additionalValues = values;
        this.additionalTypes = types;
    }

    void recordFileValue(final String name, final String value) {

        if (isNotNull(name) && isNotNull(value)) {
            fileValues.put(name, value);
            fileExisted = true;
        }
    }

    void recordEnvValue(final String name, final String value) {

        if (isNotNull(name) && isNotNull(value)) {
            envValues.put(name, value);
        }
    }

    /**
     * Checks whether the given parameter's current value came from an environment variable override.
     *
     * @param name  parameter key
     * @param value current parameter value
     * @return true if overridden by environment variable with matching value
     */
    public boolean isEnvValue(final String name, final String value) {

        return envValues.containsKey(name) && Objects.equals(value, envValues.get(name));
    }

    /**
     * Checks whether the given parameter's current value came unchanged from persisted file or suite backdraft.
     *
     * @param name  parameter key
     * @param value current parameter value
     * @return true if loaded from file/backdraft and not overridden by environment
     */
    public boolean isFileValue(final String name, final String value) {

        return !isEnvValue(name, value) && fileValues.containsKey(name) && Objects.equals(value, fileValues.get(name));
    }

    /**
     * Checks whether the given key is a declared additional parameter.
     *
     * @param name parameter key
     * @return true if declared in additionalValues
     */
    public boolean isAdditionalParameter(final String name) {

        if (isNull(additionalValues) || isNull(name)) return false;
        for (final String param : additionalValues) {
            if (name.equals(param)) return true;
        }
        return false;
    }

    /**
     * Checks whether the given key is a password-type ("p") additional parameter.
     *
     * @param name parameter key
     * @return true if password parameter
     */
    public boolean isPasswordParameter(final String name) {

        if (isNull(additionalValues) || isNull(additionalTypes) || isNull(name)) return false;
        for (int i = 0; i < additionalValues.length && i < additionalTypes.length; i++) {
            if (name.equals(additionalValues[i])) {
                return "p".equalsIgnoreCase(additionalTypes[i]);
            }
        }
        return false;
    }

    /**
     * Masks the value if the parameter is marked as a password parameter.
     *
     * @param name  parameter key
     * @param value raw parameter value
     * @return masked value if sensitive and non-empty, otherwise raw value
     */
    public String maskIfSensitive(final String name, final String value) {

        if (isNotNull(value) && !value.isEmpty() && isPasswordParameter(name)) {
            return MASK;
        }
        return value;
    }

    private String computeDefaultOrigin() {

        final boolean isGui = !(isInsideDocker() || isInsideCI());
        final String mode = isGui ? "GUI" : "CLI";
        final String ip = getLocalIpAddress();
        return isNotNull(ip) && !ip.isBlank() ? s(ip, " [", mode, "]") : mode;
    }

    private String computeDefaultTag() {

        final String suiteVal = getSuite();
        return (isNotNull(suiteVal) && !suiteVal.isBlank()) ? suiteVal : "default";
    }

    /**
     * Builds a sorted map of all known default parameter values (standard ExecutionParameters and additional parameters).
     *
     * @return sorted map of default parameter key-value pairs
     */
    public Map<String, String> getSortedDefaults() {

        final Map<String, String> defaults = new SortedString();
        final String suiteVal = getSuite();
        final String expectedOldSuite = isNotNull(suiteVal) ? suiteVal : EMPTY;
        for (final ExecutionParameter param : ExecutionParameter.values()) {
            final String def = switch (param) {
                case ALL_LEAFS_EXECUTIONS -> "1";
                case ORIGIN -> computeDefaultOrigin();
                case TAG -> computeDefaultTag();
                case OLD_SUITE -> expectedOldSuite;
                default -> param.def();
            };
            defaults.put(param.name(), def);
        }
        if (isNotNull(additionalValues)) {
            for (final String param : additionalValues) {
                if (isNotNull(param) && !param.isEmpty()) {
                    defaults.putIfAbsent(param, EMPTY);
                }
            }
        }
        return defaults;
    }

    /**
     * Builds a sorted map of applied (redefined, environment-overridden, or unknown) execution parameters.
     * Sensitive password parameters are masked in the returned map.
     *
     * @param defaults map of default parameter values for comparison
     * @return sorted map of applied parameter key-value pairs
     */
    public Map<String, String> getSortedApplied(final Map<String, String> defaults) {

        final Map<String, String> sorted = new SortedString();
        for (final String key : stringPropertyNames()) {
            final String rawVal = getProperty(key, EMPTY);
            if (!defaults.containsKey(key)) {
                sorted.put(key, maskIfSensitive(key, rawVal));
            } else if (isEnvValue(key, rawVal)) {
                sorted.put(key, maskIfSensitive(key, rawVal));
            } else if (!rawVal.isEmpty() && !rawVal.equalsIgnoreCase(defaults.get(key))) {
                sorted.put(key, maskIfSensitive(key, rawVal));
            }
        }
        return sorted;
    }

    private int countAdditionalParameters() {

        if (isNull(additionalValues)) return 0;
        int count = 0;
        for (final String param : additionalValues) {
            if (isNotNull(param) && !param.isEmpty() && null == ExecutionParameter.byName(param)) {
                count++;
            }
        }
        return count;
    }

    private String buildDebugSummary() {

        final List<KeyValue<?, ?>> entries = new ArrayList<>(4);
        entries.add(kv(FILE, fileExisted ? PARAMETERS_ : NONE));
        final String mode = guiDisplayed ? "GUI" : "CLI";
        entries.add(kv(MODE, mode));
        entries.add(kv("env", envValues.size()));
        final int addCount = countAdditionalParameters();
        if (addCount > 0) {
            entries.add(kv(ADDITIONAL, addCount));
        }
        return b(a(entries, COMMA_SPACE));
    }

    /**
     * Outputs and validates the applied and unchanged default execution parameters block.
     */
    @SuppressWarnings("UseOfSystemOutOrSystemErr")
    public void logParametersConfiguration() {

        System.out.println(gray(a(PARAMETERS, buildDebugSummary())));
        final Map<String, String> defaults = getSortedDefaults();
        final int expectedDefaultsSize = defaults.size();
        final Map<String, String> sorted = getSortedApplied(defaults);
        final ParametersMeta meta = new ParametersMeta(
                defaults,
                new ArrayList<>(0),
                expectedDefaultsSize,
                countAdditionalParameters()
        );
        Renderer.outputAndValidate(this, sorted, meta);
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
    public void saveToFile() {

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

        if (!(isInsideDocker() || isInsideCI()) && !java.awt.GraphicsEnvironment.isHeadless()) {
            works.lysenko.util.apis.util._UiProvider.find().ifPresent(provider -> {
                final _GUI gui = provider.createSetupGui(this);
                guiDisplayed = true;
                gui.display();
                saveToFile();
            });
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
                (getDomain().isBlank()) ? EMPTY : s(e(ON), yb(getDomain())),
                (null == getOrigin() || getOrigin().isBlank()) ? EMPTY : s(e(FROM), yb(getOrigin())),
                (null == getTag() || getTag().isBlank()) ? EMPTY : s("[", yb(getTag()), "]")
        );
    }
}
