package works.lysenko.util.data.records.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("test.TestTime Record Tests")
class TestTimeTest {

    @Test
    @DisplayName("Should store and retrieve test and time")
    void testRecordFields() {
        final TestTime testTime = new TestTime("loginTest", "1200ms");

        assertEquals("loginTest", testTime.test());
        assertEquals("1200ms", testTime.time());
    }

    @Test
    @DisplayName("Should handle null values")
    void testNullValues() {
        final TestTime testTime = new TestTime(null, null);

        assertNull(testTime.test());
        assertNull(testTime.time());
    }

    @Test
    @DisplayName("Should satisfy equals and hashCode contract")
    void testEqualsAndHashCode() {
        final TestTime t1 = new TestTime("t1", "100ms");
        final TestTime t2 = new TestTime("t1", "100ms");
        final TestTime t3 = new TestTime("t1", "200ms");

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertNotEquals(t1, t3);
    }

    @Test
    @DisplayName("Should produce non-null toString")
    void testToString() {
        final TestTime testTime = new TestTime("test", "time");
        assertNotNull(testTime.toString());
    }
}
