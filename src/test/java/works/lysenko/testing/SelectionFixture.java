package works.lysenko.testing;

import sun.misc.Unsafe;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Exec;
import works.lysenko.base.Parameters;
import works.lysenko.base.Results;
import works.lysenko.base.TestProperties;
import works.lysenko.base.exec.Scenarios;
import works.lysenko.base.output.SequenceTracker;
import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.prop.PropEnum;
import works.lysenko.util.prop.core.Mode;
import works.lysenko.util.prop.tree.Notify;
import works.lysenko.util.prop.tree.Scenario;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Installs a minimal, browser-free execution environment so scenario selection can be unit tested.
 * <p>
 * It replaces {@link Base#core}, {@link Base#exec}, {@link Base#parameters} and {@link Base#properties},
 * records every event (and plain log line) written through the logger, and restores everything - including
 * the mutable static switches that selection reads - on {@link #close()}.
 */
@SuppressWarnings({"removal", "deprecation", "sunapi"})
public final class SelectionFixture implements AutoCloseable {


    /**
     * Events recorded as {@code "<severity>: <message>"}, in logging order.
     */
    public final List<String> events = new ArrayList<>();

    /**
     * Plain log lines in logging order.
     */
    public final List<String> logs = new ArrayList<>();

    private final Core previousCore = Base.core;
    private final Exec previousExec = Base.exec;
    private final Parameters previousParameters = Base.parameters;
    private final TestProperties previousProperties = Base.properties;
    private final boolean previousResilient = Mode.resilient;
    private final boolean previousIgnoreFailedSelection = Scenario.ignoreFailedSelection;
    private final boolean previousDoesNotFit = Notify.doesNotFit;
    private final boolean previousNotExecutable = Notify.notExecutable;
    private final Core core;

    private SelectionFixture() throws Exception {

        Base.parameters = null;
        Base.properties = null;
        SequenceTracker.reset();

        final Unsafe unsafe = unsafe();
        core = (Core) unsafe.allocateInstance(Core.class);
        set(Core.class, core, "results", new Results());
        set(Core.class, core, "logger", recordingLogger());
        Base.core = core;

        final Exec exec = (Exec) unsafe.allocateInstance(Exec.class);
        set(Exec.class, exec, "scenarios", new Scenarios());
        Base.exec = exec;
    }

    /**
     * Allocates an instance of a class without invoking any constructor.
     *
     * @param <T>   the type of the instance to allocate
     * @param clazz the class to allocate an instance of
     * @return a uninitialized instance of the specified class
     */
    @SuppressWarnings({"unchecked", "sunapi", "removal"})
    public static <T> T allocate(final Class<T> clazz) {

        try {
            return (T) unsafe().allocateInstance(clazz);
        } catch (final Exception e) {
            throw new IllegalStateException("Failed to allocate instance of " + clazz.getName(), e);
        }
    }


    /**
     * @return a freshly installed fixture; use in try-with-resources or call {@link #close()} in teardown
     */
    public static SelectionFixture install() {

        try {
            return new SelectionFixture();
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return the installed execution core
     */
    public Core core() {

        return core;
    }

    /**
     * Sets a single property on {@link Base#properties}, keeping any previously set test properties.
     */
    public void setProperty(final PropEnum property, final String value) {

        try {
            final TestProperties testProperties = new TestProperties();
            final Field theField = TestProperties.class.getDeclaredField("the");
            theField.setAccessible(true);
            final Properties props = new Properties();
            if (null != Base.properties) props.putAll((Properties) theField.get(Base.properties));
            props.put(property.getPropertyName(), value);
            theField.set(testProperties, props);
            Base.properties = testProperties;
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {

        Base.core = previousCore;
        Base.exec = previousExec;
        Base.parameters = previousParameters;
        Base.properties = previousProperties;
        Mode.resilient = previousResilient;
        Scenario.ignoreFailedSelection = previousIgnoreFailedSelection;
        Notify.doesNotFit = previousDoesNotFit;
        Notify.notExecutable = previousNotExecutable;
        SequenceTracker.reset();
    }

    private _Logs recordingLogger() {

        return (_Logs) Proxy.newProxyInstance(_Logs.class.getClassLoader(), new Class<?>[]{_Logs.class}, (proxy, method, args) -> {
            if ("logEvent".equals(method.getName())) {
                events.add(args[0] + ": " + args[1]);
            } else if ("log".equals(method.getName()) && null != args && 0 < args.length) {
                if (args[0] instanceof String line) {
                    logs.add(line);
                } else if (args.length >= 2 && args[1] instanceof String line) {
                    logs.add(line);
                }
            }
            final Class<?> type = method.getReturnType();
            if (boolean.class == type) return false;
            if (int.class == type) return 0;
            return null;
        });
    }

    private static Unsafe unsafe() throws Exception {

        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        return (Unsafe) f.get(null);
    }

    private static void set(final Class<?> owner, final Object target, final String name, final Object value) throws Exception {

        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
