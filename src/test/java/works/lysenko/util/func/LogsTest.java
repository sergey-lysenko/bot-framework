package works.lysenko.util.func;
import works.lysenko.testing.SelectionFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Exec;
import works.lysenko.base.Results;
import works.lysenko.base.TestProperties;
import works.lysenko.base.exec.Scenarios;
import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.data.enums.Severity;

import java.io.Closeable;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LogsTest {

    private Core previousCore;
    private Exec previousExec;
    private TestProperties previousProperties;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousExec = Base.exec;
        previousProperties = Base.properties;

        Base.properties = new TestProperties();

        final Core core = (Core) SelectionFixture.allocate(Core.class);
        final Results results = new Results();
        final Field rf = Core.class.getDeclaredField("results");
        rf.setAccessible(true);
        rf.set(core, results);

        final _Logs mockLogs = new _Logs() {
            @Override public Closeable getLogWriter() { return null; }
            @Override public void flush() {}
            @Override public int getSpanLength() { return 0; }
            @Override public Closeable getTelemetryWriter() { return null; }
            @Override public void log(String message) {}
            @Override public void log(int level, String message) {}
            @Override public void log(int level, String message, Long redefinedTime) {}
            @Override public void logEmptyLine() {}
            @Override public void logEvent(Severity severity, String message, String shortStackTrace) {}
        };

        final Field lf = Core.class.getDeclaredField("logger");
        lf.setAccessible(true);
        lf.set(core, mockLogs);
        Base.core = core;

        final Exec mockExec = (Exec) SelectionFixture.allocate(Exec.class);
        final Field scenariosField = Exec.class.getDeclaredField("scenarios");
        scenariosField.setAccessible(true);
        scenariosField.set(mockExec, new Scenarios());
        Base.exec = mockExec;
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
        Base.exec = previousExec;
        Base.properties = previousProperties;
    }

    @Test
    void testLogGridNullInputs() {
        assertFalse(Logs.logGrid(null, null));
    }

    @Test
    void testInstantiation() {
        Logs logs = new Logs();
        assertNotNull(logs);
    }
}
