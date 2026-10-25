package works.lysenko.util.func.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StacktraceTest {

    @Test
    void testStacktraceInstantiation() {
        Stacktrace stacktrace = new Stacktrace();
        assertNotNull(stacktrace);
    }

    @Test
    void testClearReferences() {
        assertDoesNotThrow(Stacktrace::clearReferences);
    }

    @Test
    void testGetShortWithCurrentThread() {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        assertDoesNotThrow(() -> Stacktrace.getShort(trace));
    }
}
