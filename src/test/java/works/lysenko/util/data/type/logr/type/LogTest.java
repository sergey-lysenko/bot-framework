package works.lysenko.util.data.type.logr.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogTest {
    @Test
    void testLog() {
        Log log = new Log(1L, 2, 3, "Test Message");
        assertEquals(3, log.level());
        assertEquals("Test Message", log.message());
    }
}
