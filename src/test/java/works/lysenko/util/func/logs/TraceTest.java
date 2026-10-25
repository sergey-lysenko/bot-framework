package works.lysenko.util.func.logs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TraceTest {

    @Test
    void testTraceInstantiation() {
        Trace trace = new Trace();
        assertNotNull(trace);
    }
}
