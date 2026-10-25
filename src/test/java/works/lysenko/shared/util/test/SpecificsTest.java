package works.lysenko.shared.util.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;
import works.lysenko.Base;
import works.lysenko.base.Core;
import works.lysenko.base.Exec;
import works.lysenko.base.exec.Scenarios;
import works.lysenko.util.apis.exception.checked.SafeguardException;
import works.lysenko.util.apis.log._Logs;
import works.lysenko.util.data.enums.Severity;
import works.lysenko.util.data.records.test.Triplet;

import java.io.Closeable;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SpecificsTest {
    private Exec previousExec;
    private Core previousCore;

    @BeforeEach
    void setUp() throws Exception {
        previousExec = Base.exec;
        previousCore = Base.core;

        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);

        final Exec mockExec = (Exec) unsafe.allocateInstance(Exec.class);
        final Field scenariosField = Exec.class.getDeclaredField("scenarios");
        scenariosField.setAccessible(true);
        scenariosField.set(mockExec, new Scenarios());
        Base.exec = mockExec;

        final Core mockCore = (Core) unsafe.allocateInstance(Core.class);
        
        final _Logs mockLogs = new _Logs() {
            @Override public Closeable getLogWriter() { return null; }
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
        lf.set(mockCore, mockLogs);
        
        Base.core = mockCore;
    }

    @AfterEach
    void tearDown() {
        Base.exec = previousExec;
        Base.core = previousCore;
    }

    @Test
    void testAction() throws Exception {
        final Specifics specifics = new Specifics();
        Base.properties = new works.lysenko.base.TestProperties();
        
        final Field exceptionField = works.lysenko.tree.base.exceptional.Core.class.getDeclaredField("exception");
        exceptionField.setAccessible(true);
        exceptionField.set(specifics, new RuntimeException("Test Exception"));

        final Field tripletField = works.lysenko.tree.base.exceptional.Core.class.getDeclaredField("triplet");
        tripletField.setAccessible(true);
        tripletField.set(specifics, new Triplet(new RuntimeException(), null, null));
        
        assertDoesNotThrow(specifics::action);
    }
}
