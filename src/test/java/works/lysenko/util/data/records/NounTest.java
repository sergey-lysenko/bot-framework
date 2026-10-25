package works.lysenko.util.data.records;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("Noun Record Tests")
class NounTest {

    @Test
    @DisplayName("Should store and retrieve singular and plural forms")
    void testRecordFields() {
        final Noun noun = new Noun("cat", "cats");

        assertEquals("cat", noun.singular());
        assertEquals("cats", noun.plural());
    }

    @Test
    @DisplayName("Should handle null values")
    void testNullValues() {
        final Noun noun = new Noun(null, null);

        assertNull(noun.singular());
        assertNull(noun.plural());
    }

    @Test
    @DisplayName("Should satisfy equals and hashCode contract")
    void testEqualsAndHashCode() {
        final Noun n1 = new Noun("box", "boxes");
        final Noun n2 = new Noun("box", "boxes");
        final Noun n3 = new Noun("box", "boxen");

        assertEquals(n1, n2);
        assertEquals(n1.hashCode(), n2.hashCode());
        assertNotEquals(n1, n3);
    }

    @Test
    @DisplayName("Should produce non-null toString")
    void testToString() {
        final Noun noun = new Noun("item", "items");
        assertNotNull(noun.toString());
    }
}
