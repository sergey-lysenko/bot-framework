package works.lysenko.util.data.records.grid;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import works.lysenko.util.data.enums.ScaledPropertiesSilent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("grid.ScaledPropertiesOptions Record Tests")
class ScaledPropertiesOptionsTest {

    @Test
    @DisplayName("Should store and retrieve fields")
    void testRecordFields() {
        final ScaledPropertiesOptions options = new ScaledPropertiesOptions(ScaledPropertiesSilent.YES, true, false);

        assertEquals(ScaledPropertiesSilent.YES, options.silent());
        assertTrue(options.noScales());
        assertFalse(options.allowNew());
    }

    @Test
    @DisplayName("Should handle null silent option")
    void testNullSilent() {
        final ScaledPropertiesOptions options = new ScaledPropertiesOptions(null, false, false);

        assertNull(options.silent());
        assertFalse(options.noScales());
        assertFalse(options.allowNew());
    }

    @Test
    @DisplayName("Should satisfy equals and hashCode contract")
    void testEqualsAndHashCode() {
        final ScaledPropertiesOptions o1 = new ScaledPropertiesOptions(ScaledPropertiesSilent.YES, true, true);
        final ScaledPropertiesOptions o2 = new ScaledPropertiesOptions(ScaledPropertiesSilent.YES, true, true);
        final ScaledPropertiesOptions o3 = new ScaledPropertiesOptions(ScaledPropertiesSilent.YES, false, true);

        assertEquals(o1, o2);
        assertEquals(o1.hashCode(), o2.hashCode());
        assertNotEquals(o1, o3);
    }

    @Test
    @DisplayName("Should produce non-null toString")
    void testToString() {
        final ScaledPropertiesOptions options = new ScaledPropertiesOptions(null, false, false);
        assertNotNull(options.toString());
    }
}
