package works.lysenko.util.data.records.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("test.TestSafeguard Record Tests")
class TestSafeguardTest {

    @Test
    @DisplayName("Should construct TestSafeguard from delimited string")
    void testStringConstructor() {
        final TestSafeguard safeguard = new TestSafeguard("5|1000");

        assertEquals(5, safeguard.tests());
        assertEquals(1000, safeguard.milliseconds());
    }
}
