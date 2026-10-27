package works.lysenko.shared.util.test;
import works.lysenko.testing.SelectionFixture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import works.lysenko.Base;
import works.lysenko.base.Exec;
import works.lysenko.base.exec.Scenarios;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PostflightTest {
    private Exec previousExec;

    private works.lysenko.base.Core previousCore;

    @BeforeEach
    void setUp() throws Exception {
        previousExec = Base.exec;
        previousCore = Base.core;
        final Exec mockExec = (Exec) SelectionFixture.allocate(Exec.class);
        final Field scenariosField = Exec.class.getDeclaredField("scenarios");
        scenariosField.setAccessible(true);
        scenariosField.set(mockExec, new Scenarios());
        Base.exec = mockExec;
        Base.properties = new works.lysenko.base.TestProperties();

        final works.lysenko.base.Core mockCore = (works.lysenko.base.Core) SelectionFixture.allocate(works.lysenko.base.Core.class);
        final Field loggerField = works.lysenko.base.Core.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(mockCore, new works.lysenko.util.apis.log._Logs() {
            @Override public java.io.Closeable getLogWriter() { return null; }
            @Override public void flush() {}
            @Override public int getSpanLength() { return 0; }
            @Override public java.io.Closeable getTelemetryWriter() { return null; }
            @Override public void log(String message) {}
            @Override public void log(int level, String message) {}
            @Override public void log(int level, String message, Long redefinedTime) {}
            @Override public void logEmptyLine() {}
            @Override public void logEvent(works.lysenko.util.data.enums.Severity severity, String message, String shortStackTrace) {}
        });
        Base.core = mockCore;
    }

    @AfterEach
    void tearDown() {
        Base.exec = previousExec;
        Base.core = previousCore;
    }

    @Test
    void testRun() {
        final Postflight postflight = new Postflight();
        assertDoesNotThrow(postflight::run);
    }
}
