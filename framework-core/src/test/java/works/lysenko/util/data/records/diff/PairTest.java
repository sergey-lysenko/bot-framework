package works.lysenko.util.data.records.diff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("diff.Pair Record Tests")
class PairTest {

    @Test
    @DisplayName("Should store and retrieve left and right values via accessors and interface methods")
    void testRecordFieldsAndAccessors() {
        final Pair<String, Integer> pair = Pair.p("left", 123);

        assertEquals("left", pair.left());
        assertEquals(123, pair.right());
        assertEquals("left", pair.l());
        assertEquals(123, pair.r());
        assertEquals("left", pair.key());
        assertEquals(123, pair.value());
    }

    @Test
    @DisplayName("Should handle null values")
    void testNullValues() {
        final Pair<String, String> pair = Pair.p(null, null);

        assertNull(pair.left());
        assertNull(pair.right());
        assertNull(pair.l());
        assertNull(pair.r());
        assertNull(pair.key());
        assertNull(pair.value());
    }

    @Test
    @DisplayName("Should format toString representation as left, right")
    void testToString() {
        final Pair<String, String> pair = Pair.p("hello", "world");
        assertEquals("hello, world", pair.toString());
    }
}
