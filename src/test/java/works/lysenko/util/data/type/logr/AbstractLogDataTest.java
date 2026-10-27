package works.lysenko.util.data.type.logr;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractLogDataTest {
    @Test
    void testAbstractLogData() {
        AbstractLogData logData = new AbstractLogData(1L, 2, "Test Message") {
        };
        assertEquals(1L, logData.id());
        assertEquals(2, logData.depth());
        assertEquals("Test Message", logData.message());
    }
}
