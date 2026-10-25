package works.lysenko.util.data.records;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("PropertiesMeta Record Tests")
class PropertiesMetaTest {

    @Test
    @DisplayName("Should store and retrieve defaults and keysWithValidClasses")
    void testRecordFields() {
        final Map<String, String> defaults = Map.of("key1", "val1");
        final List<String> validKeys = List.of("classKey");

        final PropertiesMeta meta = new PropertiesMeta(defaults, validKeys);

        assertEquals(defaults, meta.defaults());
        assertEquals(validKeys, meta.keysWithValidClasses());
    }

    @Test
    @DisplayName("Should handle null defaults and keys list")
    void testNullFields() {
        final PropertiesMeta meta = new PropertiesMeta(null, null);

        assertNull(meta.defaults());
        assertNull(meta.keysWithValidClasses());
    }

    @Test
    @DisplayName("Should satisfy equals and hashCode contract")
    void testEqualsAndHashCode() {
        final PropertiesMeta pm1 = new PropertiesMeta(Map.of("a", "b"), List.of("a"));
        final PropertiesMeta pm2 = new PropertiesMeta(Map.of("a", "b"), List.of("a"));
        final PropertiesMeta pm3 = new PropertiesMeta(Map.of("a", "c"), List.of("a"));

        assertEquals(pm1, pm2);
        assertEquals(pm1.hashCode(), pm2.hashCode());
        assertNotEquals(pm1, pm3);
    }

    @Test
    @DisplayName("Should produce non-null toString")
    void testToString() {
        final PropertiesMeta meta = new PropertiesMeta(Map.of(), List.of());
        assertNotNull(meta.toString());
    }
}
