package works.lysenko.base;

import works.lysenko.base.parameters.Gui;
import works.lysenko.util.apis._PropEnum;
import works.lysenko.util.apis.parameters._AdditionalParameters;
import works.lysenko.util.apis.parameters._ExecutionParameterValue;
import works.lysenko.util.apis.parameters._GUI;
import works.lysenko.util.apis.parameters._TestParameters;
import works.lysenko.util.data.enums.ExecutionParameter;
import works.lysenko.util.data.enums.Platform;
import works.lysenko.util.prop.*;

import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
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
        } else {
            final String propVal = configProps.get(propEnum.getPropertyName());
            if (isNotNull(propVal) && !propVal.isBlank() && !"null".equalsIgnoreCase(propVal.trim())) {
                put(param.name(), propVal.trim());
            } else {
                put(param.name(), EMPTY);
            }
        }
    }

    public String[] getAdditionalTypes() {

        return additionalTypes;
    }

    public String[] getAdditionalValues() {

        return additionalValues;
    }

    public String getTest() {

        final String suiteVal = getValue(SUITE);
        if (isNotNull(suiteVal) && !suiteVal.isBlank()) return suiteVal;
        return (String) get("TEST");
    }

    public String getSuite() {

        return getTest();
    }

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

    public void setSuite(final String name) {

        setTest(name);
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
    
    public void setAllLeafsExecutions(final int count) {
        
        put(ALL_LEAFS_EXECUTIONS.name(), String.valueOf(count));
    }
    
    public void setHeadless(final boolean value) {
        
        put(HEADLESS.name(), String.valueOf(value));
    }

    public void setHideOffBranches(final boolean value) {
        
        put(HIDE_OFF_BRANCHES.name(), String.valueOf(value));
    }

    public String getValue(final ExecutionParameter parameter) {

        return getPropertyString(parameter.name());
    }

    public String getDomain() {

        return getValue(DOMAIN);
    }

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
            cleanupLegacyTestKey();
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

        if (isNotNull(System.getenv(name))) put(name, System.getenv(name));
        else if (!containsKey(name)) put(name, def);
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
                (getDomain().isBlank()) ? EMPTY : s(e(ON), yb(getDomain())),
                (null == getOrigin() || getOrigin().isBlank()) ? EMPTY : s(e(FROM), yb(getOrigin())),
                (null == getTag() || getTag().isBlank()) ? EMPTY : s("[", yb(getTag()), "]")
        );
    }
}
