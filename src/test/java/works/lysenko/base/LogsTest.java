package works.lysenko.base;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;
import works.lysenko.Base;
import works.lysenko.util.data.enums.Severity;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class LogsTest {

    private Core previousCore;
    private Parameters previousParameters;
    private TestProperties previousProperties;
    private Stopwatch previousTimer;
    private works.lysenko.base.Exec previousExec;

    @BeforeEach
    void setUp() throws Exception {
        previousCore = Base.core;
        previousParameters = Base.parameters;
        previousProperties = Base.properties;
        previousTimer = Base.timer;
        previousExec = Base.exec;
        Base.parameters = new Parameters(new Properties());
        Base.properties = new TestProperties();
        Base.timer = new Stopwatch();

        final Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        final Unsafe unsafe = (Unsafe) f.get(null);
        final Core mockCore = (Core) unsafe.allocateInstance(Core.class);
        final Field loggerField = Core.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(mockCore, new works.lysenko.util.apis.log._Logs() {
            @Override public java.io.Closeable getLogWriter() { return null; }
            @Override public int getSpanLength() { return 0; }
            @Override public java.io.Closeable getTelemetryWriter() { return null; }
            @Override public void log(String message) {}
            @Override public void log(int level, String message) {}
            @Override public void log(int level, String message, Long redefinedTime) {}
            @Override public void logEmptyLine() {}
            @Override public void logEvent(Severity severity, String message, String shortStackTrace) {}
        });
        Base.core = mockCore;
        
        final works.lysenko.base.Exec mockExec = (works.lysenko.base.Exec) unsafe.allocateInstance(works.lysenko.base.Exec.class);
        final Field scenariosField = works.lysenko.base.Exec.class.getDeclaredField("scenarios");
        scenariosField.setAccessible(true);
        scenariosField.set(mockExec, new works.lysenko.base.exec.Scenarios());
        Base.exec = mockExec;

        final Field testField = Core.class.getDeclaredField("test");
        testField.setAccessible(true);
        testField.set(mockCore, java.lang.reflect.Proxy.newProxyInstance(
                works.lysenko.util.apis.test._Test.class.getClassLoader(),
                new Class<?>[]{works.lysenko.util.apis.test._Test.class},
                (proxy, method, args) -> {
                    if ("repeater".equals(method.getName())) {
                        return java.lang.reflect.Proxy.newProxyInstance(
                                works.lysenko.util.apis.test._Repeater.class.getClassLoader(),
                                new Class<?>[]{works.lysenko.util.apis.test._Repeater.class},
                                (proxy2, method2, args2) -> {
                                    if (method2.getReturnType() == int.class) return 0;
                                    if (method2.getReturnType() == Integer.class) return 0;
                                    if (method2.getReturnType() == boolean.class) return false;
                                    return null;
                                }
                        );
                    }
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == Integer.class) return 0;
                    if (method.getReturnType() == boolean.class) return false;
                    return null;
                }
        ));

        final Field issuesField = works.lysenko.base.Exec.class.getDeclaredField("issues");
        issuesField.setAccessible(true);
        issuesField.set(mockExec, new works.lysenko.base.exec.Issues());
        
        final Field scenariosFieldExec = works.lysenko.base.Exec.class.getDeclaredField("scenarios");
        scenariosFieldExec.setAccessible(true);
        scenariosFieldExec.set(mockExec, new works.lysenko.base.exec.Scenarios());

        final Field resultsField = works.lysenko.base.Core.class.getDeclaredField("results");
        resultsField.setAccessible(true);
        resultsField.set(mockCore, new works.lysenko.base.Results());
    }

    @AfterEach
    void tearDown() {
        Base.core = previousCore;
        Base.parameters = previousParameters;
        Base.properties = previousProperties;
        Base.timer = previousTimer;
        Base.exec = previousExec;
    }

    @Test
    void testLogsInitializationAndLogging() {
        Logs logs = new Logs(Collections.emptySet());
        assertNotNull(logs);
        assertNotNull(logs.getLogWriter());
        assertNotNull(logs.getTelemetryWriter());
        assertTrue(logs.getSpanLength() >= 0);

        logs.log("Test log message");
        logs.logEmptyLine();
        logs.logEvent(Severity.S0, "Test event", null);
    }
}
