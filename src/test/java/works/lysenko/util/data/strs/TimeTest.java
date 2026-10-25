package works.lysenko.util.data.strs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TimeTest {

    @Test
    void testTimeFormatting() {
        assertNull(Time.t(-1));
        assertEquals("less than millisecond", Time.t(0));
        assertEquals("1 ms", Time.t(1));
        assertEquals("1 s 500 ms", Time.t(1500));
        assertEquals("1 d 2 h 3 m 4 s 5 ms", Time.t(86400000L + 7200000L + 180000L + 4000L + 5L));
    }
}
