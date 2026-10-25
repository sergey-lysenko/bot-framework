package works.lysenko.util.data.type.logr.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LogTest {

    @Test
    void testLog() {
        final Log log = new Log(1L, 2, 1, "Test Message");
        assertEquals(1L, log.id());
        assertEquals(2, log.depth());
        assertEquals(1, log.level());
        assertEquals("Test Message", log.message());
        assertNotNull(log.render());
        assertNotNull(log.toString());
    }
}
