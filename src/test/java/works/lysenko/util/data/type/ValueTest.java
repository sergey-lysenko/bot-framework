package works.lysenko.util.data.type;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValueTest {

    @Test
    void testRecordGetters() {
        final Value value = new Value(10, 2, "stamp");
        assertEquals(10, value.value());
        assertEquals(2, value.precision());
        assertEquals("stamp", value.stamp());
    }
}
