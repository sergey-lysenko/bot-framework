package works.lysenko.tree;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Results;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.data.enums.EventType;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.type.LogRecord;
import works.lysenko.util.data.type.logr.type.Event;
import sun.misc.Unsafe;

import java.io.Closeable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"removal", "deprecation"})
class EmptyScenariosTest {

    private Core previousCore;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);
        final Core core = (Core) unsafe.allocateInstance(Core.class);
        final Results results = new Results();
        final Field rf = Core.class.getDeclaredField("results");
        rf.setAccessible(true);
        rf.set(core, results);

        final _Logs mockLogs = new _Logs() {
            private long count = 0;

            @Override public Closeable getLogWriter() { return null; }
            @Override public int getSpanLength() { return 0; }
            @Override public Closeable getTelemetryWriter() { return null; }
            @Override public void log(String message) {}
            @Override public void log(int level, String message) {}
            @Override public void log(int level, String message, Long redefinedTime) {}
            @Override public void logEmptyLine() {}
            @Override
            public void logEvent(Severity severity, String message, String shortStackTrace) {
                final EventType type = (null != severity) ? severity.type() : EventType.UNDEFINED;
                final LogRecord lr = new LogRecord(null, System.currentTimeMillis(), new Event(++count, type, 0, message, shortStackTrace));
                core.getResults().addEvent(lr);
            }
        };

        final Field lf = Core.class.getDeclaredField("logger");
        lf.setAccessible(true);
        lf.set(core, mockLogs);

        Base.core = core;
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
        Base.exec = null;
    }

    @Test
    void testEmptyRootStopsExecutionAndRegistersFailingEvent() throws SafeguardException {
        final Ctrl emptyCtrl = new Ctrl(null, Collections.emptyList());

        assertFalse(Base.core.getStopFlag(), "Stop flag should initially be false");
        assertFalse(Base.core.getResults().areFailingEvents(), "Should have no failing events initially");

        final boolean result = emptyCtrl.exec();
        assertFalse(result, "Empty Ctrl execution should return false");

        assertTrue(Base.core.getStopFlag(), "Stop flag should be set to true when root has no scenarios");
        assertTrue(Base.core.getResults().areFailingEvents(), "Results should record failing events");
        assertEquals(Severity.S0, Base.core.getResults().getGreatestSeverity());

        final List<String> failures = Base.core.getResults().getFailures();
        assertFalse(failures.isEmpty(), "Failures list should not be empty");
        assertTrue(failures.get(0).contains("has no Scenarios"));
    }
}
