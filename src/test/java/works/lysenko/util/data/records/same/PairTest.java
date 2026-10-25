package works.lysenko.util.data.records.same;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("same.Pair Record Tests")
class PairTest {

    @Test
    @DisplayName("Should store and access left and right values via all accessors")
    void testRecordFieldsAndAccessors() {
        final Pair<String> pair = Pair.p("first", "second");

        assertEquals("first", pair.left());
        assertEquals("second", pair.right());
        assertEquals("first", pair.l());
        assertEquals("second", pair.r());
        assertEquals("first", pair.key());
        assertEquals("second", pair.value());
        assertEquals("first", pair.before());
        assertEquals("second", pair.after());
    }

    @Test
    @DisplayName("Should return left or right via any()")
    void testAny() {
        final Pair<String> pair = Pair.p("a", "b");
        final String anyVal = pair.any();

        assertNotNull(anyVal);
    }

    @Test
    @DisplayName("Should format toString representation as left, right")
    void testToString() {
        final Pair<String> pair = Pair.p("foo", "bar");
        assertEquals("foo, bar", pair.toString());
    }

    @Test
    @DisplayName("Should satisfy equals and hashCode contract")
    void testEqualsAndHashCode() {
        final Pair<Integer> p1 = Pair.p(10, 20);
        final Pair<Integer> p2 = Pair.p(10, 20);
        final Pair<Integer> p3 = Pair.p(10, 30);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
        assertNotEquals(p1, p3);
    }
}
