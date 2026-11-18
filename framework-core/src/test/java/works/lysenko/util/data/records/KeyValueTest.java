package works.lysenko.util.data.records;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("KeyValue Record Tests")
class KeyValueTest {

    @Test
    @DisplayName("Should store and access key and value")
    void testFieldsAndAccessors() {
        final KeyValue<String, Integer> kv = new KeyValue<>("key1", 100);

        assertEquals("key1", kv.k());
        assertEquals(100, kv.v());
        assertEquals("key1", kv.name());
    }

    @Test
    @DisplayName("Should create KeyValue via kv() factory method")
    void testFactoryMethod() {
        final KeyValue<String, String> pair = KeyValue.kv("a", "b");

        assertEquals("a", pair.k());
        assertEquals("b", pair.v());
        assertEquals("a", pair.name());
    }

    @Test
    @DisplayName("Should format toString as key=value")
    void testToString() {
        final KeyValue<String, Integer> kv = KeyValue.kv("timeout", 5000);
        assertEquals("timeout=5000", kv.toString());
    }

    @Test
    @DisplayName("Should handle null key and value")
    void testNullValues() {
        final KeyValue<String, String> kv = KeyValue.kv(null, null);

        assertNull(kv.k());
        assertNull(kv.v());
        assertNull(kv.name());
        assertNotNull(kv.toString());
    }
}
