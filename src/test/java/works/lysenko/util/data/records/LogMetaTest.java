package works.lysenko.util.data.records;

import org.junit.jupiter.api.Test;
import works.lysenko.util.data.enums.Severity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LogMetaTest {

    @Test
    void testLogMetaRecord() {
        LogMeta meta = new LogMeta(null, 0.0, null, 0.0, true, Severity.S0);
        assertNotNull(meta);
        assertEquals(Severity.S0, meta.severity());
    }
}
