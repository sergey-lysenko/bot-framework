package works.lysenko.util.data.records.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("test.TestSafeguard Record Tests")
class TestSafeguardTest {

    @Test
    @DisplayName("Should store and retrieve tests and milliseconds")
    void testRecordFields() {
        final TestSafeguard safeguard = new TestSafeguard(10, 5000);

        assertEquals(10, safeguard.tests());
        assertEquals(5000, safeguard.milliseconds());
    }

    @Test
    @DisplayName("Should construct TestSafeguard from delimited string")
    void testStringConstructor() {
        final TestSafeguard safeguard = new TestSafeguard("5|1000");

        assertEquals(5, safeguard.tests());
        assertEquals(1000, safeguard.milliseconds());
    }

    @Test
    @DisplayName("Should satisfy equals and hashCode contract")
    void testEqualsAndHashCode() {
        final TestSafeguard s1 = new TestSafeguard(5, 100);
        final TestSafeguard s2 = new TestSafeguard(5, 100);
        final TestSafeguard s3 = new TestSafeguard(5, 200);

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
        assertNotEquals(s1, s3);
    }

    @Test
    @DisplayName("Should produce non-null toString")
    void testToString() {
        final TestSafeguard safeguard = new TestSafeguard(1, 2);
        assertNotNull(safeguard.toString());
    }
}
