package works.lysenko.base.telemetry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Data}.
 */
class DataTest {

    @Test
    void testDataRecord() {
        Data data = new Data(1000L, 4, 8, 50.0, 100, 10L, 110L, 1000000L, 2000000L, 4000000L, 5, 100000L, "test");
        assertEquals(1000L, data.timestamp());
        assertEquals(4, data.cores());
        assertEquals(8, data.threads());
        assertEquals(50.0, data.spent());
        assertEquals("test", data.comment());
    }
}
