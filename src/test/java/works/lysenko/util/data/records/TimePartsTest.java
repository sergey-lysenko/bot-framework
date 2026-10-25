package works.lysenko.util.data.records;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimePartsTest {

    @Test
    void testGetCalculation() {
        // 1 day + 2 hours + 3 minutes + 4 seconds + 500 ms
        // 86400000 + 7200000 + 180000 + 4000 + 500 = 93784500
        final long totalMs = 86400000L + 7200000L + 180000L + 4000L + 500L;
        final TimeParts parts = TimeParts.get(totalMs);

        assertEquals(1, parts.days());
        assertEquals(2, parts.hours());
        assertEquals(3, parts.minutes());
        assertEquals(4, parts.seconds());
        assertEquals(500, parts.ms());
    }
}
