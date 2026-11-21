package works.lysenko.base.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Parameters;
import works.lysenko.base.Results;
import works.lysenko.base.Stopwatch;
import works.lysenko.base.TestProperties;
import works.lysenko.testing.SelectionFixture;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.apis.scenario._Ctrl;
import works.lysenko.util.apis.scenario._Scenario;
import works.lysenko.util.apis.test._ExceptionsHandler;
import works.lysenko.util.apis.test._Exec;
import works.lysenko.util.apis.test._Patience;
import works.lysenko.util.apis.test._Repeater;
import works.lysenko.util.apis.test._Stat;
import works.lysenko.util.apis.test._Test;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.records.test.Workflow;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@SuppressWarnings({"removal", "deprecation"})
class PretestLifecycleTest {

    private Core previousCore;
    private Parameters previousParameters;
    private TestProperties previousProperties;
    private works.lysenko.base.Test previousTest;
    private Stopwatch previousTimer;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousParameters = Base.parameters;
        previousProperties = Base.properties;
        previousTest = Base.test;
        previousTimer = Base.timer;

        Base.properties = null;
        Base.timer = new Stopwatch();

        Base.parameters = new Parameters(new Properties());

        final Core core = SelectionFixture.allocate(Core.class);
        final Field rf = Core.class.getDeclaredField("results");
        rf.setAccessible(true);
        rf.set(core, new Results());

        final _Logs silentLogger = new _Logs() {
            @Override public Closeable getLogWriter() { return null; }
            @Override public void flush() {}
            @Override public int getSpanLength() { return 0; }
            @Override public Closeable getTelemetryWriter() { return null; }
            @Override public void log(final String message) {}
            @Override public void log(final int level, final String message) {}
            @Override public void log(final int level, final String message, final Long redefinedTime) {}
            @Override public void logEmptyLine() {}
            @Override public void logEvent(final Severity severity, final String message, final String shortStackTrace) {}
        };
        final Field lf = Core.class.getDeclaredField("logger");
        lf.setAccessible(true);
        lf.set(core, silentLogger);

        final _Test stubTest = new _Test() {
            @Override public _Patience patience() { return null; }
            @Override public void complete() {}
            @Override public void execute() {}
            @Override public _Exec executor() { return null; }
            @Override public _ExceptionsHandler handler() { return null; }
            @Override public _Stat statistics() { return null; }
            @Override public Runnable postflight() { return null; }
            @Override public Runnable preflight() { return null; }
            @Override public _Repeater repeater() { return null; }
            @Override public String getStatus() { return ""; }
            @Override public boolean isStopRequestedByDashboard() { return false; }
            @Override public void stop() {}
            @Override public void prepareRoot() {}
        };
        final Field tf = Core.class.getDeclaredField("test");
        tf.setAccessible(true);
        tf.set(core, stubTest);

        Base.core = core;
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
        Base.parameters = previousParameters;
        Base.properties = previousProperties;
        Base.test = previousTest;
        Base.timer = previousTimer;
    }

    static final class SamplePretest implements Runnable {
        @Override
        public void run() {
            // no-op
        }
    }

    @Test
    void workflowSupportsPretestAndBackwardCompatibleConstructor() {
        final Workflow withPretest = new Workflow(null, SamplePretest.class, null, null, null, List.of());
        assertEquals(SamplePretest.class, withPretest.pretest());

        final Workflow legacy = new Workflow(null, null, null, null, List.of());
        assertNull(legacy.pretest());
    }

    @Test
    void repeaterRunsPretestBeforeEachExecution() throws SafeguardException {
        final List<String> events = new ArrayList<>();
        final Runnable pretest = () -> events.add("pretest");
        final _Exec executor = new _Exec() {
            @Override public _Ctrl ctrl() { return null; }
            @Override public void ctrl(final _Ctrl ctrl) {}
            @Override public void exec() { events.add("exec"); }
            @Override public Set<_Scenario> getRootScenariosList() { return Set.of(); }
        };
        final _ExceptionsHandler handler = new _ExceptionsHandler() {
            @Override public Exception getException() { return null; }
            @Override public void handle(final RuntimeException exception) { events.add("error"); }
            @Override public void setExceptional(final _Scenario scenario) {}
        };
        final Stat stat = new Stat();
        final Repeater repeater = new Repeater(handler, executor, stat, () -> pretest);

        repeater.run();
        repeater.run();

        assertEquals(List.of("pretest", "exec", "pretest", "exec"), events);
        assertEquals(2, stat.history().size());
    }

    @Test
    void repeaterRoutesPretestExceptionToHandlerWithClassNameContextAndSkipsExec() throws SafeguardException {
        final List<String> events = new ArrayList<>();
        final RuntimeException boom = new IllegalStateException("reset failed");
        final class FailingPretest implements Runnable {
            @Override
            public void run() {
                events.add("pretest");
                throw boom;
            }
        }
        final Runnable pretest = new FailingPretest();
        final _Exec executor = new _Exec() {
            @Override public _Ctrl ctrl() { return null; }
            @Override public void ctrl(final _Ctrl ctrl) {}
            @Override public void exec() { events.add("exec"); }
            @Override public Set<_Scenario> getRootScenariosList() { return Set.of(); }
        };
        final AtomicReference<RuntimeException> caught = new AtomicReference<>();
        final AtomicReference<String> context = new AtomicReference<>();
        final _ExceptionsHandler handler = new _ExceptionsHandler() {
            @Override public Exception getException() { return caught.get(); }
            @Override public void handle(final RuntimeException exception) { caught.set(exception); }
            @Override public void handle(final RuntimeException exception, final String ctx) {
                caught.set(exception);
                context.set(ctx);
                events.add("handled:" + ctx);
            }
            @Override public void setExceptional(final _Scenario scenario) {}
        };
        final Stat stat = new Stat();
        final Repeater repeater = new Repeater(handler, executor, stat, () -> pretest);

        repeater.run();

        assertEquals(List.of("pretest", "handled:FailingPretest"), events);
        assertSame(boom, caught.get());
        assertEquals("FailingPretest", context.get());
    }

    @Test
    void repeaterSkipsExecWhenPretestStopsSession() throws SafeguardException {
        final List<String> events = new ArrayList<>();
        final Runnable pretest = () -> {
            events.add("pretest-stop");
            Base.core.setStopFlag(true);
        };
        final _Exec executor = new _Exec() {
            @Override public _Ctrl ctrl() { return null; }
            @Override public void ctrl(final _Ctrl ctrl) {}
            @Override public void exec() { events.add("exec"); }
            @Override public Set<_Scenario> getRootScenariosList() { return Set.of(); }
        };
        final _ExceptionsHandler handler = new _ExceptionsHandler() {
            @Override public Exception getException() { return null; }
            @Override public void handle(final RuntimeException exception) { events.add("error"); }
            @Override public void setExceptional(final _Scenario scenario) {}
        };
        final Stat stat = new Stat();
        final Repeater repeater = new Repeater(handler, executor, stat, () -> pretest);

        repeater.run();

        assertEquals(List.of("pretest-stop"), events);
    }
}
