package works.lysenko.util.data.type;

import org.junit.jupiter.api.Test;
import works.lysenko.util.apis.log._LogData;
import works.lysenko.util.data.type.logr.type.LineFeed;

import static org.junit.jupiter.api.Assertions.*;

class LogRecordTest {
    @Test
    void testLogRecordBasics() {
        _LogData data = new LineFeed(1);
        LogRecord lr = new LogRecord(123, 1000L, data);
        assertEquals(123, lr.test());
        assertEquals(1000L, lr.time());
        assertEquals(data, lr.data());
    }
}
